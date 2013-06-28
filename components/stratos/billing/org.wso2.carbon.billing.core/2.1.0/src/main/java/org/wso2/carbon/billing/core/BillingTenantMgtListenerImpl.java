/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.billing.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.billing.core.dataobjects.Item;
import org.wso2.carbon.billing.core.dataobjects.Subscription;
import org.wso2.carbon.billing.core.internal.Util;
import org.wso2.carbon.stratos.common.beans.TenantInfoBean;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.stratos.common.internal.CloudCommonServiceComponent;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.api.Tenant;
import org.wso2.carbon.user.api.TenantManager;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class BillingTenantMgtListenerImpl implements TenantMgtListener{

    private static Log log = LogFactory.getLog(BillingTenantMgtListenerImpl.class);
    /**
     * Adds the subscription entry when the tenant is created
     * @param tenantInfo Tenant information
     * @throws StratosException if adding subscription failed
     */
    public void onTenantCreate(TenantInfoBean tenantInfo) throws StratosException {
        Customer customer = new Customer();
        customer.setName(tenantInfo.getTenantDomain());
        customer.setEmail(tenantInfo.getEmail());
        customer.setStartedDate(new Date(tenantInfo.getCreatedDate().getTimeInMillis()));
        customer.setFullName(tenantInfo.getFirstname() + " " + tenantInfo.getLastname());

        customer.setId(tenantInfo.getTenantId());
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setActive(false);
        subscription.setActiveSince(Calendar.getInstance().getTime());
        subscription.setItem(new Item());
        subscription.setSubscriptionPlan(tenantInfo.getUsagePlan());
        try {
            Util.getDataAccessManager().addSubscription(subscription);
        } catch (Exception e) {
            String msg = "Could not add new subscription for tenant: " +tenantInfo.getTenantDomain();
            log.error( msg + e.getMessage(), e);
            throw new StratosException(msg, e);
        }
    }

    public void onTenantUpdate(TenantInfoBean tenantInfo) throws StratosException {
        try {
            if (tenantInfo.getUsagePlan() == null) {
                return;
            }
            Subscription currentSubscription = Util.getDataAccessManager().
                    getActiveSubscriptionOfCustomer(tenantInfo.getTenantId());
            if (currentSubscription != null && currentSubscription.getSubscriptionPlan() != null) {
                if (!currentSubscription.getSubscriptionPlan().equals(tenantInfo.getUsagePlan())) {
                    boolean updated = Util.getDataAccessManager().
                            changeSubscription(tenantInfo.getTenantId(), tenantInfo.getUsagePlan());
                    if (updated) {
                        log.info("Usage plan was changed successfully from " + currentSubscription.getSubscriptionPlan() +
                                " to " + tenantInfo.getUsagePlan());
                    }
                }
            }else{
                //tenant does not have an active subscription. First we have to check whether the tenant
                //is active. If he is active only we will add a new usage plan. Otherwise it is useless
                //to add a usage plan to an inactive tenant
                TenantManager tenantManager = CloudCommonServiceComponent.getTenantManager();
                Tenant tenant = tenantManager.getTenant(tenantInfo.getTenantId());
                if(tenant.isActive()){
                    //we add a new subscription
                    Subscription subscription = new Subscription();
                    subscription.setActive(true);
                    subscription.setSubscriptionPlan(tenantInfo.getUsagePlan());
                    subscription.setActiveSince(null);
                    subscription.setActiveUntil(null);
                    Customer customer = new Customer();
                    customer.setName(tenantInfo.getTenantDomain());
                    customer.setId(tenantInfo.getTenantId());
                    subscription.setCustomer(customer);

                    int subsId = Util.getDataAccessManager().addSubscription(subscription);
                    if(subsId>0){
                        log.info("Added a new " + subscription.getSubscriptionPlan() + " usage plan for the tenant " +
                                tenantInfo.getTenantDomain());
                    }
                }
            }
        } catch (Exception e) {
            String msg = "Error occurred while changing the subscription plan for tenant: " + tenantInfo.getTenantDomain();
            log.error(msg, e);
            throw new StratosException(msg, e);
        }
    }

    public void onTenantRename(int tenantId, String oldDomainName, String newDomainName) throws StratosException {
        //Nothing to be done
    }

    public void onTenantInitialActivation(int tenantId) throws StratosException {
        onTenantActivation(tenantId);
    }

    public void onTenantActivation(int tenantId) throws StratosException {
        //On tenant activation we need to activate the subscription

        try {

            Subscription subscription = Util.getDataAccessManager().getActiveSubscriptionOfCustomer(tenantId);

            if (subscription != null) {
                String msg = "Unable to activate the subscription for tenant: " + tenantId +
                        ". An active subscription already exists";
                log.info(msg);
            } else {
                List<Subscription> inactiveSubscriptions = Util.getDataAccessManager().getInactiveSubscriptionsOfCustomer(tenantId);
                if (inactiveSubscriptions.size() == 1) {
                    //This is the scenario where the tenant has registered, but not activated yet
                    subscription = inactiveSubscriptions.get(0);
                    boolean activated = Util.getDataAccessManager().activateSubscription(subscription.getId());
                    if (activated) {
                        log.info("Subscription was activated for tenant: " + tenantId);
                    }
                }else if(inactiveSubscriptions.size() > 1){
                    //this is the scenario where the tenant has been deactivated by admin and
                    //again activated. Here, I am adding a new active subscription which is similar to the
                    //last existed one
                    //inactiveSubscriptions.get(0) gives the latest inactive subscription
                    Subscription subscriptionToAdd = inactiveSubscriptions.get(0);
                    subscriptionToAdd.setActive(true);
                    subscriptionToAdd.setActiveSince(null);
                    subscriptionToAdd.setActiveUntil(null);

                    int subsId = Util.getDataAccessManager().addSubscription(subscriptionToAdd);
                    if(subsId>0){
                        log.info("New subscription: " + subscriptionToAdd.getSubscriptionPlan() +
                                " added and it was activated for tenant: " + tenantId);
                    }
                }else{
                    //this means there are no subscriptions. Lets handle this later
                }
            }
        } catch (Exception e) {
            String msg = "Error occurred while activating the subscription for tenant: " +
                    tenantId;
            log.error(msg, e);
            throw new StratosException(msg, e);
        }
    }

    public void onTenantDeactivation(int tenantId) throws StratosException {
        try{
            Subscription currentActiveSubscription = Util.getDataAccessManager().getActiveSubscriptionOfCustomer(tenantId);
            if(currentActiveSubscription==null){
                String msg = "There is no active subscription to deactivate for tenant: " +
                        tenantId + " on tenant deactivation";
                log.info(msg);
            }else {
                boolean deactivated = Util.getDataAccessManager().deactivateActiveSubscription(tenantId);
                if(deactivated){
                    log.info("Subscription deactivated on tenant deactivation");
                }else{
                    log.info("Subscription was not deactivated on tenant deactivation");
                }
            }
        } catch (Exception e){
            String msg = "Error occurred while deactivating the active subscription for tenant: " + tenantId;
            log.error(msg, e);
            throw new StratosException(msg, e);
        }
    }

    public void onSubscriptionPlanChange(int tenentId, String oldPlan, String newPlan) throws StratosException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getListenerOrder() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
