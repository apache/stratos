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

package org.apache.stratos.autoscaler.util;

import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterContext;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.LoadThresholds;
import org.apache.stratos.autoscaler.policy.model.Partition;
import org.apache.stratos.autoscaler.policy.model.PartitionGroup;
import org.apache.stratos.messaging.domain.topology.Cluster;


/**
 * This class contains utility methods used by Autoscaler.
 */
public class AutoscalerUtil {

	private AutoscalerUtil() {

	}
	
	/**
	 * Updates ClusterContext for given cluster
	 * @param cluster
	 * @return ClusterContext - Updated ClusterContext
	 */
	public static ClusterContext updateClusterContext(Cluster cluster) {
		AutoscalerContext context = AutoscalerContext.getInstance();
		ClusterContext clusterContext = context.getClusterContext(cluster.getClusterId());
		if (null == clusterContext) {

			clusterContext = new ClusterContext(cluster.getClusterId(), cluster.getServiceName());
			AutoscalePolicy policy = PolicyManager.getInstance().getAutoscalePolicy(cluster.getAutoscalePolicyName());

            if(policy!=null){

                //get values from policy
                LoadThresholds loadThresholds = policy.getLoadThresholds();
                float averageLimit = loadThresholds.getRequestsInFlight().getAverage();
                float gradientLimit = loadThresholds.getRequestsInFlight().getGradient();
                float secondDerivative  = loadThresholds.getRequestsInFlight().getSecondDerivative();


                clusterContext.setRequestsInFlightGradient(gradientLimit);
                clusterContext.setRequestsInFlightSecondDerivative(secondDerivative);
                clusterContext.setAverageRequestsInFlight(averageLimit);
                DeploymentPolicy deploymentPolicy = PolicyManager.getInstance().getDeploymentPolicy(cluster.getDeploymentPolicy());
                if(deploymentPolicy!=null){
                	for(PartitionGroup group :deploymentPolicy.getPartitionGroups()){
                		for (Partition partition : group.getPartitions()) {
                            clusterContext.addPartitionCount(partition.getId(), 0);
                    }
                	}
                }
                
             }

			context.addClusterContext(clusterContext);
		}
		return clusterContext;
	}

}
