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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dto.Policy;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.repository.Repository;
import org.apache.stratos.adc.mgt.subscriber.Subscriber;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.cloud.controller.util.xsd.CartridgeInfo;

import java.util.Properties;

public class SingleTenantCartridgeSubscription extends CartridgeSubscription {

    private static Log log = LogFactory.getLog(SingleTenantCartridgeSubscription.class);


    public SingleTenantCartridgeSubscription(CartridgeInfo cartridgeInfo) {
        super(cartridgeInfo);
    }

    @Override
    public void createSubscription(Subscriber subscriber, String alias, Policy autoscalingPolicy,
                                   Repository repository) throws
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, ADCException,
            RepositoryCredentialsRequiredException, RepositoryTransportException, UnregisteredCartridgeException,
            AlreadySubscribedException, RepositoryRequiredException, InvalidRepositoryException, PolicyException {

        super.createSubscription(subscriber, alias, autoscalingPolicy, repository);
        setClusterDomain(alias + "." + getHostName() + "." + getType() + ".domain");
        setHostName(alias + "." + getHostName());
    }

    @Override
    public CartridgeSubscriptionInfo registerSubscription(Properties payloadProperties)
            throws ADCException, UnregisteredCartridgeException {

        ApplicationManagementUtil.registerService(getType(),
                getClusterDomain(),
                getClusterSubDomain(),
                getPayload().createPayload(),
                getPayload().getPayloadArg().getTenantRange(),
                getHostName(),
                null);

        getPayload().delete();

        return ApplicationManagementUtil.createCartridgeSubscription(getCartridgeInfo(), getAutoscalingPolicy(),
                getType(), getAlias(), getSubscriber().getTenantId(), getSubscriber().getTenantDomain(),
                getRepository(), getHostName(), getClusterDomain(), getClusterSubDomain(),
                getMgtClusterDomain(), getMgtClusterSubDomain(), null, "PENDING");
    }

    @Override
    public void removeSubscription() throws ADCException, NotSubscribedException {

        try {
            CloudControllerServiceClient.getServiceClient().terminateAllInstances(getClusterDomain(),
                    getClusterSubDomain());

        } catch (AxisFault e) {
            String errorMsg = "Error in terminating cartridge subscription, alias " + getAlias();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = "Error in terminating cartridge subscription, alias " + getAlias();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Terminated all instances of " + getClusterDomain() + " " + getClusterSubDomain());

        try {
            CloudControllerServiceClient.getServiceClient().unregisterService(getClusterDomain(),
                    getClusterSubDomain());

        } catch (Exception e) {
            String errorMsg = "Error in unregistering service cluster with domain " + getClusterDomain() +
                    ", sub domain " + getClusterSubDomain();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Unregistered service cluster, domain " + getClusterDomain() + ", sub domain " +
                getClusterSubDomain());

        cleanupSubscription();
    }

    @Override
    public PayloadArg createPayloadParameters()
            throws ADCException {

        PayloadArg payloadArg = super.createPayloadParameters();
        if(getRepository() != null) {
            payloadArg.setRepoURL(getRepository().getUrl());
        }
        payloadArg.setHostName(getHostName());
        payloadArg.setServiceDomain(getClusterDomain());
        payloadArg.setServiceSubDomain(getMgtClusterSubDomain());
        payloadArg.setMgtServiceDomain(getMgtClusterDomain());
        payloadArg.setMgtServiceSubDomain(getMgtClusterSubDomain());
        if(getCartridgeInfo().getProvider().equals(CartridgeConstants.PROVIDER_NAME_WSO2)) {
            payloadArg.setTenantRange(Integer.toString(getSubscriber().getTenantId()));
        }
        else {
            payloadArg.setTenantRange("*");
        }

        return payloadArg;
    }
}
