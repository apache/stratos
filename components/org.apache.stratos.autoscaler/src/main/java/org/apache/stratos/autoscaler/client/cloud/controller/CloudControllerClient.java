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
    
    /* An instance of a CloudControllerClient is created when the class is loaded. 
     * Since the class is loaded only once, it is guaranteed that an object of 
     * CloudControllerClient is created only once. Hence it is singleton.
     */
    private static class InstanceHolder {
        private static final CloudControllerClient INSTANCE = new CloudControllerClient(); 
    }
    
    public static CloudControllerClient getInstance() {
    	return InstanceHolder.INSTANCE;
    }
    
    private CloudControllerClient(){
    	try {
            XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
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
            log.error(e.getMessage(), e);
            throw new PartitionValidationException(e.getMessage(), e);
        } catch (CloudControllerServiceInvalidPartitionExceptionException e) {
            log.error(e.getFaultMessage().getInvalidPartitionException().getMessage(), e);
            throw new PartitionValidationException(e.getFaultMessage().getInvalidPartitionException().getMessage());
        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
            log.error(e.getFaultMessage().getInvalidCartridgeTypeException().getMessage(), e);
            throw new PartitionValidationException(e.getFaultMessage().getInvalidCartridgeTypeException().getMessage());
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
        	log.error(e.getMessage(), e);
            throw new PartitionValidationException(e.getMessage(), e);
        } catch (CloudControllerServiceInvalidPartitionExceptionException e) {
        	log.error(e.getFaultMessage().getInvalidPartitionException().getMessage(), e);
        	throw new PartitionValidationException(e.getFaultMessage().getInvalidPartitionException().getMessage(), e);
		}

    }

    public org.apache.stratos.cloud.controller.pojo.MemberContext spawnAnInstance(Partition partition, 
    		String clusterId, String lbClusterId, String networkPartitionId) throws SpawningException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Trying to spawn an instance via cloud controller: [cluster] %s [partition] %s [lb-cluster] %s [network-partition-id] %s",
                    clusterId, partition.getId(), lbClusterId, networkPartitionId));
            }

            MemberContext member = new MemberContext();
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
        	log.error(e.getMessage(), e);
            throw new SpawningException(e.getMessage(), e);
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
        	String message = e.getFaultMessage().getUnregisteredCartridgeException().getMessage();
        	log.error(message, e);
			throw new SpawningException(message, e);
        } catch (RemoteException e) {
        	log.error(e.getMessage(), e);
            throw new SpawningException(e.getMessage(), e);
        } catch (CloudControllerServiceIllegalStateExceptionException e) {
        	log.error(e.getMessage(), e);
            throw new SpawningException(e.getMessage(), e);
		} catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
			String message = e.getFaultMessage().getInvalidIaasProviderException().getMessage();
        	log.error(message, e);
			throw new SpawningException(message, e);
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
            String msg = e.getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);

        } catch (CloudControllerServiceInvalidClusterExceptionException e) {
        	String message = e.getFaultMessage().getInvalidClusterException().getMessage();
            log.error(message, e);
            throw new TerminationException(message, e);
        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
        	String msg = e.getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);
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
        	String msg = e.getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);
        } catch (CloudControllerServiceIllegalArgumentExceptionException e) {
        	String msg = e.getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);
        } catch (CloudControllerServiceInvalidMemberExceptionException e) {
        	String msg = e.getFaultMessage().getInvalidMemberException().getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);
        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
        	String msg = e.getFaultMessage().getInvalidCartridgeTypeException().getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);
        }
    }



}
