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
package org.apache.stratos.cloud.controller.interfaces;

import org.apache.stratos.cloud.controller.exception.InvalidCartridgeTypeException;
import org.apache.stratos.cloud.controller.exception.InvalidClusterException;
import org.apache.stratos.cloud.controller.exception.InvalidMemberException;
import org.apache.stratos.cloud.controller.exception.InvalidPartitionException;
import org.apache.stratos.cloud.controller.exception.UnregisteredCartridgeException;
import org.apache.stratos.cloud.controller.exception.UnregisteredClusterException;
import org.apache.stratos.cloud.controller.pojo.Registrant;
import org.apache.stratos.cloud.controller.util.CartridgeInfo;
import org.apache.stratos.messaging.domain.policy.DeploymentPolicy;
import org.apache.stratos.messaging.domain.policy.Partition;

/**
 * This API provides a way to communicate with underline
 * Infrastructure which are supported by <i>jClouds</i>.
 * 
 */
public interface CloudControllerService {

    /**
     * Validate a given {@link Partition} for basic property existence.
     * @param partition partition to be validated.
     * @return whether the partition is a valid one.
     * @throws InvalidPartitionException if the partition is invalid.
     */
    public boolean validatePartition(Partition partition) throws InvalidPartitionException;
    
    /**
     * Validate a given {@link DeploymentPolicy} against a Cartridge.
     * @param cartridgeType type of the cartridge that this policy is going to be attached to.
     * @param deploymentPolicy policy to be validated.
     * @return whether the policy is a valid one against the given Cartridge.
     * @throws InvalidPartitionException if the policy contains at least one invalid partition.
     * @throws InvalidCartridgeTypeException if the given Cartridge type is not a valid one.
     */
    public boolean validateDeploymentPolicy(String cartridgeType, DeploymentPolicy deploymentPolicy) 
            throws InvalidPartitionException, InvalidCartridgeTypeException;

    /**
     * <p>
     * Registers the details of a newly created service cluster. This will override an already
     * present service cluster, if there is any. A service cluster is uniquely identified by its
     * domain and sub domain combination.
     * </p>
     * @param clusterContext information about the new subscription.
     * @return whether the registration is successful or not.
     * 
     * @throws UnregisteredCartridgeException
     *             when the cartridge type requested by this service is
     *             not a registered one.
     */
    public boolean registerService(Registrant registrant) throws UnregisteredCartridgeException, 
    IllegalArgumentException;

    /**
     * Calling this method will result in an instance startup, which is belong
     * to the provided Cluster ID. Also note that the instance that is starting up
     * belongs to the group whose name is derived from its Cluster ID, replacing <i>.</i>
     * by a hyphen (<i>-</i>).
     * 
     * @param clusterId
     *            cluster ID of the instance to be started up.
     * @param partition
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be started.
     * @return public IP which is associated with the newly started instance.
     */
    public String startInstance(String clusterId, Partition partition) throws IllegalArgumentException, UnregisteredCartridgeException;

    /**
     * Calling this method will spawn more than one ininstances in the
      * specified partition for the particular cluster
     *
     * @param clusterId
     *            cluster ID of the instance to be started up.
     * @param noOfInstancesToBeSpawned
     *            no of instances to be started up.
     * @param partition
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be started.
     * @return public IP which is associated with the newly started instance.
     */
//    public String startInstances(String clusterId, Partition partition, int noOfInstancesToBeSpawned);
    
    /**
     * Calling this method will result in termination of the instance with given member id in the given Partition.
     * 
     * @param memberId
     *            member ID of the instance to be terminated.
     * @param partition
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be terminated..
     * @return whether an instance terminated successfully or not.
     */
    public void terminateInstance(String memberId) throws InvalidMemberException, InvalidCartridgeTypeException, 
    IllegalArgumentException;
    
    /**
     * Calling this method will result in termination of an instance which is belong
     * to the provided cluster Id and the location scope..
     * 
     * @param clusterId
     *            cluster ID of the instance to be terminated.
     * @param partition
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be terminated..
     * @return whether an instance terminated successfully or not.
     */
//    public boolean terminateInstance(String clusterId, Partition partition);

    /**
     * Calling this method will result in termination of an instance which is belong
     * to the provided cluster Id and the location scope..
     *
     * @param noOfInstances
     *            no of instances to be started up
     * @param clusterId
     *            cluster ID of the instance to be terminated.
     * @param partition
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be terminated..
     * @return whether an instance terminated successfully or not.
     */
//    public boolean terminateInstances(String[] memberIds);

//     /**
//     * Calling this method will result in termination of an instance which is belong
//     * to the provided instance Id.
//     *
//     * @param instancesToBeTerminated
//     *            list of instance Ids to be terminated.
//     * @return whether an instance terminated successfully or not.
//     */
//    public boolean terminateUnhealthyInstances(List<String> instancesToBeTerminated);

    /**
     * Calling this method will result in termination of all instances belong
     * to the provided cluster ID.
     * 
     * @param clusterId
     *            cluster ID of the instance to be terminated.
     * @return whether an instance terminated successfully or not.
     */
    public void terminateAllInstances(String clusterId) throws IllegalArgumentException, InvalidClusterException;

    /**
    /**
     * Unregister the service cluster which represents by this domain and sub domain.
     * @param clusterId service cluster domain
     * @return whether the unregistration was successful or not.
     * @throws org.apache.stratos.cloud.controller.exception.UnregisteredClusterException if the service cluster requested is not a registered one.
     */
    public void unregisterService(String clusterId) throws UnregisteredClusterException;
    
    /**
     * This method will return the information regarding the given cartridge, if present.
     * Else this will return <code>null</code>.
     * 
     * @param cartridgeType
     *            type of the cartridge.
     * @return {@link org.apache.stratos.cloud.controller.util.CartridgeInfo} of the given cartridge type or <code>null</code>.
     * @throws UnregisteredCartridgeException if there is no registered cartridge with this type.
     */
    public CartridgeInfo getCartridgeInfo(String cartridgeType) throws UnregisteredCartridgeException;

//    /**
//     * Calling this method will result in returning the pending instances
//     * count of a particular domain.
//     * 
//     * @param clusterId
//     *            service cluster domain
//     * @return number of pending instances for this domain. If no instances of this
//     *         domain is present, this will return zero.
//     */
//    public int getPendingInstanceCount(String clusterId);

    /**
     * Calling this method will result in returning the types of {@link org.apache.stratos.cloud.controller.util.Cartridge}s
     * registered in Cloud Controller.
     * 
     * @return String array containing types of registered {@link org.apache.stratos.cloud.controller.util.Cartridge}s.
     */
    public String[] getRegisteredCartridges();

}
