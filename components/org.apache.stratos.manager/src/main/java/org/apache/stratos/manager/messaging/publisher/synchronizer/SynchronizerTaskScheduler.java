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

package org.apache.stratos.manager.messaging.publisher.synchronizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.internal.ServiceReferenceHolder;
import org.apache.stratos.manager.utils.StratosManagerConstants;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.service.TaskService;

import java.util.HashMap;

/**
 * Synchronizer task scheduler for scheduling tasks using carbon task service.
 */
public class SynchronizerTaskScheduler {

    private static final Log log = LogFactory.getLog(SynchronizerTaskScheduler.class);

    public static void schedule(String taskType, String taskName, Class taskClass) {
        TaskManager taskManager = null;
        try {
            TaskService taskService = ServiceReferenceHolder.getInstance().getTaskService();

            if (!taskService.getRegisteredTaskTypes().contains(taskType)) {
                // Register task type
                taskService.registerTaskType(taskType);

                // Register task
                taskManager = taskService.getTaskManager(taskType);
                TaskInfo.TriggerInfo triggerInfo = new TaskInfo.TriggerInfo(StratosManagerConstants.DEFAULT_CRON);
                TaskInfo taskInfo = new TaskInfo(taskName, taskClass.getName(), new HashMap<String, String>(), triggerInfo);
                taskManager.registerTask(taskInfo);
                if(log.isInfoEnabled()) {
                    log.info(String.format("Synchronization task scheduled: %s", taskName));
                }
            }
        } catch (Exception e) {
            if (taskManager != null) {
                try {
                    taskManager.deleteTask(taskName);
                } catch (TaskException te) {
                    if (log.isErrorEnabled()) {
                        log.error(te);
                    }
                }
            }
            throw new RuntimeException(String.format("Could not schedule synchronization task: %s", taskName), e);
        }
    }
}
