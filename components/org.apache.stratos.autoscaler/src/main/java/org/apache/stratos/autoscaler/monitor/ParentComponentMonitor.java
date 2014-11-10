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
import org.apache.stratos.autoscaler.applications.dependency.DependencyBuilder;
import org.apache.stratos.autoscaler.applications.dependency.DependencyTree;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.event.publisher.ClusterStatusEventPublisher;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitorFactory;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.applications.ParentComponent;

import java.util.HashMap;
import java.util.List;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class ParentComponentMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ParentComponentMonitor.class);

    //The monitors dependency tree with all the start-able/kill-able dependencies
    protected DependencyTree dependencyTree;

    public ParentComponentMonitor(ParentComponent component) throws DependencyBuilderException {
        aliasToActiveMonitorsMap = new HashMap<String, Monitor>();
        aliasToInactiveMonitorsMap = new HashMap<String, Monitor>();
        //clusterIdToClusterMonitorsMap = new HashMap<String, AbstractClusterMonitor>();
        this.id = component.getUniqueIdentifier();
        //Building the dependency for this monitor within the immediate children
        dependencyTree = DependencyBuilder.getInstance().buildDependency(component);
    }

    /**
     * This will start the child monitors based on the active of siblings according to start up order
     *
     * @param idOfEvent parent id of the event which received
     */
    protected void onChildActivatedEvent(String idOfEvent) {
        try {
            //if the activated monitor is in in_active map move it to active map
            if (this.aliasToInactiveMonitorsMap.containsKey(idOfEvent)) {
                this.aliasToActiveMonitorsMap.put(id, this.aliasToInactiveMonitorsMap.remove(idOfEvent));
            }
            boolean startDep = startDependency(idOfEvent);
            if (log.isDebugEnabled()) {
                log.debug("started a child: " + startDep + " by the group/cluster: " + idOfEvent);

            }
            if (!startDep) {
                StatusChecker.getInstance().onChildStatusChange(idOfEvent, this.id, this.appId);
            }
        } catch (TopologyInConsistentException e) {
            //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
            log.error(e);
        }

    }

    /*protected void onChildTerminatingEvent(String idOfEvent) {
        //Check whether hasDependent true
        if (!this.aliasToInactiveMonitorsMap.containsKey(idOfEvent)) {
            this.aliasToInactiveMonitorsMap.put(idOfEvent, this.aliasToActiveMonitorsMap.remove(idOfEvent));
        }

        Monitor monitor = this.aliasToInactiveMonitorsMap.get(idOfEvent);
        if (monitor != null) {
            // check if aliasToActiveMonitors are null (in case of a Cluster Monitor)
            if (monitor.getAliasToActiveMonitorsMap() != null) {
                for (Monitor monitor1 : monitor.getAliasToActiveMonitorsMap().values()) {
                    if (monitor.hasActiveMonitors()) {
                        StatusEventPublisher.sendGroupTerminatingEvent(this.appId, monitor1.getId());
                    } else {
                        StatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                                ((AbstractClusterMonitor) monitor1).getServiceId(), monitor.getId());
                    }
                }
            }
        } else {
            log.warn("Inactive Monitor not found for the id " + idOfEvent);
        }
    }*/

    // move to inactive monitors list to use in the Terminated event
    protected synchronized void markMonitorAsInactive(String monitorKey) {

        if (!this.aliasToInactiveMonitorsMap.containsKey(monitorKey)) {
            this.aliasToInactiveMonitorsMap.put(monitorKey,
                    this.aliasToActiveMonitorsMap.remove(monitorKey));
        }
    }

    /**
     * @param idOfEvent
     */
    protected void onChildInactiveEvent(String idOfEvent) {
        List<ApplicationContext> terminationList;
        Monitor monitor;

        if (this.hasDependent) {
            //need to notify the parent
            StatusChecker.getInstance().onChildStatusChange(idOfEvent, this.id, this.appId);
        } else {
            terminationList = this.dependencyTree.getTerminationDependencies(idOfEvent);
            //Checking whether all children are to be terminated.
            if (terminationList.size() ==
                    (this.aliasToActiveMonitorsMap.size() + this.aliasToInactiveMonitorsMap.size())) {
                if (this.parent != null) {
                    ApplicationBuilder.handleGroupTerminatingEvent(this.appId, this.id);
                }
            } else {
                //TODO application InActive
                if (this.parent != null) {
                    ApplicationBuilder.handleGroupInActivateEvent(this.appId, this.id);
                }
                //Since it is reached the most independent unit and has few independent monitors,
                // has to put the children down to terminating

                if (terminationList != null) {
                    //Checking the termination dependents status
                    for (ApplicationContext terminationContext : terminationList) {
                        //Check whether dependent is in_active, then start to kill it
                        monitor = this.aliasToActiveMonitorsMap.
                                get(terminationContext.getId());
                        //start to kill it
                        if (monitor != null) {
                            if (monitor.hasActiveMonitors()) {
                                //it is a group
                                ApplicationBuilder.handleGroupTerminatingEvent(this.appId,
                                        terminationContext.getId());
                            } else {
                                ClusterStatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                                        ((AbstractClusterMonitor) monitor).getServiceId(),
                                        terminationContext.getId());
                            }
                        } else {
                            log.warn("The relevant [monitor] " + terminationContext.getId() +
                                    "is not in the active map....");
                        }

                    }
                } else {
                    log.warn("Wrong inActive event received from [Child] " + idOfEvent +
                            "  to the [parent]" + " where child is identified as a independent");
                }
            }


        }

    }

    protected void onChildTerminatedEvent(String idOfEvent) {
        List<ApplicationContext> terminationList;
        boolean allDependentTerminated = false;

        ApplicationContext context = this.dependencyTree.findApplicationContextWithId(idOfEvent);
        context.setTerminated(true);

        terminationList = this.dependencyTree.getTerminationDependencies(idOfEvent);


        /**
         * Make sure that all the dependents have been terminated properly to start the recovery
         */
        if (terminationList != null) {
            allDependentTerminated = allDependentTerminated(terminationList);
        }

        List<ApplicationContext> parentContexts = this.dependencyTree.findAllParentContextWithId(idOfEvent);
        boolean parentsTerminated = false;
        if (parentContexts != null) {
            parentsTerminated = allParentTerminated(parentContexts);
        }

        if ((terminationList != null && allDependentTerminated || terminationList.isEmpty()) &&
                (parentContexts != null && parentsTerminated || parentContexts.isEmpty())) {
            //Find the non existent monitor by traversing dependency tree
            try {
                this.startDependencyOnTermination();
                List<ApplicationContext> applicationContexts = this.dependencyTree.
                        getStarAbleDependenciesByTermination();
            } catch (TopologyInConsistentException e) {
                //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
                log.error("Error while starting the monitor upon termination" + e);
            }
        } else {
            StatusChecker.getInstance().onChildStatusChange(idOfEvent, this.id, this.appId);
            log.info("" +
                    "Checking the status of group/application as no dependent found...");
        }


    }

    private boolean allDependentTerminated(List<ApplicationContext> terminationList) {
        boolean allDependentTerminated = false;
        for (ApplicationContext context1 : terminationList) {
            if (this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                log.warn("Dependent [monitor] " + context1.getId() + " not in the correct state");
                allDependentTerminated = false;
                return allDependentTerminated;
            } else if (this.aliasToInactiveMonitorsMap.containsKey(context1.getId())) {
                log.info("Waiting for the [dependent] " + context1.getId() + " to be terminated...");
                allDependentTerminated = false;
                return allDependentTerminated;
            } else {
                allDependentTerminated = true;
            }
        }
        return allDependentTerminated;
    }


    private boolean allParentTerminated(List<ApplicationContext> parentContexts) {
        boolean parentsTerminated = false;
        for (ApplicationContext context1 : parentContexts) {
            if (this.aliasToInactiveMonitorsMap.containsKey(context1.getId())) {
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

    /**
     * This will start the parallel dependencies at once from the top level.
     * it will get invoked when the monitor starts up only.
     * //TODO restarting the whole group
     */
    public void startDependency() throws TopologyInConsistentException {
        //start the first dependency
        List<ApplicationContext> applicationContexts = this.dependencyTree.getStarAbleDependencies();
        startDependency(applicationContexts);

    }

    /**
     * This will start the parallel dependencies at once from the top level
     * by traversing to find the terminated dependencies.
     * it will get invoked when start a child monitor on termination of a sub tree
     */
    public void startDependencyOnTermination() throws TopologyInConsistentException {
        //start the first dependency which went to terminated
        List<ApplicationContext> applicationContexts = this.dependencyTree.
                getStarAbleDependenciesByTermination();
        startDependency(applicationContexts);

    }

    /**
     * This will get invoked based on the activation event of its one of the child
     *
     * @param id alias/clusterId of which receive the activated event
     */
    public boolean startDependency(String id) throws TopologyInConsistentException {
        List<ApplicationContext> applicationContexts = this.dependencyTree.getStarAbleDependencies(id);
        return startDependency(applicationContexts);
    }

    /**
     * To start the dependency of the given application contexts
     *
     * @param applicationContexts the found applicationContexts to be started
     */
    private boolean startDependency(List<ApplicationContext> applicationContexts)
            throws TopologyInConsistentException {
        if (applicationContexts != null && applicationContexts.isEmpty()) {
            //all the groups/clusters have been started and waiting for activation
            log.info("There is no child found for the [group]: " + this.id);
            return false;

        }
        for (ApplicationContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            if (!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
                //to avoid if it is already started
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

    protected synchronized void startMonitor(ParentComponentMonitor parent, ApplicationContext context) {
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
        private ApplicationContext context;
        private ParentComponentMonitor parent;
        private String appId;

        public MonitorAdder(ParentComponentMonitor parent, ApplicationContext context, String appId) {
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
