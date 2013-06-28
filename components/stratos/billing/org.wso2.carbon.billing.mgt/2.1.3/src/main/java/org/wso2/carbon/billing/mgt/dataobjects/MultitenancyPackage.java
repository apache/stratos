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

import org.wso2.carbon.billing.core.dataobjects.Cash;
import org.wso2.carbon.billing.core.dataobjects.Item;

import java.util.ArrayList;
import java.util.List;

public class MultitenancyPackage extends Item {
    public static final String SUBSCRIPTION_SUB_ITEM_NAME = "subscription";
    public static final String BW_OVERUSE_SUB_ITEM_NAME = "bwOveruse";
    public static final String STORAGE_OVERUSE_SUB_ITEM_NAME = "storageOveruse";
    public static final String CARTRIDGE_OVERUSE_SUB_ITEM_NAME = "cartridgeOveruse";

    private int usersLimit;
    private Cash subscriptionCharge;
    private Cash chargePerUser;
    //private int resourceVolumeLimit;
    //private int bandwidthLimit;
    //private Cash bandwidthOveruseCharge;
    private List<MultitenancyPackageSubItem> subItems;

    public MultitenancyPackage() {
        subItems = new ArrayList<MultitenancyPackageSubItem>();
        // adding subscription sub item
        MultitenancyPackageSubItem subscriptionSubItem = new MultitenancyPackageSubItem();
        subscriptionSubItem.setName(SUBSCRIPTION_SUB_ITEM_NAME);
        subscriptionSubItem.setDescription("Subscription for package");
        subscriptionSubItem.setParent(this);
        subItems.add(subscriptionSubItem);

        // adding bandwidth overuse sub item
        MultitenancyPackageSubItem bandwdithOverUseSubItem = new MultitenancyPackageSubItem();
        bandwdithOverUseSubItem.setName(BW_OVERUSE_SUB_ITEM_NAME);
        bandwdithOverUseSubItem.setDescription("Bandwidth overuse");
        bandwdithOverUseSubItem.setParent(this);
        subItems.add(bandwdithOverUseSubItem);

        // adding storage overuse sub item
        MultitenancyPackageSubItem storageOverUseSubItem = new MultitenancyPackageSubItem();
        storageOverUseSubItem.setName(STORAGE_OVERUSE_SUB_ITEM_NAME);
        storageOverUseSubItem.setDescription("Storage overuse");
        storageOverUseSubItem.setParent(this);
        subItems.add(storageOverUseSubItem);

        // adding cartridge overuse sub item
        MultitenancyPackageSubItem cartridgeOverUseSubItem = new MultitenancyPackageSubItem();
        cartridgeOverUseSubItem.setName(CARTRIDGE_OVERUSE_SUB_ITEM_NAME);
        cartridgeOverUseSubItem.setDescription("Cartridge overuse");
        cartridgeOverUseSubItem.setParent(this);
        subItems.add(cartridgeOverUseSubItem);

    }

    public MultitenancyPackage(MultitenancyPackage staticMtPackage, boolean isSubscriptionActive) {
        usersLimit = staticMtPackage.getUsersLimit();
        chargePerUser = staticMtPackage.getChargePerUser();
        subscriptionCharge = staticMtPackage.getSubscriptionCharge();
        super.setResourceVolumeLimit(staticMtPackage.getResourceVolumeLimit());
        super.setResourceVolumeOveruseCharge(staticMtPackage.getResourceVolumeOveruseCharge());
        super.setBandwidthLimit(staticMtPackage.getBandwidthLimit());
        super.setBandwidthOveruseCharge(staticMtPackage.getBandwidthOveruseCharge());
        super.setCartridgeCPUHourLimit(staticMtPackage.getCartridgeCPUHourLimit());
        super.setCartridgeCPUOveruseCharge(staticMtPackage.getCartridgeCPUOveruseCharge());
        setId(staticMtPackage.getId());
        setName(staticMtPackage.getName());
        setCost(staticMtPackage.getCost());
        setDescription(staticMtPackage.getDescription());

        subItems = new ArrayList<MultitenancyPackageSubItem>();
        for (Item subItem : staticMtPackage.getChildren()) {
            //Storage overuse and Bandwidth overuse sub items will be added only to active subscription
            if(isSubscriptionActive || (!isSubscriptionActive && SUBSCRIPTION_SUB_ITEM_NAME.equals(subItem.getName()))){

                MultitenancyPackageSubItem subscriptionSubItem = new MultitenancyPackageSubItem();
                subscriptionSubItem.setId(subItem.getId());
                subscriptionSubItem.setName(subItem.getName());
                subscriptionSubItem.setDescription(subItem.getDescription());
                subscriptionSubItem.setParent(this);
                subItems.add(subscriptionSubItem);
            }
        }
    }

    public int getUsersLimit() {
        return usersLimit;
    }

    public void setUsersLimit(int usersLimit) {
        this.usersLimit = usersLimit;
    }

    public Cash getChargePerUser() {
        return chargePerUser;
    }

    public void setChargePerUser(Cash chargePerUser) {
        this.chargePerUser = chargePerUser;
    }

    public Cash getSubscriptionCharge() {
        return subscriptionCharge;
    }

    public void setSubscriptionCharge(Cash subscriptionCharge) {
        this.subscriptionCharge = subscriptionCharge;
    }

    /*public int getResourceVolumeLimit() {
        return resourceVolumeLimit;
    }

    public void setResourceVolumeLimit(int resourceVolumeLimit) {
        this.resourceVolumeLimit = resourceVolumeLimit;
    }
    */
    /*public int getBandwidthLimit() {
        return bandwidthLimit;
    }

    public void setBandwidthLimit(int bandwidthLimit) {
        this.bandwidthLimit = bandwidthLimit;
    }

    public Cash getBandwidthOveruseCharge() {
        return bandwidthOveruseCharge;
    }

    public void setBandwidthOveruseCharge(Cash bandwidthOveruseCharge) {
        this.bandwidthOveruseCharge = bandwidthOveruseCharge;
    }*/

    @Override
    public List<? extends Item> getChildren() {
        return subItems;
    }
}
