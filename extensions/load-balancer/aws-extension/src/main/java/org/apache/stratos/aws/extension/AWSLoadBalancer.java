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
import java.util.HashSet;
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
	private HashMap<String, LoadBalancerInfo> clusterIdToLoadBalancerMap;

	private AWSHelper awsHelper;

	public AWSLoadBalancer() throws LoadBalancerExtensionException {
		clusterIdToLoadBalancerMap = new HashMap<String, LoadBalancerInfo>();
		awsHelper = new AWSHelper();
	}

	public boolean configure(Topology topology)
			throws LoadBalancerExtensionException {

		log.info("AWS load balancer extension re-configured.");

		try {
			// Thread.sleep(10000);

			HashSet<String> activeClusters = new HashSet<String>();

			for (Service service : topology.getServices()) {
				for (Cluster cluster : service.getClusters()) {
					// Check if a load balancer is created for this cluster
					if (clusterIdToLoadBalancerMap.containsKey(cluster
							.getClusterId())) {
						// A load balancer is already present for this cluster
						// Get the load balancer and update it.

						LoadBalancerInfo loadBalancerInfo = clusterIdToLoadBalancerMap
								.get(cluster.getClusterId());

						String loadBalancerName = loadBalancerInfo.getName();
						String region = loadBalancerInfo.getRegion();

						// 1. Get all the instances attached
						// Add/remove instances as necessary

						List<Instance> attachedInstances = awsHelper
								.getAttachedInstances(loadBalancerName, region);

						Collection<Member> clusterMembers = cluster
								.getMembers();

						if (clusterMembers.size() > 0) {
							activeClusters.add(cluster.getClusterId());
						} else {
							break;
						}

						List<Instance> instancesToAddToLoadBalancer = new ArrayList<Instance>();
						List<Instance> awsInstancesInCluster = new ArrayList<Instance>();

						for (Member member : clusterMembers) {
							// if instance id of member is not in
							// attachedInstances
							// add this to instancesToAddToLoadBalancer

							System.out.println("Instance Id : "
									+ member.getInstanceId());
							System.out.println("New instance id : "
									+ awsHelper.getAWSInstanceName(member
											.getInstanceId()));

							Instance instance = new Instance(
									awsHelper.getAWSInstanceName(member
											.getInstanceId()));

							awsInstancesInCluster.add(instance);

							if (attachedInstances == null
									|| !attachedInstances.contains(instance)) {
								instancesToAddToLoadBalancer.add(instance);
							}

						}

						List<Instance> instancesToRemoveFromLoadBalancer = new ArrayList<Instance>();

						for (Instance instance : attachedInstances) {
							if (!awsInstancesInCluster.contains(instance)) {
								instancesToRemoveFromLoadBalancer.add(instance);
							}
						}

						if (instancesToRemoveFromLoadBalancer.size() > 0)
							awsHelper.deregisterInstancesFromLoadBalancer(
									loadBalancerName,
									instancesToRemoveFromLoadBalancer, region);

						if (instancesToAddToLoadBalancer.size() > 0)
							awsHelper.registerInstancesToLoadBalancer(
									loadBalancerName,
									instancesToAddToLoadBalancer, region);

						// 2. Get all the listeners
						// Add/Remove listeners as necessary

						// Is it really necessary to add/remove listeners from a
						// lb
						// to a cluster
						// Need to add only if a cluster can be used for more
						// than
						// one service (because a service my get added later)
						// or service port mappings may change

						// Need to remove only if ... same for above reason

						List<Listener> attachedListeners = awsHelper
								.getAttachedListeners(loadBalancerName, region);

						List<Listener> listenersToAddToLoadBalancer = new ArrayList<Listener>();

						List<Listener> listenersForThisCluster = awsHelper
								.getRequiredListeners(clusterMembers.iterator().next());

						for (Listener listener : listenersForThisCluster) {
							if (attachedListeners == null
									|| !attachedListeners.contains(listener)) {
								listenersToAddToLoadBalancer.add(listener);
							}
						}

						List<Listener> listenersToRemoveFromLoadBalancer = new ArrayList<Listener>();

						for (Listener listener : attachedListeners) {
							if (!listenersForThisCluster.contains(listener)) {
								listenersToRemoveFromLoadBalancer.add(listener);
							}
						}

						if (listenersToRemoveFromLoadBalancer.size() > 0)
							awsHelper.removeListenersFromLoadBalancer(
									loadBalancerName,
									listenersToRemoveFromLoadBalancer, region);

						if (listenersToAddToLoadBalancer.size() > 0)
							awsHelper.addListenersToLoadBalancer(
									loadBalancerName,
									listenersToAddToLoadBalancer, region);

						// Update domain mappings

					} else {
						// Create a new load balancer for this cluster
						Collection<Member> clusterMembers = cluster
								.getMembers();

						if (clusterMembers.size() == 0)
							break;

						String loadBalancerName = awsHelper
								.generateLoadBalancerName();

						String region = awsHelper.getAWSRegion(clusterMembers
								.iterator().next().getInstanceId());

						List<Listener> listenersForThisCluster = awsHelper
								.getRequiredListeners(clusterMembers.iterator().next());

						String loadBalancerDNSName = awsHelper
								.createLoadBalancer(loadBalancerName,
										listenersForThisCluster, region);

						log.info("Load balancer '" + loadBalancerDNSName
								+ "' created for cluster '"
								+ cluster.getClusterId());

						// register instances to LB
						List<Instance> instances = new ArrayList<Instance>();

						for (Member member : clusterMembers) {
							String instanceId = member.getInstanceId();

							System.out.println("Instance id : " + instanceId);
							System.out.println("New instance id : "
									+ awsHelper.getAWSInstanceName(instanceId));

							Instance instance = new Instance();
							instance.setInstanceId(awsHelper
									.getAWSInstanceName(instanceId));

							instances.add(instance);
						}

						awsHelper.registerInstancesToLoadBalancer(
								loadBalancerName, instances, region);

						// Create domain mappings

						LoadBalancerInfo loadBalancerInfo = new LoadBalancerInfo(
								loadBalancerName, region);

						clusterIdToLoadBalancerMap.put(cluster.getClusterId(),
								loadBalancerInfo);
						activeClusters.add(cluster.getClusterId());
					}
				}
			}

			// Find out clusters which were present earlier but are not now.
			// Delete load balancers associated with those clusters.

			for (String clusterId : clusterIdToLoadBalancerMap.keySet()) {
				if (!activeClusters.contains(clusterId)) {
					// Remove load balancer for that cluster.
					awsHelper.deleteLoadBalancer(clusterIdToLoadBalancerMap
							.get(clusterId).getName(),
							clusterIdToLoadBalancerMap.get(clusterId)
									.getRegion());
					clusterIdToLoadBalancerMap.remove(clusterId);
				}
			}

			activeClusters.clear();
		} catch (Exception e) {
			throw new LoadBalancerExtensionException(e);
		}

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
		for (LoadBalancerInfo loadBalancerInfo : clusterIdToLoadBalancerMap
				.values()) {
			// remove load balancer
			awsHelper.deleteLoadBalancer(loadBalancerInfo.getName(),
					loadBalancerInfo.getRegion());
			// Check what all needs to be done
		}

		// Remove domain mappings
	}
}

class LoadBalancerInfo {
	private String name;
	private String region;

	public LoadBalancerInfo(String name, String region) {
		this.name = name;
		this.region = region;
	}

	public String getName() {
		return name;
	}

	public String getRegion() {
		return region;
	}
}
