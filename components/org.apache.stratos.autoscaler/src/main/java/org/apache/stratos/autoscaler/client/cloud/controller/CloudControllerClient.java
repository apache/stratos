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

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.PartitionValidationException;
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.*;

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
            int cloudControllerClientTimeout = conf.getInt("autoscaler.cloudController.clientTimeout", 180000);
            stub = new CloudControllerServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, cloudControllerClientTimeout);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, cloudControllerClientTimeout);
		} catch (Exception e) {
			log.error("Stub init error", e);
		}
    }
    
    /*
     * This will validate the given partitions against the given cartridge type.
     */
    
    public boolean validateDeploymentPolicy(String cartridgeType, DeploymentPolicy deploymentPolicy) throws PartitionValidationException{
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Validating partitions of policy via cloud controller: [id] %s", deploymentPolicy.getId()));
            }
            long startTime = System.currentTimeMillis();
            boolean result = stub.validateDeploymentPolicy(cartridgeType, deploymentPolicy.getAllPartitions());
            if(log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call validateDeploymentPolicy() returned in %dms", (endTime - startTime)));
            }
            return result;
        } catch (RemoteException e) {
            log.error(e.getMessage());
            throw new PartitionValidationException(e);
        } catch (CloudControllerServiceInvalidPartitionExceptionException e) {
            log.error(e.getMessage());
            throw new PartitionValidationException(e);
        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
            log.error(e.getMessage());
            throw new PartitionValidationException(e);
        }

    }
    
    /*
     * Calls the CC to validate the partition.
     */
    public boolean validatePartition(Partition partition) throws PartitionValidationException{
        
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Validating partition via cloud controller: [id] %s", partition.getId()));
            }
            long startTime = System.currentTimeMillis();
            boolean result = stub.validatePartition(partition);
            if(log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call validatePartition() returned in %dms", (endTime - startTime)));
            }
            return result;
        } catch (RemoteException e) {
            throw new PartitionValidationException(e);
        } catch (CloudControllerServiceInvalidPartitionExceptionException e) {
        	throw new PartitionValidationException(e.getMessage(),e);
		}

    }

    public org.apache.stratos.cloud.controller.pojo.MemberContext spawnAnInstance(Partition partition, String clusterId, String lbClusterId, String networkPartitionId) throws SpawningException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Trying to spawn an instance via cloud controller: [cluster] %s [partition] %s [lb-cluster] %s [network-partition-id] %s",
                    clusterId, partition.getId(), lbClusterId, networkPartitionId));
            }

            org.apache.stratos.cloud.controller.pojo.MemberContext member = new org.apache.stratos.cloud.controller.pojo.MemberContext();
            member.setClusterId(clusterId);
            member.setPartition(partition);
            member.setLbClusterId(lbClusterId);
            member.setInitTime(System.currentTimeMillis());
            member.setNetworkPartitionId(networkPartitionId);

            long startTime = System.currentTimeMillis();
            MemberContext memberContext = stub.startInstance(member);
            if(log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call startInstance() returned in %dms", (endTime - startTime)));
            }
            return memberContext;
        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
            throw new SpawningException(e);
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            throw new SpawningException(e);
        } catch (RemoteException e) {
            throw new SpawningException(String.format("Error occurred in cloud controller while spawning instance: [cluster] %s [partition] %s [lb-cluster] %s [network-partition-id] %s",
                    clusterId, partition.getId(), lbClusterId, networkPartitionId), e);
        }
    }
    
    public void terminateAllInstances(String clusterId) throws TerminationException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Terminating all instances of cluster via cloud controller: [cluster] %s", clusterId));
            }
            long startTime = System.currentTimeMillis();
            stub.terminateAllInstances(clusterId);
            if(log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call terminateAllInstances() returned in %dms", (endTime - startTime)));
            }
        } catch (RemoteException e) {
            String msg = "Error occurred in cloud controller side while terminating instance";
            log.error(msg, e);
            throw new TerminationException(msg, e);

        } catch (CloudControllerServiceInvalidClusterExceptionException e) {
            log.error(e.getMessage());
            throw new TerminationException(e);
        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
            log.error(e.getMessage());
            throw new TerminationException(e);
        }
    }

    public void terminate(String memberId) throws TerminationException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Terminating instance via cloud controller: [member] %s", memberId));
            }
            long startTime = System.currentTimeMillis();
            stub.terminateInstance(memberId);
            if(log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call terminateInstance() returned in %dms", (endTime - startTime)));
            }
        } catch (RemoteException e) {
            throw new TerminationException("Could not terminate instance", e);
        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
            throw new TerminationException(e);
        } catch (CloudControllerServiceInvalidMemberExceptionException e) {
            throw new TerminationException(e);
        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
            throw new TerminationException(e);
        }
    }



}
