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

import java.util.ArrayList;
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
    private Map<String, List<ScheduledFuture>> serviceNameToTaskListMap;

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
        serviceNameToTaskListMap = new ConcurrentHashMap<String, List<ScheduledFuture>>();
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

                List taskList = serviceNameToTaskListMap.get(serviceName);
                if (taskList == null) {
                    taskList = new ArrayList<ScheduledFuture>();
                    serviceNameToTaskListMap.put(serviceName, taskList);
                }

                for (MockHealthStatisticsPattern statisticsPattern : statisticsPatterns) {
                    if (statisticsPattern.getCartridgeType().equals(serviceName) &&
                            (statisticsPattern.getSampleDuration() > 0)) {
                        MockHealthStatisticsUpdater runnable = new MockHealthStatisticsUpdater(statisticsPattern);
                        ScheduledFuture<?> task = scheduledExecutorService.scheduleAtFixedRate(runnable, 0,
                                statisticsPattern.getSampleDuration(), TimeUnit.SECONDS);
                        taskList.add(task);
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
            List<ScheduledFuture> taskList = serviceNameToTaskListMap.get(serviceName);
            if ((taskList != null) && (taskList.size() > 0)) {
                Iterator<ScheduledFuture> iterator = taskList.iterator();
                while(iterator.hasNext()) {
                    // Cancel task
                    ScheduledFuture task = iterator.next();
                    task.cancel(true);

                    // Remove from task list
                    iterator.remove();
                }

                if (log.isInfoEnabled()) {
                    log.info(String.format("Mock statistics updaters stopped: [service-name] %s", serviceName));
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
        List<ScheduledFuture> tasks = serviceNameToTaskListMap.get(serviceName);
        return ((tasks != null) && (tasks.size() > 0));
    }
}
