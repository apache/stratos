/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */
package org.apache.stratos.autoscaler.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.ComplexApplicationContext;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.CompositeApplication;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;
import java.util.Map;

public class CompositeApplicationMonitor extends AbstractMonitor {
    private static final Log log = LogFactory.getLog(CompositeApplicationMonitor.class);
    private CompositeApplication compositeApplication;
    private Map<String, ClusterMonitor> clusterMonitors;
    //private Map<String, ClusterMonitor> activeClusterMonitors;
    //private Map<String, ClusterMonitor> pausedClusterMonitors;
    //cluster ids need to be checked upon the instance activation or group activation
    private List<String> clusterIds;


    private Map<String, LbClusterMonitor> lbClusterMonitors;
    private String appId;

    public CompositeApplicationMonitor(CompositeApplication compositeApplication) {
        this.compositeApplication = compositeApplication;
    }

    public void registerClusterMonitor() {
        clusterIds = compositeApplication.getClusterIds();
        for (String clusterId : clusterIds) {
            if (ComplexApplicationContext.checkStartupDependencies(clusterId)) {
                startClusterMonitor(TopologyManager.getTopology().getService("").getCluster(clusterId));
                clusterIds.remove(clusterId);
            } else {
                //need to wait as it has start up dependency
            }
        }
    }

    @Override
    public void run() {

        try {
            // TODO make this configurable,
            // this is the delay the min check of normal cluster monitor to wait until LB monitor is added
            Thread.sleep(60000);
        } catch (InterruptedException ignore) {
        }
        while (!isDestroyed()) {
            if (log.isDebugEnabled()) {
                log.debug("Cluster monitor is running.. " + this.toString());
            }
            try {
                if(clusterIds.size() > 0) {
                    registerClusterMonitor();
                }
                deRegisterClusterMonitorOnTermination();
            } catch (Exception e) {
                log.error("Cluster monitor: Monitor failed." + this.toString(), e);
            }
            try {
                // TODO make this configurable
                Thread.sleep(30000);
            } catch (InterruptedException ignore) {
            }
        }
    }

    public void deRegisterClusterMonitorOnTermination() {
        //need to remove the cluster monitor and add it to the pending list
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    protected synchronized void startClusterMonitor(Cluster cluster) {
        Thread th = null;
        if (cluster.isLbCluster()
                && !this.lbClusterMonitors.containsKey(cluster.getClusterId())) {
            th = new Thread(new LBClusterMonitorAdder(
                    cluster));
        } else if (!cluster.isLbCluster() && !this.clusterMonitors.containsKey(cluster.getClusterId())) {
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

    public void setLbClusterMonitors(Map<String, LbClusterMonitor> lbClusterMonitors) {
        this.lbClusterMonitors = lbClusterMonitors;
    }

    public void setClusterMonitors(Map<String, ClusterMonitor> clusterMonitors) {
        clusterMonitors = clusterMonitors;
    }

    public boolean clusterMonitorExists(String clusterId) {
       return clusterMonitors.containsKey(clusterId);
    }

    public boolean lbMonitorExists(String clusterId) {
        return lbClusterMonitors.containsKey(clusterId);
    }

    public LbClusterMonitor getLBclusterMonitor(String clusterId) {
       return lbClusterMonitors.get(clusterId);
    }

    public ClusterMonitor getClusterMonitor(String clusterId) {
        return clusterMonitors.get(clusterId);
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
            //AutoscalerContext.getInstance().addMonitor(monitor);
            clusterMonitors.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
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
            //AutoscalerContext.getInstance().addLbMonitor(monitor);
            lbClusterMonitors.put(cluster.getClusterId(), monitor);
            if (log.isInfoEnabled()) {
                log.info(String.format("LB Cluster monitor has been added successfully: [cluster] %s",
                        cluster.getClusterId()));
            }
        }
    }

}
