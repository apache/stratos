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
 * KIND, either express or implied.  TcSee the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.client.cloud.controller;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.PolicyValidationException;
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.deployment.policy.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceIllegalArgumentExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidCartridgeTypeExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidMemberExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidPartitionExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceStub;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceUnregisteredCartridgeExceptionException;

import java.rmi.RemoteException;


/**
 * This class will call cloud controller web service to take the action decided by Autoscaler
 */
public class CloudControllerClient {

    private static final Log log = LogFactory.getLog(CloudControllerClient.class);
    private static CloudControllerServiceStub stub;
    private static CloudControllerClient instance;
    
    public static CloudControllerClient getInstance() {

        if (instance == null) {
            synchronized (CloudControllerClient.class) {
                
                if(instance == null) {
                    instance = new CloudControllerClient();
                }
            }
        }

        return instance;
    }
    
    private CloudControllerClient(){
    	try {
            XMLConfiguration conf = ConfUtil.getInstance().getConfiguration();
            int port = conf.getInt("autoscaler.cloudController.port", Constants.CLOUD_CONTROLLER_DEFAULT_PORT);
            String hostname = conf.getString("autoscaler.cloudController.hostname", "localhost");
            String epr = "https://" + hostname + ":" + port + "/" + Constants.CLOUD_CONTROLLER_SERVICE_SFX  ;
            stub = new CloudControllerServiceStub(epr);
		} catch (Exception e) {
			log.error("Stub init error", e);
		}
    }

    public void spawnInstances(Partition partition, String clusterId, int memberCountToBeIncreased) throws SpawningException {
        //call CC spawnInstances method

        log.info("Calling CC for spawning instances in cluster " + clusterId);
        log.info("Member count to be increased: " + memberCountToBeIncreased);

        for(int i =0; i< memberCountToBeIncreased; i++){
            spawnAnInstance(partition, clusterId);
        }
        
    }
    
    public boolean validateDeploymentPolicy(String cartridgeType, DeploymentPolicy policy) throws PolicyValidationException{
        
        try {
            return stub.validateDeploymentPolicy(cartridgeType, policy);
        } catch (RemoteException e) {
            log.error(e.getMessage());
            throw new PolicyValidationException(e);
        } catch (CloudControllerServiceInvalidPartitionExceptionException e) {
            log.error(e.getMessage());
            throw new PolicyValidationException(e);
        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
            log.error(e.getMessage());
            throw new PolicyValidationException(e);
        }
    }
    
    /*
     * Calls the CC to validate the partition.
     */
    public boolean validatePartition(Partition partition) throws InvalidPartitionException{
        
        try {
            return stub.validatePartition(partition);
        } catch (RemoteException e) {
            log.error(e.getMessage());
            throw new InvalidPartitionException(e);
        } catch (CloudControllerServiceInvalidPartitionExceptionException e) {
        	throw new InvalidPartitionException(e);
		}
    }

    public void spawnAnInstance(Partition partition, String clusterId) throws SpawningException {
        try {
            stub.startInstance(clusterId, partition);
        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
            log.error(e.getMessage());
            throw new SpawningException(e);
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            log.error(e.getMessage());
            throw new SpawningException(e);
        } catch (RemoteException e) {
            String msg = "Error occurred in cloud controller side while spawning instance";
            log.error(msg);
            throw new SpawningException(msg, e);
        }
    }

    public void terminate(String memberId) throws TerminationException {
        //call CC terminate method

        log.info("Calling CC for terminating member with id: " + memberId);

        try {
            stub.terminateInstance(memberId);
        } catch (RemoteException e) {
            String msg = "Error occurred in cloud controller side while terminating instance";
            log.error(msg, e);
            throw new TerminationException(msg, e);

        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
            log.error(e.getMessage());
            throw new TerminationException(e);
        } catch (CloudControllerServiceInvalidMemberExceptionException e) {
            log.error(e.getMessage());
            throw new TerminationException(e);
        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
            log.error(e.getMessage());
            throw new TerminationException(e);
        }
    }



}
