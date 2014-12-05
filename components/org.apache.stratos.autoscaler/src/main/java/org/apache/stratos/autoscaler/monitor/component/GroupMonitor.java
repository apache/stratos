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
    //Group level partition contexts
    private List<GroupLevelPartitionContext> partitionContexts;
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
        ((GroupInstance)this.instanceIdToInstanceMap.get(instanceId)).setStatus(status);

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
            this.markMonitorAsInactive(instanceId);
            onChildInactiveEvent(id, instanceId);

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
            GroupInstance instance = (GroupInstance)this.instanceIdToInstanceMap.get(instanceId);
            if (instance != null) {
                if(instance.getStatus() == GroupStatus.Terminating) {
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
            if(instance != null) {
                ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId);
            } else {
                //Using parentId need to get the children
                List<String> instanceIds = this.getInstancesByParentInstanceId(instanceId);
                if(!instanceIds.isEmpty()) {
                    for(String instanceId1 : instanceIds) {
                        ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId1);
                    }
                }

            }
        } else if (statusEvent.getStatus() == ClusterStatus.Created ||
                statusEvent.getStatus() == GroupStatus.Created) {
            Application application = ApplicationHolder.getApplications().getApplication(this.appId);
            Group group = application.getGroupRecursively(statusEvent.getId());
            //starting a new instance of this monitor
            createInstanceAndStartDependencyOnScaleup(group, statusEvent.getInstanceId());
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
     * This will start the minimum required dependency instances
     * based on the given parent instance ids
     *
     * @param group             blue print of the instance to be started
     * @param parentInstanceIds parent instanceIds used to start the child instance
     * @return whether first app startup or not
     * @throws TopologyInConsistentException
     */
    public boolean startMinimumDependencies(Group group, List<String> parentInstanceIds)
            throws TopologyInConsistentException {
        boolean initialStartup = false;
        int min = group.getGroupMinInstances();
        if (group.getInstanceContextCount() >= min) {
            startDependency(group);
        } else {
            if (group.getInstanceContextCount() > 0) {
                List<String> instanceIds = new ArrayList<String>();
                for (String parentInstanceId : parentInstanceIds) {
                    List<Instance> contexts1 = group.getInstanceContextsWithParentId(parentInstanceId);
                    //Finding the non startable instance ids
                    if (group.getInstanceContexts(parentInstanceId) == null || contexts1.isEmpty() ||
                            contexts1.size() == 0) {
                        instanceIds.add(parentInstanceId);

                    }
                }
                if (instanceIds.size() > 0) {
                    //createInstanceAndStartDependency(group, instanceIds);
                } else {
                    startDependency(group);
                }
            } else {
                //No available instances in the Applications. Need to start them all
                createInstanceAndStartDependencyAtStartup(group, parentInstanceIds);
                initialStartup = true;
            }
        }
        return initialStartup;
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
        if (this.id.equals(appId)) {
            parentInstanceContext = application.getInstanceContexts(parentInstanceId);
        } else {
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
    private GroupLevelNetworkPartitionContext getGroupLevelNetworkPartitionContext(Instance parentInstanceContext) {
    	GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext;
        if (this.networkPartitionCtxts.containsKey(parentInstanceContext)) {
            groupLevelNetworkPartitionContext = this.networkPartitionCtxts.
                    get(parentInstanceContext.getNetworkPartitionId());
        } else {
            groupLevelNetworkPartitionContext = new GroupLevelNetworkPartitionContext(
                    parentInstanceContext.getNetworkPartitionId(),
                    null, null);
            this.addNetworkPartitionContext(groupLevelNetworkPartitionContext);
        }
        return groupLevelNetworkPartitionContext;
    }
    
    /**
     * Finds the correct partition context to which the instance should be added to and 
     * created and adds required context objects.
     *
     * @param parentInstanceContext the parent instance context
     * @param group the group
     * @return the partition context
     */
    private String FindAndAddPartitionContext(Instance parentInstanceContext, Group group, boolean startup) {
    	PartitionContext partitionContext = null;
    	
    	String networkPartitionId = parentInstanceContext.getNetworkPartitionId();
        List<GroupLevelPartitionContext> childParitionContexts = null;
    
        ChildPolicy policy = PolicyManager.getInstance().
                getDeploymentPolicyByApplication(group.getApplicationId()).
                getChildPolicy(group.getUniqueIdentifier());

        ChildLevelNetworkPartition networkPartition = policy.
                getChildLevelNetworkPartition(networkPartitionId);
        
        if (startup) {  
            // Create childPartitionContexts for all possibilities if startup
            ChildLevelPartition[] childLevelPartitions = networkPartition.getChildLevelPartitions();
            childParitionContexts = new ArrayList<GroupLevelPartitionContext>();
            for (ChildLevelPartition childLevelPartition : childLevelPartitions) {
            	partitionContext = new GroupLevelPartitionContext(childLevelPartition.getMax(), childLevelPartition.getPartitionId(), networkPartitionId); 
            	childParitionContexts.add((GroupLevelPartitionContext) partitionContext);
            	this.addPartitionContext((GroupLevelPartitionContext)partitionContext);
            }
        } else {
        	// Get partition contexts already created
        	childParitionContexts = this.getPartitionCtxts();
        }
        
        // Get partitionContext to create instance in
        AutoscaleAlgorithm algorithm = this.getAutoscaleAlgorithm(networkPartition.getPartitionAlgo());
        partitionContext = algorithm.getNextScaleUpPartitionContext((PartitionContext[]) childParitionContexts.toArray());
        
    	return partitionContext.getPartitionId();
    }

    /**
     * Creates the group instance and adds the required context objects
     *
     * @param group the group
     * @param parentInstanceContext the parent instance context
     * @param partitionContext the partition context
     * @param groupLevelNetworkPartitionContext the group level network partition context
     * @param instanceIdstoStart the container with instance ids to start
     */
    private String createGroupInstance(Group group, Instance parentInstanceContext, String partitionId, 
    		GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext, String deploymentPolicyName, boolean startup)
    {
    	GroupInstance groupInstance = createGroupInstance(group, parentInstanceContext.getInstanceId(), partitionId, parentInstanceContext.getNetworkPartitionId());
        this.addInstance(groupInstance);
        
        String instanceId = groupInstance.getInstanceId();
        GroupInstanceContext groupInstanceContext = new GroupInstanceContext(instanceId);
        PartitionContext partitionContext = this.getPartitionCtxt(partitionId);
        
        if (deploymentPolicyName != null && partitionContext != null && startup) {
        	groupInstanceContext.addPartitionContext((GroupLevelPartitionContext)partitionContext);
        }
        groupLevelNetworkPartitionContext.addInstanceContext(groupInstanceContext);
        
        if (partitionContext != null) {
        	((GroupLevelPartitionContext)partitionContext).addActiveInstance(groupInstance);
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
    public void createInstanceAndStartDependencyAtStartup(Group group, List<String> parentInstanceIds)
            throws TopologyInConsistentException {
        List<String> instanceIdstoStart = new ArrayList<String>();
         
        for (String parentInstanceId : parentInstanceIds) {
            // Get parent instance context
            Instance parentInstanceContext = getParentInstanceContext(parentInstanceId);
            
            // Get existing or create new GroupLevelNetwokPartitionContext
            GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext = getGroupLevelNetworkPartitionContext(parentInstanceContext);
            
            // Determine partitionContext
            String deploymentPolicyName = group.getDeploymentPolicy();
            String partitionId = null;
            if(deploymentPolicyName != null) {
            	partitionId = FindAndAddPartitionContext(parentInstanceContext, group, true);
            }
            else { 
            	GroupInstance instance = (GroupInstance) this.parent.getInstance(parentInstanceId);
            	if (instance != null) {
            		partitionId = instance.getPartitionId();
            	}
            }
                        
            String groupInstanceId;
            // Create GroupInstance for partition instance and add to required contexts for minimum instance count
            for(int i=0; i<groupLevelNetworkPartitionContext.getMinInstanceCount(); i++) {
	            
            	groupInstanceId = createGroupInstance(group, parentInstanceContext, partitionId, groupLevelNetworkPartitionContext, deploymentPolicyName, true);
            	instanceIdstoStart.add(groupInstanceId);
            }
        }
        startDependency(group, instanceIdstoStart);
    }
    
    /**
     * This will start the group instance based on the given parent instanceId
     * A new monitor is not created in this case
     *
     * @param group
     * @param parentInstanceId
     * @throws org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException
     */
    public void createInstanceAndStartDependencyOnScaleup(Group group, String parentInstanceId)
            throws MonitorNotFoundException {
    	// Get parent instance context
        Instance parentInstanceContext = getParentInstanceContext(parentInstanceId);
        
        // Get existing or create new GroupLevelNetwokPartitionContext
        GroupLevelNetworkPartitionContext groupLevelNetworkPartitionContext = getGroupLevelNetworkPartitionContext(parentInstanceContext);
        
        // Determine partitionContext
        String deploymentPolicyName = group.getDeploymentPolicy();
        String partitionId = null;
        if(deploymentPolicyName != null) {
        	partitionId = FindAndAddPartitionContext(parentInstanceContext, group, false);
        }
        else {
        	GroupInstance instance = (GroupInstance) this.parent.getInstance(parentInstanceId);
        	if (instance != null) {
        		partitionId = instance.getPartitionId();
        	} 
        }

        String groupInstanceId = createGroupInstance(group, parentInstanceContext, partitionId, groupLevelNetworkPartitionContext, deploymentPolicyName, false);
        startDependency(group, groupInstanceId);
    }
    
    public void createInstanceAndStartDependencyOnRestart(Group group, List<String> parentInstanceIds) {
    	// TODO: Need to add functionality when restart happens
    	// Should only do required work from Monitor side and not from Topology since that is already existent
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
    private GroupInstance createGroupInstance(Group group, String parentInstanceId, String partitionId,
                                       String networkPartitionId) {
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
                instanceId, parentInstanceId,
                networkPartitionId, partitionId);
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

    public List<GroupLevelPartitionContext> getPartitionCtxts() {

        return partitionContexts;
    }

    public GroupLevelPartitionContext getPartitionCtxt(String partitionId) {
        
    	for(GroupLevelPartitionContext partitionContext : partitionContexts){
            if(partitionContext.getPartitionId().equals(partitionId)){
                return partitionContext;
            }
        }
        return null;
    }

    public void addPartitionContext(GroupLevelPartitionContext partitionContext) {
    	partitionContexts.add(partitionContext);
    }

    public int getNonTerminatedMemberCountOfPartition(String partitionId) {

        for(GroupLevelPartitionContext partitionContext : partitionContexts){
            if(partitionContext.getPartitionId().equals(partitionId)){
                return partitionContext.getNonTerminatedInstanceCount();
            }
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {

        for(GroupLevelPartitionContext partitionContext : partitionContexts){
            if(partitionContext.getPartitionId().equals(currentPartitionId)){
                return partitionContext.getActiveInstanceCount();
            }
        }
        return 0;
    }
}
