///**
// *  Licensed to the Apache Software Foundation (ASF) under one
// *  or more contributor license agreements.  See the NOTICE file
// *  distributed with this work for additional information
// *  regarding copyright ownership.  The ASF licenses this file
// *  to you under the Apache License, Version 2.0 (the
// *  "License"); you may not use this file except in compliance
// *  with the License.  You may obtain a copy of the License at
//
// *  http://www.apache.org/licenses/LICENSE-2.0
//
// *  Unless required by applicable law or agreed to in writing,
// *  software distributed under the License is distributed on an
// *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// *  KIND, either express or implied.  See the License for the
// *  specific language governing permissions and limitations
// *  under the License.
// */
//
//package org.apache.stratos.adc.mgt.payload;
//
//import org.apache.stratos.adc.mgt.dto.Policy;
//import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
//
//public class PayloadArg {
//
//    //basic
//    private String hostName;
//    private String tenantRange;
//    private int tenantId;
//    private String serviceName;
//    private String cartridgeAlias;
//    private String tenantDomain;
//    private CartridgeInfo cartridgeInfo;
//    private Policy policy;
//    private String repoURL;
//    private boolean multitenant;
//
//    //other
//	private String userDefinedPayload;
//    private String serviceDomain;
//    private String serviceSubDomain;
//    private String mgtServiceDomain;
//    private String mgtServiceSubDomain;
//    private String deployment;
//    private String subscriptionKey;
//
//    //data cartridge specific
//    private String dataCartridgeHost;
//    private String dataCartridgeAdminUser;
//    private String dataCartridgeAdminPassword;
//
//
//	public CartridgeInfo getCartridgeInfo() {
//		return cartridgeInfo;
//	}
//	public void setCartridgeInfo(CartridgeInfo cartridgeInfo) {
//		this.cartridgeInfo = cartridgeInfo;
//	}
//	public Policy getPolicy() {
//		return policy;
//	}
//	public void setPolicy(Policy policy) {
//		this.policy = policy;
//	}
//	public String getRepoURL() {
//		return repoURL;
//	}
//	public void setRepoURL(String repoURL) {
//		this.repoURL = repoURL;
//	}
//	public String getDataCartridgeAdminPassword() {
//		return dataCartridgeAdminPassword;
//	}
//	public void setDataCartridgeAdminPassword(String dataCartridgeAdminPassword) {
//		this.dataCartridgeAdminPassword = dataCartridgeAdminPassword;
//	}
//	public String getDataCartridgeHost() {
//		return dataCartridgeHost;
//	}
//	public void setDataCartridgeHost(String dataCartridgeHost) {
//		this.dataCartridgeHost = dataCartridgeHost;
//	}
//	public int getTenantId() {
//		return tenantId;
//	}
//	public void setTenantId(int tenantId) {
//		this.tenantId = tenantId;
//	}
//	public String getTenantDomain() {
//		return tenantDomain;
//	}
//	public void setTenantDomain(String tenantDomain) {
//		this.tenantDomain = tenantDomain;
//	}
//	public String getUserDefinedPayload() {
//		return userDefinedPayload;
//	}
//	public void setUserDefinedPayload(String userDefinedPayload) {
//		this.userDefinedPayload = userDefinedPayload;
//	}
//	public boolean isMultitenant() {
//		return multitenant;
//	}
//	public void setMultitenant(boolean multitenant) {
//		this.multitenant = multitenant;
//	}
//	public String getCartridgeAlias() {
//		return cartridgeAlias;
//	}
//	public void setCartridgeAlias(String cartridgeAlias) {
//		this.cartridgeAlias = cartridgeAlias;
//	}
//
//    public String getTenantRange() {
//        return tenantRange;
//    }
//
//    public void setTenantRange(String tenantRange) {
//        this.tenantRange = tenantRange;
//    }
//
//    public String getHostName() {
//        return hostName;
//    }
//
//    public void setHostName(String hostName) {
//        this.hostName = hostName;
//    }
//
//    public String getServiceDomain() {
//        return serviceDomain;
//    }
//
//    public void setServiceDomain(String serviceDomain) {
//        this.serviceDomain = serviceDomain;
//    }
//
//    public String getServiceSubDomain() {
//        return serviceSubDomain;
//    }
//
//    public void setServiceSubDomain(String serviceSubDomain) {
//        this.serviceSubDomain = serviceSubDomain;
//    }
//
//    public String getMgtServiceDomain() {
//        return mgtServiceDomain;
//    }
//
//    public void setMgtServiceDomain(String mgtServiceDomain) {
//        this.mgtServiceDomain = mgtServiceDomain;
//    }
//
//    public String getMgtServiceSubDomain() {
//        return mgtServiceSubDomain;
//    }
//
//    public void setMgtServiceSubDomain(String mgtServiceSubDomain) {
//        this.mgtServiceSubDomain = mgtServiceSubDomain;
//    }
//
//    public String getDataCartridgeAdminUser() {
//        return dataCartridgeAdminUser;
//    }
//
//    public void setDataCartridgeAdminUser(String dataCartridgeAdminUser) {
//        this.dataCartridgeAdminUser = dataCartridgeAdminUser;
//    }
//
//    public String getDeployment() {
//        return deployment;
//    }
//
//    public void setDeployment(String deployment) {
//        this.deployment = deployment;
//    }
//
//    public String getServiceName() {
//        return serviceName;
//    }
//
//    public void setServiceName(String serviceName) {
//        this.serviceName = serviceName;
//    }
//	public String getSubscriptionKey() {
//		return subscriptionKey;
//	}
//	public void setSubscriptionKey(String subscriptionKey) {
//		this.subscriptionKey = subscriptionKey;
//	}
//
//
//}
