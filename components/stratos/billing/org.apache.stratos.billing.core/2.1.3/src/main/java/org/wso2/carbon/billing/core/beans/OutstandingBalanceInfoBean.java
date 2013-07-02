/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.billing.core.beans;

import java.util.Date;

/**
 * This class holds the data to be shown in the invoice summary page of all
 * tenants
 */
public class OutstandingBalanceInfoBean {

    private int invoiceId;
    private String customerName;
    private String subscription;
    private String carriedForward;
    private Date lastPaymentDate;
    private Date lastInvoiceDate;
    private String lastPaidAmount;

    public int getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(int invoiceId) {
        this.invoiceId = invoiceId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getSubscription() {
        return subscription;
    }

    public void setSubscription(String subscription) {
        this.subscription = subscription;
    }

    public String getCarriedForward() {
        return carriedForward;
    }

    public void setCarriedForward(String carriedForward) {
        this.carriedForward = carriedForward;
    }

    public Date getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(Date lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public String getLastPaidAmount() {
        return lastPaidAmount;
    }

    public void setLastPaidAmount(String lastPaidAmount) {
        this.lastPaidAmount = lastPaidAmount;
    }

    public Date getLastInvoiceDate() {
        return lastInvoiceDate;
    }

    public void setLastInvoiceDate(Date lastInvoiceDate) {
        this.lastInvoiceDate = lastInvoiceDate;
    }
}
