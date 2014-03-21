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

package org.apache.stratos.cartridge.agent.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

/**
 * Health statistics notifier thread for publishing statistics periodically to CEP.
 */
public class HealthStatisticsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(HealthStatisticsNotifier.class);

    private final HealthStatisticsPublisher statsPublisher;
    private long statsPublisherInterval = 15000;
    private boolean terminated;

    public HealthStatisticsNotifier() {
        this.statsPublisher = new HealthStatisticsPublisher();

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

                    double memoryConsumption = HealthStatisticsReader.getMemoryConsumption();
                    if(log.isInfoEnabled()) {
                        log.info(String.format("Publishing memory consumption: %f", memoryConsumption));
                    }
                    statsPublisher.publish(
                            CartridgeAgentConfiguration.getInstance().getClusterId(),
                            CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                            CartridgeAgentConfiguration.getInstance().getMemberId(),
                            CartridgeAgentConfiguration.getInstance().getPartitionId(),
                            CartridgeAgentConstants.MEMORY_CONSUMPTION,
                            memoryConsumption
                    );

                    double loadAverage = HealthStatisticsReader.getLoadAverage();
                    if(log.isInfoEnabled()) {
                        log.info(String.format("Publishing load average: %f", loadAverage));
                    }
                    statsPublisher.publish(
                            CartridgeAgentConfiguration.getInstance().getClusterId(),
                            CartridgeAgentConfiguration.getInstance().getNetworkPartitionId(),
                            CartridgeAgentConfiguration.getInstance().getMemberId(),
                            CartridgeAgentConfiguration.getInstance().getPartitionId(),
                            CartridgeAgentConstants.LOAD_AVERAGE,
                            loadAverage
                    );
                } else if (log.isWarnEnabled()) {
                    log.warn("Statistics publisher is disabled");
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Could not publish health statistics", e);
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
