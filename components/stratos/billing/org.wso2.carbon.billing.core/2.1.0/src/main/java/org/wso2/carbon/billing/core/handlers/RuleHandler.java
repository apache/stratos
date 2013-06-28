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
package org.wso2.carbon.billing.core.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingEngineContext;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingHandler;
import org.wso2.carbon.billing.core.dataobjects.*;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.rule.common.Rule;
import org.wso2.carbon.rule.common.RuleSet;
import org.wso2.carbon.rule.common.exception.RuleConfigurationException;
import org.wso2.carbon.rule.common.exception.RuleRuntimeException;
import org.wso2.carbon.rule.common.util.Constants;
import org.wso2.carbon.rule.kernel.backend.RuleBackendRuntime;
import org.wso2.carbon.rule.kernel.backend.RuleBackendRuntimeFactory;
import org.wso2.carbon.rule.kernel.backend.Session;
import org.wso2.carbon.rule.kernel.config.RuleEngineProvider;
import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;
import java.util.*;

/**
 * Runs the billing rules against each subscription
 * At the moment only the subscription fee is calculated by
 * these rules. Overusage charges are calculated by InvoiceCalculationHandler
 */
public class RuleHandler implements BillingHandler {

    private static Log log = LogFactory.getLog(RuleHandler.class);
    private Session ruleSession;

    public void init(Map<String, String> handlerConfig) throws BillingException {

        //create a rule run time to execute rules.

        RuleEngineProvider ruleEngineProvider = Util.getRuleEngineConfigService().getRuleConfig().getRuleEngineProvider();

        Class ruleBackendRuntimeFactoryClass;

        try {
            ruleBackendRuntimeFactoryClass = Class.forName(ruleEngineProvider.getClassName());
            RuleBackendRuntimeFactory ruleBackendRuntimeFactory =
                    (RuleBackendRuntimeFactory) ruleBackendRuntimeFactoryClass.newInstance();
            RuleBackendRuntime ruleBackendRuntime =
                           ruleBackendRuntimeFactory.getRuleBackendRuntime(ruleEngineProvider.getProperties(),
                           Thread.currentThread().getContextClassLoader());

            // create a rule set to add
            RuleSet ruleSet = new RuleSet();
            Rule rule = new Rule();
            rule.setResourceType(Constants.RULE_RESOURCE_TYPE_REGULAR);

            rule.setSourceType(Constants.RULE_SOURCE_TYPE_URL);

            String ruleFile = handlerConfig.get("file");
            ruleFile = CarbonUtils.getCarbonConfigDirPath() + File.separator + ruleFile;
            rule.setValue("file://" + ruleFile);
            log.info("Rule: " + rule.getValue());

            ruleSet.addRule(rule);

            ruleBackendRuntime.addRuleSet(ruleSet);

            this.ruleSession = ruleBackendRuntime.createSession(Constants.RULE_STATEFUL_SESSION);

        } catch (Exception e) {
            String msg = "Error occurred while initializing the rule executing environment: " +
                    e.getMessage();
            log.error(msg);
            throw new BillingException(msg, e);
        } 
    }

    public void execute(BillingEngineContext handlerContext) throws BillingException {
        List<Subscription> subscriptions = handlerContext.getSubscriptions();

        List<Object> rulesInput = new ArrayList<Object>();
        Set<Integer> customerSet = new HashSet<Integer>();

        for (Subscription subscription : subscriptions) {
            // add the subscriptions
            rulesInput.add(subscription);

            // add the customers
            Customer customer = subscription.getCustomer();
            if (!customerSet.contains(customer.getId())) {
                customerSet.add(customer.getId());
                rulesInput.add(customer);

                // add the invoice too
                Invoice invoice = customer.getActiveInvoice();
                rulesInput.add(invoice);

                // add each purchases
                List<Payment> payments = invoice.getPayments();
                if (payments != null) {
                    for (Payment payment : payments) {
                        rulesInput.add(payment);
                    }
                }
            }

            // add the items
            Item item = subscription.getItem();
            rulesInput.add(item);

            List<? extends Item> children = item.getChildren();
            if (children != null) {
                for (Item subItem : item.getChildren()) {
                    rulesInput.add(subItem);
                }
            }
        }

        try {
            this.ruleSession.execute(rulesInput);
        } catch (RuleRuntimeException e) {
            String msg = "Error occurred while executing rules during the bill generation: " +
                    e.getMessage();
            log.error(msg);
            throw new BillingException(msg, e);
        }
        log.info("Rule execution phase completed.");
    }
}
