/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.event.subscriber;

import java.io.File;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.deployment.synchronizer.ArtifactRepository;
import org.apache.stratos.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.deployment.synchronizer.git.impl.GitBasedArtifactRepository;
import org.apache.stratos.messaging.event.artifact.synchronization.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.util.Util;


public class ArtifactListener implements MessageListener{
	
	 private static final Log log = LogFactory.getLog(ArtifactListener.class);
	
	@Override
	public void onMessage(Message message) {
		
		// If cluster id of the node is equal to that of message's, invoke the script
		TextMessage receivedMessage = (TextMessage) message;
		log.info(" Message received on artifact update ");
		String json = null;
		try {
			json = receivedMessage.getText();
		} catch (Exception e) {
			log.error("Exception is occurred " + e.getMessage(), e);
		}
		
		ArtifactUpdatedEvent event = (ArtifactUpdatedEvent) Util.jsonToObject(json, ArtifactUpdatedEvent.class);
		String clusterIdInPayload = readParamValueFromPayload(CartridgeAgentConstants.CLUSTER_ID);
		String localRepoPath = readParamValueFromPayload(CartridgeAgentConstants.APP_PATH);
		String clusterIdInMessage = event.getClusterId();		
		String repoURL = event.getRepoURL();
		String repoPassword = decryptPassword(event.getRepoPassword());
		String repoUsername = event.getRepoUserName();
		String tenantId = event.getTenantId();
		
		log.info("cluster id in payload " + clusterIdInPayload);
		log.info("cluster id in message " + clusterIdInMessage);
		
		if(clusterIdInPayload != null && clusterIdInPayload.equals(clusterIdInMessage)) {
			
	    	ArtifactRepository gitBasedArtifactRepository = new GitBasedArtifactRepository();
	    	RepositoryInformation repoInformation = new RepositoryInformation();
	    	repoInformation.setRepoUsername(repoUsername);
	    	repoInformation.setRepoPassword(repoPassword);
	    	repoInformation.setRepoUrl(repoURL);
	    	repoInformation.setRepoPath(localRepoPath);
	    	repoInformation.setTenantId(tenantId);    	
	    	gitBasedArtifactRepository.checkout(repoInformation);			
		}
		
	}

	
	private String readParamValueFromPayload(String param) {
		String paramValue = null;
		// read launch params
		File file = new File(System.getProperty(CartridgeAgentConstants.PARAM_FILE_PATH));

		try {
			Scanner scanner = new Scanner(file);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] params = line.split(",");
				for (String string : params) {
					 String[] var = string.split("=");
					if(param.equals(var[0])){
						paramValue = var[1];
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
			//e.printStackTrace();
			log.error("Exception is occurred in reading file. ", e);
		}
		
		return paramValue;
	}
	
	private String decryptPassword(String repoUserPassword) {
		
		String decryptPassword = "";
		String secret = readParamValueFromPayload(CartridgeAgentConstants.CARTRIDGE_KEY); 
		SecretKey key;
		Cipher cipher;
		Base64 coder;
		key = new SecretKeySpec(secret.getBytes(), "AES");
		try {
			cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
			coder = new Base64();
			byte[] encrypted = coder.decode(repoUserPassword.getBytes());
			cipher.init(Cipher.DECRYPT_MODE, key);
			byte[] decrypted = cipher.doFinal(encrypted);
			decryptPassword = new String(decrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return decryptPassword;
	}

}
