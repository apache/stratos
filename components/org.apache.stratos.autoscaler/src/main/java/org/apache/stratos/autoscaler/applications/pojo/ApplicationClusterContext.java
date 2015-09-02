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

public class ApplicationClusterContext implements Serializable {

    private static final long serialVersionUID = -7971548249412362885L;

    // cluster id
    private String clusterId;
    // cartridge uuid
    private String cartridgeUuid;
    // cartridge type
    private String cartridgeType;
    // payload as a String
    private String textPayload;
    // host name
    private String hostName;
    // autoscaling policy
    private String autoscalePolicyName;
    // deployment policy
    private String deploymentPolicyName;
    // tenant range
    private String tenantRange;
    // properties
    private Properties properties;

    private String[] dependencyClusterIds;
    private PersistenceContext persistenceContext;

    public ApplicationClusterContext(String cartridgeUuid, String clusterId, String hostName,
                                     String textPayload, String deploymentPolicyName, boolean isLbCluster,
                                     String tenantRange, String[] dependencyClusterIds) {

        this.cartridgeUuid = cartridgeUuid;
        this.clusterId = clusterId;
        this.hostName = hostName;
        this.textPayload = textPayload;
        this.deploymentPolicyName = deploymentPolicyName;
        this.tenantRange = tenantRange;
        this.dependencyClusterIds = dependencyClusterIds;
    }

    public PersistenceContext getPersistenceContext() {
        return persistenceContext;
    }

    public void setPersistenceContext(PersistenceContext persistenceContext) {
        this.persistenceContext = persistenceContext;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getCartridgeUuid() {
        return cartridgeUuid;
    }

    public void setCartridgeUuid(String cartridgeUuid) {
        this.cartridgeUuid = cartridgeUuid;
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public String getTextPayload() {
        return textPayload;
    }

    public void setTextPayload(String textPayload) {
        this.textPayload = textPayload;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getAutoscalePolicyName() {
        return autoscalePolicyName;
    }

    public void setAutoscalePolicyName(String autoscalePolicyName) {
        this.autoscalePolicyName = autoscalePolicyName;
    }

    public String getDeploymentPolicyName() {
        return deploymentPolicyName;
    }

    public void setDeploymentPolicyName(String deploymentPolicyName) {
        this.deploymentPolicyName = deploymentPolicyName;
    }

    public String getTenantRange() {
        return tenantRange;
    }

    public void setTenantRange(String tenantRange) {
        this.tenantRange = tenantRange;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public boolean equals(Object other) {

        if (other == null || !(other instanceof ApplicationClusterContext)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        ApplicationClusterContext that = (ApplicationClusterContext) other;

        return this.cartridgeUuid.equals(that.cartridgeUuid) &&
                this.clusterId.equals(that.clusterId);
    }

    public int hashCode() {
        return this.cartridgeUuid.hashCode() + this.clusterId.hashCode();
    }

    public String[] getDependencyClusterIds() {
        return dependencyClusterIds;
    }

    public void setDependencyClusterIds(String[] dependencyClusterIds) {
        this.dependencyClusterIds = dependencyClusterIds;
    }
}
