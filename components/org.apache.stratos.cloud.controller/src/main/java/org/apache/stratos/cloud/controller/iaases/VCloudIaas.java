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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.pojo.PersistanceMapping;
import org.apache.stratos.cloud.controller.validate.VCloudPartitionValidator;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.vcloud.compute.options.VCloudTemplateOptions;
import org.jclouds.vcloud.domain.network.IpAddressAllocationMode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class VCloudIaas extends Iaas {


	private static final Log log = LogFactory.getLog(VCloudIaas.class);
	
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

		IaasProvider iaasInfo = getIaasProvider();
		
		// in VCloud case we need to run a script
		if (iaasInfo.getTemplate() != null && iaasInfo.getPayload() != null) {

			Template template = iaasInfo.getTemplate();
			String script = "";
			String launchParams = "", key = "";

			// open the zip file stream
			ZipInputStream stream = new ZipInputStream(
					new ByteArrayInputStream(iaasInfo.getPayload()));

			try {

				// now iterate through each item in the stream. The get next
				// entry call will return a ZipEntry for each file in the
				// stream
				ZipEntry entry;
				while ((entry = stream.getNextEntry()) != null) {
					StringWriter writer = new StringWriter();
					IOUtils.copy(stream, writer);

					if (entry.getName().contains("launch-params")) {
						launchParams = writer.toString();
					} else if (entry.getName().contains("id_rsa")) {
						key = writer.toString();
					}

				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
				// we must always close the zip file.
				try {
					stream.close();
				} catch (IOException e) {

					log.error("failed to close the ZIP stream", e);
				}
			}

			script = "mkdir /var/lib/cloud && mkdir /var/lib/cloud/instance && mkdir /var/lib/cloud/instance/payload && "
					+ "echo \""
					+ launchParams
					+ "\" > /var/lib/cloud/instance/payload/launch-params && "
					+ "echo \""
					+ key
					+ "\" > /var/lib/cloud/instance/payload/id_rsa && "
					+ "cd /opt/ && "
					+ "chmod 755 wso2-openstack-init.sh && "
					+ "./wso2-openstack-init.sh";

			template.getOptions()
					.overrideLoginUser(iaasInfo.getProperty("loginUser"))
					.overrideLoginPassword(
							iaasInfo.getProperty("loginPassword"))
					.runScript(script);
		}

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
