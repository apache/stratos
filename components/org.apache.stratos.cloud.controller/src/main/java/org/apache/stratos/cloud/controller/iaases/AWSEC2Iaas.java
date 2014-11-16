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
import org.apache.stratos.cloud.controller.validate.AWSEC2PartitionValidator;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.jclouds.aws.ec2.AWSEC2Api;
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions;
import org.jclouds.aws.ec2.features.AWSKeyPairApi;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;
import org.jclouds.ec2.domain.Attachment;
import org.jclouds.ec2.domain.AvailabilityZoneInfo;
import org.jclouds.ec2.domain.KeyPair;
import org.jclouds.ec2.domain.PublicIpInstanceIdPair;
import org.jclouds.ec2.domain.Volume;
import org.jclouds.ec2.features.AvailabilityZoneAndRegionApi;
import org.jclouds.ec2.features.ElasticBlockStoreApi;
import org.jclouds.ec2.features.ElasticIPAddressApi;
import org.jclouds.ec2.options.DescribeAvailabilityZonesOptions;
import org.jclouds.ec2.options.DetachVolumeOptions;

import java.util.*;

public class AWSEC2Iaas extends Iaas {

	public AWSEC2Iaas(IaasProvider iaasProvider) {
		super(iaasProvider);
	}

	private static final Log log = LogFactory.getLog(AWSEC2Iaas.class);
	private static final String SUCCESSFUL_LOG_LINE = "A key-pair is created successfully in ";
	private static final String FAILED_LOG_LINE = "Key-pair is unable to create in ";

	@Override
	public void buildComputeServiceAndTemplate() {

		// builds and sets Compute Service
		ComputeServiceBuilderUtil.buildDefaultComputeService(getIaasProvider());
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

        if(!(iaasInfo instanceof IaasProvider)) {
           templateBuilder.locationId(iaasInfo.getType());
        }

        if(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
            Set<? extends Location> locations = iaasInfo.getComputeService().listAssignableLocations();
            for(Location location : locations) {
                if(location.getScope().toString().equalsIgnoreCase(CloudControllerConstants.ZONE_ELEMENT) &&
                        location.getId().equals(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE))) {
                    templateBuilder.locationId(location.getId());
                    log.info("ZONE has been set as " + iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE)
                            + " with id: " + location.getId());
                    break;
                }
            }
        }

		if (iaasInfo.getProperty(CloudControllerConstants.INSTANCE_TYPE) != null) {
			// set instance type eg: m1.large
			templateBuilder.hardwareId(iaasInfo.getProperty(CloudControllerConstants.INSTANCE_TYPE));
		}

		// build the Template
		Template template = templateBuilder.build();

        if(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
            if(!template.getLocation().getId().equals(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE))) {
                log.warn("couldn't find assignable ZONE of id :" +
                        iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) + " in the IaaS. " +
                        "Hence using the default location as " + template.getLocation().getScope().toString() +
                        " with the id " + template.getLocation().getId());
            }
        }

		// if you wish to auto assign IPs, instance spawning call should be
		// blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode?
		boolean blockUntilRunning = Boolean.parseBoolean(iaasInfo
				.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP));
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(new int[] {});

		// set EC2 specific options


        if (iaasInfo.getProperty(CloudControllerConstants.ASSOCIATE_PUBLIC_IP_ADDRESS) != null) {
              boolean associatePublicIp =  Boolean.parseBoolean(iaasInfo.getProperty(
                      CloudControllerConstants.ASSOCIATE_PUBLIC_IP_ADDRESS));
            if(associatePublicIp){
                  template.getOptions().as(AWSEC2TemplateOptions.class)
                      .associatePublicIpAddress();
              }
        }

		if (iaasInfo.getProperty(CloudControllerConstants.SUBNET_ID) != null) {
			template.getOptions().as(AWSEC2TemplateOptions.class)
					.subnetId(iaasInfo.getProperty(CloudControllerConstants.SUBNET_ID));
		}

		if (iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
			template.getOptions().as(AWSEC2TemplateOptions.class)
					.placementGroup(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE));
		}

        // security group names
		if (iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUPS) != null) {
			template.getOptions()
					.as(AWSEC2TemplateOptions.class)
					.securityGroups(iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUPS).split(
                            CloudControllerConstants.ENTRY_SEPARATOR));

		}

        // ability to define tags
        if (iaasInfo.getProperty(CloudControllerConstants.TAGS) != null) {
            template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .tags(Arrays.asList(iaasInfo.getProperty(CloudControllerConstants.TAGS)
                                        .split(CloudControllerConstants.ENTRY_SEPARATOR)));

        }

        // ability to define tags with Key-value pairs
        Map<String, String> keyValuePairTagsMap = new HashMap<String, String>();

        for (String propertyKey : iaasInfo.getProperties().keySet()){
            if(propertyKey.startsWith(CloudControllerConstants.TAGS_AS_KEY_VALUE_PAIRS_PREFIX)) {
                keyValuePairTagsMap.put(propertyKey.substring(CloudControllerConstants.TAGS_AS_KEY_VALUE_PAIRS_PREFIX.length()),
                        iaasInfo.getProperties().get(propertyKey));
                template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .userMetadata(keyValuePairTagsMap);
            }

        }
        

        if (iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUP_IDS) != null) {
            template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .securityGroupIds(iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUP_IDS)
                                        .split(CloudControllerConstants.ENTRY_SEPARATOR));

        }


		if (iaasInfo.getProperty(CloudControllerConstants.KEY_PAIR) != null) {
			template.getOptions().as(AWSEC2TemplateOptions.class)
					.keyPair(iaasInfo.getProperty(CloudControllerConstants.KEY_PAIR));
		}



        if (iaasInfo.getNetworkInterfaces() != null) {
            List<String> networks = new ArrayList<String>(iaasInfo.getNetworkInterfaces().length);
            for (NetworkInterface ni:iaasInfo.getNetworkInterfaces()) {
                networks.add(ni.getNetworkUuid());
            }
            template.getOptions().as(AWSEC2TemplateOptions.class).networks(networks);
        }

		// set Template
		iaasInfo.setTemplate(template);
	}

	@Override
	public void setDynamicPayload() {
		IaasProvider iaasInfo = getIaasProvider();
		if (iaasInfo.getTemplate() != null && iaasInfo.getPayload() != null) {

			iaasInfo.getTemplate().getOptions().as(AWSEC2TemplateOptions.class)
					.userData(iaasInfo.getPayload());
		}

	}

	@Override
	public synchronized boolean createKeyPairFromPublicKey(String region, String keyPairName,
			String publicKey) {
		
		IaasProvider iaasInfo = getIaasProvider();

		String ec2Msg = " ec2. Region: " + region + " - Key Pair Name: ";

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		AWSKeyPairApi keyPairApi = context.unwrapApi(AWSEC2Api.class).getKeyPairApiForRegion(region).get();

		KeyPair keyPair = keyPairApi.importKeyPairInRegion(region, keyPairName, publicKey);
		
		if (keyPair != null) {

			iaasInfo.getTemplate().getOptions().as(AWSEC2TemplateOptions.class)
					.keyPair(keyPair.getKeyName());

			log.info(SUCCESSFUL_LOG_LINE + ec2Msg + keyPair.getKeyName());
			return true;
		}

		log.error(FAILED_LOG_LINE + ec2Msg);

		return false;
	}

	@Override
	public synchronized String associateAddress(NodeMetadata node) {

		IaasProvider iaasInfo = getIaasProvider();
		
		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		ElasticIPAddressApi elasticIPAddressApi = context.unwrapApi(AWSEC2Api.class).getElasticIPAddressApi().get();
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		
		String ip = null;

		// first try to find an unassigned IP.
		ArrayList<PublicIpInstanceIdPair> unassignedIps = Lists
				.newArrayList(Iterables.filter(elasticIPAddressApi.describeAddressesInRegion(region, new String[0]),
						new Predicate<PublicIpInstanceIdPair>() {

							@Override
							public boolean apply(PublicIpInstanceIdPair arg0) {
								return arg0.getInstanceId() == null;
							}

						}));

		if (!unassignedIps.isEmpty()) {
			// try to prevent multiple parallel launches from choosing the same
			// ip.
			Collections.shuffle(unassignedIps);
			ip = Iterables.getLast(unassignedIps).getPublicIp();
		}

		// if no unassigned IP is available, we'll try to allocate an IP.
		if (ip == null || ip.isEmpty()) {
			try {
				ip = elasticIPAddressApi
						.allocateAddressInRegion(region);
				log.info("Allocated ip [" + ip + "]");

			} catch (Exception e) {
				String msg = "Failed to allocate an IP address. All IP addresses are in use.";
				log.error(msg, e);
				throw new CloudControllerException(msg, e);
			}
		}

		String id = node.getProviderId();

		// wait till the fixed IP address gets assigned - this is needed before
		// we associate a
		// public IP

		while (node.getPrivateAddresses() == null) {
			CloudControllerUtil.sleep(1000);
		}

		int retries = 0;
		while (retries < 12 && !associatePublicIp(elasticIPAddressApi, region, ip, id)) {

			// wait for 5s
			CloudControllerUtil.sleep(5000);
			retries++;
		}

		log.debug("Successfully associated an IP address " + ip
				+ " for node with id: " + node.getId());

		return ip;

	}
	
	@Override
	public String associatePredefinedAddress(NodeMetadata node, String ip) {
    	return "";
    }

	/**
	 * @param addressApi
	 * @param region
	 * @param ip
	 * @param id
	 */
	private boolean associatePublicIp(ElasticIPAddressApi addressApi, String region,
			String ip, String id) {
		try {
			addressApi.associateAddressInRegion(
					region, ip, id);
			log.info("Successfully associated public IP ");
			return true;
		} catch (Exception e) {
			log.debug("Exception in associating public IP " + e.getMessage());
			return false;
		}
	}

	@Override
	public synchronized void releaseAddress(String ip) {

		IaasProvider iaasInfo = getIaasProvider();
		
		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		ElasticIPAddressApi elasticIPAddressApi = context.unwrapApi(AWSEC2Api.class).getElasticIPAddressApi().get();
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);

		elasticIPAddressApi.disassociateAddressInRegion(
				region, ip);
		elasticIPAddressApi.releaseAddressInRegion(region,
				ip);
	}

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
    	
    	IaasProvider iaasInfo = getIaasProvider();
    	
        if (region == null || iaasInfo == null) {
            String msg =
                         "Region or IaaSProvider is null: region: " + region + " - IaaSProvider: " +
                                 iaasInfo;
            log.error(msg);
            throw new InvalidRegionException(msg);
        }
        
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        Set<String> regions = context.unwrapApi(AWSEC2Api.class).getConfiguredRegions();
        for (String configuredRegion : regions) {
            if (region.equalsIgnoreCase(configuredRegion)) {
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
    	
        if (zone == null || iaasInfo == null) {
            String msg =
                         "Zone or IaaSProvider is null: zone: " + zone + " - IaaSProvider: " +
                                 iaasInfo;
            log.error(msg);
            throw new InvalidZoneException(msg);
        }
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        AvailabilityZoneAndRegionApi zoneRegionApi = context.unwrapApi(AWSEC2Api.class).
        		getAvailabilityZoneAndRegionApiForRegion(region).get();
        
        Set<AvailabilityZoneInfo> availabilityZones =
                                                      zoneRegionApi.describeAvailabilityZonesInRegion(region,
                                                              new DescribeAvailabilityZonesOptions[0]);
        for (AvailabilityZoneInfo zoneInfo : availabilityZones) {
            String configuredZone = zoneInfo.getZone();
            if (zone.equalsIgnoreCase(configuredZone)) {
                if (log.isDebugEnabled()) {
                    log.debug("Found a matching zone: " + zone);
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
    	
        // there's no such concept in EC2
        String msg = "Invalid host: " + host +" in the zone: "+zone+ " and of the iaas: "+iaasInfo.getType();
        log.error(msg);
        throw new InvalidHostException(msg);
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new AWSEC2PartitionValidator();
    }

	@Override
	public String createVolume(int sizeGB, String snapshotId) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		String zone = ComputeServiceBuilderUtil.extractZone(iaasInfo);
		
		if(region == null || zone == null) {
			log.fatal("Cannot create a new volume in the [region] : "+region
					+", [zone] : "+zone+" of Iaas : "+iaasInfo);
			return null;
		}
		
		ElasticBlockStoreApi blockStoreApi = context.unwrapApi(AWSEC2Api.class).getElasticBlockStoreApiForRegion(region).get();
		
		Volume volume;
		if(StringUtils.isEmpty(snapshotId)){
			if(log.isDebugEnabled()){
        		log.info("Creating a volume in the zone " + zone);
        	}
			volume = blockStoreApi.createVolumeInAvailabilityZone(zone, sizeGB);
		}else{
			if(log.isDebugEnabled()){
        		log.info("Creating a volume in the zone " + zone + " from the shanpshot " + snapshotId);
        	}
			volume = blockStoreApi.createVolumeFromSnapshotInAvailabilityZone(zone, snapshotId);
		}
		 
		
		if (volume == null) {
			log.fatal("Volume creation was unsuccessful. [region] : " + region
					+ ", [zone] : " + zone + " of Iaas : " + iaasInfo);
			return null;
		}
		
		log.info("Successfully created a new volume [id]: "+volume.getId()
				+" in [region] : "+region+", [zone] : "+zone+" of Iaas : "+iaasInfo);
		return volume.getId();
	}

	@Override
	public String attachVolume(String instanceId, String volumeId, String deviceName) {
		IaasProvider iaasInfo = getIaasProvider();

		ComputeServiceContext context = iaasInfo.getComputeService()
				.getContext();
		
		String region = ComputeServiceBuilderUtil.extractRegion(iaasInfo);
		String zone = ComputeServiceBuilderUtil.extractZone(iaasInfo);
		String device = deviceName == null ? "/dev/sdh" : deviceName;
		
		if(region == null || zone == null) {
			log.fatal("Cannot attach the volume [id]: "+volumeId+" in the [region] : "+region
					+", [zone] : "+zone+" of Iaas : "+iaasInfo);
			return null;
		}
		
		ElasticBlockStoreApi blockStoreApi = context.unwrapApi(AWSEC2Api.class).getElasticBlockStoreApiForRegion(region).get();

        Volume.Status volumeStatus = this.getVolumeStatus(blockStoreApi, region, volumeId);

        if(log.isDebugEnabled()){
            log.debug("Volume " + volumeId + " is in state " + volumeStatus);
        }

        while(volumeStatus != Volume.Status.AVAILABLE){
            try {
                // TODO Use a proper mechanism to wait till volume becomes available.
                Thread.sleep(1000);
                volumeStatus = this.getVolumeStatus(blockStoreApi, region, volumeId);
                if(log.isDebugEnabled()){
                    log.debug("Volume " + volumeId + " is still NOT in AVAILABLE. Current State=" + volumeStatus);
                }
            } catch (InterruptedException e) {
                // Ignoring the exception
            }
        }
        if(log.isDebugEnabled()){
            log.debug("Volume " + volumeId + " became  AVAILABLE");
        }

		Attachment attachment = blockStoreApi.attachVolumeInRegion(region, volumeId, instanceId, device);

		if (attachment == null) {
			log.fatal("Volume [id]: "+volumeId+" attachment for instance [id]: "+instanceId
					+" was unsuccessful. [region] : " + region
					+ ", [zone] : " + zone + " of Iaas : " + iaasInfo);
			return null;
		}
		
		log.info("Volume [id]: "+volumeId+" attachment for instance [id]: "+instanceId
				+" was successful [status]: "+attachment.getStatus().value()+". [region] : " + region
				+ ", [zone] : " + zone + " of Iaas : " + iaasInfo);
		return attachment.getStatus().value();
	}

    private Volume.Status getVolumeStatus(ElasticBlockStoreApi blockStoreApi, String region, String volumeId){
        Set<Volume> volumeDescriptions = blockStoreApi.describeVolumesInRegion(region, volumeId);
        Iterator<Volume> it = volumeDescriptions.iterator();
        return it.next().getStatus();
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

		ElasticBlockStoreApi blockStoreApi = context.unwrapApi(AWSEC2Api.class).getElasticBlockStoreApiForRegion(region).get();

        Set<Volume> volumeDescriptions = blockStoreApi.describeVolumesInRegion(region, volumeId);
        Iterator<Volume> it = volumeDescriptions.iterator();

        while(it.hasNext()){
            Volume.Status status  = it.next().getStatus();

            if(status == Volume.Status.AVAILABLE){
                log.warn(String.format("Volume %s is already in AVAILABLE state. Volume seems to be detached somehow", volumeId));
                return;
            }
        }

		blockStoreApi.detachVolumeInRegion(region, volumeId, true, DetachVolumeOptions.Builder.fromInstance(instanceId));

		log.info("Detachment of Volume [id]: "+volumeId+" from instance [id]: "+instanceId
				+" was successful. [region] : " + region
				+ " of Iaas : " + iaasInfo);
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
		
		ElasticBlockStoreApi blockStoreApi = context.unwrapApi(AWSEC2Api.class).getElasticBlockStoreApiForRegion(region).get();
		blockStoreApi.deleteVolumeInRegion(region, volumeId);
		
		log.info("Deletion of Volume [id]: "+volumeId+" was successful. [region] : " + region
				+ " of Iaas : " + iaasInfo);
	}

    @Override
    /*
        Converts the user defined volume device to Ec2 specific device.
        For example /dev/sdf is converted to /dev/xvdf
     */
    public String getIaasDevice(String device) {
        String[] split = device.split("/");
        String x = split[split.length-1];
        StringBuilder ec2Device = new StringBuilder();
        ec2Device.append("/" + split[1]);
        ec2Device.append("/xvd");
        ec2Device.append(x.charAt(x.length()-1));
        return  ec2Device.toString();
    }


}
