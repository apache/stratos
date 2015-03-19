package org.apache.stratos.messaging.event.health.stat;

/**
 * Created by asiri on 8/10/14.
 */

import org.apache.stratos.messaging.event.Event;

public class AverageRequestsServingCapabilityEvent extends Event {
    private final String networkPartitionId;
    private final String clusterId;
    private final String clusterInstanceId;
    private final float value;

    public AverageRequestsServingCapabilityEvent(String networkPartitionId, String clusterId, String clusterInstanceId, float value) {
        this.networkPartitionId = networkPartitionId;
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.value = value;
    }

    public String getClusterId() {
        return clusterId;
    }

    public float getValue() {
        return value;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }
}
