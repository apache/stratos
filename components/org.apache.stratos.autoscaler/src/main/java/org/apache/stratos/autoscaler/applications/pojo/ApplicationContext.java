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

package org.apache.stratos.autoscaler.applications.pojo;

import org.apache.stratos.common.Properties;

import java.io.Serializable;

public class ApplicationContext implements Serializable {

    private static final long serialVersionUID = 6704036501869668646L;

    public static final String STATUS_CREATED = "Created";
    public static final String STATUS_DEPLOYED = "Deployed";
    public static final String STATUS_UNDEPLOYING = "Undeploying";

    private String applicationId;
    private String alias;
    private boolean multiTenant;
    private String name;
    private String description;
    private int tenantId;
    private String tenantDomain;
    private String tenantAdminUsername;
    private ComponentContext componentContext;
    private Properties properties;
    private String status;

    public ApplicationContext() {
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(boolean multiTenant) {
        this.multiTenant = multiTenant;
    }

    public ComponentContext getComponents() {
        return componentContext;
    }

    public void setComponents(ComponentContext componentContext) {
        this.componentContext = componentContext;
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

    public String getTenantAdminUsername() {
        return tenantAdminUsername;
    }

    public void setTenantAdminUsername(String tenantAdminUsername) {
        this.tenantAdminUsername = tenantAdminUsername;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
