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

package org.apache.stratos.manager.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.service.TaskService;

import java.util.HashMap;

/**
 * Tenant synchronizer task scheduler for scheduling the tenant synchronizer task
 * using carbon task service.
 */
public class TenantSynchronizerTaskScheduler {

    private static final Log log = LogFactory.getLog(TenantSynzhronizerTask.class);

    private static final String TENANT_SYNC_TASK_TYPE = "TENANT_SYNC_TASK_TYPE";
    private static final String TENANT_SYNC_TASK_NAME = "TENANT_SYNC_TASK";
    private static final String DEFAULT_CRON = "1 * * * * ? *";

    public static void schedule(TaskService taskService) {
        TaskManager taskManager = null;
        try {

            if (!taskService.getRegisteredTaskTypes().contains(TENANT_SYNC_TASK_TYPE)) {
                // Register task type
                taskService.registerTaskType(TENANT_SYNC_TASK_TYPE);

                // Register task
                taskManager = taskService.getTaskManager(TENANT_SYNC_TASK_TYPE);
                TaskInfo.TriggerInfo triggerInfo = new TaskInfo.TriggerInfo(DEFAULT_CRON);
                TaskInfo taskInfo = new TaskInfo(TENANT_SYNC_TASK_NAME,
                        TenantSynzhronizerTask.class.getName(),
                        new HashMap<String, String>(), triggerInfo);
                taskManager.registerTask(taskInfo);
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Tenant synchronization task scheduled: %s", TENANT_SYNC_TASK_NAME));
                }
            }

        } catch (Exception e) {
            if (taskManager != null) {
                try {
                    taskManager.deleteTask(TENANT_SYNC_TASK_NAME);
                } catch (TaskException te) {
                    if (log.isErrorEnabled()) {
                        log.error(te);
                    }
                }
            }
            throw new RuntimeException(String.format("Could not schedule tenant synchronization task: %s", TENANT_SYNC_TASK_NAME), e);
        }
    }
}
