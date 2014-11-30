package org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network;


import java.util.Map;

public class ChildPolicyHolder {

    // Key: networkPartitinID, value: ChildLevelNetworkPartition
    private Map<String, ChildLevelNetworkPartition> childLevelNetworkPartitions;
    private String deploymentPolicyId;

    public ChildPolicyHolder(Map<String, ChildLevelNetworkPartition> childLevelNetworkPartitions, String deploymentPolicyId) {
        this.childLevelNetworkPartitions = childLevelNetworkPartitions;
        this.deploymentPolicyId = deploymentPolicyId;
    }

    public String getDeploymentPolicyId() {
        return deploymentPolicyId;
    }

    public Map<String, ChildLevelNetworkPartition> getChildLevelNetworkPartitions() {
        return childLevelNetworkPartitions;
    }

    public ChildLevelNetworkPartition getChildLevelNetworkPartitionById(String networkPartitionId) {
        return childLevelNetworkPartitions.get(networkPartitionId);
    }
}
