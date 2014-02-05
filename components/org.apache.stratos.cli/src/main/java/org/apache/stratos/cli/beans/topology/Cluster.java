package org.apache.stratos.cli.beans.topology;

import java.util.List;

public class Cluster {
    private String serviceName;

    private String clusterId;

    private List<Member> member;

    private String tenantRange;

    private List<String> hostNames;

    private boolean isLbCluster;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public List<Member> getMember() {
        return member;
    }

    public void addMember(Member member) {
       this.member.add(member);
    }

    public void removeMember(Member member) {
       this.member.remove(member);
    }

    public void setMember(List<Member> member) {
        this.member = member;
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public List<String> getHostNames() {
        return hostNames;
    }

    public void setHostNames(List<String> hostNames) {
        this.hostNames = hostNames;
    }

    public void addHostNames(String hostName) {
        this.hostNames.add(hostName);
    }

    public void removeHostNames(String hostName) {
        this.hostNames.remove(hostName);
    }

    public boolean isLbCluster() {
        return isLbCluster;
    }

    public void setLbCluster(boolean lbCluster) {
        isLbCluster = lbCluster;
    }
}
