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
package org.wso2.carbon.throttling.manager.rules;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.rule.common.Rule;
import org.wso2.carbon.rule.common.RuleSet;
import org.wso2.carbon.rule.common.util.Constants;
import org.wso2.carbon.rule.kernel.backend.RuleBackendRuntime;
import org.wso2.carbon.rule.kernel.backend.RuleBackendRuntimeFactory;
import org.wso2.carbon.rule.kernel.backend.Session;
import org.wso2.carbon.rule.kernel.config.RuleEngineProvider;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.throttling.manager.exception.ThrottlingException;
import org.wso2.carbon.throttling.manager.utils.Util;

public class RuleInvoker {
    private static final Log log = LogFactory.getLog(RuleInvoker.class);
    Session session;

    public RuleInvoker() throws ThrottlingException {
        updateRules();
    }

    public synchronized void invoke(List<Object> knowledgeBase) throws ThrottlingException {

        try {
            session.execute(knowledgeBase);
        } catch (Exception e) {
            String msg = "Error occurred while executing the throttling rules: " + e.getMessage();
            log.error(msg);
            throw new ThrottlingException(msg, e);
        }
    }

    public synchronized void updateRules() throws ThrottlingException {
        
        RuleEngineProvider ruleEngineProvider = 
                Util.getRuleEngineConfigService().getRuleConfig().getRuleEngineProvider();
        
        Class ruleBackendRuntimeFactoryClass;
        RuleBackendRuntime ruleBackendRuntime;
        
        try{
            ruleBackendRuntimeFactoryClass = Class.forName(ruleEngineProvider.getClassName());
            RuleBackendRuntimeFactory ruleBackendRuntimeFactory = 
                    (RuleBackendRuntimeFactory) ruleBackendRuntimeFactoryClass.newInstance();
            ruleBackendRuntime = 
                    ruleBackendRuntimeFactory.getRuleBackendRuntime(ruleEngineProvider.getProperties(), 
                                                                    Thread.currentThread().getContextClassLoader());

            // create a rule set to add
            RuleSet ruleSet = new RuleSet();
            Rule rule = new Rule();
            rule.setResourceType(Constants.RULE_RESOURCE_TYPE_REGULAR);

            rule.setSourceType(Constants.RULE_SOURCE_TYPE_REGISTRY);
            rule.setValue("gov:" + StratosConstants.THROTTLING_RULES_PATH);
            ruleSet.addRule(rule);

            ruleBackendRuntime.addRuleSet(ruleSet);

            this.session = ruleBackendRuntime.createSession(Constants.RULE_STATEFUL_SESSION);

        }catch(Exception e){

            String msg = "Error occurred while initializing the rule executing environment: " + e.getMessage();
            log.error(msg);
            throw new ThrottlingException(msg, e);
        }

    }
}
