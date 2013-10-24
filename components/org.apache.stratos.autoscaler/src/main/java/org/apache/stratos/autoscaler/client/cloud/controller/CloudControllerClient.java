package org.apache.stratos.autoscaler.client.cloud.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.model.Partition;

/**
 * This class will call cloud controller web service to take the action decided by Autoscaler
 */
public class CloudControllerClient {

    private static final Log log = LogFactory.getLog(CloudControllerClient.class);
    public void act(String action){

        //TODO add real CC call
        log.info("Call CC to take action : " + action);
    }

    public void spawnInstances(Partition partition, int memberCountToBeIncreased) {
        //call CC spawnInstances method
    }

    public void spawnAnInstance(Partition partition) {
        //call CC spawnInstances method
    }

    public void terminate(Partition partition) {
        //call CC terminate method
    }

}
