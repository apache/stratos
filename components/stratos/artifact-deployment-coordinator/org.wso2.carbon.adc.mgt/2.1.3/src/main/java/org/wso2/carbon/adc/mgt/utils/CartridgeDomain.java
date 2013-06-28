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

package org.wso2.carbon.adc.mgt.utils;

/**
 *
 */
public class CartridgeDomain {

	private String primaryPort;
	private String proxyPort;
	private String type;
	private String tenantId;
	private int min;
	private int max;
	private String cartridgeType;
	private boolean volume;
	private String tenantDomain;
	private String clusterDomain;
	private String clusterSubDomain;

	private String hostName;
	private boolean started;

	public String getPrimaryPort() {
		return primaryPort;
	}

	public String getProxyPort() {
		return proxyPort;
	}

	public String getType() {
		return type;
	}

	public String getTenantId() {
		return tenantId;
	}

	public int getMin() {
		return min;
	}

	public int getMax() {
		return max;
	}

	public String getCartridgeType() {
		return cartridgeType;
	}

	public boolean getVolume() {
		return volume;
	}

	public String getTenantDomain() {
		return tenantDomain;
	}

	public void setPrimaryPort(String primaryPort) {
		this.primaryPort = primaryPort;
	}

	public void setProxyPort(String proxyPort) {
		this.proxyPort = proxyPort;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public void setCartridgeType(String cartridgeType) {
		this.cartridgeType = cartridgeType;
	}

	public void setVolume(boolean volume) {
		this.volume = volume;
	}

	public void setTenantDomain(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}

	public boolean isStarted() {
		return started;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getClusterDomain() {
		return clusterDomain;
	}

	public void setClusterDomain(String clusterDomain) {
		this.clusterDomain = clusterDomain;
	}

	public String getClusterSubDomain() {
		return clusterSubDomain;
	}

	public void setClusterSubDomain(String clusterSubDomain) {
		this.clusterSubDomain = clusterSubDomain;
	}

}
