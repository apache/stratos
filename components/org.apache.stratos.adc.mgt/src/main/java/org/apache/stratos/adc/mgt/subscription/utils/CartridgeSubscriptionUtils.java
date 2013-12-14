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

package org.apache.stratos.adc.mgt.subscription.utils;

import org.apache.stratos.adc.mgt.payload.BasicPayloadData;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;

public class CartridgeSubscriptionUtils {

    public static BasicPayloadData getBasicPayloadData (CartridgeSubscription cartridgeSubscription) {

        BasicPayloadData basicPayloadData = new BasicPayloadData();
        basicPayloadData.setApplicationPath(cartridgeSubscription.getCartridgeInfo().getBaseDir());
        basicPayloadData.setSubscriptionKey(cartridgeSubscription.getSubscriptionKey());
        basicPayloadData.setClusterId(cartridgeSubscription.getClusterDomain());
        basicPayloadData.setDeployment("default");//currently hard coded to default
        if(cartridgeSubscription.getRepository() != null) {
            basicPayloadData.setGitRepositoryUrl(cartridgeSubscription.getRepository().getUrl());
        }
        basicPayloadData.setHostName(cartridgeSubscription.getHostName());
        basicPayloadData.setMultitenant(String.valueOf(cartridgeSubscription.getCartridgeInfo().getMultiTenant()));
        basicPayloadData.setPortMappings(createPortMappingPayloadString(cartridgeSubscription.getCartridgeInfo()));
        basicPayloadData.setServiceName(cartridgeSubscription.getCartridgeInfo().getType());
        basicPayloadData.setSubscriptionAlias(cartridgeSubscription.getAlias());
        basicPayloadData.setTenantId(cartridgeSubscription.getSubscriber().getTenantId());
        if(cartridgeSubscription.getCartridgeInfo().getMultiTenant() ||
                cartridgeSubscription.getSubscriber().getTenantId() == -1234) {  //TODO: fix properly
            basicPayloadData.setTenantRange("*");
        } else {
            basicPayloadData.setTenantRange(String.valueOf(cartridgeSubscription.getSubscriber().getTenantId()));
        }

        return basicPayloadData;
    }

    private static String createPortMappingPayloadString (CartridgeInfo cartridgeInfo) {

        // port mappings
        StringBuilder portMapBuilder = new StringBuilder();
        org.apache.stratos.cloud.controller.pojo.PortMapping[] portMappings = cartridgeInfo.getPortMappings();
        for (org.apache.stratos.cloud.controller.pojo.PortMapping portMapping : portMappings) {
            String port = portMapping.getPort();
            portMapBuilder.append(port).append("|");
        }

        // remove last "|" character
        String portMappingString = portMapBuilder.toString().replaceAll("\\|$", "");

        return portMappingString;
    }
}
