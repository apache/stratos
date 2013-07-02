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
import org.wso2.carbon.billing.core.dataobjects.Cash;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Invoice;
import org.wso2.carbon.billing.core.dataobjects.Payment;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Subscriptions found in the subscription feeding handler are
 * added to relevant invoices via this class
 */
public class SubscriptionTreeBuildingHandler implements BillingHandler {

    private static Log log = LogFactory.getLog(SubscriptionTreeBuildingHandler.class);

    public void init(Map<String, String> handlerConfig) throws BillingException {
        // nothing to initialize
    }

    public void execute(BillingEngineContext handlerContext) throws BillingException {
        buildSubscriptionTree(handlerContext);
    }

    /**
     * Creates maps of <customer, list of subscriptions>. Then creates
     * invoices for each customers.
     * @param handlerContext this is the BillingEngineContext
     * @throws BillingException throws a BillingException
     */
    private void buildSubscriptionTree(BillingEngineContext handlerContext) throws BillingException {
        DataAccessObject dataAccessObject = BillingManager.getInstance().getDataAccessObject();

        // get the subscription from handler context
        List<Subscription> subscriptions = handlerContext.getSubscriptions();

        Map<Customer, List<Subscription>> customersSubscriptions = new HashMap<Customer, List<Subscription>>();

        for (Subscription subscription : subscriptions) {
            Customer customer = subscription.getCustomer();
            List<Subscription> customerSubscriptions = customersSubscriptions.get(customer);
            if (customerSubscriptions == null) {
                customerSubscriptions = new ArrayList<Subscription>();
            }
            customerSubscriptions.add(subscription);
            customersSubscriptions.put(customer, customerSubscriptions);
        }

        // so iterating all the customers
        for (Map.Entry<Customer, List<Subscription>> entry : customersSubscriptions.entrySet()) {
            Customer customer = entry.getKey();
            List<Subscription> customerSubscriptions = entry.getValue();

            // create an empty invoice
            Invoice invoice = new Invoice();

            // get the last invoice for the customer
            Invoice lastInvoice = dataAccessObject.getLastInvoice(customer);

            if (lastInvoice != null) {
                invoice.setBoughtForward(lastInvoice.getCarriedForward());
                long lastInvoiceEnd = lastInvoice.getEndDate().getTime();
                long currentInvoiceStart = lastInvoiceEnd + 1000;    //one second after
                                                                    // the last invoice
                invoice.setStartDate(new Date(currentInvoiceStart));
            } else {
                invoice.setBoughtForward(new Cash("$0"));
                // the earliest of the subscriptions
                long earliestSubscriptionStart = -1;
                for (Subscription subscription : customerSubscriptions) {
                    long subscriptionStartDate = subscription.getActiveSince().getTime();
                    if (earliestSubscriptionStart == -1 ||
                        subscriptionStartDate < earliestSubscriptionStart) {
                        earliestSubscriptionStart = subscriptionStartDate;
                    }
                }
                invoice.setStartDate(new Date(earliestSubscriptionStart));
            }
            
            Date currentDate = new Date();
            invoice.setEndDate(currentDate);
            // this is the date the billing is initialized, this can be probably overwritten
            invoice.setDate(currentDate);

            // and we will count for the subscriptions for the invoice
            invoice.setSubscriptions(customerSubscriptions);

            // and then we will count on the un-billed purchases of all the subscriptions of the
            // customer
            Map<Integer, Payment> purchaseOrders = new HashMap<Integer, Payment>();
            for (Subscription subscription : customerSubscriptions) {
                dataAccessObject.fillUnbilledPayments(subscription, purchaseOrders, invoice );
            }
            for (Payment payment : purchaseOrders.values()) {
                invoice.addPayment(payment);
            }

            customer.setActiveInvoice(invoice);
            invoice.setCustomer(customer);
        }

        log.info("Subscription-tree building phase completed");
    }
}
