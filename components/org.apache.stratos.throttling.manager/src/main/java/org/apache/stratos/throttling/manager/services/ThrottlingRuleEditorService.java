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
package org.apache.stratos.throttling.manager.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.throttling.manager.tasks.Task;
import org.apache.stratos.throttling.manager.utils.Util;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.throttling.manager.rules.RuleInvoker;

import java.util.List;

/**
 * This is a super tenant service to manage throttling rules
 */
public class ThrottlingRuleEditorService {
    private static final Log log = LogFactory.getLog(ThrottlingRuleEditorService.class);

    /**
     * Retrieve Throttling Rules
     *
     * @throws Exception, if retrieving the throttling rules failed.
     * @return, rule content
     */
    public String retrieveThrottlingRules() throws Exception {
        // getting the resource content.
        UserRegistry systemRegistry = Util.getSuperTenantGovernanceSystemRegistry();
        Resource ruleContentResource = systemRegistry.get(StratosConstants.THROTTLING_RULES_PATH);
        Object ruleContent = ruleContentResource.getContent();
        if (ruleContent instanceof String) {
            return (String) ruleContent;
        } else if (ruleContent instanceof byte[]) {
            return new String((byte[]) ruleContent);
        }
        String msg = "Unidentified type for the registry resource content. type: " +
                     ruleContent.getClass().getName();
        log.error(msg);
        throw new Exception(msg);
    }

    /**
     * Update throttling rules.
     *
     * @param ruleContent - content of the rule.
     * @throws Exception, if updating the throttling rules failed.
     */
    public void updateThrottlingRules(String ruleContent) throws Exception {
        // updating the rule content
        boolean updateSuccess = false;
        UserRegistry systemRegistry = Util.getSuperTenantGovernanceSystemRegistry();
        try {
            systemRegistry.beginTransaction();
            Resource ruleContentResource = systemRegistry.get(StratosConstants.THROTTLING_RULES_PATH);
            ruleContentResource.setContent(ruleContent);
            systemRegistry.put(StratosConstants.THROTTLING_RULES_PATH, ruleContentResource);

            List<Task> tasks = Util.getTasks();
            for (Task task : tasks) {
                RuleInvoker ruleInvoker = task.getRuleInvoker();
                ruleInvoker.updateRules();
            }
            updateSuccess = true;
        } finally {
            if (updateSuccess) {
                systemRegistry.commitTransaction();
            } else {
                systemRegistry.rollbackTransaction();
            }
        }
    }
}
