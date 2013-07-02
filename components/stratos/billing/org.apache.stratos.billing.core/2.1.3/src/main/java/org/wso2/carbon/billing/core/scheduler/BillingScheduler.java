/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.billing.core.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.task.TaskConstants;
import org.apache.synapse.task.TaskDescription;
import org.apache.synapse.task.TaskScheduler;
import org.apache.synapse.task.TaskSchedulerFactory;
import org.wso2.carbon.billing.core.BillingConstants;
import org.wso2.carbon.billing.core.BillingEngine;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.conf.BillingTaskConfiguration;
import org.wso2.carbon.registry.core.utils.UUIDGenerator;

import java.util.*;

public class BillingScheduler {
    private static Log log = LogFactory.getLog(BillingScheduler.class);
    private TaskScheduler taskScheduler;
    private BillingTaskConfiguration billingTaskConfig;
    private BillingEngine billingEngine;

    public BillingScheduler(BillingEngine billingEngine,
                            BillingTaskConfiguration billingTaskConfiguration) {
        this.billingTaskConfig = billingTaskConfiguration;
        this.billingEngine = billingEngine;
        taskScheduler = TaskSchedulerFactory.getTaskScheduler(TaskConstants.TASK_SCHEDULER);
        if (!taskScheduler.isInitialized()) {
            Properties properties = new Properties();
            taskScheduler.init(properties);
        }
    }

    public SchedulerContext createScheduleContext() throws BillingException {
        SchedulerContext schedulerContext = new SchedulerContext();
        ScheduleHelper triggerCalculator = billingTaskConfig.getScheduleHelper();
        if (triggerCalculator != null) {
            triggerCalculator.invoke(schedulerContext);
        }
        return schedulerContext;
    }

    public void scheduleNextCycle(SchedulerContext schedulerContext) {

        String taskName = UUIDGenerator.generateUUID();

        TaskDescription taskDescription = new TaskDescription();
        taskDescription.setName(taskName);
        taskDescription.setGroup(BillingConstants.GROUP_ID);
        taskDescription.setCron(schedulerContext.getCronString());
        
        Map<String, Object> resources = new HashMap<String, Object>();
        resources.put(BillingConstants.TASK_NAME_KEY, taskName);
        resources.put(BillingConstants.SCHEDULER_KEY, this);
        resources.put(BillingConstants.BILLING_ENGINE_KEY, billingEngine);
        resources.put(BillingConstants.SCHEDULER_CONTEXT, schedulerContext);
        taskScheduler.scheduleTask(taskDescription, resources, BillingJob.class);
    }
}
