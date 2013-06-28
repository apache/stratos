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

import java.util.List;

public class Item {
    private int id;
    private String name;
    private Cash cost;
    private Cash creditLimit; //this is the credit limit defined by the billing rules
    private int bandwidthLimit;
    private Cash bandwidthOveruseCharge;
    private int resourceVolumeLimit;
    private Cash resourceVolumeOveruseCharge;
    private int cartridgeCPUHourLimit;
    private Cash cartridgeCPUOveruseCharge;
    private String description;
    private Item parent;
    private List<? extends Item> children;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Cash getCost() {
        return cost;
    }

    public void setCost(Cash cost) {
        this.cost = cost;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Item getParent() {
        return parent;
    }

    public void setParent(Item parent) {
        this.parent = parent;
    }

    public List<? extends Item> getChildren() {
        return children;
    }

    public void setChildren(List<? extends Item> children) {
        this.children = children;
    }

    public Cash getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(Cash creditLimit) {
        this.creditLimit = creditLimit;
    }

    public int getBandwidthLimit() {
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
    }

    public int getResourceVolumeLimit() {
        return resourceVolumeLimit;
    }

    public void setResourceVolumeLimit(int resourceVolumeLimit) {
        this.resourceVolumeLimit = resourceVolumeLimit;
    }

    public Cash getResourceVolumeOveruseCharge() {
        return resourceVolumeOveruseCharge;
    }

    public void setResourceVolumeOveruseCharge(Cash resourceVolumeOveruseCharge) {
        this.resourceVolumeOveruseCharge = resourceVolumeOveruseCharge;
    }

    public int getCartridgeCPUHourLimit() {
        return cartridgeCPUHourLimit;
    }

    public void setCartridgeCPUHourLimit(int cartridgeCPUHourLimit) {
        this.cartridgeCPUHourLimit = cartridgeCPUHourLimit;
    }

    public Cash getCartridgeCPUOveruseCharge() {
        return cartridgeCPUOveruseCharge;
    }

    public void setCartridgeCPUOveruseCharge(Cash cartridgeCPUOveruseCharge) {
        this.cartridgeCPUOveruseCharge = cartridgeCPUOveruseCharge;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Item item = (Item) o;

        if (name != null ? !name.equals(item.name) : item.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
