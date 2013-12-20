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
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds runtime data of a network partition.
 *
 */
public class NetworkPartitionContext implements Serializable {

    private static final long serialVersionUID = -5458149704820463584L;
    private static final Log log = LogFactory.getLog(NetworkPartitionContext.class);
    private String id;

    private String defaultLbClusterId;

    private Map<String, String> serviceNameToLBClusterIdMap;

    private Map<String, String> clusterIdToLBClusterIdMap;

    private String partitionAlgorithm;

    //boolean values to keep whether the requests in flight parameters are reset or not
    private boolean rifReset = false, averageRifReset = false, gradientRifReset = false, secondDerivativeRifRest = false;
    
    //FIXME this should be populated via PartitionGroups a.k.a. NetworkPartitions
    private int minInstanceCount = 1, maxInstanceCount = 1;

    private Partition[] partitions;

    //Following information will keep events details
    private float averageRequestsInFlight;
    private float requestsInFlightSecondDerivative;
    private float requestsInFlightGradient;

    //details required for partition selection algorithms
    private int currentPartitionIndex;
    private Map<String, Integer> partitionToMemberCountMap;

    //partitions of this network partition
    private Map<String, PartitionContext> partitionCtxts;

    public NetworkPartitionContext(String id) {

        super();
        this.id = id;
        this.setServiceToLBClusterId(new HashMap<String, String>());
        this.setClusterIdToLBClusterIdMap(new HashMap<String, String>());
        partitionToMemberCountMap = new HashMap<String, Integer>();
        partitionCtxts = new HashMap<String, PartitionContext>();

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
        return averageRequestsInFlight;
    }

    public void setAverageRequestsInFlight(float averageRequestsInFlight) {
        this.averageRequestsInFlight = averageRequestsInFlight;
        averageRifReset = true;
        if(secondDerivativeRifRest && gradientRifReset){
            rifReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] "
                        , this.id));
            }
        }
    }

    public float getRequestsInFlightSecondDerivative() {
        return requestsInFlightSecondDerivative;
    }

    public void setRequestsInFlightSecondDerivative(float requestsInFlightSecondDerivative) {
        this.requestsInFlightSecondDerivative = requestsInFlightSecondDerivative;
        secondDerivativeRifRest = true;
        if(averageRifReset && gradientRifReset){
            rifReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] "
                        , this.id));
            }
        }
    }

    public float getRequestsInFlightGradient() {
        return requestsInFlightGradient;
    }

    public void setRequestsInFlightGradient(float requestsInFlightGradient) {
        this.requestsInFlightGradient = requestsInFlightGradient;
        gradientRifReset = true;
        if(secondDerivativeRifRest && averageRifReset){
            rifReset = true;
            if(log.isDebugEnabled()){
                log.debug(String.format("Requests in flights stats are reset, ready to do scale check [network partition] "
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void increaseMemberCountInPartitionBy(String partitionId, int count){
         if(!partitionCountExists(partitionId)){
             addPartitionCount(partitionId, 1);
         } else{
            partitionToMemberCountMap.put(partitionId, getMemberCount(partitionId) + count);
         }
     }

     public void decreaseMemberCountInPartitionBy(String partitionId, int count){

         partitionToMemberCountMap.put(partitionId, getMemberCount(partitionId) - count);
     }

     public void addPartitionCount(String partitionId, int count){
         partitionToMemberCountMap.put(partitionId, count);
     }

     public void removePartitionCount(String partitionId){

         partitionToMemberCountMap.remove(partitionId);
     }

     public boolean partitionCountExists(String partitionId){
         return partitionToMemberCountMap.containsKey(partitionId);
     }

     public int getMemberCount(String partitionId){
         if(partitionToMemberCountMap.containsKey(partitionId)) {
             return partitionToMemberCountMap.get(partitionId);
         }
         return 0;
     }

    public Map<String, PartitionContext> getPartitionCtxts() {
        return partitionCtxts;
    }

    public PartitionContext getPartitionCtxt(String partitionId) {
        return partitionCtxts.get(partitionId);
    }

    public void setPartitionCtxts(Map<String, PartitionContext> partitionCtxts) {
        this.partitionCtxts = partitionCtxts;
    }

    public void addPartitionContext(PartitionContext partitionContext) {
        partitionCtxts.put(partitionContext.getPartitionId(), partitionContext);
    }

    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
    }

    public void setPartitionAlgorithm(String partitionAlgorithm) {
        this.partitionAlgorithm = partitionAlgorithm;
    }

    public Partition[] getPartitions() {
        return partitions;
    }

    public void setPartitions(Partition[] partitions) {
        this.partitions = partitions;
        for (Partition partition: partitions){
            partitionToMemberCountMap.put(partition.getId(), 0);
        }
    }

    public void setPartitionToMemberCountMap(Map<String, Integer> partitionToMemberCountMap) {
        this.partitionToMemberCountMap = partitionToMemberCountMap;
    }

}