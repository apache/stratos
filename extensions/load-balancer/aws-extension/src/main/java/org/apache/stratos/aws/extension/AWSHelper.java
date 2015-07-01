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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.*;

/**
 * @author swapnil
 * 
 */
public class AWSHelper {
	private String awsAccessKey;
	private String awsSecretKey;
	private String availabilityZone;
	private String region;

	private BasicAWSCredentials awsCredentials;
	private ClientConfiguration clientConfiguration;

	private static final Log log = LogFactory.getLog(AWSHelper.class);

	public AWSHelper() throws LoadBalancerExtensionException {
		// Read values for awsAccessKey, awsSecretKey etc. from config file
		// Throw a proper exception / log warning if cant read credentials ?

		String awsCredentialsFile = System
				.getProperty(Constants.AWS_CREDENTIALS_FILE);

		Properties properties = new Properties();

		InputStream inputStream = null;

		try {
			inputStream = new FileInputStream(awsCredentialsFile);

			properties.load(inputStream);

			this.awsAccessKey = properties
					.getProperty(Constants.AWS_ACCESS_KEY);
			this.awsSecretKey = properties
					.getProperty(Constants.AWS_SECRET_KEY);
			this.availabilityZone = properties
					.getProperty(Constants.AVAILABILITY_ZONE_KEY);
			this.region = properties.getProperty(Constants.REGION_KEY);
		} catch (IOException e) {
			log.error("Error reading aws configuration file.");
			throw new LoadBalancerExtensionException(
					"Error reading aws configuration file.", e);
		} finally {
			try {
				inputStream.close();
			} catch (Exception e) {
				log.warn("Failed to close input stream to aws configuration file.");
			}
		}

		awsCredentials = new BasicAWSCredentials(awsAccessKey, awsSecretKey);
		clientConfiguration = new ClientConfiguration();
	}

	/**
	 * Creates a load balancer and returns its DNS name. Useful when a new
	 * cluster is added.
	 * 
	 * @param name
	 * @param listeners
	 * @return DNS name of newly created load balancer
	 */
	public String createLoadBalancer(String name, List<Listener> listeners) {
		try {
			CreateLoadBalancerRequest createLoadBalancerRequest = new CreateLoadBalancerRequest(
					name);

			createLoadBalancerRequest.setListeners(listeners);

			Set<String> availabilityZones = new HashSet<String>();
			availabilityZones.add(availabilityZone);

			createLoadBalancerRequest.setAvailabilityZones(availabilityZones);

			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
					awsCredentials, clientConfiguration);
			lbClient.setEndpoint("elasticloadbalancing." + this.region
					+ ".amazonaws.com");

			CreateLoadBalancerResult clbResult = lbClient
					.createLoadBalancer(createLoadBalancerRequest);

			return clbResult.getDNSName();
		} catch (Exception e) {
			log.error("Could not create load balancer : " + name + ".");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Deletes the load balancer with the name provided. Useful when a cluster,
	 * with which this load balancer was associated, is removed.
	 * 
	 * @param loadBalancerName
	 */
	public void deleteLoadBalancer(String loadBalancerName) {
		try {
			DeleteLoadBalancerRequest deleteLoadBalancerRequest = new DeleteLoadBalancerRequest();
			deleteLoadBalancerRequest.setLoadBalancerName(loadBalancerName);

			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
					awsCredentials, clientConfiguration);

			lbClient.deleteLoadBalancer(deleteLoadBalancerRequest);
		} catch (Exception e) {
			log.error("Could not delete load balancer : " + loadBalancerName);
		}
	}

	/**
	 * Attaches provided instances to the load balancer. Useful when new
	 * instances get added to the cluster with which this load balancer is
	 * associated.
	 * 
	 * @param loadBalancerName
	 * @param instances
	 */
	public void registerInstancesToLoadBalancer(String loadBalancerName,
			List<Instance> instances) {
		try {
			RegisterInstancesWithLoadBalancerRequest registerInstancesWithLoadBalancerRequest = new RegisterInstancesWithLoadBalancerRequest(
					loadBalancerName, instances);

			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
					awsCredentials, clientConfiguration);

			RegisterInstancesWithLoadBalancerResult result = lbClient
					.registerInstancesWithLoadBalancer(registerInstancesWithLoadBalancerRequest);
		} catch (Exception e) {
			log.error("Could not register instances to load balancer "
					+ loadBalancerName);
		}
	}

	/**
	 * Detaches provided instances from the load balancer, associated with some
	 * cluster. Useful when instances are removed from the cluster with which
	 * this load balancer is associated.
	 * 
	 * @param loadBalancerName
	 * @param instances
	 */
	public void deregisterInstancesFromLoadBalancer(String loadBalancerName,
			List<Instance> instances) {
		try {
			DeregisterInstancesFromLoadBalancerRequest deregisterInstancesFromLoadBalancerRequest = new DeregisterInstancesFromLoadBalancerRequest(
					loadBalancerName, instances);

			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
					awsCredentials, clientConfiguration);

			DeregisterInstancesFromLoadBalancerResult result = lbClient
					.deregisterInstancesFromLoadBalancer(deregisterInstancesFromLoadBalancerRequest);
		} catch (Exception e) {
			log.error("Could not de-register instances from load balancer "
					+ loadBalancerName);
		}
	}

	/**
	 * Returns description of the load balancer which is helpful in determining
	 * instances, listeners associated with load balancer
	 * 
	 * @param loadBalancerName
	 * @return description of the load balancer
	 */
	private LoadBalancerDescription getLoadBalancerDescription(
			String loadBalancerName) {
		List<String> loadBalancers = new ArrayList<String>();

		loadBalancers.add(loadBalancerName);

		DescribeLoadBalancersRequest describeLoadBalancersRequest = new DescribeLoadBalancersRequest(
				loadBalancers);

		AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
				awsCredentials, clientConfiguration);

		DescribeLoadBalancersResult result = lbClient
				.describeLoadBalancers(describeLoadBalancersRequest);

		if (result.getLoadBalancerDescriptions() == null
				|| result.getLoadBalancerDescriptions().size() == 0)
			return null;
		else
			return result.getLoadBalancerDescriptions().get(0);
	}

	/**
	 * Returns instances attached to the load balancer. Useful when deciding if
	 * all attached instances are required or some should be detached.
	 * 
	 * @param loadBalancerName
	 * @return list of instances attached
	 */
	public List<Instance> getAttachedInstances(String loadBalancerName) {
		try {
			LoadBalancerDescription lbDescription = getLoadBalancerDescription(loadBalancerName);

			if (lbDescription == null) {
				log.warn("Could not find description of load balancer "
						+ loadBalancerName);
				return null;
			}

			return lbDescription.getInstances();

		} catch (Exception e) {
			log.error("Could not find description of load balancer "
					+ loadBalancerName);
			return null;
		}
	}

	/**
	 * Adds listeners provided to the load balancer. Useful when service
	 * definition is changed, in particular port mappings. So new listeners need
	 * to be added.
	 * 
	 * @param loadBalancerName
	 * @param listeners
	 */
	public void addListenersToLoadBalancer(String loadBalancerName,
			List<Listener> listeners) {
		if (listeners.size() == 0)
			return;

		try {
			CreateLoadBalancerListenersRequest createLoadBalancerListenersRequest = new CreateLoadBalancerListenersRequest();
			createLoadBalancerListenersRequest.setListeners(listeners);
			createLoadBalancerListenersRequest
					.setLoadBalancerName(loadBalancerName);

			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
					awsCredentials, clientConfiguration);

			lbClient.createLoadBalancerListeners(createLoadBalancerListenersRequest);
		} catch (Exception e) {
			log.error("Could not add listeners to load balancer "
					+ loadBalancerName);
		}
	}

	/**
	 * Remove listeners provided from the load balancer. Useful when attached
	 * listeners are no longer required.
	 * 
	 * @param loadBalancerName
	 * @param listeners
	 */
	public void removeListenersFromLoadBalancer(String loadBalancerName,
			List<Listener> listeners) {
		if (listeners.size() == 0)
			return;

		try {
			DeleteLoadBalancerListenersRequest deleteLoadBalancerListenersRequest = new DeleteLoadBalancerListenersRequest();
			deleteLoadBalancerListenersRequest
					.setLoadBalancerName(loadBalancerName);

			List<Integer> loadBalancerPorts = new ArrayList<Integer>();

			for (Listener listener : listeners) {
				loadBalancerPorts.add(listener.getLoadBalancerPort());
			}

			deleteLoadBalancerListenersRequest
					.setLoadBalancerPorts(loadBalancerPorts);

			AmazonElasticLoadBalancingClient lbClient = new AmazonElasticLoadBalancingClient(
					awsCredentials, clientConfiguration);

			lbClient.deleteLoadBalancerListeners(deleteLoadBalancerListenersRequest);

		} catch (Exception e) {
			log.error("Could not remove listeners from load balancer "
					+ loadBalancerName);
		}
	}

	/**
	 * Returns all the listeners attached to the load balancer. Useful while
	 * deciding if all the listeners are necessary or some should be removed.
	 * 
	 * @param loadBalancerName
	 * @return list of instances attached to load balancer
	 */
	public List<Listener> getAttachedListeners(String loadBalancerName) {
		try {
			LoadBalancerDescription lbDescription = getLoadBalancerDescription(loadBalancerName);

			if (lbDescription == null) {
				log.warn("Could not find description of load balancer "
						+ loadBalancerName);
				return null;
			}

			List<Listener> listeners = new ArrayList<Listener>();

			List<ListenerDescription> listenerDescriptions = lbDescription
					.getListenerDescriptions();

			for (ListenerDescription listenerDescription : listenerDescriptions) {
				listeners.add(listenerDescription.getListener());
			}

			return listeners;

		} catch (Exception e) {
			log.error("Could not find description of load balancer "
					+ loadBalancerName);
			return null;
		}

	}

	/**
	 * Returns the Listeners required for the service. Listeners are derived
	 * from the proxy port, port and protocol values of the service.
	 * 
	 * @param service
	 * @return list of listeners required for the service
	 */
	public List<Listener> getRequiredListeners(Service service) {
		List<Listener> listeners = new ArrayList<Listener>();

		for (Port port : service.getPorts()) {
			int instancePort = port.getValue();
			int proxyPort = port.getProxy();
			String protocol = port.getProtocol();

			Listener listener = new Listener(protocol, proxyPort, instancePort);

			listeners.add(listener);
		}

		return listeners;
	}

	/**
	 * Constructs name of the load balancer to be associated with the cluster
	 * 
	 * @param clusterId
	 * @return name of the load balancer
	 */
	public String getLoadBalancerName(String clusterId) {
		String name = null;
		int length = clusterId.length();
		int endIndex = length > 31 ? 31 : length;
		name = clusterId.substring(0, endIndex);
		name = name.replace('.', '-');

		return name;
	}
}
