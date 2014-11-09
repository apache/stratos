/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.load.balancer.common.statistics.notifier;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.statistics.publisher.WSO2CEPInFlightRequestPublisher;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer statistics notifier thread for publishing statistics periodically to CEP.
 */
public class LoadBalancerStatisticsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(LoadBalancerStatisticsNotifier.class);

    private final LoadBalancerStatisticsReader statsReader;
    private final WSO2CEPInFlightRequestPublisher inFlightRequestPublisher;
    private long statsPublisherInterval = 15000;
    private String networkPartitionId;
    private boolean terminated;

    public LoadBalancerStatisticsNotifier(LoadBalancerStatisticsReader statsReader) {
        this.statsReader = statsReader;
        this.inFlightRequestPublisher = new WSO2CEPInFlightRequestPublisher();

        String interval = System.getProperty("stats.notifier.interval");
        if (interval != null) {
            statsPublisherInterval = Long.getLong(interval);
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("stats.notifier.interval: %dms", statsPublisherInterval));
        }

        networkPartitionId = System.getProperty("network.partition.id");
        if (StringUtils.isBlank(networkPartitionId)) {
            throw new RuntimeException("network.partition.id system property was not found.");
        }
    }

    @Override
    public void run() {
        while (!terminated) {
            try {
                try {
                    Thread.sleep(statsPublisherInterval);
                } catch (InterruptedException ignore) {
                }

                if (log.isDebugEnabled()) {
                    log.debug("Publishing load balancer statistics");
                }
                if (inFlightRequestPublisher.isEnabled()) {
                    try {
                        TopologyManager.acquireReadLock();
                        int requestCount;
                        int servedRequestCount;
                        int activeInstancesCount;
                        for (Service service : TopologyManager.getTopology().getServices()) {
                            for (Cluster cluster : service.getClusters()) {
                                if (!cluster.isLbCluster()) {
                                    // Publish in-flight request count of load balancer's network partition
                                    requestCount = statsReader.getInFlightRequestCount(cluster.getClusterId());
                                    servedRequestCount = statsReader.getServedRequestCount(cluster.getClusterId());
                                    if(requestCount == 0) {
                                        servedRequestCount = 0;
                                    }
                                    activeInstancesCount = statsReader.getActiveInstancesCount(cluster);
                                    inFlightRequestPublisher.publish(cluster.getClusterId(), networkPartitionId,activeInstancesCount, requestCount, servedRequestCount);
                                    log.info(String.format("In-flight request count published to cep: [cluster-id] %s [network-partition] %s [value] %d [active instances] %d [RIF] %d ",
                                            cluster.getClusterId(), networkPartitionId, servedRequestCount , activeInstancesCount ,requestCount ));
                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("In-flight request count published to cep: [cluster-id] %s [network-partition] %s [value] %d",
                                                cluster.getClusterId(), networkPartitionId, requestCount));
                                    }
                                }
                                else {
                                    // Load balancer cluster found in topology; we do not need to publish request counts for them.
                                }
                            }

                        }
                    } finally {
                        TopologyManager.releaseReadLock();
                    }
                } else if (log.isWarnEnabled()) {
                    log.warn("In-flight request count publisher is disabled");
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Could not publish load balancer statistics", e);
                }
            }
        }
    }

    /**
     * Terminate load balancer statistics notifier thread.
     */
    public void terminate() {
        terminated = true;
    }
}
