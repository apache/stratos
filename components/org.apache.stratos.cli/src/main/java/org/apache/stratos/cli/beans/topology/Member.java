package org.apache.stratos.cli.beans.topology;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Member {
    private static final long serialVersionUID = 4179661867903664661L;

    private final String serviceName;
    private final String clusterId;
    private final String networkPartitionId;
    private final String partitionId;
    private final String memberId;

    private MemberStatus status;
    private String memberIp;
    private final Map<String, Port> portMap;
    private Properties properties;
    private String lbClusterId;

    public Member(String serviceName, String clusterId, String networkPartitionId, String partitionId, String memberId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.networkPartitionId = networkPartitionId;
        this.partitionId = partitionId;
        this.memberId = memberId;
        this.portMap = new HashMap<String, Port>();
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

    public boolean isActive() {
        return (this.status == MemberStatus.Activated);
    }

    public Collection<Port> getPorts() {
        return portMap.values();
    }

    public void addPort(Port port) {
        this.portMap.put(port.getProtocol(), port);
    }

    public void addPorts(Collection<Port> ports) {
        for(Port port: ports) {
            addPort(port);
        }
    }

    public void removePort(Port port) {
        this.portMap.remove(port.getProtocol());
    }

    public void removePort(String protocol) {
        this.portMap.remove(protocol);
    }

    public boolean portExists(Port port) {
        return this.portMap.containsKey(port.getProtocol());
    }

    public Port getPort(String protocol) {
        return this.portMap.get(protocol);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

	public String getMemberIp() {
	    return memberIp;
    }

	public void setMemberIp(String memberIp) {
	    this.memberIp = memberIp;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }
}
