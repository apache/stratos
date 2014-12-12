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

import java.util.Iterator;
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
            StratosThreadPool.getScheduledExecutorService("MOCK_STATISTICS_GENERATOR_EXECUTOR_SERVICE", 10);

    private boolean scheduled;
    // Map<ServiceName, List<ScheduledFuture>>
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

    private MockHealthStatisticsGenerator() {
        serviceNameToTaskListMap = new ConcurrentHashMap<String, Map<String, ScheduledFuture>>();
    }

    /**
     * Schedule statistics updater tasks for the given service/cartridge type.
     *
     * @param serviceName
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
     * @param serviceName
     */
    public void stopStatisticsUpdaterTasks(String serviceName) {
        synchronized (MockHealthStatisticsGenerator.class) {
            Map<String, ScheduledFuture> taskMap = serviceNameToTaskListMap.get(serviceName);
            if ((taskMap != null) && (taskMap.size() > 0)) {
                Iterator<String> factorIterator = taskMap.keySet().iterator();
                while(factorIterator.hasNext()) {
                    String factor = factorIterator.next();
                    stopStatisticsUpdaterTask(serviceName, factor);
                }
            }
        }
    }

    /**
     * Stop statistics updater task of a service/cartridge type, factor.
     * @param serviceName
     * @param factor
     */
    public void stopStatisticsUpdaterTask(String serviceName, String factor) {
        Map<String, ScheduledFuture> factorToTaskMap = serviceNameToTaskListMap.get(serviceName);
        if(factorToTaskMap != null) {
            ScheduledFuture task = factorToTaskMap.get(factor);
            if(task != null) {
                task.cancel(true);
                factorToTaskMap.remove(factor);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Mock statistics updater task stopped: [service-name] %s" +
                            " [factor] %s", serviceName, factor));
                }
            }
        }
    }

    /**
     * Returns true if there are statistics updater tasks scheduled for the given service/cartridge type
     * else returns false.
     * @param serviceName
     * @return
     */
    public boolean statisticsUpdaterTasksScheduled(String serviceName) {
        Map<String, ScheduledFuture> tasks = serviceNameToTaskListMap.get(serviceName);
        return ((tasks != null) && (tasks.size() > 0));
    }
}
