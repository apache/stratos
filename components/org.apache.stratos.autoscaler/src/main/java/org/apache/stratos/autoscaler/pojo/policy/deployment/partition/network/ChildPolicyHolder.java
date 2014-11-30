package org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network;


import java.util.Map;

public class ChildPolicyHolder {

    // Key: networkPartitinID, value: ApplicationLevelNetworkPartition
    private Map<String, ApplicationLevelNetworkPartition> applicationLevelNetworkPartitions;
    private String deploymentPolicyId;

    public ChildPolicyHolder(Map<String, ApplicationLevelNetworkPartition> applicationLevelNetworkPartitions, String deploymentPolicyId) {
        this.applicationLevelNetworkPartitions = applicationLevelNetworkPartitions;
        this.deploymentPolicyId = deploymentPolicyId;
    }

    public String getDeploymentPolicyId() {
        return deploymentPolicyId;
    }

    public Map<String, ApplicationLevelNetworkPartition> getApplicationLevelNetworkPartitions() {
        return applicationLevelNetworkPartitions;
    }
}
