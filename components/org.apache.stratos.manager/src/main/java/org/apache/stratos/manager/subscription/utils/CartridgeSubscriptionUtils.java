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

package org.apache.stratos.manager.subscription.utils;

import org.apache.axis2.AxisFault;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.pojo.*;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.DuplicateCartridgeAliasException;
import org.apache.stratos.manager.exception.InvalidCartridgeAliasException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.lb.category.LBDataContext;
import org.apache.stratos.manager.payload.BasicPayloadData;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.util.Util;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

public class CartridgeSubscriptionUtils {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionUtils.class);

    public static BasicPayloadData createBasicPayload(CartridgeInfo cartridgeInfo, String subscriptionKey, Cluster cluster,
                                                      Repository repository, String alias, Subscriber subscriber) {

        BasicPayloadData basicPayloadData = new BasicPayloadData();
        basicPayloadData.setApplicationPath(cartridgeInfo.getBaseDir());
        basicPayloadData.setSubscriptionKey(subscriptionKey);
        //basicPayloadData.setDeployment("default");//currently hard coded to default
        basicPayloadData.setMultitenant(String.valueOf(cartridgeInfo.getMultiTenant()));
        basicPayloadData.setPortMappings(createPortMappingPayloadString(cartridgeInfo));
        basicPayloadData.setServiceName(cartridgeInfo.getType());
        basicPayloadData.setProvider(cartridgeInfo.getProvider());

        if (repository != null) {
            basicPayloadData.setGitRepositoryUrl(repository.getUrl());
        }

        if (cluster != null) {
            basicPayloadData.setClusterId(cluster.getClusterDomain());
            basicPayloadData.setHostName(cluster.getHostName());
        }

        if (alias != null) {
            basicPayloadData.setSubscriptionAlias(alias);
        }

        if (subscriber != null) {
            basicPayloadData.setTenantId(subscriber.getTenantId());
        }

        //TODO:remove. we do not want to know about the tenant rance in subscription!
        if (cartridgeInfo.getMultiTenant()) {  //TODO: fix properly
            basicPayloadData.setTenantRange("*");
        } else if (subscriber != null) {
            basicPayloadData.setTenantRange(String.valueOf(subscriber.getTenantId()));
        }

        return basicPayloadData;
    }

    public static BasicPayloadData createBasicPayload(Service service) {

        BasicPayloadData basicPayloadData = new BasicPayloadData();
        basicPayloadData.setApplicationPath(service.getCartridgeInfo().getBaseDir());
        basicPayloadData.setSubscriptionKey(service.getSubscriptionKey());
        basicPayloadData.setClusterId(service.getClusterId());
        //basicPayloadData.setDeployment("default");//currently hard coded to default
        basicPayloadData.setHostName(service.getHostName());
        basicPayloadData.setMultitenant(String.valueOf(service.getCartridgeInfo().getMultiTenant()));
        basicPayloadData.setPortMappings(createPortMappingPayloadString(service.getCartridgeInfo()));
        basicPayloadData.setServiceName(service.getType());
        basicPayloadData.setTenantId(service.getTenantId());
        basicPayloadData.setTenantRange(service.getTenantRange());

        return basicPayloadData;
    }

//    public static BasicPayloadData createBasicPayload (LBCategoryContext lbCategoryContext) {
//
//        BasicPayloadData basicPayloadData = new BasicPayloadData();
//        basicPayloadData.setApplicationPath(lbCategoryContext.getCartridgeInfo().getBaseDir());
//        basicPayloadData.setSubscriptionKey(lbCategoryContext.getKey());
//        basicPayloadData.setClusterId(lbCategoryContext.getCluster().getClusterDomain());
//        basicPayloadData.setDeployment("default");//currently hard coded to default
//        basicPayloadData.setHostName(lbCategoryContext.getCluster().getHostName());
//        basicPayloadData.setMultitenant(String.valueOf(lbCategoryContext.getCartridgeInfo().getMultiTenant()));
//        basicPayloadData.setPortMappings(createPortMappingPayloadString(lbCategoryContext.getCartridgeInfo()));
//        basicPayloadData.setServiceName(lbCategoryContext.getLbType());
//
//        if (lbCategoryContext.getSubscriptionAlias() != null && !lbCategoryContext.getSubscriptionAlias().isEmpty()) {
//            basicPayloadData.setSubscriptionAlias(lbCategoryContext.getSubscriptionAlias());
//        }
//
//        if (lbCategoryContext.getSubscriber() != null) {
//            basicPayloadData.setTenantId(lbCategoryContext.getSubscriber().getTenantId());
//        }
//
//        return basicPayloadData;
//    }

    private static String createPortMappingPayloadString(CartridgeInfo cartridgeInfo) {

        // port mappings
        StringBuilder portMapBuilder = new StringBuilder();
        PortMapping[] portMappings = cartridgeInfo.getPortMappings();
        for (PortMapping portMapping : portMappings) {
            String port = portMapping.getPort();
            portMapBuilder.append(port).append("|");
        }

        // remove last "|" character
        String portMappingString = portMapBuilder.toString().replaceAll("\\|$", "");

        return portMappingString;
    }

    public static String generateSubscriptionKey() {
        return RandomStringUtils.randomAlphanumeric(16);
    }

    static class TenantSubscribedEventPublisher implements Runnable {

        private int tenantId;
        private String serviceName;
        private Set<String> clusterIds;

        public TenantSubscribedEventPublisher(int tenantId, String service, Set<String> clusterIds) {
            this.tenantId = tenantId;
            this.serviceName = service;
            this.clusterIds = clusterIds;
        }

        @Override
        public void run() {
            try {
                if (log.isInfoEnabled()) {
                    log.info(String.format("Publishing tenant subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName));
                }
                TenantSubscribedEvent subscribedEvent = new TenantSubscribedEvent(tenantId, serviceName, clusterIds);
                String topic = Util.getMessageTopicName(subscribedEvent);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
                eventPublisher.publish(subscribedEvent);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error(String.format("Could not publish tenant subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName), e);
                }
            }

        }

    }

    public static void publishTenantSubscribedEvent(int tenantId, String serviceName, Set<String> clusterIds) {


        Executor exec = new Executor() {
            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };

        exec.execute(new TenantSubscribedEventPublisher(tenantId, serviceName, clusterIds));
    }

    public static void publishTenantUnSubscribedEvent(int tenantId, String serviceName, Set<String> clusterIds) {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Publishing tenant un-subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName));
            }
            TenantUnSubscribedEvent event = new TenantUnSubscribedEvent(tenantId, serviceName, clusterIds);
	        String topic = Util.getMessageTopicName(event);
            EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
            eventPublisher.publish(event);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish tenant un-subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName), e);
            }
        }
    }

    public static void validateCartridgeAlias(int tenantId, String cartridgeType, String alias) throws InvalidCartridgeAliasException, DuplicateCartridgeAliasException, ADCException {

        String patternString = "([a-z0-9]+([-][a-z0-9])*)+";
        Pattern pattern = Pattern.compile(patternString);

        if (!pattern.matcher(alias).matches()) {
            String msg = "The alias " + alias + " can contain only alpha-numeric lowercase characters. Please enter a valid alias.";
            log.error(msg);
            throw new InvalidCartridgeAliasException(msg, tenantId, cartridgeType, alias);
        }

        boolean isAliasTaken = false;
        try {
            isAliasTaken = isAliasTaken(tenantId, alias);
        } catch (Exception e) {
            String msg = "Exception : " + e.getMessage();
            log.error(msg, e);
            throw new ADCException("Error when checking alias is already taken", e);
        }

        if (isAliasTaken) {
            String msg = "The alias " + alias + " is already taken. Please try again with a different alias.";
            log.error(msg);
            throw new DuplicateCartridgeAliasException(msg, cartridgeType, alias);
        }
    }

    public static boolean isAliasTaken(int tenantId, String alias) {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
        // return (dataInsertionAndRetrievalManager.getCartridgeSubscription(tenantId, alias) == null) ? false : true;
        // fixing STRATOS-427, making the alias globally unique
        return (dataInsertionAndRetrievalManager.getCartridgeSubscriptionForAlias(alias) == null) ? false : true;
    }

    public static String limitLengthOfString(String source, int length) {

        return source.substring(0, length);
    }

    public static LBDataContext getLoadBalancerDataContext(int tenantId, String serviceType, String deploymentPolicyName, LoadbalancerConfig lbConfig) throws UnregisteredCartridgeException, ADCException {

        String lbCartridgeType = lbConfig.getType();

        LBDataContext lbDataCtxt = new LBDataContext();
        // set tenant Id
        lbDataCtxt.setTenantId(tenantId);

        Properties lbReferenceProperties = lbConfig.getProperties();

        Property lbRefProperty = new Property();
        lbRefProperty.setName(StratosConstants.LOAD_BALANCER_REF);

        for (Property prop : lbReferenceProperties.getProperties()) {

            String name = prop.getName();
            String value = prop.getValue();

            // TODO make following a chain of responsibility pattern
            if (StratosConstants.NO_LOAD_BALANCER.equals(name)) {

                if ("true".equals(value)) {
                    lbDataCtxt.setLbCategory(StratosConstants.NO_LOAD_BALANCER);

                    if (log.isDebugEnabled()) {
                        log.debug("This cartridge does not require a load balancer. " + "[Type] " + serviceType);
                    }
                    lbRefProperty.setValue(name);
                    lbDataCtxt.addLoadBalancedServiceProperty(lbRefProperty);
                    break;
                }
            } else if (StratosConstants.EXISTING_LOAD_BALANCERS.equals(name)) {

                lbDataCtxt.setLbCategory(StratosConstants.EXISTING_LOAD_BALANCERS);

                String clusterIdsVal = value;
                if (log.isDebugEnabled()) {
                    log.debug("This cartridge refers to existing load balancers. " + "[Type] " + serviceType + "[Referenced Cluster Ids] " + clusterIdsVal);
                }

                String[] clusterIds = clusterIdsVal.split(",");

                for (String clusterId : clusterIds) {
                    try {
                        AutoscalerServiceClient.getServiceClient().checkLBExistenceAgainstPolicy(clusterId, deploymentPolicyName);
                    } catch (Exception ex) {
                        // we don't need to throw the error here.
                        log.error(ex.getMessage(), ex);
                    }
                }

                lbRefProperty.setValue(name);
                lbDataCtxt.addLoadBalancedServiceProperty(lbRefProperty);
                break;

            } else if (StratosConstants.DEFAULT_LOAD_BALANCER.equals(name)) {

                if ("true".equals(value)) {

                    lbDataCtxt.setLbCategory(StratosConstants.DEFAULT_LOAD_BALANCER);

                    lbRefProperty.setValue(name);

                    CartridgeInfo lbCartridgeInfo;

                    try {
                        lbCartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(lbCartridgeType);

                    } catch (Exception e) {
                        String message = "Error getting info for " + lbCartridgeType;
                        log.error(message, e);
                        throw new ADCException(message, e);
                    }

                    if (lbCartridgeInfo == null) {
                        String msg = "Please specify a LB cartridge type for the cartridge: " + serviceType + " as category: " +
                                     StratosConstants.DEFAULT_LOAD_BALANCER;
                        log.error(msg);
                        throw new ADCException(msg);
                    }

                    lbDataCtxt.setLbCartridgeInfo(lbCartridgeInfo);

                    if (log.isDebugEnabled()) {
                        log.debug("This cartridge uses default load balancer. " + "[Type] " + serviceType);
                    }

                    try {
                        // get the valid policies for lb cartridge
                        DeploymentPolicy[] lbCartridgeDepPolicies =
                                getAutoscalerServiceClient().getDeploymentPolicies(lbCartridgeType);
                        // traverse deployment policies of lb cartridge
                        for (DeploymentPolicy policy : lbCartridgeDepPolicies) {

                            // check existence of the subscribed policy
                            if (deploymentPolicyName.equals(policy.getId())) {

                                if (!getAutoscalerServiceClient().checkDefaultLBExistenceAgainstPolicy(deploymentPolicyName)) {
                                    if (log.isDebugEnabled()) {
                                        log.debug(" Default LB doesn't exist for deployment policy [" + deploymentPolicyName + "] ");
                                    }

                                    Properties lbProperties = new Properties();

                                    // if LB cartridge definition has properties as well, combine
                                    if (lbCartridgeInfo.getProperties() != null && lbCartridgeInfo.getProperties().length > 0) {
                                        if (log.isDebugEnabled()) {
                                            log.debug(" Combining LB properties ");
                                        }
                                        lbProperties.setProperties(combine(lbCartridgeInfo.getProperties(), new Property[]{lbRefProperty}));
                                    } else {
                                        lbProperties.setProperties(new Property[]{lbRefProperty});
                                    }

                                    lbDataCtxt.addLBProperties(lbProperties);
                                }
                            }
                        }

                    } catch (Exception ex) {
                        // we don't need to throw the error here.
                        log.error(ex.getMessage(), ex);
                    }

                    // set deployment and autoscaling policies
                    lbDataCtxt.setDeploymentPolicy(deploymentPolicyName);
                    lbDataCtxt.setAutoscalePolicy(lbCartridgeInfo.getDefaultAutoscalingPolicy());

                    lbDataCtxt.addLoadBalancedServiceProperty(lbRefProperty);
                    break;
                }

            } else if (StratosConstants.SERVICE_AWARE_LOAD_BALANCER.equals(name)) {

                if ("true".equals(value)) {

                    lbDataCtxt.setLbCategory(StratosConstants.SERVICE_AWARE_LOAD_BALANCER);

                    lbRefProperty.setValue(name);

                    CartridgeInfo lbCartridgeInfo;

                    try {
                        lbCartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(lbCartridgeType);

                    } catch (Exception e) {
                        String message = "Error getting info for " + lbCartridgeType;
                        log.error(message, e);
                        throw new ADCException(message, e);
                    }

                    if (lbCartridgeInfo == null) {
                        String msg = "Please specify a LB cartridge type for the cartridge: " + serviceType + " as category: " +
                                     StratosConstants.SERVICE_AWARE_LOAD_BALANCER;
                        log.error(msg);
                        throw new ADCException(msg);
                    }

                    lbDataCtxt.setLbCartridgeInfo(lbCartridgeInfo);

                    // add a property for the service type
                    Property loadBalancedServiceTypeProperty = new Property();
                    loadBalancedServiceTypeProperty.setName(StratosConstants.LOAD_BALANCED_SERVICE_TYPE);
                    // set the load balanced service type
                    loadBalancedServiceTypeProperty.setValue(serviceType);

                    if (log.isDebugEnabled()) {
                        log.debug("This cartridge uses a service aware load balancer. [Type] " + serviceType);
                    }

                    try {

                        // get the valid policies for lb cartridge
                        DeploymentPolicy[] lbCartridgeDepPolicies = getAutoscalerServiceClient().getDeploymentPolicies(lbCartridgeType);
                        // traverse deployment policies of lb cartridge
                        for (DeploymentPolicy policy : lbCartridgeDepPolicies) {
                            // check existence of the subscribed policy
                            if (deploymentPolicyName.equals(policy.getId())) {

                                if (!getAutoscalerServiceClient().checkServiceLBExistenceAgainstPolicy(serviceType, deploymentPolicyName)) {

                                    Properties lbProperties = new Properties();

                                    // if LB cartridge definition has properties as well, combine
                                    if (lbCartridgeInfo.getProperties() != null && lbCartridgeInfo.getProperties().length > 0) {
                                        lbProperties.setProperties(combine(lbCartridgeInfo.getProperties(), new Property[]{lbRefProperty, loadBalancedServiceTypeProperty}));

                                    } else {
                                        lbProperties.setProperties(new Property[]{lbRefProperty, loadBalancedServiceTypeProperty});
                                    }

                                    // set a payload property for load balanced service type
                                    Property payloadProperty = new Property();
                                    payloadProperty.setName("LOAD_BALANCED_SERVICE_TYPE");  //TODO: refactor hardcoded name
                                    payloadProperty.setValue(serviceType);

                                    lbDataCtxt.addLBProperties(lbProperties);
                                }
                            }
                        }

                    } catch (Exception ex) {
                        // we don't need to throw the error here.
                        log.error(ex.getMessage(), ex);
                    }

                    // set deployment and autoscaling policies
                    lbDataCtxt.setDeploymentPolicy(deploymentPolicyName);
                    lbDataCtxt.setAutoscalePolicy(lbCartridgeInfo.getDefaultAutoscalingPolicy());

                    lbDataCtxt.addLoadBalancedServiceProperty(lbRefProperty);
                    break;
                }
            }
        }

        return lbDataCtxt;
    }

    private static AutoscalerServiceClient getAutoscalerServiceClient() throws ADCException {

        try {
            return AutoscalerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error in getting AutoscalerServiceClient instance";
            log.error(errorMsg, axisFault);
            throw new ADCException(errorMsg, axisFault);
        }
    }

    private static Property[] combine(Property[] propertyArray1, Property[] propertyArray2) {

        int length = propertyArray1.length + propertyArray2.length;
        Property[] combinedProperties = new Property[length];
        System.arraycopy(propertyArray1, 0, combinedProperties, 0, propertyArray1.length);
        System.arraycopy(propertyArray2, 0, combinedProperties, propertyArray1.length, propertyArray2.length);

        return combinedProperties;
    }
}
