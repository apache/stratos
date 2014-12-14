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
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyBuilder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ClusterChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.GroupChildContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.application.MonitorNotFoundException;
import org.apache.stratos.autoscaler.exception.application.TopologyInConsistentException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.policy.PolicyValidationException;
import org.apache.stratos.autoscaler.monitor.Monitor;
import org.apache.stratos.autoscaler.monitor.MonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.applications.ParentComponent;
import org.apache.stratos.messaging.domain.applications.ScalingDependentList;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.Instance;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class ParentComponentMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ParentComponentMonitor.class);
    private static final String IDENTIFIER = "Auto-Scaler";
    private static final int THREAD_POOL_SIZE = 10;

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
    //Executor service to maintain the thread pool
    private ExecutorService executorService;

    public ParentComponentMonitor(ParentComponent component) throws DependencyBuilderException {
        aliasToActiveMonitorsMap = new ConcurrentHashMap<String, Monitor>();
        inactiveInstancesMap = new ConcurrentHashMap<String, List<String>>();
        terminatingInstancesMap = new ConcurrentHashMap<String, List<String>>();
        pendingMonitorsList = new ArrayList<String>();
        //clusterIdToClusterMonitorsMap = new HashMap<String, AbstractClusterMonitor>();
        this.id = component.getUniqueIdentifier();
        //Building the startup dependencies for this monitor within the immediate children
        startupDependencyTree = DependencyBuilder.getInstance().buildDependency(component);
        //Building the scaling dependencies for this monitor within the immediate children
        scalingDependencies  =  component.getDependencyOrder().getScalingDependents();
        //Create the executor service with identifier and thread pool size
	    executorService = StratosThreadPool.getExecutorService(IDENTIFIER, THREAD_POOL_SIZE);
    }

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     */
    public void startDependency(ParentComponent component, List<String> instanceIds) {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStartAbleDependencies();
        startDependency(applicationContexts, instanceIds);

    }

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     */
    public void startDependency(ParentComponent component, String instanceId) throws
            MonitorNotFoundException {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStartAbleDependencies();
        startDependency(applicationContexts, instanceId);

    }

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     */
    public boolean startDependencyByInstanceCreation(String childId, String instanceId) throws
            MonitorNotFoundException {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts =
                this.startupDependencyTree.getStarAbleDependencies(childId);
        return startDependency(applicationContexts, instanceId);
    }

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     */
    public void startDependency(ParentComponent component) {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStartAbleDependencies();
        Collection<Instance> contexts = component.getInstanceIdToInstanceContextMap().values();
        //traversing through all the Instance context and start them
        List<String> instanceIds = new ArrayList<String>();
        for (Instance context : contexts) {
            instanceIds.add(context.getInstanceId());
        }
        startDependency(applicationContexts, instanceIds);
    }

    /**
     * This will get invoked based on the activation event of its one of the child
     *
     * @param id alias/clusterId of which receive the activated event
     */
    public boolean startDependency(String id, String instanceId) throws MonitorNotFoundException {
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree
                .getStarAbleDependencies(id);
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add(instanceId);
        boolean startup = startDependency(applicationContexts, instanceIds);
        return startup;
    }

    public boolean startAllChildrenDependency(ParentComponent component, String instanceId)
            throws TopologyInConsistentException {
        /*List<ApplicationChildContext> applicationContexts = this.startupDependencyTree
                .findAllChildrenOfAppContext(id);*/
        return false;//startDependency(applicationContexts, instanceId);
    }

    /**
     * This will start the parallel dependencies at once from the top level
     * by traversing to find the terminated dependencies.
     * it will get invoked when start a child monitor on termination of a sub tree
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
            //FIXME whether to start new monitor or throw exception
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                String msg = "Required Monitor cannot be fount in the hierarchy";
                throw new MonitorNotFoundException(msg);
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
    private boolean startDependency(List<ApplicationChildContext> applicationContexts,
                                    List<String> instanceIds) {
        if (applicationContexts != null && applicationContexts.isEmpty()) {
            //all the groups/clusters have been started and waiting for activation
            log.info("No more children to start for [group/application] " + this.id);
            return false;

        }
        for (ApplicationChildContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                //to avoid if it is already started
                startMonitor(this, context, instanceIds);
            }
        }

        return true;

    }

    /**
     * To start the dependency of the given application contexts
     *
     * @param applicationContexts the found applicationContexts to be started
     */
    private boolean startDependency(List<ApplicationChildContext> applicationContexts, String instanceId)
            throws MonitorNotFoundException {
        if (applicationContexts != null && applicationContexts.isEmpty()) {
            //all the groups/clusters have been started and waiting for activation
            log.info("There is no child found for the [group]: " + this.id);
            return false;

        }
        for (ApplicationChildContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            //FIXME whether to start new monitor or throw exception
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                String msg = "Required Monitor cannot be fount in the hierarchy";
                throw new MonitorNotFoundException(msg);
            } else {
                //starting a new instance of the child
                Monitor monitor = aliasToActiveMonitorsMap.get(context.getId());
                //Creating the new instance
                monitor.createInstanceOnDemand(instanceId);
            }
        }

        return true;

    }

    protected String generateInstanceId(ParentComponent component) {
        String instanceId = component.getUniqueIdentifier() + "_" +
                (component.getInstanceContextCount() + 1);
        return instanceId;
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

            boolean startDep;
            if (!aliasToActiveMonitorsMap.containsKey(childId) || !pendingMonitorsList.contains(childId)) {
                startDep = startDependency(childId, instanceId);
            } else {
                startDep = startDependencyByInstanceCreation(childId, instanceId);
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
                            ((AbstractClusterMonitor) monitor).getServiceId(),
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
                findApplicationContextWithIdInPrimaryTree(eventId);
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
            try {
                try {
                    this.startDependencyOnTermination(instanceId);
                } catch (MonitorNotFoundException e) {
                    e.printStackTrace();
                } catch (PolicyValidationException e) {
                    e.printStackTrace();
                } catch (PartitionValidationException e) {
                    e.printStackTrace();
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
                    ApplicationHolder.acquireReadLock();
                    try {
                        if (monitor1.verifyGroupStatus(instanceId, GroupStatus.Active)) {
                            parentsActive = true;

                        }
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                } else if (monitor instanceof AbstractClusterMonitor) {
                    AbstractClusterMonitor monitor1 = (AbstractClusterMonitor) monitor;
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
                                             ApplicationChildContext context, List<String> instanceId) {

        if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
            pendingMonitorsList.add(context.getId());
            executorService.submit(new MonitorAdder(parent, context, this.appId, instanceId));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Monitor Adder has been added: [cluster] %s ", context.getId()));
            }
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
        if (Constants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)) {

            autoscaleAlgorithm = new RoundRobin();
        } else if (Constants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)) {

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

    private class MonitorAdder implements Runnable {
        private ApplicationChildContext context;
        private ParentComponentMonitor parent;
        private String appId;
        private List<String> instanceId;

        public MonitorAdder(ParentComponentMonitor parent, ApplicationChildContext context,
                            String appId, List<String> instanceId) {
            this.parent = parent;
            this.context = context;
            this.appId = appId;
            this.instanceId = instanceId;
        }

        public void run() {
            Monitor monitor = null;
            int retries = 5;
            boolean success = false;
            while (!success && retries != 0) {
                /*//TODO remove thread.sleep, exectutor service
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }*/

                if (log.isInfoEnabled()) {
                    log.info("Monitor is going to be started for [group/cluster] "
                            + context.getId());
                }
                try {
                    monitor = MonitorFactory.getMonitor(parent, context, appId, instanceId);
                } catch (DependencyBuilderException e) {
                    String msg = "Monitor creation failed for: " + context.getId();
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Monitor creation failed for: " + context.getId();
                    log.warn(msg, e);
                    retries--;
                } catch (PolicyValidationException e) {
                    String msg = "Monitor creation failed for: " + context.getId();
                    log.warn(msg, e);
                    retries--;
                } catch (PartitionValidationException e) {
                    String msg = "Monitor creation failed for: " + context.getId();
                    log.warn(msg, e);
                    retries--;
                }
                success = true;
            }

            if (monitor == null) {
                String msg = "Monitor creation failed, even after retrying for 5 times, "
                        + "for : " + context.getId();
                log.error(msg);
                //TODO parent.notify();
                throw new RuntimeException(msg);
            }

            aliasToActiveMonitorsMap.put(context.getId(), monitor);
            pendingMonitorsList.remove(context.getId());
            // ApplicationBuilder.
            if (log.isInfoEnabled()) {
                log.info(String.format("Monitor has been added successfully for: %s",
                        context.getId()));
            }
        }
    }
}
