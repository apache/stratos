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
package org.apache.stratos.rest.endpoint.bean;

import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PersistenceBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
public class CartridgeInfoBean {
    String cartridgeType;
    String alias;
    String autoscalePolicy;
    String deploymentPolicy;
    String repoURL;
    boolean privateRepo;
    String repoUsername;
    String repoPassword;
    String dataCartridgeType;
    String dataCartridgeAlias;
    boolean commitsEnabled;

    private String serviceGroup;

    private PersistenceBean persistence;

    private List<PropertyBean> property;

    private List<String> domains;

    public CartridgeInfoBean() {
        this.domains = new ArrayList<String>();
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public void setAutoscalePolicy(String autoscalePolicy) {
        this.autoscalePolicy = autoscalePolicy;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String repoURL) {
        this.repoURL = repoURL;
    }

    public boolean isPrivateRepo() {
        return privateRepo;
    }

    public void setPrivateRepo(boolean privateRepo) {
        this.privateRepo = privateRepo;
    }

    public String getRepoUsername() {
        return repoUsername;
    }

    public void setRepoUsername(String repoUsername) {
        this.repoUsername = repoUsername;
    }

    public String getRepoPassword() {
        return repoPassword;
    }

    public void setRepoPassword(String repoPassword) {
        this.repoPassword = repoPassword;
    }

    public String getDataCartridgeType() {
        return dataCartridgeType;
    }

    public void setDataCartridgeType(String dataCartridgeType) {
        this.dataCartridgeType = dataCartridgeType;
    }

    public String getDataCartridgeAlias() {
        return dataCartridgeAlias;
    }

    public void setDataCartridgeAlias(String dataCartridgeAlias) {
        this.dataCartridgeAlias = dataCartridgeAlias;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public boolean isCommitsEnabled() {
		return commitsEnabled;
	}

	public void setCommitsEnabled(boolean commitsEnabled) {
		this.commitsEnabled = commitsEnabled;
	}

	public String getServiceGroup() {
		return serviceGroup;
	}

	public void setServiceGroup(String serviceGroup) {
		this.serviceGroup = serviceGroup;
	}

    public PersistenceBean getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceBean persistence) {
        this.persistence = persistence;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }
    public List<String> getDomains() { return domains; }

    public void setDomains(List<String> domains) { this.domains = domains; }
}
