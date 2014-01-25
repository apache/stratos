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

package org.apache.stratos.manager.deploy.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.cloud.controller.pojo.Property;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.payload.BasicPayloadData;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.payload.PayloadFactory;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.io.Serializable;

public abstract class Service implements Serializable {

    private static Log log = LogFactory.getLog(Service.class);

    private String type;
    private String autoscalingPolicyName;
    private String deploymentPolicyName;
    private String tenantRange;
    private String clusterId;
    private String hostName;
    private int tenantId;
    private String subscriptionKey;
    private CartridgeInfo cartridgeInfo;
    private PayloadData payloadData;

    public Service (String type, String autoscalingPolicyName, String deploymentPolicyName, int tenantId, CartridgeInfo cartridgeInfo,
    		String tenantRange) {

        this.type = type;
        this.autoscalingPolicyName = autoscalingPolicyName;
        this.deploymentPolicyName = deploymentPolicyName;
        this.tenantId = tenantId;
        this.cartridgeInfo = cartridgeInfo;
        this.tenantRange = tenantRange;
        this.subscriptionKey = CartridgeSubscriptionUtils.generateSubscriptionKey();
    }

    public void deploy (Properties properties) throws ADCException, UnregisteredCartridgeException {

        //generate the cluster ID (domain)for the service
        setClusterId(type + "." + cartridgeInfo.getHostName() + ".domain");
        //host name is the hostname defined in cartridge definition
        setHostName(cartridgeInfo.getHostName());

        //Create payload
        BasicPayloadData basicPayloadData = CartridgeSubscriptionUtils.createBasicPayload(this);
        //populate
        basicPayloadData.populatePayload();
        PayloadData payloadData = PayloadFactory.getPayloadDataInstance(cartridgeInfo.getProvider(),
                cartridgeInfo.getType(), basicPayloadData);

        // get the payload parameters defined in the cartridge definition file for this cartridge type
        if (cartridgeInfo.getProperties() != null && cartridgeInfo.getProperties().length != 0) {

            for (Property property : cartridgeInfo.getProperties()) {
                // check if a property is related to the payload. Currently this is done by checking if the
                // property name starts with 'payload_parameter.' suffix. If so the payload param name will
                // be taken as the substring from the index of '.' to the end of the property name.
                if (property.getName()
                        .startsWith(CartridgeConstants.CUSTOM_PAYLOAD_PARAM_NAME_PREFIX)) {
                    String payloadParamName = property.getName();
                    payloadData.add(payloadParamName.substring(payloadParamName.indexOf(".") + 1), property.getValue());
                }
            }
        }

        //set PayloadData instance
        setPayloadData(payloadData);
    }

    public void undeploy () throws ADCException {

        try {
            CloudControllerServiceClient.getServiceClient().terminateAllInstances(clusterId);

        } catch (Exception e) {
            String errorMsg = "Error in undeploying Service with type " + type;
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }

        log.info("terminated instance with Service Type " + type);

        try {
            CloudControllerServiceClient.getServiceClient().unregisterService(clusterId);

        } catch (Exception e) {
            String errorMsg = "Error in unregistering service cluster with domain " + clusterId;
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Unregistered service with domain " + clusterId);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public CartridgeInfo getCartridgeInfo() {
        return cartridgeInfo;
    }

    public void setCartridgeInfo(CartridgeInfo cartridgeInfo) {
        this.cartridgeInfo = cartridgeInfo;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }

    public void setSubscriptionKey(String subscriptionKey) {
        this.subscriptionKey = subscriptionKey;
    }

    public PayloadData getPayloadData() {
        return payloadData;
    }

    public void setPayloadData(PayloadData payloadData) {
        this.payloadData = payloadData;
    }
}
