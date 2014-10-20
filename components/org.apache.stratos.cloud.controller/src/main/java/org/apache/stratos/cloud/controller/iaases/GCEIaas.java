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

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.jcloud.ComputeServiceBuilderUtil;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.pojo.NetworkInterface;
import org.apache.stratos.cloud.controller.validate.GCEPartitionValidator;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.jclouds.collect.IterableWithMarker;
import org.jclouds.googlecomputeengine.GoogleComputeEngineApi;
import org.jclouds.googlecomputeengine.features.DiskApi;
import org.jclouds.googlecomputeengine.features.InstanceApi;
import org.jclouds.googlecomputeengine.features.RegionApi;
import org.jclouds.googlecomputeengine.features.ZoneApi;
import org.jclouds.googlecomputeengine.domain.Disk;
import org.jclouds.googlecomputeengine.domain.Instance;
import org.jclouds.googlecomputeengine.domain.Region;
import org.jclouds.googlecomputeengine.domain.Zone;
import org.jclouds.googlecomputeengine.domain.Operation;
import org.jclouds.googlecomputeengine.options.AttachDiskOptions;
import org.jclouds.googlecomputeengine.options.AttachDiskOptions.DiskType;
import com.google.inject.Key;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import static org.jclouds.util.Predicates2.retry;
import static java.util.concurrent.TimeUnit.SECONDS;
import com.google.common.base.Predicate;
import java.util.concurrent.atomic.AtomicReference;
import com.google.common.util.concurrent.Atomics;

public class GCEIaas extends Iaas {


	private static final Log log = LogFactory.getLog(GCEIaas.class);
	
	private static final String PROJECTNAME = "projectName";
	
	public GCEIaas(IaasProvider iaasProvider) {
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

		log.info("gce buildTemplate");

		TemplateBuilder templateBuilder = iaasInfo.getComputeService()
				.templateBuilder();

		// set image id specified
		templateBuilder.imageId(iaasInfo.getImage());

		String zone = iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE);
		if(zone != null) {
			Set<? extends Location> locations = iaasInfo.getComputeService().listAssignableLocations();
			for(Location location : locations) {
				if(location.getScope().toString().equalsIgnoreCase(CloudControllerConstants.ZONE_ELEMENT) &&
					location.getId().equals(zone)) {
					templateBuilder.locationId(location.getId());
					log.info("ZONE has been set as " + zone
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

		if(zone != null) {
			if(!template.getLocation().getId().equals(zone)) {
				log.warn("couldn't find assignable ZONE of id :" + zone +
				" in the IaaS. Hence using the default location as " + template.getLocation().getScope().toString() +
				" with the id " + template.getLocation().getId());
			}
		}

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

		if (zone != null) {
			templateBuilder.locationId(zone);
			log.debug("setting location to " + zone);
		}

		// ability to define tags with Key-value pairs
		Map<String, String> keyValuePairTagsMap = new HashMap<String, String>();

		for (String propertyKey : iaasInfo.getProperties().keySet()){
			if(propertyKey.startsWith(CloudControllerConstants.TAGS_AS_KEY_VALUE_PAIRS_PREFIX)) {
				keyValuePairTagsMap.put(propertyKey.substring(CloudControllerConstants.TAGS_AS_KEY_VALUE_PAIRS_PREFIX.length()),
					iaasInfo.getProperties().get(propertyKey));
				template.getOptions()
				    .userMetadata(keyValuePairTagsMap);
			}
			log.info("usermeta data key:"+ propertyKey + " value: " + iaasInfo.getProperties().get(propertyKey));
		}

		if (iaasInfo.getNetworkInterfaces() != null) {
			List<String> networks = new ArrayList<String>(iaasInfo.getNetworkInterfaces().length);
			for (NetworkInterface ni:iaasInfo.getNetworkInterfaces()) {
				networks.add(ni.getNetworkUuid());
				log.info("using network interface " + ni.getNetworkUuid());
			}
			template.getOptions().as(TemplateOptions.class).networks(networks);
			log.info("using network interface " + networks);
		}

		// set Template
		iaasInfo.setTemplate(template);
	}

	@Override
	public void setDynamicPayload() {
		// in vCloud case we need to run a script
		IaasProvider iaasInfo = getIaasProvider();

		if (iaasInfo.getTemplate() == null || iaasInfo.getPayload() == null) {
			if (log.isDebugEnabled()) {
				log.debug("Payload for GCE not found");
			}
			return;
		}

		// Payload is a String value
		String payload = new String(iaasInfo.getPayload());

		log.info("setDynamicPayload " + payload);

		Map<String, String> keyValuePairTagsMap = new HashMap<String, String>();
		keyValuePairTagsMap.put("stratos_usermetadata", payload);
		iaasInfo.getTemplate().getOptions().userMetadata(keyValuePairTagsMap);
	}

	@Override
	public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {

		// Not applicable for GCE - Not called by stratos cloud controller as well
		return false;
	}

	@Override
	public String associateAddress(NodeMetadata node) {

		// TODO
		return "";

	}

	@Override
	public String associatePredefinedAddress(NodeMetadata node, String ip) {
		return "";
	}

	@Override
	public void releaseAddress(String ip) {
		// TODO
	}

	@Override
	public boolean isValidRegion(String region) throws InvalidRegionException {
		IaasProvider iaasInfo = getIaasProvider();

		if (region == null || iaasInfo == null) {
			String msg = "Region or IaaSProvider is null: region: " + region + " - IaaSProvider: " + iaasInfo;
			log.error(msg);
			throw new InvalidRegionException(msg);
		}

		GoogleComputeEngineApi api = getGCEApi();
		RegionApi regionApi = api.getRegionApiForProject(iaasInfo.getProperty(PROJECTNAME));

		for(IterableWithMarker<Region> page : regionApi.list()) {
			for(Region r : page) {
				if (region.equalsIgnoreCase(r.getName())) {
					log.debug("Found a matching region: " + region);
					return true;
				}
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
			String msg = "Zone or IaaSProvider is null: region: " + region + 
				     " zone: " + zone + " - IaaSProvider: " + iaasInfo;
			log.error(msg);
			throw new InvalidZoneException(msg);
		}

		GoogleComputeEngineApi api = getGCEApi();
		ZoneApi zoneApi = api.getZoneApiForProject(iaasInfo.getProperty(PROJECTNAME));

		for(IterableWithMarker<Zone> page : zoneApi.list()) {
			for(Zone z : page) {
				if (zone.equalsIgnoreCase(z.getName())) {
					log.debug("Found a matching zone: " + zone);
					return true;
				}
			}
		}

		String msg = "Invalid zone: " + zone + " in the region: " + region + " and of the iaas: " + iaasInfo.getType();
		log.error(msg);
		throw new InvalidZoneException(msg);
	}

	@Override
	public boolean isValidHost(String zone, String host) throws InvalidHostException {
		IaasProvider iaasInfo = getIaasProvider();

		// Not called by cloud controller
		// there's no such concept in GCE

		String msg = "Invalid host: " + host + " in the zone: " + zone + " and of the iaas: " + iaasInfo.getType();
		log.error(msg);
		throw new InvalidHostException(msg);
	}

	@Override
	public PartitionValidator getPartitionValidator() {
		return new GCEPartitionValidator();
	}

	@Override
        public String createVolume(int sizeGB, String snapshotId) {
		// generate a random diskname
		Random rand = new Random();
		String diskName = "stratos-disk-" + rand.nextInt(100000);
		DiskApi diskApi = getGCEDiskApi();
		String zone = getZone();

		log.debug("Creating volume: " + diskName + " in zone: " + zone + " of size: " + sizeGB);

		Operation oper = diskApi.createInZone(diskName, sizeGB, zone);

		oper = waitGCEOperationDone(oper);
		if (oper.getStatus() != Operation.Status.DONE) {
			log.error("Failed to create volume: " + diskName + " of size: " + sizeGB +
				  " in zone: " + zone + " operation: " + oper);
			return null;
		}

		return diskName;
	}

	@Override
	public String attachVolume(String instanceId, String volumeId, String deviceName) {
		DiskApi diskApi = getGCEDiskApi();
		InstanceApi instApi = getGCEInstanceApi();
		String zone = getZone();

		log.debug("Trying to attach volume: " + volumeId + " to instance: " + instanceId +
			  " in zone: " + zone + " at devicename: " + deviceName);

		Disk disk = diskApi.getInZone(zone, volumeId);
		if (disk == null) {
			log.error("Failed to get volume: " + volumeId + " in zone: " + zone);
			return null;
		}

		log.debug("Found volumeId: " + volumeId + " volume: " + disk);

		Operation oper = instApi.attachDiskInZone(zone, instanceId,
						new AttachDiskOptions().type(DiskType.PERSISTENT)
							.source(disk.getSelfLink())
							.mode(AttachDiskOptions.DiskMode.READ_WRITE)
							.deviceName(deviceName));
		oper = waitGCEOperationDone(oper);
		if (oper.getStatus() != Operation.Status.DONE) {
			log.error("Failed to attach volume: " + volumeId + " to instance: " + instanceId +
				  " in zone: " + zone + " at device: " + deviceName + " operation: " + oper);
			return null;
		}

		return volumeId;
	}

	@Override
	public void detachVolume(String instanceId, String volumeId) {
		InstanceApi instApi = getGCEInstanceApi();
		String zone = getZone();
		Instance inst = instApi.getInZone(zone, instanceId);

		log.debug("Trying to detach volume: " + volumeId + " from instance: " + instanceId +
			  " " + inst + " in zone: " + zone);

		if (inst == null) {
			log.error("Failed to find instance: " + instanceId + " in zone: " + zone);
			return;
		}

		for(Instance.AttachedDisk disk : inst.getDisks()) {
			Instance.PersistentAttachedDisk persistentDisk = (Instance.PersistentAttachedDisk)disk;

			log.debug("Found disk - src: " + persistentDisk.getSourceDiskName() +
				  " devicename: " + persistentDisk.getDeviceName());

			if (persistentDisk.getSourceDiskName().equals(volumeId)) {
				Operation oper = instApi.detachDiskInZone(zone, instanceId, persistentDisk.getDeviceName().get());
				oper = waitGCEOperationDone(oper);
				if (oper.getStatus() != Operation.Status.DONE) {
					log.error("Failed to detach volume: " + volumeId + " to instance: " + instanceId +
						  " in zone: " + zone + " at device: " + persistentDisk.getDeviceName() +
						  " result operation: " + oper);
				}
				return;
			}
		}

		log.error("Cannot find volume: " + volumeId + " in instance: " + instanceId);
	}

	@Override
	public void deleteVolume(String volumeId) {
		DiskApi diskApi = getGCEDiskApi();
		String zone = getZone();

		log.debug("Deleting volume: " + volumeId + " in zone: " + zone);

		Operation oper = diskApi.deleteInZone(zone, volumeId);

		oper = waitGCEOperationDone(oper);
		if (oper.getStatus() != Operation.Status.DONE) {
			log.error("Failed to delete volume: " + volumeId + " in zone: " + zone +
				  " operation: " + oper);
		}
	}

	@Override
	public String getIaasDevice(String device) {
		return device;
	}

	private String getZone() {
                IaasProvider iaasInfo = getIaasProvider();
		return iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE);
	}

	private GoogleComputeEngineApi getGCEApi() {
		IaasProvider iaasInfo = getIaasProvider();
		ComputeServiceContext context = iaasInfo.getComputeService().getContext();
		GoogleComputeEngineApi api = context.unwrapApi(GoogleComputeEngineApi.class);

		return api;
	}

	private DiskApi getGCEDiskApi() {
		IaasProvider iaasInfo = getIaasProvider();
		String projectName = iaasInfo.getProperty(PROJECTNAME);
		return getGCEApi().getDiskApiForProject(projectName);
	}

	private InstanceApi getGCEInstanceApi() {
		IaasProvider iaasInfo = getIaasProvider();
		String projectName = iaasInfo.getProperty(PROJECTNAME);
		return getGCEApi().getInstanceApiForProject(projectName);
	}

	private Operation waitGCEOperationDone(Operation operation) {
		int maxWaitTime = 15; // 15 seconds
                IaasProvider iaasInfo = getIaasProvider();
		Injector injector = ContextBuilder.newBuilder(iaasInfo.getProvider())
					  .credentials(iaasInfo.getIdentity(), iaasInfo.getCredential())
					  .buildInjector();
		Predicate<AtomicReference<Operation>> zoneOperationDonePredicate =
			    injector.getInstance(Key.get(new TypeLiteral<Predicate<AtomicReference<Operation>>>() {
				}, Names.named("zone")));
		AtomicReference<Operation> operationReference = Atomics.newReference(operation);
		retry(zoneOperationDonePredicate, maxWaitTime, 1, SECONDS).apply(operationReference);

		return operationReference.get();
	}
}

