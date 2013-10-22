package org.apache.stratos.autoscaler.client.cloud.controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class will call cloud controller web service to take the action decided by Autoscaler
 */
public class CloudControllerClient {

    private static final Log log = LogFactory.getLog(CloudControllerClient.class);
    public void act(String action){

        //TODO add real CC call
        log.info("Call CC to take action : " + action);
    }
}
