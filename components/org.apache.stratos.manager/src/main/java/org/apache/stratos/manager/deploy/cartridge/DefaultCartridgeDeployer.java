/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.manager.deploy.cartridge;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeConfig;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.exception.ADCException;

public class DefaultCartridgeDeployer extends CartridgeDeployer{

	private static Log log = LogFactory.getLog(DefaultCartridgeDeployer.class);
	
	@Override
	protected void preDeployment() {
				
	}

	@Override
	protected void postDeployment() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void deployCartridge(CartridgeConfig cartridgeConfig) throws ADCException {
		try {
			CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getServiceClient();
			cloudControllerServiceClient.deployCartridgeDefinition(cartridgeConfig);
		} catch (Exception e) {
			String msg = "Exception in deploying the cartridge. ";
			log.error(msg + e.getMessage());
			throw new ADCException(msg);
		}	
	}

}
