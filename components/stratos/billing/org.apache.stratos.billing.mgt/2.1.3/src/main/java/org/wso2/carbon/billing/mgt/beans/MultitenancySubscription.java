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

public class MultitenancySubscription {
    String subscribedPackage;
    BilledEntry[] billedEntries;
    Date activeSince;
    Date activeUntil;
    boolean isActive;

    public String getSubscribedPackage() {
        return subscribedPackage;
    }

    public void setSubscribedPackage(String subscribedPackage) {
        this.subscribedPackage = subscribedPackage;
    }

    public BilledEntry[] getBilledEntries() {
        return Arrays.copyOf(billedEntries, billedEntries.length);
    }

    public void setBilledEntries(BilledEntry[] billedEntries) {
        this.billedEntries = Arrays.copyOf(billedEntries, billedEntries.length);
    }

    public Date getActiveSince() {
        return new Date(activeSince.getTime());
    }

    public void setActiveSince(Date activeSince) {
        this.activeSince = new Date(activeSince.getTime());
    }

    public Date getActiveUntil() {
        return new Date(activeUntil.getTime());
    }

    public void setActiveUntil(Date activeUntil) {
        this.activeUntil = new Date( activeUntil.getTime());
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
