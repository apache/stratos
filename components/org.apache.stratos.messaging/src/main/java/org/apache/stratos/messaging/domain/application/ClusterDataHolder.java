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

package org.apache.stratos.messaging.domain.application;

import java.io.Serializable;

/**
 * Data Holder for a Cluster.
 * Will hold the Service Type and Cluster Id.
 */

public class ClusterDataHolder implements Serializable {

    // Service/Cartridge type
    private String serviceType;
    // Cluster id
    private String clusterId;

    private int minInstances;
    private int maxInstances;

    public ClusterDataHolder(String serviceType, String clusterId) {

        this.serviceType = serviceType;
        this.clusterId = clusterId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getClusterId() {
        return clusterId;
    }

    public int getMinInstances() {
        return minInstances;
    }

    public void setMinInstances(int minInstances) {
        this.minInstances = minInstances;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }
}
