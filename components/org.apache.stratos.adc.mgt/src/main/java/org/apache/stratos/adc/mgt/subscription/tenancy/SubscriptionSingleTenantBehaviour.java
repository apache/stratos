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

package org.apache.stratos.adc.mgt.subscription.tenancy;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
import org.apache.stratos.adc.mgt.exception.ADCException;
import org.apache.stratos.adc.mgt.exception.AlreadySubscribedException;
import org.apache.stratos.adc.mgt.exception.NotSubscribedException;
import org.apache.stratos.adc.mgt.exception.UnregisteredCartridgeException;
import org.apache.stratos.adc.mgt.payload.PayloadArg;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;

import java.util.Properties;

public class SubscriptionSingleTenantBehaviour extends SubscriptionTenancyBehaviour {

    private static Log log = LogFactory.getLog(SubscriptionSingleTenantBehaviour.class);

    public SubscriptionSingleTenantBehaviour(CartridgeSubscription cartridgeSubscription) {
        super(cartridgeSubscription);
    }

    public void createSubscription() throws ADCException, AlreadySubscribedException {

        //set the cluster and hostname
        cartridgeSubscription.getCluster().setClusterDomain(cartridgeSubscription.getAlias() + "." +
                cartridgeSubscription.getCluster().getHostName() + "." + cartridgeSubscription.getType() + ".domain");
        cartridgeSubscription.getCluster().setHostName(cartridgeSubscription.getAlias() + "." +
                cartridgeSubscription.getCluster().getHostName());
    }

    public void registerSubscription(Properties properties) throws ADCException, UnregisteredCartridgeException {

        ApplicationManagementUtil.registerService(cartridgeSubscription.getType(),
                cartridgeSubscription.getCluster().getClusterDomain(),
                cartridgeSubscription.getCluster().getClusterSubDomain(),
                cartridgeSubscription.getPayload().createPayload(),
                cartridgeSubscription.getPayload().getPayloadArg().getTenantRange(),
                cartridgeSubscription.getCluster().getHostName(),
                properties);

        cartridgeSubscription.getPayload().delete();
    }

    public void removeSubscription() throws ADCException, NotSubscribedException {

        try {
            CloudControllerServiceClient.getServiceClient().terminateAllInstances(cartridgeSubscription.getCluster().getClusterDomain());

        } catch (AxisFault e) {
            String errorMsg = "Error in terminating cartridge subscription, alias " + cartridgeSubscription.getAlias();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);

        } catch (Exception e) {
            String errorMsg = "Error in terminating cartridge subscription, alias " + cartridgeSubscription.getAlias();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Terminated all instances of " + cartridgeSubscription.getCluster().getClusterDomain() + " " +
                cartridgeSubscription.getCluster().getClusterSubDomain());

        try {
            CloudControllerServiceClient.getServiceClient().unregisterService(cartridgeSubscription.getCluster().getClusterDomain());

        } catch (Exception e) {
            String errorMsg = "Error in unregistering service cluster with domain " + cartridgeSubscription.getCluster().getClusterDomain() +
                    ", sub domain " + cartridgeSubscription.getCluster().getClusterSubDomain();
            log.error(errorMsg);
            throw new ADCException(errorMsg, e);
        }

        log.info("Unregistered service cluster, domain " + cartridgeSubscription.getCluster().getClusterDomain() + ", sub domain " +
                cartridgeSubscription.getCluster().getClusterSubDomain());
    }

    public PayloadArg createPayloadParameters(PayloadArg payloadArg) throws ADCException {


        if(cartridgeSubscription.getRepository() != null) {
            payloadArg.setRepoURL(cartridgeSubscription.getRepository().getUrl());
        }
        payloadArg.setHostName(cartridgeSubscription.getCluster().getHostName());
        payloadArg.setServiceDomain(cartridgeSubscription.getCluster().getClusterDomain());
        payloadArg.setServiceSubDomain(cartridgeSubscription.getCluster().getMgtClusterSubDomain());
        payloadArg.setMgtServiceDomain(cartridgeSubscription.getCluster().getMgtClusterDomain());
        payloadArg.setMgtServiceSubDomain(cartridgeSubscription.getCluster().getMgtClusterSubDomain());
        if(cartridgeSubscription.getCartridgeInfo().getProvider().equals(CartridgeConstants.PROVIDER_NAME_WSO2)) {
            payloadArg.setTenantRange(Integer.toString(cartridgeSubscription.getSubscriber().getTenantId()));
        }
        else {
            payloadArg.setTenantRange("*");
        }

        return payloadArg;
    }
}
