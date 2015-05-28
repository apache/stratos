/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.aws.extension;

import java.util.HashMap;
import java.util.List;

import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.elasticloadbalancing.model.Listener;



public class AWSLoadBalancer implements LoadBalancer {
	
	// A map <clusterId, load balancer id>
	private HashMap<String, String> clusterToLoadBalancer;

	private AWSHelper awsHelper;
	
	public AWSLoadBalancer()
	{
		clusterToLoadBalancer = new HashMap<String, String>();
		awsHelper = new AWSHelper();
	}
	
	public boolean configure(Topology topology) throws LoadBalancerExtensionException {
		
		for (Service service : topology.getServices()) {
			
			List<Listener> listenersForThisService = awsHelper.getRequiredListeners(service);
			
            for (Cluster cluster : service.getClusters()) {
                
            	// Check if a load balancer is created for this cluster
            	
            	if(clusterToLoadBalancer.containsKey(cluster.getClusterId()))
    			{
            		// Get the load balancer
            		// Update it 
    			}
            	else
            	{
            		String loadBalancerName =service.getServiceName()+"-"+cluster.getClusterId();
            		
            		String loadBalancerDNSName = awsHelper.createLoadBalancer(loadBalancerName, listenersForThisService);
            		
            		// register instances to LB
            		
            		// Create domain mappings
            	}
            	
            }
        }
		
		return true;
	}
	
	public void start() throws LoadBalancerExtensionException {
		
	}
	
	public void reload() throws LoadBalancerExtensionException {
		
	}
	     
	public void stop() throws LoadBalancerExtensionException {
		
	}	
}
