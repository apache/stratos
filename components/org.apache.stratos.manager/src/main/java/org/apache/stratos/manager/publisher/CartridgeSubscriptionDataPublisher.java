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
package org.apache.stratos.manager.publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.exception.ADCException;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.databridge.agent.thrift.AsyncDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.Event;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * This will publish the subscription details of tenants to a data receiver.
 * Data is published when a tenant subscribe to a cartridge and when it unsubscribe.
 */
public class CartridgeSubscriptionDataPublisher {

	private static final Log log = LogFactory
			.getLog(CartridgeSubscriptionDataPublisher.class);
	private static AsyncDataPublisher dataPublisher;
	private static StreamDefinition streamDefinition;
	private static final String stratosManagerEventStreamVersion = "1.0.0";

	@SuppressWarnings("deprecation")
	public static void publish(int tenantID, String adminUser,
			String cartridgeAlias, String cartridgeType, String repositoryUrl,
			boolean isMultiTenant, String autoScalingPolicy,
			String deploymentPolicy, String clusterID, String hostName,
			String mappedDomain, String action) throws ADCException {
		
		//check if bam is enabled in cartridge-config.properties
		if(! Boolean.parseBoolean(System.getProperty(CartridgeConstants.BAM_PUBLISHER_ENABLED))){
            return;
        }
		
		log.debug(CartridgeConstants.DATA_PUB_TASK_NAME+" cycle started.");

		if (dataPublisher == null) {
			createDataPublisher();

			// If we cannot create a data publisher we should give up
			// this means data will not be published
			if (dataPublisher == null) {
				log.error("Data Publisher cannot be created or found.");
				return;
			}
		}

		//Construct the data to be published
		List<Object> payload = new ArrayList<Object>();
		
		// Payload values
		payload.add(String.valueOf(tenantID));
		payload.add(handleNull(adminUser));
		payload.add(handleNull(cartridgeAlias));
		payload.add(cartridgeType);
		payload.add(handleNull(repositoryUrl));
		payload.add(handleNull(String.valueOf(isMultiTenant)));
		payload.add(handleNull(autoScalingPolicy));
		payload.add(handleNull(deploymentPolicy));
		payload.add(String.valueOf(clusterID));
		payload.add(handleNull(hostName));
		payload.add(handleNull(mappedDomain));
		payload.add(handleNull(action));

		Event event = new Event();
		event.setPayloadData(payload.toArray());
		event.setArbitraryDataMap(new HashMap<String, String>());

		try {
			if (log.isDebugEnabled()) {
				log.debug(String.format(
						"Publishing BAM event: [stream] %s [version] %s",
						streamDefinition.getName(),
						streamDefinition.getVersion()));
			}
			dataPublisher.publish(streamDefinition.getName(),
					streamDefinition.getVersion(), event);
		} catch (AgentException e) {
			if (log.isErrorEnabled()) {
				log.error(
						String.format(
								"Could not publish BAM event: [stream] %s [version] %s",
								streamDefinition.getName(),
								streamDefinition.getVersion()), e);
			}
		}
	}

	private static StreamDefinition initializeStream() throws Exception {
		streamDefinition = new StreamDefinition( CartridgeConstants.STRATOS_MANAGER_EVENT_STREAM,
				stratosManagerEventStreamVersion);
		streamDefinition.setNickName("stratos.manager");
		streamDefinition.setDescription("Tenant Subscription Data");
		// Payload definition
		List<Attribute> payloadData = new ArrayList<Attribute>();
		payloadData.add(new Attribute(CartridgeConstants.TENANT_ID_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.ADMIN_USER_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.CARTRIDGE_ALIAS_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.CARTRIDGE_TYPE_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.REPOSITORY_URL_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.MULTI_TENANT_BEHAVIOR_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.AUTO_SCALE_POLICY_COL, AttributeType.STRING));
		payloadData
				.add(new Attribute(CartridgeConstants.DEPLOYMENT_POLICY_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.CLUSTER_ID_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.HOST_NAME_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.MAPPED_DOMAIN_COL, AttributeType.STRING));
		payloadData.add(new Attribute(CartridgeConstants.ACTION_COL, AttributeType.STRING));
		streamDefinition.setPayloadData(payloadData);
		return streamDefinition;
	}

	private static void createDataPublisher() throws ADCException {
		// creating the agent
		ServerConfiguration serverConfig = CarbonUtils.getServerConfiguration();
		String trustStorePath = serverConfig.getFirstProperty("Security.TrustStore.Location");
		String trustStorePassword = serverConfig.getFirstProperty("Security.TrustStore.Password");
		
		//value is in the carbon.xml file and should be set to the thrift port of BAM
		String bamServerUrl = serverConfig.getFirstProperty("BamServerURL");
        
        //getting the BAM related values from cartridge-config.properties
		String adminUsername = System.getProperty(CartridgeConstants.BAM_ADMIN_USERNAME);
		String adminPassword = System.getProperty(CartridgeConstants.BAM_ADMIN_PASSWORD); 

		System.setProperty("javax.net.ssl.trustStore", trustStorePath);
		System.setProperty("javax.net.ssl.trustStorePassword",
				trustStorePassword);

		try {
			dataPublisher = new AsyncDataPublisher(
					"tcp://" + bamServerUrl + "", adminUsername, adminPassword);
			initializeStream();
			dataPublisher.addStreamDefinition(streamDefinition);
		} catch (Exception e) {
			String msg = "Unable to create a data publisher to "+ bamServerUrl;
			log.error(msg, e);
			throw new ADCException(msg, e);
		}
	}
	
	private static String handleNull(String val) {
	    if (val == null) {
	        return "";
	    }
	    return val;
	}

}
