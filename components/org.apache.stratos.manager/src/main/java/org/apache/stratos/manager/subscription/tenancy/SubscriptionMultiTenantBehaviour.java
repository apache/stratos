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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.common.Properties;
import org.apache.stratos.manager.dao.Cluster;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.payload.PayloadData;
import org.apache.stratos.manager.publisher.InstanceNotificationPublisher;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.subscriber.Subscriber;
import org.apache.stratos.manager.utils.CartridgeConstants;

import java.util.Map;


public class SubscriptionMultiTenantBehaviour extends SubscriptionTenancyBehaviour {

    private static Log log = LogFactory.getLog(SubscriptionMultiTenantBehaviour.class);


    public PayloadData create (String alias, Cluster cluster, Subscriber subscriber, Repository repository, CartridgeInfo cartridgeInfo,
                        String subscriptionKey, Map<String, String> customPayloadEntries) throws ADCException, AlreadySubscribedException {

        boolean allowMultipleSubscription = Boolean.
                valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

        if (!allowMultipleSubscription) {
            // If the cartridge is multi-tenant. We should not let users createSubscription twice.
            boolean subscribed;

            try {
                subscribed = hasAlreadySubscribed(subscriber.getTenantId(), cartridgeInfo.getType());

            } catch (Exception e) {
                String msg = "Error checking whether the cartridge type " + cartridgeInfo.getType() + " is already subscribed";
                log.error(msg, e);
                throw new ADCException(msg, e);
            }

            if (subscribed) {
                String msg = "Already subscribed to " + cartridgeInfo.getType() + ". This multi-tenant cartridge will not be available to createSubscription";
                if (log.isDebugEnabled()) {
                    log.debug(msg);
                }
                throw new AlreadySubscribedException(msg, cartridgeInfo.getType());
            }
        }

        // get the cluster domain and host name from deployed Service
        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();

        Service deployedService;
        try {
            deployedService = dataInsertionAndRetrievalManager.getService(cartridgeInfo.getType());

        } catch (PersistenceManagerException e) {
            String errorMsg = "Error in checking if Service is available is PersistenceManager";
            log.error(errorMsg, e);
            throw new ADCException(errorMsg, e);
        }

        if (deployedService == null) {
            String errorMsg = "There is no deployed Service for type " + cartridgeInfo.getType();
            log.error(errorMsg);
            throw new ADCException(errorMsg);
        }

        //set the cluster and hostname
        cluster.setClusterDomain(deployedService.getClusterId());
        cluster.setHostName(deployedService.getHostName());

        if (repository != null) {

            // publish the ArtifactUpdated event
            log.info(" Multitenant --> Publishing Artifact update event -- ");
            log.info(" Values :  cluster id - " + cluster.getClusterDomain() + "  tenant - " + subscriber.getTenantId());
            InstanceNotificationPublisher publisher = new InstanceNotificationPublisher();
            publisher.sendArtifactUpdateEvent(repository, cluster.getClusterDomain(), String.valueOf(subscriber.getTenantId()));

        } else {
            if(log.isDebugEnabled()) {
                log.debug("No repository found for subscription with alias: " + alias + ", type: " + cartridgeInfo.getType() +
                        ". Not sending the Artifact Updated event");
            }
        }

        // no payload
        return null;
    }

    public void register(CartridgeInfo cartridgeInfo, Cluster cluster, PayloadData payloadData, String autoscalePolicyName,
                         String deploymentPolicyName, Properties properties, Persistence persistence)
            throws ADCException, UnregisteredCartridgeException {

        //nothing to do
    }

    public void remove (String clusterId, String alias) throws ADCException, NotSubscribedException {

        log.info("Cartridge Subscription with alias " + alias + ", and cluster id " + clusterId +
                " is a multi-tenant cartridge and therefore will not terminate all instances and " +
                "unregister services");
    }

    private static boolean hasAlreadySubscribed(int tenantId, String cartridgeType) {

        DataInsertionAndRetrievalManager dataInsertionAndRetrievalManager = new DataInsertionAndRetrievalManager();
        return ( dataInsertionAndRetrievalManager.getCartridgeSubscriptions(tenantId, cartridgeType) == null ||
                 dataInsertionAndRetrievalManager.getCartridgeSubscriptions(tenantId, cartridgeType).isEmpty() ) ? false : true;
    }
}
