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

package org.apache.stratos.load.balancer.extension.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.statistics.WSO2CEPInFlightRequestPublisher;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.Collection;

/**
 * Load balancer statistics notifier thread for publishing statistics periodically to CEP.
 */
public class LoadBalancerInFlightRequestCountNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(LoadBalancerInFlightRequestCountNotifier.class);

    private LoadBalancerStatsReader statsReader;
    private final WSO2CEPInFlightRequestPublisher statsPublisher;
    private long statsPublisherInterval = 15000;
    private boolean terminated;

    public LoadBalancerInFlightRequestCountNotifier(LoadBalancerStatsReader statsReader) {
        this.statsReader = statsReader;
        this.statsPublisher = new WSO2CEPInFlightRequestPublisher();

        String interval = System.getProperty("stats.notifier.interval");
        if (interval != null) {
            statsPublisherInterval = Long.getLong(interval);
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

                if (statsPublisher.isEnabled()) {
                    try {
                        TopologyManager.acquireReadLock();
                        Collection<String> partitionIds;
                        int requestCount;
                        for (Service service : TopologyManager.getTopology().getServices()) {
                            for (Cluster cluster : service.getClusters()) {
                                partitionIds =  cluster.findPartitionIds();
                                for(String partitionId : partitionIds) {
                                    // Publish in-flight request count of each cluster partition
                                    requestCount = statsReader.getInFlightRequestCount(cluster.getClusterId(), partitionId);
                                    statsPublisher.publish(cluster.getClusterId(), partitionId, requestCount);
                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("In-flight request count published to cep: [cluster-id] %s [partition] %s [value] %d",
                                                cluster.getClusterId(), partitionId, requestCount));
                                    }
                                }
                            }
                        }
                    }
                    finally {
                        TopologyManager.releaseReadLock();
                    }
                } else if (log.isWarnEnabled()) {
                    log.warn("CEP statistics publisher is disabled");
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Could not publish in-flight request count", e);
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
