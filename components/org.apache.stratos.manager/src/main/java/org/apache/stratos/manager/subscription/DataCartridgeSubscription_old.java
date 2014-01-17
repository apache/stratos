///*
// * Licensed to the Apache Software Foundation (ASF) under one
// * or more contributor license agreements.  See the NOTICE file
// * distributed with this work for additional information
// * regarding copyright ownership.  The ASF licenses this file
// * to you under the Apache License, Version 2.0 (the
// * "License"); you may not use this file except in compliance
// * with the License.  You may obtain a copy of the License at
// *
// *  http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing,
// * software distributed under the License is distributed on an
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// * KIND, either express or implied.  See the License for the
// * specific language governing permissions and limitations
// * under the License.
// */
//
//package org.apache.stratos.manager.subscription;
//
//import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
//import org.apache.stratos.manager.dao.DataCartridge;
//import org.apache.stratos.manager.dto.Policy;
//import org.apache.stratos.manager.exception.*;
//import org.apache.stratos.manager.payload.PayloadArg;
//import org.apache.stratos.manager.repository.Repository;
//import org.apache.stratos.manager.subscriber.Subscriber;
//import org.apache.stratos.manager.utils.ApplicationManagementUtil;
//import org.apache.stratos.manager.utils.CartridgeConstants;
//import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
//
//import java.util.Properties;
//
//public class DataCartridgeSubscription extends SingleTenantCartridgeSubscription {
//
//    private String host;
//    private String username;
//    private String password;
//
//    public DataCartridgeSubscription(CartridgeInfo cartridgeInfo) {
//
//        super(cartridgeInfo);
//        this.setDBHost("localhost");
//        this.setUsername(CartridgeConstants.MYSQL_DEFAULT_USER);
//        this.setPassword(ApplicationManagementUtil.generatePassword());
//    }
//
//    @Override
//    public void createSubscription(Subscriber subscriber, String alias, Policy autoscalingPolicy, Repository repository)
//
//            throws InvalidCartridgeAliasException,
//            DuplicateCartridgeAliasException, ADCException, RepositoryCredentialsRequiredException,
//            RepositoryTransportException, UnregisteredCartridgeException, AlreadySubscribedException,
//            RepositoryRequiredException, InvalidRepositoryException, PolicyException {
//
//        super.createSubscription(subscriber, alias, autoscalingPolicy, repository);
//    }
//
//    public Repository manageRepository (String repoURL, String repoUserName, String repoUserPassword,
//                                        boolean privateRepo, String cartridgeAlias, CartridgeInfo cartridgeInfo,
//                                        String tenantDomain) {
//
//        //no repository for data cartridge instances
//        return null;
//    }
//
//    public PayloadArg createPayloadParameters() throws ADCException {
//
//        PayloadArg payloadArg = super.createPayloadParameters();
//        payloadArg.setDataCartridgeHost(this.getDBHost());
//        payloadArg.setDataCartridgeAdminUser(getUsername());
//        payloadArg.setDataCartridgeAdminPassword(getPassword());
//
//        return payloadArg;
//    }
//
//    public CartridgeSubscriptionInfo registerSubscription(Properties payloadProperties)
//            throws ADCException, UnregisteredCartridgeException {
//
//        ApplicationManagementUtil.registerService(getType(),
//                getCluster().getClusterDomain(),
//                getCluster().getClusterSubDomain(),
//                getPayloadData().createPayload(),
//                getPayloadData().getPayloadArg().getTenantRange(),
//                getCluster().getHostName(),
//                ApplicationManagementUtil.setRegisterServiceProperties(getAutoscalingPolicyName(),
//                        getSubscriber().getTenantId(), getAlias()));
//
//        getPayloadData().delete();
//
//        DataCartridge dataCartridge = new DataCartridge();
//        dataCartridge.setUserName(getUsername());
//        dataCartridge.setPassword(getPassword());
//        dataCartridge.setDataCartridgeType(getType());
//
//        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicyName(),
//                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
//                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
//                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), dataCartridge, "PENDING",getSubscriptionKey());
//
//    }
//
//    public String getDBHost() {
//        return host;
//    }
//
//    public void setDBHost(String host) {
//        this.host = host;
//    }
//
//    public String getUsername() {
//        return username;
//    }
//
//    public void setUsername(String username) {
//        this.username = username;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//
//    public void setPassword(String password) {
//        this.password = password;
//    }
//}
