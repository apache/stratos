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
package org.apache.stratos.cloud.controller.impl;

import org.apache.stratos.cloud.controller.exception.InvalidCartridgeDefinitionException;
import org.apache.stratos.cloud.controller.pojo.CartridgeConfig;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.wso2.carbon.registry.core.session.UserRegistry;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Testing operations provided by Cloud Controller.
 */
public class CloudControllerServiceImplTest {
	private static RegistryManager registry;
	private static FasterLookUpDataHolder data ;
	private static ServiceReferenceHolder serviceRefHolder =
			ServiceReferenceHolder.getInstance();
	private static CloudControllerServiceImpl service;

	@BeforeClass
	public static void setUp(){
		UserRegistry userReg = mock(UserRegistry.class);
		serviceRefHolder = mock(ServiceReferenceHolder.class);
		
		when(serviceRefHolder.getRegistry()).thenReturn(userReg);
		
		registry = mock(RegistryManager.class);
		when(registry.retrieve()).thenReturn(null);
		
		data = mock(FasterLookUpDataHolder.class);
		
		service = new CloudControllerServiceImpl(true);
		
	}
	
	@Test
	public void testDeployCartridgeDefinition() throws Exception {
		CartridgeConfig cartridgeConfig = new CartridgeConfig();
		cartridgeConfig.setType("php");
		cartridgeConfig.setBaseDir("/tmp");
		try {
			service.deployCartridgeDefinition(cartridgeConfig);
		} catch(Exception e) {
			assertEquals(InvalidCartridgeDefinitionException.class, e.getClass());
			assertEquals("Invalid Cartridge Definition: Cartridge Type: php. "
					+ "Cause: Iaases of this Cartridge is null or empty.", e.getMessage());
		}
	}
}
