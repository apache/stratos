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

package org.apache.stratos.cloud.controller.iaases.mock.statistics.generator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.iaases.mock.config.MockIaasConfig;
import org.apache.stratos.common.threading.StratosThreadPool;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock health statistics generator.
 */
public class MockHealthStatisticsGenerator {

    private static final Log log = LogFactory.getLog(MockHealthStatisticsGenerator.class);

    private static ScheduledExecutorService scheduledExecutorService =
            StratosThreadPool.getScheduledExecutorService("MOCK_STATISTICS_GENERATOR_EXECUTOR_SERVICE", 100);
    private static boolean scheduled;

    public static void scheduleStatisticsUpdaters() {
        if(!scheduled) {
            synchronized (MockHealthStatisticsGenerator.class) {
                if(!scheduled) {
                    List<MockHealthStatisticsPattern> statisticsPatterns = MockIaasConfig.getInstance().
                            getMockHealthStatisticsConfig().getStatisticsPatterns();

                    for (MockHealthStatisticsPattern statisticsPattern : statisticsPatterns) {
                        scheduledExecutorService.scheduleAtFixedRate(new MockHealthStatisticsUpdater(statisticsPattern), 0,
                                statisticsPattern.getSampleDuration(), TimeUnit.SECONDS);
                    }

                    if (log.isInfoEnabled()) {
                        log.info("Mock statistics updaters scheduled");
                    }
                    scheduled = true;
                }
            }
        }
    }

    public static void stopStatisticsUpdaters() {
        synchronized (MockHealthStatisticsGenerator.class) {
            scheduledExecutorService.shutdownNow();
        }
    }

    public static boolean isScheduled() {
        return scheduled;
    }
}
