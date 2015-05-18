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

package org.apache.stratos.common.client;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.*;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.common.constants.StratosConstants;

import java.rmi.RemoteException;

public class CloudControllerServiceClient {

    private CloudControllerServiceStub stub;

    private static final Log log = LogFactory.getLog(CloudControllerServiceClient.class);
    private static volatile CloudControllerServiceClient instance;

    private CloudControllerServiceClient(String epr) throws AxisFault {

        String ccSocketTimeout = System.getProperty(StratosConstants.CLOUD_CONTROLLER_CLIENT_SOCKET_TIMEOUT) == null ?
                StratosConstants.DEFAULT_CLIENT_SOCKET_TIMEOUT :
                System.getProperty(StratosConstants.CLOUD_CONTROLLER_CLIENT_SOCKET_TIMEOUT);

        String ccConnectionTimeout = System.getProperty(StratosConstants.CLOUD_CONTROLLER_CLIENT_CONNECTION_TIMEOUT) == null ?
                StratosConstants.DEFAULT_CLIENT_CONNECTION_TIMEOUT :
                System.getProperty(StratosConstants.CLOUD_CONTROLLER_CLIENT_CONNECTION_TIMEOUT);

        try {
            stub = new CloudControllerServiceStub(epr);
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, Integer.valueOf(ccSocketTimeout));
            stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, new Integer(ccConnectionTimeout));

        } catch (AxisFault axisFault) {
            String msg = "Could not initialize cloud controller service client";
            log.error(msg, axisFault);
            throw new AxisFault(msg, axisFault);
        }
    }

    public static CloudControllerServiceClient getInstance() throws AxisFault {
        if (instance == null) {
            synchronized (CloudControllerServiceClient.class) {
                if (instance == null) {
                    String cloudControllerServiceUrl = System.getProperty(StratosConstants.CLOUD_CONTROLLER_SERVICE_URL);
                    if (StringUtils.isBlank(cloudControllerServiceUrl)) {
                        throw new RuntimeException(String.format("System property not found: %s",
                                StratosConstants.CLOUD_CONTROLLER_SERVICE_URL));
                    }
                    instance = new CloudControllerServiceClient(cloudControllerServiceUrl);
                }
            }
        }
        return instance;
    }

    public void addCartridge(Cartridge cartridgeConfig)
            throws RemoteException, CloudControllerServiceCartridgeAlreadyExistsExceptionException,
            CloudControllerServiceInvalidCartridgeDefinitionExceptionException,
            CloudControllerServiceInvalidIaasProviderExceptionException {
        stub.addCartridge(cartridgeConfig);
    }

    public void updateCartridge(Cartridge cartridgeConfig)
            throws RemoteException, CloudControllerServiceInvalidCartridgeDefinitionExceptionException,
            CloudControllerServiceInvalidIaasProviderExceptionException,
            CloudControllerServiceCartridgeDefinitionNotExistsExceptionException {
        stub.updateCartridge(cartridgeConfig);
    }

    public void removeCartridge(String cartridgeType) throws RemoteException, CloudControllerServiceInvalidCartridgeTypeExceptionException {
        stub.removeCartridge(cartridgeType);
    }

    public String[] getServiceGroupSubGroups(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
        return stub.getServiceGroupSubGroups(name);
    }

    public String[] getServiceGroupCartridges(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
        return stub.getServiceGroupCartridges(name);
    }

    public Dependencies getServiceGroupDependencies(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
        return stub.getServiceGroupDependencies(name);
    }

    public ServiceGroup getServiceGroup(String name) throws RemoteException, CloudControllerServiceInvalidServiceGroupExceptionException {
        return stub.getServiceGroup(name);
    }

    public void terminateInstance(String memberId) throws Exception {
        if (log.isInfoEnabled()) {
            log.info(String.format("Terminating instance via cloud controller: [member] %s", memberId));
        }
        long startTime = System.currentTimeMillis();
        stub.terminateInstance(memberId);
        if (log.isDebugEnabled()) {
            long endTime = System.currentTimeMillis();
            log.debug(String.format("Service call terminateInstance() returned in %dms", (endTime - startTime)));
        }
    }

    public void terminateInstanceForcefully(String memberId) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Terminating instance forcefully via cloud controller: [member] %s", memberId));
        }
        stub.terminateInstanceForcefully(memberId);
    }

    public void terminateAllInstances(String clusterId) throws RemoteException,
            CloudControllerServiceInvalidClusterExceptionException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Terminating all instances of cluster via cloud controller: [cluster] %s", clusterId));
        }
        long startTime = System.currentTimeMillis();
        stub.terminateInstances(clusterId);

        if (log.isDebugEnabled()) {
            long endTime = System.currentTimeMillis();
            log.debug(String.format("Service call terminateInstances() returned in %dms", (endTime - startTime)));
        }
    }

    public String[] getRegisteredCartridges() throws RemoteException {
        return stub.getCartridges();
    }

    public Cartridge getCartridge(String cartridgeType) throws RemoteException,
            CloudControllerServiceCartridgeNotFoundExceptionException {
        return stub.getCartridge(cartridgeType);
    }

    public ClusterContext getClusterContext(String clusterId) throws RemoteException {

        return stub.getClusterContext(clusterId);
    }

    public boolean deployKubernetesCluster(KubernetesCluster kubernetesCluster) throws RemoteException,
            CloudControllerServiceInvalidKubernetesClusterExceptionException,
            CloudControllerServiceKubernetesClusterAlreadyExistsExceptionException {
        return stub.addKubernetesCluster(kubernetesCluster);
    }

    public boolean addKubernetesHost(String kubernetesClusterId, KubernetesHost kubernetesHost)
            throws RemoteException, CloudControllerServiceInvalidKubernetesHostExceptionException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {

        return stub.addKubernetesHost(kubernetesClusterId, kubernetesHost);
    }

    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster) throws RemoteException,
            CloudControllerServiceInvalidKubernetesMasterExceptionException,
            CloudControllerServiceNonExistingKubernetesMasterExceptionException {
        return stub.updateKubernetesMaster(kubernetesMaster);
    }

    public KubernetesCluster[] getAvailableKubernetesClusters() throws RemoteException {
        return stub.getKubernetesClusters();
    }

    public KubernetesCluster getKubernetesCluster(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.getKubernetesCluster(kubernetesClusterId);
    }

    public void undeployKubernetesCluster(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        stub.removeKubernetesCluster(kubernetesClusterId);
    }

    public boolean undeployKubernetesHost(String kubernetesHostId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesHostExceptionException {
        return stub.removeKubernetesHost(kubernetesHostId);
    }

    public KubernetesHost[] getKubernetesHosts(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.getHostsForKubernetesCluster(kubernetesClusterId);
    }

    public KubernetesMaster getKubernetesMaster(String kubernetesClusterId) throws RemoteException,
            CloudControllerServiceNonExistingKubernetesClusterExceptionException {
        return stub.getMasterForKubernetesCluster(kubernetesClusterId);
    }

    public boolean updateKubernetesHost(KubernetesHost kubernetesHost) throws RemoteException,
            CloudControllerServiceInvalidKubernetesHostExceptionException,
            CloudControllerServiceNonExistingKubernetesHostExceptionException {
        return stub.updateKubernetesHost(kubernetesHost);
    }

    public void validateNetworkPartitionOfDeploymentPolicy(String cartridgeType, String networkPartitionId)
            throws RemoteException,
            CloudControllerServiceInvalidPartitionExceptionException,
            CloudControllerServiceInvalidCartridgeTypeExceptionException {
        stub.validateDeploymentPolicyNetworkPartition(cartridgeType, networkPartitionId);
    }

    public void addNetworkPartition(NetworkPartition networkPartition) throws RemoteException,
            CloudControllerServiceNetworkPartitionAlreadyExistsExceptionException,
            CloudControllerServiceInvalidNetworkPartitionExceptionException {
        stub.addNetworkPartition(networkPartition);
    }

    public void removeNetworkPartition(String networkPartitionId) throws RemoteException,
            CloudControllerServiceNetworkPartitionNotExistsExceptionException {
        stub.removeNetworkPartition(networkPartitionId);
    }

    public void updateNetworkPartition(NetworkPartition networkPartition) throws RemoteException,
            CloudControllerServiceNetworkPartitionNotExistsExceptionException {
        stub.updateNetworkPartition(networkPartition);
    }

    public NetworkPartition[] getNetworkPartitions() throws RemoteException {
        return stub.getNetworkPartitions();
    }

    public NetworkPartition getNetworkPartition(String networkPartitionId) throws RemoteException {
        return stub.getNetworkPartition(networkPartitionId);
    }

    public void createClusterInstance(String serviceType, String clusterId,
                                      String alias, String instanceId, String partitionId,
                                      String networkPartitionId) throws RemoteException {
        try {
            stub.createClusterInstance(serviceType, clusterId, alias,
                    instanceId, partitionId, networkPartitionId);

        } catch (CloudControllerServiceClusterInstanceCreationExceptionException e) {
            String msg = e.getFaultMessage().getClusterInstanceCreationException().getMessage();
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
    }
}
