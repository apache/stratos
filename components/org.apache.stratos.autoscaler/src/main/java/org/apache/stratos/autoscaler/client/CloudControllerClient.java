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
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.applications.pojo.ApplicationClusterContext;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.kubernetes.KubernetesManager;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.*;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.apache.stratos.common.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            int port = conf.getInt("autoscaler.cloudController.port", Constants.CLOUD_CONTROLLER_DEFAULT_PORT);
            String hostname = conf.getString("autoscaler.cloudController.hostname", "localhost");
            String epr = "https://" + hostname + ":" + port + "/" + Constants.CLOUD_CONTROLLER_SERVICE_SFX;
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

    public synchronized boolean validateDeploymentPolicy(String cartridgeType, DeploymentPolicy deploymentPolicy) throws PartitionValidationException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Validating partitions of policy via cloud controller: [id] %s", deploymentPolicy.getId()));
            }
            long startTime = System.currentTimeMillis();
            boolean result = stub.validateDeploymentPolicy(cartridgeType, deploymentPolicy.getAllPartitions());
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

    public synchronized MemberContext spawnAnInstance(Partition partition,
                                                      String clusterId, String lbClusterId, String networkPartitionId, boolean isPrimary, int minMemberCount) throws SpawningException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Trying to spawn an instance via cloud controller: [cluster] %s [partition] %s [lb-cluster] %s [network-partition-id] %s",
                        clusterId, partition.getId(), lbClusterId, networkPartitionId));
            }

            MemberContext member = new MemberContext();
            member.setClusterId(clusterId);
            member.setPartition(partition);
            member.setLbClusterId(lbClusterId);
            member.setInitTime(System.currentTimeMillis());
            member.setNetworkPartitionId(networkPartitionId);
            Properties memberContextProps = new Properties();
            Property isPrimaryProp = new Property();
            isPrimaryProp.setName("PRIMARY");
            isPrimaryProp.setValue(String.valueOf(isPrimary));

            Property minCountProp = new Property();
            minCountProp.setName("MIN_COUNT");
            minCountProp.setValue(String.valueOf(minMemberCount));

            memberContextProps.addProperties(isPrimaryProp);
            memberContextProps.addProperties(minCountProp);
            member.setProperties(memberContextProps);


            long startTime = System.currentTimeMillis();
            MemberContext memberContext = stub.startInstance(member);
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call startInstance() returned in %dms", (endTime - startTime)));
            }
            return memberContext;
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String message = e.getFaultMessage().getUnregisteredCartridgeException().getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new SpawningException(e.getMessage(), e);
        } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
            String message = e.getFaultMessage().getInvalidIaasProviderException().getMessage();
            log.error(message, e);
            throw new SpawningException(message, e);
        }
    }

    public synchronized void terminateAllInstances(String clusterId) throws TerminationException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Terminating all instances of cluster via cloud controller: [cluster] %s", clusterId));
            }
            long startTime = System.currentTimeMillis();
            stub.terminateAllInstances(clusterId);
            if (log.isDebugEnabled()) {
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
        }
    }

    public synchronized void createApplicationClusters(String appId,
                                                       Set<ApplicationClusterContext> appClusterContexts) {
        List<ApplicationClusterContextDTO> contextDTOs =
                                        new ArrayList<ApplicationClusterContextDTO>();
        for(ApplicationClusterContext context : appClusterContexts) {
           ApplicationClusterContextDTO dto = new ApplicationClusterContextDTO();
            dto.setClusterId(context.getClusterId());
            dto.setAutoscalePolicyName(context.getAutoscalePolicyName());
            dto.setDeploymentPolicyName(context.getDeploymentPolicyName());
            dto.setCartridgeType(context.getCartridgeType());
            dto.setHostName(context.getHostName());
            dto.setTenantRange(context.getTenantRange());
            dto.setTextPayload(context.getTextPayload());
            dto.setLbCluster(context.isLbCluster());
            dto.setProperties(context.getProperties());
            contextDTOs.add(dto);
        }

        ApplicationClusterContextDTO[] applicationClusterContextDTOs =
                new ApplicationClusterContextDTO[contextDTOs.size()];
        contextDTOs.toArray(applicationClusterContextDTOs);
        try {
            stub.createApplicationClusters(appId, applicationClusterContextDTOs);
        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            //throw new TerminationException(msg, e);
        } catch (CloudControllerServiceApplicationClusterRegistrationExceptionException e) {
            //e.printStackTrace();
            String msg = e.getMessage();
            log.error(msg, e);
        }


    }

    public synchronized void terminate(String memberId) throws TerminationException {
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
        }
    }

    public CartridgeInfo getCartrdgeInformation(String cartridgeType) throws CartridgeInformationException {

        try {
            return stub.getCartridgeInfo(cartridgeType);

        } catch (RemoteException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new CartridgeInformationException(msg, e);
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String msg = e.getMessage();
            log.error(msg, e);
            throw new CartridgeInformationException(msg, e);
        }
    }

    /**
     * @param kubernetesClusterId
     * 				kubernetes cluster id in which the cluster needs be created
     * @param clusterId
     * 				service cluster id
     * @return the {@link MemberContext}
     * @throws SpawningException
     * 				if client can't connect to cloud controller service, if
     * 				cartridge not found for the given cluster id, or if the given
     * 				kubernetes cluster id is not valid
     */
    public synchronized MemberContext[] startContainers(String kubernetesClusterId, String clusterId) throws SpawningException {
        try {

            KubernetesManager kubernetesManager = KubernetesManager.getInstance();
            KubernetesMaster kubernetesMaster = kubernetesManager.getKubernetesMasterInGroup(kubernetesClusterId);
            String kubernetesMasterIP = kubernetesMaster.getHostIpAddress();
            KubernetesGroup kubernetesGroup = kubernetesManager.getKubernetesGroup(kubernetesClusterId);
            int lower = kubernetesGroup.getPortRange().getLower();
            int upper = kubernetesGroup.getPortRange().getUpper();
            String portRange = Integer.toString(lower) + "-" + Integer.toString(upper);

            ContainerClusterContext context = new ContainerClusterContext();
            context.setClusterId(clusterId);
            Properties memberContextProps = new Properties();
            Property kubernetesClusterMasterIPProps = new Property();
            kubernetesClusterMasterIPProps.setName(StratosConstants.KUBERNETES_MASTER_IP);
            kubernetesClusterMasterIPProps.setValue(kubernetesMasterIP);
            memberContextProps.addProperties(kubernetesClusterMasterIPProps);
            Property kubernetesClusterPortRangeProps = new Property();
            kubernetesClusterPortRangeProps.setName(StratosConstants.KUBERNETES_PORT_RANGE);
            kubernetesClusterPortRangeProps.setValue(portRange);
            memberContextProps.addProperties(kubernetesClusterPortRangeProps);
            context.setProperties(memberContextProps);
            long startTime = System.currentTimeMillis();
            MemberContext[] memberContexts = stub.startContainers(context);

            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call startContainer() returned in %dms", (endTime - startTime)));
            }
            return memberContexts;
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String msg = String.format("Error while creating containers. Cartridge not found for cluster [%s] ", clusterId);
            log.error(msg, e);
            throw new SpawningException(msg, e);
        } catch (RemoteException e) {
        	String msg = "Error while creating containers, couldn't communicate with cloud controller service";
        	log.error(msg, e);
        	throw new SpawningException(msg, e);
        } catch (NonExistingKubernetesGroupException e) {
        	String msg = String.format("Error while creating containers, invalid kubernetes group [%s] ", kubernetesClusterId);
        	log.error(msg, e);
        	throw new SpawningException(msg, e);
        }
    }

    public synchronized void terminateAllContainers(String clusterId) throws TerminationException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Terminating containers via cloud controller: [cluster] %s", clusterId));
            }
            long startTime = System.currentTimeMillis();
            stub.terminateAllContainers(clusterId);
            if (log.isDebugEnabled()) {
                long endTime = System.currentTimeMillis();
                log.debug(String.format("Service call terminateContainer() returned in %dms", (endTime - startTime)));
            }
        } catch (RemoteException e) {
        	String msg = "Error while creating containers, couldn't communicate with cloud controller service";
            log.error(msg, e);
            throw new TerminationException(msg, e);
        } catch (CloudControllerServiceInvalidClusterExceptionException e) {
        	String msg = "Invalid Cluster [clusterId] " + clusterId;
            log.error(msg, e);
            throw new TerminationException(msg, e);
        }
    }

    public synchronized MemberContext[] updateContainers(String clusterId, int replicas)
            throws SpawningException {
        try {
            log.info(String.format("Updating kubernetes replication controller via cloud controller: " +
                    "[cluster] %s [replicas] %s", clusterId, replicas));
            MemberContext[] memberContexts = stub.updateContainers(clusterId, replicas);
            return memberContexts;
        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
            String msg = "Error while updating kubernetes controller, cartridge not found for [cluster] " + clusterId;
            log.error(msg, e);
            throw new SpawningException(msg, e);
        } catch (RemoteException e) {
            String msg = "Error while updating kubernetes controller, cannot communicate with " +
                    "cloud controller service";
            log.error(msg, e);
            throw new SpawningException(msg, e);
        }
    }

    public synchronized void terminateContainer(String memberId) throws TerminationException {
        try {
            stub.terminateContainer(memberId);
        } catch (RemoteException e) {
            String msg = "Error while updating kubernetes controller, cannot communicate with " +
                    "cloud controller service";
            log.error(msg, e);
            throw new TerminationException(msg, e);
        } catch (CloudControllerServiceMemberTerminationFailedExceptionException e) {
            String msg = "Error while terminating container, member not valid for member id : " + memberId;
            log.error(msg, e);
            throw new TerminationException(msg, e);
        }
    }
}
