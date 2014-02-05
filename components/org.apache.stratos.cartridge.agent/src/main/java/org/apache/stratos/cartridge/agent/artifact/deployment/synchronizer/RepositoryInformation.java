/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer;

/**
 * @author wso2
 *
 */
public class RepositoryInformation {

	private String repoUrl;
	private String repoUsername;
	private String repoPassword;
	private String repoPath;
	private String tenantId;
	private boolean isMultitenant;
	
	public String getRepoUrl() {
		return repoUrl;
	}
	public void setRepoUrl(String repoUrl) {
		this.repoUrl = repoUrl;
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
	public String getRepoPath() {
		return repoPath;
	}
	public void setRepoPath(String repoPath) {
		this.repoPath = repoPath;
	}
	public String getTenantId() {
		return tenantId;
	}
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	public boolean isMultitenant() {
		return isMultitenant;
	}
	public void setMultitenant(boolean isMultitenant) {
		this.isMultitenant = isMultitenant;
	}
	
}
