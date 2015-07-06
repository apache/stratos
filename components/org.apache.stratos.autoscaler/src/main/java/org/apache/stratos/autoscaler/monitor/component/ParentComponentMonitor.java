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
import org.apache.stratos.autoscaler.algorithms.partition.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithms.partition.RoundRobin;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyBuilder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.network.ParentLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.NetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.events.ScalingDownBeyondMinEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingEvent;
import org.apache.stratos.autoscaler.monitor.events.ScalingUpBeyondMaxEvent;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.application.GroupStatus;
import org.apache.stratos.messaging.domain.application.ParentComponent;
import org.apache.stratos.messaging.domain.application.ScalingDependentList;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.*;
import java.util.concurrent.*;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class ParentComponentMonitor extends Monitor {

    private static final Log log = LogFactory.getLog(ParentComponentMonitor.class);

    //Scheduler executor service to execute this monitor in a thread
    private final ScheduledExecutorService scheduler = StratosThreadPool.getScheduledExecutorService(
            "autoscaler.monitor.scheduler.thread.pool", 100);
    // future to cancel it when destroying monitors
    private ScheduledFuture<?> schedulerFuture;
    //The monitors dependency tree with all the start-able/kill-able dependencies
    protected DependencyTree startupDependencyTree;
    //The monitors dependency tree with all the scaling dependencies
    protected Set<ScalingDependentList> scalingDependencies;
    //monitors map, key=GroupAlias/clusterId and value=GroupMonitor/AbstractClusterMonitor
    protected Map<String, Monitor> aliasToActiveChildMonitorsMap;
    //Pending monitors list
    protected List<String> pendingChildMonitorsList;
    //instanceIds map, key=alias, value instanceIds stopped monitors
    protected Map<String, List<String>> inactiveInstancesMap;
    //terminating map, key=alias, value instanceIds
    protected Map<String, List<String>> terminatingInstancesMap;
    //network partition contexts
    protected Map<String, NetworkPartitionContext> networkPartitionContextsMap;
    //Executor service to maintain the thread pool
    private ExecutorService executorService;

    public ParentComponentMonitor(ParentComponent component) throws DependencyBuilderException {
        aliasToActiveChildMonitorsMap = new ConcurrentHashMap<String, Monitor>();
        inactiveInstancesMap = new ConcurrentHashMap<String, List<String>>();
        terminatingInstancesMap = new ConcurrentHashMap<String, List<String>>();
        pendingChildMonitorsList = new ArrayList<String>();
        id = component.getUniqueIdentifier();

        // Building the startup dependencies for this monitor within the immediate children
        startupDependencyTree = DependencyBuilder.getInstance().buildDependency(component);

        // Building the scaling dependencies for this monitor within the immediate children
        if ((component.getDependencyOrder() != null) &&
                (component.getDependencyOrder().getScalingDependents() != null)) {
            scalingDependencies = DependencyBuilder.getInstance().buildScalingDependencies(component);
        } else {
            // No scaling dependencies found, initialize to an empty set
            scalingDependencies = new HashSet<ScalingDependentList>();
        }

        // Create the executor service with identifier and thread pool size
        executorService = StratosThreadPool.getExecutorService(AutoscalerConstants.AUTOSCALER_THREAD_POOL_ID,
                AutoscalerConstants.AUTOSCALER_THREAD_POOL_SIZE);
        networkPartitionContextsMap = new ConcurrentHashMap<String, NetworkPartitionContext>();
    }

    /**
     * This will create Instance on demand as requested by monitors
     *
     * @param instanceId instance Id of the instance to be created
     * @return whether it is created or not
     */
    public abstract boolean createInstanceOnTermination(String instanceId);

    /**
     * Starting the scheduler for the monitor
     */
    public void startScheduler() {
        int monitoringIntervalMilliseconds = 60000;
        schedulerFuture = scheduler.scheduleAtFixedRate(this, 0,
                monitoringIntervalMilliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * This will stop the scheduler which is running for the monitor
     */
    protected void stopScheduler() {
        schedulerFuture.cancel(true);
    }

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     */
    public void startDependency(ParentComponent component, List<String> parentInstanceIds) {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStartAbleDependencies();
        startDependency(applicationContexts, parentInstanceIds);
    }

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     *
     * @param component        The component which needs to be used to create the monitor
     * @param parentInstanceId parent-instance-id of the instance which added to the monitor
     */
    public void startDependency(ParentComponent component, String parentInstanceId) {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStartAbleDependencies();
        List<String> parentInstanceIds = new ArrayList<String>();
        parentInstanceIds.add(parentInstanceId);
        startDependency(applicationContexts, parentInstanceIds);
    }

    /**
     * This will get invoked based on the activation event of its one of the child
     *
     * @param componentId alias/clusterId of which receive the activated event
     * @param instanceId  instance id of the instance
     * @return whether the instance has created or not
     */
    public boolean startDependency(String componentId, String instanceId) {
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStarAbleDependencies(componentId);
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        return startDependency(applicationContexts, instanceIds);
    }


    /**
     * This will start the parallel dependencies at once from the top level
     * by traversing to find the terminated dependencies.
     * it will get invoked when starting a child instance on termination of a sub tree
     *
     * @param instanceId instance id of the instance
     */
    public void startDependencyOnTermination(String instanceId) {

        //start the first dependency which went to terminated
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStarAbleDependenciesByTermination(this, instanceId);
        for (ApplicationChildContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            if (!this.aliasToActiveChildMonitorsMap.containsKey(context.getId())) {
                log.info(String.format("Starting dependent monitor on termination: [application] %s " +
                        "[component] %s", getAppId(), context.getId()));
                List<String> parentInstanceIds = new ArrayList<String>();
                parentInstanceIds.add(instanceId);
                startMonitor(this, context, parentInstanceIds);
            } else {
                //starting a new instance of the child
                Monitor monitor = aliasToActiveChildMonitorsMap.get(context.getId());
                //Creating the new instance
                if(monitor instanceof ParentComponentMonitor) {
                    ((ParentComponentMonitor) monitor).createInstanceOnTermination(instanceId);
                } else {
                    monitor.createInstanceOnDemand(instanceId);
                }
            }
        }
    }

    /**
     * To start the dependency of the given application contexts
     *
     * @param applicationContexts the found applicationContexts to be started
     * @param parentInstanceIds   the instance-ids of the parent instance
     */
    private boolean startDependency(List<ApplicationChildContext> applicationContexts,
                                    List<String> parentInstanceIds) {
        if (applicationContexts == null || applicationContexts.isEmpty()) {
            //all the groups/clusters have been started and waiting for activation
            log.info("No more dependent monitors to be started for component: [type] " +
                    getMonitorType().toString().toLowerCase()
                    + "[component] " + this.id);
            return false;

        } else {
            for (ApplicationChildContext context : applicationContexts) {
                if (!this.aliasToActiveChildMonitorsMap.containsKey(context.getId())) {
                    log.info(String.format("Starting dependent monitor: [application] %s [component] %s",
                            getAppId(), context.getId()));
                    startMonitor(this, context, parentInstanceIds);
                } else {
                    log.info(String.format("Dependent monitor already created, creating instance: " +
                            "[application] %s [component] %s", getAppId(), context.getId()));

                    Monitor monitor = aliasToActiveChildMonitorsMap.get(context.getId());
                    // Creating new instance
                    for (String instanceId : parentInstanceIds) {
                        monitor.createInstanceOnDemand(instanceId);
                    }
                }
            }
            return true;
        }

    }

    @Override
    public void onChildScalingEvent(ScalingEvent scalingEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Child scaling event received to [parent] %s [network-partition] %s [event-id] %s " +
                            "[group-instance] %s [factor] %s", this.getId(), scalingEvent.getNetworkPartitionId(),
                    scalingEvent.getId(), scalingEvent.getInstanceId(), scalingEvent.getFactor()));
        }
        String networkPartitionId = scalingEvent.getNetworkPartitionId();
        String instanceId = scalingEvent.getInstanceId();
        String id = scalingEvent.getId();
        NetworkPartitionContext networkPartitionContext =
                this.getNetworkPartitionContextsMap().get(networkPartitionId);
        if (networkPartitionContext != null) {
            InstanceContext instanceContext = networkPartitionContext.
                    getInstanceContext(instanceId);
            if (instanceContext != null) {
                if (instanceContext.containsScalingEvent(id)) {
                    instanceContext.removeScalingEvent(id);
                    instanceContext.addScalingEvent(scalingEvent);
                } else {
                    instanceContext.addScalingEvent(scalingEvent);
                }
            }
        }


    }

    /**
     * This will get triggered by the child when scale down beyond min happened in the child
     *
     * @param scalingDownBeyondMinEvent scalingDownBeyondMinEvent used to keep the instance-id,
     *                                  network-partition-id of the instance
     *                                  which is trying to get scale down.
     */
    public void onChildScalingDownBeyondMinEvent(ScalingDownBeyondMinEvent scalingDownBeyondMinEvent) {

        String networkPartitionId = scalingDownBeyondMinEvent.getNetworkPartitionId();
        String instanceId = scalingDownBeyondMinEvent.getInstanceId();
        getNetworkPartitionContext(networkPartitionId).getInstanceContext(instanceId).
                addScalingDownBeyondMinEvent(scalingDownBeyondMinEvent);
    }

    @Override
    public void onChildScalingOverMaxEvent(ScalingUpBeyondMaxEvent scalingUpBeyondMaxEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Child Scaling max out event received to [group]: " + this.getId()
                    + ", [network partition]: " + scalingUpBeyondMaxEvent.getNetworkPartitionId()
                    + ", [event] " + scalingUpBeyondMaxEvent.getId() + ", " +
                    "[group instance] " + scalingUpBeyondMaxEvent.getInstanceId());
        }
        //adding the scaling over max event to group instance Context
        String networkPartitionId = scalingUpBeyondMaxEvent.getNetworkPartitionId();
        String instanceId = scalingUpBeyondMaxEvent.getInstanceId();
        String id = scalingUpBeyondMaxEvent.getId();
        NetworkPartitionContext networkPartitionContext =
                this.getNetworkPartitionContextsMap().get(networkPartitionId);
        if (networkPartitionContext != null) {
            InstanceContext instanceContext = networkPartitionContext.
                    getInstanceContext(instanceId);
            if (instanceContext != null) {
                if (instanceContext.containsScalingEvent(id)) {
                    instanceContext.removeScalingOverMaxEvent(id);
                    instanceContext.addScalingOverMaxEvent(scalingUpBeyondMaxEvent);
                } else {
                    instanceContext.addScalingOverMaxEvent(scalingUpBeyondMaxEvent);
                }
            }
        }
        //calling monitor to go for group scaling or notify the parent
        //this.monitor();

    }

    public NetworkPartitionContext getNetworkPartitionContext(String networkPartitionId) {
        return this.getNetworkPartitionContextsMap().get(networkPartitionId);
    }


    /**
     * This will start the child monitors based on the active of siblings according to start up order
     *
     * @param childId parent id of the event which received
     */
    protected void onChildActivatedEvent(String childId, String instanceId) {

        removeInstanceFromFromInactiveMap(childId, instanceId);
        removeInstanceFromFromTerminatingMap(childId, instanceId);

        boolean startDep = false;
        if (!aliasToActiveChildMonitorsMap.containsKey(childId) ||
                !pendingChildMonitorsList.contains(childId)) {

            // Need to decide whether it has become active in the first iteration.
            // Then need to start the dependents.
            // If it is a second iteration, then if there is no dependents,
            // no need to invoke start dependencies.

            Monitor childMonitor = aliasToActiveChildMonitorsMap.get(childId);
            if(childMonitor != null) {
                Instance instance = childMonitor.getInstance(instanceId);
                boolean firstIteration = false;
                if(instance != null) {
                    if(instance instanceof GroupInstance) {
                        GroupInstance groupInstance = (GroupInstance)instance;
                        firstIteration = groupInstance.getPreviousState() == GroupStatus.Created;
                    } else if(instance instanceof ClusterInstance) {
                        ClusterInstance clusterInstance = (ClusterInstance)instance;
                        firstIteration = clusterInstance.getPreviousState() == ClusterStatus.Created;
                    }
                    if(firstIteration || childMonitor.hasStartupDependents()) {
                        startDep = startDependency(childId, instanceId);
                    }
                } else {
                    startDep = startDependency(childId, instanceId);
                }
            } else {
                startDep = startDependency(childId, instanceId);
            }

        }

        //Checking whether all the monitors got created
        if (!startDep) {
            ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().
                    process(this.id, this.appId, instanceId);
        } else {
            log.info("started a child upon activation of " + childId +
                    " for [application] " + appId + " [" + getMonitorType() + "] " + id);
        }

    }

    /**
     * This will act upon the any child in-activated event and terminate other dependents or
     * terminate all according to the termination behavior
     *
     * @param childId    id of the child
     * @param instanceId id of the instance which got the status changed
     */
    protected void onChildInactiveEvent(String childId, final String instanceId) {
        List<ApplicationChildContext> terminationList;
        terminationList = this.startupDependencyTree.getTerminationDependencies(childId);

        //Need to notify the parent about the status  change from Active-->Inactive
        ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().
                process(id, appId, instanceId);

        /**
         * Since it is reached the most independent unit and has few independent monitors,
         * has to put the children down to terminating
         */
        if (this.startupDependencyTree.getTerminationBehavior() ==
                DependencyTree.TerminationBehavior.TERMINATE_ALL &&
                terminationList.size() == this.aliasToActiveChildMonitorsMap.size()) {
            //handling the kill-all scenario
            if (parent != null) {
                //send terminating to the parent. So that it will push terminating to its children
                log.info("[group-instance] " + instanceId + "  in [group] " + id +
                        " in [application] " + appId + " has been marked as " +
                        "terminating due to [terminate-all] behavior");
                ApplicationBuilder.handleGroupTerminatingEvent(appId, id, instanceId);

            } else {
                //if it is an application, send terminating event individually for children
                log.info("Since this is application, all children will get terminated one-by-one " +
                        "for [application] " + appId + " [application-instance] " + instanceId +
                        " due to [terminate-all] behavior");
                sendTerminatingEventOnNotification(terminationList, childId, true, instanceId);
            }
        } else {
            log.info("Dependent children of [instance] " + instanceId + "  in [group] " + id +
                    " in [application] " + appId + " will be marked as terminating due to " +
                    "[terminate-dependents] behavior");
            sendTerminatingEventOnNotification(terminationList, childId, false, instanceId);
        }
    }

    /**
     * Utility method to send the termination notification to group/cluster based on termination list
     *
     * @param terminationList termination list of siblings
     * @param notifier        who notified
     * @param terminateAll    whether terminate-all or not
     * @param instanceId      instance id of the instance
     */
    private void sendTerminatingEventOnNotification(List<ApplicationChildContext> terminationList,
                                                    String notifier, boolean terminateAll, String instanceId) {
        Monitor monitor;
        //Checking the termination dependents status
        for (ApplicationChildContext terminationContext : terminationList) {
            //Check whether dependent is in_active, then start to kill it
            monitor = this.aliasToActiveChildMonitorsMap.
                    get(terminationContext.getId());
            //start to kill it
            if (monitor != null) {
                if (monitor instanceof GroupMonitor) {
                    //it is a group, so not sending terminating as it can be in inactive. If group needs terminating,
                    //it will be handled by the terminating case of its children
                    if (terminateAll || !notifier.equals(monitor.getId())) {
                        ApplicationBuilder.handleGroupTerminatingEvent(this.appId,
                                terminationContext.getId(), instanceId);
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster Terminating event for [application] " + appId +
                                " [group] " + this.id + " [cluster]: " + terminationContext.getId());
                    }
                    ClusterStatusEventPublisher.sendClusterStatusClusterTerminatingEvent(this.appId,
                            ((ClusterMonitor) monitor).getServiceId(),
                            terminationContext.getId(), instanceId);
                }
            } else {
                log.warn("The relevant [monitor] " + terminationContext.getId() +
                        " in [application] " + appId + "is not in the active map....");
            }

        }
    }

    /**
     * Act upon one of its children got terminated
     *
     * @param eventId id of the notifier
     */
    protected void onChildTerminatedEvent(String eventId, String instanceId) {
        List<ApplicationChildContext> terminationList;
        boolean allDependentTerminated = false;

        //Retrieving the termination list
        terminationList = this.startupDependencyTree.getTerminationDependencies(eventId);

        //Make sure that all the dependents have been terminated properly to start the recovery
        if (terminationList != null) {
            allDependentTerminated = allDependentTerminated(terminationList, instanceId);
        }
        log.info("Calculating the dependencies to be started upon the termination of the " +
                "[group/cluster] " + eventId + " for [instance] " + instanceId +
                " of [application] " + appId);

        List<ApplicationChildContext> parentContexts = this.startupDependencyTree.
                findAllParentContextWithId(eventId);
        boolean parentsTerminated = false;
        boolean allParentsActive = false;
        //make sure all the parent contexts got terminated or whether all of them are active
        if (parentContexts != null) {
            parentsTerminated = allParentTerminated(parentContexts, instanceId);
            allParentsActive = allParentActive(parentContexts, instanceId);
        }

        if ((terminationList == null || terminationList.isEmpty() || allDependentTerminated) &&
                (parentContexts == null || parentContexts.isEmpty() ||
                        parentsTerminated || allParentsActive)) {
            //Starting the dependency sibling upon termination of most in-dependent sibling
            this.startDependencyOnTermination(instanceId);
        } else {
            ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().
                    process(id, appId, instanceId);
            log.info("Checking the status of [group/application] as no dependent found for " +
                    "[application] " + appId + " [group] " + id + " [instance] " + instanceId);
        }


    }

    /**
     * Calculating whether all the dependent has terminated or not
     *
     * @param terminationList termination list of siblings
     * @param instanceId      instance id of the instance
     * @return whether all dependents terminated or not
     */
    private boolean allDependentTerminated(List<ApplicationChildContext> terminationList, String instanceId) {
        boolean allDependentTerminated = false;
        for (ApplicationChildContext context1 : terminationList) {
            if (this.inactiveInstancesMap.containsKey(context1.getId()) &&
                    this.inactiveInstancesMap.get(context1.getId()).contains(instanceId)
                    || this.terminatingInstancesMap.containsKey(context1.getId()) &&
                    this.terminatingInstancesMap.get(context1.getId()).contains(instanceId)) {
                log.info("Waiting for the [dependent] " + context1.getId() + " [instance] " +
                        instanceId + "to be terminated...");
                return false;
            } else if (this.aliasToActiveChildMonitorsMap.get(context1.getId()).getInstance(instanceId) != null) {
                log.info("[Dependent] " + context1.getId() + "[Instance] " + instanceId +
                        "has not been started to terminate yet. Hence waiting....");
            } else {
                allDependentTerminated = true;
            }
        }
        return allDependentTerminated;
    }


    /**
     * Calculating whether all the required siblings are terminated or not
     *
     * @param parentContexts all the siblings who required to be active
     * @param instanceId     instance id of the instance which has state change
     * @return whether all the required siblings are terminated or not
     */
    private boolean allParentTerminated(List<ApplicationChildContext> parentContexts,
                                        String instanceId) {
        boolean parentsTerminated = false;
        for (ApplicationChildContext context1 : parentContexts) {
            if (this.inactiveInstancesMap.containsKey(context1.getId()) &&
                    this.inactiveInstancesMap.get(context1.getId()).contains(instanceId)
                    || this.terminatingInstancesMap.containsKey(context1.getId()) &&
                    this.terminatingInstancesMap.get(context1.getId()).contains(instanceId)) {
                log.info("Waiting for the [Parent Monitor] " + context1.getId()
                        + " to be terminated");
                return false;
            } else if (this.aliasToActiveChildMonitorsMap.get(context1.getId()).getInstance(instanceId) != null) {
                log.info("[Dependent Parent] " + context1.getId() + "[Instance] " + instanceId +
                        "has not been started to terminate yet. Hence waiting....");
            } else {
                log.info("[Parent Monitor] " + context1.getId()
                        + " has already been terminated");
                parentsTerminated = true;
            }
        }
        return parentsTerminated;
    }

    /**
     * Calculating whether all the required siblings are active or not
     *
     * @param parentContexts all the siblings who required to be active
     * @param instanceId     instance id of the instance which has state change
     * @return whether all the required siblings are active or not
     */
    private boolean allParentActive(List<ApplicationChildContext> parentContexts, String instanceId) {
        boolean parentsActive = false;
        for (ApplicationChildContext context1 : parentContexts) {
            if (this.inactiveInstancesMap.containsKey(context1.getId()) &&
                    this.inactiveInstancesMap.get(context1.getId()).contains(instanceId) ||
                    this.terminatingInstancesMap.containsKey(context1.getId()) &&
                            this.terminatingInstancesMap.get(context1.getId()).contains(instanceId)) {
                log.info("Dependent [Monitor] " + context1.getId()
                        + " is not yet active");
                return false;
            } else if (this.aliasToActiveChildMonitorsMap.containsKey(context1.getId())) {
                Monitor monitor = this.aliasToActiveChildMonitorsMap.get(context1.getId());

                if (monitor instanceof GroupMonitor) {
                    try {
                        ApplicationHolder.acquireReadLock();
                        //verify whether the GroupInstance is active or not
                        parentsActive = verifyGroupStatus(context1.getId(), instanceId,
                                GroupStatus.Active);
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                } else if (monitor instanceof ClusterMonitor) {
                    ClusterMonitor monitor1 = (ClusterMonitor) monitor;
                    try {
                        TopologyManager.acquireReadLockForCluster(monitor1.getServiceId(),
                                monitor1.getClusterId());
                        ClusterInstance clusterInstance = (ClusterInstance) monitor1.
                                getInstance(instanceId);
                        parentsActive = clusterInstance != null &&
                                clusterInstance.getStatus() == ClusterStatus.Active;
                    } finally {
                        TopologyManager.releaseReadLockForCluster(monitor1.getServiceId(),
                                monitor1.getClusterId());
                    }
                }

            }
        }
        return parentsActive;
    }

    /**
     * Calculating whether the instance of the group in a required status
     *
     * @param childId        id of the child where the instance resides
     * @param instanceId     instance id of the instance which has state change
     * @param requiredStatus the status to be checked
     * @return whether the group instance in required status or not
     */
    public boolean verifyGroupStatus(String childId, String instanceId, GroupStatus requiredStatus) {
        Monitor monitor = this.getMonitor(childId);
        if (!(monitor instanceof GroupMonitor)) {
            return false;
        }
        List<String> groupInstances;
        GroupInstance groupInstance = (GroupInstance) monitor.getInstance(instanceId);
        if (groupInstance == null) {
            groupInstances = monitor.getInstancesByParentInstanceId(instanceId);
        } else {
            return groupInstance.getStatus() == requiredStatus;
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

        if(log.isDebugEnabled()) {
            log.debug(String.format("Calculating the group instances status for [application] " +
                    "%s [group] %s [group-instance] %s [required-status] %s [no-of-instances] %s",
                    appId, childId, instanceId, requiredStatus.toString(),
                    noOfInstancesOfRequiredStatus));
        }

        if (!groupInstances.isEmpty()) {
            ParentLevelNetworkPartitionContext networkPartitionContext =
                    (ParentLevelNetworkPartitionContext) ((GroupMonitor) monitor).
                            getNetworkPartitionContextsMap().get(networkPartitionId);
            int minInstances = networkPartitionContext.getMinInstanceCount();
            //if terminated all the instances in this instances map should be in terminated state
            if (noOfInstancesOfRequiredStatus == this.inactiveInstancesMap.size() &&
                    requiredStatus == GroupStatus.Terminated) {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Group instances in required status for [application] " +
                                    "%s [group] %s [group-instance] %s [required-status] %s",
                            appId, childId, instanceId, GroupStatus.Terminated.toString()));
                }
                return true;
            } else if (noOfInstancesOfRequiredStatus >= minInstances) {
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Group instances in required status for [application] " +
                                    "%s [group] %s [group-instance] %s [required-status] %s",
                            appId, childId, instanceId, requiredStatus.toString()));
                }
                return true;
            } else {
                //of only one is inActive implies that the whole group is Inactive
                if (requiredStatus == GroupStatus.Inactive && noOfInstancesOfRequiredStatus >= 1) {
                    if(log.isDebugEnabled()) {
                        log.debug(String.format("Group instances in required status for [application] " +
                                        "%s [group] %s [group-instance] %s [required-status] %s",
                                appId, childId, instanceId, GroupStatus.Inactive.toString()));
                    }
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * handling the dependent scaling
     *
     * @param instanceContext         instance-context of the Instance which receives
     *                                the scale notification from the child
     * @param networkPartitionContext network-partition-context which belongs to the instance
     */
    protected void handleDependentScaling(InstanceContext instanceContext,
                                          NetworkPartitionContext networkPartitionContext) {
        /**
         * Dependency scaling handling
         * Finding out the highest scaling events within the scaling dependencies
         */
        List<ScalingEvent> highestScalingEventOfDependencies = new ArrayList<ScalingEvent>();
        for (ScalingDependentList scalingDependentList : scalingDependencies) {
            ScalingEvent highestFactorEvent = null;
            for (String scalingDependentListComponent : scalingDependentList.
                    getScalingDependentListComponents()) {
                ScalingEvent scalingEvent = instanceContext.
                        getScalingEvent(scalingDependentListComponent);
                if (scalingEvent != null) {
                    if (highestFactorEvent == null) {
                        highestFactorEvent = scalingEvent;
                    } else {
                        if (scalingEvent.getFactor() > highestFactorEvent.getFactor()) {
                            highestFactorEvent = scalingEvent;
                        }
                    }
                }
            }
            if (highestFactorEvent != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Found the highest factor for the [dependent set] " +
                            highestFactorEvent.getId() + " the factor is " +
                            highestFactorEvent.getFactor());
                }
                highestScalingEventOfDependencies.add(highestFactorEvent);
            }
        }

        for (ScalingEvent highestScalingEventOfChild : highestScalingEventOfDependencies) {
            //find the child context of this group,
            //Notifying children, if this group has scaling dependencies
            if (scalingDependencies != null && !scalingDependencies.isEmpty()) {
                for (ScalingDependentList scalingDependentList : scalingDependencies) {
                    if (scalingDependentList.getScalingDependentListComponents().
                            contains(highestScalingEventOfChild.getId())) {
                        for (String scalingDependentListComponent : scalingDependentList
                                .getScalingDependentListComponents()) {
                            Monitor monitor = aliasToActiveChildMonitorsMap.get(
                                    scalingDependentListComponent);
                            if (monitor instanceof GroupMonitor ||
                                    monitor instanceof ClusterMonitor) {
                                ScalingEvent scalingEvent = new ScalingEvent(monitor.getId(),
                                        networkPartitionContext.getId(),
                                        instanceContext.getId(),
                                        highestScalingEventOfChild.getFactor());

                                log.info("Notifying the [child] " + scalingEvent.getId() + " [instance] " +
                                        scalingEvent.getInstanceId() + " with the highest [factor] " +
                                        scalingEvent.getFactor() + " upon decision of dependent scaling");

                                monitor.onParentScalingEvent(scalingEvent);
                            }
                        }
                        break;
                    }

                }
            }
        }
    }

    /**
     * move to inactive monitors list to use in the Terminated event
     *
     * @param childId    id of the child where the instance resides
     * @param instanceId instance id of the instance which has state change
     */
    protected synchronized void markInstanceAsInactive(String childId, String instanceId) {
        if (this.inactiveInstancesMap.containsKey(childId)) {
            this.inactiveInstancesMap.get(childId).add(instanceId);
        } else {
            List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(instanceId);
            this.inactiveInstancesMap.put(childId, instanceIds);
        }
    }

    /**
     * move to inactive monitors list to use in the Terminated event
     *
     * @param childId    id of the child where the instance resides
     * @param instanceId instance id of the instance which has state change
     */
    protected synchronized void removeInstanceFromFromInactiveMap(String childId,
                                                                  String instanceId) {
        if (this.inactiveInstancesMap.containsKey(childId) &&
                this.inactiveInstancesMap.get(childId).contains(instanceId)) {
            this.inactiveInstancesMap.get(childId).remove(instanceId);
            if (this.inactiveInstancesMap.get(childId).isEmpty()) {
                this.inactiveInstancesMap.remove(childId);
            }
        }
    }

    /**
     * move to inactive monitors list to use in the Terminated event
     *
     * @param childId    id of the child where the instance resides
     * @param instanceId instance id of the instance which has state change
     */
    protected synchronized void removeInstanceFromFromTerminatingMap(String childId, String instanceId) {
        if (this.terminatingInstancesMap.containsKey(childId) &&
                this.terminatingInstancesMap.get(childId).contains(instanceId)) {
            this.terminatingInstancesMap.get(childId).remove(instanceId);
            if (this.terminatingInstancesMap.get(childId).isEmpty()) {
                this.terminatingInstancesMap.remove(childId);
            }
        }
    }

    /**
     * move to inactive monitors list to use in the Terminated event
     *
     * @param childId    id of the child where the instance resides
     * @param instanceId instance id of the instance which has state change
     */
    protected synchronized void markInstanceAsTerminating(String childId, String instanceId) {
        if (this.inactiveInstancesMap.containsKey(childId) &&
                this.inactiveInstancesMap.get(childId).contains(instanceId)) {
            this.inactiveInstancesMap.get(childId).remove(instanceId);
        }
        if (this.terminatingInstancesMap.containsKey(childId)) {
            this.terminatingInstancesMap.get(childId).add(instanceId);
        } else {
            List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(instanceId);
            this.terminatingInstancesMap.put(childId, instanceIds);
        }
    }


    /**
     * Utility method to start the monitor thread for the children
     *
     * @param parent            The parent monitor which starting its own children
     * @param context           the child context which used to start the specific child
     * @param parentInstanceIds the instance-ids of the parent instance
     */
    protected synchronized void startMonitor(ParentComponentMonitor parent,
                                             ApplicationChildContext context, List<String> parentInstanceIds) {
        if (!this.aliasToActiveChildMonitorsMap.containsKey(context.getId())) {
            pendingChildMonitorsList.add(context.getId());
            executorService.submit(new MonitorAdder(parent, context, this.appId, parentInstanceIds));

            String monitorTypeStr = AutoscalerUtil.findMonitorType(context).toString().toLowerCase();
            log.info(String.format("Monitor scheduled: [type] %s [component] %s ",
                    monitorTypeStr, context.getId()));
        }
    }

    /**
     * This will return the child monitors map
     *
     * @return child monitors map
     */
    public Map<String, Monitor> getAliasToActiveChildMonitorsMap() {
        return aliasToActiveChildMonitorsMap;
    }

    /**
     * This will return the specific child monitor
     *
     * @param childId id of the child
     * @return child monitor
     */
    public Monitor getMonitor(String childId) {
        return this.aliasToActiveChildMonitorsMap.get(childId);
    }

    /**
     * Whether the monitor has child monitors or not
     *
     * @return true if the monitor has child monitors
     */
    public boolean hasMonitors() {

        return this.aliasToActiveChildMonitorsMap != null;
    }

    /**
     * This will give the algorithm for the partitions for this monitor
     *
     * @param partitionAlgorithm algorithm name to be used for the partitions
     * @return partition-algorithm instance of relevant partition algorithm
     */
    public PartitionAlgorithm getAutoscaleAlgorithm(String partitionAlgorithm) {
        PartitionAlgorithm autoscaleAlgorithm = null;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Partition algorithm is %s ", partitionAlgorithm));
        }
        if (StratosConstants.PARTITION_ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new RoundRobin();
        } else if (StratosConstants.PARTITION_ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new OneAfterAnother();
        } else {
            if (log.isErrorEnabled()) {
                log.error(String.format("Partition algorithm %s could not be identified !", partitionAlgorithm));
            }
        }
        return autoscaleAlgorithm;
    }

    /**
     * Scale dependencies from the children
     *
     * @return scale dependencies
     */
    public Set<ScalingDependentList> getScalingDependencies() {
        return scalingDependencies;
    }

    /**
     * Startup-order tree built among the children according to the specified startup-order
     *
     * @return startup-dependency-tree
     */
    public DependencyTree getStartupDependencyTree() {
        return startupDependencyTree;
    }

    /**
     * This will give the network partitions used by this monitor
     *
     * @return network-partition-contexts
     */
    public Map<String, NetworkPartitionContext> getNetworkPartitionContextsMap() {
        return networkPartitionContextsMap;
    }

    /**
     * Inner class used a Thread to start the relevant child monitor in a asynchronous manner
     */
    private class MonitorAdder implements Runnable {

        private final ApplicationChildContext context;
        private final ParentComponentMonitor parent;
        private final String appId;
        private final List<String> parentInstanceIds;
        private final String monitorTypeStr;

        public MonitorAdder(ParentComponentMonitor parent, ApplicationChildContext context,
                            String appId, List<String> parentInstanceIds) {
            this.parent = parent;
            this.context = context;
            this.appId = appId;
            this.parentInstanceIds = parentInstanceIds;
            this.monitorTypeStr = AutoscalerUtil.findMonitorType(context).toString().toLowerCase();
        }

        public void run() {
            try {
                long startTime = System.currentTimeMillis();
                long endTime = startTime;
                int retries = 5;
                Monitor monitor = null;
                boolean success = false;
                while (!success && retries >= 0) {

                    startTime = System.currentTimeMillis();
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Starting monitor: [type] %s [component] %s",
                                monitorTypeStr, context.getId()));
                    }

                    try {
                        monitor = MonitorFactory.getMonitor(parent, context, appId, parentInstanceIds);
                    } catch (DependencyBuilderException e) {
                        String msg = String.format("Monitor creation failed: [type] %s [component] %s",
                                monitorTypeStr, context.getId());
                        log.warn(msg, e);
                        retries--;
                    } catch (TopologyInConsistentException e) {
                        String msg = String.format("Monitor creation failed: [type] %s [component] %s",
                                monitorTypeStr, context.getId());
                        log.warn(msg, e);
                        retries--;
                    } catch (PolicyValidationException e) {
                        String msg = String.format("Monitor creation failed: [type] %s [component] %s",
                                monitorTypeStr, context.getId());
                        log.warn(msg, e);
                        retries--;
                    } catch (PartitionValidationException e) {
                        String msg = String.format("Monitor creation failed: [type] %s [component] %s",
                                monitorTypeStr, context.getId());
                        log.warn(msg, e);
                        retries--;
                    }
                    success = true;
                    endTime = System.currentTimeMillis();
                }

                if (monitor == null) {
                    String msg = String.format("Monitor creation failed even after retrying for " +
                            "5 times: [type] %s [component] %s ", monitorTypeStr, context.getId());
                    log.error(msg);
                    throw new RuntimeException(msg);
                }

                aliasToActiveChildMonitorsMap.put(context.getId(), monitor);
                pendingChildMonitorsList.remove(context.getId());

                if (log.isInfoEnabled()) {
                    long startupTime = (endTime - startTime) / 1000;
                    log.info(String.format("Monitor started successfully: [type] %s [component] %s [dependents] %s " +
                                    "[startup-time] %d seconds", monitorTypeStr, context.getId(),
                            getIdList(context.getApplicationChildContextList()), startupTime));
                }
            } catch (Exception e) {
                log.error(String.format("An error occurred while starting monitor: [type] %s [component] %s",
                        monitorTypeStr, context.getId()), e);
            }
        }

        private String getIdList(List<ApplicationChildContext> applicationChildContextList) {
            StringBuilder stringBuilder = new StringBuilder();
            if ((applicationChildContextList != null) && (applicationChildContextList.size() > 0)) {
                for (ApplicationChildContext applicationChildContext : applicationChildContextList) {
                    stringBuilder.append(applicationChildContext.getId());
                    if (applicationChildContextList.indexOf(applicationChildContext) <
                            (applicationChildContextList.size() - 1)) {
                        stringBuilder.append(", ");
                    }
                }
            } else {
                stringBuilder.append("none");
            }
            return stringBuilder.toString();
        }
    }
}
