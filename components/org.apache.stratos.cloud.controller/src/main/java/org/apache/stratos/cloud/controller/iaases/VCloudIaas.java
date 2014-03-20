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
package org.apache.stratos.cloud.controller.iaases;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.validate.VCloudPartitionValidator;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.vcloud.compute.options.VCloudTemplateOptions;
import org.jclouds.vcloud.domain.network.IpAddressAllocationMode;
import org.wso2.carbon.utils.CarbonUtils;

public class VCloudIaas extends Iaas {


	private static final Log log = LogFactory.getLog(VCloudIaas.class);
	
	private static final String SHELL_TYPE = "shellType";
	private static final String SCRIPTS_PATH = "scripts";
	private static final String CUSTOMIZATION_SCRIPT = "customization";
	private static final String PAYLOAD = "PAYLOAD";
	
	public VCloudIaas(IaasProvider iaasProvider) {
		super(iaasProvider);
	}

	@Override
	public void buildComputeServiceAndTemplate() {

		IaasProvider iaasInfo = getIaasProvider();
		
		// builds and sets Compute Service
		ComputeServiceBuilderUtil.buildDefaultComputeService(iaasInfo);

		// builds and sets Template
		buildTemplate();

	}

	public void buildTemplate() {
		IaasProvider iaasInfo = getIaasProvider();
		
		if (iaasInfo.getComputeService() == null) {
			String msg = "Compute service is null for IaaS provider: "
					+ iaasInfo.getName();
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		TemplateBuilder templateBuilder = iaasInfo.getComputeService()
				.templateBuilder();

		// set image id specified
		templateBuilder.imageId(iaasInfo.getImage());

		// build the Template
		Template template = templateBuilder.build();

		// if you wish to auto assign IPs, instance spawning call should be
		// blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode? - default mode is
		// non-blocking
		boolean blockUntilRunning = Boolean.parseBoolean(iaasInfo
				.getProperty("autoAssignIp"));
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by Jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(22, 80, 8080, 443, 8243);

		template.getOptions().as(VCloudTemplateOptions.class)
				.ipAddressAllocationMode(IpAddressAllocationMode.POOL);

		// set Template
		iaasInfo.setTemplate(template);
	}

	@Override
	public void setDynamicPayload() {
		// in vCloud case we need to run a script
		IaasProvider iaasInfo = getIaasProvider();

		if (iaasInfo.getTemplate() == null || iaasInfo.getPayload() == null) {
			if (log.isDebugEnabled()) {
				log.debug("Payload for vCloud not found");
			}
			return;
		}

		String shellType = iaasInfo.getProperty(SHELL_TYPE);

		if (shellType == null || shellType.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("Shell Type for vCloud Customization script not found from properties");
			}
			return;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("Shell Type '%s' will be used for vCloud Customization script", shellType));
		}

		// Payload is a String value
		String payload = new String(iaasInfo.getPayload());

		if (log.isDebugEnabled()) {
			log.debug(String.format("Payload '%s' will be used for vCloud Customization script", shellType));
		}

		Template template = iaasInfo.getTemplate();

		File scriptPath = new File(CarbonUtils.getCarbonConfigDirPath(), SCRIPTS_PATH);

		File customizationScriptFile = new File(new File(scriptPath, shellType), CUSTOMIZATION_SCRIPT);

		if (!customizationScriptFile.exists()) {
			if (log.isWarnEnabled()) {
				log.warn(String.format("The vCloud Customization script '%s' does not exist",
						customizationScriptFile.getAbsolutePath()));
			}
			return;
		}

		String customizationScript = null;

		try {
			customizationScript = FileUtils.readFileToString(customizationScriptFile);
		} catch (IOException e) {
			if (log.isErrorEnabled()) {
				log.error(
						String.format("Error reading the vCloud Customization script '%s'",
								customizationScriptFile.getAbsolutePath()), e);
			}
		}

		if (customizationScript == null || customizationScript.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No content vCloud Customization script not found from properties");
			}
			return;
		}

		// Set payload
		customizationScript = customizationScript.replaceAll(PAYLOAD, payload);

		if (log.isDebugEnabled()) {
			log.debug(String.format("The vCloud Customization script\n%s", customizationScript));
		}

		// Run the script
		template.getOptions().runScript(customizationScript);
	}

	@Override
	public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {

		// TODO
		return false;
	}

	@Override
	public String associateAddress(NodeMetadata node) {

		// TODO
		return "";

	}

	@Override
	public void releaseAddress(String ip) {
		// TODO
	}

    @Override
    public boolean isValidRegion(String region) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isValidZone(String region, String zone) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public boolean isValidHost(String zone, String host) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new VCloudPartitionValidator();
    }

	@Override
	public String createVolume(int sizeGB) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String attachVolume(String instanceId, String volumeId, String deviceName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void detachVolume(String instanceId, String volumeId) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteVolume(String volumeId) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public String getIaasDevice(String device) {
        return device;
    }

}
