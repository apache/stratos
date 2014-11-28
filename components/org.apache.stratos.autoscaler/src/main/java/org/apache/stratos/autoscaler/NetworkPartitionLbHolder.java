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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds LB data of a network partition.
 *
 */
public class NetworkPartitionLbHolder implements Serializable{

    private static final long serialVersionUID = -7181166769691018046L;
    private final String networkPartitionId;

    private String defaultLbClusterId;

    private Map<String, String> serviceNameToLBClusterIdMap;

    private Map<String, String> clusterIdToLBClusterIdMap;

//    private final String partitionAlgorithm;
//
//    //boolean values to keep whether the requests in flight parameters are reset or not
//    private boolean rifReset = false, averageRifReset = false, gradientRifReset = false, secondDerivativeRifRest = false;
//    //boolean values to keep whether the memory consumption parameters are reset or not
//    private boolean memoryConsumptionReset = false, averageMemoryConsumptionReset = false,
//            gradientMemoryConsumptionReset = false, secondDerivativeMemoryConsumptionRest = false;
//    //boolean values to keep whether the load average parameters are reset or not
//    private boolean loadAverageReset = false, averageLoadAverageReset = false, gradientLoadAverageReset = false,
//            secondDerivativeLoadAverageRest = false;
//
//    //FIXME this should be populated via NetworkPartitions
//    private int minInstanceCount = 1, maxInstanceCount = 1;
//
//    private final Partition[] partitions;

    //Following information will keep events details
//    private RequestsInFlight requestsInFlight;
//    private MemoryConsumption memoryConsumption;
//    private LoadAverage loadAverage;

    //details required for partition selection algorithms
//    private int currentPartitionIndex;
////    private Map<String, Integer> partitionToMemberCountMap;
//
//    //partitions of this network partition
//    private final Map<String, PartitionContext> partitionCtxts;

    public NetworkPartitionLbHolder(String networkPartitionId) {

        super();
        this.networkPartitionId = networkPartitionId;
//        this.partitionAlgorithm = partitionAlgo;
//        this.partitions = partitions;
        this.setServiceToLBClusterId(new HashMap<String, String>());
        this.setClusterIdToLBClusterIdMap(new HashMap<String, String>());
//        partitionToMemberCountMap = new HashMap<String, Integer>();
//        partitionCtxts = new HashMap<String, PartitionContext>();
//        requestsInFlight = new RequestsInFlight();
//        loadAverage = new LoadAverage();
//        memoryConsumption = new MemoryConsumption();

    }

    public String getDefaultLbClusterId() {

        return this.defaultLbClusterId;

    }

    public void setDefaultLbClusterId(final String defaultLbClusterId) {

        this.defaultLbClusterId = defaultLbClusterId;

    }

    public String getLBClusterIdOfService(final String serviceName) {

        return this.serviceNameToLBClusterIdMap.get(serviceName);

    }

    public Map<String, String> getServiceToLBClusterId() {

        return this.serviceNameToLBClusterIdMap;

    }

    public void setServiceToLBClusterId(final Map<String, String> serviceToLBClusterId) {

        this.serviceNameToLBClusterIdMap = serviceToLBClusterId;

    }
    
    public void addServiceLB(final String serviceName, final String lbClusterId) {
        this.serviceNameToLBClusterIdMap.put(serviceName, lbClusterId);
    }

    public String getLBClusterIdOfCluster(final String clusterId) {

        return this.clusterIdToLBClusterIdMap.get(clusterId);

    }

    public Map<String, String> getClusterIdToLBClusterIdMap() {

        return this.clusterIdToLBClusterIdMap;

    }

    public void setClusterIdToLBClusterIdMap(final Map<String, String> clusterIdToLBClusterIdMap) {

        this.clusterIdToLBClusterIdMap = clusterIdToLBClusterIdMap;

    }

    public boolean removeLbClusterId(String clusterId) {
    	if (isLBExist(clusterId)) {
    		if(isDefaultLBExist() && defaultLbClusterId.equals(clusterId)) {
    			defaultLbClusterId = null;
    			return true;
    			
    		} else if (serviceNameToLBClusterIdMap.containsValue(clusterId)){
    			for (String service : serviceNameToLBClusterIdMap.keySet()) {
					if(clusterId.equals(serviceNameToLBClusterIdMap.get(service))) {
						serviceNameToLBClusterIdMap.remove(service);
						return true;
					}
				}
    		} else if (clusterIdToLBClusterIdMap.containsValue(clusterId)){
    			for (String cluster : clusterIdToLBClusterIdMap.keySet()) {
					if(clusterId.equals(clusterIdToLBClusterIdMap.get(cluster))) {
						clusterIdToLBClusterIdMap.remove(cluster);
						return true;
					}
				}
    		}
    	}
    	return false;
    }

    public boolean isLBExist(final String clusterId) {

        return clusterId != null &&
               (clusterId.equals(this.defaultLbClusterId) ||
                this.serviceNameToLBClusterIdMap.containsValue(clusterId) || this.clusterIdToLBClusterIdMap.containsValue(clusterId));

    }

    public boolean isDefaultLBExist() {

        return defaultLbClusterId != null;

    }

    public boolean isServiceLBExist(String serviceName) {

        return this.serviceNameToLBClusterIdMap.containsKey(serviceName) &&
                this.serviceNameToLBClusterIdMap.get(serviceName) != null;

    }

    public boolean isClusterLBExist(String clusterId) {

        return this.clusterIdToLBClusterIdMap.containsKey(clusterId) &&
                this.clusterIdToLBClusterIdMap.get(clusterId) != null;

    }

    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.networkPartitionId == null) ? 0 : this.networkPartitionId.hashCode());
        return result;

    }

    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof NetworkPartitionLbHolder)) {
            return false;
        }
        final NetworkPartitionLbHolder other = (NetworkPartitionLbHolder) obj;
        if (this.networkPartitionId == null) {
            if (other.networkPartitionId != null) {
                return false;
            }
        }
        else if (!this.networkPartitionId.equals(other.networkPartitionId)) {
            return false;
        }
        return true;
    }

    @Override
	public String toString() {
		return "NetworkPartitionLbHolder [networkPartitionId="
				+ networkPartitionId + ", defaultLbClusterId="
				+ defaultLbClusterId + ", serviceNameToLBClusterIdMap="
				+ serviceNameToLBClusterIdMap + ", clusterIdToLBClusterIdMap="
				+ clusterIdToLBClusterIdMap + "]";
	}

	public String getNetworkPartitionId() {
        return networkPartitionId;
    }

}