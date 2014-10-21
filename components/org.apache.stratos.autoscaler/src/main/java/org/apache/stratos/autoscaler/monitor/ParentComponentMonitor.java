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
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.TopologyInConsistentException;
import org.apache.stratos.autoscaler.grouping.dependency.DependencyBuilder;
import org.apache.stratos.autoscaler.grouping.dependency.DependencyTree;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.grouping.topic.StatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.GroupStatus;
import org.apache.stratos.messaging.domain.topology.ParentComponent;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.HashMap;
import java.util.List;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class ParentComponentMonitor extends Monitor {
    private static final Log log = LogFactory.getLog(ParentComponentMonitor.class);

    //id of the monitor, it can be alias or the id
    //protected String id;
    //The monitors dependency tree with all the startable/killable dependencies
    protected DependencyTree dependencyTree;
    //Application id of this particular monitor
    //protected String appId;

    public ParentComponentMonitor(ParentComponent component) throws DependencyBuilderException {
        aliasToActiveMonitorsMap = new HashMap<String, Monitor>();
        //clusterIdToClusterMonitorsMap = new HashMap<String, AbstractClusterMonitor>();
        this.id = component.getUniqueIdentifier();
        //Building the dependency for this monitor within the immediate children
        dependencyTree = DependencyBuilder.getInstance().buildDependency(component);
    }

    /**
     * Will monitor the immediate children upon any event triggers from parent/children
     *
     * @param statusEvent will be sent by parent/children with the current status
     */
    protected abstract void monitor(MonitorStatusEvent statusEvent);


    protected void onChildActivatedEvent(MonitorStatusEvent statusEvent) {
        try {
            //if the activated monitor is in in_active map move it to active map
            if (this.aliasToInActiveMonitorsMap.containsKey(id)) {
                this.aliasToActiveMonitorsMap.put(id, this.aliasToInActiveMonitorsMap.remove(id));
            }
            boolean startDep = startDependency(statusEvent.getId());
            if (log.isDebugEnabled()) {
                log.debug("started a child: " + startDep + " by the group/cluster: " + id);

            }
            if (!startDep) {
                StatusChecker.getInstance().onChildStatusChange(id, this.id, this.appId);
            }
        } catch (TopologyInConsistentException e) {
            //TODO revert the siblings and notify parent, change a flag for reverting/un-subscription
            log.error(e);
        }

    }

    protected void onChildTerminatingEvent() {
        //Check whether hasDependent true
        if (!this.aliasToInActiveMonitorsMap.containsKey(id)) {
            this.aliasToInActiveMonitorsMap.put(id, this.aliasToActiveMonitorsMap.remove(id));
        }

        Monitor monitor = this.aliasToInActiveMonitorsMap.get(id);
        for (Monitor monitor1 : monitor.getAliasToActiveMonitorsMap().values()) {
            if (monitor.hasMonitors()) {
                StatusEventPublisher.sendGroupTerminatingEvent(this.appId, monitor1.getId());
            } else {
                StatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                        ((AbstractClusterMonitor) monitor1).getServiceId(), monitor.getId());
            }
        }
    }

    protected void onChildInActiveEvent() {
        List<ApplicationContext> terminationList;
        Monitor monitor;
        terminationList = this.dependencyTree.getTerminationDependencies(id);
        //Temporarily move the group/cluster to inactive list
        this.aliasToInActiveMonitorsMap.put(id, this.aliasToActiveMonitorsMap.remove(id));

        if (terminationList != null) {
            //Checking the termination dependents status
            for (ApplicationContext terminationContext : terminationList) {
                //Check whether dependent is in_active, then start to kill it
                monitor = this.aliasToActiveMonitorsMap.
                        get(terminationContext.getId());
                //start to kill it
                if (monitor.hasMonitors()) {
                    //it is a group
                    StatusEventPublisher.sendGroupTerminatingEvent(this.appId, terminationContext.getId());
                } else {
                    StatusEventPublisher.sendClusterTerminatingEvent(this.appId,
                            ((AbstractClusterMonitor) monitor).getServiceId(), terminationContext.getId());

                }
            }
        } else {
            log.warn("Wrong inActive event received from [Child] " + id + "  to the [parent]"
                    + " where child is identified as a independent");
        }
    }

    protected void onChildTerminatedEvent() {
        List<ApplicationContext> terminationList;
        boolean allDependentTerminated = true;
        terminationList = this.dependencyTree.getTerminationDependencies(id);
        if (terminationList != null) {
            for (ApplicationContext context1 : terminationList) {
                if (this.aliasToInActiveMonitorsMap.containsKey(context1.getId())) {
                    log.info("Waiting for the [Parent Monitor] " + context1.getId()
                            + " to be terminated");
                    allDependentTerminated = false;
                } else if (this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                    log.warn("Dependent [monitor] " + context1.getId() + " not in the correct state");
                    allDependentTerminated = false;
                } else {
                    allDependentTerminated = true;
                }
            }

            if (allDependentTerminated) {

            }
        } else {
            List<ApplicationContext> parentContexts = this.dependencyTree.findAllParentContextWithId(id);
            boolean canStart = false;
            if (parentContexts != null) {
                for (ApplicationContext context1 : parentContexts) {
                    if (this.aliasToInActiveMonitorsMap.containsKey(context1.getId())) {
                        log.info("Waiting for the [Parent Monitor] " + context1.getId()
                                + " to be terminated");
                        canStart = false;
                    } else if (this.aliasToActiveMonitorsMap.containsKey(context1.getId())) {
                        if (canStart) {
                            log.warn("Found the Dependent [monitor] " + context1.getId()
                                    + " in the active list wrong state");
                        }
                    } else {
                        log.info("[Parent Monitor] " + context1.getId()
                                + " has already been terminated");
                        canStart = true;
                    }
                }

                if (canStart) {
                    //start the monitor
                }

            } else {
                //Start the monitor
            }

        }
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
            if(!this.aliasToActiveMonitorsMap.containsKey(context.getId())) {
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

            AutoscalerContext.getInstance().addMonitor(monitor);
            aliasToActiveMonitorsMap.put(context.getId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Monitor has been added successfully for: %s",
                        context.getId()));
            }
        }
    }


    /*protected synchronized void startGroupMonitor(ParentComponentMonitor parent, GroupContext groupContext) {
        Thread th = null;
        //String groupId = group.getUniqueIdentifier();
        if (!this.aliasToActiveMonitorsMap.containsKey(groupId)) {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Group monitor Adder has been added: [group] %s ",
                                groupId));
            }
            th = new Thread(
                    new GroupMonitorAdder(parent, groupId, this.appId));
        }

        if (th != null) {
            th.start();
            *//*try {
                th.join();
            } catch (InterruptedException ignore) {
            }*//*

            log.info(String
                    .format("Group monitor thread has been started successfully: [group] %s ",
                            groupId));
        }
    }
*/

    /*private Group getGroupFromTopology(String groupId) throws TopologyInConsistentException {
        Application application = TopologyManager.getTopology().getApplication(this.appId);
        if(application != null) {
            Group group = application.getGroupRecursively(groupId);
            if(group != null) {
                return group;
            } else {
                String msg = "[Group] " + groupId + " cannot be found in the Topology";
                throw new TopologyInConsistentException(msg);
            }
        } else {
            String msg = "[Application] " + this.appId + " cannot be found in the Topology";
            throw new TopologyInConsistentException(msg);
        }
    }*/

    /*protected synchronized void startClusterMonitor(ParentComponentMonitor parent, ApplicationContext clusterContext) {
        Thread th = null;
        if (!this.aliasToActiveMonitorsMap.containsKey(clusterContext.getId())) {
            th = new Thread(
                    new ClusterMonitorAdder(parent, clusterContext));
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Cluster monitor Adder has been added: [cluster] %s ",
                                clusterContext.getClusterId()));
            }
        }
        if (th != null) {
            th.start();
            log.info(String
                    .format("Cluster monitor thread has been started successfully: [cluster] %s ",
                            clusterContext.getClusterId()));
        }
    }*/


    /*public Map<String, AbstractClusterMonitor> getClusterIdToClusterMonitorsMap() {
        return clusterIdToClusterMonitorsMap;
    }

    public void setClusterIdToClusterMonitorsMap(Map<String, AbstractClusterMonitor> clusterIdToClusterMonitorsMap) {
        this.clusterIdToClusterMonitorsMap = clusterIdToClusterMonitorsMap;
    }

    public void addAbstractMonitor(AbstractClusterMonitor monitor) {
        this.clusterIdToClusterMonitorsMap.put(monitor.getClusterId(), monitor);
    }

    public AbstractClusterMonitor getAbstractMonitor(String clusterId) {
        return this.clusterIdToClusterMonitorsMap.get(clusterId);
    }
*/


    /*private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;
        private ParentComponentMonitor parent;

        public ClusterMonitorAdder(ParentComponentMonitor parent, Cluster cluster) {
            this.parent = parent;
            this.cluster = cluster;
        }

        public void run() {
            ClusterMonitor monitor = null;
            int retries = 5;
            boolean success = false;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("CLuster monitor is going to be started for [cluster] "
                                + cluster.getClusterId());
                    }
                    monitor = AutoscalerUtil.getClusterMonitor(cluster);
                    monitor.setParent(parent);
                    //setting the status of cluster monitor w.r.t Topology cluster
                    //if(cluster.getStatus() != Status.Created &&
                    if(cluster.getStatus() != monitor.getStatus()) {
                        //updating the status, so that it will notify the parent
                        monitor.setStatus(cluster.getStatus());
                    }
                    //monitor.addObserver(parent);
                    success = true;
                    //TODO start the status checker
                } catch (PolicyValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.warn(msg, e);
                    retries--;


                } catch (PartitionValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.warn(msg, e);
                    retries--;

                }

            } while (!success && retries != 0);


            if (monitor == null) {
                String msg = "Cluster monitor creation failed, even after retrying for 5 times, "
                        + "for cluster: " + cluster.getClusterId();
                log.error(msg);
                //TODO parent.notify();
                throw new RuntimeException(msg);
            }

            Thread th = new Thread(monitor);
            th.start();

            AutoscalerContext.getInstance().addMonitor(monitor);
            aliasToActiveMonitorsMap.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }
*/


    /*private class GroupMonitorAdder implements Runnable {
        private ParentComponentMonitor parent;
        private String groupId;
        private String appId;

        public GroupMonitorAdder(ParentComponentMonitor parent, String groupId, String appId) {
            this.parent = parent;
            this.groupId = groupId;
            this.appId = appId;
        }

        public void run() {
            GroupMonitor monitor = null;
            int retries = 5;
            boolean success = false;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }

                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Group monitor is going to be started for [group] "
                                + groupId );
                    }
                    monitor = AutoscalerUtil.getGroupMonitor(groupId, appId);
                    //setting the parent monitor
                    monitor.setParent(parent);
                    //setting the status of cluster monitor w.r.t Topology cluster
                    //if(group.getStatus() != Status.Created &&

                    //monitor.addObserver(parent);
                    success = true;
                } catch (DependencyBuilderException e) {
                    String msg = "Group monitor creation failed for group: " + groupId;
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Group monitor creation failed for group: " + groupId;
                    log.warn(msg, e);
                    retries--;
                }
            } while (!success && retries != 0);

            if (monitor == null) {
                String msg = "Group monitor creation failed, even after retrying for 5 times, "
                        + "for group: " + groupId;
                log.error(msg);
                //TODO parent.notify(); as it got to failed

                throw new RuntimeException(msg);
            }

            aliasToActiveMonitorsMap.put(groupId, monitor);
            //parent.addObserver(monitor);

            if (log.isInfoEnabled()) {
                log.info(String.format("Group monitor has been added successfully: [group] %s",
                        groupId));
            }
        }
    }

    private class LBClusterMonitorAdder implements Runnable {
        private Cluster cluster;

        public LBClusterMonitorAdder(Cluster cluster) {
            this.cluster = cluster;
        }

        public void run() {
            LbClusterMonitor monitor = null;
            int retries = 5;
            boolean success = false;
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }
                try {
                    monitor = AutoscalerUtil.getLBClusterMonitor(cluster);
                    success = true;

                } catch (PolicyValidationException e) {
                    String msg = "LB Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.warn(msg, e);
                    retries--;

                } catch (PartitionValidationException e) {
                    String msg = "LB Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.warn(msg, e);
                    retries--;
                }
            } while (!success && retries <= 0);

            if (monitor == null) {
                String msg = "LB Cluster monitor creation failed, even after retrying for 5 times, "
                        + "for cluster: " + cluster.getClusterId();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            Thread th = new Thread(monitor);
            th.start();
            AutoscalerContext.getInstance().addLbMonitor(monitor);
            aliasToActiveMonitorsMap.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("LB Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }*/


}
