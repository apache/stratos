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

package org.apache.stratos.cloud.controller.iaases.mock.statistics;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.iaases.mock.MockMemberContext;

/**
 * Health statistics notifier thread for publishing statistics periodically to CEP.
 */
public class MockHealthStatisticsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(MockHealthStatisticsNotifier.class);

    public static final String MEMORY_CONSUMPTION = "memory_consumption";
    public static final String LOAD_AVERAGE = "load_average";

    private final MockMemberContext mockMemberContext;
    private final MockHealthStatisticsPublisher statsPublisher;
    private final double memoryConsumption = 20.0;
    private final double loadAvereage = 40.0;

    public MockHealthStatisticsNotifier(MockMemberContext mockMemberContext) {
        this.mockMemberContext = mockMemberContext;
        this.statsPublisher = new MockHealthStatisticsPublisher();
        this.statsPublisher.setEnabled(true);
    }

    @Override
    public void run() {
        try {
            if (statsPublisher.isEnabled()) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Publishing memory consumption: %f", memoryConsumption));
                }
                statsPublisher.publish(
                        mockMemberContext.getClusterId(),
                        mockMemberContext.getNetworkPartitionId(),
                        mockMemberContext.getMemberId(),
                        mockMemberContext.getPartitionId(),
                        MEMORY_CONSUMPTION,
                        memoryConsumption
                );

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Publishing load average: %f", loadAvereage));
                }
                statsPublisher.publish(
                        mockMemberContext.getClusterId(),
                        mockMemberContext.getNetworkPartitionId(),
                        mockMemberContext.getMemberId(),
                        mockMemberContext.getPartitionId(),
                        LOAD_AVERAGE,
                        loadAvereage
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
