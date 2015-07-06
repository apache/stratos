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

import java.io.Serializable;
import java.util.Arrays;


/**
 * The model class for NetworkPartition definition.
 */
public class NetworkPartition implements Serializable {

    private static final long serialVersionUID = -8043298009352097370L;

    private String uuid;
    private String id;
    private int tenantId;
    private String provider;
    private boolean activeByDefault;
    private Partition[] partitions;
    private String partitionAlgo;
    private Properties properties;

    /**
     * Gets the local Id of the network partition.
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the local Id of the network partition.
     */
    public void setId(String id) {
        this.id = id;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public void setPartitions(Partition[] partitions) {
        if (partitions == null) {
            this.partitions = partitions;
        } else {
            this.partitions = Arrays.copyOf(partitions, partitions.length);
        }
    }

    /**
     * Gets the value of the partitions.
     */
    public Partition[] getPartitions() {
        if (partitions == null) {
            partitions = new Partition[0];
        }
        return this.partitions;
    }

    /**
     * Gets the global Id network partition
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Sets the global Id network partition
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isActiveByDefault() {
        return activeByDefault;
    }

    public void setActiveByDefault(boolean activeByDefault) {
        this.activeByDefault = activeByDefault;
    }

    public Partition getPartition(String partitionId) {
        for (Partition partition : partitions) {
            if (partition.getId().equals(partitionId)) {
                return partition;
            }
        }
        return null;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getPartitionAlgo() {
        return partitionAlgo;
    }

    public void setPartitionAlgo(String partitionAlgo) {
        this.partitionAlgo = partitionAlgo;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
