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
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Invoice;
import org.wso2.carbon.billing.core.dataobjects.Item;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores the generated invoice in the billing database.
 */
public class DefaultFinalizingHandler implements BillingHandler {

    private static Log log = LogFactory.getLog(DefaultFinalizingHandler.class);
    public void init(Map<String, String> handlerConfig) throws BillingException {
        // nothing to initialize
    }

    public void execute(BillingEngineContext handlerContext) throws BillingException {
        // saving the bill
        saveInvoice(handlerContext);
    }

    private void saveInvoice(BillingEngineContext handlerContext) throws BillingException {
        DataAccessObject dataAccessObject = BillingManager.getInstance().getDataAccessObject();
        List<Subscription> subscriptions = handlerContext.getSubscriptions();
        Map<Integer, Invoice> invoiceMap = new HashMap<Integer, Invoice>();
        
        for (Subscription subscription : subscriptions) {
            Customer customer = subscription.getCustomer();
            Invoice invoice = customer.getActiveInvoice();
            if (invoiceMap.get(customer.getId()) == null) {
                invoiceMap.put(customer.getId(), invoice);
            }
        }

        // from the invoice set we are calculating the purchase orders
        for (Invoice invoice : invoiceMap.values()) {
            // save the invoice first
            dataAccessObject.addInvoice(invoice);
            subscriptions = invoice.getSubscriptions();
            for (Subscription subscription : subscriptions) {
                // associate the subscription with the invoice.
                int invoiceSubscriptionId =
                        dataAccessObject.addInvoiceSubscription(invoice, subscription);
                // now iterate all the items and save it in invoice subscription item space
                if (subscription.getItem() != null) {
                    addInvoiceSubscriptionItem(subscription.getItem(), invoiceSubscriptionId);
                }
            }
        }
        log.info( invoiceMap.size() + " Invoices saved to the database");
    }

    private void addInvoiceSubscriptionItem(Item item, 
                                            int invoiceSubscriptionId) throws BillingException {
        DataAccessObject dataAccessObject = BillingManager.getInstance().getDataAccessObject();
        dataAccessObject.addInvoiceSubscriptionItem(item, invoiceSubscriptionId);
        // and iterate through all the item children
        if (item.getChildren() != null) {
            for (Item subItem : item.getChildren()) {
                addInvoiceSubscriptionItem(subItem, invoiceSubscriptionId);
            }
        }
    }
}
