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
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.grouping.DependencyBuilder;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.LbClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.ParentBehavior;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.*;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class Monitor extends Observable implements Observer {

    private static final Log log = LogFactory.getLog(Monitor.class);

    protected String id;

    protected Map<String, GroupMonitor> groupMonitors;
    protected Map<String, AbstractClusterMonitor> abstractClusterMonitors;

    protected Queue<String> preOrderTraverse = new LinkedList<String>();

    protected ParentBehavior component;

    public Monitor(ParentBehavior component) {
        groupMonitors = new HashMap<String, GroupMonitor>();
        abstractClusterMonitors = new HashMap<String, AbstractClusterMonitor>();
        this.component = component;
        startDependency();
    }


    public Map<String, GroupMonitor> getGroupMonitors() {
        return groupMonitors;
    }

    public void setGroupMonitors(Map<String, GroupMonitor> groupMonitors) {
        this.groupMonitors = groupMonitors;
    }

    public Map<String, AbstractClusterMonitor> getAbstractClusterMonitors() {
        return abstractClusterMonitors;
    }

    public void addAbstractMonitor(AbstractClusterMonitor monitor) {
       this.abstractClusterMonitors.put(monitor.getClusterId(), monitor);
    }

    public AbstractClusterMonitor getAbstractMonitor(String clusterId) {
        return this.abstractClusterMonitors.get(clusterId);
    }

    public void setAbstractClusterMonitors(Map<String, AbstractClusterMonitor> abstractClusterMonitors) {
        this.abstractClusterMonitors = abstractClusterMonitors;
    }

    public abstract void monitor();

    @Override
    public void update(Observable observable, Object arg) {
        if(arg instanceof Event) {
            Event event = (Event) arg;
            if(log.isDebugEnabled()) {
                log.debug(String.format("Event received: %s", event.getClass().getName()));
            }
            onEvent(event);
        }
    }

    /**
     * Triggered when an event is received.
     * @param event
     */
    protected abstract void onEvent(Event event);

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void startDependency() {
        //Need to get the order every time as group/cluster might already been started
        //TODO breadth first search in a tree and find the parallel one
        //TODO build up the tree with ordered manner

        preOrderTraverse = DependencyBuilder.getStartupOrder(component);

        //start the first dependency
        if(!preOrderTraverse.isEmpty()) {
            String dependency = preOrderTraverse.poll();
            if (dependency.contains("group")) {
                startGroupMonitor(this, dependency.substring(6), component);
            } else if (dependency.contains("cartridge")) {
                ClusterDataHolder clusterDataHolder = component.getClusterData(dependency.substring(10));
                String clusterId = clusterDataHolder.getClusterId();
                String serviceName = clusterDataHolder.getServiceType();
                Cluster cluster = null;
                TopologyManager.acquireReadLock();
                cluster = TopologyManager.getTopology().getService(serviceName).getCluster(clusterId);
                TopologyManager.releaseReadLock();
                if (cluster != null) {
                    startClusterMonitor(cluster);
                } else {
                    //TODO throw exception since Topology is inconsistent
                }

            }
        } else {
            //all the groups/clusters have been started and waiting for activation
            log.info("All the groups/clusters of the [group]: " + this.id + " have been started.");
        }
    }
    protected synchronized void startClusterMonitor(Cluster cluster) {
        Thread th = null;
        if (cluster.isLbCluster()
                && !this.abstractClusterMonitors.containsKey(cluster.getClusterId())) {
            th = new Thread(new LBClusterMonitorAdder(
                    cluster));
        } else if (!cluster.isLbCluster() && !this.abstractClusterMonitors.containsKey(cluster.getClusterId())) {
            th = new Thread(
                    new ClusterMonitorAdder(cluster));
        }
        if (th != null) {
            th.start();
            try {
                th.join();
            } catch (InterruptedException ignore) {
            }

            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Cluster monitor thread has been started successfully: [cluster] %s ",
                                cluster.getClusterId()));
            }
        }
    }

    protected synchronized void startGroupMonitor(Monitor parent, String dependency, ParentBehavior component) {
        Thread th = null;
        if (!this.groupMonitors.containsKey(dependency)) {
            th = new Thread(
                    new GroupMonitorAdder(parent, dependency, component));
        }

        if (th != null) {
            th.start();
            try {
                th.join();
            } catch (InterruptedException ignore) {
            }

            if (log.isDebugEnabled()) {
                log.debug(String
                        .format("Group monitor thread has been started successfully: [group] %s ",
                                dependency));
            }
        }
    }


    private class ClusterMonitorAdder implements Runnable {
        private Cluster cluster;

        public ClusterMonitorAdder(Cluster cluster) {
            this.cluster = cluster;
        }

        public void run() {
            ClusterMonitor monitor = null;
            int retries = 5;
            boolean success = false;
            while (!success && retries != 0) {
                try {
                    monitor = AutoscalerUtil.getClusterMonitor(cluster);
                    success = true;
                    //TODO start the status checker
                } catch (PolicyValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
                    retries--;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                    }

                } catch (PartitionValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
                    retries--;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                    }
                }

            }

            if (monitor == null) {
                String msg = "Cluster monitor creation failed, even after retrying for 5 times, "
                        + "for cluster: " + cluster.getClusterId();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            Thread th = new Thread(monitor);
            th.start();

            AutoscalerContext.getInstance().addMonitor(monitor);
            abstractClusterMonitors.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }

    private class GroupMonitorAdder implements Runnable {
        private ParentBehavior group;
        private String dependency;
        private Monitor parent;

        public GroupMonitorAdder(Monitor parent, String dependency, ParentBehavior group) {
            this.group = group;
            this.dependency = dependency;
            this.parent = parent;
        }

        public void run() {
            GroupMonitor monitor = null;
            int retries = 5;
            boolean success = false;
            while (!success && retries != 0) {
                try {
                    monitor = AutoscalerUtil.getGroupMonitor(group.getGroup(dependency));
                    monitor.addObserver(parent);
                    success = true;

                } catch (Exception e) {
                    String msg = "Group monitor creation failed for group: " + dependency;
                    log.debug(msg, e);
                    retries--;
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e1) {
                    }

                }
            }

            if (monitor == null) {
                String msg = "Group monitor creation failed, even after retrying for 5 times, "
                        + "for group: " + dependency;
                log.error(msg);
                throw new RuntimeException(msg);
            }

            groupMonitors.put(dependency, monitor);
            parent.addObserver(monitor);

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
                    log.debug(msg, e);
                    retries--;

                } catch (PartitionValidationException e) {
                    String msg = "LB Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
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
            abstractClusterMonitors.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("LB Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }


}
