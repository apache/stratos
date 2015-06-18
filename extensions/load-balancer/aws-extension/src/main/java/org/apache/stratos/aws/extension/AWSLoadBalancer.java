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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.*;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.load.balancer.extension.api.LoadBalancer;

import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.Listener;

public class AWSLoadBalancer implements LoadBalancer {

	private static final Log log = LogFactory.getLog(AWSLoadBalancer.class);

	// A map <clusterId, load balancer id>
	private HashMap<String, String> clusterIdToLoadBalancerMap;

	private AWSHelper awsHelper;

	public AWSLoadBalancer() throws LoadBalancerExtensionException {
		clusterIdToLoadBalancerMap = new HashMap<String, String>();
		awsHelper = new AWSHelper();
	}

	public boolean configure(Topology topology)
			throws LoadBalancerExtensionException {

		log.info("AWS load balancer extension re-configured.");

		for (Service service : topology.getServices()) {

			List<Listener> listenersForThisService = awsHelper
					.getRequiredListeners(service);

			for (Cluster cluster : service.getClusters()) {

				// cluster.getHostNames()

				// Check if a load balancer is created for this cluster

				if (clusterIdToLoadBalancerMap.containsKey(cluster
						.getClusterId())) {
					// A load balancer is already present for this cluster
					// Get the load balancer and update it.

					String loadBalancerName = clusterIdToLoadBalancerMap
							.get(cluster.getClusterId());

					// 1. Get all the instances attached
					// Add/remove instances as necessary

					List<Instance> attachedInstances = awsHelper
							.getAttachedInstances(loadBalancerName);

					Collection<Member> clusterInstances = cluster.getMembers();

					List<Instance> instancesToAddToLoadBalancer = new ArrayList<Instance>();

					for (Member member : clusterInstances) {
						// if instance id of member is not in attachedInstances
						// add this to instancesToAddToLoadBalancer

					}

					List<Instance> instancesToRemoveFromLoadBalancer = new ArrayList<Instance>();

					for (Instance instance : attachedInstances) {
						if (!clusterInstances
								.contains(instance.getInstanceId())) {
							instancesToRemoveFromLoadBalancer.add(instance);
						}
					}

					if (instancesToRemoveFromLoadBalancer.size() > 0)
						awsHelper.deregisterInstancesFromLoadBalancer(
								loadBalancerName,
								instancesToRemoveFromLoadBalancer);

					if (instancesToAddToLoadBalancer.size() > 0)
						awsHelper.registerInstancesToLoadBalancer(
								loadBalancerName, instancesToAddToLoadBalancer);

					// 2. Get all the listeners
					// Add/Remove listeners as necessary

					// Is it really necessary to add/remove listeners from a lb
					// to a cluster
					// Need to add only if a cluster can be used for more than
					// one service (because a service my get added later)
					// or service port mappings may change

					// Need to remove only if ... same for above reason

					List<Listener> attachedListeners = awsHelper
							.getAttachedListeners(loadBalancerName);

					List<Listener> listenersToAddToLoadBalancer = new ArrayList<Listener>();

					for (Listener listener : listenersForThisService) {
						// Need to check if Listener class supports equals
						// method or not

						// if listener required for this service is not in
						// attachedListeners
						// add this to listenersToAddToLoadBalancer
					}

					List<Listener> listenersToRemoveFromLoadBalancer = new ArrayList<Listener>();

					for (Listener listener : attachedListeners) {
						// Need to check if Listener class supports equals
						// method or not

						if (!listenersForThisService.contains(listener)) {
							listenersToRemoveFromLoadBalancer.add(listener);
						}
					}

					if (listenersToRemoveFromLoadBalancer.size() > 0)
						awsHelper.removeListenersFromLoadBalancer(
								loadBalancerName,
								listenersToRemoveFromLoadBalancer);

					if (listenersToAddToLoadBalancer.size() > 0)
						awsHelper.addListenersToLoadBalancer(loadBalancerName,
								listenersToAddToLoadBalancer);

					// Update domain mappings

				} else {
					// Create a new load balancer for this cluster

					String loadBalancerName = service.getServiceName() + "-"
							+ cluster.getClusterId();

					String loadBalancerDNSName = awsHelper.createLoadBalancer(
							loadBalancerName, listenersForThisService);

					// register instances to LB

					List<Instance> instances = new ArrayList<Instance>();

					for (Member m : cluster.getMembers()) {
						String instanceId = ""; // of member // after making
												// changes suggested in mail

						Instance instance = new Instance();
						instance.setInstanceId(instanceId);

						instances.add(instance);
					}

					awsHelper.registerInstancesToLoadBalancer(loadBalancerName,
							instances);

					// Create domain mappings
				}

			}
		}

		// Find out clusters which were present earlier but are not now.
		// Delete load balancers associated with those clusters.

		return true;
	}

	public void start() throws LoadBalancerExtensionException {

		log.info("AWS load balancer extension started.");
	}

	public void reload() throws LoadBalancerExtensionException {
		// Check what is appropriate to do here.
		log.info("AWS load balancer extension reloaded.");
	}

	public void stop() throws LoadBalancerExtensionException {
		// Remove all load balancers

		for (String loadBalancerName : clusterIdToLoadBalancerMap.values()) {
			// remove load balancer
			awsHelper.deleteLoadBalancer(loadBalancerName);

			// Check what all needs to be done
		}

		// Remove domain mappings
	}
}
