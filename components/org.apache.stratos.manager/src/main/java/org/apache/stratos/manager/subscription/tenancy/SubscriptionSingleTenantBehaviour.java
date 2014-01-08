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

package org.apache.stratos.manager.subscription.tenancy;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.exception.AlreadySubscribedException;
import org.apache.stratos.manager.exception.NotSubscribedException;
import org.apache.stratos.manager.exception.UnregisteredCartridgeException;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.cloud.controller.pojo.Properties;


public class SubscriptionSingleTenantBehaviour extends SubscriptionTenancyBehaviour {

    private static Log log = LogFactory.getLog(SubscriptionSingleTenantBehaviour.class);



    public void createSubscription(CartridgeSubscription cartridgeSubscription) throws ADCException, AlreadySubscribedException {

        //set the cluster and hostname
        cartridgeSubscription.getCluster().setClusterDomain(cartridgeSubscription.getAlias() + "." +
                cartridgeSubscription.getCluster().getHostName() + "." + cartridgeSubscription.getType() + ".domain");
        cartridgeSubscription.getCluster().setHostName(cartridgeSubscription.getAlias() + "." +
                cartridgeSubscription.getCluster().getHostName());
    }

    public void registerSubscription(CartridgeSubscription cartridgeSubscription, Properties properties) throws ADCException, UnregisteredCartridgeException {

        log.info("Payload: " + cartridgeSubscription.getPayloadData().getCompletePayloadData().toString());

        ApplicationManagementUtil.registerService(cartridgeSubscription.getType(),
                cartridgeSubscription.getCluster().getClusterDomain(),
                cartridgeSubscription.getCluster().getClusterSubDomain(),
                cartridgeSubscription.getPayloadData().getCompletePayloadData(),
                cartridgeSubscription.getPayloadData().getBasicPayloadData().getTenantRange(),
                cartridgeSubscription.getCluster().getHostName(),
                cartridgeSubscription.getAutoscalingPolicyName(),
                cartridgeSubscription.getDeploymentPolicyName(),
                properties);
    }

    public void removeSubscription(CartridgeSubscription cartridgeSubscription) throws ADCException, NotSubscribedException {

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
}
