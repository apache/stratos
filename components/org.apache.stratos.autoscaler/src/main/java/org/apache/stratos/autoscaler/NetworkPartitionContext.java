/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler;

import java.util.*;

/**
 * Holds runtime data of a network partition.
 * @author nirmal
 *
 */
public class NetworkPartitionContext {

    private String provider;

    private String region;

    private String defaultLbClusterId;

    private Map<String, String> serviceNameToLBClusterIdMap;

    private Map<String, String> clusterIdToLBClusterIdMap;

    public NetworkPartitionContext(final String provider, final String region) {

        super();
        this.provider = provider;
        this.region = region;
        this.setServiceToLBClusterId(new HashMap<String, String>());
        this.setClusterIdToLBClusterIdMap(new HashMap<String, String>());

    }

    public boolean isLBExist(final String clusterId) {

        return clusterId != null &&
               (clusterId.equals(this.defaultLbClusterId) ||
                this.serviceNameToLBClusterIdMap.containsValue(clusterId) || this.clusterIdToLBClusterIdMap.containsValue(clusterId));

    }

    public String getProvider() {

        return this.provider;

    }

    public void setProvider(final String provider) {

        this.provider = provider;

    }

    public String getRegion() {

        return this.region;

    }

    public void setRegion(final String region) {

        this.region = region;

    }

    public String getDefaultLbClusterId() {

        return this.defaultLbClusterId;

    }

    public void setDefaultLbClusterId(final String defaultLbClusterId) {

        this.defaultLbClusterId = defaultLbClusterId;

    }

    public String getLBClusterIdOfService(final String serviceName) {

        return (String) this.serviceNameToLBClusterIdMap.get(serviceName);

    }

    public Map<String, String> getServiceToLBClusterId() {

        return this.serviceNameToLBClusterIdMap;

    }

    public void setServiceToLBClusterId(final Map<String, String> serviceToLBClusterId) {

        this.serviceNameToLBClusterIdMap = serviceToLBClusterId;

    }

    public String getLBClusterIdOfCluster(final String clusterId) {

        return (String) this.clusterIdToLBClusterIdMap.get(clusterId);

    }

    public Map<String, String> getClusterIdToLBClusterIdMap() {

        return this.clusterIdToLBClusterIdMap;

    }

    public void setClusterIdToLBClusterIdMap(final Map<String, String> clusterIdToLBClusterIdMap) {

        this.clusterIdToLBClusterIdMap = clusterIdToLBClusterIdMap;

    }

    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = 31 * result + ((this.provider == null) ? 0 : this.provider.hashCode());
        result = 31 * result + ((this.region == null) ? 0 : this.region.hashCode());
        return result;

    }

    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NetworkPartitionContext)) {
            return false;
        }
        final NetworkPartitionContext other = (NetworkPartitionContext) obj;
        if (this.provider == null) {
            if (other.provider != null) {
                return false;
            }
        }
        else if (!this.provider.equals(other.provider)) {
            return false;
        }
        if (this.region == null) {
            if (other.region != null) {
                return false;
            }
        }
        else if (!this.region.equals(other.region)) {
            return false;
        }
        return true;
    }

}