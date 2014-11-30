package org.apache.stratos.autoscaler.pojo.policy.deployment.partition;

public class ChildLevelPartition {
    private String partitionId;
    private String networkPartitionId;
    private int max;

    public ChildLevelPartition(String partitionId, String networkPartitionId, int max) {
        this.partitionId = partitionId;
        this.networkPartitionId = networkPartitionId;
        this.max = max;
    }

    public int getMax() {
        return max;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }
}
