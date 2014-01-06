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

package org.apache.stratos.cloud.controller.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.topology.TopologySynchronizerTask;
import org.wso2.carbon.ntask.core.service.TaskService;

/**
 * Topology synchronizer task scheduler for scheduling the topology synchronizer task
 * using carbon task service.
 */
public class TopologySynchronizerTaskScheduler {

    private static final Log log = LogFactory.getLog(TopologySynchronizerTaskScheduler.class);

    private static final String TOPOLOGY_SYNC_TASK_TYPE = "TOPOLOGY_SYNC_TASK_TYPE";
    private static final String TOPOLOGY_SYNC_TASK_NAME = "TOPOLOGY_SYNC_TASK";
    private static final String DEFAULT_CRON = "1 * * * * ? *";

    public static void schedule(TaskService taskService) {
        // TODO: Replace this with task scheduler
        Thread thread = new Thread(new TaskRunnable());
        thread.start();
    }

    private static class TaskRunnable implements Runnable {
        @Override
        public void run() {        	
            while (true) {
                try {
                    log.debug("Running topology synchronizer task");
                    TopologySynchronizerTask task = new TopologySynchronizerTask();
                    task.execute();
                    try {
                        Thread.sleep(60 * 1000);
                    } catch (InterruptedException e) {
                    }
                } catch (Exception e) {
                    log.error(e);
                }
            }
        }
    }

//    public static void schedule(TaskService taskService) {
//        TaskManager taskManager = null;
//        try {
//
//            if (!taskService.getRegisteredTaskTypes().contains(TOPOLOGY_SYNC_TASK_TYPE)) {
//                // Register task type
//                taskService.registerTaskType(TOPOLOGY_SYNC_TASK_TYPE);
//
//                // Register task
//                taskManager = taskService.getTaskManager(TOPOLOGY_SYNC_TASK_TYPE);
//                TaskInfo.TriggerInfo triggerInfo = new TaskInfo.TriggerInfo(DEFAULT_CRON);
//                TaskInfo taskInfo = new TaskInfo(TOPOLOGY_SYNC_TASK_NAME,
//                        TopologySynchronizerTask.class.getName(),
//                        new HashMap<String, String>(), triggerInfo);
//                taskManager.registerTask(taskInfo);
//                if(log.isDebugEnabled()) {
//                    log.debug(String.format("Topology synchronization task scheduled: %s", TOPOLOGY_SYNC_TASK_NAME));
//                }
//            }
//
//        } catch (Exception e) {
//            if (taskManager != null) {
//                try {
//                    taskManager.deleteTask(TOPOLOGY_SYNC_TASK_NAME);
//                } catch (TaskException te) {
//                    if (log.isErrorEnabled()) {
//                        log.error(te);
//                    }
//                }
//            }
//            throw new RuntimeException(String.format("Could not schedule topology synchronization task: %s", TOPOLOGY_SYNC_TASK_NAME), e);
//        }
//    }
}
