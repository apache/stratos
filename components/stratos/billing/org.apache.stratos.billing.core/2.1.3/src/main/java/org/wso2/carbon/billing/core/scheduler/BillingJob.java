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
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.carbon.billing.core.BillingConstants;
import org.wso2.carbon.billing.core.BillingEngine;
import org.wso2.carbon.billing.core.BillingException;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This job is run by the scheduled task for
 * bill generation
 */
public class BillingJob implements Job {
    private static final Log log = LogFactory.getLog(BillingJob.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // first we generate the bill
        BillingEngine billingEngine =
                (BillingEngine) jobExecutionContext.getMergedJobDataMap().get(
                        BillingConstants.BILLING_ENGINE_KEY);
        SchedulerContext schedulerContext =
                (SchedulerContext) jobExecutionContext.getMergedJobDataMap().get(
                        BillingConstants.SCHEDULER_CONTEXT);
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long startTime = System.currentTimeMillis();
            log.info("Bill generation started at " + dateFormat.format(new Date(System.currentTimeMillis())));

            billingEngine.generateBill(schedulerContext);

            log.info("Bill generation completed at " + dateFormat.format(new Date(System.currentTimeMillis())));
            long timeTaken = System.currentTimeMillis() - startTime;
            log.info("Time taken for bill generation: " + timeTaken/1000 + "s");
        } catch (BillingException e) {
            String msg = "Error in generating the bill";
            log.error(msg, e);
            throw new JobExecutionException(msg, e);
        }
        

    }
}
