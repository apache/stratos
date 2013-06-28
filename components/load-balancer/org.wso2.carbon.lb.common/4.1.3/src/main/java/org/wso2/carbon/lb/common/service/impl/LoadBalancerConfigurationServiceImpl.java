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
package org.wso2.carbon.lb.common.service.impl;

import java.util.Map;

import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.structure.Node;
import org.wso2.carbon.lb.common.conf.structure.NodeBuilder;
import org.wso2.carbon.lb.common.conf.util.Constants;
import org.wso2.carbon.lb.common.conf.util.HostContext;
import org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

public class LoadBalancerConfigurationServiceImpl implements LoadBalancerConfigurationService {

    @Override
    public Object getLoadBalancerConfig() {
        return LoadBalancerConfiguration.getInstance();
    }

	@Override
    public Object getHostContexts(String config) {

		// build a Node object for whole loadbalancer.conf
        Node rootNode = new Node();
        rootNode.setName(Constants.SERVICES_ELEMENT);
        rootNode = NodeBuilder.buildNode(rootNode, config);
		
        Map<String, HostContext> oldMap = LoadBalancerConfiguration.getInstance().getHostContextMap();
        LoadBalancerConfiguration.getInstance().createServicesConfig(rootNode);
        
//        MapDifference<String, HostContext> diff = Maps.difference(LoadBalancerConfiguration.getInstance().getHostContextMap(),
//                                                             oldMap );
//		
//		return diff.entriesOnlyOnLeft();
        return LoadBalancerConfiguration.getInstance().getHostContextMap();
    }

//	@Override
//    public Object getHostContext(String config) {
//	    return null;
//    }

}
