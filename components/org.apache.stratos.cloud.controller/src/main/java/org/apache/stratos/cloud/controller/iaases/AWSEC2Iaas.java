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
import org.jclouds.ec2.domain.AvailabilityZoneInfo;
import org.jclouds.ec2.domain.KeyPair;
import org.jclouds.ec2.domain.PublicIpInstanceIdPair;
import org.jclouds.ec2.features.AvailabilityZoneAndRegionApi;
import org.jclouds.ec2.features.ElasticIPAddressApi;
import org.jclouds.ec2.options.DescribeAvailabilityZonesOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

@SuppressWarnings("deprecation")
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
		IaasProvider iaas = getIaasProvider();
		if (iaas.getComputeService() == null) {
			String msg = "Compute service is null for IaaS provider: "
					+ iaas.getName();
			log.fatal(msg);
			throw new CloudControllerException(msg);
		}

		TemplateBuilder templateBuilder = iaas.getComputeService()
				.templateBuilder();

		// set image id specified
		templateBuilder.imageId(iaas.getImage());

        if(!(iaas instanceof IaasProvider)) {
           templateBuilder.locationId(iaas.getType());
        }

		if (iaas.getProperty(CloudControllerConstants.INSTANCE_TYPE) != null) {
			// set instance type eg: m1.large
			templateBuilder.hardwareId(iaas.getProperty(CloudControllerConstants.INSTANCE_TYPE));
		}

		// build the Template
		Template template = templateBuilder.build();

		// if you wish to auto assign IPs, instance spawning call should be
		// blocking, but if you
		// wish to assign IPs manually, it can be non-blocking.
		// is auto-assign-ip mode or manual-assign-ip mode?
		boolean blockUntilRunning = Boolean.parseBoolean(iaas
				.getProperty(CloudControllerConstants.AUTO_ASSIGN_IP));
		template.getOptions().as(TemplateOptions.class)
				.blockUntilRunning(blockUntilRunning);

		// this is required in order to avoid creation of additional security
		// groups by jclouds.
		template.getOptions().as(TemplateOptions.class)
				.inboundPorts(new int[] {});

		// set EC2 specific options
		if (iaas.getProperty(CloudControllerConstants.SUBNET_ID) != null) {
			template.getOptions().as(AWSEC2TemplateOptions.class)
					.subnetId(iaas.getProperty(CloudControllerConstants.SUBNET_ID));
		}

		if (iaas.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
			template.getOptions().as(AWSEC2TemplateOptions.class)
					.placementGroup(iaas.getProperty(CloudControllerConstants.AVAILABILITY_ZONE));
		}

        // security group names
		if (iaas.getProperty(CloudControllerConstants.SECURITY_GROUPS) != null) {
			template.getOptions()
					.as(AWSEC2TemplateOptions.class)
					.securityGroups(
							iaas.getProperty(CloudControllerConstants.SECURITY_GROUPS).split(
									CloudControllerConstants.ENTRY_SEPARATOR));

		}

        // security group ids
        if (iaas.getProperty(CloudControllerConstants.SECURITY_GROUP_IDS) != null) {
            template.getOptions()
                    .as(AWSEC2TemplateOptions.class)
                    .securityGroupIds(iaas.getProperty(CloudControllerConstants.SECURITY_GROUP_IDS)
                                        .split(CloudControllerConstants.ENTRY_SEPARATOR));

        }


		if (iaas.getProperty(CloudControllerConstants.KEY_PAIR) != null) {
			template.getOptions().as(AWSEC2TemplateOptions.class)
					.keyPair(iaas.getProperty(CloudControllerConstants.KEY_PAIR));
		}

		// set Template
		iaas.setTemplate(template);
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


}
