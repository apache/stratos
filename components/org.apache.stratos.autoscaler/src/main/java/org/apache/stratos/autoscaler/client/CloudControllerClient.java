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

package org.apache.stratos.autoscaler.client;

import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.exception.cartridge.CartridgeInformationException;
import org.apache.stratos.autoscaler.exception.cartridge.SpawningException;
import org.apache.stratos.autoscaler.exception.cartridge.TerminationException;
import org.apache.stratos.autoscaler.exception.partition.PartitionValidationException;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerObjectConverter;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.domain.InstanceContext;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

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

    private CloudControllerClient() {
        try {
            XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
            int port = conf.getInt("autoscaler.cloudController.port", AutoscalerConstants.CLOUD_CONTROLLER_DEFAULT_PORT);
            String hostname = conf.getString("autoscaler.cloudController.hostname", "localhost");
            String epr = "https://" + hostname + ":" + port + "/" + AutoscalerConstants.CLOUD_CONTROLLER_SERVICE_SFX;
            int cloudControllerClientTimeout = conf.getInt("autoscaler.cloudController.clientTimeout", 180000);

            stub = new CloudControllerServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, cloudControllerClientTimeout);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, cloudControllerClientTimeout);
        } catch (Exception e) {
            log.error("Could not initialize cloud controller client", e);
        }
    }
    
    /*
     * This will validate the given partitions against the given cartridge type.
     */

    public synchronized boolean validateDeploymentPolicy(String cartridgeType, org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.Partition[] partitions) throws PartitionValidationException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Validating partitions of policy via cloud controller: [cartridge-type] %s", cartridgeType));
            }
            long startTime = System.currentTimeMillis();
            boolean result = stub.validateDeploymentPolicy(cartridgeType,
                    AutoscalerObjectConverter.convertASPartitionsToCCStubPartitions(partitions));
            if (log.isDebugEnabled()) {
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
    public synchronized boolean validatePartition(Partition partition) throws PartitionValidationException {

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Validating partition via cloud controller: [id] %s", partition.getId()));
            }
            long startTime = System.currentTimeMillis();
            boolean result = stub.validatePartition(partition);
            if (log.isDebugEnabled()) {
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

    public synchronized MemberContext startInstance(Partition partition,
                                                    String clusterId, String clusterInstanceId,
                                                    String networkPartitionId, boolean isPrimary,
                                                    int minMemberCount) throws SpawningException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Trying to spawn an instance via cloud controller: " +
                                "[cluster] %s [partition] %s [network-partition-id] %s",
                        clusterId, partition.getId(), networkPartitionId));
            }

            XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
            long expiryTime = conf.getLong(StratosConstants.OBSOLETED_VM_MEMBER_EXPIRY_TIMEOUT, 86400000);
            if (log.isDebugEnabled()) {
                log.debug("Member obsolete expiry time is set to: " + expiryTime);
            }

            InstanceContext instanceContext = new InstanceContext();
            instanceContext.setClusterId(clusterId);
            instanceContext.setClusterInstanceId(clusterInstanceId);
            instanceContext.setPartition(partition);
            instanceContext.setInitTime(System.currentTimeMillis());
            instanceContext.setObsoleteExpiryTime(expiryTime);
            instanceContext.setNetworkPartitionId(networkPartitionId);

            Properties memberContextProps = new Properties();
            Property isPrimaryProp = new Property();
            isPrimaryProp.setName("PRIMARY");
            isPrimaryProp.setValue(String.valueOf(isPrimary));

            Property minCountProp = new Property();
            minCountProp.setName(StratosConstants.MIN_COUNT);
            minCountProp.setValue(String.valueOf(minMemberCount));

            memberContextProps.addProperty(isPrimaryProp);
            memberContextProps.addProperty(minCountProp);
            instanceContext.setProperties(AutoscalerUtil.toStubProperties(memberContextProps));

            long startTime = System.currentTimeMillis();
            MemberContext memberContext = stub.startInstance(instanceContext);
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call startInstance() returned in %dms", (endTime - startTime)));
            }
            return memberContext;
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String message = e.getFaultMessage().getCartridgeNotFoundException().getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new SpawningException(e.getMessage(), e);
        } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
            String message = e.getFaultMessage().getInvalidIaasProviderException().getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        } catch (CloudControllerServiceCloudControllerExceptionException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        }
    }

    public synchronized void terminateInstances(String clusterId) throws TerminationException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Terminating all instances of cluster via cloud controller: [cluster] %s", clusterId));
            }
            long startTime = System.currentTimeMillis();
            stub.terminateInstances(clusterId);
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call terminateInstances() returned in %dms", (endTime - startTime)));
            }
        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);

        } catch (CloudControllerServiceInvalidClusterExceptionException e) {
            String message = e.getFaultMessage().getInvalidClusterException().getMessage();
            log.error(message, e);
            throw new TerminationException(message, e);
        }
    }

    public synchronized void createApplicationClusters(String appId,
                                                       ApplicationClusterContext[] applicationClusterContexts) {
        List<org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext> contextDTOs =
                                        new ArrayList<org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext>();
        if(applicationClusterContexts != null) {
            for (ApplicationClusterContext applicationClusterContext : applicationClusterContexts) {
                if(applicationClusterContext != null) {
                    org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext dto = new org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext();
                    dto.setClusterId(applicationClusterContext.getClusterId());
                    dto.setAutoscalePolicyName(applicationClusterContext.getAutoscalePolicyName());
                    dto.setDeploymentPolicyName(applicationClusterContext.getDeploymentPolicyName());
                    dto.setCartridgeType(applicationClusterContext.getCartridgeType());
                    dto.setHostName(applicationClusterContext.getHostName());
                    dto.setTenantRange(applicationClusterContext.getTenantRange());
                    dto.setTextPayload(applicationClusterContext.getTextPayload());
                    dto.setLbCluster(applicationClusterContext.isLbCluster());
                    dto.setProperties(AutoscalerUtil.toStubProperties(applicationClusterContext.getProperties()));
                    dto.setDependencyClusterIds(applicationClusterContext.getDependencyClusterIds());
                    contextDTOs.add(dto);
                }
            }
        }

        org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext[] applicationClusterContextDTOs =
                new org.apache.stratos.cloud.controller.stub.domain.ApplicationClusterContext[contextDTOs.size()];
        contextDTOs.toArray(applicationClusterContextDTOs);
        try {
            stub.createApplicationClusters(appId, applicationClusterContextDTOs);
        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            //throw new TerminationException(msg, e);
        } catch (CloudControllerServiceApplicationClusterRegistrationExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
        }


    }

    public void createClusterInstance (String serviceType, String clusterId, String alias,
                                       String instanceId, String partitionId, String networkPartitionId){
        try {
            stub.createClusterInstance(serviceType, clusterId, alias, instanceId,
                    partitionId, networkPartitionId);

        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        } catch (CloudControllerServiceClusterInstanceCreationExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }

    public synchronized void terminateInstance(String memberId) throws TerminationException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Terminating instance via cloud controller: [member] %s", memberId));
            }
            long startTime = System.currentTimeMillis();
            stub.terminateInstance(memberId);
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call terminateInstance() returned in %dms", (endTime - startTime)));
            }
        } catch (RemoteException e) {
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
        } catch (CloudControllerServiceCloudControllerExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new TerminationException(msg, e);
        }
    }

    public CartridgeInfo getCartrdgeInformation(String cartridgeType) throws CartridgeInformationException {

        try {
            return stub.getCartridgeInfo(cartridgeType);

        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new CartridgeInformationException(msg, e);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new CartridgeInformationException(msg, e);
        }
    }
}
