package org.apache.stratos.manager.subscription;

import org.apache.stratos.cloud.controller.pojo.PersistanceMapping;
import org.apache.stratos.cloud.controller.pojo.Property;

/**
 * This holds the data that are gathered at the time of subscription. This is usefull when passing subscription details to the method calls.
 */
public class SubscriptionData {


    private String cartridgeType;
    private String cartridgeAlias;

    private String autoscalingPolicyName;
    private String deploymentPolicyName;
    private String tenantDomain;
    private int tenantId;
    private String tenantAdminUsername;
    private String repositoryType = "git";
    private String repositoryURL;
    private boolean isPrivateRepository;
    private String repositoryUsername;
    private String repositoryPassword;
    private String lbClusterId;
    private Property[] properties;
    private String dataCartridgeAlias;
    private String lbAlias;
    private PersistanceMapping persistanceMapping;

    public String getCartridgeType() {
        return cartridgeType;
    }

    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public String getCartridgeAlias() {
        return cartridgeAlias;
    }

    public void setCartridgeAlias(String cartridgeAlias) {
        this.cartridgeAlias = cartridgeAlias;
    }

    public String getAutoscalingPolicyName() {
        return autoscalingPolicyName;
    }

    public void setAutoscalingPolicyName(String autoscalingPolicyName) {
        this.autoscalingPolicyName = autoscalingPolicyName;
    }

    public String getDeploymentPolicyName() {
        return deploymentPolicyName;
    }

    public void setDeploymentPolicyName(String deploymentPolicyName) {
        this.deploymentPolicyName = deploymentPolicyName;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantAdminUsername() {
        return tenantAdminUsername;
    }

    public void setTenantAdminUsername(String tenantAdminUsername) {
        this.tenantAdminUsername = tenantAdminUsername;
    }

    public String getRepositoryType() {
        return repositoryType;
    }

    public void setRepositoryType(String repositoryType) {
        this.repositoryType = repositoryType;
    }

    public String getRepositoryURL() {
        return repositoryURL;
    }

    public void setRepositoryURL(String repositoryURL) {
        this.repositoryURL = repositoryURL;
    }

    public boolean isPrivateRepository() {
        return isPrivateRepository;
    }

    public void setPrivateRepository(boolean isPrivateRepository) {
        this.isPrivateRepository = isPrivateRepository;
    }

    public String getRepositoryUsername() {
        return repositoryUsername;
    }

    public void setRepositoryUsername(String repositoryUsername) {
        this.repositoryUsername = repositoryUsername;
    }

    public String getRepositoryPassword() {
        return repositoryPassword;
    }

    public void setRepositoryPassword(String repositoryPassword) {
        this.repositoryPassword = repositoryPassword;
    }

    public String getLbClusterId() {
        return lbClusterId;
    }

    public void setLbClusterId(String lbClusterId) {
        this.lbClusterId = lbClusterId;
    }

    public Property[] getProperties() {
        return properties;
    }

    public void setProperties(Property[] properties) {
        this.properties = properties;
    }

    public void setDataCartridgeAlias(String dataCartridgeAlias) {this.dataCartridgeAlias = dataCartridgeAlias; }

    public String getDataCartridgeAlias() {return dataCartridgeAlias;}

    public String getLbAlias() {
        return lbAlias;
    }

    public void setLbAlias(String lbAlias) {
        this.lbAlias = lbAlias;
    }

    public PersistanceMapping getPersistanceMapping() {
        return persistanceMapping;
    }

    public void setPersistanceMapping(PersistanceMapping persistanceMapping) {
        this.persistanceMapping = persistanceMapping;
    }
}
