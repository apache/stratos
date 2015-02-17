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
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyBuilder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.network.NetworkPartitionContext;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
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
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class ParentComponentMonitor extends Monitor implements Runnable {

    private static final Log log = LogFactory.getLog(ParentComponentMonitor.class);

    //Scheduler executor service to execute this monitor in a thread
    private final ScheduledExecutorService scheduler = StratosThreadPool.getScheduledExecutorService(
            "autoscaler.monitor.scheduler.thread.pool", 100);
    //The monitors dependency tree with all the start-able/kill-able dependencies
    protected DependencyTree startupDependencyTree;
    //The monitors dependency tree with all the scaling dependencies
    protected Set<ScalingDependentList> scalingDependencies;
    //monitors map, key=GroupAlias/clusterId and value=GroupMonitor/AbstractClusterMonitor
    protected Map<String, Monitor> aliasToActiveMonitorsMap;
    //Pending monitors list
    protected List<String> pendingMonitorsList;
    //instanceIds map, key=alias, value instanceIds stopped monitors
    protected Map<String, List<String>> inactiveInstancesMap;
    //terminating map, key=alias, value instanceIds
    protected Map<String, List<String>> terminatingInstancesMap;
    //network partition contexts
    protected Map<String, NetworkPartitionContext> networkPartitionCtxts;
    //Executor service to maintain the thread pool
    private ExecutorService executorService;
    //Monitoring interval of the monitor
    private int monitoringIntervalMilliseconds = 60000;     //TODO get this from config file

    public ParentComponentMonitor(ParentComponent component) throws DependencyBuilderException {
        aliasToActiveMonitorsMap = new ConcurrentHashMap<String, Monitor>();
        inactiveInstancesMap = new ConcurrentHashMap<String, List<String>>();
        terminatingInstancesMap = new ConcurrentHashMap<String, List<String>>();
        pendingMonitorsList = new ArrayList<String>();
        //clusterIdToClusterMonitorsMap = new HashMap<String, AbstractClusterMonitor>();
        this.id = component.getUniqueIdentifier();
        //Building the startup dependencies for this monitor within the immediate children
        startupDependencyTree = DependencyBuilder.getInstance().buildDependency(getAppId(), component);
        //Building the scaling dependencies for this monitor within the immediate children
        if ((component.getDependencyOrder() != null) && (component.getDependencyOrder().
                                                            getScalingDependents() != null)) {
            scalingDependencies = DependencyBuilder.getInstance().buildScalingDependencies(component);
        } else {
            // No scaling dependencies found, initialize to an empty set
            scalingDependencies = new HashSet<ScalingDependentList>();
        }
        //Create the executor service with identifier and thread pool size
        executorService = StratosThreadPool.getExecutorService(AutoscalerConstants.AUTOSCALER_THREAD_POOL_ID,
                AutoscalerConstants.AUTOSCALER_THREAD_POOL_SIZE);
        networkPartitionCtxts = new HashMap<String, NetworkPartitionContext>();
    }

    /**
     * This will monitor the network partition context with child notifications
     */
    public abstract void monitor();

    public void startScheduler() {
        scheduler.scheduleAtFixedRate(this, 0, monitoringIntervalMilliseconds, TimeUnit.MILLISECONDS);
    }

    protected void stopScheduler() {
        scheduler.shutdownNow();
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
     */
    public void startDependency(ParentComponent component, String parentInstanceId) throws
            MonitorNotFoundException {
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
     */
    public boolean startDependency(String componentId, String instanceId) throws MonitorNotFoundException {
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.getStarAbleDependencies(componentId);
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
    public void startDependencyOnTermination(String instanceId) throws TopologyInConsistentException,
            MonitorNotFoundException, PolicyValidationException, PartitionValidationException {

        //start the first dependency which went to terminated
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStarAbleDependenciesByTermination(this, instanceId);
        for (ApplicationChildContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                log.info(String.format("Starting dependent monitor on termination: [application] %s [component] %s",
                        getAppId(), context.getId()));
                List<String> parentInstanceIds = new ArrayList<String>();
                parentInstanceIds.add(instanceId);
                startMonitor(this, context, parentInstanceIds);
            } else {
                //starting a new instance of the child
                Monitor monitor = aliasToActiveMonitorsMap.get(context.getId());
                //Creating the new instance
                monitor.createInstanceOnDemand(instanceId);
            }
        }
    }

    /**
     * To start the dependency of the given application contexts
     *
     * @param applicationContexts the found applicationContexts to be started
     */
    private boolean startDependency(List<ApplicationChildContext> applicationContexts, List<String> parentInstanceIds) {
        if (applicationContexts != null && applicationContexts.isEmpty()) {
            //all the groups/clusters have been started and waiting for activation
            log.info("No more dependent monitors to be started for component: [type] " + getMonitorType().toString().toLowerCase()
                    + "[component] " + this.id);
            return false;

        }
        for (ApplicationChildContext context : applicationContexts) {
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                log.info(String.format("Starting dependent monitor: [application] %s [component] %s",
                        getAppId(), context.getId()));
                startMonitor(this, context, parentInstanceIds);
            } else {
                log.info(String.format("Dependent monitor already created, creating instance: " +
                                "[application] %s [component] %s", getAppId(), context.getId()));

                Monitor monitor = aliasToActiveMonitorsMap.get(context.getId());
                // Creating new instance
                for (String instanceId : parentInstanceIds) {
                    monitor.createInstanceOnDemand(instanceId);
                }
            }
        }
        return true;
    }

    @Override
    public void onChildScalingEvent(ScalingEvent scalingEvent) {
        log.info("Child scaling event received to [parent]: " + this.getId()
                + ", [network partition]: " + scalingEvent.getNetworkPartitionId()
                + ", [event] " + scalingEvent.getId() + ", [group instance] " + scalingEvent.getInstanceId());

        String networkPartitionId = scalingEvent.getNetworkPartitionId();
        String instanceId = scalingEvent.getInstanceId();
        String id = scalingEvent.getId();
        NetworkPartitionContext networkPartitionContext =
                this.networkPartitionCtxts.get(networkPartitionId);
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
                this.networkPartitionCtxts.get(networkPartitionId);
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
        this.monitor();

    }

    public NetworkPartitionContext getNetworkPartitionContext(String networkPartitionId) {
        return this.networkPartitionCtxts.get(networkPartitionId);
    }


    /**
     * This will start the child monitors based on the active of siblings according to start up order
     *
     * @param childId parent id of the event which received
     */
    protected void onChildActivatedEvent(String childId, String instanceId) {
        try {
            removeInstanceFromFromInactiveMap(childId, instanceId);
            removeInstanceFromFromTerminatingMap(childId, instanceId);

            boolean startDep = false;
            if (!aliasToActiveMonitorsMap.containsKey(childId) || !pendingMonitorsList.contains(childId)) {
                startDep = startDependency(childId, instanceId);
            }

            //Checking whether all the monitors got created
            if (!startDep) {
                ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().
                        process(this.id, this.appId, instanceId);
            } else {
                log.info("started a child: " + startDep + " by the group/cluster: " + this.id);
            }
        } catch (MonitorNotFoundException e) {
            //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
            log.error(e);
        }
    }

    /**
     * @param childId
     */
    protected void onChildInactiveEvent(String childId, final String instanceId) {
        List<ApplicationChildContext> terminationList;
        terminationList = this.startupDependencyTree.getTerminationDependencies(childId);
        //Need to notify the parent about the status  change from Active-->Inactive
        // TODO to make app also inaction if (this.parent != null) {
        ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().
                process(id, appId, instanceId);

        //TODO checking whether terminating them in reverse order,
        // TODO if so can handle it in the parent event.

        //Since it is reached the most independent unit and has few independent monitors,
        // has to put the children down to terminating
        if (this.startupDependencyTree.getTerminationBehavior() ==
                DependencyTree.TerminationBehavior.TERMINATE_ALL &&
                terminationList.size() == this.aliasToActiveMonitorsMap.size()) {
            //handling the killall scenario
            if (this.parent != null) {
                //send terminating to the parent. So that it will push terminating to its children
                ApplicationBuilder.handleGroupTerminatingEvent(this.appId, this.id, instanceId);
            } else {
                //if it is an application, send terminating event individually for children
                sendTerminatingEventOnNotification(terminationList, childId, true, instanceId);
            }
            log.info("The group" + childId + " has been marked as terminating " +
                    "due to all the children are to be terminated");
        } else {
            sendTerminatingEventOnNotification(terminationList, childId, false, instanceId);
        }
    }

    private void sendTerminatingEventOnNotification(List<ApplicationChildContext> terminationList,
                                                    String notifier, boolean terminateAll, String instanceId) {
        Monitor monitor;
        //Checking the termination dependents status
        for (ApplicationChildContext terminationContext : terminationList) {
            //Check whether dependent is in_active, then start to kill it
            monitor = this.aliasToActiveMonitorsMap.
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
                        log.info("Publishing Cluster Terminating event for [application]: " + appId +
                                " [group] " + this.id + " [cluster]: " + terminationContext.getId());
                    }
                    ClusterStatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                            ((ClusterMonitor) monitor).getServiceId(),
                            terminationContext.getId(), instanceId);
                }
            } else {
                log.warn("The relevant [monitor] " + terminationContext.getId() +
                        "is not in the active map....");
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

        ApplicationChildContext context = this.startupDependencyTree.
                getApplicationChildContextByIdInPrimaryTree(eventId);
        context.setTerminated(true);
        terminationList = this.startupDependencyTree.getTerminationDependencies(eventId);
        //Make sure that all the dependents have been terminated properly to start the recovery
        if (terminationList != null) {
            allDependentTerminated = allDependentTerminated(terminationList, instanceId);
        }
        log.info("Calculating the dependencies to be started upon the termination of the " +
                "group/cluster " + eventId + " for [instance] " + instanceId);

        List<ApplicationChildContext> parentContexts = this.startupDependencyTree.
                findAllParentContextWithId(eventId);
        boolean parentsTerminated = false;
        boolean allParentsActive = false;
        //make sure all the parent contexts got terminated or whether all of them are active
        if (parentContexts != null) {
            parentsTerminated = allParentTerminated(parentContexts, instanceId);
            allParentsActive = allParentActive(parentContexts, instanceId);
        }

        if ((terminationList.isEmpty() || allDependentTerminated) &&
                (parentContexts.isEmpty() || parentsTerminated || allParentsActive)) {
            //Find the non existent monitor by traversing dependency tree

            String errorMessage = String.format("Could not start dependency on termination: [instance-id] %s", instanceId);

            try {
                try {
                    this.startDependencyOnTermination(instanceId);
                } catch (MonitorNotFoundException e) {
                    log.error(errorMessage, e);
                } catch (PolicyValidationException e) {
                    log.error(errorMessage, e);
                } catch (PartitionValidationException e) {
                    log.error(errorMessage, e);
                }
            } catch (TopologyInConsistentException e) {
                //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
                log.error("Error while starting the monitor upon termination" + e);
            }
        } else {
            ServiceReferenceHolder.getInstance().getGroupStatusProcessorChain().
                    process(this.id, this.appId, instanceId);
            log.info("Checking the status of group/application as no dependent found...");
        }


    }

    private boolean allDependentTerminated(List<ApplicationChildContext> terminationList, String instanceId) {
        boolean allDependentTerminated = false;
        for (ApplicationChildContext context1 : terminationList) {
            if (this.inactiveInstancesMap.containsKey(context1.getId()) &&
                    this.inactiveInstancesMap.get(context1.getId()).contains(instanceId)
                    || this.terminatingInstancesMap.containsKey(context1.getId()) &&
                    this.terminatingInstancesMap.get(context1.getId()).contains(instanceId)) {
                log.info("Waiting for the [dependent] " + context1.getId() + " [instance] " +
                        instanceId + "to be terminated...");
                allDependentTerminated = false;
                return allDependentTerminated;
            } else if (this.aliasToActiveMonitorsMap.get(context1.getId()).getInstance(instanceId) != null) {
                log.info("[Dependent] " + context1.getId() + "[Instance] " + instanceId +
                        "has not been started to terminate yet. Hence waiting....");
            } else {
                allDependentTerminated = true;
            }
        }
        return allDependentTerminated;
    }


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
                parentsTerminated = false;
                return parentsTerminated;
            } else if (this.aliasToActiveMonitorsMap.get(context1.getId()).getInstance(instanceId) != null) {
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

    private boolean allParentActive(List<ApplicationChildContext> parentContexts, String instanceId) {
        boolean parentsActive = false;
        for (ApplicationChildContext context1 : parentContexts) {
            if (this.inactiveInstancesMap.containsKey(context1.getId()) &&
                    this.inactiveInstancesMap.get(context1.getId()).contains(instanceId) ||
                    this.terminatingInstancesMap.containsKey(context1.getId()) &&
                            this.terminatingInstancesMap.get(context1.getId()).contains(instanceId)) {
                parentsActive = false;
                log.info("Dependent [Monitor] " + context1.getId()
                        + " is not yet active");
                return parentsActive;
            } else if (this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                Monitor monitor = this.aliasToActiveMonitorsMap.get(context1.getId());

                if (monitor instanceof GroupMonitor) {
                    GroupMonitor monitor1 = (GroupMonitor) monitor;
                    try {
                        ApplicationHolder.acquireReadLock();
                        if (monitor1.verifyGroupStatus(context1.getId(), instanceId, GroupStatus.Active)) {
                            parentsActive = true;

                        }
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                } else if (monitor instanceof ClusterMonitor) {
                    ClusterMonitor monitor1 = (ClusterMonitor) monitor;
                    TopologyManager.acquireReadLockForCluster(monitor1.getServiceId(),
                            monitor1.getClusterId());
                    try {
                        if (((ClusterInstance) monitor1.getInstance(instanceId)).getStatus()
                                == ClusterStatus.Active) {
                            parentsActive = true;
                        }
                    } finally {
                        TopologyManager.releaseReadLockForCluster(monitor1.getServiceId(),
                                monitor1.getClusterId());
                    }
                }

            }
        }
        return parentsActive;
    }

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
                            Monitor monitor = aliasToActiveMonitorsMap.get(
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
                    }
                    break;
                }
            }
        }
        //Resetting the events
        instanceContext.setIdToScalingEvent(new ConcurrentHashMap<String, ScalingEvent>());
    }

    // move to inactive monitors list to use in the Terminated event
    protected synchronized void markInstanceAsInactive(String childId, String instanceId) {
        if (this.inactiveInstancesMap.containsKey(childId)) {
            this.inactiveInstancesMap.get(childId).add(instanceId);
        } else {
            List<String> instanceIds = new ArrayList<String>();
            instanceIds.add(instanceId);
            this.inactiveInstancesMap.put(childId, instanceIds);
        }
    }

    // move to inactive monitors list to use in the Terminated event
    protected synchronized void removeInstanceFromFromInactiveMap(String childId, String instanceId) {
        if (this.inactiveInstancesMap.containsKey(childId) &&
                this.inactiveInstancesMap.get(childId).contains(instanceId)) {
            this.inactiveInstancesMap.get(childId).remove(instanceId);
            if (this.inactiveInstancesMap.get(childId).isEmpty()) {
                this.inactiveInstancesMap.remove(childId);
            }
        }
    }

    // move to inactive monitors list to use in the Terminated event
    protected synchronized void removeInstanceFromFromTerminatingMap(String childId, String instanceId) {
        if (this.terminatingInstancesMap.containsKey(childId) &&
                this.terminatingInstancesMap.get(childId).contains(instanceId)) {
            this.terminatingInstancesMap.get(childId).remove(instanceId);
            if (this.terminatingInstancesMap.get(childId).isEmpty()) {
                this.terminatingInstancesMap.remove(childId);
            }
        }
    }

    // move to inactive monitors list to use in the Terminated event
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


    protected synchronized void startMonitor(ParentComponentMonitor parent,
                                             ApplicationChildContext context, List<String> parentInstanceIds) {
	    if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
		    pendingMonitorsList.add(context.getId());
		    executorService.submit(new MonitorAdder(parent, context, this.appId, parentInstanceIds));

            String monitorTypeStr = AutoscalerUtil.findMonitorType(context).toString().toLowerCase();
            log.info(String.format("Monitor scheduled: [type] %s [component] %s ",
                        monitorTypeStr, context.getId()));
	    }
    }

    public Map<String, Monitor> getAliasToActiveMonitorsMap() {
        return aliasToActiveMonitorsMap;
    }

    public void setAliasToActiveMonitorsMap(Map<String, Monitor> aliasToActiveMonitorsMap) {
        this.aliasToActiveMonitorsMap = aliasToActiveMonitorsMap;
    }

    public boolean hasActiveMonitors() {
        boolean hasMonitor = false;
        if ((this.aliasToActiveMonitorsMap != null && !this.aliasToActiveMonitorsMap.isEmpty())) {
            hasMonitor = true;
        }
        return hasMonitor;
    }

    public Monitor getMonitor(String childId) {
        return this.aliasToActiveMonitorsMap.get(childId);
    }

    public boolean hasMonitors() {

        return this.aliasToActiveMonitorsMap != null;
    }

    public boolean hasIndependentChild() {
        boolean hasInDepChild = false;
        for (Monitor monitor : this.aliasToActiveMonitorsMap.values()) {
            if (!monitor.hasStartupDependents()) {
                hasInDepChild = true;
                break;
            }
        }
        return hasInDepChild;
    }

    public Map<String, List<String>> getAliasToInactiveMonitorsMap() {
        return this.inactiveInstancesMap;
    }

    public void setAliasToInactiveMonitorsMap(Map<String, List<String>> inactiveMonitorsList) {
        this.inactiveInstancesMap = inactiveMonitorsList;
    }

    public Map<String, List<String>> getTerminatingInstancesMap() {
        return terminatingInstancesMap;
    }

    public void setTerminatingInstancesMap(Map<String, List<String>> terminatingInstancesMap) {
        this.terminatingInstancesMap = terminatingInstancesMap;
    }

    public AutoscaleAlgorithm getAutoscaleAlgorithm(String partitionAlgorithm) {
        AutoscaleAlgorithm autoscaleAlgorithm = null;
        if (log.isDebugEnabled()) {
            log.debug(String.format("Partition algorithm is ", partitionAlgorithm));
        }
        if (StratosConstants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new RoundRobin();
        } else if (StratosConstants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new OneAfterAnother();
        } else {
            if (log.isErrorEnabled()) {
                log.error(String.format("Partition algorithm %s could not be identified !", partitionAlgorithm));
            }
        }
        return autoscaleAlgorithm;
    }

    public Set<ScalingDependentList> getScalingDependencies() {
        return scalingDependencies;
    }

    public DependencyTree getStartupDependencyTree() {
        return startupDependencyTree;
    }

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
            long startTime = System.currentTimeMillis();
            long endTime = startTime;
            int retries = 5;
            Monitor monitor = null;
            boolean success = false;
            while (!success && retries != 0) {

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
                String msg = String.format("Monitor creation failed even after retrying for 5 times: "
                        + "[type] %s [component] ", monitorTypeStr, context.getId());
                log.error(msg);
                //TODO parent.notify();
                throw new RuntimeException(msg);
            }

            aliasToActiveMonitorsMap.put(context.getId(), monitor);
            pendingMonitorsList.remove(context.getId());

            if (log.isInfoEnabled()) {
                long startupTime = (endTime - startTime)/1000;
                log.info(String.format("Monitor started successfully: [type] %s [component] %s [dependents] %s " +
                                "[startup-time] %d seconds", monitorTypeStr, context.getId(),
                        getIdList(context.getApplicationChildContextList()), startupTime));
            }
        }

        private String getIdList(List<ApplicationChildContext> applicationChildContextList) {
            StringBuilder stringBuilder = new StringBuilder();
            if((applicationChildContextList != null) && (applicationChildContextList.size() > 0)) {
                for(ApplicationChildContext applicationChildContext : applicationChildContextList) {
                    stringBuilder.append(applicationChildContext.getId());
                    if(applicationChildContextList.indexOf(applicationChildContext) <
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
