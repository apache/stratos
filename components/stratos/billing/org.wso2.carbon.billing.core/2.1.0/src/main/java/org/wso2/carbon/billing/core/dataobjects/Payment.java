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

public class Payment {
    int id;
    Date date;
    Cash amount;
    String description;
    List<Subscription> subscriptions;
    Invoice invoice;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getDate() {
        if(date!=null){
            return new Date(date.getTime());
        }else{
            return null;
        }
    }

    public void setDate(Date date) {
        if(date!=null){
            this.date = new Date(date.getTime());
        }else{
            this.date = null;
        }
    }

    public Cash getAmount() {
        return amount;
    }

    public void setAmount(Cash amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<Subscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public void addSubscription(Subscription subscription) {
        if (this.subscriptions == null) {
            this.subscriptions = new ArrayList<Subscription>();
        }
        subscriptions.add(subscription);
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }
}
