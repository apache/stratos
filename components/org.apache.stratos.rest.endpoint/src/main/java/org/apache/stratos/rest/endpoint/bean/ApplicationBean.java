package org.apache.stratos.rest.endpoint.bean;

import org.apache.stratos.rest.endpoint.bean.topology.Cluster;
import org.apache.stratos.rest.endpoint.bean.topology.Instance;

import javax.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="applications")
public class ApplicationBean {
    private String id;
    private String tenantDomain;
    private String tenantAdminUsername;
    public List<GroupBean> groups = null;
    public List<Cluster> clusters = null;
    private List<Instance> instances;


    public ApplicationBean(){
        this.groups = new ArrayList<GroupBean>();
        this.clusters = new ArrayList<Cluster>();
    }
    public void addGroup(GroupBean groupBean) {
        this.groups.add(groupBean);
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public void setTenantAdminUsername(String tenantAdminUsername) {
        this.tenantAdminUsername = tenantAdminUsername;
    }

    public String getId() {
        return id;
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
    
    
}
