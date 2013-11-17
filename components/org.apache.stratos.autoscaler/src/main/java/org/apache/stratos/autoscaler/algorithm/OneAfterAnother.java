/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.stratos.autoscaler.algorithm;

import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.ClusterContext;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.Partition;

/**
 * Completes partitions in the order defined in autoscaler policy, go to next if current one reached the max limit
 */
public class OneAfterAnother implements AutoscaleAlgorithm {

    public Partition getNextScaleUpPartition(String clusterId) {

        // Find cluster context
        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);

        // Find service id
        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();

        //Find relevant policyId using topology
        String policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getAutoscalePolicyName();

        // Find number of partitions
        int noOfPartitions = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().size();
        
        for(int i=0; i<noOfPartitions;i++)
        {
            // Here in "one after another" algorithm, next partition is also the current partition unless it reached its max
            int nextPartitionIndex = clusterContext.getCurrentPartitionIndex();;

            //Find next partition
            Partition nextPartition = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().get(nextPartitionIndex);
            String nextPartitionId = nextPartition.getId();

            if (clusterContext.partitionCountExists(nextPartitionId)) {            	
                if (clusterContext.getMemberCount(nextPartitionId) >= nextPartition.getPartitionMembersMax()) {
                    if(nextPartitionIndex == (noOfPartitions - 1)) {
                        // All partitions have reached their max
                        return null;
                    }
                    // Selected partition's max has reached, it will try next partition
                    AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(nextPartitionIndex + 1);
                }
                else {
                    // Increase member partition member count by one
                    AutoscalerContext.getInstance().getClusterContext(clusterId).increaseMemberCountInPartition(nextPartitionId, 1);
                    return nextPartition;
                }
            } else {

                // Add the partition count entry to cluster context
                AutoscalerContext.getInstance().getClusterContext(clusterId).addPartitionCount(nextPartitionId, 1);
            }

        }

        return null;
    }

    public Partition getNextScaleDownPartition(String clusterId) {

        // Find cluster context
        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);

        // Find service id
        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();

        //Find relevant policyId using topology
        String policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getAutoscalePolicyName();

        // Find number of partitions
        int noOfPartitions = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().size();
        
        for(int i=0;i<noOfPartitions;i++)
        {

            // Here in "one after another" algorithm, next partition is also the current partition unless it reached its max
            int nextPartitionIndex = clusterContext.getCurrentPartitionIndex();;

            //Find next partition
            Partition nextPartition = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().get(nextPartitionIndex);
            String nextPartitionId = nextPartition.getId();

            if (clusterContext.partitionCountExists(nextPartitionId)) {
                if (clusterContext.getMemberCount(nextPartitionId) >= nextPartition.getPartitionMembersMax()) {
                    if(nextPartitionIndex == 0) {
                        // All partitions have reached their min
                        return null;
                    }

                    // Selected partition's min has reached, it will try next partition
                    AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(nextPartitionIndex - 1);
                }
                else {
                    // Increase member partition member count by one
                    AutoscalerContext.getInstance().getClusterContext(clusterId).increaseMemberCountInPartition(nextPartitionId, 1);
                    return nextPartition;
                }
            } else {

                // Add the partition count entry to cluster context
                AutoscalerContext.getInstance().getClusterContext(clusterId).addPartitionCount(nextPartitionId, 1);
            }
        }
        
        return null;
    }
    

    @Override
    public boolean scaleUpPartitionAvailable(String clusterId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean scaleDownPartitionAvailable(String clusterId) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
