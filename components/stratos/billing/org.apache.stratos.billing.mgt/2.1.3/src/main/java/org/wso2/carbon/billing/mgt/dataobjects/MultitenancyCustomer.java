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
package org.wso2.carbon.billing.mgt.dataobjects;

import org.wso2.carbon.billing.core.dataobjects.Customer;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

/**
 * Multitenancy customer class
 */
public class MultitenancyCustomer extends Customer {

    private int numberOfUsers;
    private long incomingBandwidth;
    private long outgoingBandwidth;
    private long totalBandwidth;
    private long currentStorage;
    private long historyStorage;
    private long totalStorage;
    private long cartridgeCPUHours;
    private int tenantId = MultitenantConstants.INVALID_TENANT_ID;

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getNumberOfUsers() {
        return numberOfUsers;
    }

    public void setNumberOfUsers(int numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }

    public long getIncomingBandwidth() {
        return incomingBandwidth;
    }

    public void setIncomingBandwidth(long incomingBandwidth) {
        this.incomingBandwidth = incomingBandwidth;
    }

    public long getOutgoingBandwidth() {
        return outgoingBandwidth;
    }

    public void setOutgoingBandwidth(long outgoingBandwidth) {
        this.outgoingBandwidth = outgoingBandwidth;
    }

    public long getTotalBandwidth() {
        return totalBandwidth;
    }

    public void setTotalBandwidth(long totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
    }

    public long getCurrentStorage() {
        return currentStorage;
    }

    public void setCurrentStorage(long currentStorage) {
        this.currentStorage = currentStorage;
    }

    public long getHistoryStorage() {
        return historyStorage;
    }

    public void setHistoryStorage(long historyStorage) {
        this.historyStorage = historyStorage;
    }

    public long getTotalStorage() {
        return totalStorage;
    }

    public void setTotalStorage(long totalStorage) {
        this.totalStorage = totalStorage;
    }

    public long getCartridgeCPUHours() {
        return cartridgeCPUHours;
    }

    public void setCartridgeCPUHours(long cartridgeCPUHours) {
        this.cartridgeCPUHours = cartridgeCPUHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MultitenancyCustomer that = (MultitenancyCustomer) o;

        if (tenantId != that.tenantId) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + tenantId;
        return result;
    }
}
