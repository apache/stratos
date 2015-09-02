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
import org.apache.stratos.common.statistics.publisher.InFlightRequestPublisher;
import org.apache.stratos.common.statistics.publisher.InFlightRequestPublisherFactory;
import org.apache.stratos.common.statistics.publisher.StatisticsPublisherType;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.domain.Service;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;

/**
 * Load balancer statistics notifier thread for publishing statistics periodically to CEP.
 */
public class LoadBalancerStatisticsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(LoadBalancerStatisticsNotifier.class);

    private final LoadBalancerStatisticsReader statsReader;
    private final TopologyProvider topologyProvider;
    private final InFlightRequestPublisher inFlightRequestPublisher;
    private long statsPublisherInterval = 15000;
    private String networkPartitionId;
    private boolean terminated;

    public LoadBalancerStatisticsNotifier(LoadBalancerStatisticsReader statsReader, TopologyProvider topologyProvider) {
        this.statsReader = statsReader;
        this.topologyProvider = topologyProvider;
        this.inFlightRequestPublisher = InFlightRequestPublisherFactory.createInFlightRequestPublisher(
                StatisticsPublisherType.WSO2CEP);

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
                    String clusterInstanceId = statsReader.getClusterInstanceId();
                    for (Service service : topologyProvider.getTopology().getServices()) {
                        for (Cluster cluster : service.getClusters()) {
                            // Publish in-flight request count of load balancer's network partition
                            int requestCount = statsReader.getInFlightRequestCount(cluster.getClusterId());
                            inFlightRequestPublisher.publish(System.currentTimeMillis(), cluster.getClusterId(),
                                    clusterInstanceId,
                                    networkPartitionId, requestCount);

                            if (log.isDebugEnabled()) {
                                log.debug(String.format("In-flight request count published to cep: [cluster-id] %s " +
                                                "[cluster-instance-id] %s [network-partition] %s [value] %d ",
                                        cluster.getClusterId(), clusterInstanceId, networkPartitionId, requestCount));
                            }
                        }
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
