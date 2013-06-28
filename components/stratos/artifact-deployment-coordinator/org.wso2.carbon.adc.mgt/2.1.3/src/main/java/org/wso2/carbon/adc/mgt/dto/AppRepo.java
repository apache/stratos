/*
 * Copyright WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.adc.mgt.dto;

public class AppRepo {

	private int tenantId;
	private String repoName;
	private String cartridge;
	private String appName;
	private boolean isWebRoot;
	private String tenantPubKey;
	private String tenantCartridgePubKey;

	public int getTenantId() {
		return tenantId;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public String getRepoName() {
		return repoName;
	}

	public void setRepoName(String repoName) {
		this.repoName = repoName;
	}

	public String getCartridge() {
		return cartridge;
	}

	public void setCartridge(String cartridge) {
		this.cartridge = cartridge;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public boolean isWebRoot() {
		return isWebRoot;
	}

	public void setWebRoot(boolean isWebRoot) {
		this.isWebRoot = isWebRoot;
	}

	public String getTenantPubKey() {
		return tenantPubKey;
	}

	public void setTenantPubKey(String tenantPubKey) {
		this.tenantPubKey = tenantPubKey;
	}

	public String getTenantCartridgePubKey() {
		return tenantCartridgePubKey;
	}

	public void setTenantCartridgePubKey(String tenantCartridgePubKey) {
		this.tenantCartridgePubKey = tenantCartridgePubKey;
	}

}
