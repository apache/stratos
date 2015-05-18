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

package org.apache.stratos.common.beans.cartridge;

import org.apache.stratos.common.beans.PropertyBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "cartridgeBean")
public class CartridgeBean {

    private String type;
    private String provider;
    /**
     * Category: framework|data|load-balancer
     */
    private String category;
    private String host;
    private String displayName;
    private String description;
    private String version;
    private boolean multiTenant;
    private DeploymentBean deployment;
    private List<PortMappingBean> portMapping;
    private String tenantPartitions;
    private List<IaasProviderBean> iaasProvider;
    private PersistenceBean persistence;
    /**
     * Load balancing ip type: public|private
     */
    private String loadBalancingIPType;
    private List<PropertyBean> property;

    private String getDeploymentDetails() {
        if (getDeployment() != null) {
            return getDeployment().toString();
        }
        return null;
    }

    private String getPortMappings() {
        StringBuilder portMappingBuilder = new StringBuilder();
        if (getPortMapping() != null && !getPortMapping().isEmpty()) {
            for (PortMappingBean portMappingBean : getPortMapping()) {
                portMappingBuilder.append(portMappingBean.toString());
            }
        }
        return portMappingBuilder.toString();
    }

    private String getIaasProviders() {

        StringBuilder iaasBuilder = new StringBuilder();
        if (getIaasProvider() != null && !getIaasProvider().isEmpty()) {
            for (IaasProviderBean iaasProviderBean : getIaasProvider()) {
                iaasBuilder.append(iaasProviderBean.toString());
            }
        }
        return iaasBuilder.toString();
    }

    private String getProperties() {

        StringBuilder propertyBuilder = new StringBuilder();
        if (getProperty() != null) {
            for (PropertyBean propertyBean : getProperty()) {
                propertyBuilder.append(propertyBean.getName() + " : " + propertyBean.getValue() + " | ");
            }
        }
        return propertyBuilder.toString();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(boolean multiTenant) {
        this.multiTenant = multiTenant;
    }

    public DeploymentBean getDeployment() {
        return deployment;
    }

    public void setDeployment(DeploymentBean deployment) {
        this.deployment = deployment;
    }

    public List<PortMappingBean> getPortMapping() {
        return portMapping;
    }

    public void setPortMapping(List<PortMappingBean> portMapping) {
        this.portMapping = portMapping;
    }

    public PersistenceBean getPersistence() {
        return persistence;
    }

    public void setPersistence(PersistenceBean persistence) {
        this.persistence = persistence;
    }

    public List<IaasProviderBean> getIaasProvider() {
        return iaasProvider;
    }

    public void setIaasProvider(List<IaasProviderBean> iaasProvider) {
        this.iaasProvider = iaasProvider;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

    public String getTenantPartitions() {
        return tenantPartitions;
    }

    public void setTenantPartitions(String tenantPartitions) {
        this.tenantPartitions = tenantPartitions;
    }

    public String getLoadBalancingIPType() {
        return loadBalancingIPType;
    }

    public void setLoadBalancingIPType(String loadBalancingIPType) {
        this.loadBalancingIPType = loadBalancingIPType;
    }

    public String toString() {

        return "Type: " + getType() +
                ", Provider: " + getProvider() +
                ", Category: " + getCategory() +
                ", Host: " + getHost() +
                ", Display Name: " + getDisplayName() +
                ", Description: " + getDescription() +
                ", Version: " + getVersion() +
                ", Multi-Tenant " + isMultiTenant() +
                ", Deployment" + getDeploymentDetails() +
                ", Port Mapping: " + getPortMappings() +
                ", Tenant Partitions: " + getTenantPartitions() +
                ", IaaS Providers: " + getIaasProviders() +
                ", Persistence " + (getPersistence() == null ? "" : getPersistence().toString()) +
                ", Load Balancing IP Type: " + getLoadBalancingIPType() +
                ", Properties: " + getProperties();
    }
}
