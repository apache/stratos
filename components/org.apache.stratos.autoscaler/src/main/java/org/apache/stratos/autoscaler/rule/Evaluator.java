/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterContext;
import org.apache.stratos.autoscaler.Constants;
import org.apache.stratos.autoscaler.algorithm.AutoscaleAlgorithm;
import org.apache.stratos.autoscaler.algorithm.OneAfterAnother;
import org.apache.stratos.autoscaler.algorithm.RoundRobin;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.message.receiver.TopologyManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.LoadThresholds;
import org.apache.stratos.autoscaler.policy.model.Partition;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;

/**
 * This is the rules evaluator
 */
public class Evaluator {

	private static final Log log = LogFactory.getLog(Evaluator.class);

	CloudControllerClient cloudControllerClient = new CloudControllerClient();

	public void evaluatePojos() {

		// Get all values of services Map from topology
		Topology topology = TopologyManager.getTopology();
		for (String clusterId : AutoscalerContext.getInstance().getClusterContexes().keySet()) {
			boolean clusterAvailable = false;
			for (Service service : topology.getServices()) {
				if (service.clusterExists(clusterId)) {
					clusterAvailable = true;
				}
			}
			if (!clusterAvailable) {
				try {
					cloudControllerClient.terminateAll(clusterId);
					AutoscalerContext.getInstance().removeClusterContext(clusterId);
				} catch (Throwable e) {
					log.info("Error occurred " ,e);
				}

			}
		}

		for (Service service : topology.getServices()) {
			// Get all values of clusters from service
			for (Cluster cluster : service.getClusters()) {
				if (AutoscalerContext.getInstance().getClusterContext(cluster.getClusterId()) == null) {
					ClusterContext cC = new ClusterContext(cluster.getClusterId(),
							cluster.getServiceName());

					AutoscalePolicy policy = PolicyManager.getInstance().getPolicy(
							cluster.getAutoscalePolicyName());
					for (Partition partition : policy.getHAPolicy().getPartitions()) {
						cC.addPartitionCount(partition.getId(), 0);
					}
					AutoscalerContext.getInstance().addClusterContext(cC);
				}
				minimumCheck(cluster.getClusterId(), cluster.getServiceName(),
						cluster.getAutoscalePolicyName());

				healthCheck(cluster.getClusterId(), cluster.getAutoscalePolicyName());

			}
		}
	}

	public void minimumCheck(String clusterId, String serviceId, String autoscalePolicyId) {

		AutoscalePolicy policy = PolicyManager.getInstance().getPolicy(autoscalePolicyId);

		for (Partition partition : policy.getHAPolicy().getPartitions()) {
			String partitionId = partition.getId();
			int partitionMin = partition.getPartitionMin();

			int currentMemberCount = AutoscalerContext.getInstance().getClusterContext(clusterId)
					.getPartitionCount(partitionId);

			if (currentMemberCount < partitionMin) {

				int memberCountToBeIncreased = partitionMin - currentMemberCount;
				 try {
				 cloudControllerClient.spawnInstances(partition, clusterId,
				 memberCountToBeIncreased);
				 } catch (SpawningException e) {
					 log.info("Error occurred " ,e);
				 }
				// catch if there is error, and try to spawnInstances again
				// update current member count if success
				AutoscalerContext.getInstance().getClusterContext(clusterId)
						.increaseMemberCountInPartition(partitionId, memberCountToBeIncreased);
			}

		}
	}

	public void healthCheck(String clusterId, String autoscalePolicyId) {

		ClusterContext clusterContext = AutoscalerContext.getInstance()
				.getClusterContext(clusterId);

		float lbStatAverage = clusterContext.getAverageRequestsInFlight();
		float lbStatGradient = clusterContext.getRequestsInFlightGradient();
		float lbStatSecondDerivative = clusterContext.getRequestsInFlightSecondDerivative();

		LoadThresholds loadThresholds = PolicyManager.getInstance().getPolicy(autoscalePolicyId)
				.getLoadThresholds();
		float averageLimit = loadThresholds.getRequestsInFlight().getAverage();
		float gradientLimit = loadThresholds.getRequestsInFlight().getGradient();
		float secondDerivative = loadThresholds.getRequestsInFlight().getSecondDerivative();

		String partitionAlgorithm = PolicyManager.getInstance().getPolicy(autoscalePolicyId)
				.getHAPolicy().getPartitionAlgo();

		AutoscaleAlgorithm autoscaleAlgorithm = null;
		if (Constants.ROUND_ROBIN_ALGORITHM_ID.equals(partitionAlgorithm)) {

			autoscaleAlgorithm = new RoundRobin();
		} else if (Constants.ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(partitionAlgorithm)) {

			autoscaleAlgorithm = new OneAfterAnother();
		}

		if (lbStatAverage > averageLimit && lbStatGradient > gradientLimit) {

			Partition partition = autoscaleAlgorithm.getNextScaleUpPartition(clusterId);

			if (lbStatSecondDerivative > secondDerivative) {

				int numberOfInstancesToBeSpawned = 2; // take from a config

				try {
					cloudControllerClient.spawnInstances(partition, clusterId,
							numberOfInstancesToBeSpawned);
				} catch (SpawningException e) {
					log.info("Error occurred " ,e);
				}
				// spawnInstances Two

			} else {

				try {
					cloudControllerClient.spawnAnInstance(partition, clusterId);
				} catch (SpawningException e) {
					log.info("Error occurred " ,e);
				}
				// spawnInstances one
			}
		} else if (lbStatAverage < averageLimit && lbStatGradient < gradientLimit) {

			// terminate one
			Partition partition = autoscaleAlgorithm.getNextScaleDownPartition(clusterId);
			try {
				cloudControllerClient.terminate(partition, clusterId);
			} catch (TerminationException e) {
				log.info("Error occurred " ,e);
			}

		}

	}
}
