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
package org.apache.stratos.cloud.controller.iaases.openstack;

import com.google.common.base.Optional;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.domain.IaasProvider;
import org.apache.stratos.cloud.controller.domain.NetworkInterface;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.apache.stratos.cloud.controller.iaases.JcloudsIaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.iaases.openstack.networking.NeutronNetworkingApi;
import org.apache.stratos.cloud.controller.iaases.openstack.networking.NovaNetworkingApi;
import org.apache.stratos.cloud.controller.iaases.openstack.networking.OpenstackNetworkingApi;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.*;
import org.jclouds.openstack.nova.v2_0.domain.zonescoped.AvailabilityZone;
import org.jclouds.openstack.nova.v2_0.extensions.*;
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class OpenstackIaas extends JcloudsIaas {

	private static final Log log = LogFactory.getLog(OpenstackIaas.class);
	private static final String SUCCESSFUL_LOG_LINE = "A key-pair is created successfully in ";
	private static final String FAILED_LOG_LINE = "Key-pair is unable to create in ";
	
	private OpenstackNetworkingApi openstackNetworkingApi;

	public OpenstackIaas(IaasProvider iaasProvider) {
		super(iaasProvider);
		setOpenstackNetworkingApi(iaasProvider);
	}
	
    private void setOpenstackNetworkingApi(IaasProvider iaasProvider) {
        String openstackNetworkingProvider = iaasProvider.getProperty(CloudControllerConstants.OPENSTACK_NETWORKING_PROVIDER);
        if (openstackNetworkingProvider != null && 
                        openstackNetworkingProvider.equals(CloudControllerConstants.OPENSTACK_NEUTRON_NETWORKING)) {
                if (log.isDebugEnabled()) {
                        String msg = String.format("Openstack networking provider is %s. Trying to instanstiate %s", 
                                        openstackNetworkingProvider, NeutronNetworkingApi.class.getName());
                        log.debug(msg);
                }
                openstackNetworkingApi = new NeutronNetworkingApi(iaasProvider);
        } else {
                if (log.isDebugEnabled()) {
                        String msg = String.format("Openstack networking provider is %s. Hence trying to instanstiate %s", 
                                        openstackNetworkingProvider, NovaNetworkingApi.class.getName());
                        log.debug(msg);
                }
                openstackNetworkingApi = new NovaNetworkingApi(iaasProvider);
        }
    }
	
	@Override
	public void buildComputeServiceAndTemplate() {
		// builds and sets Compute Service
        ComputeService computeService = ComputeServiceBuilderUtil.buildDefaultComputeService(getIaasProvider());
        getIaasProvider().setComputeService(computeService);

		// builds and sets Template
		buildTemplate();
	}

	public void buildTemplate() {
		IaasProvider iaasProvider = getIaasProvider();
		
		if (iaasProvider.getComputeService() == null) {
			throw new CloudControllerException(
					"Compute service is null for IaaS provider: "
							+ iaasProvider.getName());
		}

		TemplateBuilder templateBuilder = iaasProvider.getComputeService()
				.templateBuilder();
		templateBuilder.imageId(iaasProvider.getImage());
        if(!(iaasProvider instanceof IaasProvider)) {
           templateBuilder.locationId(iaasProvider.getType());
        }
        
        // to avoid creation of template objects in each and every time, we
        // create all at once!

		String instanceType;

		// set instance type
		if (((instanceType = iaasProvider.getProperty(CloudControllerConstants.INSTANCE_TYPE)) != null)) {

			templateBuilder.hardwareId(instanceType);
		}

		Template template = templateBuilder.build();

		// In Openstack the call to IaaS should be blocking, in order to retrieve 
		// IP addresses.
		boolean blockUntilRunning = true;
		if(iaasProvider.getProperty(CloudControllerConstants.BLOCK_UNTIL_RUNNING) != null) {
			blockUntilRunning = Boolean.parseBoolean(iaasProvider.getProperty(
					CloudControllerConstants.BLOCK_UNTIL_RUNNING));
		}
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by Jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(new int[] {});

		if (iaasProvider.getProperty(CloudControllerConstants.SECURITY_GROUPS) != null) {
			template.getOptions()
					.as(NovaTemplateOptions.class)
					.securityGroupNames(
							iaasProvider.getProperty(CloudControllerConstants.SECURITY_GROUPS).split(
									CloudControllerConstants.ENTRY_SEPARATOR));
		}

		if (iaasProvider.getProperty(CloudControllerConstants.KEY_PAIR) != null) {
			template.getOptions().as(NovaTemplateOptions.class)
					.keyPairName(iaasProvider.getProperty(CloudControllerConstants.KEY_PAIR));
		}
		
        if (iaasProvider.getNetworkInterfaces() != null) {
            Set<Network> novaNetworksSet = new LinkedHashSet<Network>(iaasProvider.getNetworkInterfaces().length);
            for (NetworkInterface ni:iaasProvider.getNetworkInterfaces()) {
                novaNetworksSet.add(Network.builder().networkUuid(ni.getNetworkUuid()).fixedIp(ni.getFixedIp())
                        .portUuid(ni.getPortUuid()).build());
            }
            template.getOptions().as(NovaTemplateOptions.class).novaNetworks(novaNetworksSet);
        }
		
		if (iaasProvider.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
			template.getOptions().as(NovaTemplateOptions.class)
					.availabilityZone(iaasProvider.getProperty(CloudControllerConstants.AVAILABILITY_ZONE));
		}
		
		//TODO
//		if (iaas.getProperty(CloudControllerConstants.HOST) != null) {
//            template.getOptions().as(NovaTemplateOptions.class)
//                    .(CloudControllerConstants.HOST);
//        }

		// set Template
		iaasProvider.setTemplate(template);
	}

    @Override
	public void setDynamicPayload(byte[] payload) {
		if (getIaasProvider().getTemplate() != null) {
			getIaasProvider().getTemplate().getOptions().as(NovaTemplateOptions.class).userData(payload);
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
	public synchronized List<String> associateAddresses(NodeMetadata node) {
		//TODO return the list of IP addresses once the topology changes is done
		return openstackNetworkingApi.associateAddresses(node);
	}
	
	@Override
	public synchronized String associatePredefinedAddress (NodeMetadata node, String ip) {
		return openstackNetworkingApi.associatePredefinedAddress(node, ip);
	}	

	@Override
	public synchronized void releaseAddress(String ip) {
		openstackNetworkingApi.releaseAddress(ip);
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
        return new OpenstackPartitionValidator();
    }

	@Override
	public String createVolume(int sizeGB, String snapshotId) {
		IaasProvider iaasInfo = getIaasProvider();
		
		if (iaasInfo == null) {
		    log.fatal(String.format("Cannot create a new volume with snapshot ID : %s", snapshotId));
		    return null;
		}
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		String zone = ComputeServiceBuilderUtil.extractZone(iaasInfo);
		
        if (region == null) {
        	log.fatal(String.format("Cannot create a new volume. Extracted region is null for Iaas : %s", iaasInfo));
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
        int timeout = 1000 * 60 * timeoutInMins;
        long timout = System.currentTimeMillis() + timeout;

        IaasProvider iaasInfo = getIaasProvider();
        String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
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
            log.fatal(String.format("Cannot attach the volume [id]: %s. Extracted region is null for Iaas : %s", volumeId, iaasInfo));
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

            attachment = volumeAttachmentApi.attachVolumeToServerAsDevice(volumeId, removeRegionPrefix(instanceId), device);

            try {
                volumeBecameAttached = waitForStatus(volumeId, Volume.Status.IN_USE, 2);
            } catch (TimeoutException e) {
                log.error("[Volume ID] " + volumeId + "did not become IN_USE within expected timeout");
            }
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

    private String removeRegionPrefix(String instanceId) {
        String instaneIdDelimeter = "/";
        if(instanceId.contains(instaneIdDelimeter)) {
            return instanceId.split(instaneIdDelimeter)[1];
        }else{
            return instanceId;
        }
    }

    @Override
	public void detachVolume(String instanceId, String volumeId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);


        //NovaApi novaApi = context.unwrapApi(NovaApi.class);
        //VolumeApi api = novaApi.getVolumeExtensionForZone(region).get();
		
		if(region == null) {
			log.fatal(String.format("Cannot detach the volume [id]: %s from the instance [id]: %s. Extracted region is null for Iaas : %s", volumeId, instanceId, iaasInfo));
			return;
		}
        if(log.isDebugEnabled()) {
            log.debug(String.format("Starting to detach volume %s from the instance %s", volumeId, instanceId));
        }

        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        VolumeAttachmentApi attachmentApiapi = novaApi.getVolumeAttachmentExtensionForZone(region).get();
        VolumeApi volumeApi = novaApi.getVolumeExtensionForZone(region).get();

        if (attachmentApiapi.detachVolumeFromServer(volumeId, removeRegionPrefix(instanceId))) {
        	log.info(String.format("Detachment of Volume [id]: %s from instance [id]: %s was successful. [region] : %s of Iaas : %s", volumeId, instanceId, region, iaasInfo));
        }else{
            log.error(String.format("Detachment of Volume [id]: %s from instance [id]: %s was unsuccessful. [region] : %s [volume Status] : %s", volumeId, instanceId, region, getVolumeStatus(volumeApi, volumeId)));
        }
	}

	@Override
	public void deleteVolume(String volumeId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
        NovaApi novaApi = context.unwrapApi(NovaApi.class);
        VolumeApi api = novaApi.getVolumeExtensionForZone(region).get();


		if(region == null) {
			log.fatal(String.format("Cannot delete the volume [id]: %s. Extracted region is null for Iaas : %s", volumeId, iaasInfo));
			return;
		}

        Volume volume = api.get(volumeId);
        if(volume == null){
            log.warn(String.format("Could not remove volume [id] %s since volume does not exist" , volumeId));
            return;
        }

        Volume.Status volumeStatus = volume.getStatus();

        if(volumeStatus == Volume.Status.IN_USE){
            try {
                waitForStatus(volumeId, Volume.Status.AVAILABLE, 2);
            } catch (TimeoutException e) {
                //Timeout Exception is occurred if the instance did not become available withing expected time period.
                //Hence volume will not be deleted.

                log.error("[Volume ID] " + volumeId + "did not become AVAILABLE within expected timeout, hence returning without deleting the volume");
                return;
            }
        }

        // Coming here means either AVAILABLE or ERROR
        if (api.delete(volumeId)) {
        	log.info(String.format("Deletion of Volume [id]: %s was successful. [region] : %s of Iaas : %s", volumeId, region, iaasInfo));
        }else{
            log.error(String.format("Deletion of Volume [id]: %s was unsuccessful. [region] : %s of Iaas : %s", volumeId, region, iaasInfo));
        }
	}

    @Override
    public String getIaasDevice(String device) {
        return device;
    }

    private Volume.Status getVolumeStatus(VolumeApi volumeApi, String volumeId){
        Volume volume = volumeApi.get(volumeId);
        if(volume != null) {
            return volume.getStatus();
        }

        return null;
    }
}
