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
import org.apache.stratos.cloud.controller.util.IaasProvider;
import org.apache.stratos.cloud.controller.validate.interfaces.PartitionValidator;

/**
 * All IaaSes that are going to support by Cloud Controller, should extend this abstract class.
 */
public abstract class Iaas {
    
    /**
     * This should build the {@link ComputeService} object and the {@link Template} object,
     * using the information from {@link IaasProvider} and should set the built 
     * {@link ComputeService} object in the {@link IaasProvider#setComputeService(ComputeService)}
     * and also should set the built {@link Template} object in the 
     * {@link IaasProvider#setTemplate(Template)}.
     * @param iaasInfo corresponding {@link IaasProvider}
     */
    public abstract void buildComputeServiceAndTemplate(IaasProvider iaasInfo);
    
    /**
     * This method provides a way to set payload that can be obtained from {@link IaasProvider#getPayload()}
     * in the {@link Template} of this IaaS.
     * @param iaasInfo corresponding {@link IaasProvider}
     */
    public abstract void setDynamicPayload(IaasProvider iaasInfo);
    
    /**
     * This will obtain an IP address from the allocated list and associate that IP with this node.
     * @param iaasInfo corresponding {@link IaasProvider}
     * @param node Node to be associated with an IP.
     * @return associated public IP.
     */
    public abstract String associateAddress(IaasProvider iaasInfo, NodeMetadata node);
    
    /**
     * This will deallocate/release the given IP address back to pool.
     * @param iaasInfo corresponding {@link IaasProvider}
     * @param ip public IP address to be released.
     */
    public abstract void releaseAddress(IaasProvider iaasInfo, String ip);
    
    /**
     * This method should create a Key Pair corresponds to a given public key in the respective region having the name given.
     * Also should override the value of the key pair in the {@link Template} of this IaaS.
     * @param iaasInfo {@link IaasProvider} 
     * @param region region that the key pair will get created.
     * @param keyPairName name of the key pair. NOTE: Jclouds adds a prefix : <code>jclouds#</code>
     * @param publicKey public key, from which the key pair will be created.
     * @return whether the key pair creation is successful or not.
     */
    public abstract boolean createKeyPairFromPublicKey(IaasProvider iaasInfo, String region, String keyPairName, String publicKey);
    
    /**
     * Validate a given region name against a particular IaaS.
     * If a particular IaaS doesn't have a concept called region, it can simply return false.
     * @param iaasInfo {@link IaasProvider} 
     * @param region name of the region.
     * @return whether the region is valid or not.
     */
    public abstract boolean isValidRegion(IaasProvider iaasInfo, String region);
    
    /**
     * Validate a given zone name against a particular region in an IaaS.
     * If a particular IaaS doesn't have a concept called zone, it can simply return false.
     * @param iaasInfo {@link IaasProvider} 
     * @param region region of the IaaS that the zone belongs to.
     * @param zone 
     * @return whether the zone is valid in the given region or not.
     */
    public abstract boolean isValidZone(IaasProvider iaasInfo, String region, String zone);
    
    /**
     * Validate a given host id against a particular zone in an IaaS.
     * If a particular IaaS doesn't have a concept called hosts, it can simply return false.
     * @param iaasInfo {@link IaasProvider} 
     * @param zone zone of the IaaS that the host belongs to.
     * @param host
     * @return whether the host is valid in the given zone or not.
     */
    public abstract boolean isValidHost(IaasProvider iaasInfo, String zone, String host);
    
    /**
     * provides the {@link PartitionValidator} corresponds to this particular IaaS.
     * @return {@link PartitionValidator}
     */
    public abstract PartitionValidator getPartitionValidator();

    public abstract void buildTemplate(IaasProvider iaas);
    
}
