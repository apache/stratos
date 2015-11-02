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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.stratos.common.Properties;
import org.apache.stratos.messaging.domain.topology.KubernetesService;

import java.io.Serializable;
import java.util.List;

/**
 * Holds runtime data of a Cluster.
 */
public class ClusterContext implements Serializable {

    private static final long serialVersionUID = 4830667953031229223L;

    private final String applicationId;
    private final String clusterId;
    private final String cartridgeType;
    private final String payload;
    private final String hostName;
    private final boolean isLbCluster;
    private boolean isVolumeRequired;
    private Volume[] volumes;
    // timeout in milliseconds - this would be the per member time that CC waits before forcefully terminate instances
    // on an unregistration.
    private long timeoutInMillis;
    private Properties properties;
    private List<KubernetesService> kubernetesServices;
    private String kubernetesClusterId;

    public ClusterContext(String applicationId, String cartridgeType, String clusterId, String payload, String hostName,
                          boolean isLbCluster, Properties properties) {

        this.applicationId = applicationId;
        this.cartridgeType = cartridgeType;
        this.clusterId = clusterId;
        this.payload = payload;
        this.hostName = hostName;
        this.isLbCluster = isLbCluster;
        this.setProperties(properties);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    public String getPayload() {
        return payload;
    }

    public String getHostName() {
        return hostName;
    }

    public boolean isLbCluster() {
        return isLbCluster;
    }

    public boolean isVolumeRequired() {
        return isVolumeRequired;
    }

    public void setVolumeRequired(boolean isVolumeRequired) {
        this.isVolumeRequired = isVolumeRequired;
    }

    public long getTimeoutInMillis() {
        return timeoutInMillis;
    }

    public void setTimeoutInMillis(long timeoutInMillis) {
        this.timeoutInMillis = timeoutInMillis;
    }

    public Volume[] getVolumes() {
        return volumes;
    }

    public void setVolumes(Volume[] volumes) {
        this.volumes = ArrayUtils.clone(volumes);
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public List<KubernetesService> getKubernetesServices() {
        return kubernetesServices;
    }

    public void setKubernetesServices(List<KubernetesService> kubernetesServices) {
        this.kubernetesServices = kubernetesServices;
    }

    public void setKubernetesClusterId(String kubernetesClusterId) {
        this.kubernetesClusterId = kubernetesClusterId;
    }

    public String getKubernetesClusterId() {
        return kubernetesClusterId;
    }
}
