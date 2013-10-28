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

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.IaasProvider;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;

import java.util.ArrayList;
import java.util.Collections;

public class OpenstackNovaIaas extends Iaas {

	private static final Log log = LogFactory.getLog(OpenstackNovaIaas.class);
	private static final String SUCCESSFUL_LOG_LINE = "A key-pair is created successfully in ";
	private static final String FAILED_LOG_LINE = "Key-pair is unable to create in ";

	@Override
	public void buildComputeServiceAndTemplate(IaasProvider iaasInfo) {

		// builds and sets Compute Service
		ComputeServiceBuilderUtil.buildDefaultComputeService(iaasInfo);

		// builds and sets Template
		buildTemplate(iaasInfo);

	}

	private void buildTemplate(IaasProvider iaas) {
		if (iaas.getComputeService() == null) {
			throw new CloudControllerException(
					"Compute service is null for IaaS provider: "
							+ iaas.getName());
		}

		// // if domain to template map is null
		// if (entity.getDomainToTemplateMap() == null) {
		// // we initialize it
		// entity.setDomainToTemplateMap(new HashMap<String, Template>());
		// }

		TemplateBuilder templateBuilder = iaas.getComputeService()
				.templateBuilder();
		templateBuilder.imageId(iaas.getImage());

		// to avoid creation of template objects in each and every time, we
		// create all
		// at once!
		// for (org.apache.cartridge.autoscaler.service.util.ServiceContext temp
		// :
		// serviceContexts) {

		String instanceType;

		// set instance type
		if (((instanceType = iaas.getProperty("instanceType")) != null)) {

			templateBuilder.hardwareId(instanceType);
		}

		Template template = templateBuilder.build();

		// if you wish to auto assign IPs, instance spawning call should be
		// blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode?
		boolean blockUntilRunning = Boolean.parseBoolean(iaas
				.getProperty("autoAssignIp"));
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by Jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(new int[] {});

		if (iaas.getProperty("securityGroups") != null) {
			template.getOptions()
					.as(NovaTemplateOptions.class)
					.securityGroupNames(
							iaas.getProperty("securityGroups").split(
									CloudControllerConstants.ENTRY_SEPARATOR));
		}

		/*if (iaas.getProperty(CloudControllerConstants.PAYLOAD_FOLDER) != null) {
			template.getOptions()
					.as(NovaTemplateOptions.class)
					.userData(
							ComputeServiceBuilderUtil.getUserData(CarbonUtils
									.getCarbonHome()
									+ File.separator
									+ iaas.getProperty(CloudControllerConstants.PAYLOAD_FOLDER)));
		}
*/
		if (iaas.getProperty("keyPair") != null) {
			template.getOptions().as(NovaTemplateOptions.class)
					.keyPairName(iaas.getProperty("keyPair"));
		}

		// set Template
		iaas.setTemplate(template);
	}

	@Override
	public void setDynamicPayload(IaasProvider iaasInfo) {

		if (iaasInfo.getTemplate() != null && iaasInfo.getPayload() != null) {

			iaasInfo.getTemplate().getOptions().as(NovaTemplateOptions.class)
					.userData(iaasInfo.getPayload());
		}

	}

	@Override
	public synchronized boolean createKeyPairFromPublicKey(
			IaasProvider iaasInfo, String region, String keyPairName,
			String publicKey) {

		String openstackNovaMsg = " Openstack-nova. Region: " + region
				+ " - Name: ";

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		NovaApi novaApi = context.unwrap(NovaApiMetadata.CONTEXT_TOKEN).getApi();

		KeyPairApi api = novaApi.getKeyPairExtensionForZone(region).get();

		KeyPair keyPair = api.createWithPublicKey(keyPairName, publicKey);

		if (keyPair != null) {

			iaasInfo.getTemplate().getOptions().as(NovaTemplateOptions.class)
					.keyPairName(keyPair.getName());

			log.info(SUCCESSFUL_LOG_LINE + openstackNovaMsg + keyPair.getName());
			return true;
		}

		log.error(FAILED_LOG_LINE + openstackNovaMsg);
		return false;

	}

	@Override
	public synchronized String associateAddress(IaasProvider iaasInfo,
			NodeMetadata node) {

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		NovaApi novaClient = context.unwrap(NovaApiMetadata.CONTEXT_TOKEN).getApi();
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

		FloatingIPApi floatingIp = novaClient.getFloatingIPExtensionForZone(
				region).get();

		String ip = null;
		// first try to find an unassigned IP.
		ArrayList<FloatingIP> unassignedIps = Lists.newArrayList(Iterables
				.filter(floatingIp.list(),
						new Predicate<FloatingIP>() {

							@Override
							public boolean apply(FloatingIP arg0) {
								// FIXME is this the correct filter?
								return arg0.getFixedIp() == null;
							}

						}));

		if (!unassignedIps.isEmpty()) {
			// try to prevent multiple parallel launches from choosing the same
			// ip.
			Collections.shuffle(unassignedIps);
			ip = Iterables.getLast(unassignedIps).getIp();
		}

		// if no unassigned IP is available, we'll try to allocate an IP.
		if (ip == null || ip.isEmpty()) {
			FloatingIP allocatedFloatingIP = floatingIp.create();
			if (allocatedFloatingIP == null) {
				String msg = "Failed to allocate an IP address.";
				log.error(msg);
				throw new CloudControllerException(msg);
			}
			ip = allocatedFloatingIP.getIp();
		}

		// wait till the fixed IP address gets assigned - this is needed before
		// we associate a public IP

		while (node.getPrivateAddresses() == null) {
			CloudControllerUtil.sleep(1000);
		}

		int retries = 0;
		while (retries < 5
				&& !associateIp(floatingIp, ip, node.getProviderId())) {

			// wait for 5s
			CloudControllerUtil.sleep(5000);
			retries++;
		}

		NodeMetadataBuilder.fromNodeMetadata(node)
				.publicAddresses(ImmutableSet.of(ip)).build();

		log.info("Successfully associated an IP address " + ip
				+ " for node with id: " + node.getId());

		return ip;
	}

	@Override
	public synchronized void releaseAddress(IaasProvider iaasInfo, String ip) {

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		NovaApi novaApi = context.unwrap(NovaApiMetadata.CONTEXT_TOKEN).getApi();
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

		FloatingIPApi floatingIPApi = novaApi
				.getFloatingIPExtensionForZone(region).get();

		for (FloatingIP floatingIP : floatingIPApi.list()) {
			if (floatingIP.getIp().equals(ip)) {
				floatingIPApi.delete(floatingIP.getId());
				break;
			}
		}

	}

	private boolean associateIp(FloatingIPApi api, String ip, String id) {
		try {
			api.addToServer(ip, id);
			return true;
		} catch (RuntimeException ex) {
			return false;
		}
	}

}
