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
package org.apache.stratos.autoscaler.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyBuilder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.applications.ParentComponent;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class ParentComponentMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ParentComponentMonitor.class);

    //The monitors dependency tree with all the start-able/kill-able dependencies
    protected DependencyTree startupDependencyTree;

    //The monitors dependency tree with all the scaling dependencies
    protected DependencyTree scalingDependencyTree;

    public ParentComponentMonitor(ParentComponent component) throws DependencyBuilderException {
        aliasToActiveMonitorsMap = new HashMap<String, Monitor>();
        inactiveMonitorsList = new ArrayList<String>();
        setTerminatingMonitorsList(new ArrayList<String>());
        //clusterIdToClusterMonitorsMap = new HashMap<String, AbstractClusterMonitor>();
        this.id = component.getUniqueIdentifier();
        //Building the startup dependencies for this monitor within the immediate children
        startupDependencyTree = DependencyBuilder.getInstance().buildDependency(component);

        //Building the scaling dependencies for this monitor within the immediate children
        scalingDependencyTree = DependencyBuilder.getInstance().buildDependency(component);
    }

    /**
     * This will start the child monitors based on the active of siblings according to start up order
     *
     * @param eventId parent id of the event which received
     */
    protected void onChildActivatedEvent(String eventId) {
        try {
            //if the activated monitor is in in_active map move it to active map
            if (this.inactiveMonitorsList.contains(eventId)) {
                this.inactiveMonitorsList.remove(eventId);
            }

            if (this.terminatingMonitorsList.contains(eventId)) {
                this.terminatingMonitorsList.remove(eventId);
            }

            boolean startDep = startDependency(eventId);
            if (log.isDebugEnabled()) {
                log.debug("started a child: " + startDep + " by the group/cluster: " + eventId);

            }
            if (!startDep) {
                StatusChecker.getInstance().onChildStatusChange(eventId, this.id, this.appId);
            }
        } catch (TopologyInConsistentException e) {
            //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
            log.error(e);
        }

    }

    // move to inactive monitors list to use in the Terminated event
    protected synchronized void markMonitorAsInactive(String monitorKey) {

        if (!this.inactiveMonitorsList.contains(monitorKey)) {
            this.inactiveMonitorsList.add(monitorKey);
        }
    }

    // move to inactive monitors list to use in the Terminated event
    protected synchronized void markMonitorAsTerminating(String monitorKey) {
        if (!this.terminatingMonitorsList.contains(monitorKey)) {
            if (this.inactiveMonitorsList.contains(monitorKey)) {
                this.inactiveMonitorsList.remove(monitorKey);
            }
            this.terminatingMonitorsList.add(monitorKey);
        }
    }

    /**
     * @param eventId
     */
    protected void onChildInactiveEvent(String eventId) {
        List<ApplicationChildContext> terminationList;
        Monitor monitor;
        terminationList = this.startupDependencyTree.getTerminationDependencies(eventId);
        //Need to notify the parent about the status  change from Active-->InActive
        if (this.parent != null) {
            StatusChecker.getInstance().onChildStatusChange(eventId, this.id, this.appId);
        }
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
                ApplicationBuilder.handleGroupTerminatingEvent(this.appId, eventId);
            } else {
                //if it is an application, send terminating event individually for children
                sendTerminatingEventOnNotification(terminationList, eventId, true);
            }
            log.info("The group" + eventId + " has been marked as terminating " +
                    "due to all the children are to be terminated");
        } else {
            sendTerminatingEventOnNotification(terminationList, eventId, false);
        }
    }

    private void sendTerminatingEventOnNotification(List<ApplicationChildContext> terminationList,
                                                    String notifier, boolean terminateAll) {
        Monitor monitor;
        //Checking the termination dependents status
        for (ApplicationChildContext terminationContext : terminationList) {
            //Check whether dependent is in_active, then start to kill it
            monitor = this.aliasToActiveMonitorsMap.
                    get(terminationContext.getId());
            //start to kill it
            if (monitor != null) {
                if (monitor.hasActiveMonitors()) {
                    //it is a group, so not sending terminating as it can be in inActive. If group needs terminating,
                    //it will be handled by the terminating case of its children
                    if (terminateAll || !notifier.equals(monitor.getId())) {
                        ApplicationBuilder.handleGroupTerminatingEvent(this.appId,
                                terminationContext.getId());
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info("Publishing Cluster Terminating event for [application]: " + appId +
                                " [group] " + this.id + " [cluster]: " + terminationContext.getId());
                    }
                    ClusterStatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                            ((AbstractClusterMonitor) monitor).getServiceId(),
                            terminationContext.getId());
                }
            } else {
                log.warn("The relevant [monitor] " + terminationContext.getId() +
                        "is not in the active map....");
            }

        }
    }

    /**
     * Act upon one of its children got terminated
     * @param eventId id of the notifier
     */

    protected void onChildTerminatedEvent(String eventId) {
        List<ApplicationChildContext> terminationList;
        boolean allDependentTerminated = false;

        ApplicationChildContext context = this.startupDependencyTree.findApplicationContextWithIdInPrimaryTree(eventId);
        context.setTerminated(true);

        terminationList = this.startupDependencyTree.getTerminationDependencies(eventId);


        /**
         * Make sure that all the dependents have been terminated properly to start the recovery
         */
        if (terminationList != null) {
            allDependentTerminated = allDependentTerminated(terminationList);
        }

        List<ApplicationChildContext> parentContexts = this.startupDependencyTree.findAllParentContextWithId(eventId);
        boolean parentsTerminated = false;
        boolean allParentsActive = false;
        if (parentContexts != null) {
            parentsTerminated = allParentTerminated(parentContexts);
            allParentsActive = allParentActive(parentContexts);
        }

        if (( terminationList.isEmpty() || allDependentTerminated) &&
                (parentContexts.isEmpty() || parentsTerminated || allParentsActive)) {
            //Find the non existent monitor by traversing dependency tree
            try {
                this.startDependencyOnTermination();
            } catch (TopologyInConsistentException e) {
                //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
                log.error("Error while starting the monitor upon termination" + e);
            }
        } else {
            StatusChecker.getInstance().onChildStatusChange(eventId, this.id, this.appId);
            log.info("" +
                    "Checking the status of group/application as no dependent found...");
        }


    }

    private boolean allDependentTerminated(List<ApplicationChildContext> terminationList) {
        boolean allDependentTerminated = false;
        for (ApplicationChildContext context1 : terminationList) {
            if (this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                log.warn("Dependent [monitor] " + context1.getId() + " not in the correct state");
                allDependentTerminated = false;
                return allDependentTerminated;
            } else if (this.inactiveMonitorsList.contains(context1.getId())) {
                log.info("Waiting for the [dependent] " + context1.getId() + " to be terminated...");
                allDependentTerminated = false;
                return allDependentTerminated;
            } else {
                allDependentTerminated = true;
            }
        }
        return allDependentTerminated;
    }


    private boolean allParentTerminated(List<ApplicationChildContext> parentContexts) {
        boolean parentsTerminated = false;
        for (ApplicationChildContext context1 : parentContexts) {
            if (this.inactiveMonitorsList.contains(context1.getId())) {
                log.info("Waiting for the [Parent Monitor] " + context1.getId()
                        + " to be terminated");
                parentsTerminated = false;
                return parentsTerminated;
            } else if (this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                if (parentsTerminated) {
                    log.warn("Found the Dependent [monitor] " + context1.getId()
                            + " in the active list wrong state");
                }
            } else {
                log.info("[Parent Monitor] " + context1.getId()
                        + " has already been terminated");
                parentsTerminated = true;
            }
        }
        return parentsTerminated;
    }

    private boolean allParentActive(List<ApplicationChildContext> parentContexts) {
        boolean parentsActive = false;
        for (ApplicationChildContext context1 : parentContexts) {
            if (this.inactiveMonitorsList.contains(context1.getId()) ||
                    this.terminatingMonitorsList.contains(context1.getId())) {
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
                        if (monitor1.getStatus() == GroupStatus.Active) {
                            parentsActive = true;

                        }
                    } finally {
                        ApplicationHolder.releaseReadLock();
                    }
                } else if(monitor instanceof AbstractClusterMonitor) {
                    AbstractClusterMonitor monitor1 = (AbstractClusterMonitor)monitor;
                    TopologyManager.acquireReadLockForCluster(monitor1.getServiceId(),
                                                                monitor1.getClusterId());
                    try {
                        if(monitor1.getStatus() == ClusterStatus.Active) {
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

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     * //TODO restarting the whole group
     */
    public void startDependency() throws TopologyInConsistentException {
        //start the first dependency
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.getStarAbleDependencies();
        startDependency(applicationContexts);

    }

    /**
     * This will start the parallel dependencies at once from the top level
     * by traversing to find the terminated dependencies.
     * it will get invoked when start a child monitor on termination of a sub tree
     */
    public void startDependencyOnTermination() throws TopologyInConsistentException {
        //start the first dependency which went to terminated
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.
                getStarAbleDependenciesByTermination();
        startDependency(applicationContexts);

    }

    /**
     * This will get invoked based on the activation event of its one of the child
     *
     * @param id alias/clusterId of which receive the activated event
     */
    public boolean startDependency(String id) throws TopologyInConsistentException {
        List<ApplicationChildContext> applicationContexts = this.startupDependencyTree.getStarAbleDependencies(id);
        return startDependency(applicationContexts);
    }

    /**
     * To start the dependency of the given application contexts
     *
     * @param applicationContexts the found applicationContexts to be started
     */
    private boolean startDependency(List<ApplicationChildContext> applicationContexts)
            throws TopologyInConsistentException {
        if (applicationContexts != null && applicationContexts.isEmpty()) {
            //all the groups/clusters have been started and waiting for activation
            log.info("There is no child found for the [group]: " + this.id);
            return false;

        }
        for (ApplicationChildContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                //to avoid if it is already started
                context.setTerminated(false);
                startMonitor(this, context);
            }
        }

        return true;

    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    protected synchronized void startMonitor(ParentComponentMonitor parent, ApplicationChildContext context) {
        Thread th = null;
        if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
            th = new Thread(
                    new MonitorAdder(parent, context, this.appId));
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Monitor Adder has been added: [cluster] %s ",
                                context.getId()));
            }
        }
        if (th != null) {
            th.start();
            log.info(String
                    .format("Monitor thread has been started successfully: [cluster] %s ",
                            context.getId()));
        }
    }

    private class MonitorAdder implements Runnable {
        private ApplicationChildContext context;
        private ParentComponentMonitor parent;
        private String appId;

        public MonitorAdder(ParentComponentMonitor parent, ApplicationChildContext context, String appId) {
            this.parent = parent;
            this.context = context;
            this.appId = appId;
        }

        public void run() {
            Monitor monitor = null;
            int retries = 5;
            boolean success;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }

                if (log.isDebugEnabled()) {
                    log.debug("Monitor is going to be started for [group/cluster] "
                            + context.getId());
                }
                try {
                    monitor = ApplicationMonitorFactory.getMonitor(parent, context, appId);
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

            } while (!success && retries != 0);


            if (monitor == null) {
                String msg = "Monitor creation failed, even after retrying for 5 times, "
                        + "for : " + context.getId();
                log.error(msg);
                //TODO parent.notify();
                throw new RuntimeException(msg);
            }
            aliasToActiveMonitorsMap.put(context.getId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Monitor has been added successfully for: %s",
                        context.getId()));
            }
        }
    }

}