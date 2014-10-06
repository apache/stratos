package org.apache.stratos.cloud.controller.iaases;


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
import org.jclouds.cloudstack.domain.Zone;
import org.jclouds.cloudstack.options.ListZonesOptions;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.domain.Location;

import java.util.Set;

public class CloudstackIaas extends Iaas{

    private static final Log log = LogFactory.getLog(CloudstackIaas.class);



    public CloudstackIaas(IaasProvider iaasProvider) {super(iaasProvider);}

    @Override
    public void buildComputeServiceAndTemplate() {

        // builds and sets Compute Service
        ComputeServiceBuilderUtil.buildDefaultComputeService(getIaasProvider());
        // builds and sets Template
        buildTemplate();

    }


    @Override
    public void buildTemplate() {
///todo implement this method

        IaasProvider iaasInfo = getIaasProvider();

        //if compute service is not available
        if (iaasInfo.getComputeService() == null) {
            String msg = "Compute service is null for IaaS provider: "
                    + iaasInfo.getName();
            log.fatal(msg);
            throw new CloudControllerException(msg);
        }

        //create templateBuilder
        TemplateBuilder templateBuilder = iaasInfo.getComputeService()
                .templateBuilder();

        //todo what is this image id???
        // set image id specified
        templateBuilder.imageId(iaasInfo.getImage());

        System.out.println("===========iaasInfo.getImage()=============== "+iaasInfo.getImage());

        //logic

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





        //build the template
        Template template = templateBuilder.build();
        // set Template
        iaasInfo.setTemplate(template);


    }

    @Override
    public void setDynamicPayload() {
        ///todo implement this method
    }

    @Override
    public String associateAddress(NodeMetadata node) {

        //todo implement this method
        return null;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {

        //todo implement this method
        return null;
    }

    @Override
    public void releaseAddress(String ip) {
//todo implement this method
    }

    @Override
    public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {
        //todo implement this method
        return false;
    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {


        //todo study more about this

        IaasProvider iaasInfo = getIaasProvider();

        //no such a method in jclouds
        String msg = "Invalid region: " + region +" in the iaas: " +iaasInfo.getType();
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

        for(org.jclouds.cloudstack.domain.Zone configuredZone :zoneSet){
            if(configuredZone.getName().equalsIgnoreCase(zone)){
                return true;
            }
        }
        String msg = "Invalid zone: " + zone +" in the iaas: "+iaasInfo.getType();
        log.error(msg);
        throw new InvalidZoneException(msg);
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        //todo implement this method




        return true;
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return new CloudstackPartitionValidator();
    }



    @Override
    public String createVolume(int sizeGB, String snapshotId) {

        //todo implement this method
        return null;
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {

        //todo implement this method
        return null;
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {
//todo implement this method
    }

    @Override
    public void deleteVolume(String volumeId) {
//todo implement this method
    }

    @Override
    public String getIaasDevice(String device)
    {//todo implement this method
        return null;
    }
}
