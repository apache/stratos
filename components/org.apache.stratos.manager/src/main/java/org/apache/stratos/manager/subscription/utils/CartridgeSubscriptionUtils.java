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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.DuplicateCartridgeAliasException;
import org.apache.stratos.manager.exception.InvalidCartridgeAliasException;
import org.apache.stratos.manager.payload.BasicPayloadData;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.util.Constants;

import java.util.regex.Pattern;

public class CartridgeSubscriptionUtils {

    private static Log log = LogFactory.getLog(CartridgeSubscriptionUtils.class);

    public static BasicPayloadData createBasicPayload (CartridgeSubscription cartridgeSubscription) {

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
        //TODO:remove. we do not want to know about the tenant rance in subscription!
        if(cartridgeSubscription.getCartridgeInfo().getMultiTenant() ||
                cartridgeSubscription.getSubscriber().getTenantId() == -1234) {  //TODO: fix properly
            basicPayloadData.setTenantRange("*");
        } else {
            basicPayloadData.setTenantRange(String.valueOf(cartridgeSubscription.getSubscriber().getTenantId()));
        }

        return basicPayloadData;
    }

    public static BasicPayloadData createBasicPayload (Service service) {

        BasicPayloadData basicPayloadData = new BasicPayloadData();
        basicPayloadData.setApplicationPath(service.getCartridgeInfo().getBaseDir());
        basicPayloadData.setSubscriptionKey(service.getSubscriptionKey());
        basicPayloadData.setClusterId(service.getClusterId());
        basicPayloadData.setDeployment("default");//currently hard coded to default
        basicPayloadData.setHostName(service.getHostName());
        basicPayloadData.setMultitenant(String.valueOf(service.getCartridgeInfo().getMultiTenant()));
        basicPayloadData.setPortMappings(createPortMappingPayloadString(service.getCartridgeInfo()));
        basicPayloadData.setServiceName(service.getType());
        basicPayloadData.setTenantId(service.getTenantId());
        basicPayloadData.setTenantRange(service.getTenantRange());

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

    public static String generateSubscriptionKey() {
        String key = RandomStringUtils.randomAlphanumeric(16);
        log.info("Generated key  : " + key); // TODO -- remove the log
        return key;
    }

    public static void publishTenantSubscribedEvent(int tenantId, String serviceName) {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Publishing tenant subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName));
            }
            TenantSubscribedEvent subscribedEvent = new TenantSubscribedEvent(tenantId, serviceName);
            EventPublisher eventPublisher = new EventPublisher(Constants.TENANT_TOPIC);
            eventPublisher.publish(subscribedEvent);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish tenant subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName), e);
            }
        }
    }

    public static void publishTenantUnSubscribedEvent(int tenantId, String serviceName) {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Publishing tenant un-subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName));
            }
            TenantUnSubscribedEvent event = new TenantUnSubscribedEvent(tenantId, serviceName);
            EventPublisher eventPublisher = new EventPublisher(Constants.TENANT_TOPIC);
            eventPublisher.publish(event);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(String.format("Could not publish tenant un-subscribed event: [tenant-id] %d [service] %s", tenantId, serviceName), e);
            }
        }
    }

    public static void validateCartridgeAlias (int tenantId, String cartridgeType, String alias) throws InvalidCartridgeAliasException, DuplicateCartridgeAliasException, ADCException {

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

    public static boolean isAliasTaken (int tenantId, String alias) {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
        // return (dataInsertionAndRetrievalManager.getCartridgeSubscription(tenantId, alias) == null) ? false : true;
        // fixing STRATOS-427, making the alias globally unique
        return (dataInsertionAndRetrievalManager.getCartridgeSubscriptionForAlias(alias) == null) ? false : true;
    }
}
