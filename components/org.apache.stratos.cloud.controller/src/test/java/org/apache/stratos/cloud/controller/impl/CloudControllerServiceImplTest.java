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
import org.apache.stratos.cloud.controller.iaases.AWSEC2Iaas;
import org.apache.stratos.cloud.controller.pojo.CartridgeConfig;
import org.apache.stratos.cloud.controller.pojo.IaasConfig;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;
import org.jclouds.ec2.compute.internal.EC2TemplateBuilderImpl;
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
	private static FasterLookUpDataHolder data;
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
		
		IaasConfig iaasConfig = mock(IaasConfig.class);
		when(iaasConfig.getType()).thenReturn("ec2");
		when(iaasConfig.getClassName()).thenReturn(AWSEC2Iaas.class.getName());
		when(iaasConfig.getProvider()).thenReturn("aws-ec2");
		when(iaasConfig.getIdentity()).thenReturn("EC2_IDENTITY");
		when(iaasConfig.getCredential()).thenReturn("EC2_CREDENTIAL");
//		when(iaasConfig.getComputeService()).thenReturn(computeService);
		when(iaasConfig.getImageId()).thenReturn("us-east-1/ami-623e940a");
		cartridgeConfig.setIaasConfigs(new IaasConfig[]{iaasConfig});
		
		IaasProvider iaasProvider = mock(IaasProvider.class);
		ComputeService computeService = mock(ComputeService.class); 
		EC2TemplateBuilderImpl templateBuilder = mock(EC2TemplateBuilderImpl.class);
		Template template = mock(Template.class);
		CloudControllerUtil util = mock(CloudControllerUtil.class); 
		
		when(template.getOptions()).thenReturn(new AWSEC2TemplateOptions());
		when(templateBuilder.build()).thenReturn(template);
		when(computeService.templateBuilder()).thenReturn(templateBuilder);
		
		AWSEC2Iaas iaas = mock(AWSEC2Iaas.class);
		doNothing().when(iaas).buildTemplate();
		when(iaasProvider.getIaas()).thenReturn(iaas);
		
//		List<IaasProvider> iaases = new ArrayList<IaasProvider>();
//		iaases.add(iaasProvider);
//		data.setIaasProviders(iaases);
//		when(data.getIaasProviders()).thenReturn(iaases);
//		Iaas iaas = new AWSEC2Iaas(iaasProvider);
//		doThrow(new RuntimeException()).when(iaasProvider).setIaas(iaas);
		
		service.deployCartridgeDefinition(cartridgeConfig);
		
	}
}
