/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.dao;

import org.apache.stratos.manager.repository.Repository;

import java.util.List;

public class CartridgeSubscriptionInfo {

	private int subscriptionId;
	private int tenantId;
	private String cartridge;
	private String provider;
	private String hostName;
	private String policy;
	private List<PortMapping> portMappings;
	private String clusterDomain;
	private String clusterSubdomain;
	private Repository repository;
	private String state;
	private String alias;
	private String tenantDomain;
	private DataCartridge dataCartridge;
	private String baseDirectory;
	private String mappedDomain;
	private String mgtClusterDomain;
	private String mgtClusterSubDomain;
	private String subscriptionKey;

	public int getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscriptionId(int subscriptionId) {
		this.subscriptionId = subscriptionId;
	}

	public int getTenantId() {
		return tenantId;
	}

	public void setTenantId(int tenantId) {
		this.tenantId = tenantId;
	}

	public String getCartridge() {
		return cartridge;
	}

	public void setCartridge(String cartridge) {
		this.cartridge = cartridge;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getPolicy() {
		return policy;
	}

	public void setPolicy(String policy) {
		this.policy = policy;
	}

	public List<PortMapping> getPortMappings() {
		return portMappings;
	}

	public void setPortMappings(List<PortMapping> portMappings) {
		this.portMappings = portMappings;
	}

	public String getClusterDomain() {
		return clusterDomain;
	}

	public void setClusterDomain(String clusterDomain) {
		this.clusterDomain = clusterDomain;
	}

	public String getClusterSubdomain() {
		return clusterSubdomain;
	}

	public void setClusterSubdomain(String clusterSubdomain) {
		this.clusterSubdomain = clusterSubdomain;
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getTenantDomain() {
		return tenantDomain;
	}

	public void setTenantDomain(String tenantDomain) {
		this.tenantDomain = tenantDomain;
	}

	public DataCartridge getDataCartridge() {
		return dataCartridge;
	}

	public void setDataCartridge(DataCartridge dataCartridge) {
		this.dataCartridge = dataCartridge;
	}

	public String getBaseDirectory() {
		return baseDirectory;
	}

	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public String getMappedDomain() {
		return mappedDomain;
	}

	public void setMappedDomain(String mappedDomain) {
		this.mappedDomain = mappedDomain;
	}

	public String getMgtClusterDomain() {
		return mgtClusterDomain;
	}

	public void setMgtClusterDomain(String mgtClusterDomain) {
		this.mgtClusterDomain = mgtClusterDomain;
	}

	public String getMgtClusterSubDomain() {
		return mgtClusterSubDomain;
	}

	public void setMgtClusterSubDomain(String mgtClusterSubDomain) {
		this.mgtClusterSubDomain = mgtClusterSubDomain;
	}

	public String getSubscriptionKey() {
		return subscriptionKey;
	}

	public void setSubscriptionKey(String subscriptionKey) {
		this.subscriptionKey = subscriptionKey;
	}	

}
