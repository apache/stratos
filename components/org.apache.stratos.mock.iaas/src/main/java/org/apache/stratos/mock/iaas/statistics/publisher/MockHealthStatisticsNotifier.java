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

package org.apache.stratos.mock.iaas.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.statistics.publisher.*;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.exceptions.NoStatisticsFoundException;
import org.apache.stratos.mock.iaas.services.impl.MockScalingFactor;
import org.apache.stratos.mock.iaas.statistics.MockHealthStatistics;

/**
 * Health statistics notifier thread for publishing statistics periodically to CEP.
 */
public class MockHealthStatisticsNotifier implements Runnable {
    private static final Log log = LogFactory.getLog(MockHealthStatisticsNotifier.class);

    public static final String MEMORY_CONSUMPTION = "memory_consumption";
    public static final String LOAD_AVERAGE = "load_average";

    private final MockInstanceContext mockMemberContext;
    private final HealthStatisticsPublisher healthStatisticsPublisher;
    private final InFlightRequestPublisher inFlightRequestPublisher;

    public MockHealthStatisticsNotifier(MockInstanceContext mockMemberContext) {
        this.mockMemberContext = mockMemberContext;
        this.healthStatisticsPublisher = HealthStatisticsPublisherFactory.createHealthStatisticsPublisher(
                StatisticsPublisherType.WSO2CEP);
        this.healthStatisticsPublisher.setEnabled(true);

        this.inFlightRequestPublisher = InFlightRequestPublisherFactory.createInFlightRequestPublisher(
                StatisticsPublisherType.WSO2CEP);
        this.inFlightRequestPublisher.setEnabled(true);
    }

    @Override
    public void run() {
        if (!healthStatisticsPublisher.isEnabled()) {
            if (log.isWarnEnabled()) {
                log.warn("Statistics publisher is disabled");
            }
            return;
        }

        try {
            double memoryConsumption = MockHealthStatistics.getInstance().getStatistics(
                    mockMemberContext.getServiceName(), MockScalingFactor.MemoryConsumption);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Publishing memory consumption: [member-id] %s [value] %f",
                        mockMemberContext.getMemberId(), memoryConsumption));
            }
            healthStatisticsPublisher.publish(
                    System.currentTimeMillis(),
                    mockMemberContext.getClusterId(),
                    mockMemberContext.getClusterInstanceId(),
                    mockMemberContext.getNetworkPartitionId(),
                    mockMemberContext.getMemberId(),
                    mockMemberContext.getPartitionId(),
                    MEMORY_CONSUMPTION,
                    memoryConsumption
            );
        } catch (NoStatisticsFoundException ignore) {
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not publish health statistic: memory consumption", e);
            }
        }


        try {
            double loadAvereage = MockHealthStatistics.getInstance().getStatistics(
                    mockMemberContext.getServiceName(), MockScalingFactor.LoadAverage);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Publishing load average: [member-id] %s [value] %f",
                        mockMemberContext.getMemberId(), loadAvereage));
            }
            healthStatisticsPublisher.publish(
                    System.currentTimeMillis(),
                    mockMemberContext.getClusterId(),
                    mockMemberContext.getClusterInstanceId(),
                    mockMemberContext.getNetworkPartitionId(),
                    mockMemberContext.getMemberId(),
                    mockMemberContext.getPartitionId(),
                    LOAD_AVERAGE,
                    loadAvereage
            );
        } catch (NoStatisticsFoundException ignore) {
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not publish health statistic: load average", e);
            }
        }

        try {
            int requestsInFlight = MockHealthStatistics.getInstance().getStatistics(
                    mockMemberContext.getServiceName(), MockScalingFactor.RequestsInFlight);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Publishing requests in flight: [member-id] %s [value] %f",
                        mockMemberContext.getMemberId(), requestsInFlight));
            }
            inFlightRequestPublisher.publish(
                    System.currentTimeMillis(),
                    mockMemberContext.getClusterId(),
                    mockMemberContext.getClusterInstanceId(),
                    mockMemberContext.getNetworkPartitionId(),
                    requestsInFlight);
        } catch (NoStatisticsFoundException ignore) {
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not publish health statistic: requests in flight", e);
            }
        }
    }
}
