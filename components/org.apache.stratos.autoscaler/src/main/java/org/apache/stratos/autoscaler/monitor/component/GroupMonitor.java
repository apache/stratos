/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.monitor.component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.PartitionAlgorithm;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.group.GroupInstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.PartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ParentLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.NetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.*;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.partition.NetworkPartitionRef;
import org.apache.stratos.common.partition.PartitionRef;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.domain.application.GroupStatus;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends ParentComponentMonitor {

    private static final Log log = LogFactory.getLog(GroupMonitor.class);

    private final ExecutorService executorService;
    //has scaling dependents
    protected boolean hasScalingDependents;
    //Indicates whether groupScaling enabled or not
    private boolean groupScalingEnabled;

    /**
     * Constructor of GroupMonitor
     *
     * @param group Takes the group from the Topology
     * @throws DependencyBuilderException    throws when couldn't build the Topology
     * @throws TopologyInConsistentException throws when topology is inconsistent
     */
    public GroupMonitor(Group group, String appId, List<String> parentInstanceId,
                        boolean hasScalingDependents) throws DependencyBuilderException,
            TopologyInConsistentException {
        super(group);

        int threadPoolSize = Integer.getInteger(AutoscalerConstants.GROUP_MONITOR_THREAD_POOL_SIZE, 20);
        this.executorService = StratosThreadPool.getExecutorService(
                AutoscalerConstants.GROUP_MONITOR_THREAD_POOL_ID, threadPoolSize);

        this.groupScalingEnabled = group.isGroupScalingEnabled();
        this.appId = appId;
        this.hasScalingDependents = hasScalingDependents;
    }

    @Override
    public MonitorType getMonitorType() {
        return MonitorType.Group;
    }

    @Override
    public void run() {
        try {
            monitor();
        } catch (Exception e) {
            log.error("Group monitor failed : " + this.toString(), e);
        }
    }

    /**
     * This thread will monitor the children across all the network partitions and take
     * decision for scale-up or scale-down
     */
    public synchronized void monitor() {
        final Collection<NetworkPartitionContext> networkPartitionContexts =
                this.getNetworkPartitionContextsMap().values();

        Runnable monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (log.isDebugEnabled()) {
                    log.debug("Group monitor is running: [group] " + id);
                }
                for (NetworkPartitionContext networkPartitionContext : networkPartitionContexts) {

                    for (InstanceContext instanceContext : networkPartitionContext.
                            getInstanceIdToInstanceContextMap().values()) {
                        GroupInstance instance = (GroupInstance) instanceIdToInstanceMap.
                                get(instanceContext.getId());
                        //stopping the monitoring when the group is inactive/Terminating/Terminated
                        if (instance.getStatus().getCode() <= GroupStatus.Active.getCode()) {
                            //Gives priority to scaling max out rather than dependency scaling
                            if (!instanceContext.getIdToScalingOverMaxEvent().isEmpty()) {
                                //handling the max out of the children
                                handleScalingUpBeyondMax(instanceContext, networkPartitionContext);

                            } else if (!instanceContext.getIdToScalingEvent().isEmpty()) {
                                //handling the dependent scaling
                                handleDependentScaling(instanceContext, networkPartitionContext);

                            } else if (!instanceContext.getIdToScalingDownBeyondMinEvent().isEmpty()) {
                                //scale down only when extra instances found
                                handleScalingDownBeyondMin(instanceContext,
                                        networkPartitionContext, false);
                            }

                            //Resetting the events events
                            instanceContext.setIdToScalingOverMaxEvent(
                                    new ConcurrentHashMap<String, ScalingUpBeyondMaxEvent>());
                            instanceContext.setIdToScalingEvent(
                                    new ConcurrentHashMap<String, ScalingEvent>());
                            instanceContext.setIdToScalingOverMaxEvent(
                                    new ConcurrentHashMap<String, ScalingUpBeyondMaxEvent>());
                        }
                    }

                    ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext
                            = (ParentLevelNetworkPartitionContext) networkPartitionContext;
                    int nonTerminatedInstancesCount = parentLevelNetworkPartitionContext.
                            getNonTerminatedInstancesCount();
                    int minInstances = parentLevelNetworkPartitionContext.
                            getMinInstanceCount();
                    int maxInstances = parentLevelNetworkPartitionContext.
                            getMaxInstanceCount();
                    int activeInstances = parentLevelNetworkPartitionContext.getActiveInstancesCount();
                    if (nonTerminatedInstancesCount < minInstances) {
                        int instancesToBeCreated = minInstances - nonTerminatedInstancesCount;
                        for (int i = 0; i < instancesToBeCreated; i++) {
                            for (InstanceContext parentInstanceContext : parent.
                                    getNetworkPartitionContext(parentLevelNetworkPartitionContext.getId()).
                                    getInstanceIdToInstanceContextMap().values()) {
                                //keep on scale-up/scale-down only if the application is active
                                ApplicationMonitor appMonitor = AutoscalerContext.getInstance().
                                        getAppMonitor(appId);
                                int activeAppInstances = ((ParentLevelNetworkPartitionContext) appMonitor.
                                        getNetworkPartitionContext(parentLevelNetworkPartitionContext.getId())).
                                        getActiveInstancesCount();
                                if (activeAppInstances > 0) {
                                    //Creating new group instance based on the existing parent instances
                                    createInstanceOnDemand(parentInstanceContext.getId());
                                }
                            }

                        }
                    }
                    //If the active instances are higher than the max instances,
                    // the group instance has to get terminated
                    if (activeInstances > maxInstances) {
                        int instancesToBeTerminated = activeInstances - maxInstances;
                        Collection<InstanceContext> contexts = networkPartitionContext.
                                getInstanceIdToInstanceContextMap().values();
                        List<InstanceContext> contextList = new ArrayList<InstanceContext>(contexts);
                        for (int i = 0; i < instancesToBeTerminated; i++) {
                            InstanceContext instanceContext = contextList.get(i);
                            //scale down only when extra instances found
                            handleScalingDownBeyondMin(instanceContext,
                                    networkPartitionContext, true);

                        }
                    }
                }
            }
        };
        executorService.execute(monitoringRunnable);
    }

    /**
     * Handling the max out case of the group instances
     *
     * @param instanceContext         the instance which reaches its max
     * @param networkPartitionContext the network-partition used for the instances
     */
    private void handleScalingUpBeyondMax(InstanceContext instanceContext,
                                          NetworkPartitionContext networkPartitionContext) {
        if (!hasScalingDependents) {
            //handling the group scaling and if pending instances found,
            // reject the max
            createGroupInstanceOnScaling(networkPartitionContext,
                    instanceContext.getParentInstanceId());
        } else {
            notifyParentOnScalingUpBeyondMax(networkPartitionContext, instanceContext);
        }
    }

    /**
     * Handling the group scale-down
     *
     * @param instanceContext    the instance which has all the children notification with scale-down
     * @param nwPartitionContext the network-partition used for the instance
     * @param forceScaleDown     whether it is force scale-down or not
     */
    private void handleScalingDownBeyondMin(InstanceContext instanceContext,
                                            NetworkPartitionContext nwPartitionContext,
                                            boolean forceScaleDown) {
        //Traverse through all the children to see whether all have sent the scale down
        boolean allChildrenScaleDown = false;
        for (Monitor monitor : this.aliasToActiveChildMonitorsMap.values()) {
            if (instanceContext.getScalingDownBeyondMinEvent(monitor.getId()) == null) {
                allChildrenScaleDown = false;
                break;
            } else {
                allChildrenScaleDown = true;
            }
        }
        //all the children sent the scale down, then the group-instance will try to scale down or
        // if it is a force scale-down
        if (allChildrenScaleDown || forceScaleDown) {
            if (hasScalingDependents) {
                ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext
                        = (ParentLevelNetworkPartitionContext) nwPartitionContext;
                if (parentLevelNetworkPartitionContext.getNonTerminatedInstancesCount() >
                        parentLevelNetworkPartitionContext.getMinInstanceCount()) {
                    //Will scale down based on dependent manner
                    float minInstances = parentLevelNetworkPartitionContext.getMinInstanceCount();
                    float factor =
                            (parentLevelNetworkPartitionContext.getNonTerminatedInstancesCount() - 1) / minInstances;
                    ScalingEvent scalingEvent = new ScalingEvent(this.id, parentLevelNetworkPartitionContext.getId(),
                            instanceContext.getId(), factor);
                    this.parent.onChildScalingEvent(scalingEvent);
                } else {
                    //Parent has to handle this scale down as by dependent scale down
                    ScalingDownBeyondMinEvent newScalingDownBeyondMinEvent =
                            new ScalingDownBeyondMinEvent(this.id,
                                    parentLevelNetworkPartitionContext.getId(), instanceContext.getParentInstanceId());
                    this.parent.onChildScalingDownBeyondMinEvent(newScalingDownBeyondMinEvent);
                }

            } else {
                if (groupScalingEnabled) {
                    ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext
                            = (ParentLevelNetworkPartitionContext) nwPartitionContext;
                    if (parentLevelNetworkPartitionContext.getNonTerminatedInstancesCount() >
                            parentLevelNetworkPartitionContext.getMinInstanceCount()) {
                        //send terminating to the specific group instance in the scale down
                        ApplicationBuilder.handleGroupTerminatingEvent(this.appId, this.id,
                                instanceContext.getId());
                    }
                } else {
                    //Parent has to handle this scale down as by parent group
                    // scale down or application scale down
                    ScalingDownBeyondMinEvent newScalingDownBeyondMinEvent =
                            new ScalingDownBeyondMinEvent(this.id,
                                    nwPartitionContext.getId(), instanceContext.getParentInstanceId());
                    this.parent.onChildScalingDownBeyondMinEvent(newScalingDownBeyondMinEvent);
                }
            }

        }
    }

    /**
     * Create a new group instance upon scale-up
     *
     * @param networkPartitionContext the network-partition used for the instances
     * @param parentInstanceId        the parent instance id
     */
    private void createGroupInstanceOnScaling(final NetworkPartitionContext networkPartitionContext,
                                              final String parentInstanceId) {
        if (groupScalingEnabled) {
            if (((ParentLevelNetworkPartitionContext) networkPartitionContext).getPendingInstancesCount() == 0) {
                //one of the child is loaded and max out.
                // Hence creating new group instance
                if (log.isDebugEnabled()) {
                    log.debug("Handling group scaling for the [group] " + id +
                            "upon a max out event from " +
                            "the children");
                }
                boolean createOnDemand = createInstanceOnDemand(parentInstanceId);
                if (!createOnDemand) {
                    //couldn't create new instance. Hence notifying the parent
                    Runnable sendScaleMaxOut = new Runnable() {
                        @Override
                        public void run() {
                            MonitorStatusEventBuilder.handleScalingOverMaxEvent(parent,
                                    networkPartitionContext.getId(),
                                    parentInstanceId,
                                    appId);
                        }
                    };
                    executorService.execute(sendScaleMaxOut);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Pending Group instance found. " +
                            "Hence waiting for it to become active");
                }
            }

        } else {
            //notifying the parent if no group scaling enabled here
            Runnable sendScaleMaxOut = new Runnable() {
                @Override
                public void run() {
                    MonitorStatusEventBuilder.handleScalingOverMaxEvent(parent,
                            networkPartitionContext.getId(),
                            parentInstanceId,
                            appId);
                }
            };
            executorService.execute(sendScaleMaxOut);
        }
    }

    /**
     * Notify the parent when there is not space to scale-up
     *
     * @param networkPartitionContext the network-partition used for the instances
     * @param instanceContext         the instance which reaches its max
     */
    private void notifyParentOnScalingUpBeyondMax(NetworkPartitionContext networkPartitionContext,
                                                  InstanceContext instanceContext) {
        //has scaling dependents. Should notify the parent
        if (log.isDebugEnabled()) {
            log.debug("This [Group] " + id + " [scale-up] dependencies. " +
                    "Hence notifying the [parent] " + parent.getId());
        }
        ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext
                = (ParentLevelNetworkPartitionContext) networkPartitionContext;
        //notifying the parent when scale dependents found
        int maxInstances = parentLevelNetworkPartitionContext.getMaxInstanceCount();
        if (groupScalingEnabled && maxInstances > parentLevelNetworkPartitionContext.
                getNonTerminatedInstancesCount()) {
            //increase group by one more instance and calculate the factor for the group scaling
            // and notify parent to scale all the dependent in parallel with this factor
            float minInstances = ((ParentLevelNetworkPartitionContext)
                    networkPartitionContext).getMinInstanceCount();

            float factor = (minInstances + 1) / minInstances;
            MonitorStatusEventBuilder.
                    handleClusterScalingEvent(parent,
                            networkPartitionContext.getId(),
                            instanceContext.getParentInstanceId(),
                            factor, id);
        } else {
            MonitorStatusEventBuilder.handleScalingOverMaxEvent(parent,
                    networkPartitionContext.getId(),
                    instanceContext.getParentInstanceId(),
                    id);
        }
    }

    /**
     * Will set the status of the monitor based on Topology Group status/child status like scaling
     *
     * @param status status of the group
     */
    public void setStatus(GroupStatus status, String instanceId, String parentInstanceId) {
        GroupInstance groupInstance = (GroupInstance) this.instanceIdToInstanceMap.get(instanceId);
        if (groupInstance == null) {
            if (status != GroupStatus.Terminated) {
                log.warn("The required group [instance] " + instanceId + " not found in the GroupMonitor");
            }
        } else {
            if (groupInstance.getStatus() != status) {
                groupInstance.setStatus(status);
            }
        }
        Group group = ApplicationHolder.getApplications().getApplication(this.appId).
                getGroupRecursively(this.id);
        if (group != null) {

            int minGroupInstances = group.getGroupMinInstances();
            int maxGroupInstances = group.getGroupMaxInstances();
            //Checking whether group scaling enabled or whether spinning group instances possible,
            // then have to use parent instance Id when notifying the parent
            // as parent doesn't aware if more than one group instances are there
            if (this.isGroupScalingEnabled() || minGroupInstances > 1 || maxGroupInstances > 1) {
                try {
                    ApplicationHolder.acquireReadLock();
                    Application application = ApplicationHolder.getApplications().
                            getApplication(this.appId);
                    if (application != null) {
                        //Notifying the parent using parent's instance Id,
                        // as it has group scaling enabled. Notify parent
                        log.info("[Group] " + this.id + " is notifying the [parent] " +
                                this.parent.getId() + " [instance] " + parentInstanceId);
                        MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent,
                                status, this.id, parentInstanceId);

                    }
                } finally {
                    ApplicationHolder.releaseReadLock();
                }
            } else {
                // notifying the parent
                log.info("[Group] " + this.id + " is notifying the [parent] "
                        + this.parent.getId() + " [instance] " + instanceId);
                MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent,
                        status, this.id, instanceId);
            }
        }

        //notify the children about the state change
        try {
            MonitorStatusEventBuilder.notifyChildren(this, new GroupStatusEvent(status,
                    this.id, instanceId));
        } catch (MonitorNotFoundException e) {
            log.error("Error while notifying the children from the [group] " + this.id, e);
            //TODO revert siblings
        }
    }

    @Override
    public void onChildStatusEvent(MonitorStatusEvent statusEvent) {
        String childId = statusEvent.getId();
        String instanceId = statusEvent.getInstanceId();
        LifeCycleState status1 = statusEvent.getStatus();
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination
        if (status1 == GroupStatus.Active) {
            //Verifying whether all the minimum no of instances of child
            // became active to take next action
            boolean isChildActive = verifyGroupStatus(childId, instanceId, GroupStatus.Active);
            if (isChildActive) {
                onChildActivatedEvent(childId, instanceId);
            } else {
                log.info("Waiting for other group instances to be active");
            }

        } else if (status1 == ClusterStatus.Active) {
            onChildActivatedEvent(childId, instanceId);

        } else if (status1 == ClusterStatus.Inactive || status1 == GroupStatus.Inactive) {
            markInstanceAsInactive(childId, instanceId);
            onChildInactiveEvent(childId, instanceId);

        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inactive in the map
            markInstanceAsTerminating(childId, instanceId);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Act upon child instance termination
            onTerminationOfInstance(childId, instanceId);
        }
    }

    /**
     * Get executed to see whether to start a new instance or not,
     * when one of the child instance is terminated
     *
     * @param childId    child-id of the terminated instance
     * @param instanceId instance-id of the terminated instance
     */
    private void onTerminationOfInstance(String childId, String instanceId) {
        //Check whether all dependent goes Terminated and then start them in parallel.
        removeInstanceFromFromInactiveMap(childId, instanceId);
        removeInstanceFromFromTerminatingMap(childId, instanceId);

        GroupInstance instance = (GroupInstance) instanceIdToInstanceMap.get(instanceId);
        if (instance != null) {
            // If this parent instance is terminating, then based on child notification,
            // it has to decide its state rather than starting a the children recovery
            if (instance.getStatus() == GroupStatus.Terminating ||
                    instance.getStatus() == GroupStatus.Terminated) {
                ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().process(id,
                        appId, instanceId);
            } else {
                //Checking whether the child who notified is still active.
                // If it is active(scale down case), no need to act upon it.
                // Otherwise act upon Termination and see whether it is required to start
                // instance again based on termination behavior
                boolean active = verifyGroupStatus(childId, instanceId, GroupStatus.Active);
                if (!active) {
                    onChildTerminatedEvent(childId, instanceId);
                } else {
                    log.info("[Group Instance] " + instanceId + " is still active upon termination" +
                            " of the [child] " + childId);
                }
            }
        } else {
            log.warn("The required instance cannot be found in the the [GroupMonitor] " +
                    id);
        }
    }


    @Override
    public void onParentStatusEvent(MonitorStatusEvent statusEvent)
            throws MonitorNotFoundException {
        String instanceId = statusEvent.getInstanceId();
        // send the ClusterTerminating event
        if (statusEvent.getStatus() == GroupStatus.Terminating ||
                statusEvent.getStatus() == ApplicationStatus.Terminating) {
            //Get all the instances which related to this instanceId
            GroupInstance instance = (GroupInstance) this.instanceIdToInstanceMap.get(instanceId);
            if (instance != null) {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Publishing Group terminating event for [application] " +
                            "%s [group] %s [instance] %s", appId, id, instanceId));
                }
                ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId);
            } else {
                //Using parentId need to get the all the children and ask them to terminate
                // as they can't exist without the parent
                List<String> instanceIds = this.getInstancesByParentInstanceId(instanceId);
                if (!instanceIds.isEmpty()) {
                    for (String instanceId1 : instanceIds) {
                        if (log.isInfoEnabled()) {
                            log.info(String.format("Publishing Group terminating event for" +
                                            " [application] %s [group] %s [instance] %s",
                                    appId, id, instanceId1));
                        }
                        ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId1);
                    }
                }

            }
        } else if (statusEvent.getStatus() == ClusterStatus.Created ||
                statusEvent.getStatus() == GroupStatus.Created) {
            //starting a new instance of this monitor
            createInstanceOnDemand(statusEvent.getInstanceId());
        }
    }

    @Override
    public void onParentScalingEvent(ScalingEvent scalingEvent) {

        log.info("Parent scaling event received to [group]: " + this.getId()
                + ", [network partition]: " + scalingEvent.getNetworkPartitionId()
                + ", [event] " + scalingEvent.getId() + ", [group instance] " +
                scalingEvent.getInstanceId() + ", [factor] " + scalingEvent.getFactor());

        //Parent notification always brings up new group instances in order to keep the ratio.
        String networkPartitionId = scalingEvent.getNetworkPartitionId();
        final String parentInstanceId = scalingEvent.getInstanceId();
        final NetworkPartitionContext networkPartitionContext = this.getNetworkPartitionContextsMap().
                get(networkPartitionId);

        float factor = scalingEvent.getFactor();
        ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext
                = (ParentLevelNetworkPartitionContext) networkPartitionContext;
        int currentInstances = parentLevelNetworkPartitionContext.getNonTerminatedInstancesCount();
        float requiredInstances = factor * parentLevelNetworkPartitionContext.getMinInstanceCount();
        int ceilingRequiredInstances = (int) Math.ceil(requiredInstances);
        if (ceilingRequiredInstances > currentInstances) {

            int instancesToBeCreated = ceilingRequiredInstances - currentInstances;
            for (int count = 0; count < instancesToBeCreated; count++) {

                createGroupInstanceOnScaling(networkPartitionContext, parentInstanceId);
            }
        } else if (ceilingRequiredInstances < currentInstances) {

            int instancesToBeTerminated = currentInstances - ceilingRequiredInstances;
            for (int count = 0; count < instancesToBeTerminated; count++) {

                //have to scale down
                if (parentLevelNetworkPartitionContext.getPendingInstancesCount() != 0) {
                    ApplicationBuilder.handleGroupTerminatingEvent(appId, this.id,
                            parentLevelNetworkPartitionContext.getPendingInstances().get(0).getId());

                } else {
                    List<InstanceContext> activeInstances =
                            parentLevelNetworkPartitionContext.getActiveInstances();
                    ApplicationBuilder.handleGroupTerminatingEvent(appId, this.id,
                            activeInstances.get(activeInstances.size() - 1).toString());
                }
            }
        }


    }

    /**
     * Whether group-scaling-enabled for this group or not
     *
     * @return whether group-scaling-enabled or not
     */
    public boolean isGroupScalingEnabled() {
        return groupScalingEnabled;
    }

    /**
     * Gets the parent instance context.
     *
     * @param parentInstanceId the parent instance id
     * @return the parent instance context
     */
    private Instance getParentInstanceContext(String parentInstanceId) {
        Instance parentInstanceContext;

        Application application = ApplicationHolder.getApplications().getApplication(this.appId);
        //if parent is application
        if (this.parent.getId().equals(appId)) {
            parentInstanceContext = application.getInstanceContexts(parentInstanceId);
        } else {
            //if parent is group
            Group parentGroup = application.getGroupRecursively(this.parent.getId());
            parentInstanceContext = parentGroup.getInstanceContexts(parentInstanceId);
        }

        return parentInstanceContext;
    }

    /**
     * Gets the group level network partition context.
     *
     * @param parentInstanceContext the parent instance context
     * @return the group level network partition context
     */
    private ParentLevelNetworkPartitionContext getGroupLevelNetworkPartitionContext(String groupAlias,
                                                                                    String appId,
                                                                                    Instance parentInstanceContext) {
        ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext;
        String deploymentPolicyId = AutoscalerUtil.getDeploymentPolicyIdByAlias(appId, groupAlias);
        DeploymentPolicy deploymentPolicy = PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId);

        String networkPartitionId = parentInstanceContext.getNetworkPartitionId();
        if (this.getNetworkPartitionContextsMap().containsKey(networkPartitionId)) {
            parentLevelNetworkPartitionContext = (ParentLevelNetworkPartitionContext) this.getNetworkPartitionContextsMap().
                    get(networkPartitionId);
        } else {
            if (deploymentPolicy != null) {
                NetworkPartitionRef[] networkPartitions = deploymentPolicy.getNetworkPartitionRefs();
                NetworkPartitionRef networkPartition = null;
                for (NetworkPartitionRef networkPartition1 : networkPartitions) {
                    if (networkPartition1.getId().equals(networkPartitionId)) {
                        networkPartition = networkPartition1;
                    }
                }

                if (networkPartition != null) {
                    parentLevelNetworkPartitionContext = new ParentLevelNetworkPartitionContext(
                            networkPartitionId,
                            networkPartition.getPartitionAlgo());
                } else {
                    parentLevelNetworkPartitionContext = new ParentLevelNetworkPartitionContext(
                            networkPartitionId);
                }

            } else {
                parentLevelNetworkPartitionContext = new ParentLevelNetworkPartitionContext(
                        networkPartitionId);
            }
            if (log.isInfoEnabled()) {
                log.info("[Network partition] " + networkPartitionId + "has been added for the " +
                        "[Group] " + this.id);
            }
            this.addNetworkPartitionContext(parentLevelNetworkPartitionContext);
        }
        return parentLevelNetworkPartitionContext;
    }

    /**
     * Finds the correct partition context to which the instance should be added to and
     * created and adds required context objects.
     *
     * @param parentInstanceContext   the parent instance context
     * @param networkPartitionContext the GroupLevelNetworkPartitionContext
     * @param groupAlias              alias of the group
     */
    private void addPartitionContext(Instance parentInstanceContext,
                                     ParentLevelNetworkPartitionContext networkPartitionContext, String groupAlias) {

        String networkPartitionId = parentInstanceContext.getNetworkPartitionId();

        String deploymentPolicyId = AutoscalerUtil.getDeploymentPolicyIdByAlias(appId, groupAlias);
        DeploymentPolicy deploymentPolicy = PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId);

        if (deploymentPolicy == null) {

            String parentPartitionId = parentInstanceContext.getPartitionId();
            if (parentPartitionId != null && networkPartitionContext.getPartitionCtxt(parentPartitionId) == null) {
                GroupLevelPartitionContext partitionContext = new GroupLevelPartitionContext(parentPartitionId, networkPartitionId);
                networkPartitionContext.addPartitionContext(partitionContext);
                if (log.isInfoEnabled()) {
                    log.info("[Partition] " + parentPartitionId + "has been added for the " + "[Group] " + this.id);
                }
            }

        } else {

            NetworkPartitionRef[] networkPartitions = deploymentPolicy.getNetworkPartitionRefs();
            NetworkPartitionRef networkPartitionRef = null;
            if (networkPartitions != null && networkPartitions.length != 0) {
                for (NetworkPartitionRef i : networkPartitions) {
                    if (i.getId().equals(networkPartitionId)) {
                        networkPartitionRef = i;
                    }
                }
            }

            if (networkPartitionRef != null) {
                if (networkPartitionContext.getPartitionCtxts().isEmpty()) {
                    PartitionRef[] partitions = networkPartitionRef.getPartitionRefs();
                    if (partitions != null && partitions.length != 0) {
                        for (PartitionRef partition : partitions) {

                            if (networkPartitionContext.getPartitionCtxt(partition.getId()) == null) {

                                GroupLevelPartitionContext groupLevelPartitionContext = new GroupLevelPartitionContext(
                                        partition.getId(), networkPartitionId, deploymentPolicyId);
                                networkPartitionContext.addPartitionContext(groupLevelPartitionContext);
                                if (log.isInfoEnabled()) {
                                    log.info(String.format("[Partition] %s has been added for the [Group] %s",
                                            partition.getId(), this.id));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the group instance and adds the required context objects
     *
     * @param group                              the group
     * @param parentInstanceContext              the parent instance context
     * @param partitionContext                   partition-context used to create the group instance
     * @param parentLevelNetworkPartitionContext the group level network partition context
     */
    private String createGroupInstanceAndAddToMonitor(Group group, Instance parentInstanceContext,
                                                      PartitionContext partitionContext,
                                                      ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext,
                                                      GroupInstance groupInstance) {

        String partitionId = null;

        if (groupInstance == null) {
            if (partitionContext != null) {
                partitionId = partitionContext.getPartitionId();
            }

            groupInstance = createGroupInstance(group, parentInstanceContext.getNetworkPartitionId(),
                    parentInstanceContext.getInstanceId(), partitionId);
        }

        this.addInstance(groupInstance);

        String instanceId = groupInstance.getInstanceId();
        GroupInstanceContext groupInstanceContext = new GroupInstanceContext(instanceId);
        groupInstanceContext.setParentInstanceId(groupInstance.getParentId());

        groupInstanceContext.addPartitionContext((GroupLevelPartitionContext) partitionContext);
        parentLevelNetworkPartitionContext.addInstanceContext(groupInstanceContext);
        parentLevelNetworkPartitionContext.addPendingInstance(groupInstanceContext);

        if (log.isInfoEnabled()) {
            log.info("Group [Instance context] " + instanceId +
                    " has been added to [Group] " + this.id);
        }

        if (partitionContext != null) {
            ((GroupLevelPartitionContext) partitionContext).addActiveInstance(groupInstance);
        }

        return instanceId;
    }

    /**
     * This will create the required instance and start the dependency
     * This method will be called on initial startup
     *
     * @param group             blue print of the instance to be started
     * @param parentInstanceIds parent instanceIds used to start the child instance
     * @throws TopologyInConsistentException
     */
    public boolean createInstanceAndStartDependencyAtStartup(Group group, List<String> parentInstanceIds)
            throws TopologyInConsistentException {
        boolean initialStartup = true;
        List<String> instanceIdsToStart = new ArrayList<String>();

        for (String parentInstanceId : parentInstanceIds) {
            // Get parent instance context
            Instance parentInstanceContext = getParentInstanceContext(parentInstanceId);

            // Get existing or create new GroupLevelNetworkPartitionContext
            ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext =
                    getGroupLevelNetworkPartitionContext(group.getUniqueIdentifier(),
                            this.appId, parentInstanceContext);
            //adding the partitionContext to the network partition context
            addPartitionContext(parentInstanceContext, parentLevelNetworkPartitionContext, group.getAlias());

            String groupInstanceId;
            PartitionContext partitionContext;
            String parentPartitionId = parentInstanceContext.getPartitionId();

            // Create GroupInstance for partition instance and add to required contexts for minimum instance count
            int groupMin = group.getGroupMinInstances();
            int groupMax = group.getGroupMaxInstances();
            //Setting the network-partition minimum instances as group min instances
            parentLevelNetworkPartitionContext.setMinInstanceCount(groupMin);
            parentLevelNetworkPartitionContext.setMaxInstanceCount(groupMax);

            //Have to check whether group has generated its own instances
            List<Instance> existingGroupInstances = group.getInstanceContextsWithParentId(parentInstanceId);
            for (Instance instance : existingGroupInstances) {
                initialStartup = false;
                partitionContext = parentLevelNetworkPartitionContext.
                        getPartitionContextById(instance.getPartitionId());
                groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                        partitionContext,
                        parentLevelNetworkPartitionContext,
                        (GroupInstance) instance);
                instanceIdsToStart.add(groupInstanceId);
            }

            /**
             * If the group instances have been partially created or not created,
             * then create everything
             */
            if (existingGroupInstances.size() <= groupMin) {
                for (int i = 0; i < groupMin - existingGroupInstances.size(); i++) {
                    // Get partitionContext to create instance in
                    partitionContext = getPartitionContext(parentLevelNetworkPartitionContext,
                            parentPartitionId);
                    groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                            partitionContext,
                            parentLevelNetworkPartitionContext,
                            null);
                    instanceIdsToStart.add(groupInstanceId);
                }
            }

        }
        if (log.isInfoEnabled()) {
            log.info("Starting the dependencies for the [Group] " + group.getUniqueIdentifier());
        }
        startDependency(group, instanceIdsToStart);
        return initialStartup;
    }

    /**
     * This will give the partition-context from the group-network-partition-context
     * based on the selected algorithm where if parent has a partition-id,
     * then that will be returned instead of parsing the algorithm
     *
     * @param parentLevelNetworkPartitionContext the group-network-partition-context
     * @param parentPartitionId                  parent-partition-id
     * @return the partition-context
     */
    private PartitionContext getPartitionContext(
            ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext,
            String parentPartitionId) {
        PartitionContext partitionContext = null;
        // Get partitionContext to create instance in
        List<GroupLevelPartitionContext> partitionContexts = parentLevelNetworkPartitionContext.
                getPartitionCtxts();
        GroupLevelPartitionContext[] groupLevelPartitionContexts =
                new GroupLevelPartitionContext[partitionContexts.size()];
        if (parentPartitionId == null) {
            if (!partitionContexts.isEmpty()) {
                PartitionAlgorithm algorithm = this.getAutoscaleAlgorithm(
                        parentLevelNetworkPartitionContext.getPartitionAlgorithm());
                partitionContext = algorithm.getNextScaleUpPartitionContext(
                        (partitionContexts.toArray(groupLevelPartitionContexts)));
            }
        } else {
            partitionContext = parentLevelNetworkPartitionContext.
                    getPartitionContextById(parentPartitionId);
        }
        return partitionContext;
    }


    /**
     * This will start the group instance based on the given parent instanceId
     * A new monitor is not created in this case
     *
     * @param parentInstanceId the parent instance id
     */
    public boolean createInstanceOnDemand(String parentInstanceId) {
        // Get parent instance context
        Instance parentInstanceContext = getParentInstanceContext(parentInstanceId);
        List<String> instanceIdsToStart = new ArrayList<String>();

        Group group = ApplicationHolder.getApplications().
                getApplication(this.appId).getGroupRecursively(this.id);

        // Get existing or create new GroupLevelNetworkPartitionContext
        ParentLevelNetworkPartitionContext parentLevelNetworkPartitionContext =
                getGroupLevelNetworkPartitionContext(group.getUniqueIdentifier(),
                        this.appId, parentInstanceContext);
        //adding the partitionContext to the network partition context
        addPartitionContext(parentInstanceContext, parentLevelNetworkPartitionContext,
                group.getAlias());

        String groupInstanceId;
        PartitionContext partitionContext;
        String parentPartitionId = parentInstanceContext.getPartitionId();
        int groupMax = group.getGroupMaxInstances();
        int groupMin = group.getGroupMinInstances();
        List<Instance> instances = group.getInstanceContextsWithParentId(parentInstanceId);
        if (instances.isEmpty()) {
            //Need to create totally new group instance
            for (int i = 0; i < groupMin; i++) {
                // Get partitionContext to create instance in
                partitionContext = getPartitionContext(parentLevelNetworkPartitionContext,
                        parentPartitionId);
                groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                        partitionContext,
                        parentLevelNetworkPartitionContext,
                        null);
                instanceIdsToStart.add(groupInstanceId);
            }
        } else {
            //have to create one more instance
            if (group.getInstanceContextCount() < groupMax) {
                // Get partitionContext to create instance in
                partitionContext = getPartitionContext(parentLevelNetworkPartitionContext,
                        parentPartitionId);
                if (partitionContext != null) {
                    groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                            partitionContext,
                            parentLevelNetworkPartitionContext,
                            null);
                    instanceIdsToStart.add(groupInstanceId);
                } else {
                    log.warn("Partition context is null, [Group] " + group.getUniqueIdentifier() + " has reached " +
                            "the maximum limit as [max] " + groupMax +
                            ". Hence trying to notify the parent.");
                }
            } else {
                log.warn("[Group] " + group.getUniqueIdentifier() + " has reached " +
                        "the maximum limit as [max] " + groupMax +
                        ". Hence trying to notify the parent.");
            }
        }
        boolean startedOnDemand = false;
        if (!instanceIdsToStart.isEmpty()) {
            startedOnDemand = true;

            //Starting the child instances
            startDependency(group, instanceIdsToStart);
        }

        return startedOnDemand;
    }


    /**
     * This will create the group instance in the applications Topology
     *
     * @param group              the group in which the instance is getting created
     * @param parentInstanceId   the parent instance id
     * @param partitionId        partition-id where the group instance is to be created
     * @param networkPartitionId network-partition-id where the group instance is to be created
     * @return the created group-instance
     */
    private GroupInstance createGroupInstance(Group group, String networkPartitionId,
                                              String parentInstanceId, String partitionId) {

        return ApplicationBuilder.handleGroupInstanceCreatedEvent(appId, group.getUniqueIdentifier(),
                parentInstanceId, networkPartitionId, partitionId);
    }

    /**
     * Add network-partition-context to the map
     *
     * @param parentLevelNetworkPartitionContext the group level network partition context
     */
    public void addNetworkPartitionContext(ParentLevelNetworkPartitionContext
                                                   parentLevelNetworkPartitionContext) {
        this.getNetworkPartitionContextsMap().put(parentLevelNetworkPartitionContext.getId(),
                parentLevelNetworkPartitionContext);
    }


    @Override
    public void destroy() {
        stopScheduler();
    }

}
