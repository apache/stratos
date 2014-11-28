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

package org.apache.stratos.cloud.controller.messaging.publisher;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.messaging.topology.TopologySynchronizerTask;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.wso2.carbon.ntask.common.TaskException;
import org.wso2.carbon.ntask.core.TaskInfo;
import org.wso2.carbon.ntask.core.TaskManager;
import org.wso2.carbon.ntask.core.service.TaskService;

/**
 * Topology synchronizer task scheduler for scheduling the topology synchronizer task
 * using carbon task service.
 */
public class TopologySynchronizerTaskScheduler {

    private static final Log log = LogFactory.getLog(TopologySynchronizerTaskScheduler.class);

    private static final CloudControllerContext dataHolder = CloudControllerContext.getInstance();

    public static void schedule(TaskService taskService) {
        TaskManager taskManager = null;
        try {

            if (!taskService.getRegisteredTaskTypes().contains(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE)) {
                // Register task type
                taskService.registerTaskType(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);

                // Register task
                taskManager = taskService.getTaskManager(CloudControllerConstants.TOPOLOGY_SYNC_TASK_TYPE);
                String cronProp = dataHolder.getTopologyConfig().getProperty(CloudControllerConstants.CRON_PROPERTY);
				String cron = cronProp != null ?  cronProp :CloudControllerConstants.TOPOLOGY_SYNC_CRON ;
                TaskInfo.TriggerInfo triggerInfo = new TaskInfo.TriggerInfo(cron);
                TaskInfo taskInfo = new TaskInfo(CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME,
                        TopologySynchronizerTask.class.getName(),
                        new HashMap<String, String>(), triggerInfo);
                taskManager.registerTask(taskInfo);
                if(log.isDebugEnabled()) {
                    log.debug(String.format("Topology synchronization task scheduled: %s", CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME));
                }
            }

        } catch (Exception e) {
            if (taskManager != null) {
                try {
                    taskManager.deleteTask(CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME);
                } catch (TaskException te) {
                    if (log.isErrorEnabled()) {
                        log.error(te);
                    }
                }
            }
            
            String msg = String.format("Could not schedule topology synchronization task: %s", CloudControllerConstants.TOPOLOGY_SYNC_TASK_NAME);
            log.error(msg, e);
			throw new RuntimeException(msg, e);
        }
    }
}
