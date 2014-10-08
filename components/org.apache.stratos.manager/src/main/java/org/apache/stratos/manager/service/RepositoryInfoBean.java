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

package org.apache.stratos.manager.service;

import java.util.Arrays;

/**
 *
 */
public class RepositoryInfoBean {

	private String repoURL;
	private String cartridgeAlias;
	private String tenantDomain;
	private String userName;
	private String password;
	private String[] dirArray;

	public RepositoryInfoBean(String repoURL, String cartridgeAlias, String tenantDomain,
                              String userName, String password, String[] dirArray) {
	    this.repoURL = repoURL;
	    this.cartridgeAlias = cartridgeAlias;
	    this.tenantDomain = tenantDomain;
	    this.userName = userName;
	    this.setPassword(password);
        setDirArray(dirArray);
    }
	public String getRepoURL() {
    	return repoURL;
    }
	public void setRepoURL(String repoURL) {
    	this.repoURL = repoURL;
    }
	public String getCartridgeAlias() {
    	return cartridgeAlias;
    }
	public void setCartridgeAlias(String cartridgeAlias) {
    	this.cartridgeAlias = cartridgeAlias;
    }
	public String getTenantDomain() {
    	return tenantDomain;
    }
	public void setTenantDomain(String tenantDomain) {
    	this.tenantDomain = tenantDomain;
    }
	public String getUserName() {
    	return userName;
    }
	public void setUserName(String userName) {
    	this.userName = userName;
    }
	public String[] getDirArray() {
    	return dirArray;
    }
	public void setDirArray(String[] dirArray) {
        if(dirArray == null) {
            this.dirArray = new String[0];
        } else {
            this.dirArray = Arrays.copyOf(dirArray, dirArray.length);
        }
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
}
