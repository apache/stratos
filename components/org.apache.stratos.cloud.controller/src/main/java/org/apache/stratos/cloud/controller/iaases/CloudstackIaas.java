package org.apache.stratos.cloud.controller.iaases;


import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.apache.stratos.cloud.controller.interfaces.Iaas;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;
import org.jclouds.compute.domain.NodeMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CloudstackIaas extends Iaas{

    private static final Log log = LogFactory.getLog(CloudstackIaas.class);



    public CloudstackIaas(IaasProvider iaasProvider) {super(iaasProvider);}

    @Override
    public void buildComputeServiceAndTemplate() {




    }

    @Override
    public void setDynamicPayload() {

    }

    @Override
    public String associateAddress(NodeMetadata node) {
        return null;
    }

    @Override
    public String associatePredefinedAddress(NodeMetadata node, String ip) {
        return null;
    }

    @Override
    public void releaseAddress(String ip) {

    }

    @Override
    public boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey) {
        return false;
    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
        return false;
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException {
        return false;
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        return false;
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return null;
    }

    @Override
    public void buildTemplate() {

    }

    @Override
    public String createVolume(int sizeGB, String snapshotId) {
        return null;
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        return null;
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {

    }

    @Override
    public void deleteVolume(String volumeId) {

    }

    @Override
    public String getIaasDevice(String device) {
        return null;
    }
}
