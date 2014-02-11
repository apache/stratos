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

package org.apache.stratos.manager.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.CarbonUtils;

/**
 * Holds the some of the data required by the webapps component
 */
public class DataHolder {
	private static ConfigurationContext clientConfigContext;
	private static ConfigurationContext serverConfigContext;

	private static RealmService realmService;
	private static RegistryService registryService;
	//private static TopologyManagementService topologyMgtService;
	private static EventPublisher eventPublisher;

	public static RealmService getRealmService() {
		return realmService;
	}

	public static void setRealmService(RealmService realmService) {
		DataHolder.realmService = realmService;
	}

	public static RegistryService getRegistryService() {
		return registryService;
	}

	public static ConfigurationContext getClientConfigContext() {
		CarbonUtils.checkSecurity();
		return clientConfigContext;
	}

	public static void setClientConfigContext(ConfigurationContext clientConfigContext) {
		DataHolder.clientConfigContext = clientConfigContext;
	}

	public static ConfigurationContext getServerConfigContext() {
		CarbonUtils.checkSecurity();
		return serverConfigContext;
	}

	public static void setServerConfigContext(ConfigurationContext serverConfigContext) {
		DataHolder.serverConfigContext = serverConfigContext;
	}

	public static void setRegistryService(RegistryService registryService) {
		DataHolder.registryService = registryService;
	}

	public static EventPublisher getEventPublisher() {
		return eventPublisher;
	}

	public static void setEventPublisher(EventPublisher eventPublisher) {
		DataHolder.eventPublisher = eventPublisher;
	}	
	
}
