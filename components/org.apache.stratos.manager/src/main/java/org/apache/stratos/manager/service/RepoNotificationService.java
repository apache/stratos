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

package org.apache.stratos.manager.service;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.manager.internal.DataHolder;
import org.apache.stratos.manager.publisher.InstanceNotificationPublisher;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.manager.utils.PersistenceManager;
import org.wso2.carbon.core.deployment.SynchronizeGitRepositoryRequest;

import java.util.List;
import java.util.UUID;


public class RepoNotificationService {

    private static final Log log = LogFactory.getLog(RepoNotificationService.class);


    public void notifyRepoUpdate(String tenantDomain, String cartridgeAlias) throws Exception {
        // FIXME Throwing generic Exception is wrong
        log.info("Updating repository of tenant : " + tenantDomain + " , cartridge: " +
                cartridgeAlias);

        CartridgeSubscriptionInfo subscription = null;
        try {
            subscription = PersistenceManager.getSubscription(tenantDomain, cartridgeAlias);
        } catch (Exception e) {
            String msg = "Failed to find subscription for " + cartridgeAlias + ". "
                    + (e.getMessage() != null ? e.getMessage() : "");
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        if (subscription == null) {
            String msg = "Tenant " + tenantDomain + " is not subscribed for " + cartridgeAlias;
            log.error(msg);
            throw new Exception("You have not subscribed for " + cartridgeAlias);
        }

        try {
            handleRepoSynch(subscription);
        } catch (Exception e) {
            String msg = "Failed to synchronize the repository for " + cartridgeAlias + ". "
                    + (e.getMessage() != null ? e.getMessage() : "");
            log.error(msg, e);
            throw new Exception(msg, e);
        }

    }

    public void synchronize(String repositoryURL) throws Exception {

        log.info(" repository URL received : " + repositoryURL);
        List<CartridgeSubscriptionInfo> subscription = PersistenceManager.getSubscription(repositoryURL);
        for (CartridgeSubscriptionInfo cartridgeSubscriptionInfo : subscription) {
            handleRepoSynch(cartridgeSubscriptionInfo);
        }
    }

    private void handleRepoSynch(CartridgeSubscriptionInfo subscription) throws Exception {
        if (subscription == null) {
            throw new Exception("Cannot synchronize repository. subscription is null");
        }

        if (CartridgeConstants.PROVIDER_NAME_WSO2.equals(subscription.getProvider())) {
            createAndSendClusterMessage(subscription.getTenantId(), subscription.getTenantDomain(),
                    UUID.randomUUID(), subscription.getClusterDomain(),
                    subscription.getClusterSubdomain());

        } else {
            InstanceNotificationPublisher notificationHandler = new InstanceNotificationPublisher();
            notificationHandler.sendArtifactUpdateEvent(subscription.getRepository(),
                    subscription.getClusterDomain(),
                    String.valueOf(subscription.getTenantId()));
        }
    }

    private void createAndSendClusterMessage(int tenantId, String tenantDomain, UUID uuid,
                                             String clusterDomain, String clusterSubdomain) {

        SynchronizeGitRepositoryRequest request =
                new SynchronizeGitRepositoryRequest(tenantId,
                        tenantDomain,
                        uuid);

        ClusteringAgent clusteringAgent =
                DataHolder.getServerConfigContext()
                        .getAxisConfiguration().getClusteringAgent();
        GroupManagementAgent groupMgtAgent =
                clusteringAgent.getGroupManagementAgent(clusterDomain,
                        clusterSubdomain);

        try {
            log.info("Sending Request to.. " + clusterDomain + " : " + clusterSubdomain);
            groupMgtAgent.send(request);

        } catch (ClusteringFault e) {
            e.printStackTrace();
        }


    }

}
