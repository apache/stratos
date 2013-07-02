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
package org.wso2.carbon.billing.core.dataobjects;

import java.util.Date;
import java.util.List;

/**
 * Subscription class - information on subscriptions which the users
 * are subscribed to.
 */
public class Subscription {
    int id;
    Date activeSince;
    Date activeUntil;
    Item item;
    Customer customer;
    boolean active;
    int invoiceSubscriptionId = -1; // requires only if associated with an invoice
    List<Payment> payments;
    String subscriptionPlan;        //i.e. multitenance-small, multitencay-medium .....

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getActiveSince() {
        if(activeSince!=null){
            return  new Date(activeSince.getTime());
        }else{
            return null;
        }
    }

    public void setActiveSince(Date activeSince) {
        if(activeSince!=null){
            this.activeSince = new Date(activeSince.getTime());
        }else{
            this.activeSince = null;
        }

    }

    public Date getActiveUntil() {
        if(activeUntil!=null){
            return  new Date(activeUntil.getTime());
        }else{
            return null;
        }
    }

    public void setActiveUntil(Date activeUntil) {
        if(activeUntil!=null){
            this.activeUntil = new Date(activeUntil.getTime());
        }else{
            this.activeUntil = null;
        }

    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getInvoiceSubscriptionId() {
        return invoiceSubscriptionId;
    }

    public void setInvoiceSubscriptionId(int invoiceSubscriptionId) {
        this.invoiceSubscriptionId = invoiceSubscriptionId;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public String getSubscriptionPlan() {
        return subscriptionPlan;
    }

    public void setSubscriptionPlan(String subscriptionPlan) {
        this.subscriptionPlan = subscriptionPlan;
    }
}
