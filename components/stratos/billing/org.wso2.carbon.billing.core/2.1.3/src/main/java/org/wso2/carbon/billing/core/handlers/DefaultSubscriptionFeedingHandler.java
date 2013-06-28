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
import org.wso2.carbon.billing.core.dataobjects.Item;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;
import org.wso2.carbon.billing.core.utilities.CustomerUtils;

import java.util.List;
import java.util.Map;

/**
 * This class finds all relevant subscription entries for
 * the period considered in billing (i.e. billing cycle)
 */
public class DefaultSubscriptionFeedingHandler implements BillingHandler {

    private static Log log = LogFactory.getLog(DefaultSubscriptionFeedingHandler.class);

    public void init(Map<String, String> handlerConfig) throws BillingException {
        // nothing to initialize
    }

    /**
     *
     * @param handlerContext - BillingEngineContext which keeps the data
     * used in the bill generation process
     * @throws BillingException
     */
    public void execute(BillingEngineContext handlerContext) throws BillingException {
        feedSubscriptions(handlerContext);
    }

    /**
     * Finds the subscription of all the tenants applicable for the period being
     * considered in bill generation
     * @param handlerContext
     * @throws BillingException
     */
    private void feedSubscriptions(BillingEngineContext handlerContext) throws BillingException {
        // get the subscriptions right here..
        Customer customer = handlerContext.getCustomer();
        List<Subscription> subscriptions = getFilteredActiveSubscriptions(null, customer);
        // prepare the handler context
        handlerContext.setSubscriptions(subscriptions);
        String infoMsg = "Subscription feeding phase completed. ";
        if (subscriptions!=null){
            infoMsg += subscriptions.size() + " subscriptions fed. ";
        }else{
            infoMsg += "0 subscriptions fed. ";
        }
        log.info(infoMsg);
    }

    /**
     *
     * @param filter - currently we pass the string "multitencay" because we
     * need all the subscriptions. But, if you want subscriptions of a specific usage plan
     * then you can pass it here. 
     * @param customer - if generating an interim invoice, this is applicable.
     * otherwise this will be null
     * @return a list of subscriptions
     * @throws BillingException
     */
    private List<Subscription> getFilteredActiveSubscriptions(String filter,
                                                Customer customer) throws BillingException {
        DataAccessObject dataAccessObject = BillingManager.getInstance().getDataAccessObject();
        List<Subscription> subscriptions = null;
        try {
            dataAccessObject.beginTransaction();
            if (customer == null) {
                subscriptions = dataAccessObject.getFilteredActiveSubscriptions(filter);
            } else {
                subscriptions = dataAccessObject.getFilteredActiveSubscriptionsForCustomer(filter,
                                                                                           customer);
            }

            // iterate through all the subscriptions and assign correct customer and item
            for (Subscription subscription : subscriptions) {
                Customer dummyCustomer = subscription.getCustomer();
                Customer correctCustomer = getCustomer(dummyCustomer.getId());
                subscription.setCustomer(correctCustomer);

                Item dummyItem = subscription.getItem();
                Item correctItem = getItem(dummyItem.getId());
                subscription.setItem(correctItem);
            }
            dataAccessObject.commitTransaction();
        }catch(Exception e){
            String msg = "Error occurred while feeding subscription entries: " +
                            e.getMessage();
            log.error(msg);
            dataAccessObject.rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscriptions;
    }

    /**
     * Gets the billable item for a subscription
     * @param itemId in the subscription entry
     * @return the item retrieved from the db
     * @throws BillingException
     */
    private Item getItem(int itemId) throws BillingException {
        DataAccessObject dataAccessObject = BillingManager.getInstance().getDataAccessObject();
        Item item = null;
        try {
            dataAccessObject.beginTransaction();
            item = dataAccessObject.getItem(itemId);
            dataAccessObject.commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while retrieving item for item id: " + itemId +
                            ": " + e.getMessage();
            dataAccessObject.rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return item;
    }

    private Customer getCustomer(int customerId) throws BillingException {
        Customer customer = new Customer();
        CustomerUtils.fillCustomerData(customerId, customer);
        return customer;
    }
}
