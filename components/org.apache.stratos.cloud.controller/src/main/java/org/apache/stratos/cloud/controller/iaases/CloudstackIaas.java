package org.apache.stratos.cloud.controller.iaases;


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
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.validate.CloudstackPartitionValidator;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.jclouds.cloudstack.CloudStackApi;
import org.jclouds.cloudstack.compute.options.CloudStackTemplateOptions;
import org.jclouds.cloudstack.domain.*;
import org.jclouds.cloudstack.features.VolumeApi;
import org.jclouds.cloudstack.options.ListPublicIPAddressesOptions;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.TemplateOptions;
import org.jclouds.domain.Location;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeoutException;

public class CloudstackIaas extends Iaas {

    private static final Log log = LogFactory.getLog(CloudstackIaas.class);

    public CloudstackIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
    }

    @Override
    public void buildComputeServiceAndTemplate() {
        // builds and sets Compute Service
        ComputeServiceBuilderUtil.buildDefaultComputeService(getIaasProvider());
        // builds and sets Template
        buildTemplate();
    }

    @Override
    public void buildTemplate() {

        IaasProvider iaasInfo = getIaasProvider();

        //if compute service is not available
        if (iaasInfo.getComputeService() == null) {
            String msg = "Compute service is null for IaaS provider: "
                    + iaasInfo.getName();
            log.error(msg);
            throw new CloudControllerException(msg);
        }

        //create templateBuilder
        TemplateBuilder templateBuilder = iaasInfo.getComputeService()
                .templateBuilder();

        //**SET PROPERTIES TO templateBuilder OBJECT**//

        /**
         * PROPERTY - 1
         * set image id specified
         */
        templateBuilder.imageId(iaasInfo.getImage());

        /**
         *  PROPERTY-2
         *  if user has specified a zone in cloud-controller.xml, set the zone into templateBuilder object
         *  (user should provide the zone id for this, because zone name is not unique in cloudstack)
         */
        if (iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE) != null) {
            Set<? extends Location> locations = iaasInfo.getComputeService().listAssignableLocations();
            for (Location location : locations) {
                if (location.getId().equals(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE))) {
                    //if the zone is valid set the zone to templateBuilder Object
                    templateBuilder.locationId(location.getId());
                    log.info("Zone has been set as " + iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE)
                            + " with id: " + location.getId());
                    break;
                }
            }
        }

        /**
         * PROPERTY-3
         * if user has specified an instance type in cloud-controller.xml, set the instance type into templateBuilder
         * object.(service offering)
         *Important:Specify the Service Offering type ID. Not the name. Because the name is not unique in cloudstack.
         */
        if (iaasInfo.getProperty(CloudControllerConstants.INSTANCE_TYPE) != null) {
            templateBuilder.hardwareId(iaasInfo.getProperty(CloudControllerConstants.INSTANCE_TYPE));
        }

        //build the template
        Template template = templateBuilder.build();

        /**if you wish to auto assign IPs, instance spawning call should be
         * blocking, but if you
         * wish to assign IPs manually, it can be non-blocking.
         * is auto-assign-ip mode or manual-assign-ip mode?
         */
        boolean blockUntilRunning = Boolean.parseBoolean(iaasInfo
                .getProperty(CloudControllerConstants.AUTO_ASSIGN_IP));
        template.getOptions().as(TemplateOptions.class)
                .blockUntilRunning(blockUntilRunning);

        // this is required in order to avoid creation of additional security
        // groups by Jclouds.
        template.getOptions().as(TemplateOptions.class)
                .inboundPorts(new int[]{});


        //**SET CLOUDSTACK SPECIFIC PROPERTIES TO TEMPLATE OBJECT**//

        //set security group - If you are using basic zone
        if (iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUP_IDS) != null) {
            template.getOptions()
                    .as(CloudStackTemplateOptions.class)
                    .securityGroupIds(Arrays.asList(iaasInfo.getProperty(CloudControllerConstants.SECURITY_GROUP_IDS)
                            .split(CloudControllerConstants.ENTRY_SEPARATOR)));
        }


        /**
         * set network ID - If you are using advanced zone
         * in cloudstack sometimes we get unautorized exception if we didn't specify the
         * domain ID and user name
         */
        if (iaasInfo.getProperty(CloudControllerConstants.NETWORK_IDS) != null) {
            template.getOptions()
                    .as(CloudStackTemplateOptions.class)
                    .networks(Arrays.asList(iaasInfo.getProperty(CloudControllerConstants.NETWORK_IDS)
                            .split(CloudControllerConstants.ENTRY_SEPARATOR)));
        }

        //set user name
        if (iaasInfo.getProperty(CloudControllerConstants.USER_NAME) != null) {
            template.getOptions().as(CloudStackTemplateOptions.class)
                    .account(iaasInfo.getProperty(CloudControllerConstants.USER_NAME));
        }
        //set domain ID
        if (iaasInfo.getProperty(CloudControllerConstants.DOMAIN_ID) != null) {
            template.getOptions().as(CloudStackTemplateOptions.class)
                    .domainId(iaasInfo.getProperty(CloudControllerConstants.DOMAIN_ID));
        }

        /**
         *Set key pair
         * in cloudstack sometimes we get unauthorized exception if we didn't specify the
         * domain ID and user name
         */
        if (iaasInfo.getProperty(CloudControllerConstants.KEY_PAIR) != null) {
            template.getOptions().as(CloudStackTemplateOptions.class)
                    .keyPair(iaasInfo.getProperty(CloudControllerConstants.KEY_PAIR));
        }

        // ability to define tags
        if (iaasInfo.getProperty(CloudControllerConstants.TAGS) != null) {
            template.getOptions()
                    .as(CloudStackTemplateOptions.class)
                    .tags(Arrays.asList(iaasInfo.getProperty(CloudControllerConstants.TAGS)
                            .split(CloudControllerConstants.ENTRY_SEPARATOR)));
        }
        //set disk offering to the instance
        if (iaasInfo.getProperty(CloudControllerConstants.DISK_OFFERING) != null) {
            template.getOptions()
                    .as(CloudStackTemplateOptions.class)
                    .diskOfferingId(iaasInfo.getProperty(CloudControllerConstants.DISK_OFFERING));
        }

        // set Template
        iaasInfo.setTemplate(template);
    }

    @Override
    public void setDynamicPayload() {
        IaasProvider iaasInfo = getIaasProvider();
        if (iaasInfo.getTemplate() != null && iaasInfo.getPayload() != null) {
            iaasInfo.getTemplate().getOptions().as(CloudStackTemplateOptions.class)
                    .userMetadata(convertByteArrayToHashMap(iaasInfo.getPayload()));
        }
    }

    /**
     * IMPORTANT
     * In cloudstack we can assign public IPs, if we are using an advanced zone only. If we are using a basic zone
     * we cannot assign public ips.
     * <p/>
     * When we use an advanced zone, a public IP address will get automatically assigned to the vm. So we don't need
     * to find an unallocated IP address and assign that address to the vm. If you are using a basic zone you cannot
     * assign public IPs
     * <p/>
     * So  this method will find the IP that has been assigned to the vm and return it.
     */
    @Override
    public String associateAddress(NodeMetadata node) {

        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);
        String ip = null;

        // get all allocated IPs
        ListPublicIPAddressesOptions listPublicIPAddressesOptions = new ListPublicIPAddressesOptions();
        listPublicIPAddressesOptions.zoneId(iaasInfo.getProperty(CloudControllerConstants.AVAILABILITY_ZONE));

        Set<PublicIPAddress> publicIPAddresses = cloudStackApi.getAddressApi()
                .listPublicIPAddresses(listPublicIPAddressesOptions);

        String id = node.getProviderId(); //vm ID

        for (PublicIPAddress publicIPAddress : publicIPAddresses) {
            if (publicIPAddress.getVirtualMachineId().equals(id)) { //check whether this instance has
                // already got an public ip or not
                ip = publicIPAddress.getIPAddress(); //A public ip has been successfully assigned to the vm
                log.info("Successfully associated an IP address " + ip
                        + " for node with id: " + node.getId());
                break;
            }

        }

        if (ip == null || ip.isEmpty()) { //IP has not been successfully assigned to VM(That means there are
            //  no more IPs  available for the VM)
            String msg = "No address associated for node with id: " + node.getId();
            log.warn(msg);
            throw new CloudControllerException(msg);
        }

        return ip;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {
        return "";
    }

    @Override
    public void releaseAddress(String ip) {
        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);
        cloudStackApi.getAddressApi().disassociateIPAddress(ip);
    }

    @Override
    public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {

        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);
        SshKeyPair sshKeyPair = cloudStackApi.getSSHKeyPairApi().createSSHKeyPair(keyPairName);

        if (sshKeyPair != null) {

            iaasInfo.getTemplate().getOptions().as(CloudStackTemplateOptions.class)
                    .keyPair(sshKeyPair.getName());

            log.info("A key-pair is created successfully - Key Pair Name: " + sshKeyPair.getName());
            return true;
        }
        log.error("Key-pair is unable to create");
        return false;
    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {

        IaasProvider iaasInfo = getIaasProvider();
        //no such method in Jclouds cloudstack api
        String msg = "Invalid region: " + region + " in the iaas: " + iaasInfo.getType();
        log.error(msg);
        throw new InvalidRegionException(msg);
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException {

        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);
        ListZonesOptions listZonesOptions = new ListZonesOptions();
        listZonesOptions.available(true);
        Set<Zone> zoneSet = cloudStackApi.getZoneApi().listZones(listZonesOptions);

        for (org.jclouds.cloudstack.domain.Zone configuredZone : zoneSet) {
            if (configuredZone.getName().equalsIgnoreCase(zone)) {
                return true;
            }
        }
        String msg = "Invalid zone: " + zone + " in the iaas: " + iaasInfo.getType();
        log.error(msg);
        throw new InvalidZoneException(msg);
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {

        IaasProvider iaasInfo = getIaasProvider();
        // there's no such method in jclouds cloustack api
        String msg = "Invalid host: " + host + " in the zone: " + zone + " and of the iaas: " + iaasInfo.getType();
        log.error(msg);
        throw new InvalidHostException(msg);

    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new CloudstackPartitionValidator();
    }

    @Override
    public String createVolume(int sizeGB, String snapshotId) {

        //todo return volume ID if volume is created
        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService()
                .getContext();

        String zone = ComputeServiceBuilderUtil.extractZone(iaasInfo);
        String diskOfferingID = iaasInfo.getTemplate().getOptions().as(CloudStackTemplateOptions.class)
                .getDiskOfferingId();
        if (zone == null && diskOfferingID == null) {
            log.error("Could not create a volume in the , [zone] : " + zone + " of Iaas : " + iaasInfo);
            return null;
        }

        VolumeApi volumeApi = context.unwrapApi(CloudStackApi.class).getVolumeApi();

        Volume volume;
        if (StringUtils.isEmpty(snapshotId)) {
            if (log.isInfoEnabled()) {
                log.info("Creating a volume in the zone " + zone);
            }

            //cloudstack jcloud api does not return a volume object
            volumeApi.createVolumeFromCustomDiskOfferingInZone(null, diskOfferingID, zone, sizeGB);

            //  volume = blockStoreApi.createVolumeInAvailabilityZone(zone, sizeGB);
        } else {
            if (log.isInfoEnabled()) {
                log.info("Creating a volume in the zone " + zone + " from the snapshot " + snapshotId);
            }
            volumeApi.createVolumeFromSnapshotInZone(null, diskOfferingID, zone);
        }

        return null;
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService()
                .getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);

        //get volume
        org.jclouds.cloudstack.domain.Volume volume = cloudStackApi.getVolumeApi().getVolume(volumeId);

        //get current volume state
        Volume.State volumeState = volume.getState();

        if (log.isDebugEnabled()) {
            log.debug("Volume " + volumeId + " is in state " + volumeState);
        }

        //if volume is not available, not allocated or cannot use
        //volume state ALLOCATED   means that volume has not been attached to any instance.

        //TODO there is an error with logic.
        if (!(volumeState == Volume.State.ALLOCATED || volumeState == Volume.State.CREATING
                || volumeState == Volume.State.READY)) {
            log.error(String.format("Volume %s can not be attached. Volume status is %s", volumeId, volumeState));
        }

        //check whether the account of volume and instance is same
        if (!volume.getAccount().equals(cloudStackApi.getVirtualMachineApi()
                .getVirtualMachine(instanceId).getAccount())) {
            log.error(String.format("Volume %s can not be attached. Instance account and Volume account " +
                    "are not the same ", volumeId));
        }

        boolean volumeBecameAvailable = false, volumeBecameAttached = false;

        try {
            if (volumeState == Volume.State.CREATING) {

                volumeBecameAvailable = waitForStatus(volumeId, Volume.State.ALLOCATED, 5);

            } else if (volumeState == Volume.State.READY) {
                volumeBecameAvailable = true;
            }

        } catch (TimeoutException e) {
            log.error("[Volume ID] " + volumeId + "did not become ALLOCATED within expected timeout");
        }

        //if volume state is 'ALLOCATED'
        if (volumeBecameAvailable) {

            //attach volume into instance
            cloudStackApi.getVolumeApi().attachVolume(volumeId, instanceId);

            try {
                volumeBecameAttached = waitForStatus(volumeId, Volume.State.READY, 2);
            } catch (TimeoutException e) {
                log.error("[Volume ID] " + volumeId + "did not become READY within expected timeout");
            }
        }

        try {
            // waiting 5seconds till volumes are actually attached.
            Thread.sleep(5000);
        } catch (InterruptedException ignored) {

        }

        //If volume state is not 'READY'
        if (!volumeBecameAttached) {
            log.error(String.format("[Volume ID] %s attachment is called, but not yet became attached", volumeId));
        }

        log.info(String.format("Volume [id]: %s attachment for instance [id]: %s was successful [status]: Attaching." +
                " of Iaas : %s", volumeId, instanceId, iaasInfo));

        return "Attaching";

    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {


        IaasProvider iaasInfo = getIaasProvider();

        ComputeServiceContext context = iaasInfo.getComputeService()
                .getContext();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Starting to detach volume %s from the instance %s", volumeId, instanceId));
        }

        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);

        cloudStackApi.getVolumeApi().detachVolume(volumeId);

        try {
            //TODO this is true only for newly created volumes
            if (waitForStatus(volumeId, Volume.State.ALLOCATED, 5)) {
                log.info(String.format("Detachment of Volume [id]: %s from instance [id]: %s was successful of Iaas : %s", volumeId, instanceId, iaasInfo));
            }
        } catch (TimeoutException e) {
            log.error(String.format("Detachment of Volume [id]: %s from instance [id]: %s was unsuccessful. [volume Status] : %s", volumeId, instanceId, iaasInfo));
        }

    }

    @Override
    public void deleteVolume(String volumeId) {
        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService()
                .getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);
        cloudStackApi.getVolumeApi().deleteVolume(volumeId);
        log.info("Deletion of Volume [id]: " + volumeId + " was successful. "
                + " of Iaas : " + iaasInfo);
    }

    @Override
    public String getIaasDevice(String device) {//todo implement this method(auto generated method)
        return null;
    }

    private boolean waitForStatus(String volumeId, Volume.State expectedStatus, int timeoutInMilliseconds) throws TimeoutException {
        int timeout = 1000 * 60 * timeoutInMilliseconds;
        long timout = System.currentTimeMillis() + timeout;

        IaasProvider iaasInfo = getIaasProvider();
        ComputeServiceContext context = iaasInfo.getComputeService().getContext();
        CloudStackApi cloudStackApi = context.unwrapApi(CloudStackApi.class);

        //get volume
        org.jclouds.cloudstack.domain.Volume volume = cloudStackApi.getVolumeApi().getVolume(volumeId);

        Volume.State volumeState = volume.getState();

        while (volumeState != expectedStatus) {
            try {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Volume %s is still NOT in %s. Current State=%s", volumeId, expectedStatus, volumeState));
                }
                if (volumeState == Volume.State.FAILED || volumeState == Volume.State.DESTROYED || volumeState == Volume.State.UNRECOGNIZED) {
                    log.error("Volume " + volumeId + " is in state" + volumeState);
                    return false;
                }

                Thread.sleep(1000);
                volumeState = volume.getState();
                if (System.currentTimeMillis() > timout) {
                    throw new TimeoutException();
                }
            } catch (InterruptedException e) {
                // Ignoring the exception
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("Volume %s status became %s", volumeId, expectedStatus));
        }

        return true;
    }

    private Map<String, String> convertByteArrayToHashMap(byte[] byteArray) {

        Map<String, String> map = new HashMap<String, String>();

        String stringFromByteArray = new String(byteArray);
        String[] keyValuePairs = stringFromByteArray.split(",");

        for (String keyValuePair : keyValuePairs) {
            String[] keyValue = keyValuePair.split("=");
            if (keyValue.length > 1) {
                map.put(keyValue[0], keyValue[1]);
            }
        }

        return map;
    }
}
