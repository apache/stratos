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
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.subscriber.Subscriber;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.Properties;

public class DataCartridgeSubscription extends CartridgeSubscription {

    private String host;
    private String username;
    private String password;

    /**
     * Constructor
     *
     * @param cartridgeInfo CartridgeInfo subscription
     */
    public DataCartridgeSubscription(CartridgeInfo cartridgeInfo) {

        super(cartridgeInfo);
        this.setHost("localhost");
        this.setUsername(CartridgeConstants.MYSQL_DEFAULT_USER);
        this.setPassword(ApplicationManagementUtil.generatePassword());
    }

    @Override
    public void createSubscription(Subscriber subscriber, String alias, String autoscalingPolicyName, String deploymentPolicyName,
                                   Repository repository)

            throws InvalidCartridgeAliasException,
            DuplicateCartridgeAliasException, ADCException, RepositoryCredentialsRequiredException,
            RepositoryTransportException, UnregisteredCartridgeException, AlreadySubscribedException,
            RepositoryRequiredException, InvalidRepositoryException, PolicyException {

        super.createSubscription(subscriber, alias, autoscalingPolicyName, deploymentPolicyName, repository);
        subscriptionTenancyBehaviour.createSubscription();
    }

    public PayloadArg createPayloadParameters() throws ADCException {

        PayloadArg payloadArg = super.createPayloadParameters();
        payloadArg.setDataCartridgeHost(this.getHost());
        payloadArg.setDataCartridgeAdminUser(getUsername());
        payloadArg.setDataCartridgeAdminPassword(getPassword());

        return subscriptionTenancyBehaviour.createPayloadParameters(payloadArg);
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties properties) throws ADCException,
            UnregisteredCartridgeException {

        Properties props = new Properties();
        props.setProperties(getCartridgeInfo().getProperties());
        //subscriptionTenancyBehaviour.registerSubscription(ApplicationManagementUtil.
        //        setRegisterServiceProperties(getAutoscalingPolicyName(), getSubscriber().getTenantId(), getAlias()));
        subscriptionTenancyBehaviour.registerSubscription(props);

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
    public void removeSubscription() throws ADCException, NotSubscribedException {

        subscriptionTenancyBehaviour.removeSubscription();
        super.cleanupSubscription();
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
