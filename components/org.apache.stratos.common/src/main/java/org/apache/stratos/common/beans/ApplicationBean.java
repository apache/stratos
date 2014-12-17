package org.apache.stratos.common.beans;

import org.apache.stratos.common.beans.topology.Cluster;
import org.apache.stratos.common.beans.topology.Instance;

import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="applications")
public class ApplicationBean {

    private String id;
    private String name;
    private String description;
    private String tenantDomain;
    private String tenantAdminUsername;
    private List<GroupBean> groups = null;
    private List<Cluster> clusters = null;
    private List<Instance> instances;

    public ApplicationBean(){
        this.setGroups(new ArrayList<GroupBean>());
        this.setClusters(new ArrayList<Cluster>());
    }
    public void addGroup(GroupBean groupBean) {
        this.getGroups().add(groupBean);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public void setTenantAdminUsername(String tenantAdminUsername) {
        this.tenantAdminUsername = tenantAdminUsername;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public String getTenantAdminUsername() {
        return tenantAdminUsername;
    }

	public List<Instance> getInstances() {
		return instances;
	}

    public void setInstances(List<Instance> instances) {
		this.instances = instances;
	}

    public List<GroupBean> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupBean> groups) {
        this.groups = groups;
    }

    public List<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(List<Cluster> clusters) {
        this.clusters = clusters;
    }
}
