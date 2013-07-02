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
package org.wso2.carbon.billing.mgt.beans;

import java.util.Arrays;
import java.util.Date;

public class MultitenancyInvoice {
    private int invoiceId;
    private Date billingDate;
    private Date startDate;
    private Date endDate;
    private String boughtForward;
    private String carriedForward;
    private String totalPayments;
    private String totalCost;
    private boolean lastInvoice;
    private MultitenancySubscription[] subscriptions;
    private MultitenancyPurchaseOrder[] purchaseOrders;

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public Date getBillingDate() {
        return new Date(billingDate.getTime());
    }

    public void setBillingDate(Date billingDate) {
        this.billingDate = new Date(billingDate.getTime());
    }

    public Date getStartDate() {
        return new Date(startDate.getTime());
    }

    public void setStartDate(Date startDate) {
        this.startDate = new Date(startDate.getTime());
    }

    public Date getEndDate() {
        return new Date(endDate.getTime());
    }

    public void setEndDate(Date endDate) {
        this.endDate = new Date(endDate.getTime());
    }

    public String getBoughtForward() {
        return boughtForward;
    }

    public void setBoughtForward(String boughtForward) {
        this.boughtForward = boughtForward;
    }

    public String getCarriedForward() {
        return carriedForward;
    }

    public void setCarriedForward(String carriedForward) {
        this.carriedForward = carriedForward;
    }

    public String getTotalPayments() {
        return totalPayments;
    }

    public void setTotalPayments(String totalPayments) {
        this.totalPayments = totalPayments;
    }

    public String getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(String totalCost) {
        this.totalCost = totalCost;
    }

    public boolean isLastInvoice() {
        return lastInvoice;
    }

    public void setLastInvoice(boolean lastInvoice) {
        this.lastInvoice = lastInvoice;
    }

    public MultitenancySubscription[] getSubscriptions() {
        return Arrays.copyOf(subscriptions, subscriptions.length);
    }

    public void setSubscriptions(MultitenancySubscription[] subscriptions) {
        this.subscriptions = Arrays.copyOf(subscriptions, subscriptions.length);
    }

    public MultitenancyPurchaseOrder[] getPurchaseOrders() {
        return Arrays.copyOf(purchaseOrders, purchaseOrders.length);
    }

    public void setPurchaseOrders(MultitenancyPurchaseOrder[] purchaseOrders) {
        this.purchaseOrders = Arrays.copyOf(purchaseOrders, purchaseOrders.length);
    }
}
