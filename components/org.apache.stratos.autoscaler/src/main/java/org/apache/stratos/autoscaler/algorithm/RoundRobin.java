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

/**
* Select partition in round robin manner and return
*/
public class RoundRobin implements AutoscaleAlgorithm{

//    public Partition getNextScaleUpPartition(String clusterId){
//
//        String policyId;
//        int nextPartitionIndex;
//        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
//        int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
//
//        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();
//
//        //Find relevant policyId using topology
//        policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getAutoscalePolicyName();
//
//
//        int noOfPartitions = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().size();
//
//        if (currentPartitionIndex + 1 >= noOfPartitions) {
//
//            nextPartitionIndex = 0;
//        } else {
//
//            nextPartitionIndex = currentPartitionIndex++;
//        }
//
//        //Set next partition as current partition in Autoscaler Context
//        AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(nextPartitionIndex);
//
//        //Find next partition
//        Partition nextPartition = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions()
//                .get(nextPartitionIndex);
//        String nextPartitionId = nextPartition.getId();
//
//        if(clusterContext.partitionCountExists(nextPartitionId)){
//
//            //If the current partitions max is reached, it will try next partition
//            if(clusterContext.getPartitionCount(nextPartitionId) >= nextPartition.getPartitionMembersMax()){
//
//                nextPartition = getNextScaleUpPartition(clusterId);
//            }
//        } else {
//
//            //Add the partition count entry to cluster context
//            AutoscalerContext.getInstance().getClusterContext(clusterId).addPartitionCount(nextPartitionId, 1);
//        }
//        return nextPartition;
//    }
//
//
//    public Partition getNextScaleDownPartition(String clusterId){
//
//        String policyId;
//        int nextPartitionIndex;
//        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
//        int currentPartitionIndex = clusterContext.getCurrentPartitionIndex();
//
//        String serviceId = AutoscalerContext.getInstance().getClusterContext(clusterId).getServiceId();
//
//        //Find relevant policyId using topology
//        policyId = TopologyManager.getTopology().getService(serviceId).getCluster(clusterId).getAutoscalePolicyName();
//
//
//        int noOfPartitions = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions().size();
//
//        if (currentPartitionIndex - 1 >= noOfPartitions) {
//
//            nextPartitionIndex = 0;
//        } else {
//
//            nextPartitionIndex = currentPartitionIndex--;
//        }
//
//        //Set next partition as current partition in Autoscaler Context
//        AutoscalerContext.getInstance().getClusterContext(clusterId).setCurrentPartitionIndex(nextPartitionIndex);
//
//        //Find next partition
//        Partition nextPartition = PolicyManager.getInstance().getPolicy(policyId).getHAPolicy().getPartitions()
//                .get(nextPartitionIndex);
//        String nextPartitionId = nextPartition.getId();
//
//        if(clusterContext.partitionCountExists(nextPartitionId)
//                && (clusterContext.getPartitionCount(nextPartitionId) <= nextPartition.getPartitionMembersMin())){
//
//
//            //If the current partitions max is reached, it will try next partition
//            nextPartition = getNextScaleDownPartition(clusterId);
//        }
//        return nextPartition;
//    }


//    public Partition getScaleDownPartition(String clusterId){
//        Partition partition = PolicyManager.getInstance().getPolicy("economyPolicy").getHAPolicy().getPartitions()
//                            .get(0);
//
//        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
//        int partitionMemberCount = clusterContext.getPartitionCount(partition.getId());
//
//        if(partitionMemberCount >= partition.getPartitionMembersMin())       {
//
//            clusterContext.increaseMemberCountInPartition(partition.getId(), partitionMemberCount - 1);
//        } else{
//            partition = null;
//        }
//        return partition;
//    }


//    @Override
//    public boolean scaleUpPartitionAvailable(String clusterId) {
//        return false;  //To change body of implemented methods use File | Settings | File Templates.
//    }
//
//    @Override
//    public boolean scaleDownPartitionAvailable(String clusterId) {
//        return false;  //To change body of implemented methods use File | Settings | File Templates.
//    }          partition = null;
//        }
//
//        return partition;
//    }



//    public Partition getScaleUpPartition(String clusterId){
//        Partition partition = PolicyManager.getInstance().getPolicy("economyPolicy").getHAPolicy().getPartitions()
//                            .get(0);
//
//        ClusterContext clusterContext = AutoscalerContext.getInstance().getClusterContext(clusterId);
//        int partitionMemberCount = clusterContext.getPartitionCount(partition.getId());
//
//        if(partitionMemberCount <= partition.getPartitionMembersMax())       {
//
//            clusterContext.increaseMemberCountInPartition(partition.getId(), partitionMemberCount + 1);
//        } else{
//            partition = null;
//        }
//
//        return partition;
//    }
}