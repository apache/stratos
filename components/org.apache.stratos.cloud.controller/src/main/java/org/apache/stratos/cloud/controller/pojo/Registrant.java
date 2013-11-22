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
package org.apache.stratos.cloud.controller.pojo;

import org.apache.stratos.cloud.controller.util.Properties;

/**
 * Upon a new subscription, Stratos Manager would send this POJO.
 * @author nirmal
 *
 */
public class Registrant {

    private String clusterId;
    private String tenantRange;
    private String cartridgeType;
    private String hostName;
    private Properties properties;
    private String payload;
    private String autoScalerPolicyName;
    
    public String getClusterId() {
        return clusterId;
    }
    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }
    public String getTenantRange() {
        return tenantRange;
    }
    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }
    public String getCartridgeType() {
        return cartridgeType;
    }
    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }
    public String getHostName() {
        return hostName;
    }
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    public Properties getProperties() {
        return properties;
    }
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }
    public String getAutoScalerPolicyName() {
        return autoScalerPolicyName;
    }
    public void setAutoScalerPolicyName(String autoScalerPolicyName) {
        this.autoScalerPolicyName = autoScalerPolicyName;
    }
    
}
