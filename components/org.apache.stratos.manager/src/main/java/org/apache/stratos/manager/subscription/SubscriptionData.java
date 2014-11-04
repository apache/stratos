/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.manager.subscription;

import org.apache.stratos.cloud.controller.stub.pojo.Persistence;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
    private PersistenceContext persistanceCtxt;
    private boolean isCommitsEnabled;
    private String serviceGroup;
    private Set<String> domains;
    private Persistence persistence;
    private Properties properties;
    private String serviceName;

    public SubscriptionData() {
        this.domains = new HashSet<String>();
    }

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

    public PersistenceContext getPersistanceContext() {
        return persistanceCtxt;
    }

    public void setPersistanceCtxt(PersistenceContext persistanceCtxt) {
        this.persistanceCtxt = persistanceCtxt;
    }

	public boolean isCommitsEnabled() {
		return isCommitsEnabled;
	}

	public void setCommitsEnabled(boolean isCommitsEnabled) {
		this.isCommitsEnabled = isCommitsEnabled;
	}

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

    public void addDomains(Set<String> domains) {
        domains.addAll(domains);
    }

    public void removeDomain(String domain) {
        domains.remove(domain);
    }

    public void removeDomains(Set<String> domains) {
        domains.removeAll(domains);
    }

    public Set<String> getDomains() {
        return Collections.unmodifiableSet(domains);
    }

    public Persistence getPersistence() {
        return persistence;
    }

     public void setPersistence(Persistence persistence) {
        this.persistence = persistence;
     }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

}
