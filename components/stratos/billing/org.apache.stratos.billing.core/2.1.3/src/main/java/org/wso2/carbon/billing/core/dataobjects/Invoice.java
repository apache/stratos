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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Invoice class - holds the invoice information
 */
public class Invoice {
    int id;
    Date date;
    Date startDate;
    Date endDate;
    Cash boughtForward;
    Cash carriedForward;
    Cash totalCost;
    Cash totalPayment;
    Customer customer;
    List<Subscription> subscriptions = new ArrayList<Subscription>();
    List<Payment> payments = new ArrayList<Payment>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        return new Date(date.getTime());
    }

    public void setDate(Date date) {
        this.date = new Date(date.getTime());
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

    public Cash getBoughtForward() {
        return boughtForward;
    }

    public void setBoughtForward(Cash boughtForward) {
        this.boughtForward = boughtForward;
    }

    public Cash getCarriedForward() {
        return carriedForward;
    }

    public void setCarriedForward(Cash carriedForward) {
        this.carriedForward = carriedForward;
    }

    public Cash getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Cash totalCost) {
        this.totalCost = totalCost;
    }

    public Cash getTotalPayment() {
        return totalPayment;
    }

    public void setTotalPayment(Cash totalPayment) {
        this.totalPayment = totalPayment;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public void addPayment(Payment payment) {
        this.payments.add(payment);
    }
}
