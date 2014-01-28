package org.apache.stratos.cli.beans.topology;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class Cluster {
    private static final long serialVersionUID = -361960242360176077L;

	private final String serviceName;
    private final String clusterId;
    private final String autoscalePolicyName;
    private final String deploymentPolicyName;

    private List<String> hostNames;
    private String tenantRange;
    private boolean isLbCluster;
    // Key: Member.memberId
    private Map<String, Member> memberMap;

    private String loadBalanceAlgorithmName;
    private Properties properties;

    public Cluster(String serviceName, String clusterId, String deploymentPolicyName, String autoscalePolicyName) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.deploymentPolicyName = deploymentPolicyName;
        this.autoscalePolicyName = autoscalePolicyName;
        this.hostNames = new ArrayList<String>();
        this.memberMap = new HashMap<String, Member>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public List<String> getHostNames() {
        return hostNames;
    }

    public void addHostName(String hostName) {
        this.hostNames.add(hostName);
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public Collection<Member> getMembers() {
        return memberMap.values();
    }

    public boolean hasMembers() {
        return  memberMap.isEmpty();
    }



    public void addMember(Member member) {
        memberMap.put(member.getMemberId(), member);
    }

    public void removeMember(Member member) {
        memberMap.remove(member.getMemberId());
    }

    public Member getMember(String memberId) {
        return memberMap.get(memberId);
    }

    public boolean memberExists(String memberId) {
        return this.memberMap.containsKey(memberId);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getAutoscalePolicyName() {
        return autoscalePolicyName;
    }

	public String getDeploymentPolicyName() {
		return deploymentPolicyName;
	}

    public String getLoadBalanceAlgorithmName() {
        return loadBalanceAlgorithmName;
    }

    public void setLoadBalanceAlgorithmName(String loadBalanceAlgorithmName) {
        this.loadBalanceAlgorithmName = loadBalanceAlgorithmName;
    }

    public boolean isLbCluster() {
        return isLbCluster;
    }

    public void setLbCluster(boolean isLbCluster) {
        this.isLbCluster = isLbCluster;
    }

    @Override
    public String toString() {
        return "Cluster [serviceName=" + serviceName + ", clusterId=" + clusterId +
                ", autoscalePolicyName=" + autoscalePolicyName + ", deploymentPolicyName=" +
                deploymentPolicyName + ", hostNames=" + hostNames + ", tenantRange=" + tenantRange +
                ", isLbCluster=" + isLbCluster + ", properties=" + properties + "]";
    }

    /**
     * Check whether a given tenant id is in tenant range of the cluster.
     *
     * @param tenantId
     * @return
     */
    public boolean tenantIdInRange(int tenantId) {
        if(StringUtils.isBlank(getTenantRange())) {
            return false;
        }

        if("*".equals(getTenantRange())) {
            return true;
        }
        else {
            String[] array = getTenantRange().split("-");
            int tenantStart = Integer.parseInt(array[0]);
            if(tenantStart <= tenantId) {
                String tenantEndStr = array[1];
                if("*".equals(tenantEndStr)) {
                    return true;
                }
                else {
                    int tenantEnd = Integer.parseInt(tenantEndStr);
                    if(tenantId <= tenantEnd) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find partitions used by the cluster and return their ids as a collection.
     *
     * @return
     */
    public Collection<String> findPartitionIds() {
        Map<String, Boolean> partitionIds = new HashMap<String, Boolean>();
        for(Member member : getMembers()) {
            if((StringUtils.isNotBlank(member.getPartitionId())) && (!partitionIds.containsKey(member.getPartitionId()))) {
                partitionIds.put(member.getPartitionId(), true);
            }
        }
        return partitionIds.keySet();
    }
}
