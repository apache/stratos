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
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.LbClusterMonitor;
import org.apache.stratos.autoscaler.monitor.group.GroupMonitor;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Group;
import org.apache.stratos.messaging.event.Event;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Monitor is to monitor it's child monitors and
 * control them according to the dependencies respectively.
 */
public abstract class Monitor implements Observer, Runnable {

    private static final Log log = LogFactory.getLog(Monitor.class);

    protected String id;

    protected Map<String, GroupMonitor> groupMonitors;
    protected Map<String, AbstractClusterMonitor> abstractClusterMonitors;
    protected Map<String, StatusChecker> statusCheckers;

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

    protected synchronized void startGroupMonitor(Group group) {
        Thread th = null;
        if (!this.groupMonitors.containsKey(group.getAlias())) {
            th = new Thread(
                    new GroupMonitorAdder(group));
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
                                group.getAlias()));
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
            do {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e1) {
                }

                try {
                    monitor = AutoscalerUtil.getClusterMonitor(cluster);
                    success = true;
                    //TODO start the status checker
                } catch (PolicyValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
                    retries--;

                } catch (PartitionValidationException e) {
                    String msg = "Cluster monitor creation failed for cluster: " + cluster.getClusterId();
                    log.debug(msg, e);
                    retries--;
                }
            } while (!success && retries != 0);

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
        private Group group;

        public GroupMonitorAdder(Group group) {
            this.group = group;
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
                    monitor = AutoscalerUtil.getGroupMonitor(group);
                    success = true;

                } catch (Exception e) {
                    String msg = "Group monitor creation failed for group: " + group.getAlias();
                    log.debug(msg, e);
                    retries--;

                }
            } while (!success && retries != 0);

            if (monitor == null) {
                String msg = "Group monitor creation failed, even after retrying for 5 times, "
                        + "for group: " + group.getAlias();
                log.error(msg);
                throw new RuntimeException(msg);
            }

            Thread th = new Thread(monitor);
            th.start();

            groupMonitors.put(group.getAlias(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Group monitor has been added successfully: [group] %s",
                        group.getAlias()));
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
