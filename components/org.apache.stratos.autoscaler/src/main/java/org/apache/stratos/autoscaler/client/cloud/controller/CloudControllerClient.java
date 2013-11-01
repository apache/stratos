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
    
    public CloudControllerClient(){
    	try {
			stub = new CloudControllerServiceStub(
					"https://localhost:9444/services/CloudControllerService");
		} catch (Exception e) {
			log.error("Stub init error", e);
		}
    }

    public void spawnInstances(Partition partition, String clusterId, int memberCountToBeIncreased) throws SpawningException {
        //call CC spawnInstances method

        log.info("Calling CC for spawning instances in cluster " + clusterId);
        LocationScope locationScope = new LocationScope();
        locationScope.setCloud(partition.getIaas());
        locationScope.setRegion(partition.getZone());

        try {
            for(int i =0; i< memberCountToBeIncreased; i++){
                stub.startInstance(clusterId, locationScope);
            }
        } catch (RemoteException e) {
            log.error("Error occurred in cloud controller side while spawning instance");
//            throw new SpawningException("Error occurred in cloud controller side while spawning instance", e );
        }
    }

    public void spawnAnInstance(Partition partition, String clusterId) throws SpawningException {
        //call CC spawnInstances method

        log.info("Calling CC for spawning an instance in cluster " + clusterId);
        LocationScope locationScope = new LocationScope();
        locationScope.setCloud(partition.getIaas());
        locationScope.setRegion(partition.getZone());

        try {
            stub.startInstance(clusterId, locationScope);
        } catch (RemoteException e) {

            log.error("Error occurred in cloud controller side while spawning instance");
//            throw new SpawningException("Error occurred in cloud controller side while spawning instance", e );
        }
    }

    public void terminate(Partition partition, String clusterId) throws TerminationException {
        //call CC terminate method

        log.info("Calling CC for terminating an instance in cluster " + clusterId);
        LocationScope locationScope = new LocationScope();
        locationScope.setCloud(partition.getIaas());
        locationScope.setRegion(partition.getZone());

        try {
            stub.terminateInstance(clusterId, locationScope);
        } catch (RemoteException e) {

            log.error("Error occurred in cloud controller side while terminating instance");
//            throw new TerminationException("Error occurred in cloud controller side while terminating instance", e );
        }
    }



}
