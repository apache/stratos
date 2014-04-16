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
package org.apache.stratos.cli.beans.cartridge;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class CartridgeInfoBean {
    String cartridgeType;
    String alias;
    String policy;
    String repoURL;
    String repoUsername;
    String repoPassword;
    private String autoscalePolicy;
    private String deploymentPolicy;
    private String size;

    boolean privateRepo;
    private boolean removeOnTermination;
    private boolean persistanceRequired;
    private boolean commitsEnabled;

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

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
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

    public String getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public void setAutoscalePolicy(String autoscalePolicy) {
        this.autoscalePolicy = autoscalePolicy;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public boolean isRemoveOnTermination() {
        return removeOnTermination;
    }

    public void setRemoveOnTermination(boolean removeOnTermination) {
        this.removeOnTermination = removeOnTermination;
    }

    public boolean isPersistanceRequired() {
        return persistanceRequired;
    }

    public void setPersistanceRequired(boolean persistanceRequired) {
        this.persistanceRequired = persistanceRequired;
    }

	public boolean isCommitsEnabled() {
		return commitsEnabled;
	}

	public void setCommitsEnabled(boolean commitsEnabled) {
		this.commitsEnabled = commitsEnabled;
	}
    
    
}
