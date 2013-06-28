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
package org.wso2.carbon.lb.common.service;

import java.util.Map;

import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.util.HostContext;

/**
 * This service provides a way to consume details in loadbalancer.conf file.
 * Also to update the runtime object model of loadbalancer conf.
 */
public interface LoadBalancerConfigurationService {

    /**
     * Provides a reference to the runtime object model of loadbalancer.conf
     * @return {@link Object} which is an instance of {@link LoadBalancerConfiguration} 
     */
    public Object getLoadBalancerConfig();
    
//    /**
//     * Return a {@link Map} of {@link HostContext} objects, built using the given config.
//     * @param config service configuration.
//     * @return {@link Map} {@link Object}
//     */
//    public Object getHostContext(String config);
    
    /**
     * Return a {@link Map} of {@link HostContext} objects, built using the given configuration.
     * @param config service configuration diff. This can be in following format.
     * 
     * <p/>
     * appserver {
     * hosts                   appserver.cloud-test.wso2.com;
     * domains   {
     * 		wso2.as1.domain {
     * 			tenant_range    1-100;
     * 		}
     *		wso2.as2.domain {
     * 			tenant_range    101-200;
     * 		}
     * 		wso2.as3.domain {
     *	 		tenant_range    *;
     * 		}
     * 	}
     * } 
     * <p/>
     * esb {
     * hosts                   esb.cloud-test.wso2.com;
     * domains   {
     * 		wso2.esb.domain {
     *	 		tenant_range    *;
     * 		}
     * 	}
     * }
     * <p/>
     * @return a {@link Map} of {@link HostContext} objects.
     * key - host name
     * Value - {@link HostContext}
     */
    public Object getHostContexts(String config) ;
    
}
