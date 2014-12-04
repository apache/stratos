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
package org.apache.stratos.cloud.controller.iaases.openstack.networking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Leveraging openstack-nova networking apis.
 * 
 * @author rajkumar
 */
public class NovaNetworkingApi implements OpenstackNetworkingApi {

    private static final Log log = LogFactory.getLog(NovaNetworkingApi.class);
    private IaasProvider iaasProvider;

    public NovaNetworkingApi(IaasProvider iaasProvider) {
        this.iaasProvider = iaasProvider;
    }

    @Override
    public List<String> associateAddresses(NodeMetadata node) {

		ComputeServiceContext context = iaasProvider.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasProvider);

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
		FloatingIPApi floatingIp = novaApi.getFloatingIPExtensionForZone(
				region).get();

		String ip = null;
		// first try to find an unassigned IP.
		ArrayList<FloatingIP> unassignedIps = Lists.newArrayList(Iterables
				.filter(floatingIp.list(),
						new Predicate<FloatingIP>() {

							@Override
							public boolean apply(FloatingIP arg0) {
								return arg0.getInstanceId() == null;
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
			String defaultFloatingIpPool = iaasProvider.getProperty(CloudControllerConstants.DEFAULT_FLOATING_IP_POOL);
			FloatingIP allocatedFloatingIP;
			if ((defaultFloatingIpPool == null) || "".equals(defaultFloatingIpPool)) {
				allocatedFloatingIP = floatingIp.create();
			} else {
				allocatedFloatingIP = floatingIp.allocateFromPool(defaultFloatingIpPool);
			}
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
		
		if (node.getPublicAddresses() != null
				&& node.getPublicAddresses().iterator().hasNext()) {
			log.info("A public IP ("
					+ node.getPublicAddresses().iterator().next()
					+ ") is already allocated to the instance [id] : "
					+ node.getId());
			return null;
		}

		int retries = 0;
		//TODO make 5 configurable
		while (retries < 5
				&& !associateIp(floatingIp, ip, node.getProviderId())) {

			// wait for 5s
			CloudControllerUtil.sleep(5000);
			retries++;
		}

		log.info("Successfully associated an IP address " + ip
				+ " for node with id: " + node.getId());

		List<String> allocatedIPAddresses = new ArrayList<String>();
		allocatedIPAddresses.add(ip);
		
		return allocatedIPAddresses;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {
		if(log.isDebugEnabled()) {
			log.debug("OpenstackNovaIaas:associatePredefinedAddress:ip:" + ip);
		}
		
		ComputeServiceContext context = iaasProvider.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasProvider);

		FloatingIPApi floatingIp = context.unwrapApi(NovaApi.class).getFloatingIPExtensionForZone(
                region).get();

		if(log.isDebugEnabled()) {
			log.debug("OpenstackNovaIaas:associatePredefinedAddress:floatingip:" + floatingIp);
		}
		
		// get the list of all unassigned IP.
		ArrayList<FloatingIP> unassignedIps = Lists.newArrayList(Iterables
				.filter(floatingIp.list(),
						new Predicate<FloatingIP>() {

							@Override
							public boolean apply(FloatingIP arg0) {
								// FIXME is this the correct filter?
								return arg0.getFixedIp() == null;
							}

						}));
		
		boolean isAvailable = false;
		for (FloatingIP fip : unassignedIps) {
			if(log.isDebugEnabled()) {
				log.debug("OpenstackNovaIaas:associatePredefinedAddress:iterating over available floatingip:" + fip);
			}
			if (ip.equals(fip.getIp())) {
				if(log.isDebugEnabled()) {
					log.debug(String.format("OpenstackNovaIaas:associatePredefinedAddress:floating ip in use:%s /ip:%s", fip, ip));
				}
				isAvailable = true;
				break;
			}
		}
		
		if (isAvailable) {
			// assign ip
			if(log.isDebugEnabled()) {
				log.debug("OpenstackNovaIaas:associatePredefinedAddress:assign floating ip:" + ip);
			}
			// exercise same code as in associateAddress()
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

			log.info("OpenstackNovaIaas:associatePredefinedAddress:Successfully associated an IP address " + ip
					+ " for node with id: " + node.getId());
		} else {
			// unable to allocate predefined ip,
			log.info("OpenstackNovaIaas:associatePredefinedAddress:Unable to allocate predefined ip:" 
					+ " for node with id: " + node.getId());
			return null;
		}

		
		NodeMetadataBuilder.fromNodeMetadata(node)
				.publicAddresses(ImmutableSet.of(ip)).build();

		log.info("OpenstackNovaIaas:associatePredefinedAddress::Successfully associated an IP address " + ip
				+ " for node with id: " + node.getId());

		return ip;
    }

    @Override
    public void releaseAddress(String ip) {
    	
		ComputeServiceContext context = iaasProvider.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasProvider);

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
		FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone(region).get();

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