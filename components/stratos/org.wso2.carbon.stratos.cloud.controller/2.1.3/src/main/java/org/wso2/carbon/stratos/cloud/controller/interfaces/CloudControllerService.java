/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.stratos.cloud.controller.interfaces;

import org.wso2.carbon.lb.common.conf.util.Constants;
import org.wso2.carbon.stratos.cloud.controller.exception.UnregisteredCartridgeException;
import org.wso2.carbon.stratos.cloud.controller.exception.UnregisteredServiceException;
import org.wso2.carbon.stratos.cloud.controller.util.Cartridge;
import org.wso2.carbon.stratos.cloud.controller.util.CartridgeInfo;
import org.wso2.carbon.stratos.cloud.controller.util.Properties;

/**
 * This Interface provides a way to communicate with underline
 * Infrastructure which are supported by <i>JClouds</i>.
 * 
 */
public interface CloudControllerService {

    /**
     * Creates a key pair in all IaaSes that are configured for the given cartridge,
     * having the given name and public key.
     * 
     * <p/>
     * <h4>Supported Formats</h4>
     * <ul>
     * <li>OpenSSH public key format (e.g., the format in ~/.ssh/authorized_keys)</li>
     * <li>Base64 encoded DER format</li>
     * <li>SSH public key file format as specified in RFC4716</li>
     * </ul>
     * DSA keys are not supported. Make sure your key generator is set up to create RSA keys.
     * <p/>
     * Supported lengths: 1024, 2048, and 4096.
     * <p/>
     * 
     * @param cartridgeType
     *            type of the cartridge. Note this cartridge type should be already
     *            registered one.
     * @param keyPairName
     *            name of the key pair which is going to get created in IaaSes.
     * @param publicKey
     *            The public key.
     * 
     */
	@Deprecated
    public boolean createKeyPairFromPublicKey(String cartridgeType, String keyPairName,
        String publicKey);

    /**
     * This method will return the information regarding the given cartridge, if present.
     * Else this will return <code>null</code>.
     * 
     * @param cartridgeType
     *            type of the cartridge.
     * @return {@link CartridgeInfo} of the given cartridge type or <code>null</code>.
     * @throws UnregisteredCartridgeException if there is no registered cartridge with this type.
     */
    public CartridgeInfo getCartridgeInfo(String cartridgeType) throws UnregisteredCartridgeException;

    /**
     * Calling this method will result in returning the pending instances
     * count of a particular domain.
     * 
     * @param domainName
     *            service cluster domain
     * @param sudDomainName
     *            service clustering sub domain of the instance to be started up.
     *            If this is null, the default value will be used. Default value is
     *            {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return number of pending instances for this domain. If no instances of this
     *         domain is present, this will return zero.
     */
    public int getPendingInstanceCount(String domainName, String subDomainName);

    /**
     * Calling this method will result in returning the types of {@link Cartridge}s
     * registered in Cloud Controller.
     * 
     * @return String array containing types of registered {@link Cartridge}s.
     */
    public String[] getRegisteredCartridges();
    
    /**
     * <p>
     * Registers the details of a newly created service cluster. This will override an already
     * present service cluster, if there is any. A service cluster is uniquely identified by its
     * domain and sub domain combination.
     * </p>
     * @param domain
     *            service cluster domain
     * @param subDomain
     *            service cluster sub domain
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
    public boolean registerService(String domain, String subDomain, String tenantRange, String cartridgeType,
        String hostName, Properties properties, byte[] payload) throws UnregisteredCartridgeException;

    /**
     * Calling this method will result in an instance startup, which is belong
     * to the provided service domain. Also note that the instance that is starting up
     * belongs to the group whose name is derived from its service domain, replacing <i>.</i>
     * by a hyphen (<i>-</i>).
     * 
     * @param domainName
     *            service clustering domain of the instance to be started up.
     * @param subDomainName
     *            service clustering sub domain of the instance to be started up.
     *            If this is null, the default value will be used. Default value is
     *            {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return public IP which is associated with the newly started instance.
     */
    public String startInstance(String domainName, String subDomainName);

    /**
     * Calling this method will result in termination of all instances belong
     * to the provided service domain and sub domain.
     * 
     * @param domainName
     *            service domain of the instance to be terminated.
     * @param sudDomainName
     *            service clustering sub domain of the instances to be terminated.
     *            If this is null, the default value will be used. Default value is
     *            {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateAllInstances(String domainName, String subDomainName);

    /**
     * Calling this method will result in termination of an instance which is belong
     * to the provided service domain and sub domain.
     * 
     * @param domainName
     *            service domain of the instance to be terminated.
     * @param sudDomainName
     *            service clustering sub domain of the instance to be terminated.
     *            If this is null, the default value will be used. Default value is
     *            {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return whether an instance terminated successfully or not.
     */
    public boolean terminateInstance(String domainName, String subDomainName);
    
    /**
     * Calling this method will result in termination of the lastly spawned instance which is
     * belong to the provided service domain and sub domain.
     * 
     * @param domainName
     *            service domain of the instance to be terminated.
     * @param sudDomainName
     *            service clustering sub domain of the instance to be terminated.
     *            If this is null, the default value will be used. Default value is
     *            {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return whether the termination is successful or not.
     */
    public boolean terminateLastlySpawnedInstance(String domainName, String subDomainName);

    /**
     * Unregister the service cluster which represents by this domain and sub domain.
     * @param domain service cluster domain
     * @param subDomain service cluster sub domain
     * @return whether the unregistration was successful or not.
     * @throws UnregisteredServiceException if the service cluster requested is not a registered one.
     */
    public boolean unregisterService(String domain, String subDomain) throws UnregisteredServiceException;

}
