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
package org.wso2.carbon.billing.mgt.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.DataAccessManager;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.mgt.util.Util;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.tenant.TenantManager;

import java.util.List;

/**
 * This service allows other components to access billing related data
 * without going through a billing manager
 */
public class BillingDataAccessService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(BillingDataAccessService.class);

    /**
     * Add a new subscription to the BC_SUBSCRIPTION table
     * @param subscription  object with subscription info
     * @return the subscription id of the added subscription
     * @throws Exception Exception
     */
    public int addSubscription(Subscription subscription) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        return dataAccessManager.addSubscription(subscription);
    }

    /**
     * Delete a particular tenants subscription and all related data
     * @param tenantId id of the tenant whose billing details should be deleted
     * @throws Exception thorwn if an error is occured while deleting data
     */
    public void deleteBillingData(int tenantId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        dataAccessManager.deleteBillingData(tenantId);
    }

    /**
     * Finds the customer with a given tenant domain
     * @param customerName  is the tenant domain
     * @return  a customer object
     * @throws Exception Exception
     */
    public Customer getCustomerWithName(String customerName) throws Exception {

        //This is invoked by tenants only. Therefore securing this by checking 
        //whether the string passed is actually the domain of current tenant
        UserRegistry userRegistry = (UserRegistry) getGovernanceUserRegistry();
        int currentTenantId = userRegistry.getTenantId();

        TenantManager tenantManager = Util.getRealmService().getTenantManager();
        String currentTenantDomain = tenantManager.getDomain(currentTenantId);

        if(!customerName.equals(currentTenantDomain)){
            String msg = "Tenant: " + currentTenantDomain + " is trying to get customer object of tenant: " +
                    customerName + ".";
            log.error(msg);
            throw new Exception(msg);
        }

        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        Customer customer = null;
        List<Customer> customers = dataAccessManager.getCustomersWithName(customerName);
        if (customers.size() > 0) {
            customer = customers.get(0);
        }
        return customer;
    }

    /**
     * Get a subscription with a given id
     * @param subscriptionId subscription id
     * @return a subscription object
     * @throws Exception Exception
     */
    public Subscription getSubscription(int subscriptionId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        return dataAccessManager.getSubscription(subscriptionId);
    }


    /**
     * Method called by super-tenant to get the active subscription of a tenant
     * @param tenantId Tenant Id
     * @return subscription details
     * @throws Exception Exception
     */
    public Subscription getActiveSubscriptionOfCustomerBySuperTenant(int tenantId) throws Exception{
        return getActiveSubscriptionOfCustomer(tenantId);
    }

    /**
     * Method called by tenant to get the active subscription
     * @return subscription details
     * @throws Exception Exception
     */
    public Subscription getActiveSubscriptionOfCustomerByTenant() throws Exception{
        UserRegistry userRegistry = (UserRegistry) getGovernanceUserRegistry();
        int tenantId = userRegistry.getTenantId();

        return getActiveSubscriptionOfCustomer(tenantId);
    }


    /**
     * Gets the item id for a given item name and a parent id
     * For example "subscription" item id of Demo subscription
     * @param name  e.g. "subscription", "bwOveruse", "storageOveruse"
     * @param parentId there is a parent item in BC_ITEM
     * @return  the item id from the BC_ITEM table
     * @throws Exception Exception
     */
    public int getItemIdWithName(String name, int parentId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        return dataAccessManager.getItemIdWithName(name, parentId);
    }

    /**
     * This is used by the tenants 
     * @param subscriptionPlan new Usage plan name that user expect to go
     * @return whether the operation was successful or not
     * @throws Exception Exception
     */
    public boolean changeSubscriptionByTenant(String subscriptionPlan) throws Exception {

        UserRegistry userRegistry = (UserRegistry) getGovernanceUserRegistry();
        int tenantId = userRegistry.getTenantId();

        return changeSubscription(tenantId, subscriptionPlan);

    }

    /**
     * This is used by the super tenant
     * @param customerId    this is the tenant id
     * @param subscriptionPlan  new usage plan name
     * @return  whether the operation was successful or not
     * @throws Exception Exception
     */
    public boolean changeSubscriptionBySuperTenant(int customerId, String subscriptionPlan) throws Exception {

        return changeSubscription(customerId, subscriptionPlan);
    }

    /**
     * Gets the inactive subscriptions of a customer ordered by ACTIVE_SINCE time
     * in the descending order (i.e. latest ones first)
     * @param customerId this is the tenant id
     * @return  an array of subscriptions
     * @throws Exception Exception
     */
    public Subscription[] getInactiveSubscriptionsOfCustomer(int customerId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        List<Subscription> subscriptions = dataAccessManager.getInactiveSubscriptionsOfCustomer(customerId);
        Subscription[] subscriptionArray;
        if (subscriptions != null && subscriptions.size() > 0) {
            subscriptionArray = subscriptions.toArray(new Subscription[subscriptions.size()]);
        } else {
            subscriptionArray = new Subscription[0];
        }
        return subscriptionArray;
    }

    /**
     * Activate a subscription with a given id
     * @param subscriptionId is the id of subscription which needs to be activated
     * @return  true or false based on whether the operation was successful or not
     * @throws Exception Exception
     */
    public boolean activateSubscription(int subscriptionId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        return dataAccessManager.activateSubscription(subscriptionId);
    }

    /**
     * Method called by tenants when deactivating the active subscriptions
     * @return true | false based on whether the operation was successful or not
     * @throws Exception Exception
     */
    public boolean deactivateActiveSubscriptionByTenant() throws Exception{
        UserRegistry registry = (UserRegistry) getGovernanceUserRegistry();
        int currentTenantId = registry.getTenantId();

        return deactivateActiveSubscription(currentTenantId);
    }


    /**
     * Method called by super-tenant
     * @param tenantId Tenant Id
     * @return true | false based on whether the operation was successful or not
     * @throws Exception Exception
     */
    public boolean deactivateActiveSubscriptionBySuperTenant(int tenantId) throws Exception{
        return deactivateActiveSubscription(tenantId);
    }
    


    /**
     * This is the private method called by tenant or super tenant operations when
     * changing(updating) the subscription plan
     * @param tenantId Tenant ID
     * @param subscriptionPlan new usage plan (subscription plan) name
     * @return true or false based on whether the operation was successful
     * @throws Exception Exception
     */
    private boolean changeSubscription(int tenantId, String subscriptionPlan) throws Exception {

        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        if (dataAccessManager.changeSubscription(tenantId, subscriptionPlan)) {
            try {
                Util.executeThrottlingRules(tenantId);
            }
            catch (Exception e) {
                log.error("Error occurred executing throttling rules after updating the subscription " +
                        "to " + subscriptionPlan + ". " + e.toString());
            }
            //send mail
            return true;
        } else {
            return false;
        }
    }

    /**
     * Gets the active subscription of a customer. There can be only one active
     * subscription for a customer at a given time
     * @param tenantId Tenant Id
     * @return  a subscription object
     * @throws Exception Exception
     */
    private Subscription getActiveSubscriptionOfCustomer(int tenantId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        return dataAccessManager.getActiveSubscriptionOfCustomer(tenantId);
    }

    /**
     * Deactivates the active subscription of a customer
     * @param tenantId is the customer id (both have the same meaning)
     * @return true or false based on whether the operation was successful or not
     * @throws Exception Exception
     */
    private boolean deactivateActiveSubscription(int tenantId) throws Exception {
        DataAccessManager dataAccessManager = Util.getDataAccessManager();
        return dataAccessManager.deactivateActiveSubscription(tenantId);
    }

    /**
     * This method is used to update the usage plan when updating the from Demo to a paying plan
     * @param subscriptionPlan new Usage plan name that user expect to go
     * @param tenantDomain the domain of the tenant that should be updated 
     * @return whether the operation was successful or not
     * @throws Exception Exception
     */
    public boolean changeSubscriptionForTenant(String subscriptionPlan, String tenantDomain) throws Exception {
        int tenantId = Util.getRealmService().getTenantManager().getTenantId(tenantDomain);
        return changeSubscription(tenantId, subscriptionPlan);
    }

}
