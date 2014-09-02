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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.pojo.NetworkInterface;
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
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.HostAggregate;
import org.jclouds.openstack.nova.v2_0.domain.KeyPair;
import org.jclouds.openstack.nova.v2_0.domain.Network;
import org.jclouds.openstack.nova.v2_0.domain.Volume;
import org.jclouds.openstack.nova.v2_0.domain.VolumeAttachment;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.AvailabilityZone;
import org.jclouds.openstack.nova.v2_0.extensions.AvailabilityZoneApi;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.HostAggregateApi;
import org.jclouds.openstack.nova.v2_0.extensions.KeyPairApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeApi;
import org.jclouds.openstack.nova.v2_0.extensions.VolumeAttachmentApi;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;
import org.jclouds.rest.RestContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

@SuppressWarnings("deprecation")
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

		// In Openstack the call to IaaS should be blocking, in order to retrieve 
		// IP addresses.
		boolean blockUntilRunning = true;
		if(iaasInfo.getProperty(CloudControllerConstants.BLOCK_UNTIL_RUNNING) != null) {
			blockUntilRunning = Boolean.parseBoolean(iaasInfo.getProperty(
					CloudControllerConstants.BLOCK_UNTIL_RUNNING));
		}
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
		
        if (iaasInfo.getNetworkInterfaces() != null) {
            Set<Network> novaNetworksSet = new LinkedHashSet<Network>(iaasInfo.getNetworkInterfaces().length);
            for (NetworkInterface ni:iaasInfo.getNetworkInterfaces()) {
                novaNetworksSet.add(Network.builder().networkUuid(ni.getNetworkUuid()).fixedIp(ni.getFixedIp())
                        .portUuid(ni.getPortUuid()).build());
            }
            template.getOptions().as(NovaTemplateOptions.class).novaNetworks(novaNetworksSet);
        }
		
		if (iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
			template.getOptions().as(NovaTemplateOptions.class)
					.availabilityZone(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE));
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
        NovaApi novaApi = context.unwrapApi(NovaApi.class);
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
	public synchronized String associateAddress(NodeMetadata node) {
		
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

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
			String defaultFloatingIpPool = iaasInfo.getProperty(CloudControllerConstants.DEFAULT_FLOATING_IP_POOL);
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

		return ip;
	}
	
	@Override
	public synchronized String associatePredefinedAddress (NodeMetadata node, String ip) {
		if(log.isDebugEnabled()) {
			log.debug("OpenstackNovaIaas:associatePredefinedAddress:ip:" + ip);
		}
		
		IaasProvider iaasInfo = getIaasProvider();
		
		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

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
			return "";
		}

		
		NodeMetadataBuilder.fromNodeMetadata(node)
				.publicAddresses(ImmutableSet.of(ip)).build();

		log.info("OpenstackNovaIaas:associatePredefinedAddress::Successfully associated an IP address " + ip
				+ " for node with id: " + node.getId());

		return ip;
		
	}	

	@Override
	public synchronized void releaseAddress(String ip) {

		IaasProvider iaasInfo = getIaasProvider();
		
		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();

		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

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
        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        Set<String> zones = novaApi.getConfiguredZones();
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
        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        Optional<? extends AvailabilityZoneApi> availabilityZoneApi = novaApi.getAvailabilityZoneApi(region);
        for (AvailabilityZone z : availabilityZoneApi.get().list()) {
			
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
            String msg = String.format("Host or Zone or IaaSProvider is null: host: %s - zone: %s - IaaSProvider: %s", host, zone, iaasInfo);
            log.error(msg);
            throw new InvalidHostException(msg);
        }
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        HostAggregateApi hostApi = novaApi.getHostAggregateExtensionForZone(zone).get();
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
        
        String msg = String.format("Invalid host: %s in the zone: %s and of the iaas: %s", host, zone, iaasInfo.getType());
        log.error(msg);
        throw new InvalidHostException(msg);
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new OpenstackNovaPartitionValidator();
    }

	@Override
	public String createVolume(int sizeGB, String snapshotId) {
		IaasProvider iaasInfo = getIaasProvider();
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		String zone = ComputeServiceBuilderUtil.extractZone(iaasInfo);
		
        if (region == null || iaasInfo == null) {
        	log.fatal(String.format("Cannot create a new volume in the [region] : %s of Iaas : %s", region, iaasInfo));
            return null;
        }
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        VolumeApi volumeApi = novaApi.getVolumeExtensionForZone(region).get();
        Volume volume;
        if(StringUtils.isEmpty(snapshotId)){
        	if(log.isDebugEnabled()){
        		log.info("Creating a volume in the zone " + zone);
        	}
        	volume = volumeApi.create(sizeGB, CreateVolumeOptions.Builder.availabilityZone(zone));
        }else{
        	if(log.isDebugEnabled()){
        		log.info("Creating a volume in the zone " + zone + " from the shanpshot " + snapshotId);
        	}
        	volume = volumeApi.create(sizeGB, CreateVolumeOptions.Builder.availabilityZone(zone).snapshotId(snapshotId));
        }

        if (volume == null) {
            log.fatal(String.format("Volume creation was unsuccessful. [region] : %s [zone] : %s of Iaas : %s", region, zone, iaasInfo));
            return null;
        }

        String volumeId = volume.getId();
        /*
        Volume.Status volumeStatus = this.getVolumeStatus(volumeApi, volumeId);

        if(!(volumeStatus == Volume.Status.AVAILABLE || volumeStatus == Volume.Status.CREATING)){
            log.error(String.format("Error while creating [volume id] %s. Volume status is %s", volumeId, volumeStatus));
            return volumeId;
        }
        try {
            if(!waitForStatus(volumeApi, volumeId, Volume.Status.AVAILABLE)){
                log.error("Volume did not become AVAILABLE. Current status is " + volume.getStatus());
            }
        } catch (TimeoutException e) {
            log.error("[Volume ID] " + volumeId + "did not become AVAILABLE within expected timeout");
            return volumeId;
        }
        */
		log.info(String.format("Successfully created a new volume [id]: %s in [region] : %s [zone] : %s of Iaas : %s [Volume ID]%s", volume.getId(), region, zone, iaasInfo, volume.getId()));
		return volumeId;
	}

    private boolean waitForStatus(String volumeId, Volume.Status expectedStatus, int timeoutInMins) throws TimeoutException {
        long timeout = 1000 * 60 * timeoutInMins;
        long timout = System.currentTimeMillis() + timeout;

        IaasProvider iaasInfo = getIaasProvider();
        String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();;
        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        VolumeApi volumeApi = novaApi.getVolumeExtensionForZone(region).get();
        Volume.Status volumeStatus = this.getVolumeStatus(volumeApi, volumeId);

        while(volumeStatus != expectedStatus){
            try {
                if(log.isDebugEnabled()){
                    log.debug(String.format("Volume %s is still NOT in %s. Current State=%s", volumeId, expectedStatus, volumeStatus));
                }
                if(volumeStatus == Volume.Status.ERROR){
                    log.error("Volume " + volumeId + " is in state ERROR");
                    return false;
                }
                Thread.sleep(1000);
                volumeStatus = this.getVolumeStatus(volumeApi, volumeId);
                if (System.currentTimeMillis()> timout) {
                    throw new TimeoutException();
                }
            } catch (InterruptedException e) {
                // Ignoring the exception
            }
        }
        if(log.isDebugEnabled()){
            log.debug(String.format("Volume %s status became %s", volumeId, expectedStatus));
        }

        return true;
    }

    @Override
	public String attachVolume(String instanceId, String volumeId, String deviceName) {
        IaasProvider iaasInfo = getIaasProvider();

        if (StringUtils.isEmpty(volumeId)) {
            log.error("Volume provided to attach can not be null");
        }

        if (StringUtils.isEmpty(instanceId)) {
            log.error("Instance provided to attach can not be null");
        }

        ComputeServiceContext context = iaasInfo.getComputeService()
                .getContext();
        String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
        String device = deviceName == null ? "/dev/vdc" : deviceName;

        if (region == null) {
            log.fatal(String.format("Cannot attach the volume [id]: %s in the [region] : %s of Iaas : %s", volumeId, region, iaasInfo));
            return null;
        }

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        VolumeApi volumeApi = novaApi.getVolumeExtensionForZone(region).get();
        VolumeAttachmentApi volumeAttachmentApi = novaApi.getVolumeAttachmentExtensionForZone(region).get();

        Volume.Status volumeStatus = this.getVolumeStatus(volumeApi, volumeId);

        if (log.isDebugEnabled()) {
            log.debug("Volume " + volumeId + " is in state " + volumeStatus);
        }

        if (!(volumeStatus == Volume.Status.AVAILABLE || volumeStatus == Volume.Status.CREATING)) {
            log.error(String.format("Volume %s can not be attached. Volume status is %s", volumeId, volumeStatus));
            return null;
        }

        boolean volumeBecameAvailable = false, volumeBecameAttached = false;
        try {
            volumeBecameAvailable = waitForStatus(volumeId, Volume.Status.AVAILABLE, 5);
        } catch (TimeoutException e) {
            log.error("[Volume ID] " + volumeId + "did not become AVAILABLE within expected timeout");
        }

        VolumeAttachment attachment = null;
        if (volumeBecameAvailable) {
            attachment = volumeAttachmentApi.attachVolumeToServerAsDevice(volumeId, instanceId, device);

            try {
                volumeBecameAttached = waitForStatus(volumeId, Volume.Status.IN_USE, 2);
            } catch (TimeoutException e) {
                log.error("[Volume ID] " + volumeId + "did not become IN_USE within expected timeout");
            }
        }
        try {
            // waiting 5seconds till volumes are actually attached.
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (attachment == null) {
			log.fatal(String.format("Volume [id]: %s attachment for instance [id]: %s was unsuccessful. [region] : %s of Iaas : %s", volumeId, instanceId, region, iaasInfo));
			return null;
		}

        if(! volumeBecameAttached){
           log.error(String.format("[Volume ID] %s attachment is called, but not yet became attached", volumeId));
        }

		log.info(String.format("Volume [id]: %s attachment for instance [id]: %s was successful [status]: Attaching. [region] : %s of Iaas : %s", volumeId, instanceId, region, iaasInfo));
		return "Attaching";
	}

	@Override
	public void detachVolume(String instanceId, String volumeId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		
		if(region == null) {
			log.fatal(String.format("Cannot detach the volume [id]: %s from the instance [id]: %s of the [region] : %s of Iaas : %s", volumeId, instanceId, region, iaasInfo));
			return;
		}
        if(log.isDebugEnabled()) {
            log.debug(String.format("Starting to detach volume %s from the instance %s", volumeId, instanceId));
        }

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        VolumeAttachmentApi api = novaApi.getVolumeAttachmentExtensionForZone(region).get();
        if (api.detachVolumeFromServer(volumeId, instanceId)) {
        	log.info(String.format("Detachment of Volume [id]: %s from instance [id]: %s was successful. [region] : %s of Iaas : %s", volumeId, instanceId, region, iaasInfo));
        }else{
            log.error(String.format("Detachment of Volume [id]: %s from instance [id]: %s was unsuccessful. [volume Status] : %s", volumeId, instanceId, region, iaasInfo));
        }
        
	}

	@Override
	public void deleteVolume(String volumeId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		
		if(region == null) {
			log.fatal(String.format("Cannot delete the volume [id]: %s of the [region] : %s of Iaas : %s", volumeId, region, iaasInfo));
			return;
		}

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
		VolumeApi api = novaApi.getVolumeExtensionForZone(region).get();
        if (api.delete(volumeId)) {
        	log.info(String.format("Deletion of Volume [id]: %s was successful. [region] : %s of Iaas : %s", volumeId, region, iaasInfo));
        }
	}

    @Override
    public String getIaasDevice(String device) {
        return device;
    }

    private Volume.Status getVolumeStatus(VolumeApi volumeApi, String volumeId){
        return volumeApi.get(volumeId).getStatus();
    }
}
