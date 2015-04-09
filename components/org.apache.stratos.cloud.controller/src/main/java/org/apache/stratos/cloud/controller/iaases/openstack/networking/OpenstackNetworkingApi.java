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
package org.apache.stratos.cloud.controller.iaases.openstack.networking;

import org.jclouds.compute.domain.NodeMetadata;

import java.util.List;

/**
 * Openstack can support Neutron Network or Nova Network, but not both at same time.
 * Each have different ways of assigning and releasing Floating IPs.
 * Networking operation should be implemented by implementing this interface
 * for different openstack networks.
 *
 * @author rajkumar
 */
public interface OpenstackNetworkingApi {
    /**
     * Associate FloatingIPs to the given node.
     * NetworkInterfaces, FloatingNetworks and fixed IPs details
     * will be retrieved from cartridge deployment.
     *
     * @param node the node to be associated with FloatingIPs
     * @return list of associated FloatinigIPs
     */
    public abstract List<String> associateAddresses(NodeMetadata node);

    /**
     * Associate the given FloatingIP to the given node.
     * NetworkInterfaces, FloatingNetworks and fixed IPs details
     * will be retrieved from cartridge deployment.
     *
     * @param node Node to be associated with FloatingIPs
     * @return list of associated FloatinigIPs
     */
    public abstract String associatePredefinedAddress(NodeMetadata node, String ip);

    /**
     * Deallocate/delete the given FloatingIP.
     *
     * @param ip FloatingIP address to be released.
     */
    public abstract void releaseAddress(String ip);
}