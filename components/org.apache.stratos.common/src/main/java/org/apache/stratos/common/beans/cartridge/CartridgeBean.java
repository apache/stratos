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

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "cartridgeDefinitionBean")
public class CartridgeBean {

	private String type;
	private String host;
	private String provider;
    /**
     * Category: Application|Framework|Data|LoadBalancer
     */
    private String category;
	private String displayName;
	private String description;
	private String version;
	private boolean multiTenant;
	private boolean isPublic;
    private DeploymentBean deployment;
    private List<PortMappingBean> portMapping;
    private String tenantPartitions;
    private PersistenceBean persistence;
    private List<IaasProviderBean> iaasProvider;
    private String defaultAutoscalingPolicy;
    private String defaultDeploymentPolicy;
    private String serviceGroup;
    private List<PropertyBean> property;
    private List<String> exportingProperties;
	private String[] metadataKeys;

    public String toString () {

        return "Type: " + getType() + ", Provider: " + getProvider() + ", Category: " + getCategory() + ", Host: " + getHost() + ", Display Name: " + getDisplayName() +
                ", Description: " + getDescription() +  ", Version: " + getVersion() + ", Multitenant " + isMultiTenant() +", Public " + isPublic() + "\n" +
                getDeploymentDetails() + "\n PortMapping: " + getPortMappings() + "\n IaaS: " + getIaasProviders() +
                "\n Properties: " + getProperties() +"\n VolumeBean mappings "+ getPersistence().toString()
                + "\n Exports " + getExportingProperties().toString();
    }

	private String getDeploymentDetails () {

        if(getDeployment() != null) {
            return getDeployment().toString();
        }
        return null;
    }

    private String getPortMappings () {

        StringBuilder portMappingBuilder = new StringBuilder();
        if(getPortMapping() != null && !getPortMapping().isEmpty()) {
            for(PortMappingBean portMappingBean : getPortMapping()) {
                portMappingBuilder.append(portMappingBean.toString());
            }
        }
        return portMappingBuilder.toString();
    }

    private String getIaasProviders () {

        StringBuilder iaasBuilder = new StringBuilder();
        if(getIaasProvider() != null && !getIaasProvider().isEmpty()) {
            for(IaasProviderBean iaasProviderBean : getIaasProvider()) {
                iaasBuilder.append(iaasProviderBean.toString());
            }
        }
        return iaasBuilder.toString();
    }

    private String getProperties () {

        StringBuilder propertyBuilder = new StringBuilder();
        if(getProperty() != null) {
            for(PropertyBean propertyBean : getProperty()) {
                propertyBuilder.append(propertyBean.getName() + " : " + propertyBean.getValue() + " | ");
            }
        }
        return propertyBuilder.toString();
    }

    public List<String> getExportingProperties() {
        return exportingProperties;
    }

    public void setExportingProperties(List<String> exportingProperties) {
        this.exportingProperties = exportingProperties;
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

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
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

    public String getDefaultAutoscalingPolicy() {
        return defaultAutoscalingPolicy;
    }

    public void setDefaultAutoscalingPolicy(String defaultAutoscalingPolicy) {
        this.defaultAutoscalingPolicy = defaultAutoscalingPolicy;
    }

    public String getDefaultDeploymentPolicy() {
        return defaultDeploymentPolicy;
    }

    public void setDefaultDeploymentPolicy(String defaultDeploymentPolicy) {
        this.defaultDeploymentPolicy = defaultDeploymentPolicy;
    }

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public String getTenantPartitions() {
        return tenantPartitions;
    }

    public void setTenantPartitions(String tenantPartitions) {
        this.tenantPartitions = tenantPartitions;
    }

	public String[] getMetadataKeys() {
		return metadataKeys;
	}

	public void setMetadataKeys(String[] metadataKeys) {
		this.metadataKeys = metadataKeys;
	}
}
