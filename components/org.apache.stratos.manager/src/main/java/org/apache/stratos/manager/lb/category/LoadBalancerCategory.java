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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Property;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.payload.BasicPayloadData;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.payload.PayloadFactory;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.util.Map;
import java.util.Set;

public abstract class LoadBalancerCategory {

    private static Log log = LogFactory.getLog(LoadBalancerCategory.class);

    private LBCategoryContext lbCategoryContext;

    public LoadBalancerCategory (LBCategoryContext lbCategoryContext) {
        this.setLbCategoryContext(lbCategoryContext);
    }

    public void register () throws ADCException, UnregisteredCartridgeException {

        log.info("Payload: " + getLbCategoryContext().getPayloadData().getCompletePayloadData().toString());

        ApplicationManagementUtil.registerService(getLbCategoryContext().getLbType(),
                getLbCategoryContext().getCluster().getClusterDomain(),
                getLbCategoryContext().getCluster().getClusterSubDomain(),
                getLbCategoryContext().getPayloadData().getCompletePayloadData(),
                getLbCategoryContext().getPayloadData().getBasicPayloadData().getTenantRange(),
                getLbCategoryContext().getCluster().getHostName(),
                getLbCategoryContext().getAutoscalingPolicyName(),
                getLbCategoryContext().getDeploymentPolicyName(),
                getLbCategoryContext().getProperties());
    }

    public PayloadData createPayload() throws ADCException {

        //Create the payload
        BasicPayloadData basicPayloadData = CartridgeSubscriptionUtils.createBasicPayload(lbCategoryContext);
        //Populate the basic payload details
        basicPayloadData.populatePayload();

        CartridgeInfo cartridgeInfo = getLbCategoryContext().getCartridgeInfo();
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

        //check if there are any custom payload entries defined
        if (getLbCategoryContext().getCustomPayloadEntries() != null) {
            //add them to the payload
            Map<String, String> customPayloadEntries = getLbCategoryContext().getCustomPayloadEntries();
            Set<Map.Entry<String,String>> entrySet = customPayloadEntries.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                payloadData.add(entry.getKey(), entry.getValue());
            }
        }

        return payloadData;
    }

    public void unregister () throws ADCException {

        try {
            CloudControllerServiceClient.getServiceClient().terminateAllInstances(getLbCategoryContext().getCluster().getClusterDomain());

        } catch (AxisFault e) {
            String errorMsg = "Error in terminating cartridge subscription, cluster id: " + getLbCategoryContext().getCluster().getClusterDomain();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = "Error in terminating cartridge subscription, cluster id: " + getLbCategoryContext().getCluster().getClusterDomain();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Terminated all instances of " + getLbCategoryContext().getCluster().getClusterDomain() + " " +
                getLbCategoryContext().getCluster().getClusterSubDomain());

        try {
            CloudControllerServiceClient.getServiceClient().unregisterService(getLbCategoryContext().getCluster().getClusterDomain());

        } catch (Exception e) {
            String errorMsg = "Error in unregistering service cluster with domain " + getLbCategoryContext().getCluster().getClusterDomain() +
                    ", sub domain " + getLbCategoryContext().getCluster().getClusterSubDomain();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Unregistered service cluster, domain " + getLbCategoryContext().getCluster().getClusterDomain() + ", sub domain " +
                getLbCategoryContext().getCluster().getClusterSubDomain());
    }

    public LBCategoryContext getLbCategoryContext() {
        return lbCategoryContext;
    }

    public void setLbCategoryContext(LBCategoryContext lbCategoryContext) {
        this.lbCategoryContext = lbCategoryContext;
    }
}
