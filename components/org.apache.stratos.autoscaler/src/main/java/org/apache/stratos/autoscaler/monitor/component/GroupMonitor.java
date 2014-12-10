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
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupChildContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.group.GroupInstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.PartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.GroupLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.events.GroupStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ChildPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelNetworkPartition;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelPartition;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ApplicationStatus;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends ParentComponentMonitor implements Runnable {

    private static final Log log = LogFactory.getLog(GroupMonitor.class);

    //Indicates whether groupScaling enabled or not
    private boolean groupScalingEnabled;
    //Network partition contexts
    private Map<String, GroupLevelNetworkPartitionContext> networkPartitionCtxts;

    //Indicates whether the monitor is destroyed or not
    private boolean isDestroyed;
    //Monitoring interval of the monitor
    private int monitoringIntervalMilliseconds = 60000;     //TODO get this from config file

    /**
     * Constructor of GroupMonitor
     *
     * @param group Takes the group from the Topology
     * @throws DependencyBuilderException    throws when couldn't build the Topology
     * @throws TopologyInConsistentException throws when topology is inconsistent
     */
    public GroupMonitor(Group group, String appId, List<String> parentInstanceId) throws DependencyBuilderException,
            TopologyInConsistentException {
        super(group);
        this.appId = appId;
        networkPartitionCtxts = new HashMap<String, GroupLevelNetworkPartitionContext>();
    }

    @Override
    public void run() {
        while (!isDestroyed) {
            try {

                if (log.isDebugEnabled()) {
                    log.debug("Group monitor is running : " + this.toString());
                }
                monitor();
            } catch (Exception e) {
                log.error("Group monitor failed : " + this.toString(), e);
            }
            try {
                Thread.sleep(monitoringIntervalMilliseconds);
            } catch (InterruptedException ignore) {
            }
        }
    }

    public void monitor() {

        Runnable monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                //TODO implement group monitor
            }
        };
        monitoringRunnable.run();
    }

    /**
     * Will set the status of the monitor based on Topology Group status/child status like scaling
     *
     * @param status status of the group
     */
    public void setStatus(GroupStatus status, String instanceId) {
        GroupInstance groupInstance = (GroupInstance) this.instanceIdToInstanceMap.get(instanceId);
        if(groupInstance == null) {
            log.warn("The required group [instance] " + instanceId + " not found in the GroupMonitor");
        } else {
            if(groupInstance.getStatus() != status) {
                groupInstance.setStatus(status);
            }
        }
        if (status == GroupStatus.Inactive && !this.hasStartupDependents) {
            log.info("[Group] " + this.id + "is not notifying the parent, " +
                    "since it is identified as the independent unit");
        } else {
            // notify parent
            log.info("[Group] " + this.id + "is notifying the [parent] " + this.parent.getId());
            if (this.isGroupScalingEnabled()) {
                ApplicationHolder.acquireReadLock();
                try {
                    Application application = ApplicationHolder.getApplications().
                            getApplication(this.appId);
                    if (application != null) {
                        //Notifying the parent using parent's instance Id,
                        // as it has group scaling enabled.
                        Group group = application.getGroupRecursively(this.id);
                        if (group != null) {
                            GroupInstance context = group.getInstanceContexts(instanceId);
                            MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent,
                                    status, this.id, context.getParentId());

                        }
                    }
                } finally {
                    ApplicationHolder.releaseReadLock();
                }
            } else {
                MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent,
                        status, this.id, instanceId);
            }
        }
        //notify the children about the state change
        try {
            MonitorStatusEventBuilder.notifyChildren(this, new GroupStatusEvent(status, this.id, instanceId));
        } catch (MonitorNotFoundException e) {
            log.error("Error while notifying the children from the [group] " + this.id, e);
            //TODO revert siblings
        }
    }

    @Override
    public void onChildStatusEvent(MonitorStatusEvent statusEvent) {
        String id = statusEvent.getId();
        String instanceId = statusEvent.getInstanceId();
        LifeCycleState status1 = statusEvent.getStatus();
        //Events coming from parent are In_Active(in faulty detection), Scaling events, termination

        if (status1 == ClusterStatus.Active || status1 == GroupStatus.Active) {
            onChildActivatedEvent(id, instanceId);

        } else if (status1 == ClusterStatus.Inactive || status1 == GroupStatus.Inactive) {
            //handling restart of stratos
            if (!this.aliasToActiveMonitorsMap.get(id).hasStartupDependents()) {
                onChildActivatedEvent(id, instanceId);
            } else {
                this.markMonitorAsInactive(instanceId);
                onChildInactiveEvent(id, instanceId);
            }


        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inActive in the map
            this.markMonitorAsTerminating(instanceId);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Check whether all dependent goes Terminated and then start them in parallel.
            if (this.terminatingMonitorsList.contains(id)) {
                this.terminatingMonitorsList.remove(id);
                this.aliasToActiveMonitorsMap.remove(id);
            } else {
                log.warn("[monitor] " + id + " cannot be found in the inActive monitors list");
            }
            GroupInstance instance = (GroupInstance) this.instanceIdToInstanceMap.get(instanceId);
            if (instance != null) {
                if (instance.getStatus() == GroupStatus.Terminating || instance.getStatus() == GroupStatus.Terminated) {
                    ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().process(this.id,
                            appId, instanceId);
                } else {
                    onChildTerminatedEvent(id, instanceId);
                }
            } else {
                log.warn("The required instance cannot be found in the the [GroupMonitor] " +
                        this.id);
            }
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
                ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId);
            } else {
                //Using parentId need to get the children
                List<String> instanceIds = this.getInstancesByParentInstanceId(instanceId);
                if (!instanceIds.isEmpty()) {
                    for (String instanceId1 : instanceIds) {
                        ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId1);
                    }
                }

            }
        } else if (statusEvent.getStatus() == ClusterStatus.Created ||
                statusEvent.getStatus() == GroupStatus.Created) {
            Application application = ApplicationHolder.getApplications().getApplication(this.appId);
            Group group = application.getGroupRecursively(statusEvent.getId());
            //starting a new instance of this monitor
            createInstanceAndStartDependencyOnScaleup(statusEvent.getInstanceId());
        }
    }

    @Override
    public void onChildScalingEvent(MonitorScalingEvent scalingEvent) {

        if (hasGroupScalingDependent) {

            //notify parent
            parent.onChildScalingEvent(scalingEvent);
        }

        if (log.isDebugEnabled()) {
            log.debug("Child scaling event received to [group]: " + this.getId()
                    + ", [network partition]: " + scalingEvent.getNetworkPartitionId()
                    + ", [event] " + scalingEvent.getId() + ", [group instance] " + scalingEvent.getInstanceId());
        }

        //find the child context of this group, from scaling dependency tree
        GroupChildContext currentChildContextInScalingTree =
                (GroupChildContext) scalingDependencyTree.findApplicationContextWithIdInScalingDependencyTree(id);

        //Notifying children, if this group has scaling dependencies
        if (currentChildContextInScalingTree.isGroupScalingEnabled()) {
            for (ApplicationChildContext applicationChildContext :
                    currentChildContextInScalingTree.getApplicationChildContextList()) {

                //Get group monitor so that it can notify it's children
                Monitor monitor = aliasToActiveMonitorsMap.get(applicationChildContext.getId());

                if (monitor instanceof GroupMonitor || monitor instanceof ApplicationMonitor) {

                    monitor.onParentScalingEvent(scalingEvent);
                }
            }
        }
    }

    @Override
    public void onParentScalingEvent(MonitorScalingEvent scalingEvent) {

        //Notify all children about scaling
    }

    @Override
    public void onEvent(MonitorScalingEvent scalingEvent) {

    }

    public boolean isGroupScalingEnabled() {
        return groupScalingEnabled;
    }

    public void setGroupScalingEnabled(boolean groupScalingEnabled) {
        this.groupScalingEnabled = groupScalingEnabled;
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
    private GroupLevelNetworkPartitionContext getGroupLevelNetworkPartitionContext(String groupId,
                                                                                   String appId,
                                                                                   Instance parentInstanceContext) {
        GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext;
        ChildPolicy policy = PolicyManager.getInstance().
                getDeploymentPolicyByApplication(appId).
                getChildPolicy(groupId);

        String networkPartitionId = parentInstanceContext.getNetworkPartitionId();
        if (this.networkPartitionCtxts.containsKey(parentInstanceContext)) {
            groupLevelNetworkPartitionContext = this.networkPartitionCtxts.
                    get(networkPartitionId);
        } else {
            if (policy != null) {
                ChildLevelNetworkPartition networkPartition = policy.
                        getChildLevelNetworkPartition(parentInstanceContext.getNetworkPartitionId());
                groupLevelNetworkPartitionContext = new GroupLevelNetworkPartitionContext(
                        networkPartitionId,
                        networkPartition.getPartitionAlgo());
            } else {
                groupLevelNetworkPartitionContext = new GroupLevelNetworkPartitionContext(
                        networkPartitionId);
            }
            if (log.isInfoEnabled()) {
                log.info("[Network partition] " + networkPartitionId + "has been added for the " +
                        "[Group] " + this.id);
            }
            this.addNetworkPartitionContext(groupLevelNetworkPartitionContext);
        }
        return groupLevelNetworkPartitionContext;
    }

    /**
     * Finds the correct partition context to which the instance should be added to and
     * created and adds required context objects.
     *
     * @param parentInstanceContext   the parent instance context
     * @param networkPartitionContext the GroupLevelNetworkPartitionContext
     * @return the partition context
     */
    private void addPartitionContext(Instance parentInstanceContext,
                                     GroupLevelNetworkPartitionContext networkPartitionContext) {

        String networkPartitionId = parentInstanceContext.getNetworkPartitionId();
        List<GroupLevelPartitionContext> childPartitionContexts;

        ChildPolicy policy = PolicyManager.getInstance().
                getDeploymentPolicyByApplication(this.appId).
                getChildPolicy(this.id);


        PartitionContext partitionContext;
        String parentPartitionId = parentInstanceContext.getPartitionId();

        if (policy == null) {
            if (parentPartitionId != null &&
                    networkPartitionContext.getPartitionCtxt(parentPartitionId) == null) {
                partitionContext = new GroupLevelPartitionContext(parentPartitionId,
                                                networkPartitionId);
                networkPartitionContext.addPartitionContext((GroupLevelPartitionContext) partitionContext);
                if (log.isInfoEnabled()) {
                    log.info("[Partition] " + parentPartitionId + "has been added for the " +
                            "[Group] " + this.id);
                }
            }
        } else {
            ChildLevelNetworkPartition networkPartition = policy.
                    getChildLevelNetworkPartition(networkPartitionId);
            if (networkPartitionContext.getPartitionCtxts().isEmpty()) {
                // Create childPartitionContexts for all possibilities if startup
                ChildLevelPartition[] childLevelPartitions = networkPartition.getChildLevelPartitions();
                childPartitionContexts = new ArrayList<GroupLevelPartitionContext>();

                for (ChildLevelPartition childLevelPartition : childLevelPartitions) {
                    partitionContext = new GroupLevelPartitionContext(childLevelPartition.getMax(),
                            childLevelPartition.getPartitionId(), networkPartitionId);
                    childPartitionContexts.add((GroupLevelPartitionContext) partitionContext);
                    networkPartitionContext.addPartitionContext(
                            (GroupLevelPartitionContext) partitionContext);
                    if (log.isInfoEnabled()) {
                        log.info("[Partition] " + childLevelPartition.getPartitionId() +
                                "has been added for the [Group] " + this.id);
                    }
                }
            }
        }
    }

    /**
     * Creates the group instance and adds the required context objects
     *
     * @param group                             the group
     * @param parentInstanceContext             the parent instance context
     * @param partitionContext
     * @param groupLevelNetworkPartitionContext the group level network partition context
     */
    private String createGroupInstanceAndAddToMonitor(Group group, Instance parentInstanceContext,
                                                      PartitionContext partitionContext,
                                                      GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext,
                                                      GroupInstance groupInstance) {

        String partitionId;

        if (groupInstance == null) {
            partitionId = partitionContext.getPartitionId();

            groupInstance = createGroupInstance(group, parentInstanceContext.getNetworkPartitionId(),
                    parentInstanceContext.getInstanceId(), partitionId);
        }

        this.addInstance(groupInstance);

        String instanceId = groupInstance.getInstanceId();
        GroupInstanceContext groupInstanceContext = new GroupInstanceContext(instanceId);

        groupInstanceContext.addPartitionContext((GroupLevelPartitionContext) partitionContext);
        groupLevelNetworkPartitionContext.addInstanceContext(groupInstanceContext);

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
            GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext =
                    getGroupLevelNetworkPartitionContext(group.getUniqueIdentifier(),
                            this.appId, parentInstanceContext);
            //adding the partitionContext to the network partition context
            addPartitionContext(parentInstanceContext, groupLevelNetworkPartitionContext);

            String groupInstanceId;
            PartitionContext partitionContext;
            String parentPartitionId = parentInstanceContext.getPartitionId();

            // Create GroupInstance for partition instance and add to required contexts for minimum instance count
            int groupMin = group.getGroupMinInstances();

            //Have to check whether group has generated its own instances
            List<Instance> existingGroupInstances = group.getInstanceContextsWithParentId(parentInstanceId);
            for (Instance instance : existingGroupInstances) {
                initialStartup = false;
                partitionContext = groupLevelNetworkPartitionContext.
                        getPartitionContextById(instance.getPartitionId());
                groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                        partitionContext,
                        groupLevelNetworkPartitionContext,
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
                    List<GroupLevelPartitionContext> partitionContexts = groupLevelNetworkPartitionContext.
                            getPartitionCtxts();
                    GroupLevelPartitionContext[] groupLevelPartitionContexts =
                            new GroupLevelPartitionContext[partitionContexts.size()];
                    if (parentPartitionId == null) {
                        AutoscaleAlgorithm algorithm = this.getAutoscaleAlgorithm(
                                groupLevelNetworkPartitionContext.getPartitionAlgorithm());
                        partitionContext = algorithm.getNextScaleUpPartitionContext(
                                (partitionContexts.toArray(groupLevelPartitionContexts)));
                    } else {
                        partitionContext = groupLevelNetworkPartitionContext.
                                getPartitionContextById(parentPartitionId);
                    }
                    groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                            partitionContext,
                            groupLevelNetworkPartitionContext,
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
     * This will start the group instance based on the given parent instanceId
     * A new monitor is not created in this case
     *
     * @param parentInstanceId
     * @throws org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException
     */
    public void createInstanceAndStartDependencyOnScaleup(String parentInstanceId)
            throws MonitorNotFoundException {
        // Get parent instance context
        Instance parentInstanceContext = getParentInstanceContext(parentInstanceId);

        //TODO to get lock
        Group group = ApplicationHolder.getApplications().
                getApplication(this.appId).getGroupRecursively(this.id);

        // Get existing or create new GroupLevelNetworkPartitionContext
        GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext =
                getGroupLevelNetworkPartitionContext(group.getUniqueIdentifier(),
                        this.appId, parentInstanceContext);
        //adding the partitionContext to the network partition context
        addPartitionContext(parentInstanceContext, groupLevelNetworkPartitionContext);

        String groupInstanceId;
        PartitionContext partitionContext;
        String parentPartitionId = parentInstanceContext.getPartitionId();
        int groupMax = group.getGroupMaxInstances();
        if (group.getInstanceContextCount() < groupMax) {
            // Get partitionContext to create instance in
            if (parentPartitionId == null) {
                AutoscaleAlgorithm algorithm = this.getAutoscaleAlgorithm(
                        groupLevelNetworkPartitionContext.getPartitionAlgorithm());
                partitionContext = algorithm.getNextScaleUpPartitionContext(
                        (PartitionContext[]) groupLevelNetworkPartitionContext.
                                getPartitionCtxts().toArray());
            } else {
                partitionContext = groupLevelNetworkPartitionContext.
                        getPartitionContextById(parentPartitionId);
            }
            if (partitionContext != null) {
                groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                        partitionContext,
                        groupLevelNetworkPartitionContext,
                        null);
                startDependency(group, groupInstanceId);
            } else {
                log.warn("[Group] " + group.getUniqueIdentifier() + " has reached the maximum limit as " +
                        "[max] " + groupMax + ". Hence trying to notify the parent.");
            }
        } else {
            log.warn("[Group] " + group.getUniqueIdentifier() + " has reached the maximum limit as " +
                    "[max] " + groupMax + ". Hence trying to notify the parent.");
        }

    }


    /**
     * This will create the group instance in the applications Topology
     *
     * @param group
     * @param parentInstanceId
     * @param partitionId
     * @param networkPartitionId
     * @return
     */
    private GroupInstance createGroupInstance(Group group, String networkPartitionId,
                                              String parentInstanceId, String partitionId) {
        String instanceId = parentInstanceId;
        int minGroupInstances = group.getGroupMinInstances();
        int maxGroupInstances = group.getGroupMaxInstances();
        /*
        * When min != 1 or max != 1, we need to generate
        * instance ids as it is having more than one group instances
        */
        if (minGroupInstances > 1 || maxGroupInstances > 1) {
            instanceId = this.generateInstanceId(group);
        }
        return ApplicationBuilder.handleGroupInstanceCreatedEvent(appId, group.getUniqueIdentifier(),
                parentInstanceId, networkPartitionId, instanceId, partitionId);
    }

    public Map<String, GroupLevelNetworkPartitionContext> getNetworkPartitionCtxts() {
        return networkPartitionCtxts;
    }

    public void setNetworkPartitionCtxts(Map<String, GroupLevelNetworkPartitionContext> networkPartitionCtxts) {
        this.networkPartitionCtxts = networkPartitionCtxts;
    }

    public void addNetworkPartitionContext(GroupLevelNetworkPartitionContext clusterLevelNetworkPartitionContext) {
        this.networkPartitionCtxts.put(clusterLevelNetworkPartitionContext.getId(), clusterLevelNetworkPartitionContext);
    }

    public void setDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }

    public boolean verifyGroupStatus(String instanceId, GroupStatus requiredStatus) {
        if (this.instanceIdToInstanceMap.containsKey(instanceId)) {
            if (((GroupInstance) this.instanceIdToInstanceMap.get(instanceId)).getStatus() == requiredStatus) {
                return true;
            }
        } else {
            List<GroupInstance> instances = new ArrayList<GroupInstance>();
            String networkPartitionId = null;
            int noOfInstancesOfRequiredStatus = 0;
            for (Instance instance : this.instanceIdToInstanceMap.values()) {
                GroupInstance groupInstance = (GroupInstance) instance;
                if (groupInstance.getParentId().equals(instanceId)) {
                    instances.add(groupInstance);
                    networkPartitionId = groupInstance.getNetworkPartitionId();
                    if (groupInstance.getStatus() == requiredStatus) {
                        noOfInstancesOfRequiredStatus++;
                    }
                }
            }
            if (!instances.isEmpty()) {
                int minInstances = this.networkPartitionCtxts.get(networkPartitionId).
                        getMinInstanceCount();
                if (noOfInstancesOfRequiredStatus >= minInstances) {
                    return true;
                } else {
                    if (requiredStatus == GroupStatus.Inactive && noOfInstancesOfRequiredStatus >= 1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


}
