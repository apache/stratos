/**
 *  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.mediator.autoscale.lbautoscale.task;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.synapse.task.TaskConstants;
import org.apache.synapse.task.TaskDescriptionRepository;
import org.apache.synapse.task.TaskScheduler;
import org.wso2.carbon.utils.AbstractAxis2ConfigurationContextObserver;

public class AutoscalerTaskInitializer extends AbstractAxis2ConfigurationContextObserver {

    public static final java.lang.String CARBON_TASK_SCHEDULER = "CARBON_TASK_SCHEDULER";
    public static final java.lang.String CARBON_TASK_REPOSITORY = "CARBON_TASK_REPOSITORY";

    public void createdConfigurationContext(ConfigurationContext configContext) {
        TaskScheduler scheduler = (TaskScheduler)configContext.getProperty(AutoscalerTaskInitializer.CARBON_TASK_SCHEDULER);
        if (scheduler == null) {
            scheduler = new TaskScheduler(TaskConstants.TASK_SCHEDULER);
            scheduler.init(null);
            configContext.setProperty(AutoscalerTaskInitializer.CARBON_TASK_SCHEDULER, scheduler);
        } else if(!scheduler.isInitialized()) {
            scheduler.init(null);
        }

        if (configContext.getProperty(AutoscalerTaskInitializer.CARBON_TASK_REPOSITORY) == null) {
            TaskDescriptionRepository repository = new TaskDescriptionRepository();
            configContext.setProperty(
                    AutoscalerTaskInitializer.CARBON_TASK_REPOSITORY, repository);
        }
    }

}
