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

/**
 * Customer class - This holds the information of tenants
 * i.e. customer=tenant
 */
public class Customer {
    private int id;
    private String name;
    private String context;
    private String fullName;
    private String email;
    private Date startedDate;
    private String address;
    private Invoice activeInvoice;
    private long totalBandwidth;
    private long totalStorage;
    private long totalCartridgeCPUHours;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Date getStartedDate() {
        return new Date(startedDate.getTime());
    }

    public void setStartedDate(Date startedDate) {
        this.startedDate = new Date(startedDate.getTime());
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * This will be used by rules to retrieve the active invoice
     *
     * @return
     */
    public final Invoice getActiveInvoice() {
        return activeInvoice;
    }

    public final void setActiveInvoice(Invoice invoice) {
        this.activeInvoice = invoice;
    }

    public long getTotalBandwidth() {
        return totalBandwidth;
    }

    public void setTotalBandwidth(long totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
    }

    public long getTotalStorage() {
        return totalStorage;
    }

    public void setTotalStorage(long totalStorage) {
        this.totalStorage = totalStorage;
    }

    public long getTotalCartridgeCPUHours() {
        return totalCartridgeCPUHours;
    }

    public void setTotalCartridgeCPUHours(long totalCartridgeCPUHours) {
        this.totalCartridgeCPUHours = totalCartridgeCPUHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Customer customer = (Customer) o;

        if (id != customer.id) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
