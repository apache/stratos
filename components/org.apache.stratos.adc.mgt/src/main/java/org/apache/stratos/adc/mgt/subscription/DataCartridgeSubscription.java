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

package org.apache.stratos.adc.mgt.subscription;

import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dao.DataCartridge;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;

import java.util.HashMap;
import java.util.Map;

public class DataCartridgeSubscription extends CartridgeSubscription {

    private String host;
    private String username;
    private String password;

    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo instance
     * @param subscriptionTenancyBehaviour SubscriptionTenancyBehaviour instance
     */
    public DataCartridgeSubscription(CartridgeInfo cartridgeInfo, SubscriptionTenancyBehaviour
            subscriptionTenancyBehaviour) {

        super(cartridgeInfo, subscriptionTenancyBehaviour);
        setHost("localhost");
        setUsername("root");
        setPassword("root");
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties) throws ADCException,
            UnregisteredCartridgeException {

        Properties props = new Properties();
        props.setProperties(getCartridgeInfo().getProperties());
        getSubscriptionTenancyBehaviour().registerSubscription(this, props);

        DataCartridge dataCartridge = new DataCartridge();
        dataCartridge.setUserName(getUsername());
        dataCartridge.setPassword(getPassword());
        dataCartridge.setDataCartridgeType(getType());

        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicyName(),
                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), dataCartridge, "PENDING",getSubscriptionKey());
    }

    public Repository manageRepository (String repoURL, String repoUserName, String repoUserPassword,
                                        boolean privateRepo, String cartridgeAlias, CartridgeInfo cartridgeInfo,
                                        String tenantDomain) {

        //no repository for data cartridge instances
        return null;
    }

    @Override
    public Map<String, String> getCustomPayloadEntries() {

        Map<String, String> payloadEntriesMap = new HashMap<String, String>();
        payloadEntriesMap.put("MYSQL_HOST", host);
        payloadEntriesMap.put("MYSQL_USER", username);
        payloadEntriesMap.put("MYSQL_PASSWORD", password);

        return payloadEntriesMap;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
