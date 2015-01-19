package org.apache.stratos.common.beans.topology;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class ApplicationInfoBean {

    private String id;
    private String name;
    private String status;
    private String description;
    private String tenantDomain;
    private String tenantAdminUsername;
    private List<ApplicationInstanceBean> applicationInstances;

    public ApplicationInfoBean() {
        setApplicationInstances(new ArrayList<ApplicationInstanceBean>());
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getTenantAdminUsername() {
        return tenantAdminUsername;
    }

    public void setTenantAdminUsername(String tenantAdminUsername) {
        this.tenantAdminUsername = tenantAdminUsername;
    }
    public List<ApplicationInstanceBean> getApplicationInstances() {
        return applicationInstances;
    }

    public void setApplicationInstances(List<ApplicationInstanceBean> applicationInstances) {
        this.applicationInstances = applicationInstances;
    }
}
