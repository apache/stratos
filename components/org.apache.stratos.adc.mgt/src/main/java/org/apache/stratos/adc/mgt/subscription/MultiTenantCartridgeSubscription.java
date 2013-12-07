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
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
//import org.apache.stratos.adc.mgt.dto.Policy;
//import org.apache.stratos.adc.mgt.exception.*;
//import org.apache.stratos.adc.mgt.internal.DataHolder;
//import org.apache.stratos.adc.mgt.payload.PayloadArg;
//import org.apache.stratos.adc.mgt.repository.Repository;
//import org.apache.stratos.adc.mgt.subscriber.Subscriber;
//import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
//import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
//import org.apache.stratos.adc.mgt.utils.PersistenceManager;
//import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
//import org.apache.stratos.adc.topology.mgt.serviceobjects.DomainContext;
//import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
//
//import java.util.Properties;
//
//public class MultiTenantCartridgeSubscription extends CartridgeSubscription {
//
//    private static Log log = LogFactory.getLog(MultiTenantCartridgeSubscription.class);
//
//    public MultiTenantCartridgeSubscription(CartridgeInfo cartridgeInfo) {
//        super(cartridgeInfo);
//    }
//
//    @Override
//    public void createSubscription(Subscriber subscriber, String alias, Policy autoscalingPolicy, Repository repository)
//
//            throws InvalidCartridgeAliasException,
//            DuplicateCartridgeAliasException, ADCException, AlreadySubscribedException,
//            RepositoryCredentialsRequiredException, RepositoryTransportException, UnregisteredCartridgeException,
//            InvalidRepositoryException, RepositoryRequiredException, PolicyException {
//
//        super.createSubscription(subscriber, alias, autoscalingPolicy, repository);
//
//        boolean allowMultipleSubscription = Boolean.
//                valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));
//
//        if (!allowMultipleSubscription) {
//            // If the cartridge is multi-tenant. We should not let users createSubscription twice.
//            boolean subscribed;
//            try {
//                subscribed = PersistenceManager.isAlreadySubscribed(getType(), subscriber.getTenantId());
//            } catch (Exception e) {
//                String msg = "Error checking whether the cartridge type " + getType()
//                        + " is already subscribed";
//                log.error(msg, e);
//                throw new ADCException(msg, e);
//            }
//
//            if (subscribed) {
//                String msg = "Already subscribed to " + getType()
//                        + ". This multi-tenant cartridge will not be available to createSubscription";
//                if (log.isDebugEnabled()) {
//                    log.debug(msg);
//                }
//                throw new AlreadySubscribedException(msg, getType());
//            }
//        }
//
//        TopologyManagementService topologyService = DataHolder.getTopologyMgtService();
//        DomainContext[] domainContexts = topologyService.getDomainsAndSubdomains(getType(), subscriber.getTenantId());
//        log.info("Retrieved " + domainContexts.length + " domain and corresponding subdomain pairs");
//
//        if (domainContexts.length > 0) {
//            if(domainContexts.length > 2) {
//                if(log.isDebugEnabled())
//                    log.debug("Too many domain sub domain pairs");
//            }
//
//            for (DomainContext domainContext : domainContexts) {
//                if (domainContext.getSubDomain().equalsIgnoreCase("mgt")) {
//                    getCluster().setMgtClusterDomain(domainContext.getDomain());
//                    getCluster().setMgtClusterSubDomain(domainContext.getSubDomain());
//                } else {
//                    getCluster().setClusterDomain(domainContext.getDomain());
//                    getCluster().setClusterSubDomain(domainContext.getSubDomain());
//                }
//            }
//        } else {
//            String msg = "Domain contexts not found for " + getType() + " and tenant id " + subscriber.getTenantId();
//            log.warn(msg);
//            throw new ADCException(msg);
//        }
//    }
//
//    @Override
//    public void removeSubscription() throws ADCException, NotSubscribedException {
//
//        log.info("Cartridge with alias " + getAlias() + ", and type " + getType() +
//                " is a multi-tenant cartridge and therefore will not terminate all instances and " +
//                "unregister services");
//
//        super.cleanupSubscription();
//    }
//
//    @Override
//    public CartridgeSubscriptionInfo registerSubscription(Properties properties) {
//
//        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicy(),
//                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
//                getRepository(), getCluster().getHostName(), getCluster().getClusterDomain(), getCluster().getClusterSubDomain(),
//                getCluster().getMgtClusterDomain(), getCluster().getMgtClusterSubDomain(), null, "PENDING",getSubscriptionKey());
//    }
//
//    @Override
//    public PayloadArg createPayloadParameters() {
//
//        return null;
//    }
//
//}
