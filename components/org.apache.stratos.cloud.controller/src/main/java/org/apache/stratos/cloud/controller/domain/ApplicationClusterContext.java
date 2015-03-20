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

package org.apache.stratos.cloud.controller.domain;

import org.apache.stratos.common.Properties;

public class ApplicationClusterContext {

    // cluster id
    private String clusterId;
    // cartridge type
    private String cartridgeType;
    // payload as a String
    private String textPayload;
    // host name
    private String hostName;
    // flag to indicate LB cluster
    private boolean isLbCluster;
    // autoscaling policy
    private String autoscalePolicyName;
    // deployment policy
    private String deploymentPolicyName;
    // tenant rance
    private String tenantRange;
    // properties
    private Properties properties;
	//dependencyclusterid
	private String[] dependencyClusterIds;

    private boolean isVolumeRequired;

    private Volume[] volumes;

    public ApplicationClusterContext() {
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
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

    public boolean isLbCluster() {
        return isLbCluster;
    }

    public void setLbCluster(boolean lbCluster) {
        isLbCluster = lbCluster;
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

    public boolean equals(Object other) {

        if(other == null || !(other instanceof ApplicationClusterContext)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        ApplicationClusterContext that = (ApplicationClusterContext)other;

        return this.cartridgeType.equals(that.cartridgeType) &&
                this.clusterId.equals(that.clusterId);
    }

    public int hashCode() {
        return this.cartridgeType.hashCode() + this.clusterId.hashCode();
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

	public String[] getDependencyClusterIds() {
		return dependencyClusterIds;
	}

	public void setDependencyClusterIds(String[] dependencyClusterIds) {
		this.dependencyClusterIds = dependencyClusterIds;
	}

    public boolean isVolumeRequired() {
        return isVolumeRequired;
    }

    public void setVolumeRequired(boolean isVolumeRequired) {
        this.isVolumeRequired = isVolumeRequired;
    }

    public Volume[] getVolumes() {
        return volumes;
    }

    public void setVolumes(Volume[] volumes) {
        this.volumes = volumes;
    }

}
