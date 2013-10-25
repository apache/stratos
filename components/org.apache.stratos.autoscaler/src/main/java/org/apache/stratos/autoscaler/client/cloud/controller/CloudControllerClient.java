package org.apache.stratos.autoscaler.client.cloud.controller;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.policy.model.Partition;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceStub;
import org.apache.stratos.cloud.controller.util.xsd.LocationScope;

import java.rmi.RemoteException;


/**
 * This class will call cloud controller web service to take the action decided by Autoscaler
 */
public class CloudControllerClient {

    private static final Log log = LogFactory.getLog(CloudControllerClient.class);
    private CloudControllerServiceStub stub;

    public void spawnInstances(Partition partition, String clusterId, int memberCountToBeIncreased) throws SpawningException {
        //call CC spawnInstances method

        LocationScope locationScope = new LocationScope();
        locationScope.setCloud(partition.getIaas());
        locationScope.setRegion(partition.getZone());

        try {
            stub.startInstances(clusterId, locationScope, memberCountToBeIncreased);
        } catch (RemoteException e) {
            throw new SpawningException("Error occurred in cloud controller side while spawning instance", e );
        }
    }

    public void spawnAnInstance(Partition partition, String clusterId) throws SpawningException {
        //call CC spawnInstances method
                           
        LocationScope locationScope = new LocationScope();
        locationScope.setCloud(partition.getIaas());
        locationScope.setRegion(partition.getZone());

        try {
            stub.startInstance(clusterId, locationScope);
        } catch (RemoteException e) {
            throw new SpawningException("Error occurred in cloud controller side while spawning instance", e );
        }
    }

    public void terminate(Partition partition, String clusterId) throws TerminationException {
        //call CC terminate method


        LocationScope locationScope = new LocationScope();
        locationScope.setCloud(partition.getIaas());
        locationScope.setRegion(partition.getZone());

        try {
            stub.terminateInstance(clusterId, locationScope);
        } catch (RemoteException e) {
            throw new TerminationException("Error occurred in cloud controller side while spawning instance", e );
        }
    }


}
