/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.throttling.manager.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.throttling.agent.client.ThrottlingRuleInvoker;
import org.wso2.carbon.throttling.manager.dataobjects.ThrottlingDataContext;
import org.wso2.carbon.throttling.manager.rules.KnowledgeBaseManager;
import org.wso2.carbon.throttling.manager.rules.RuleInvoker;
import org.wso2.carbon.throttling.manager.tasks.Task;
import org.wso2.carbon.throttling.manager.utils.Util;
import org.wso2.carbon.throttling.manager.validation.ValidationInfoManager;

import java.util.ArrayList;
import java.util.List;

public class MultitenancyThrottlingService extends AbstractAdmin implements ThrottlingRuleInvoker {
    
    private static Log log = LogFactory.getLog(MultitenancyThrottlingService.class);

    public void executeThrottlingRules(int tenantId) throws Exception {

        //UserRegistry registry = (UserRegistry) getGovernanceUserRegistry();
        int currentTenantId = tenantId;
        
        List<Task> tasks = Util.getTasks();
        for (Task task: tasks) {
            // initialize the knowledge base
            List<Object> knowledgeBase = new ArrayList<Object>();
            ThrottlingDataContext throttlingDataContext =
                    KnowledgeBaseManager.feedKnowledgeBase(currentTenantId, task, knowledgeBase);

            RuleInvoker ruleInvoker = task.getRuleInvoker();
            ruleInvoker.invoke(knowledgeBase);

            log.info("Throttling rules executed for tenant id: " + currentTenantId);
            
            ValidationInfoManager.persistValidationDetails(throttlingDataContext);
        }
    }
}
