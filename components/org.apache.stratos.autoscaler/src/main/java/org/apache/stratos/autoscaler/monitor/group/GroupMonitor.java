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
package org.apache.stratos.autoscaler.monitor.group;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ParentComponentLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupChildContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.EventHandler;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorStatusEventBuilder;
import org.apache.stratos.autoscaler.monitor.ParentComponentMonitor;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.events.*;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.messaging.domain.applications.*;
import org.apache.stratos.messaging.domain.instance.context.GroupInstanceContext;
import org.apache.stratos.messaging.domain.instance.context.InstanceContext;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.ArrayList;
import java.util.List;

/**
 * This is GroupMonitor to monitor the group which consists of
 * groups and clusters
 */
public class GroupMonitor extends ParentComponentMonitor implements EventHandler {
    private static final Log log = LogFactory.getLog(GroupMonitor.class);
    //whether groupScaling enabled or not
    private boolean groupScalingEnabled;

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
        //starting the minimum start able dependencies
        //startMinimumDependencies(group, parentInstanceId);
    }

    /**
     * Will set the status of the monitor based on Topology Group status/child status like scaling
     *
     * @param status status of the group
     */
    public void setStatus(GroupStatus status, String instanceId) {

        if (status == GroupStatus.Inactive && !this.hasStartupDependents) {
            log.info("[Group] " + this.id + "is not notifying the parent, " +
                    "since it is identified as the independent unit");
        } else {
            // notify parent
            log.info("[Group] " + this.id + "is notifying the [parent] " + this.parent.getId());
            if(this.isGroupScalingEnabled()) {
                ApplicationHolder.acquireReadLock();
                try {
                    Application application = ApplicationHolder.getApplications().
                                                                    getApplication(this.appId);
                    if(application != null) {
                        //Notifying the parent using parent's instance Id,
                        // as it has group scaling enabled.
                        Group group = application.getGroupRecursively(this.id);
                        if(group != null) {
                            GroupInstanceContext context = group.getInstanceContexts(instanceId);
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
        MonitorStatusEventBuilder.notifyChildren(this, new GroupStatusEvent(status, getId(), null));
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
            this.markMonitorAsInactive(id);
            onChildInactiveEvent(id, instanceId);

        } else if (status1 == ClusterStatus.Created || status1 == GroupStatus.Created) {
            if (this.terminatingMonitorsList.contains(id)) {
                this.terminatingMonitorsList.remove(id);
                this.aliasToActiveMonitorsMap.remove(id);
                if(AutoscalerContext.getInstance().getClusterMonitors().containsKey(id)) {
                    AutoscalerContext.getInstance().removeClusterMonitor(id);
                }
            }
            //If cluster monitor, need to terminate the existing one
            //TODO block
            /*if (this.status == GroupStatus.Terminating) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
            } else {
                onChildTerminatedEvent(id);
            }*/

        } else if (status1 == ClusterStatus.Terminating || status1 == GroupStatus.Terminating) {
            //mark the child monitor as inActive in the map
            this.markMonitorAsTerminating(id);

        } else if (status1 == ClusterStatus.Terminated || status1 == GroupStatus.Terminated) {
            //Check whether all dependent goes Terminated and then start them in parallel.
            if (this.terminatingMonitorsList.contains(id)) {
                this.terminatingMonitorsList.remove(id);
                this.aliasToActiveMonitorsMap.remove(id);
            } else {
                log.warn("[monitor] " + id + " cannot be found in the inActive monitors list");
            }
            //TODO block
            /*if (this.status == GroupStatus.Terminating || this.status == GroupStatus.Terminated) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
                log.info("Executing the un-subscription request for the [monitor] " + id);
            }*/
        }
    }

    @Override
    public void onParentStatusEvent(MonitorStatusEvent statusEvent) {
        String instanceId = statusEvent.getInstanceId();
        // send the ClusterTerminating event
        if (statusEvent.getStatus() == GroupStatus.Terminating ||
                statusEvent.getStatus() == ApplicationStatus.Terminating) {
            ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId);
        } else if(statusEvent.getStatus() == ClusterStatus.Created ||
                                            statusEvent.getStatus() == GroupStatus.Created) {
            Application application = ApplicationHolder.getApplications().getApplication(this.appId);
            Group group = application.getGroupRecursively(statusEvent.getId());
            //starting a new instance of this monitor
            createGroupInstance(group, statusEvent.getInstanceId());
        }
    }

    @Override
    public void onChildScalingEvent(MonitorScalingEvent scalingEvent) {

        if(hasGroupScalingDependent){

            //notify parent
            parent.onChildScalingEvent(scalingEvent);
        }

        if(log.isDebugEnabled()){
            log.debug("Child scaling event received to [group]: " + this.getId()
                    + ", [network partition]: " + scalingEvent.getNetworkPartitionId()
                    + ", [event] " + scalingEvent.getId() + ", [group instance] " + scalingEvent.getInstanceId());
        }

        //find the child context of this group, from scaling dependency tree
        GroupChildContext currentChildContextInScalingTree =
                (GroupChildContext) scalingDependencyTree.findApplicationContextWithIdInScalingDependencyTree(id);

        //Notifying children, if this group has scaling dependencies
        if (currentChildContextInScalingTree.isGroupScalingEnabled()){
            for (ApplicationChildContext applicationChildContext : currentChildContextInScalingTree.getApplicationChildContextList()){

                //Get group monitor so that it can notify it's children
                Monitor monitor = aliasToActiveMonitorsMap.get(applicationChildContext.getId());

                if(monitor instanceof GroupMonitor || monitor instanceof ApplicationMonitor){

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

    public void startMinimumDependencies(Group group, List<String> parentInstanceIds)
            throws TopologyInConsistentException {
        int min = group.getGroupMinInstances();
        if(group.getInstanceContextCount() >= min) {
            startDependency(group);
        } else {
            if(group.getInstanceContextCount() > 0) {
                List<String> instanceIds = new ArrayList<String>();
                for(String parentInstanceId : parentInstanceIds) {
                    List<InstanceContext> contexts1 =  group.getInstanceContextsWithParentId(parentInstanceId);
                    //Finding the non startable instance ids
                    if(group.getInstanceContexts(parentInstanceId) == null || contexts1.isEmpty() ||
                            contexts1.size() == 0) {
                        instanceIds.add(parentInstanceId);

                    }
                }
                if(instanceIds.size() > 0) {
                    createInstanceAndStartDependency(group, parentInstanceIds);
                } else {
                    startDependency(group);
                }
            } else {
                //No available instances in the Applications. Need to start them all
                createInstanceAndStartDependency(group, parentInstanceIds);
            }
        }
    }

    private void createInstanceAndStartDependency(Group group, List<String> parentInstanceIds)
            throws TopologyInConsistentException {
        List<String> instanceIds = new ArrayList<String>();
        String deploymentPolicyName = group.getDeploymentPolicy();

        String instanceId;
        for(String parentInstanceId : parentInstanceIds) {
            InstanceContext parentInstanceContext = this.parent.getInstanceContext(parentInstanceId);
            ParentComponentLevelNetworkPartitionContext clusterLevelNetworkPartitionContext;
            if(this.networkPartitionCtxts.containsKey(parentInstanceContext)) {
                clusterLevelNetworkPartitionContext = this.networkPartitionCtxts.
                                            get(parentInstanceContext.getNetworkPartitionId());
            } else {
                clusterLevelNetworkPartitionContext = new ParentComponentLevelNetworkPartitionContext(
                                                        parentInstanceContext.getNetworkPartitionId(),
                                                        null, null);
                this.addNetworkPartitionContext(clusterLevelNetworkPartitionContext);
            }

            if(deploymentPolicyName != null) {
                DeploymentPolicy deploymentPolicy = PolicyManager.getInstance()
                        .getDeploymentPolicy(deploymentPolicyName);
                PartitionGroup partitionGroup = deploymentPolicy.
                        getPartitionGroup(parentInstanceContext.getNetworkPartitionId());

                AutoscaleAlgorithm algorithm = this.getAutoscaleAlgorithm(partitionGroup.getPartitionAlgo());
                Partition partition = algorithm.getNextScaleUpPartition(clusterLevelNetworkPartitionContext, this.id);
            }
            instanceId = createGroupInstance(group, parentInstanceId);
            instanceIds.add(instanceId);



        }
        startDependency(group, instanceIds);
    }

    private String createGroupInstance(Group group, String parentInstanceId) {
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
        ApplicationBuilder.handleGroupInstanceCreatedEvent(appId, group.getUniqueIdentifier(),
                                                            instanceId, parentInstanceId);
        return instanceId;
    }
}
