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
//package org.apache.stratos.adc.mgt.subscription;
//
//import org.apache.axis2.AxisFault;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
//import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
//import org.apache.stratos.adc.mgt.dto.Policy;
//import org.apache.stratos.adc.mgt.exception.*;
//import org.apache.stratos.adc.mgt.payload.PayloadArg;
//import org.apache.stratos.adc.mgt.repository.Repository;
//import org.apache.stratos.adc.mgt.subscriber.Subscriber;
//import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
//import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
//import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
//
//import java.util.Properties;
//
//public class SingleTenantCartridgeSubscription extends CartridgeSubscription {
//
//    private static Log log = LogFactory.getLog(SingleTenantCartridgeSubscription.class);
//
//
//    public SingleTenantCartridgeSubscription(CartridgeInfo cartridgeInfo) {
//        super(cartridgeInfo);
//    }
//
//    @Override
//    public void createSubscription(Subscriber subscriber, String alias, Policy autoscalingPolicy,
//                                   Repository repository) throws
//            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, ADCException,
//            RepositoryCredentialsRequiredException, RepositoryTransportException, UnregisteredCartridgeException,
//            AlreadySubscribedException, RepositoryRequiredException, InvalidRepositoryException, PolicyException {
//
//        super.createSubscription(subscriber, alias, autoscalingPolicy, repository);
//        getCluster().setClusterDomain(alias + "." + getCluster().getHostName() + "." + getType() + ".domain");
//        getCluster().setHostName(alias + "." + getCluster().getHostName());
//    }
//
//    @Override
//    public CartridgeSubscriptionInfo registerSubscription(Properties payloadProperties)
//            throws ADCException, UnregisteredCartridgeException {
//
//        ApplicationManagementUtil.registerService(getType(),
//                getCluster().getClusterDomain(),
//                getCluster().getClusterSubDomain(),
//                getPayload().createPayload(),
//                getPayload().getPayloadArg().getTenantRange(),
//                getCluster().getHostName(),
//                null);
//
//        getPayload().delete();
//
//        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicyName(),
//                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
//                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
//                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), null, "PENDING",getSubscriptionKey());
//    }
//
//    @Override
//    public void removeSubscription() throws ADCException, NotSubscribedException {
//
//        try {
//            CloudControllerServiceClient.getServiceClient().terminateAllInstances(getCluster().getClusterDomain());
//
//        } catch (AxisFault e) {
//            String errorMsg = "Error in terminating cartridge subscription, alias " + getAlias();
//            log.error(errorMsg);
//            throw new ADCException(errorMsg, e);
//
//        } catch (Exception e) {
//            String errorMsg = "Error in terminating cartridge subscription, alias " + getAlias();
//            log.error(errorMsg);
//            throw new ADCException(errorMsg, e);
//        }
//
//        log.info("Terminated all instances of " + getCluster().getClusterDomain() + " " + getCluster().getClusterSubDomain());
//
//        try {
//            CloudControllerServiceClient.getServiceClient().unregisterService(getCluster().getClusterDomain());
//
//        } catch (Exception e) {
//            String errorMsg = "Error in unregistering service cluster with domain " + getCluster().getClusterDomain() +
//                    ", sub domain " + getCluster().getClusterSubDomain();
//            log.error(errorMsg);
//            throw new ADCException(errorMsg, e);
//        }
//
//        log.info("Unregistered service cluster, domain " + getCluster().getClusterDomain() + ", sub domain " +
//                getCluster().getClusterSubDomain());
//
//        super.cleanupSubscription();
//    }
//
//    @Override
//    public PayloadArg createPayloadParameters()
//            throws ADCException {
//
//        PayloadArg payloadArg = super.createPayloadParameters();
//        if(getRepository() != null) {
//            payloadArg.setRepoURL(getRepository().getUrl());
//        }
//        payloadArg.setHostName(getCluster().getHostName());
//        payloadArg.setServiceDomain(getCluster().getClusterDomain());
//        payloadArg.setServiceSubDomain(getCluster().getMgtClusterSubDomain());
//        payloadArg.setMgtServiceDomain(getCluster().getMgtClusterDomain());
//        payloadArg.setMgtServiceSubDomain(getCluster().getMgtClusterSubDomain());
//        if(getCartridgeInfo().getProvider().equals(CartridgeConstants.PROVIDER_NAME_WSO2)) {
//            payloadArg.setTenantRange(Integer.toString(getSubscriber().getTenantId()));
//        }
//        else {
//            payloadArg.setTenantRange("*");
//        }
//
//        return payloadArg;
//    }
//}
