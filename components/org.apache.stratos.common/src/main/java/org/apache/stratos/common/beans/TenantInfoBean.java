/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.common.beans;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Bean class for Tenant Information
 */
@XmlRootElement
public class TenantInfoBean {

    private String admin; //admin's user name
    private String firstName;
    private String lastName;
    private String adminPassword;
    private String tenantDomain;
    private int tenantId;
    private String email;
    private boolean active;
    private String successKey;
    private long createdDate;
    private String originatedService;
    private String usagePlan;

    public TenantInfoBean() {

    }

    /*copy constructor*/
    public TenantInfoBean(TenantInfoBean tenantInfoBean) {
        this.admin = tenantInfoBean.admin;
        this.firstName = tenantInfoBean.firstName;
        this.lastName = tenantInfoBean.lastName;
        this.adminPassword = tenantInfoBean.adminPassword;
        this.tenantDomain = tenantInfoBean.tenantDomain;
        this.tenantId = tenantInfoBean.tenantId;
        this.email = tenantInfoBean.email;
        this.active = tenantInfoBean.active;
        this.successKey = tenantInfoBean.successKey;
        this.createdDate = tenantInfoBean.createdDate;
        this.originatedService = tenantInfoBean.originatedService;
        this.usagePlan = tenantInfoBean.usagePlan;
    }

    public String getFirstname() {
        return firstName;
    }

    public void setFirstname(String firstName) {
        this.firstName = firstName;
    }

    public String getLastname() {
        return lastName;
    }

    public void setLastname(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {

        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getAdmin() {
        return admin;
    }

    public void setAdmin(String admin) {
        this.admin = admin;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getSuccessKey() {
        return successKey;
    }

    public void setSuccessKey(String successKey) {
        this.successKey = successKey;
    }

    public long getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(long createdDate) {
        this.createdDate = createdDate;
    }

    public String getOriginatedService() {
        return originatedService;
    }

    public void setOriginatedService(String originatedService) {
        this.originatedService = originatedService;
    }

    public String getUsagePlan() {
        return usagePlan;
    }

    public void setUsagePlan(String usagePlan) {
        this.usagePlan = usagePlan;
    }
}
