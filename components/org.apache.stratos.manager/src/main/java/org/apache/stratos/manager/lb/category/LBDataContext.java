/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.lb.category;

import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Property;

import java.util.ArrayList;
import java.util.List;

public class LBDataContext {

    private String lbCategory;
    private int tenantId;
    private String autoscalePolicy;
    private String deploymentPolicy;
    private List<Property> lbProperperties;
    private List<Property> loadBalancedServiceProperties;
    private CartridgeInfo lbCartridgeInfo;


    public LBDataContext() {
        lbProperperties = new ArrayList<Property>();
        loadBalancedServiceProperties = new ArrayList<Property>();
    }

    public CartridgeInfo getLbCartridgeInfo() {
        return lbCartridgeInfo;
    }

    public void setLbCartridgeInfo(CartridgeInfo lbCartridgeInfo) {
        this.lbCartridgeInfo = lbCartridgeInfo;
    }

    public String getLbCategory() {
        return lbCategory;
    }

    public void setLbCategory(String lbCategory) {
        this.lbCategory = lbCategory;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public void setAutoscalePolicy(String autoscalePolicy) {
        this.autoscalePolicy = autoscalePolicy;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public List<Property> getLbProperperties() {
        return lbProperperties;
    }

    public void addLbProperperty (Property lbProperperty) {
        this.lbProperperties.add(lbProperperty);
    }

    public void addLBProperties(Properties loadBalancedServiceProperties) {

        for (Property property : loadBalancedServiceProperties.getProperties()) {
            addLbProperperty(property);
        }
    }

    public List<Property> getLoadBalancedServiceProperties() {
        return loadBalancedServiceProperties;
    }

    public void addLoadBalancedServiceProperty(Property loadBalancedServiceProperty) {
        this.loadBalancedServiceProperties.add(loadBalancedServiceProperty);
    }
}
