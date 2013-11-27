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
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatsPublisher;
import org.apache.stratos.load.balancer.common.statistics.WSO2CEPStatsPublisher;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Load balancer statistics notifier thread for publishing statistics periodically to CEP.
 */
public class LoadBalancerStatsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(LoadBalancerStatsNotifier.class);

    private LoadBalancerStatsReader statsReader;
    private final LoadBalancerStatsPublisher statsPublisher;
    private long statsPublisherInterval = 15000;
    private boolean terminated;

    public LoadBalancerStatsNotifier(LoadBalancerStatsReader statsReader) {
        this.statsReader = statsReader;
        this.statsPublisher = new WSO2CEPStatsPublisher();

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
                Map<String, Integer> stats = new HashMap<String, Integer>();
                for (Service service : TopologyManager.getTopology().getServices()) {
                    for (Cluster cluster : service.getClusters()) {
                        stats.put(cluster.getClusterId(), statsReader.getInFlightRequestCount(cluster.getClusterId()));
                    }
                }
                if (stats.size() > 0) {
                    if(statsPublisher.isEnabled()) {
                        statsPublisher.publish(stats);
                    }
                    else if (log.isWarnEnabled()) {
                        log.warn("Load balancer statistics publisher is disabled");
                    }
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Could not publish load balancer stats", e);
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
