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
package org.wso2.carbon.billing.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.billing.core.jdbc.DataAccessObject;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.TenantManager;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;


public class DataAccessManager {

    private static Log log = LogFactory.getLog(DataAccessManager.class);
    private DataAccessObject dataAccessObject = null;

    public DataAccessManager(DataSource dataSource) {
        this.dataAccessObject = new DataAccessObject(dataSource);
    }
    
    public DataAccessManager(DataAccessObject dao){
        this.dataAccessObject = dao;
    }

    public void beginTransaction() throws BillingException {
        dataAccessObject.beginTransaction();
    }

    public void commitTransaction() throws BillingException {
        dataAccessObject.commitTransaction();
    }

    public void rollbackTransaction() throws BillingException {
        dataAccessObject.rollbackTransaction();
    }

    public int addSubscription(Subscription subscription) throws BillingException {
        int subscriptionId = 0;
        try {
            beginTransaction();
            subscriptionId = dataAccessObject.addSubscription(subscription,
                                subscription.getSubscriptionPlan());
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while adding subscription: " + subscription.getSubscriptionPlan()+
                            " for the customer " + subscription.getCustomer().getName() + " " + e.getMessage() ;
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscriptionId;
    }

    public void deleteBillingData(int tenantId) throws BillingException {
        try {
            beginTransaction();
            dataAccessObject.deleteBillingData(tenantId);
            commitTransaction();
        } catch (Exception e) {
            rollbackTransaction();
            String msg = "Error occurred while deleting subscription for tenant id: " + tenantId ;
            log.error(msg, e);
            throw new BillingException(msg, e);
        }
    }

    public List<Customer> getCustomersWithName(String customerName) throws BillingException {
        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        List<Customer> customers = new ArrayList<Customer>();
        try {
            int tenantId = tenantManager.getTenantId(customerName);
            Tenant tenant = tenantManager.getTenant(tenantId);
            if (tenant != null) {
                Customer customer = new Customer();
                customer.setId(tenant.getId());
                customer.setName(tenant.getDomain());
                customer.setStartedDate(tenant.getCreatedDate());
                customer.setEmail(tenant.getEmail());
                //customer.setAddress();
                customers.add(customer);
            }
        } catch (Exception e) {
            String msg = "Failed to get customers for customers: " + customerName + ".";
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        return customers;
    }

    public Subscription getSubscription(int subscriptionId) throws BillingException {
        Subscription subscription = null;
        try {
            beginTransaction();
            subscription = dataAccessObject.getSubscription(subscriptionId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting subscription with id: " + subscriptionId +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscription;
    }

    public Subscription getActiveSubscriptionOfCustomer(int customerId) throws BillingException {
        Subscription subscription;
        try {
            beginTransaction();
            subscription = dataAccessObject.getActiveSubscriptionOfCustomer(customerId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting active subscription for customer: "
                            + customerId + " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscription;
    }

    public int getItemIdWithName(String name, int parentId) throws BillingException {
        int itemId;
        try {
            beginTransaction();
            itemId = dataAccessObject.getItemId(name, parentId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting item id for item name: " + name +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return itemId;
    }

    public boolean changeSubscription(int customerId, String subscriptionPlan) throws BillingException {
        boolean changed = false;
        String oldSubscriptionPlan = null;

        try {
            beginTransaction();
            Subscription oldSubscription = getActiveSubscriptionOfCustomer(customerId);
            if(oldSubscription!=null){
                oldSubscriptionPlan = oldSubscription.getSubscriptionPlan();
            }
            changed = dataAccessObject.changeSubscription(customerId, subscriptionPlan);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while changing subscription to: " + subscriptionPlan +
                            " for customer: " + customerId + " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }

        try {
            // Notify Listeners about the subscription change
            Util.alertTenantSubscriptionPlanChange(
                    customerId, oldSubscriptionPlan, subscriptionPlan);
        } catch (StratosException e) {
            log.error(e.getMessage(), e);
            throw new BillingException(e.getMessage(), e);
        }

        return changed;
    }

    public List<Subscription> getInactiveSubscriptionsOfCustomer(int customerId) throws BillingException {
        List<Subscription> subscriptions;
        try {
            beginTransaction();
            subscriptions = dataAccessObject.getInactiveSubscriptionsOfCustomer(customerId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while getting inactive subscriptions of customer: " +
                            customerId + " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return subscriptions;

    }

    public boolean activateSubscription(int subscriptionId) throws BillingException {
        boolean activated = false;
        try {
            beginTransaction();
            activated = dataAccessObject.activateSubscription(subscriptionId);
            commitTransaction();
        } catch(Exception e){
            String msg = "Error occurred while activating subscription with id: " + subscriptionId +
                            " " + e.getMessage();
            log.error(msg, e);
            rollbackTransaction();
            throw new BillingException(msg, e);
        }
        return activated;
    }

    public boolean deactivateActiveSubscription(int tenantId) throws BillingException {
        boolean deactivated = false;
        try{
            beginTransaction();
            deactivated = dataAccessObject.deactivateCurrentSubscriptoin(tenantId);
            commitTransaction();
        }catch(Exception e){
            rollbackTransaction();
            String msg = "Error occurred while deactivating the active subscription of customer: " +
                            tenantId;
            log.error(msg, e);
            throw new BillingException(msg, e);
        }

        return deactivated;
    }



}
