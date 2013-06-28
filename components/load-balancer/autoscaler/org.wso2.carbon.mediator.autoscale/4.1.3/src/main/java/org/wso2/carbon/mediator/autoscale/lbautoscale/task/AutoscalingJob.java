/**
 * Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.mediator.autoscale.lbautoscale.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.task.Task;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.util.Map;

public class AutoscalingJob implements Job {

    public static final String AUTOSCALER_TASK = "autoscalerTask";

    public static final String SYNAPSE_ENVI = "synapseEnv";

    private static final Log log = LogFactory.getLog(AutoscalingJob.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        try {

            Task task = null;

            JobDetail jobDetail = jobExecutionContext.getJobDetail();

            if (log.isDebugEnabled()) {
                log.debug("Executing Autoscaler task : " + jobDetail.getKey().getName());
            }

            @SuppressWarnings("rawtypes")
            Map mjdm = jobExecutionContext.getMergedJobDataMap();

            task = (Task) mjdm.get(AUTOSCALER_TASK);

            if (task instanceof ManagedLifecycle) {
                // Execute Autoscaler task
                ((ServiceRequestsInFlightAutoscaler) task).execute();
            }

        } catch (Exception e) {
            throw new JobExecutionException(e);
        }

    }

}
