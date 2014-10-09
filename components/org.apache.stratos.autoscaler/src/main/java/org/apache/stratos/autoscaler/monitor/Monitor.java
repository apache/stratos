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
import org.apache.stratos.autoscaler.grouping.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.GroupContext;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.LbClusterMonitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class Monitor implements EventHandler {
    private static final Log log = LogFactory.getLog(Monitor.class);

    //id of the monitor, it can be alias or the id
    protected String id;
    //GroupMonitor map, key=GroupAlias and value=GroupMonitor
    protected Map<String, GroupMonitor> aliasToGroupMonitorsMap;
    //AbstractMonitor map, key=clusterId and value=AbstractMonitors
    protected Map<String, AbstractClusterMonitor> clusterIdToClusterMonitorsMap;
    //The monitors dependency tree with all the startable/killable dependencies
    protected DependencyTree dependencyTree;
    //application/group reference from the Topology
    protected ParentComponent component;
    //status of the monitor whether it is running/in_maintainable/terminated
    protected Status status;
    //Application id of this particular monitor
    protected String appId;

    public Monitor(ParentComponent component) throws DependencyBuilderException {
        aliasToGroupMonitorsMap = new HashMap<String, GroupMonitor>();
        clusterIdToClusterMonitorsMap = new HashMap<String, AbstractClusterMonitor>();
        this.component = component;
        //Building the dependency for this monitor within the immediate children
        dependencyTree = DependencyBuilder.getInstance().buildDependency(component);
    }

    /**
     * Will monitor the immediate children upon any event triggers from parent/children
     *
     * @param statusEvent will be sent by parent/children with the current status
     */
    protected abstract void monitor(MonitorStatusEvent statusEvent);


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
        if (applicationContexts == null) {
            //all the groups/clusters have been started and waiting for activation
            log.info("There is no child found for the [group]: " + this.id);
            return false;

        }
        for (ApplicationContext context : applicationContexts) {
            if (log.isDebugEnabled()) {
                log.debug("Dependency check for the Group " + context.getId() + " started");
            }
            if (context instanceof GroupContext) {
                startGroupMonitor(this, context.getId(), component);
            } else if (context instanceof ClusterContext) {
                String clusterId = context.getId();
                String serviceName = null;
                for(ClusterDataHolder dataHolder : component.getClusterDataMap().values()) {
                    if(dataHolder.getClusterId().equals(clusterId)) {
                        serviceName = dataHolder.getServiceType();
                    }
                }
                Cluster cluster;
                //acquire read lock for the service and cluster
                TopologyManager.acquireReadLockForCluster(clusterId, serviceName);
                try {
                    Topology topology = TopologyManager.getTopology();
                    if (topology.serviceExists(serviceName)) {
                        Service service = topology.getService(serviceName);
                        if (service.clusterExists(clusterId)) {
                            cluster = service.getCluster(clusterId);
                            if (log.isDebugEnabled()) {
                                log.debug("Dependency check starting the [cluster]" + clusterId);
                            }
                            startClusterMonitor(this, cluster);
                        } else {
                            String msg = "[Cluster] " + clusterId + " cannot be found in the " +
                                    "Topology for [service] " + serviceName;
                            throw new TopologyInConsistentException(msg);
                        }
                    } else {
                        String msg = "[Service] " + serviceName + " cannot be found in the Topology";
                        throw new TopologyInConsistentException(msg);

                    }
                } finally {
                    //release read lock for the service and cluster
                    TopologyManager.releaseReadLockForCluster(clusterId, serviceName);
                }
            }
        }
        return true;

    }

    protected synchronized void startClusterMonitor(Monitor parent, Cluster cluster) {
        Thread th = null;
        if (cluster.isLbCluster()
                && !this.clusterIdToClusterMonitorsMap.containsKey(cluster.getClusterId())) {
            th = new Thread(new LBClusterMonitorAdder(
                    cluster));
        } else if (!cluster.isLbCluster() && !this.clusterIdToClusterMonitorsMap.containsKey(cluster.getClusterId())) {
            th = new Thread(
                    new ClusterMonitorAdder(parent, cluster));
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Cluster monitor Adder has been added: [cluster] %s ",
                                cluster.getClusterId()));
            }
        }
        if (th != null) {
            th.start();
            /*try {
                th.join();
            } catch (InterruptedException ignore) {
            }*/

            log.info(String
                    .format("Cluster monitor thread has been started successfully: [cluster] %s ",
                            cluster.getClusterId()));
        }
    }

    protected synchronized void startGroupMonitor(Monitor parent, String dependency, ParentComponent component) {
        Thread th = null;
        if (!this.aliasToGroupMonitorsMap.containsKey(dependency)) {
            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Group monitor Adder has been added: [group] %s ",
                                dependency));
            }
            th = new Thread(
                    new GroupMonitorAdder(parent, dependency, component));
        }

        if (th != null) {
            th.start();
            /*try {
                th.join();
            } catch (InterruptedException ignore) {
            }*/

            log.info(String
                    .format("Group monitor thread has been started successfully: [group] %s ",
                            dependency));
        }
    }

    public Status getStatus() {
        return status;
    }

    public Map<String, GroupMonitor> getAliasToGroupMonitorsMap() {
        return aliasToGroupMonitorsMap;
    }

    public void setAliasToGroupMonitorsMap(Map<String, GroupMonitor> aliasToGroupMonitorsMap) {
        this.aliasToGroupMonitorsMap = aliasToGroupMonitorsMap;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, AbstractClusterMonitor> getClusterIdToClusterMonitorsMap() {
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

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;
        private Monitor parent;

        public ClusterMonitorAdder(Monitor parent, Cluster cluster) {
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
            clusterIdToClusterMonitorsMap.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }

    private class GroupMonitorAdder implements Runnable {
        private String dependency;
        private Monitor parent;
        private ParentComponent component;

        public GroupMonitorAdder(Monitor parent, String dependency, ParentComponent group) {
            this.dependency = dependency;
            this.parent = parent;
            this.component = group;
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
                                + dependency);
                    }
                    monitor = AutoscalerUtil.getGroupMonitor(component.getGroup(dependency));
                    monitor.setParent(parent);
                    //monitor.addObserver(parent);
                    success = true;
                } catch (DependencyBuilderException e) {
                    String msg = "Group monitor creation failed for group: " + dependency;
                    log.warn(msg, e);
                    retries--;
                } catch (TopologyInConsistentException e) {
                    String msg = "Group monitor creation failed for group: " + dependency;
                    log.warn(msg, e);
                    retries--;
                }
            } while (!success && retries != 0);

            if (monitor == null) {
                String msg = "Group monitor creation failed, even after retrying for 5 times, "
                        + "for group: " + dependency;
                log.error(msg);
                //TODO parent.notify(); as it got to failed

                throw new RuntimeException(msg);
            }

            aliasToGroupMonitorsMap.put(dependency, monitor);
            //parent.addObserver(monitor);

            if (log.isInfoEnabled()) {
                log.info(String.format("Group monitor has been added successfully: [group] %s",
                        dependency));
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
            clusterIdToClusterMonitorsMap.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("LB Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }


}
