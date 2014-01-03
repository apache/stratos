package org.apache.stratos.messaging.event.topology;

import org.apache.stratos.messaging.domain.topology.MemberStatus;

import java.io.Serializable;
import java.util.Properties;

public class MemberReadyToShutdownEvent extends TopologyEvent implements Serializable {
    private final String serviceName;
    private final String clusterId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String memberId;
    private MemberStatus status;
    private Properties properties;

    public MemberReadyToShutdownEvent(String serviceName, String clusterId,
                                      String networkPartitionId, String partitionId, String memberId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
        this.memberId = memberId;
    }

     public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getMemberId() {
        return memberId;
    }

    public MemberStatus getStatus() {
        return status;
    }

    public void setStatus(MemberStatus status) {
        this.status = status;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

	public String getPartitionId() {
		return partitionId;
	}

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }
}
