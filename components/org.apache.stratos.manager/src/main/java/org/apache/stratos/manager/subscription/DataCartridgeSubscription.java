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

package org.apache.stratos.manager.subscription;

import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.dao.DataCartridge;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.subscription.tenancy.SubscriptionTenancyBehaviour;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;

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
        setDBHost("localhost");
        setDBUsername("root");
        setDBPassword(ApplicationManagementUtil.generatePassword());
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties, Persistence persistence) throws ADCException,
            UnregisteredCartridgeException {

        getSubscriptionTenancyBehaviour().register (getCartridgeInfo(), getCluster(), getPayloadData(), getAutoscalingPolicyName(),
                getDeploymentPolicyName(), properties, persistence);

        DataCartridge dataCartridge = new DataCartridge();
        dataCartridge.setUserName(getDBUsername());
        dataCartridge.setPassword(getDBPassword());
        dataCartridge.setDataCartridgeType(getType());

        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicyName(),
                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), dataCartridge, "PENDING",getSubscriptionKey());
    }

    public Repository manageRepository (String repoURL, String repoUserName, String repoUserPassword,
                                        boolean privateRepo) {

        //no repository for data cartridge instances
        return null;
    }

    @Override
    public Map<String, String> getCustomPayloadEntries() {

        Map<String, String> payloadEntriesMap = new HashMap<String, String>();
        payloadEntriesMap.put("DB_HOST", host);
        payloadEntriesMap.put("DB_USER", username);
        payloadEntriesMap.put("MYSQL_PASSWORD", password);

        return payloadEntriesMap;
    }

    public String getDBHost() {
        return host;
    }

    public void setDBHost(String host) {
        this.host = host;
    }

    public String getDBUsername() {
        return username;
    }

    public void setDBUsername(String username) {
        this.username = username;
    }

    public String getDBPassword() {
        return password;
    }

    public void setDBPassword(String password) {
        this.password = password;
    }
}
