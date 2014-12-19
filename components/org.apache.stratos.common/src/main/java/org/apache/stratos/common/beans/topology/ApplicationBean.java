package org.apache.stratos.common.beans.topology;

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
    private List<ApplicationInstanceBean> applicationInstances;

    public ApplicationBean(){
        applicationInstances = new ArrayList<ApplicationInstanceBean>();
    }

    public void addGroupInstance(ApplicationInstanceBean groupInstance) {
        this.getGroups().add(groupInstance);
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


    public void setApplicationInstances(List<ApplicationInstanceBean> instances) {
		this.applicationInstances = instances;
	}

    public List<ApplicationInstanceBean> getGroups() {
        return applicationInstances;
    }

    public void setGroups(List<ApplicationInstanceBean> groups) {
        this.applicationInstances = groups;
    }

    public List<ApplicationInstanceBean> getApplicationInstances() {
        return applicationInstances;
    }
}
