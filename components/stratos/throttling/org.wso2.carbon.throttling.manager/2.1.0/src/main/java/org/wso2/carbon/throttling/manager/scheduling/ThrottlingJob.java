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
package org.wso2.carbon.throttling.manager.scheduling;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataContext;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.rules.KnowledgeBaseManager;
import org.wso2.carbon.throttling.manager.rules.RuleInvoker;
import org.wso2.carbon.throttling.manager.tasks.Task;
import org.wso2.carbon.throttling.manager.utils.Util;
import org.wso2.carbon.throttling.manager.validation.ValidationInfoManager;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThrottlingJob implements Job {
    private static final Log log = LogFactory.getLog(ThrottlingJob.class);
    public static final String THROTTLING_TASK_CONTEXT_KEY = "throttlingTask";

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        carbonContext.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        Task task = (Task) jobExecutionContext.getMergedJobDataMap().get(THROTTLING_TASK_CONTEXT_KEY);
        executeTask(task);
    }

    public void executeTask(Task task) throws JobExecutionException {
        // initialize and prepare the knowledge base.
        List<Object> knowledgeBase = new ArrayList<Object>();
        Map<Integer, ThrottlingDataContext> contextMap;
        try {
            contextMap = feedKnowledgeBase(task, knowledgeBase);
        } catch (ThrottlingException e) {
            String msg = "Error in feeding knowledge base.";
            log.error(msg, e);
            throw new JobExecutionException(msg, e);
        }

        // invoke the rule.
        RuleInvoker ruleInvoker = task.getRuleInvoker();
        try {
            //updating the rule. this is important if we are having more than one managers running
            ruleInvoker.updateRules();
            ruleInvoker.invoke(knowledgeBase);
            log.info("Throttling rules executed successfully");
        } catch (ThrottlingException e) {
            String msg = "Error in invoking the throttling rule invoker.";
            log.error(msg, e);
            throw new JobExecutionException(msg, e);
        }
        // now persist the access validation information
        for (int tenantId : contextMap.keySet()) {
            ThrottlingDataContext dataContext = contextMap.get(tenantId);
            try {
                ValidationInfoManager.persistValidationDetails(dataContext);
            } catch (ThrottlingException e) {
                String msg = "Error in persisting validation details. Tenant id: " + tenantId + ".";
                log.error(msg, e);
                throw new JobExecutionException(msg, e);
            }
        }
    }

    private Map<Integer, ThrottlingDataContext> feedKnowledgeBase(Task task,
            List<Object> knowledgeBase) throws ThrottlingException {
        Map<Integer, ThrottlingDataContext> contextMap =
                new HashMap<Integer, ThrottlingDataContext>();
        // execute the task for each tenant
        Tenant[] tenants;
        try {
            tenants = Util.getAllTenants();
        } catch (UserStoreException e) {
            String msg = "Error in getting all the tenants.";
            log.error(msg, e);
            throw new ThrottlingException(msg, e);
        }
        // prepare the knowledge base using data on each tenant

        for (Tenant tenant : tenants) {
            if (!tenant.isActive()) {
                continue;
            }
            int tenantId = tenant.getId();

            ThrottlingDataContext throttlingDataContext =
                    KnowledgeBaseManager.feedKnowledgeBase(tenantId, task, knowledgeBase);

            // store the context in the map.
            contextMap.put(tenantId, throttlingDataContext);
        }
        return contextMap;
    }
}
