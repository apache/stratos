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

package org.apache.stratos.mock.iaas.statistics.generator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.mock.iaas.config.MockIaasConfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mock health statistics generator.
 */
public class MockHealthStatisticsGenerator {

    private static final Log log = LogFactory.getLog(MockHealthStatisticsGenerator.class);

    private static volatile MockHealthStatisticsGenerator instance;
    private static final ScheduledExecutorService scheduledExecutorService =
            StratosThreadPool.getScheduledExecutorService("mock.iaas.health.statistics.generator.thread.pool", 10);

    // Map<ServiceName, Map<ScalingFactor, ScheduledFuture>>
    private Map<String, Map<String, ScheduledFuture>> serviceNameToTaskListMap;

    public static MockHealthStatisticsGenerator getInstance() {
        if (instance == null) {
            synchronized (MockHealthStatisticsGenerator.class) {
                if (instance == null) {
                    instance = new MockHealthStatisticsGenerator();
                }
            }
        }
        return instance;
    }

    /**
     * Private default constructor.
     */
    private MockHealthStatisticsGenerator() {
        serviceNameToTaskListMap = new ConcurrentHashMap<String, Map<String, ScheduledFuture>>();
    }

    /**
     * Schedule statistics updater tasks for the given service/cartridge type.
     *
     * @param serviceName service name/cartridge type
     */
    public void scheduleStatisticsUpdaterTasks(String serviceName) {
        synchronized (MockHealthStatisticsGenerator.class) {
            if (!statisticsUpdaterTasksScheduled(serviceName)) {
                List<MockHealthStatisticsPattern> statisticsPatterns = MockIaasConfig.getInstance().
                        getMockHealthStatisticsConfig().getStatisticsPatterns();

                Map<String, ScheduledFuture> taskList = serviceNameToTaskListMap.get(serviceName);
                if (taskList == null) {
                    taskList = new ConcurrentHashMap<String, ScheduledFuture>();
                    serviceNameToTaskListMap.put(serviceName, taskList);
                }

                for (MockHealthStatisticsPattern statisticsPattern : statisticsPatterns) {
                    if (statisticsPattern.getCartridgeType().equals(serviceName) &&
                            (statisticsPattern.getSampleDuration() > 0)) {
                        MockHealthStatisticsUpdater runnable = new MockHealthStatisticsUpdater(statisticsPattern);
                        ScheduledFuture<?> task = scheduledExecutorService.scheduleAtFixedRate(runnable, 0,
                                statisticsPattern.getSampleDuration(), TimeUnit.SECONDS);
                        taskList.put(statisticsPattern.getFactor().toString(), task);
                    }
                }

                if (log.isInfoEnabled()) {
                    log.info(String.format("Mock statistics updaters scheduled: [service-name] %s", serviceName));
                }
            }
        }
    }

    /**
     * Stop statistics updater tasks of the given service/cartridge type.
     *
     * @param serviceName service name/cartridge type
     */
    public void stopStatisticsUpdaterTasks(String serviceName) {
        synchronized (MockHealthStatisticsGenerator.class) {
            Map<String, ScheduledFuture> taskMap = serviceNameToTaskListMap.get(serviceName);
            if ((taskMap != null) && (taskMap.size() > 0)) {
                for (String scalingFactor : taskMap.keySet()) {
                    stopStatisticsUpdaterTask(serviceName, scalingFactor);
                }
            }
        }
    }

    /**
     * Stop statistics updater task of a service/cartridge type, factor.
     *
     * @param serviceName   service name/cartridge type
     * @param scalingFactor scaling factor
     */
    public void stopStatisticsUpdaterTask(String serviceName, String scalingFactor) {
        Map<String, ScheduledFuture> autoscalingFactorToTaskMap = serviceNameToTaskListMap.get(serviceName);
        if (autoscalingFactorToTaskMap != null) {
            ScheduledFuture task = autoscalingFactorToTaskMap.get(scalingFactor);
            if (task != null) {
                task.cancel(true);
                autoscalingFactorToTaskMap.remove(scalingFactor);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Mock statistics updater task stopped: [service-name] %s" +
                            " [scaling-factor] %s", serviceName, scalingFactor));
                }
            }
        }
    }

    /**
     * Returns true if there are statistics updater tasks scheduled for the given service/cartridge type
     * else returns false.
     *
     * @param serviceName service name/cartridge type
     * @return true if statistics updater tasks are scheduled
     */
    public boolean statisticsUpdaterTasksScheduled(String serviceName) {
        Map<String, ScheduledFuture> tasks = serviceNameToTaskListMap.get(serviceName);
        return ((tasks != null) && (tasks.size() > 0));
    }
}
