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

import org.apache.stratos.cloud.controller.stub.pojo.Properties;

import java.io.Serializable;

public class ApplicationContext implements Serializable {

    private static final long serialVersionUID = 6704036501869668646L;

    private int tenantId;

    private String tenantDomain;

    private String teantAdminUsername;

    private String applicationId;

    private String alias;

    private ComponentContext componentContext;

    private SubscribableInfoContext[] subscribableInfoContexts;

    private Properties properties;

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

    public ComponentContext getComponents() {
        return componentContext;
    }

    public void setComponents(ComponentContext componentContext) {
        this.componentContext = componentContext;
    }

    public SubscribableInfoContext[] getSubscribableInfoContext() {
        return subscribableInfoContexts;
    }

    public void setSubscribableInfoContext(SubscribableInfoContext[] subscribableInfoContexts) {
        this.subscribableInfoContexts = subscribableInfoContexts;
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

    public String getTeantAdminUsername() {
        return teantAdminUsername;
    }

    public void setTeantAdminUsername(String teantAdminUsername) {
        this.teantAdminUsername = teantAdminUsername;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
