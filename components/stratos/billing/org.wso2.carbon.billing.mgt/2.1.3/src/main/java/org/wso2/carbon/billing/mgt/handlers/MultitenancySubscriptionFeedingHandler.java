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
package org.wso2.carbon.billing.mgt.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.BillingEngineContext;
import org.wso2.carbon.billing.core.BillingException;
import org.wso2.carbon.billing.core.BillingHandler;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Item;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;
import org.wso2.carbon.billing.core.utilities.CustomerUtils;
import org.wso2.carbon.billing.mgt.api.MultitenancyBillingInfo;
import org.wso2.carbon.billing.mgt.dataobjects.MultitenancyCustomer;
import org.wso2.carbon.billing.mgt.dataobjects.MultitenancyPackage;
import org.wso2.carbon.billing.mgt.util.Util;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.wso2.carbon.usage.beans.TenantUsage;

import java.util.*;

/**
 * Gets subscriptions and feeds them to the Billing Engine context
 */
public class MultitenancySubscriptionFeedingHandler implements BillingHandler {
    private static final Log log = LogFactory.getLog(MultitenancySubscriptionFeedingHandler.class);

    private Map<Integer, MultitenancyPackage> multitenancyPackagesMap = new HashMap<Integer, MultitenancyPackage>();

    public void init(Map<String, String> handlerConfig) throws BillingException {
        DataAccessObject dataAccessObject = Util.getBillingManager().getDataAccessObject();
        MultitenancyBillingInfo billingInfo = Util.getMultitenancyBillingInfo();

        // here we are initializing the packages
        List<MultitenancyPackage> multitenancyPackages = billingInfo.getMultitenancyPackages();
        try {
            dataAccessObject.beginTransaction();
            for (MultitenancyPackage multitenancyPackage : multitenancyPackages) {
                // check the package existence in the database; If not available, insert it
                int itemId = dataAccessObject.getItemIdWithName(multitenancyPackage.getName());
                if (itemId == DataAccessObject.INVALID) {
                    itemId = dataAccessObject.addItem(multitenancyPackage);
                }
                multitenancyPackage.setId(itemId);
                multitenancyPackagesMap.put(itemId, multitenancyPackage);
                // and add all the sub items too
                for (Item subItem : multitenancyPackage.getChildren()) {
                    int subItemId = dataAccessObject.getItemId(subItem.getName(), itemId);
                    if (subItemId == DataAccessObject.INVALID) {
                        subItemId = dataAccessObject.addItem(subItem);
                    }
                    subItem.setId(subItemId);
                }
            }
            dataAccessObject.commitTransaction();
        }catch (Exception e){
            dataAccessObject.rollbackTransaction();
            log.error(e.getMessage());
            throw new BillingException(e.getMessage(), e);
        }
    }

    public void execute(BillingEngineContext handlerContext) throws BillingException {
        feedSubscriptions(handlerContext);
    }

    private void feedSubscriptions(BillingEngineContext handlerContext) throws BillingException {
        // get the subscriptions of the customer.
        Customer customer = handlerContext.getCustomer();
        List<Subscription> subscriptions = getSubscriptions(null, customer); //if the customer is null
                                                                               // this will get subscriptions
                                                                              // of all customers

        //Filtering out the subscription entries of customers who has not activated their accounts
        //This will avoid an invoice being generated for such customers
        Date endDate = new Date();

        Iterator iterator = subscriptions.iterator();
        while(iterator.hasNext()){
            Subscription subscription = (Subscription) iterator.next();
            if(!subscription.isActive() && subscription.getActiveUntil().after(endDate)){
                iterator.remove();
            }
        }

        // prepare the handler context
        handlerContext.setSubscriptions(subscriptions);
        String infoMsg = "Subscription feeding phase completed. ";
        if (subscriptions!=null){
            infoMsg += subscriptions.size() + " subscriptions fed. ";
        }else{
            infoMsg += "0 subscriptions fed. ";
        }
        log.info(infoMsg);
        // resetting the single customer back from the fed data
        // this is applicable in the interim invoice scenario
        //If this is not done, the customer object in the BillingEngine context will have
        //a null invoice
        if (customer != null && subscriptions != null && subscriptions.size() > 0) {
            Subscription subscription = subscriptions.get(0);
            handlerContext.setCustomer(subscription.getCustomer());
        }
    }

    /**
     *
     * @param itemId
     * @param isSubscriptionActive subitems for bw_overuse and storage_overuse
     * will be added only for the active subscription
     * @return
     */
    private Item getItem(int itemId, boolean isSubscriptionActive) {
        return new MultitenancyPackage(multitenancyPackagesMap.get(itemId), isSubscriptionActive);
    }

    /**
     * Retrieving tenant info and filling the customer object
     * @param customerId
     * @return
     * @throws BillingException
     */
    private Customer getCustomer(int customerId) throws BillingException {
        MultitenancyCustomer customer = new MultitenancyCustomer();
        CustomerUtils.fillCustomerData(customerId, customer);
        customer.setTenantId(customerId);

        fillTenantUsage(customer);
        return customer;
    }

    /**
     * Fill usage data of the customer
     * @param customer
     * @throws BillingException
     */
    private void fillTenantUsage(MultitenancyCustomer customer) throws BillingException {
        // first get the current month string
        Calendar calendar = Calendar.getInstance();
        String monthString = CommonUtil.getMonthString(calendar);
        try {
            int tenantId = customer.getTenantId();
            TenantUsage usage =
                    Util.getTenantUsageRetriever().getTenantUsage(tenantId, monthString);

            long currentDataCapacity = usage.getRegistryContentCapacity();
            long historyDataCapacity = usage.getRegistryContentHistoryCapacity();
            customer.setCurrentStorage(currentDataCapacity);
            customer.setHistoryStorage(historyDataCapacity);
            customer.setTotalStorage(currentDataCapacity + historyDataCapacity);

            long incomingBW = usage.getTotalIncomingBandwidth();
            long outgoingBW = usage.getTotalOutgoingBandwidth();
            customer.setIncomingBandwidth(incomingBW);
            customer.setOutgoingBandwidth(outgoingBW);
            customer.setTotalBandwidth(incomingBW + outgoingBW);

            //Getting the cartridge hours and setting it to the customer
            customer.setTotalCartridgeCPUHours(usage.getTotalCartridgeHours().getCartridgeHours());

            log.debug("Customer: " + customer.getTenantId() + " - Data Capacity: " + customer.getTotalStorage());

            customer.setNumberOfUsers(usage.getNumberOfUsers());
        } catch (Exception e) {
            String msg = "Error in getting the tenant usage for customer name: " 
                    + customer.getName() + ".";
            log.error(msg);
            throw new BillingException(msg, e);
        }
    }

    /**
     * Gets subscriptions of customer(s)
     * @param filter currently there is a filter "multitenancy" defined in the billing-config.xml
     * This will be removed in the future trunk
     * @param customer if the customer is null it gets subscriptions of all customers
     * @return
     * @throws BillingException
     */
    private List<Subscription> getSubscriptions(String filter,
                                                Customer customer)throws BillingException {
        DataAccessObject dataAccessObject = Util.getBillingManager().getDataAccessObject();
        List<Subscription> subscriptions = null;
        Map<Integer, Customer> customersCache = new HashMap<Integer, Customer>();
        try {
            dataAccessObject.beginTransaction();
            if (customer == null) {
                subscriptions = dataAccessObject.getFilteredActiveSubscriptions(filter);
            } else {
                subscriptions = dataAccessObject.getFilteredActiveSubscriptionsForCustomer(filter, customer);
            }
            if(subscriptions!=null && subscriptions.size()>0){
                for (Subscription subscription : subscriptions) {
                    Customer dummyCustomer = subscription.getCustomer();
                    int customerId = dummyCustomer.getId();
                    Customer correctCustomer = customersCache.get(customerId);
                    if (correctCustomer == null) {
                        correctCustomer = getCustomer(customerId);
                        customersCache.put(customerId, correctCustomer);
                    }
                    subscription.setCustomer(correctCustomer);

                    Item dummyItem = subscription.getItem();
                    Item correctItem = getItem(dummyItem.getId(), subscription.isActive());
                    subscription.setItem(correctItem);
                }
            }
            dataAccessObject.commitTransaction();
        }catch (Exception e){
            dataAccessObject.rollbackTransaction();
            String msg = "Error occurred while getting subscriptions";
            if(customer != null) {
                msg = msg + " for customer: " + customer.getName();
            }
            log.error(msg);
            throw new BillingException(msg, e);
        }
        return subscriptions;
    }
}
