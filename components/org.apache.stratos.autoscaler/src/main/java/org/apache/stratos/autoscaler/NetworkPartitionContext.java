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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.policy.model.RequestsInFlight;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds runtime data of a network partition.
 *
 */
public class NetworkPartitionContext implements Serializable{

	private static final long serialVersionUID = -8851073480764734511L;
	private static final Log log = LogFactory.getLog(NetworkPartitionContext.class);
    private final String id;

    private String defaultLbClusterId;

    private Map<String, String> serviceNameToLBClusterIdMap;

    private Map<String, String> clusterIdToLBClusterIdMap;

    private final String partitionAlgorithm;

    //boolean values to keep whether the requests in flight parameters are reset or not
    private boolean rifReset = false, averageRifReset = false, gradientRifReset = false, secondDerivativeRifRest = false;
    //boolean values to keep whether the memory consumption parameters are reset or not
    private boolean memoryConsumptionReset = false, averageMemoryConsumptionReset = false,
            gradientMemoryConsumptionReset = false, secondDerivativeMemoryConsumptionRest = false;
    //boolean values to keep whether the load average parameters are reset or not
    private boolean loadAverageReset = false, averageLoadAverageReset = false, gradientLoadAverageReset = false,
            secondDerivativeLoadAverageRest = false;

    //FIXME this should be populated via PartitionGroups a.k.a. NetworkPartitions
    private int minInstanceCount = 1, maxInstanceCount = 1;

    private final Partition[] partitions;

    //Following information will keep events details
    private RequestsInFlight requestsInFlight;
    private MemoryConsumption memoryConsumption;
    private LoadAverage loadAverage;

    //details required for partition selection algorithms
    private int currentPartitionIndex;
//    private Map<String, Integer> partitionToMemberCountMap;

    //partitions of this network partition
    private final Map<String, PartitionContext> partitionCtxts;

    public NetworkPartitionContext(String id, String partitionAlgo, Partition[] partitions) {

        super();
        this.id = id;
        this.partitionAlgorithm = partitionAlgo;
        this.partitions = partitions;
        this.setServiceToLBClusterId(new HashMap<String, String>());
        this.setClusterIdToLBClusterIdMap(new HashMap<String, String>());
//        partitionToMemberCountMap = new HashMap<String, Integer>();
        partitionCtxts = new HashMap<String, PartitionContext>();
        requestsInFlight = new RequestsInFlight();
        loadAverage = new LoadAverage();
        memoryConsumption = new MemoryConsumption();

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
    
    public void addServiceLB(final String serviceName, final String lbClusterId) {
        this.serviceNameToLBClusterIdMap.put(serviceName, lbClusterId);
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

    public int getMinInstanceCount() {
        return minInstanceCount;
    }

    public void setMinInstanceCount(int minInstanceCount) {
        this.minInstanceCount = minInstanceCount;
    }

    public int getMaxInstanceCount() {
        return maxInstanceCount;
    }

    public void setMaxInstanceCount(int maxInstanceCount) {
        this.maxInstanceCount = maxInstanceCount;
    }

    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = 31 * result + ((this.id == null) ? 0 : this.id.hashCode());
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
        if (this.id == null) {
            if (other.id != null) {
                return false;
            }
        }
        else if (!this.id.equals(other.id)) {
            return false;
        }
        return true;
    }



    @Override
    public String toString() {
        return "NetworkPartitionContext [id=" + id + ", defaultLbClusterId=" + defaultLbClusterId +
               ", partitionAlgorithm=" + partitionAlgorithm + ", minInstanceCount=" +
               minInstanceCount + ", maxInstanceCount=" + maxInstanceCount + "]";
    }

    public int getCurrentPartitionIndex() {
        return currentPartitionIndex;
    }

    public void setCurrentPartitionIndex(int currentPartitionIndex) {
        this.currentPartitionIndex = currentPartitionIndex;
    }

    public float getAverageRequestsInFlight() {
        return requestsInFlight.getAverage();
    }

    public void setAverageRequestsInFlight(float averageRequestsInFlight) {
        requestsInFlight.setAverage(averageRequestsInFlight);
        averageRifReset = true;
        if(secondDerivativeRifRest && gradientRifReset){
            rifReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public float getRequestsInFlightSecondDerivative() {
        return requestsInFlight.getSecondDerivative();
    }

    public void setRequestsInFlightSecondDerivative(float requestsInFlightSecondDerivative) {
        requestsInFlight.setSecondDerivative(requestsInFlightSecondDerivative);
        secondDerivativeRifRest = true;
        if(averageRifReset && gradientRifReset){
            rifReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public float getRequestsInFlightGradient() {
        return requestsInFlight.getGradient();
    }

    public void setRequestsInFlightGradient(float requestsInFlightGradient) {
        requestsInFlight.setGradient(requestsInFlightGradient);
        gradientRifReset = true;
        if(secondDerivativeRifRest && averageRifReset){
            rifReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public boolean isRifReset() {
        return rifReset;
    }

    public void setRifReset(boolean rifReset) {
        this.rifReset = rifReset;
        this.averageRifReset = rifReset;
        this.gradientRifReset = rifReset;
        this.secondDerivativeRifRest = rifReset;
    }


    public float getAverageMemoryConsumption() {
        return memoryConsumption.getAverage();
    }

    public void setAverageMemoryConsumption(float averageMemoryConsumption) {
        memoryConsumption.setAverage(averageMemoryConsumption);
        averageMemoryConsumptionReset = true;
        if(secondDerivativeMemoryConsumptionRest && gradientMemoryConsumptionReset){
            memoryConsumptionReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public float getMemoryConsumptionSecondDerivative() {
        return memoryConsumption.getSecondDerivative();
    }

    public void setMemoryConsumptionSecondDerivative(float memoryConsumptionSecondDerivative) {
        memoryConsumption.setSecondDerivative(memoryConsumptionSecondDerivative);
        secondDerivativeMemoryConsumptionRest = true;
        if(averageMemoryConsumptionReset && gradientMemoryConsumptionReset){
            memoryConsumptionReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public float getMemoryConsumptionGradient() {
        return memoryConsumption.getGradient();
    }

    public void setMemoryConsumptionGradient(float memoryConsumptionGradient) {
        memoryConsumption.setGradient(memoryConsumptionGradient);
        gradientMemoryConsumptionReset = true;
        if(secondDerivativeMemoryConsumptionRest && averageMemoryConsumptionReset){
            memoryConsumptionReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public boolean isMemoryConsumptionReset() {
        return memoryConsumptionReset;
    }

    public void setMemoryConsumptionReset(boolean memoryConsumptionReset) {
        this.memoryConsumptionReset = memoryConsumptionReset;
        this.averageMemoryConsumptionReset = memoryConsumptionReset;
        this.gradientMemoryConsumptionReset = memoryConsumptionReset;
        this.secondDerivativeMemoryConsumptionRest = memoryConsumptionReset;
    }


    public float getAverageLoadAverage() {
        return loadAverage.getAverage();
    }

    public void setAverageLoadAverage(float averageLoadAverage) {
        loadAverage.setAverage(averageLoadAverage);
        averageLoadAverageReset = true;
        if(secondDerivativeLoadAverageRest && gradientLoadAverageReset){
            loadAverageReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public float getLoadAverageSecondDerivative() {
        return loadAverage.getSecondDerivative();
    }

    public void setLoadAverageSecondDerivative(float loadAverageSecondDerivative) {
        loadAverage.setSecondDerivative(loadAverageSecondDerivative);
        secondDerivativeLoadAverageRest = true;
        if(averageLoadAverageReset && gradientLoadAverageReset){
            loadAverageReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public float getLoadAverageGradient() {
        return loadAverage.getGradient();
    }

    public void setLoadAverageGradient(float loadAverageGradient) {
        loadAverage.setGradient(loadAverageGradient);
        gradientLoadAverageReset = true;
        if(secondDerivativeLoadAverageRest && averageLoadAverageReset){
            loadAverageReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] %s"
                        , this.id));
            }
        }
    }

    public boolean isLoadAverageReset() {
        return loadAverageReset;
    }

    public void setLoadAverageReset(boolean loadAverageReset) {
        this.loadAverageReset = loadAverageReset;
        this.averageLoadAverageReset = loadAverageReset;
        this.gradientLoadAverageReset = loadAverageReset;
        this.secondDerivativeLoadAverageRest = loadAverageReset;
    }



    public String getId() {
        return id;
    }

//    public void increaseMemberCountOfPartition(String partitionId, int count){
//         if(!partitionCountExists(partitionId)){
//             addPartitionCount(partitionId, 1);
//         } else{
//            partitionToMemberCountMap.put(partitionId, getMemberCountOfPartition(partitionId) + count);
//         }
//     }

//     public void decreaseMemberCountOfPartition(String partitionId, int count){
//
//         partitionToMemberCountMap.put(partitionId, getMemberCountOfPartition(partitionId) - count);
//     }
//
//     public void addPartitionCount(String partitionId, int count){
//         partitionToMemberCountMap.put(partitionId, count);
//     }
//
//     public void removePartitionCount(String partitionId){
//
//         partitionToMemberCountMap.remove(partitionId);
//     }

//     public boolean partitionCountExists(String partitionId){
//         return partitionToMemberCountMap.containsKey(partitionId);
//     }

     public int getMemberCountOfPartition(String partitionId){
//         if(partitionToMemberCountMap.containsKey(partitionId)) {
//             return partitionToMemberCountMap.get(partitionId);
//         }
//         return 0;
         if(partitionCtxts.containsKey(partitionId)){
             return getPartitionCtxt(partitionId).getTotalMemberCount();
         }
         return 0;
     }

    public Map<String, PartitionContext> getPartitionCtxts() {
        return partitionCtxts;
    }

    public PartitionContext getPartitionCtxt(String partitionId) {
        return partitionCtxts.get(partitionId);
    }

    public void addPartitionContext(PartitionContext partitionContext) {
        partitionCtxts.put(partitionContext.getPartitionId(), partitionContext);
    }

    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
    }

    public Partition[] getPartitions() {
        return partitions;
    }

//    public void setPartitions(Partition[] partitions) {
//        this.partitions = partitions;
//        for (Partition partition: partitions){
//            partitionToMemberCountMap.put(partition.getId(), 0);
//        }
//    }

//    public void setPartitionToMemberCountMap(Map<String, Integer> partitionToMemberCountMap) {
//        this.partitionToMemberCountMap = partitionToMemberCountMap;
//    }

}