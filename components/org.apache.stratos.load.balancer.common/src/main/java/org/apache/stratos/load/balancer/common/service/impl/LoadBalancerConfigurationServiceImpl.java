/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.common.service.impl;

import org.apache.stratos.load.balancer.common.conf.LoadBalancerConfiguration;
import org.apache.stratos.load.balancer.common.conf.structure.Node;
import org.apache.stratos.load.balancer.common.conf.structure.NodeBuilder;
import org.apache.stratos.load.balancer.common.conf.util.Constants;
import org.apache.stratos.load.balancer.common.service.LoadBalancerConfigurationService;

public class LoadBalancerConfigurationServiceImpl implements
		LoadBalancerConfigurationService {

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

		LoadBalancerConfiguration.getInstance().createServicesConfig(rootNode);

		return LoadBalancerConfiguration.getInstance().getHostContextMap();
	}

}
