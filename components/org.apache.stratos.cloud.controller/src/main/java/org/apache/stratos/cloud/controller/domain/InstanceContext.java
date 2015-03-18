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

/**
 * Instance context defines information required for starting an IaaS instance.
 */
public class InstanceContext {
    private String clusterId;
    private String clusterInstanceId;
    private long initTime;
    private long obsoleteExpiryTime;
    private String networkPartitionId;
    private Partition partition;
    private String cartridgeType;
    private Properties properties;
    private boolean isVolumeRequired;
    private Volume[] volumes;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public String getCartridgeType() {
        return cartridgeType;
    }

    public void setCartridgeType(String cartridgeType) {
        this.cartridgeType = cartridgeType;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }

    public void setClusterInstanceId(String clusterInstanceId) {
        this.clusterInstanceId = clusterInstanceId;
    }

    public long getInitTime() {
        return initTime;
    }

    public void setInitTime(long initTime) {
        this.initTime = initTime;
    }

    public long getObsoleteExpiryTime() {
        return obsoleteExpiryTime;
    }

    public void setObsoleteExpiryTime(long obsoleteExpiryTime) {
        this.obsoleteExpiryTime = obsoleteExpiryTime;
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
        this.volumes = ArrayUtils.clone(volumes);
    }

}
