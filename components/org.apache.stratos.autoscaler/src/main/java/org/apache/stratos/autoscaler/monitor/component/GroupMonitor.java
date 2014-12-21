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
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.group.GroupInstanceContext;
import org.apache.stratos.autoscaler.context.partition.GroupLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.PartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.GroupLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.NetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.events.GroupStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingOverMaxEvent;
import org.apache.stratos.autoscaler.monitor.events.builder.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ChildPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelNetworkPartition;
import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ChildLevelPartition;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.domain.applications.*;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends ParentComponentMonitor {

    private static final Log log = LogFactory.getLog(GroupMonitor.class);
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
        this.groupScalingEnabled = group.isGroupScalingEnabled();
        this.appId = appId;
        this.hasScalingDependents = hasScalingDependents;
    }

    @Override
    public void run() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Group monitor is running : " + this.toString());
            }
            monitor();
        } catch (Exception e) {
            log.error("Group monitor failed : " + this.toString(), e);
        }
    }

    public synchronized void monitor() {
        final Collection<NetworkPartitionContext> networkPartitionContexts =
                this.networkPartitionCtxts.values();

        Runnable monitoringRunnable = new Runnable() {
            @Override
            public void run() {
                if (log.isDebugEnabled()) {
                    log.debug("Group monitor is running====== : " + this.toString());
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
                                if(!hasScalingDependents) {
                                    //handling the group scaling and if pending instances found,
                                    // reject the max
                                    if (groupScalingEnabled) {
                                        if(networkPartitionContext.getPendingInstancesCount() == 0) {
                                            //one of the child is loaded and max out.
                                            // Hence creating new group instance
                                            if(log.isDebugEnabled()) {
                                                log.debug("Handling group scaling for the [group] " + id +
                                                        "upon a max out event from " +
                                                        "the children");
                                            }
                                            boolean createOnDemand = createInstanceOnDemand(instanceContext.
                                                    getParentInstanceId());
                                            if (!createOnDemand) {
                                                //couldn't create new instance. Hence notifying the parent
                                                MonitorStatusEventBuilder.handleScalingOverMaxEvent(parent,
                                                        networkPartitionContext.getId(),
                                                        instanceContext.getParentInstanceId(),
                                                        appId);
                                            }
                                        } else {
                                            if(log.isDebugEnabled()) {
                                                log.debug("Pending Group instance found. " +
                                                        "Hence waiting for it to become active");
                                            }
                                        }

                                    } else {
                                        //notifying the parent if no group scaling enabled here
                                        MonitorStatusEventBuilder.handleScalingOverMaxEvent(parent,
                                                networkPartitionContext.getId(),
                                                instanceContext.getParentInstanceId(),
                                                appId);
                                    }
                                } else {
                                    //has scaling dependents. Should notify the parent
                                    if(log.isDebugEnabled()) {
                                        log.debug("This [Group] " + id + " [scale-up] dependencies. " +
                                                "Hence notifying the [parent] " + parent.getId() );
                                    }
                                    //notifying the parent when scale dependents found
                                    MonitorStatusEventBuilder.handleScalingOverMaxEvent(parent,
                                            networkPartitionContext.getId(),
                                            instanceContext.getParentInstanceId(),
                                            appId);
                                }
                                //Resetting the max events
                                instanceContext.setIdToScalingOverMaxEvent(
                                        new HashMap<String, ScalingOverMaxEvent>());
                            } else {
                                handleDependentScaling(instanceContext, networkPartitionContext);
                            }
                        }


                    }


                }
            }
        };
        monitoringRunnable.run();
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

        if (this.isGroupScalingEnabled()) {
            try {
                ApplicationHolder.acquireReadLock();
                Application application = ApplicationHolder.getApplications().
                        getApplication(this.appId);
                if (application != null) {
                    //Notifying the parent using parent's instance Id,
                    // as it has group scaling enabled.
                    Group group = application.getGroupRecursively(this.id);
                    if (group != null) {
                        // notify parent
                        log.info("[Group] " + this.id + "is notifying the [parent] " + this.parent.getId() +
                                " [instance] " + parentInstanceId);
                        MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent,
                                status, this.id, parentInstanceId);
                    }
                }
            } finally {
                ApplicationHolder.releaseReadLock();
            }
        } else {
            // notifying the parent
            log.info("[Group] " + this.id + "is notifying the [parent] " + this.parent.getId() +
                    " [instance] " + instanceId);
            MonitorStatusEventBuilder.handleGroupStatusEvent(this.parent,
                    status, this.id, instanceId);
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
            //TODO handling restart of stratos when group is inactive
            markInstanceAsInactive(childId, instanceId);
            onChildInactiveEvent(childId, instanceId);

        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inactive in the map
            markInstanceAsTerminating(childId, instanceId);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Verifying whether all the minimum no of instances of child
            // became active to take next action
            if (status1 == GroupStatus.Terminated) {
                /*boolean childTerminated = verifyGroupStatus(instanceId, (GroupStatus) status1);
                if (childTerminated) {*/
                onTerminationOfInstance(childId, instanceId);
                /*} else {
                    log.info("Waiting for other group instances to be terminated");
                }*/
            } else {
                onTerminationOfInstance(childId, instanceId);
            }
        }
    }

    private void onTerminationOfInstance(String childId, String instanceId) {
        //Check whether all dependent goes Terminated and then start them in parallel.
        removeInstanceFromFromInactiveMap(childId, instanceId);
        removeInstanceFromFromTerminatingMap(childId, instanceId);

        GroupInstance instance = (GroupInstance) instanceIdToInstanceMap.get(instanceId);
        if (instance != null) {
            if (instance.getStatus() == GroupStatus.Terminating ||
                    instance.getStatus() == GroupStatus.Terminated) {
                ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().process(id,
                        appId, instanceId);
            } else {
                onChildTerminatedEvent(childId, instanceId);
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
                    log.info("Publishing Cluster terminating event for [application] " + appId +
                            " [group] " + id + " [instance] " + instanceId);
                }
                ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId);
            } else {
                //Using parentId need to get the all the children and ask them to terminate
                // as they can't exist without the parent
                List<String> instanceIds = this.getInstancesByParentInstanceId(instanceId);
                if (!instanceIds.isEmpty()) {
                    for (String instanceId1 : instanceIds) {
                        if (log.isInfoEnabled()) {
                            log.info("Publishing Cluster terminating event for [application] " + appId +
                                    " [group] " + id + " [instance] " + instanceId1);
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
        //TODO group scaling, if there are enough spaces else notify the parent with max out
        //Notifying children, if this group has scaling dependencies
        if (scalingDependencies != null && !scalingDependencies.isEmpty()) {
            for (ScalingDependentList scalingDependentList : scalingDependencies) {
                if (scalingDependentList.getScalingDependentListComponents().
                        contains(scalingEvent.getId())) {
                    for (String scalingDependentListComponent : scalingDependentList
                            .getScalingDependentListComponents()) {
                        Monitor monitor = aliasToActiveMonitorsMap.get(
                                scalingDependentListComponent);
                        if (monitor instanceof GroupMonitor ||
                                monitor instanceof ClusterMonitor) {
                            ScalingEvent childScalingEvent = new ScalingEvent(monitor.getId(),
                                    monitor.getId(),
                                    scalingEvent.getInstanceId(),
                                    scalingEvent.getFactor());
                            monitor.onParentScalingEvent(childScalingEvent);
                        }
                    }
                }
                break;
            }
        }
    }

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
    private GroupLevelNetworkPartitionContext getGroupLevelNetworkPartitionContext(String groupId,
                                                                                   String appId,
                                                                                   Instance parentInstanceContext) {
        GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext;
        ChildPolicy policy = PolicyManager.getInstance().
                getDeploymentPolicyByApplication(appId).
                getChildPolicy(groupId);

        String networkPartitionId = parentInstanceContext.getNetworkPartitionId();
        if (this.networkPartitionCtxts.containsKey(networkPartitionId)) {
            groupLevelNetworkPartitionContext = (GroupLevelNetworkPartitionContext) this.networkPartitionCtxts.
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
                    if (networkPartitionContext.
                            getPartitionCtxt(childLevelPartition.getPartitionId()) == null) {
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
        groupLevelNetworkPartitionContext.addInstanceContext(groupInstanceContext);
        groupLevelNetworkPartitionContext.addPendingInstance(groupInstanceContext);

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
            //Setting the networkpartition minimum instances as group min instances
            groupLevelNetworkPartitionContext.setMinInstanceCount(groupMin);

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
                    partitionContext = getPartitionContext(groupLevelNetworkPartitionContext,
                            parentPartitionId);
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

    private PartitionContext getPartitionContext(
            GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext,
            String parentPartitionId) {
        PartitionContext partitionContext = null;
        // Get partitionContext to create instance in
        List<GroupLevelPartitionContext> partitionContexts = groupLevelNetworkPartitionContext.
                getPartitionCtxts();
        GroupLevelPartitionContext[] groupLevelPartitionContexts =
                new GroupLevelPartitionContext[partitionContexts.size()];
        if (parentPartitionId == null) {
            if (!partitionContexts.isEmpty()) {
                AutoscaleAlgorithm algorithm = this.getAutoscaleAlgorithm(
                        groupLevelNetworkPartitionContext.getPartitionAlgorithm());
                partitionContext = algorithm.getNextScaleUpPartitionContext(
                        (partitionContexts.toArray(groupLevelPartitionContexts)));
            }
        } else {
            partitionContext = groupLevelNetworkPartitionContext.
                    getPartitionContextById(parentPartitionId);
        }
        return partitionContext;
    }


    /**
     * This will start the group instance based on the given parent instanceId
     * A new monitor is not created in this case
     *
     * @param parentInstanceId
     * @throws org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException
     */
    public boolean createInstanceOnDemand(String parentInstanceId) {
        // Get parent instance context
        Instance parentInstanceContext = getParentInstanceContext(parentInstanceId);
        List<String> instanceIdsToStart = new ArrayList<String>();


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
        int groupMin = group.getGroupMinInstances();
        List<Instance> instances = group.getInstanceContextsWithParentId(parentInstanceId);
        if (instances.isEmpty()) {
            //Need to create totally new group instance
            for (int i = 0; i < groupMin; i++) {
                // Get partitionContext to create instance in
                partitionContext = getPartitionContext(groupLevelNetworkPartitionContext,
                        parentPartitionId);
                groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                        partitionContext,
                        groupLevelNetworkPartitionContext,
                        null);
                instanceIdsToStart.add(groupInstanceId);
            }
        } else {
            //have to create one more instance
            if (group.getInstanceContextCount() < groupMax) {
                // Get partitionContext to create instance in
                partitionContext = getPartitionContext(groupLevelNetworkPartitionContext,
                        parentPartitionId);
                if (partitionContext != null) {
                    groupInstanceId = createGroupInstanceAndAddToMonitor(group, parentInstanceContext,
                            partitionContext,
                            groupLevelNetworkPartitionContext,
                            null);
                    instanceIdsToStart.add(groupInstanceId);
                } else {
                    log.warn("[Group] " + group.getUniqueIdentifier() + " has reached the maximum limit as " +
                            "[max] " + groupMax + ". Hence trying to notify the parent.");
                }
            } else {
                log.warn("[Group] " + group.getUniqueIdentifier() + " has reached the maximum limit as " +
                        "[max] " + groupMax + ". Hence trying to notify the parent.");
            }
        }
        boolean startedOnDemand = false;
        if (!instanceIdsToStart.isEmpty()) {
            startedOnDemand = true;
        }
        //TODO Starting all the instances, can do in parallel
        for (String instanceId : instanceIdsToStart) {
            try {
                startDependency(group, instanceId);
            } catch (MonitorNotFoundException e) {
                //TODO exception handling
                log.error("Error while creating the group/cluster instance", e);
            }
        }
        return startedOnDemand;
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

        return ApplicationBuilder.handleGroupInstanceCreatedEvent(appId, group.getUniqueIdentifier(),
                parentInstanceId, networkPartitionId, partitionId);
    }

    public void addNetworkPartitionContext(GroupLevelNetworkPartitionContext clusterLevelNetworkPartitionContext) {
        this.networkPartitionCtxts.put(clusterLevelNetworkPartitionContext.getId(), clusterLevelNetworkPartitionContext);
    }


    public boolean verifyGroupStatus(String childId, String instanceId, GroupStatus requiredStatus) {
        Monitor monitor = this.getMonitor(childId);
        List<String> groupInstances;
        GroupInstance groupInstance = (GroupInstance) monitor.getInstance(instanceId);
        if (groupInstance == null) {
            groupInstances = monitor.getInstancesByParentInstanceId(instanceId);
        } else {
            if (groupInstance.getStatus() == requiredStatus) {
                return true;
            } else {
                return false;
            }
        }

        String networkPartitionId = null;
        int noOfInstancesOfRequiredStatus = 0;
        for (String childInstanceId : groupInstances) {
            GroupInstance childGroupInstance = (GroupInstance) monitor.getInstance(childInstanceId);
            networkPartitionId = childGroupInstance.getNetworkPartitionId();
            if (childGroupInstance.getStatus() == requiredStatus) {
                noOfInstancesOfRequiredStatus++;
            }
        }

        if (!groupInstances.isEmpty()) {
            GroupLevelNetworkPartitionContext networkPartitionContext =
                    (GroupLevelNetworkPartitionContext) this.networkPartitionCtxts.
                            get(networkPartitionId);
            int minInstances = networkPartitionContext.getMinInstanceCount();
            //if terminated all the instances in this instances map should be in terminated state
            //if terminated all the instances in this instances map should be in terminated state
            if (noOfInstancesOfRequiredStatus == this.inactiveInstancesMap.size() &&
                    requiredStatus == GroupStatus.Terminated) {
                return true;
            } else if (noOfInstancesOfRequiredStatus >= minInstances) {
                return true;
            } else {
                //of only one is inActive implies that the whole group is Inactive
                if (requiredStatus == GroupStatus.Inactive && noOfInstancesOfRequiredStatus >= 1) {
                    return true;
                }
            }

        }
        return false;
    }


    @Override
    public void destroy() {
        stopScheduler();
    }

}
