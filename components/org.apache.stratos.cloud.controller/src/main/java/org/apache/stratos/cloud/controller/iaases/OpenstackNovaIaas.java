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
import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.validate.OpenstackNovaPartitionValidator;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaApiMetadata;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.HostAggregate;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.AvailabilityZone;
import org.jclouds.openstack.nova.v2_0.extensions.AvailabilityZoneAPI;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.HostAggregateApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;
import org.jclouds.rest.RestContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

public class OpenstackNovaIaas extends Iaas {

	private static final Log log = LogFactory.getLog(OpenstackNovaIaas.class);
	private static final String SUCCESSFUL_LOG_LINE = "A key-pair is created successfully in ";
	private static final String FAILED_LOG_LINE = "Key-pair is unable to create in ";

	public OpenstackNovaIaas(IaasProvider iaasProvider) {
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
			throw new CloudControllerException(
					"Compute service is null for IaaS provider: "
							+ iaasInfo.getName());
		}

		TemplateBuilder templateBuilder = iaasInfo.getComputeService()
				.templateBuilder();
		templateBuilder.imageId(iaasInfo.getImage());
        if(!(iaasInfo instanceof IaasProvider)) {
           templateBuilder.locationId(iaasInfo.getType());
        }

        // to avoid creation of template objects in each and every time, we
        // create all at once!

		String instanceType;

		// set instance type
		if (((instanceType = iaasInfo.getProperty(CloudControllerConstants.INSTANCE_TYPE)) != null)) {

			templateBuilder.hardwareId(instanceType);
		}

		Template template = templateBuilder.build();

		// if you wish to auto assign IPs, instance spawning call should be
		// blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode?
		boolean blockUntilRunning = Boolean.parseBoolean(iaasInfo
				.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP));
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by Jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(new int[] {});

		if (iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUPS) != null) {
			template.getOptions()
					.as(NovaTemplateOptions.class)
					.securityGroupNames(
							iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUPS).split(
									CloudControllerConstants.ENTRY_SEPARATOR));
		}

		if (iaasInfo.getProperty(CloudControllerConstants.KEY_PAIR) != null) {
			template.getOptions().as(NovaTemplateOptions.class)
					.keyPairName(iaasInfo.getProperty(CloudControllerConstants.KEY_PAIR));
		}
		
		if (iaasInfo.getProperty(CloudControllerConstants.NETWORK_INTERFACES) != null) {
			String networksStr = iaasInfo.getProperty(CloudControllerConstants.NETWORK_INTERFACES);
			String[] networksArray = networksStr.split(CloudControllerConstants.ENTRY_SEPARATOR);
			template.getOptions()
					.as(NovaTemplateOptions.class).networks(Arrays.asList(networksArray));
		}
		
		//TODO
//		if (iaas.getProperty(CloudControllerConstants.HOST) != null) {
//            template.getOptions().as(NovaTemplateOptions.class)
//                    .(CloudControllerConstants.HOST);
//        }

		// set Template
		iaasInfo.setTemplate(template);
	}

    @Override
	public void setDynamicPayload() {

    	IaasProvider iaasInfo = getIaasProvider();
    	
		if (iaasInfo.getTemplate() != null && iaasInfo.getPayload() != null) {

			iaasInfo.getTemplate().getOptions().as(NovaTemplateOptions.class)
					.userData(iaasInfo.getPayload());
		}

	}

	@Override
	public synchronized boolean createKeyPairFromPublicKey(String region, String keyPairName,
			String publicKey) {

		IaasProvider iaasInfo = getIaasProvider();
		
		String openstackNovaMsg = " Openstack-nova. Region: " + region
				+ " - Name: ";

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
		KeyPairApi api = nova.getApi().getKeyPairExtensionForZone(region).get();

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
	public synchronized String associateAddress(NodeMetadata node) {
		
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

		RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
		FloatingIPApi floatingIp = nova.getApi().getFloatingIPExtensionForZone(
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

		NodeMetadataBuilder.fromNodeMetadata(node)
				.publicAddresses(ImmutableSet.of(ip)).build();

		log.info("Successfully associated an IP address " + ip
				+ " for node with id: " + node.getId());

		return ip;
	}

	@Override
	public synchronized void releaseAddress(String ip) {

		IaasProvider iaasInfo = getIaasProvider();
		
		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

		@SuppressWarnings("deprecation")
		RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
		@SuppressWarnings("deprecation")
		FloatingIPApi floatingIPApi = nova.getApi()
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

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
    	IaasProvider iaasInfo = getIaasProvider();
    	
        // jclouds' zone = region in openstack
        if (region == null || iaasInfo == null) {
            String msg =
                         "Region or IaaSProvider is null: region: " + region + " - IaaSProvider: " +
                                 iaasInfo;
            log.error(msg);
            throw new InvalidRegionException(msg);
        }
        
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
        Set<String> zones = nova.getApi().getConfiguredZones();
        for (String configuredZone : zones) {
            if (region.equalsIgnoreCase(configuredZone)) {
                if (log.isDebugEnabled()) {
                    log.debug("Found a matching region: " + region);
                }
                return true;
            }
        }
        
        String msg = "Invalid region: " + region +" in the iaas: "+iaasInfo.getType();
        log.error(msg);
        throw new InvalidRegionException(msg);
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException {
    	IaasProvider iaasInfo = getIaasProvider();
    	
    	// jclouds availability zone = stratos zone
    	if (region == null || zone == null || iaasInfo == null) {
            String msg = "Host or Zone or IaaSProvider is null: region: " + region + " - zone: " +
                    zone + " - IaaSProvider: " + iaasInfo;
            log.error(msg);
            throw new InvalidZoneException(msg);
        }
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
        AvailabilityZoneAPI zoneApi = nova.getApi().getAvailabilityZoneApi(region);
        for (AvailabilityZone z : zoneApi.list()) {
			
        	if (zone.equalsIgnoreCase(z.getName())) {
        		if (log.isDebugEnabled()) {
        			log.debug("Found a matching availability zone: " + zone);
        		}
        		return true;
        	}
		}
        
        String msg = "Invalid zone: " + zone +" in the region: "+region+ " and of the iaas: "+iaasInfo.getType();
        log.error(msg);
        throw new InvalidZoneException(msg);
        
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
    	IaasProvider iaasInfo = getIaasProvider();
    	
        if (host == null || zone == null || iaasInfo == null) {
            String msg = "Host or Zone or IaaSProvider is null: host: " + host + " - zone: " +
                    zone + " - IaaSProvider: " + iaasInfo;
            log.error(msg);
            throw new InvalidHostException(msg);
        }
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
        HostAggregateApi hostApi = nova.getApi().getHostAggregateExtensionForZone(zone).get();
        for (HostAggregate hostAggregate : hostApi.list()) {
            for (String configuredHost : hostAggregate.getHosts()) {
                if (host.equalsIgnoreCase(configuredHost)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found a matching host: " + host);
                    }
                    return true;
                }
            }
        }
        
        String msg = "Invalid host: " + host +" in the zone: "+zone+ " and of the iaas: "+iaasInfo.getType();
        log.error(msg);
        throw new InvalidHostException(msg);
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new OpenstackNovaPartitionValidator();
    }

	@Override
	public String createVolume(int sizeGB) {
		IaasProvider iaasInfo = getIaasProvider();
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		String zone = ComputeServiceBuilderUtil.extractZone(iaasInfo);
		
        if (region == null || iaasInfo == null) {
        	log.fatal("Cannot create a new volume in the [region] : "+region
					+" of Iaas : "+iaasInfo);
            return null;
        }
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        
        RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
        VolumeApi api = nova.getApi().getVolumeExtensionForZone(region).get();
        Volume volume = api.create(sizeGB, CreateVolumeOptions.Builder.availabilityZone(zone));
        if (volume == null) {
			log.fatal("Volume creation was unsuccessful. [region] : " + region+" [zone] : " + zone
					+ " of Iaas : " + iaasInfo);
			return null;
		}
		
		log.info("Successfully created a new volume [id]: "+volume.getId()
				+" in [region] : "+region+" [zone] : "+zone+" of Iaas : "+iaasInfo);
		return volume.getId();
	}

	@Override
	public String attachVolume(String instanceId, String volumeId, String deviceName) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		String device = deviceName == null ? "/dev/vdc" : deviceName;
		
		if(region == null) {
			log.fatal("Cannot attach the volume [id]: "+volumeId+" in the [region] : "+region
					+" of Iaas : "+iaasInfo);
			return null;
		}
		
		RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
        VolumeAttachmentApi api = nova.getApi().getVolumeAttachmentExtensionForZone(region).get();
        VolumeAttachment attachment = api.attachVolumeToServerAsDevice(volumeId, instanceId, device);
        
        if (attachment == null) {
			log.fatal("Volume [id]: "+volumeId+" attachment for instance [id]: "+instanceId
					+" was unsuccessful. [region] : " + region
					+ " of Iaas : " + iaasInfo);
			return null;
		}
		
		log.info("Volume [id]: "+volumeId+" attachment for instance [id]: "+instanceId
				+" was successful [status]: "+"Attaching"+". [region] : " + region
				+ " of Iaas : " + iaasInfo);
		return "Attaching";
	}

	@Override
	public void detachVolume(String instanceId, String volumeId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		
		if(region == null) {
			log.fatal("Cannot detach the volume [id]: "+volumeId+" from the instance [id]: "+instanceId
					+" of the [region] : "+region
					+" of Iaas : "+iaasInfo);
			return;
		}
		
		RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
        VolumeAttachmentApi api = nova.getApi().getVolumeAttachmentExtensionForZone(region).get();
        if (api.detachVolumeFromServer(volumeId, instanceId)) {
        	log.info("Detachment of Volume [id]: "+volumeId+" from instance [id]: "+instanceId
    				+" was successful. [region] : " + region
    				+ " of Iaas : " + iaasInfo);
        }
        
	}

	@Override
	public void deleteVolume(String volumeId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		
		if(region == null) {
			log.fatal("Cannot delete the volume [id]: "+volumeId+" of the [region] : "+region
					+" of Iaas : "+iaasInfo);
			return;
		}
		
		RestContext<NovaApi, NovaAsyncApi> nova = context.unwrap();
		VolumeApi api = nova.getApi().getVolumeExtensionForZone(region).get();
        if (api.delete(volumeId)) {
        	log.info("Deletion of Volume [id]: "+volumeId+" was successful. [region] : " + region
    				+ " of Iaas : " + iaasInfo);
        }
	}

}
