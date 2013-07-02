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
package org.wso2.carbon.billing.test;

import junit.framework.TestCase;
import org.wso2.carbon.billing.core.BillingEngine;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.billing.core.conf.BillingConfiguration;
import org.wso2.carbon.billing.core.dataobjects.*;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.utils.CarbonUtils;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class CarbonHome1Test extends TestCase {
    private static final String BILLING_CONFIG = "billing-config.xml";
    private static final String SELLER_ID = "carbonHome1TestShop";
    private BillingEngine billingEngine;

    public void setUp() throws Exception {
        String carbonHome = "src/test/resources/carbonhome1";
        System.setProperty("carbon.home", carbonHome);
        System.setProperty("carbon.config.dir.path", carbonHome + "/repository/conf");

//        SessionDescription sessionDescription = new SessionDescription();
//        sessionDescription.setSessionType(SessionDescription.STATEFUL_SESSION);
        /*RuleServerManager ruleServerManager = new RuleServerManager();// TODO to get from a OSGI service
        RuleServerConfiguration configuration = new RuleServerConfiguration(new JSR94BackendRuntimeFactory());
        ruleServerManager.init(configuration);
        */
//        OMElement config = TestUtils.loadConfigXML();
////        RuleServerConfiguration configuration = new RuleServerConfiguration(new JSR94BackendRuntimeFactory());
//        RuleServerManager ruleServerManager = new RuleServerManager();
//        ruleServerManager.init(ruleServerConfiguration);

        //Util.setRuleEngineConfigService(ruleServerManager);

        String configFile = CarbonUtils.getCarbonConfigDirPath() + "/" + BILLING_CONFIG;
        BillingConfiguration billingConfiguration = new BillingConfiguration(configFile);

        DataSource dataSource = billingConfiguration.getDataSource();
        assertNotNull("data should be not null", dataSource);
        try {
            if (BillingManager.getInstance() != null) {
                BillingManager.destroyInstance();
            }
        } catch (Exception e) {

        }
        BillingManager billingManager = new BillingManager(billingConfiguration);
        //billingManager.scheduleBilling();
        billingEngine = billingManager.getBillingEngine(SELLER_ID);
    }

    public void testDataSource() throws BillingException {
        TestUtils.deleteAllTables();
        Item item = new Item();
        item.setName("myitem-0");
        item.setCost(new Cash("$50.23"));
        item.setDescription("Just a simple item");

        Item accessedItem;
        boolean succeeded = false;
        try {
            billingEngine.beginTransaction();
            int itemId;
            List<Item> items = billingEngine.getItemsWithName(item.getName());
            for (Item existingItem : items) {
                //billingEngine.deleteItem(existingItem.getId());
            }
            itemId = billingEngine.addItem(item);
            accessedItem = billingEngine.getItem(itemId);
            succeeded = true;
        } finally {
            if (succeeded) {
                billingEngine.commitTransaction();
            } else {
                billingEngine.rollbackTransaction();
            }
        }
        assertEquals("Item name", accessedItem.getName(), item.getName());
        assertEquals("Item description", accessedItem.getDescription(), item.getDescription());
        assertEquals("Item cost", accessedItem.getCost().serializeToString(), item.getCost().serializeToString());
    }

    public void testEngine() throws BillingException {
        TestUtils.deleteAllTables();
        // first enter some items
        String[] itemNames = {"item-1", "item-2", "item-3", "item-4", "item-5", "item-6", "item-7", "item-8"};
        Cash[] itemCosts = {new Cash("$1.2"), new Cash("$2.12"), new Cash("$3.24"), new Cash("$4.34"),
                new Cash("$5.82"), new Cash("$6.92"), new Cash("$7.11"), new Cash("$8.01")};
        List<Item> items = new ArrayList<Item>();
        boolean succeeded = false;
        try {
            billingEngine.beginTransaction();
            for (int i = 0; i < Math.min(itemNames.length, itemCosts.length); i++) {
                String itemName = itemNames[i];
                Cash itemCost = itemCosts[i];
                Item item = new Item();
                item.setName(itemName);
                item.setCost(itemCost);
                List<Item> existingItems = billingEngine.getItemsWithName(itemName);
                for (Item existingItem : existingItems) {
                    //billingEngine.deleteItem(existingItem.getId());
                }
                billingEngine.addItem(item);
                items.add(item);
            }
            succeeded = true;
        } finally {
            if (succeeded) {
                billingEngine.commitTransaction();
            } else {
                billingEngine.rollbackTransaction();
            }
        }

        String[] customerNames = {"customer-1", "customer-2", "customer-3", "customer-4", "customer-5", "customer-6"};
        List<Customer> customers = new ArrayList<Customer>();
        succeeded = false;
        try {
            billingEngine.beginTransaction();
            for (String customerName : customerNames) {
                Customer customer = new Customer();
                customer.setName(customerName);

                List<Customer> existingCustomers = billingEngine.getCustomersWithName(customerName);
                for (Customer existingCustomer : existingCustomers) {
                    //billingEngine.deleteCustomer(existingCustomer.getId());
                }

                //billingEngine.addCustomer(customer);
                customers.add(customer);
            }
            succeeded = true;
        } finally {
            if (succeeded) {
                billingEngine.commitTransaction();
            } else {
                billingEngine.rollbackTransaction();
            }
        }

        // adding the subscriptions
        List<Subscription> subscriptions = new ArrayList<Subscription>();
        succeeded = false;
        try {
            billingEngine.beginTransaction();
            // first we clean the subscription table

            int[] subIdToItemId = {0, 3, 2, 1, 4, 7, 6, 5, 2, 3, 1, 1, 4, 6, 5, 0};
            int[] subIdToCustomerId = {0, 3, 2, 1, 4, 1, 0, 5, 2, 3, 1, 1, 4, 0, 5, 0};
            String[] payment1 = {"$0.5", "$3.2", "$2", "$1.8", "$4", "1", "0.8",
                    "$5", "$2", "$3.2", "$1", "$1.2", "$4", "0.2", "$5", "$0.2"};
            String[] payment2 = {"$5", "$2", "$3.2", "$1", "$1.2", "$4", "0.2",
                    "$5", "$0.2", "$0.5", "$3.2", "$2", "$1.8", "$4", "1", "0.8"};

            // then get some customers subscribed to items
            Calendar calendarToStart = Calendar.getInstance();
            calendarToStart.set(Calendar.YEAR, 2010);
            calendarToStart.set(Calendar.MONTH, Calendar.JANUARY);
            calendarToStart.set(Calendar.DAY_OF_MONTH, 20);
            calendarToStart.set(Calendar.HOUR_OF_DAY, 12);
            calendarToStart.set(Calendar.MINUTE, 10);
            calendarToStart.set(Calendar.SECOND, 20);
            long timestampToStart = calendarToStart.getTimeInMillis();
            for (int i = 0; i < 15; i++) {
                long startTime = (10000 * i) % 60000 + timestampToStart;
                long duration = (5000 * i) % 40000;
                long endTime = startTime + duration;
                Customer customer = customers.get(subIdToCustomerId[i]);
                Item item = items.get(subIdToItemId[i]);
                Subscription subscription = new Subscription();
                subscription.setCustomer(customer);
                subscription.setItem(item);
                subscription.setActive(true);

                subscription.setActiveSince(new Date(startTime));
                subscription.setActiveUntil(new Date(endTime));

                billingEngine.addSubscription(subscription);
                subscriptions.add(subscription);

                // adding purchase order - purchase order1
                Payment purchaseOrder1 = new Payment();
                purchaseOrder1.addSubscription(subscription);
                purchaseOrder1.setAmount(new Cash(payment1[i]));
                billingEngine.addPayment(purchaseOrder1);

                // adding purchase order - purchase order1
                Payment purchaseOrder2 = new Payment();
                purchaseOrder2.addSubscription(subscription);
                purchaseOrder2.setAmount(new Cash(payment2[i]));
                billingEngine.addPayment(purchaseOrder2);
            }

            succeeded = true;

        } finally {
            if (succeeded) {
                billingEngine.commitTransaction();
            } else {
                billingEngine.rollbackTransaction();
            }
        }

        billingEngine.generateBill();

        // now get the invoice of each customers
        Cash[] totalCosts = {new Cash("$1.20"), new Cash("$2.12"), new Cash("$3.24"),
                new Cash("$4.34"), new Cash("$5.82"), new Cash("$6.92")};
        Cash[] totalPayments = {new Cash("$5.50"), new Cash("$2.80"), new Cash("$5.20"),
                new Cash("$5.20"), new Cash("$5.20"), new Cash("$10.00")};
        Cash[] carriedForward = {new Cash("$-4.30"), new Cash("$-0.68"), new Cash("$-1.96"),
                new Cash("$-0.86"), new Cash("$0.62"), new Cash("$-3.08")};
        for (int i = 0; i < customers.size(); i++) {
            Customer customer = customers.get(i);
            Invoice invoice = billingEngine.getLastInvoice(customer);
            assertEquals(totalCosts[i], invoice.getTotalCost());
            assertEquals(totalPayments[i], invoice.getTotalPayment());
            assertEquals(carriedForward[i], invoice.getCarriedForward());
        }
    }
}
