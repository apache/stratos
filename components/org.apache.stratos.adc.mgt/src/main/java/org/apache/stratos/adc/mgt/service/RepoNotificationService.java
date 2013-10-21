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

package org.apache.stratos.adc.mgt.service;

import java.io.File;
import java.util.List;
import java.util.UUID;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.ClusteringFault;
import org.apache.axis2.clustering.management.GroupManagementAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.wso2.carbon.core.deployment.SynchronizeGitRepositoryRequest;
import org.wso2.carbon.utils.CarbonUtils;


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
			log.info(" wso2 cartridge.. ");
			createAndSendClusterMessage(subscription.getTenantId(), subscription.getTenantDomain(),
			                            UUID.randomUUID(), subscription.getClusterDomain(),
			                            subscription.getClusterSubdomain());
			//for manager node
			           /* if (subscription.getMgtClusterSubDomain() != null && !subscription.getMgtClusterSubDomain().isEmpty()) {
			                createAndSendClusterMessage(subscription.getTenantId(), subscription.getTenantDomain(),
			                        UUID.randomUUID(), subscription.getMgtClusterDomain(),
			                        subscription.getMgtClusterSubDomain());
			            }
			            else {
			                if(log.isDebugEnabled())
			                    log.debug("Manager node cluster information not found, not sending the SynchronizeGitRepositoryRequest");
			            }*/

		} else {

			// Query DB and get all the IP s for this tenant 
			// Invoke update-subscription script
			
			String appPath = subscription.getBaseDirectory();
			String cartridgePrivateKey = System.getProperty(CartridgeConstants.CARTRIDGE_KEY);
			
			File keyFile = new File(cartridgePrivateKey);
			if (!keyFile.exists()) {
				log.error("The key file does not exist! " + cartridgePrivateKey);
			}

			if (subscription != null) {
				TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();

				
				if (topologyMgtService == null) {
					String msg = " Topology Management Service is null ";
					log.error(msg);
					throw new Exception(msg);
				}

				String[] activeIpArray =
				                         topologyMgtService.getActiveIPs(subscription.getCartridge(),
				                                                         subscription.getClusterDomain(),
				                                                         subscription.getClusterSubdomain());
				try {

					for (String instanceIp : activeIpArray) {
						String command =
						                 CarbonUtils.getCarbonHome() + File.separator + "bin" +
						                         File.separator + "update-subscription.sh " +
						                         instanceIp + " " + appPath + " " +
						                         cartridgePrivateKey + " /";
						log.info("Update subscription command.... " + command);
						Process proc = Runtime.getRuntime().exec(command);
						proc.waitFor();
					}

				} catch (Exception e) {
					log.error("Exception is occurred in notify update operation. Reason : " +
					          e.getMessage());
					throw e;
				}
			}
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
