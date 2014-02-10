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
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.subscriber.Subscriber;

import java.util.Map;

public class LBCategoryContext {

    private String lbType;
    private PayloadData payloadData;
    private Properties properties;
    private Cluster cluster;
    private String autoscalingPolicyName;
    private String deploymentPolicyName;
    private Map<String, String> customPayloadEntries;
    private CartridgeInfo cartridgeInfo;
    private String key;
    private String loadbalancedServiceType;
    private String subscriptionAlias;
    private Subscriber subscriber;

    public LBCategoryContext (String lbType, Cluster cluster, String autoscalingPolicyName, String deploymentPolicyName,
                              CartridgeInfo cartridgeInfo, PayloadData payloadData, String key) {

        this.lbType = lbType;
        this.cluster = cluster;
        this.autoscalingPolicyName = autoscalingPolicyName;
        this.deploymentPolicyName = deploymentPolicyName;
        this.cartridgeInfo = cartridgeInfo;
        this.payloadData = payloadData;
        this.key = key;
    }

    public PayloadData getPayloadData() {
        return payloadData;
    }

    public void setPayloadData(PayloadData payloadData) {
        this.payloadData = payloadData;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getAutoscalingPolicyName() {
        return autoscalingPolicyName;
    }

    public void setAutoscalingPolicyName(String autoscalingPolicyName) {
        this.autoscalingPolicyName = autoscalingPolicyName;
    }

    public String getDeploymentPolicyName() {
        return deploymentPolicyName;
    }

    public void setDeploymentPolicyName(String deploymentPolicyName) {
        this.deploymentPolicyName = deploymentPolicyName;
    }

    public Map<String, String> getCustomPayloadEntries() {
        return customPayloadEntries;
    }

    public void setCustomPayloadEntries(Map<String, String> customPayloadEntries) {
        this.customPayloadEntries = customPayloadEntries;
    }

    public CartridgeInfo getCartridgeInfo() {
        return cartridgeInfo;
    }

    public void setCartridgeInfo(CartridgeInfo cartridgeInfo) {
        this.cartridgeInfo = cartridgeInfo;
    }

    public String getLbType() {
        return lbType;
    }

    public void setLbType(String lbType) {
        this.lbType = lbType;
    }

    public String getLoadbalancedServiceType() {
        return loadbalancedServiceType;
    }

    public void setLoadbalancedServiceType(String loadbalancedServiceType) {
        this.loadbalancedServiceType = loadbalancedServiceType;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSubscriptionAlias() {
        return subscriptionAlias;
    }

    public void setSubscriptionAlias(String subscriptionAlias) {
        this.subscriptionAlias = subscriptionAlias;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public void setSubscriber(Subscriber subscriber) {
        this.subscriber = subscriber;
    }
}
