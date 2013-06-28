/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.autoscaler.service;

import org.wso2.carbon.lb.common.conf.util.Constants;

/**
 * This Interface provides a way for a component, to communicate with an underline
 * Infrastructure which are supported by <i>JClouds</i>.
 * 
 */
public interface IAutoscalerService {
    
    /**
     * Initialize the service.
     * @param isSpi if this service is to be used by SPI, this parameter should be set to
     * true. When this is true, you should specify an image id, each time you
     * are starting an instance i.e. you should use {@link #startSpiInstance(String, String)}
     * method, instead of using {@link #startInstance(String)}.
     * @return
     */
    public boolean initAutoscaler(boolean isSpi);

    
    /**
     * Calling this method will result in an instance startup, which is belong
     * to the provided service domain. This method is non-blocking, means we do not
     * wait till the instance is started up. Also note that the instance that is starting up
     * belongs to the group whose name is derived from its service domain, replacing <i>.</i>
     * by a hyphen (<i>-</i>). 
     * @param domainName service clustering domain of the instance to be started up.
     * @param sudDomainName service clustering sub domain of the instance to be started up.
     * If this is null, the default value will be used. Default value is 
     * {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return whether the starting up is successful or not.
     */
    public boolean startInstance(String domainName, String sudDomainName);
    
    /**
     * Calling this method will result in an instance startup, which is belong
     * to the provided service domain. This method will return the public IP address of
     * the instance that is started. Thus, this method is blocking, since we need to 
     * return the IP Address and for that we have to wait till the instance started up.
     * @param domainName should be in following format.
     * <code>${service-domain}\t\${tenant-id}</code>.
     * @param imageId starting instance will be an instance of this image. Image id should
     * be a valid one.
     * @param sudDomainName service clustering sub domain of the instance to be started up.
     * If this is null, the default value will be used. Default value is 
     * {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return public IP address of the instance in String format. If instance failed to 
     * start, this will return an empty String.
     */
    public String startSpiInstance(String domainName, String subDomainName, String imageId);
    
   
    /**
     * Calling this method will result in termination of an instance which is belong
     * to the provided service domain.
     * @param domainName service domain of the instance to be terminated.
     * @param sudDomainName service clustering sub domain of the instance to be started up.
     * If this is null, the default value will be used. Default value is {@link Constants}.DEFAULT_SUB_DOMAIN.
     * @return whether an instance terminated successfully or not.
     */
	public boolean terminateInstance(String domainName, String subDomainName);
	
	/**
	 * Calling this method will result in termination of the lastly spawned instance which is
	 * belong to the provided service domain.
	 * @param domainName service domain of the instance to be terminated.
	 * @param sudDomainName service clustering sub domain of the instance to be started up.
     * If this is null, the default value will be used. Default value is {@link Constants}.DEFAULT_SUB_DOMAIN.
	 * @return whether the termination is successful or not.
	 */
	public boolean terminateLastlySpawnedInstance(String domainName, String subDomainName);
	
	/**
     * Calling this method will result in termination of an instance which has the
     * provided public IP address.
     * @param publicIp public IP address of the instance to be terminated.
     * @return whether the instance terminated successfully or not.
     */
    public boolean terminateSpiInstance(String publicIp);
	
	/**
	 * Calling this method will result in returning the pending instances
	 * count of a particular domain.
	 * @param domainName service domain
	 * @param sudDomainName service clustering sub domain of the instance to be started up.
     * If this is null, the default value will be used. Default value is {@link Constants}.DEFAULT_SUB_DOMAIN.
	 * @return number of pending instances for this domain. If no instances of this 
	 * domain is present, this will return zero.
	 */
	public int getPendingInstanceCount(String domainName, String subDomainName);
	
}
