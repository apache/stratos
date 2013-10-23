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

import org.apache.stratos.cloud.controller.exception.UnregisteredCartridgeException;
import org.apache.stratos.cloud.controller.exception.UnregisteredServiceException;
import org.apache.stratos.cloud.controller.util.CartridgeInfo;
import org.apache.stratos.cloud.controller.util.LocationScope;
import org.apache.stratos.cloud.controller.util.Properties;

/**
 * This Interface provides a way to communicate with underline
 * Infrastructure which are supported by <i>JClouds</i>.
 * 
 */
public interface CloudControllerService {


    /**
     * <p>
     * Registers the details of a newly created service cluster. This will override an already
     * present service cluster, if there is any. A service cluster is uniquely identified by its
     * domain and sub domain combination.
     * </p>
     * @param clusterId
     *            service cluster domain
     * @param tenantRange
     * 			  tenant range eg: '1-10' or '2'
     * @param cartridgeType
     *            cartridge type of the new service. This should be an already registered cartridge
     *            type.
     * @param hostName
     * 			  host name of this service instance
     * @param properties
     * 			  Set of properties related to this service definition.
     * @param payload
     *            payload which will be passed to instance to be started. Payload shouldn't contain 
     *            xml tags.
     * @return whether the registration is successful or not.
     * 
     * @throws UnregisteredCartridgeException
     *             when the cartridge type requested by this service is
     *             not a registered one.
     */
    public boolean registerService(String clusterId, String tenantRange, String cartridgeType,
        String hostName, Properties properties, byte[] payload, String autoScalerPolicyName) throws UnregisteredCartridgeException;

    /**
     * Calling this method will result in an instance startup, which is belong
     * to the provided Cluster ID. Also note that the instance that is starting up
     * belongs to the group whose name is derived from its Cluster ID, replacing <i>.</i>
     * by a hyphen (<i>-</i>).
     * 
     * @param clusterId
     *            cluster ID of the instance to be started up.
     * @param locationScope
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be started.
     * @return public IP which is associated with the newly started instance.
     */
    public String startInstance(String clusterId, LocationScope locationScope);
    
    /**
     * Calling this method will result in termination of an instance which is belong
     * to the provided cluster Id and the location scope..
     * 
     * @param clusterId
     *            cluster ID of the instance to be terminated.
     * @param locationScope
     *            It contains the region, zone, network and host of a IaaS where
     *            an instance need to be terminated..
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateInstance(String clusterId, LocationScope locationScope);

     /**
     * Calling this method will result in termination of an instance which is belong
     * to the provided instance Id.
     *
     * @param instanceId
     *            instance Id of the instance to be terminated.
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateInstance(String instanceId);

    /**
     * Calling this method will result in termination of all instances belong
     * to the provided cluster ID.
     * 
     * @param clusterId
     *            cluster ID of the instance to be terminated.
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateAllInstances(String clusterId);

    
    /**
     * Calling this method will result in termination of the lastly spawned instance which is
     * belong to the provided clusterID.
     * 
     * @param clusterId
     *            service domain of the instance to be terminated.
     * @return whether the termination is successful or not.
     */
    public boolean terminateLastlySpawnedInstance(String clusterId);

    /**
     * Unregister the service cluster which represents by this domain and sub domain.
     * @param clusterId service cluster domain
     * @return whether the unregistration was successful or not.
     * @throws org.apache.stratos.cloud.controller.exception.UnregisteredServiceException if the service cluster requested is not a registered one.
     */
    public boolean unregisterService(String clusterId) throws UnregisteredServiceException;
    
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

    /**
     * Calling this method will result in returning the pending instances
     * count of a particular domain.
     * 
     * @param clusterId
     *            service cluster domain
     * @return number of pending instances for this domain. If no instances of this
     *         domain is present, this will return zero.
     */
    public int getPendingInstanceCount(String clusterId);

    /**
     * Calling this method will result in returning the types of {@link org.apache.stratos.cloud.controller.util.Cartridge}s
     * registered in Cloud Controller.
     * 
     * @return String array containing types of registered {@link org.apache.stratos.cloud.controller.util.Cartridge}s.
     */
    public String[] getRegisteredCartridges();

}
