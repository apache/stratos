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

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.Template;
import org.apache.stratos.cloud.controller.exception.InvalidHostException;
import org.apache.stratos.cloud.controller.exception.InvalidRegionException;
import org.apache.stratos.cloud.controller.exception.InvalidZoneException;
import org.apache.stratos.cloud.controller.pojo.IaasProvider;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;

/**
 * All IaaSes that are going to support by Cloud Controller, should extend this abstract class.
 */
public abstract class Iaas {
	/**
	 * Reference to the corresponding {@link IaasProvider}
	 */
	private IaasProvider iaasProvider;
	
	public Iaas(IaasProvider iaasProvider) {
		this.setIaasProvider(iaasProvider);
	}
	
	public IaasProvider getIaasProvider() {
		return iaasProvider;
	}

	public void setIaasProvider(IaasProvider iaasProvider) {
		this.iaasProvider = iaasProvider;
	}
	
    /**
     * This should build the {@link ComputeService} object and the {@link Template} object,
     * using the information from {@link IaasProvider} and should set the built 
     * {@link ComputeService} object in the {@link IaasProvider#setComputeService(ComputeService)}
     * and also should set the built {@link Template} object in the 
     * {@link IaasProvider#setTemplate(Template)}.
     */
    public abstract void buildComputeServiceAndTemplate();
    
    /**
     * This method provides a way to set payload that can be obtained from {@link IaasProvider#getPayload()}
     * in the {@link Template} of this IaaS.
     */
    public abstract void setDynamicPayload();
    
    /**
     * This will obtain an IP address from the allocated list and associate that IP with this node.
     * @param node Node to be associated with an IP.
     * @return associated public IP.
     */
    public abstract String associateAddress(NodeMetadata node);
    
    /**
     * This will obtain a predefined IP address and associate that IP with this node, if ip is already in use allocate ip from pool 
     * (through associateAddress())
     * @param node Node to be associated with an IP.
     * @ip preallocated floating Ip
     * @return associated public IP.
     */

    abstract public String associatePredefinedAddress(NodeMetadata node, String ip);
    
    /**
     * This will deallocate/release the given IP address back to pool.
     * @param iaasInfo corresponding {@link IaasProvider}
     * @param ip public IP address to be released.
     */
    public abstract void releaseAddress(String ip);
    
    /**
     * This method should create a Key Pair corresponds to a given public key in the respective region having the name given.
     * Also should override the value of the key pair in the {@link Template} of this IaaS.
     * @param region region that the key pair will get created.
     * @param keyPairName name of the key pair. NOTE: Jclouds adds a prefix : <code>jclouds#</code>
     * @param publicKey public key, from which the key pair will be created.
     * @return whether the key pair creation is successful or not.
     */
    public abstract boolean createKeyPairFromPublicKey(String region, String keyPairName, String publicKey);
    
    /**
     * Validate a given region name against a particular IaaS.
     * If a particular IaaS doesn't have a concept called region, it can simply throw {@link InvalidRegionException}.
     * @param region name of the region.
     * @return whether the region is valid.
     * @throws InvalidRegionException if the region is invalid.
     */
    public abstract boolean isValidRegion(String region) throws InvalidRegionException;
    
    /**
     * Validate a given zone name against a particular region in an IaaS.
     * If a particular IaaS doesn't have a concept called zone, it can simply throw {@link InvalidZoneException}.
     * @param region region of the IaaS that the zone belongs to.
     * @param zone 
     * @return whether the zone is valid in the given region or not.
     * @throws InvalidZoneException if the zone is invalid in a given region.
     */
    public abstract boolean isValidZone(String region, String zone) throws InvalidZoneException;
    
    /**
     * Validate a given host id against a particular zone in an IaaS.
     * If a particular IaaS doesn't have a concept called hosts, it can simply throw {@link InvalidHostException}.
     * @param zone zone of the IaaS that the host belongs to.
     * @param host
     * @return whether the host is valid in the given zone or not.
     * @throws InvalidHostException if the host is invalid in a given zone.
     */
    public abstract boolean isValidHost(String zone, String host) throws InvalidHostException;
    
    /**
     * provides the {@link PartitionValidator} corresponds to this particular IaaS.
     * @return {@link PartitionValidator}
     */
    public abstract PartitionValidator getPartitionValidator();

    /**
     * Builds only the jclouds {@link Template}
     */
    public abstract void buildTemplate();
    
    /**
     * Create a new volume in the respective Iaas.
     * @param sizeGB size of the volume in Giga Bytes.
     * @return Id of the created volume.
     */
    public abstract String createVolume(int sizeGB);
   
    /**
     * Attach a given volume to an instance at the specified device path.
     * @param instanceId of the instance.
     * @param volumeId volume id of the volume to be attached.
     * @param deviceName name of the device that the volume would bind to.
     * @return the status of the attachment.
     */
    public abstract String attachVolume(String instanceId, String volumeId, String deviceName);
    
    /**
     * Detach a given volume from the given instance.
     * @param instanceId of the instance.
     * @param volumeId volume id of the volume to be detached.
     */
    public abstract void detachVolume(String instanceId, String volumeId);
    
    /**
     * Delete a given volume.
     * @param volumeId volume id of the volume to be detached.
     */
    public abstract void deleteVolume(String volumeId);

    /*
    This returns the device of the volume specified by the user. This is depends on IAAS.
    For an instance /dev/sdf maps to /dev/xvdf in EC2
     */

    public abstract String getIaasDevice(String device);
}
