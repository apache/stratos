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
package org.apache.stratos.cloud.controller.services;

import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesMaster;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;

/**
 * This API provides a way to communicate with underline
 * Infrastructure which are supported by <i>jClouds</i>.
 */
public interface CloudControllerService {

    /**
     * Add a cartridge
     *
     * @param cartridgeConfig cartridge configuration to be deployed
     * @throws InvalidCartridgeDefinitionException if the cartridge configuration is not valid.
     * @throws InvalidIaasProviderException        if the iaas providers configured are not valid.
     * @throws IllegalArgumentException            if the provided argument is not valid.
     */
    public boolean addCartridge(Cartridge cartridgeConfig)
            throws InvalidCartridgeDefinitionException, InvalidIaasProviderException, CartridgeAlreadyExistsException;

    /**
     * Update a cartridge
     *
     * @param cartridgeConfig
     * @throws InvalidCartridgeDefinitionException
     * @throws InvalidIaasProviderException
     * @throws org.apache.stratos.cloud.controller.exception.CartridgeDefinitionNotExistsException
     */
    public boolean updateCartridge(Cartridge cartridgeConfig) throws InvalidCartridgeDefinitionException,
            InvalidIaasProviderException,
            CartridgeDefinitionNotExistsException;

    /**
     * Remove a cartridge
     *
     * @param cartridgeType type of the cartridge to be undeployed.
     * @throws InvalidCartridgeTypeException if the cartridge type specified is not a deployed cartridge.
     */
    public boolean removeCartridge(String cartridgeType) throws InvalidCartridgeTypeException;

    /**
     * Add a cartridge group
     *
     * @param servicegroup
     * @throws InvalidServiceGroupException
     */
    public boolean addServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException;

    /**
     * Remove a cartridge group
     *
     * @param name
     * @throws InvalidServiceGroupException
     */
    public boolean removeServiceGroup(String name) throws InvalidServiceGroupException;

    /**
     * Get cartridge group
     *
     * @param groupName
     * @return
     * @throws InvalidServiceGroupException
     */
    public ServiceGroup getServiceGroup(String groupName) throws InvalidServiceGroupException;

    /**
     * Get cartridge group sub group
     *
     * @param groupName
     * @return
     * @throws InvalidServiceGroupException
     */
    public String[] getServiceGroupSubGroups(String groupName) throws InvalidServiceGroupException;

    /**
     * Get cartridges of a cartridge group
     *
     * @param groupName
     * @return
     * @throws InvalidServiceGroupException
     */
    public String[] getServiceGroupCartridges(String groupName) throws InvalidServiceGroupException;

    /**
     * Get cartridge group dependencies
     *
     * @param groupName
     * @return
     * @throws InvalidServiceGroupException
     */
    public Dependencies getServiceGroupDependencies(String groupName) throws InvalidServiceGroupException;

    /**
     * Validate a given {@link Partition} for basic property existence.
     *
     * @param partition partition to be validated.
     * @return whether the partition is a valid one.
     * @throws InvalidPartitionException if the partition is invalid.
     */
    boolean validatePartition(Partition partition) throws InvalidPartitionException;

    /**
     * Validate a given deployment policy
     *
     * @param cartridgeType      type of the cartridge
     * @param networkPartitionId id of network partition to be validated
     * @return whether the policy is a valid one against the given Cartridge.
     * @throws InvalidPartitionException     if the policy contains at least one invalid partition.
     * @throws InvalidCartridgeTypeException if the given Cartridge type is not a valid one.
     */
    boolean validateDeploymentPolicyNetworkPartition(String cartridgeType, String networkPartitionId)
            throws InvalidPartitionException, InvalidCartridgeTypeException;

    /**
     * <p>
     * Registers the details of a newly created service cluster. This will override an already
     * present service cluster, if there is any. A service cluster is uniquely identified by its
     * domain and sub domain combination.
     * </p>
     *
     * @param registrant information about the new subscription.
     * @return whether the registration is successful or not.
     * @throws org.apache.stratos.cloud.controller.exception.CartridgeNotFoundException when the cartridge type
     *                                                                                  requested by this service is
     *                                                                                  not a registered one.
     */
    boolean registerService(Registrant registrant) throws CartridgeNotFoundException;

    /**
     * Start instances with the given instance contexts. Instances startup process will run in background and
     * this method will return with the relevant member contexts.
     *
     * @param instanceContexts An array of instance contexts
     * @return member contexts
     * @throws org.apache.stratos.cloud.controller.exception.CartridgeNotFoundException   if the requested Cartridge
     *                                                                                    type is not a registered one.
     * @throws org.apache.stratos.cloud.controller.exception.InvalidIaasProviderException if the iaas requested is not
     *                                                                                    valid.
     */
    MemberContext[] startInstances(InstanceContext[] instanceContexts) throws CartridgeNotFoundException,
            InvalidIaasProviderException,
            CloudControllerException;

    /**
     * Calling this method will result in termination of the instance with given member id in the given Partition.
     *
     * @param memberId member ID of the instance to be terminated.
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateInstance(String memberId) throws InvalidMemberException, InvalidCartridgeTypeException,
            CloudControllerException;

    public boolean terminateInstanceForcefully(String memberId) throws InvalidCartridgeTypeException;

    /**
     * Calling this method will result in termination of all instances belong
     * to the provided cluster ID.
     *
     * @param clusterId cluster ID of the instance to be terminated.
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateInstances(String clusterId) throws InvalidClusterException;

    /**
     * Update the topology with current cluster status.
     *
     * @param serviceName id of service which the cluster belongs to.
     * @param clusterId   id of the subjected cluster.
     * @param instanceId  id of the cluster instance.
     * @param status      total number of replicas to be set to the controller.
     */
    public boolean updateClusterStatus(String serviceName, String clusterId, String instanceId, ClusterStatus status);

    /**
     * Unregister the service cluster identified by the given cluster id.
     *
     * @param clusterId service cluster id.
     * @throws UnregisteredClusterException if the service cluster requested is not a registered one.
     */
    public boolean unregisterService(String clusterId) throws UnregisteredClusterException;

    /**
     * This method will return the information regarding the given cartridge, if present.
     * Else this will return <code>null</code>.
     *
     * @param cartridgeType type of the cartridge.
     * @return {@link org.apache.stratos.cloud.controller.domain.Cartridge} of the given cartridge type or
     * <code>null</code>.
     * @throws org.apache.stratos.cloud.controller.exception.CartridgeNotFoundException if there is no registered
     *                                                                                  cartridge with this type.
     */
    Cartridge getCartridge(String cartridgeType) throws CartridgeNotFoundException;

    /**
     * Calling this method will result in returning the types of {@link org.apache.stratos.cloud.controller.domain
     * .Cartridge}s
     * registered in Cloud Controller.
     *
     * @return String array containing types of registered {@link org.apache.stratos.cloud.controller.domain.Cartridge}s.
     */
    String[] getCartridges();

    /**
     * Returns the {@link org.apache.stratos.cloud.controller.domain.ClusterContext} object associated with the given cluster id, or null if not found
     *
     * @param clusterId cluster id
     * @return {@link org.apache.stratos.cloud.controller.domain.ClusterContext} object  associated with the given cluster id, or null
     */
    public ClusterContext getClusterContext(String clusterId);

    /**
     * Creates the clusters relevant to an application in the topology model
     *
     * @param appId               application id
     * @param appClustersContexts cluster information holder object
     * @throws ApplicationClusterRegistrationException if the cluster information are null/empty
     */
    public boolean createApplicationClusters(String appId, ApplicationClusterContext[] appClustersContexts) throws
            ApplicationClusterRegistrationException;

    /**
     * Creates a cluster instance with the given information
     *
     * @param serviceType serviceType
     * @param clusterId   cluster id
     * @param alias       alias provided in the subscription parameters
     * @param instanceId  instance id
     * @throws ClusterInstanceCreationException if an y error occurs in cluster instance creation
     */
    public boolean createClusterInstance(String serviceType, String clusterId, String alias,
                                         String instanceId, String partitionId,
                                         String networkPartitionId) throws
            ClusterInstanceCreationException;

    /**
     * Retrieves registered Kubernetes clusters.
     */
    public KubernetesCluster[] getKubernetesClusters();

    /**
     * Retrieves Kubernetes cluster for given Kubernetes cluster ID.
     *
     * @param kubernetesClusterId
     */
    public KubernetesCluster getKubernetesCluster(String kubernetesClusterId)
            throws NonExistingKubernetesClusterException;

    /**
     * Retrieves Kubernetes Master for given Kubernetes cluster ID.
     *
     * @param kubernetesClusterId
     */
    public KubernetesMaster getMasterForKubernetesCluster(String kubernetesClusterId)
            throws NonExistingKubernetesClusterException;

    /**
     * Retrieves Kubernetes Hosts for given Kubernetes cluster ID.
     *
     * @param kubernetesClusterId
     */
    public KubernetesHost[] getHostsForKubernetesCluster(String kubernetesClusterId)
            throws NonExistingKubernetesClusterException;

    /**
     * Register a Kubernetes cluster.
     *
     * @param kubernetesCluster
     * @throws org.apache.stratos.cloud.controller.exception.InvalidKubernetesClusterException
     */
    public boolean addKubernetesCluster(KubernetesCluster kubernetesCluster) throws InvalidKubernetesClusterException,
            KubernetesClusterAlreadyExistsException;

    /**
     * Update a Kubernetes cluster.
     *
     * @param kubernetesCluster
     * @throws org.apache.stratos.cloud.controller.exception.InvalidKubernetesClusterException
     */
    public boolean updateKubernetesCluster(KubernetesCluster kubernetesCluster) throws InvalidKubernetesClusterException,
            KubernetesClusterAlreadyExistsException;

    /**
     * Add a Kubernetes host to a Kubernetes cluster.
     *
     * @param groupId
     * @param kubernetesHost
     * @throws org.apache.stratos.cloud.controller.exception.InvalidKubernetesHostException
     */
    public boolean addKubernetesHost(String groupId, KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException,
            NonExistingKubernetesClusterException;

    /**
     * Update a Kubernetes host.
     *
     * @param kubernetesHost
     * @throws InvalidKubernetesHostException
     */
    public boolean updateKubernetesHost(KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException,
            NonExistingKubernetesHostException;

    /**
     * Remove a Kubernetes host.
     *
     * @param groupId
     * @throws NonExistingKubernetesClusterException
     */
    public boolean removeKubernetesCluster(String groupId) throws NonExistingKubernetesClusterException;

    /**
     * Update a Kubernetes host.
     *
     * @param hostId
     * @throws InvalidKubernetesHostException
     */
    public boolean removeKubernetesHost(String hostId) throws NonExistingKubernetesHostException;

    /**
     * Update a Kubernetes Master in a Kubernetes cluster.
     *
     * @param kubernetesMaster
     * @throws NonExistingKubernetesMasterException
     */
    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException, NonExistingKubernetesMasterException;

    /**
     * Add network partition
     *
     * @param networkPartition
     * @throws NetworkPartitionAlreadyExistsException
     */
    public boolean addNetworkPartition(NetworkPartition networkPartition) throws NetworkPartitionAlreadyExistsException,
            InvalidNetworkPartitionException;

    /**
     * Remove network partition
     *
     * @param networkPartitionId
     * @throws NetworkPartitionNotExistsException
     */
    public boolean removeNetworkPartition(String networkPartitionId) throws NetworkPartitionNotExistsException;

    /**
     * Update network partition
     *
     * @param networkPartition
     * @throws NetworkPartitionNotExistsException
     */
    public boolean updateNetworkPartition(NetworkPartition networkPartition) throws NetworkPartitionNotExistsException;

    /**
     * Get network partitions
     *
     * @return
     */
    public NetworkPartition[] getNetworkPartitions();

    /**
     * Get network partition by network partition id
     *
     * @param networkPartitionId
     * @return
     */
    public NetworkPartition getNetworkPartition(String networkPartitionId);

    /**
     * Remove member from cloud controller side context, topology etc.
     *
     * @param member member to be removed
     */

    /**
     * Remove member from cloud controller side context, topology etc.
     *
     * @param applicationId      app of the member
     * @param cartridgeType      cartridge of the member
     * @param clusterId          cluster of the member
     * @param memberId           id of the member
     * @param networkPartitionId nw partition of the member
     * @param partition          partition of the member
     * @return whether the removal is successful
     */
    boolean removeExpiredObsoletedMemberFromCloudController(String applicationId, String cartridgeType,
                                                            String clusterId, String memberId,
                                                            String networkPartitionId, Partition partition);

    /**
     * Returns the available Iaas Providers
     */
    public String[] getIaasProviders();

}