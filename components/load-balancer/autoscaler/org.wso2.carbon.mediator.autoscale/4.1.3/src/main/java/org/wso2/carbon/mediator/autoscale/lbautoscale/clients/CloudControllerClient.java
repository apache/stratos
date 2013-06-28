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
package org.wso2.carbon.mediator.autoscale.lbautoscale.clients;

/**
 * Each Implementation which provides access to Cloud Controller, should implement this interface.
 */
public abstract class CloudControllerClient {

    /**
     * Initializes the client.
     */
    public abstract void init();
    
    /**
     * Should start an instance.
     * @param domainName clustering domain.
     * @param subDomainName clustering sub domain.
     * @return Public IP of the spawned instance.
     * @throws Exception 
     */
    public abstract String startInstance(String domainName, String subDomainName) throws Exception;
    
    /**
     * Terminates an instance belongs to the given cluster.
     * @param domainName clustering domain.
     * @param subDomainName clustering sub domain.
     * @return whether the termination is successful or not.
     * @throws Exception
     */
    public abstract boolean terminateInstance(String domainName, String subDomainName) throws Exception;
    
    /**
     * Terminates lastly spawned instance of the given cluster.
     * @param domainName clustering domain.
     * @param subDomainName clustering sub domain.
     * @return whether the termination is successful or not.
     * @throws Exception
     */
    public abstract boolean terminateLastlySpawnedInstance(String domainName, String subDomainName) throws Exception;
    
    /**
     * Return pending instance count of the given cluster.
     * @param domainName clustering domain.
     * @param subDomainName clustering sub domain.
     * @return pending instance count.
     * @throws Exception
     */
    public abstract int getPendingInstanceCount(String domainName, String subDomainName) throws Exception;
}