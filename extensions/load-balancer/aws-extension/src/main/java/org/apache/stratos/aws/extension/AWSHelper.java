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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.*;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult;
import com.amazonaws.services.opsworks.model.AttachElasticLoadBalancerRequest;


public class AWSHelper 
{
	private String awsAccessKey;
	private String awsSecretKey;
	private String httpProxy;
	private String availabilityZone;

	private BasicAWSCredentials awsCredentials;
	private ClientConfiguration clientConfiguration;
	
	private static final Log log = LogFactory.getLog(AWSHelper.class);
	
	public AWSHelper()
	{
		// Read values for awsAccessKey, awsSecretKey etc. from config file
		// Throw a proper exception / log warning if cant read credentials ?
		
		awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
		clientConfiguration = new ClientConfiguration();
	}
	
	/*
	 * Creates a load balancer and returns its DNS name.
	 */
	public String createLoadBalancer(String name, List<Listener> listeners)
	{
		try
		{
			CreateLoadBalancerRequest createLoadBalancerRequest = new CreateLoadBalancerRequest(name);
			
			createLoadBalancerRequest.setListeners(listeners);
			
			Set<String> availabilityZones = new HashSet<String>();
			availabilityZones.add(availabilityZone);
			
			createLoadBalancerRequest.setAvailabilityZones(availabilityZones);
			
			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(awsCredentials, clientConfiguration);
			
			CreateLoadBalancerResult clbResult = lbClient.createLoadBalancer(createLoadBalancerRequest);
			
			return clbResult.getDNSName();
		}
		catch(Exception e)
		{
			log.error("Could not create load balancer " + name + ".");
			return null;
		}
	}
	
	public void registerInstancesToLoadBalancer(String loadBalancerName, List<Instance> instances)
	{
		try
		{
			RegisterInstancesWithLoadBalancerRequest registerInstancesWithLoadBalancerRequest = new RegisterInstancesWithLoadBalancerRequest(loadBalancerName, instances);
		
			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(awsCredentials, clientConfiguration);
			
			RegisterInstancesWithLoadBalancerResult result = lbClient.registerInstancesWithLoadBalancer(registerInstancesWithLoadBalancerRequest);
		}
		catch(Exception e)
		{
			log.error("Could not register instances to load balancer " + loadBalancerName);
		}
	}
	
	public void deregisterInstancesFromLoadBalancer(String loadBalancerName, List<Instance> instances)
	{
		try
		{
			DeregisterInstancesFromLoadBalancerRequest deregisterInstancesFromLoadBalancerRequest = new DeregisterInstancesFromLoadBalancerRequest(loadBalancerName, instances);
		
			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(awsCredentials, clientConfiguration);
			
			DeregisterInstancesFromLoadBalancerResult result = lbClient.deregisterInstancesFromLoadBalancer(deregisterInstancesFromLoadBalancerRequest);
		}
		catch(Exception e)
		{
			log.error("Could not de-register instances from load balancer " + loadBalancerName);
		}
	}
	
	public List<Instance> getAttachedInstances(String loadBalancerName)
	{
		return new ArrayList<Instance>();
	}
	
	public void addListenersToLoadBalancer(String loadBalancerName, List<Listener> listeners)
	{
		try
		{
			
		}
		catch(Exception e)
		{
			log.error("Could not add listeners to load balancer " + loadBalancerName);
		}
	}
	
	public void removeListenersFromLoadBalancer(String loadBalancerName, List<Listener> listeners)
	{
		try
		{
			
		}
		catch(Exception e)
		{
			log.error("Could not remove listeners from load balancer " + loadBalancerName);
		}
	}
	
	public List<Listener> getAttachedListeners(String loadBalancerName)
	{
		return new ArrayList<Listener>();
	}
	
	public List<Listener> getRequiredListeners(Service service)
	{
		List<Listener> listeners = new ArrayList<Listener>();
		
		for(Port port : service.getPorts())
		{
			int instancePort = port.getValue();
			// Read other values as well and create a listener object
			
			
		}
		
		return listeners;
	}
}
