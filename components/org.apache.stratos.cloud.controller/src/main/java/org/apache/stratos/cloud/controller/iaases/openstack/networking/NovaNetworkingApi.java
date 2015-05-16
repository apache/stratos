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

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Leveraging openstack-nova networking apis.
 */
public class NovaNetworkingApi implements OpenstackNetworkingApi {

    private static final Log log = LogFactory.getLog(NovaNetworkingApi.class);
    private IaasProvider iaasProvider;

    public NovaNetworkingApi(IaasProvider iaasProvider) {
        this.iaasProvider = iaasProvider;
    }

    @Override
    public List<String> associateAddresses(NodeMetadata node) {

        ComputeServiceContext context = iaasProvider.getComputeService().getContext();
        String region = ComputeServiceBuilderUtil.extractRegion(iaasProvider);

        if(StringUtils.isEmpty(region)) {
            throw new RuntimeException("Could not find region in iaas provider: " + iaasProvider.getName());
        }

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        FloatingIPApi floatingIPApi = novaApi.getFloatingIPExtensionForZone(region).get();

        String ip = null;
        // first try to find an unassigned IP.
        FluentIterable<FloatingIP> floatingIPs = floatingIPApi.list();
        ArrayList<FloatingIP> unassignedIps = Lists.newArrayList(Iterables.filter(floatingIPs,
                new Predicate<FloatingIP>() {
                    @Override
                    public boolean apply(FloatingIP floatingIP) {
                        return floatingIP.getInstanceId() == null;
                    }
                }));

        if (!unassignedIps.isEmpty()) {
            // try to prevent multiple parallel launches from choosing the same ip.
            Collections.shuffle(unassignedIps);
            ip = Iterables.getLast(unassignedIps).getIp();
        }

        // if no unassigned IP is available, we'll try to allocate an IP.
        if (StringUtils.isEmpty(ip)) {
            String floatingIpPool = iaasProvider.getProperty(CloudControllerConstants.DEFAULT_FLOATING_IP_POOL);
            FloatingIP allocatedFloatingIP;
            if (StringUtils.isEmpty(floatingIpPool)) {
                allocatedFloatingIP = floatingIPApi.create();
            } else {
                log.debug(String.format("Trying to allocate a floating IP address from IP pool %s", floatingIpPool));
                allocatedFloatingIP = floatingIPApi.allocateFromPool(floatingIpPool);
            }
            if (allocatedFloatingIP == null) {
                String msg = String.format("Floating IP API did not return a floating IP address from IP pool %s",
                        floatingIpPool);
                log.error(msg);
                throw new CloudControllerException(msg);
            }
            ip = allocatedFloatingIP.getIp();
        }

        // wait till the fixed IP address gets assigned - this is needed before
        // we associate a public IP
        log.info(String.format("Waiting for private IP addresses get allocated: [node-id] %s", node.getId()));
        while (node.getPrivateAddresses() == null) {
            CloudControllerUtil.sleep(1000);
        }
        log.info(String.format("Private IP addresses allocated: %s", node.getPrivateAddresses()));

        if ((node.getPublicAddresses() != null) && (node.getPublicAddresses().iterator().hasNext())) {
            log.info("Public IP address "
                    + node.getPublicAddresses().iterator().next()
                    + " is already allocated to the instance: [node-id]  "
                    + node.getId());
            return null;
        }

        int retries = 0;
        int retryCount = Integer.getInteger("stratos.public.ip.association.retry.count", 5);
        while ((retries < retryCount) && (!associateIp(floatingIPApi, ip, node.getProviderId()))) {
            // wait for 5s
            CloudControllerUtil.sleep(5000);
            retries++;
        }

        log.info(String.format("Successfully associated an IP address: [node-id] %s [ip] %s", node.getId(), ip));

        List<String> allocatedIPAddresses = new ArrayList<String>();
        allocatedIPAddresses.add(ip);
        return allocatedIPAddresses;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Trying to associate predefined IP address: [node-id] %s [ip] %s",
                    node.getId(), ip));
        }

        ComputeServiceContext context = iaasProvider.getComputeService().getContext();
        String region = ComputeServiceBuilderUtil.extractRegion(iaasProvider);

        FloatingIPApi floatingIPApi = context.unwrapApi(NovaApi.class).getFloatingIPExtensionForZone(region).get();

        // get the list of all unassigned IP.
        ArrayList<FloatingIP> unassignedFloatingIPs = Lists.newArrayList(Iterables.filter(floatingIPApi.list(),
                new Predicate<FloatingIP>() {
                    @Override
                    public boolean apply(FloatingIP floatingIP) {
                        return StringUtils.isEmpty(floatingIP.getFixedIp());
                    }
                }));

        boolean isAvailable = false;
        for (FloatingIP floatingIP : unassignedFloatingIPs) {
            if (log.isDebugEnabled()) {
                log.debug("OpenstackNovaIaas:associatePredefinedAddress:iterating over available floatingip:" + floatingIP);
            }
            if (ip.equals(floatingIP.getIp())) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("OpenstackNovaIaas:associatePredefinedAddress:floating ip in use:%s /ip:%s", floatingIP, ip));
                }
                isAvailable = true;
                break;
            }
        }

        if (isAvailable) {
            // assign ip
            if (log.isDebugEnabled()) {
                log.debug("OpenstackNovaIaas:associatePredefinedAddress:assign floating ip:" + ip);
            }
            // exercise same code as in associateAddress()
            // wait till the fixed IP address gets assigned - this is needed before
            // we associate a public IP

            while (node.getPrivateAddresses() == null) {
                CloudControllerUtil.sleep(1000);
            }

            int retries = 0;
            int retryCount = Integer.getInteger("stratos.public.ip.association.retry.count", 5);
            while (retries < retryCount && !associateIp(floatingIPApi, ip, node.getProviderId())) {
                // wait for 5s
                CloudControllerUtil.sleep(5000);
                retries++;
            }

            NodeMetadataBuilder.fromNodeMetadata(node).publicAddresses(ImmutableSet.of(ip)).build();
            log.info(String.format("Successfully associated predefined IP address: [node-id] %s [ip] %s ",
                    node.getId(), ip));
            return ip;
        } else {
            log.warn(String.format("Could not associate predefined IP address: [node-id] %s [ip] %s ",
                    node.getId(), ip));
            return null;
        }
    }

    @Override
    public void releaseAddress(String ip) {

        ComputeServiceContext context = iaasProvider.getComputeService().getContext();
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

    private boolean associateIp(FloatingIPApi floatingIPApi, String ip, String providerId) {
        try {
            floatingIPApi.addToServer(ip, providerId);
            return true;
        } catch (RuntimeException e) {
            log.warn(String.format("Could not associate IP address to instance: [ip] %s [provider-id] %s",
                    ip, providerId), e);
            return false;
        }
    }
}