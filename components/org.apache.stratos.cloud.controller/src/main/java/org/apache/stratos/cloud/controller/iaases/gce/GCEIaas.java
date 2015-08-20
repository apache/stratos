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
package org.apache.stratos.cloud.controller.iaases.gce;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.Atomics;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
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
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.ComputeServiceBuilderUtil;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.Location;
import org.jclouds.googlecomputeengine.GoogleComputeEngineApi;
import org.jclouds.googlecomputeengine.compute.options.GoogleComputeEngineTemplateOptions;
import org.jclouds.googlecomputeengine.domain.AttachDisk;
import org.jclouds.googlecomputeengine.domain.Disk;
import org.jclouds.googlecomputeengine.domain.Instance;
import org.jclouds.googlecomputeengine.domain.Operation;
import org.jclouds.googlecomputeengine.features.DiskApi;
import org.jclouds.googlecomputeengine.features.InstanceApi;
import org.jclouds.googlecomputeengine.options.DiskCreationOptions;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.jclouds.util.Predicates2.retry;

public class GCEIaas extends JcloudsIaas {
    private static final Log log = LogFactory.getLog(GCEIaas.class);
    public static final int MAX_WAIT_TIME = 60; // seconds

    public GCEIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
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
        IaasProvider iaasInfo = getIaasProvider();

        if (iaasInfo.getComputeService() == null) {
            String msg = "Compute service is null for IaaS provider: "
                    + iaasInfo.getName();
            log.fatal(msg);
            throw new CloudControllerException(msg);
        }

        if (log.isDebugEnabled()) {
            log.debug("Building template for Google Compute Engine IaaS");
        }
        TemplateBuilder templateBuilder = iaasInfo.getComputeService().templateBuilder();

        // set image id specified
        templateBuilder.imageId(iaasInfo.getImage());

        String zone = iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE);
        if (zone != null) {
            Set<? extends Location> locations = iaasInfo.getComputeService().listAssignableLocations();
            for (Location location : locations) {
                if (location.getScope().toString().equalsIgnoreCase(CloudControllerConstants.ZONE_ELEMENT) &&
                        location.getId().equals(zone)) {
                    templateBuilder.locationId(location.getId());
                    log.info("zone has been set as " + zone
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

        if (zone != null) {
            if (!template.getLocation().getId().equals(zone)) {
                log.warn("couldn't find assignable zone of id :" + zone +
                        " in the IaaS. Hence using the default location as " +
                        template.getLocation().getScope().toString() +
                        " with the id " + template.getLocation().getId());
            }
        }

        // if you wish to auto assign IPs, instance spawning call should be
        // blocking, but if you
        // wish to assign IPs manually, it can be non-blocking.
        // is auto-assign-ip mode or manual-assign-ip mode? - default mode is
        // non-blocking
        boolean blockUntilRunning = Boolean.parseBoolean(iaasInfo.getProperty("autoAssignIp"));
        template.getOptions().as(GoogleComputeEngineTemplateOptions.class).blockUntilRunning(blockUntilRunning);

        // this is required in order to avoid creation of additional security groups by Jclouds.
        template.getOptions().as(GoogleComputeEngineTemplateOptions.class).inboundPorts(22, 80, 8080, 443, 8243);

        if (zone != null) {
            templateBuilder.locationId(zone);
            log.debug("setting location to " + zone);
        }

        // ability to define tags with Key-value pairs
        Map<String, String> keyValuePairTagsMap = new HashMap<String, String>();

        for (String propertyKey : iaasInfo.getProperties().keySet()) {
            if (propertyKey.startsWith(CloudControllerConstants.TAGS_AS_KEY_VALUE_PAIRS_PREFIX)) {
                keyValuePairTagsMap
                        .put(propertyKey.substring(CloudControllerConstants.TAGS_AS_KEY_VALUE_PAIRS_PREFIX.length()),
                                iaasInfo.getProperties().get(propertyKey));
                template.getOptions()
                        .userMetadata(keyValuePairTagsMap);
            }
            log.info("usermeta data key:" + propertyKey + " value: " + iaasInfo.getProperties().get(propertyKey));
        }

        if (iaasInfo.getNetworkInterfaces() != null) {
            List<String> networks = new ArrayList<String>(iaasInfo.getNetworkInterfaces().length);
            for (NetworkInterface ni : iaasInfo.getNetworkInterfaces()) {
                networks.add(ni.getNetworkUuid());
                log.info("using network interface " + ni.getNetworkUuid());
            }
            template.getOptions().as(GoogleComputeEngineTemplateOptions.class).networks(networks);
            log.info("using network interface " + networks);
        }

        // set Template
        iaasInfo.setTemplate(template);
    }

    @Override
    public void setDynamicPayload(byte[] payload) {
        // in vCloud case we need to run a script
        IaasProvider iaasInfo = getIaasProvider();

        if (iaasInfo.getTemplate() == null || payload == null) {
            if (log.isDebugEnabled()) {
                log.debug("Template or payload for GCE not found");
            }
            return;
        }

        // Payload is a String value
        String payloadStr = new String(payload);

        log.info("setDynamicPayload " + payloadStr);

        Map<String, String> keyValuePairTagsMap = new HashMap<String, String>();
        keyValuePairTagsMap.put("stratos_usermetadata", payloadStr);
        iaasInfo.getTemplate().getOptions().userMetadata(keyValuePairTagsMap);
    }

    @Override
    public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {

        // Not applicable for GCE - Not called by stratos cloud controller as well
        return false;
    }

    @Override
    public List<String> associateAddresses(NodeMetadata node) {

        // TODO
        return null;

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
        if (api.regions().get(region) != null) {
            return true;
        }
        String msg = "Invalid region: " + region + " in the iaas: " + iaasInfo.getType();
        log.error(msg);
        throw new InvalidRegionException(msg);
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException {
        IaasProvider iaasInfo = getIaasProvider();

        if (zone == null || iaasInfo == null) {
            String msg = "Zone or IaaSProvider is null. [region] " + region + ", [zone] " + zone + ", [IaaSProvider] "
                    + iaasInfo;
            log.error(msg);
            throw new InvalidZoneException(msg);
        }

        GoogleComputeEngineApi api = getGCEApi();
        if (api.zones().get(zone) != null) {
            return true;
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
        log.info("Creating volume: " + diskName + " in zone: " + zone + " of size: " + sizeGB);
        try {
            DiskCreationOptions diskCreationOptions = new DiskCreationOptions.Builder().sizeGb(sizeGB).sourceSnapshot
                    (new URI(snapshotId)).build();
            Operation oper = diskApi.create(diskName, diskCreationOptions);
            oper = waitGCEOperationDone(oper);
            if (!oper.status().equals(Operation.Status.DONE)) {
                log.error("Failed to create volume: " + diskName + " of size: " + sizeGB +
                        " in zone: " + zone + " operation: " + oper);
                return null;
            }
            return diskName;
        }
        catch (Exception e) {
            log.error("Error creating volume", e);
        }
        return null;
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        DiskApi diskApi = getGCEDiskApi();
        InstanceApi instApi = getGCEInstanceApi();
        String zone = getZone();

        log.info("Trying to attach volume: " + volumeId + " to instance: " + instanceId +
                " in zone: " + zone + " at devicename: " + deviceName);

        Disk disk = diskApi.get(volumeId);
        if (disk == null) {
            log.error("Failed to get volume: " + volumeId + " in zone: " + zone);
            return null;
        }
        log.debug("Found volumeId: " + volumeId + " volume: " + disk);
        try {
            Operation oper =
                    instApi.attachDisk(instanceId, AttachDisk.create(AttachDisk.Type.PERSISTENT, AttachDisk.Mode
                            .READ_WRITE, disk.selfLink(), deviceName, true, null, false, null, null));
            oper = waitGCEOperationDone(oper);
            if (!oper.status().equals(Operation.Status.DONE)) {
                log.error("Failed to attach volume: " + volumeId + " to instance: " + instanceId +
                        " in zone: " + zone + " at device: " + deviceName + " operation: " + oper);
                return null;
            }
            return volumeId;
        }
        catch (Exception e) {
            log.error("Error attaching volume", e);
        }
        return null;
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {
        InstanceApi instApi = getGCEInstanceApi();
        String zone = getZone();
        Instance inst = instApi.get(instanceId);
        log.info("Trying to detach volume: " + volumeId + " from instance: " + instanceId + " in zone: " + zone);
        if (inst == null) {
            log.error("Failed to find instance: " + instanceId + " in zone: " + zone);
            return;
        }
        for (Instance.AttachedDisk disk : inst.disks()) {
            if (disk.deviceName().equals(volumeId)) {
                log.info("Found disk to be detached. Source: " + disk.source() + " devicename: " + disk.deviceName());
                Operation oper = instApi.detachDisk(instanceId, disk.deviceName());
                oper = waitGCEOperationDone(oper);
                if (!oper.status().equals(Operation.Status.DONE)) {
                    log.error("Failed to detach volume: " + volumeId + " to instance: " + instanceId +
                            " in zone: " + zone + " at device: " + disk.deviceName() + " result operation: " + oper);
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
        log.info("Deleting volume: " + volumeId + " in zone: " + zone);
        Operation oper = diskApi.delete(volumeId);

        oper = waitGCEOperationDone(oper);
        if (!oper.status().equals(Operation.Status.DONE)) {
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
        return getGCEApi().disksInZone(getZone());
    }

    private InstanceApi getGCEInstanceApi() {
        return getGCEApi().instancesInZone(getZone());
    }

    private Operation waitGCEOperationDone(Operation operation) {
        IaasProvider iaasInfo = getIaasProvider();
        Injector injector = ContextBuilder.newBuilder(iaasInfo.getProvider())
                .credentials(iaasInfo.getIdentity(), iaasInfo.getCredential())
                .buildInjector();
        Predicate<AtomicReference<Operation>> zoneOperationDonePredicate =
                injector.getInstance(Key.get(new TypeLiteral<Predicate<AtomicReference<Operation>>>() {
                }, Names.named("zone")));
        AtomicReference<Operation> operationReference = Atomics.newReference(operation);
        retry(zoneOperationDonePredicate, MAX_WAIT_TIME, 1, SECONDS).apply(operationReference);

        return operationReference.get();
    }
}