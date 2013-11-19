/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Defines cluster context properties.
 */
public class ClusterContext {

    private String serviceId;

    private String clusterId;

    private float averageRequestsInFlight;

    private float requestsInFlightSecondDerivative;

    private float requestsInFlightGradient;
    
    private int memberCount;

    //This map will keep number of currently spawned instance count against partitionId
    private Map<String, Integer> partitionCountMap;

    private int currentPartitionIndex;
    private int currentPartitionGroupIndex;

    private Properties properties;

    private Map<String, MemberContext> memberContextMap;

    public ClusterContext(String clusterId, String serviceId) {

        this.clusterId = clusterId;
        this.serviceId = serviceId;
        memberContextMap = new HashMap<String, MemberContext>();
        partitionCountMap = new HashMap<String, Integer>();
        memberCount = 0;
    }

    public String getClusterId() {

        return clusterId;
    }

    public Properties getProperties() {

        return properties;
    }

    public void setProperties(Properties properties) {

        this.properties = properties;
    }

    public float getAverageRequestsInFlight() {
        return averageRequestsInFlight;
    }

    public void setAverageRequestsInFlight(float averageRequestsInFlight) {

        this.averageRequestsInFlight = averageRequestsInFlight;
    }

    public float getRequestsInFlightSecondDerivative() {

        return requestsInFlightSecondDerivative;
    }

    public void setRequestsInFlightSecondDerivative(float requestsInFlightSecondDerivative) {

        this.requestsInFlightSecondDerivative = requestsInFlightSecondDerivative;
    }

    public float getRequestsInFlightGradient() {

        return requestsInFlightGradient;
    }

    public void setRequestsInFlightGradient(float requestsInFlightGradient) {

        this.requestsInFlightGradient = requestsInFlightGradient;
    }

    /**
     *
     * @param memberContext will be added to map
     */
    public void addMemberContext(MemberContext memberContext) {

        memberContextMap.put(memberContext.getMemberId(), memberContext);
    }

    /**
     * {@link MemberContext} which carries memberId will be removed from map
     * @param memberId
     */
    public void removeMemberContext(String memberId){

        memberContextMap.remove(memberId);
    }

    public void increaseMemberCount(int count){
        memberCount += count;

    }
    public void decreaseMemberCount(){
        memberCount --;

    }

   public void increaseMemberCountInPartitionBy(String partitionId, int count){

        partitionCountMap.put(partitionId, partitionCountMap.get(partitionId) + count);
    }

    public void decreaseMemberCountInPartitionBy(String partitionId, int count){

        partitionCountMap.put(partitionId, partitionCountMap.get(partitionId) - count);
    }

    public void addPartitionCount(String partitionId, int count){    	
        partitionCountMap.put(partitionId, count);
    }

    public void removePartitionCount(String partitionId){

        partitionCountMap.remove(partitionId);
    }

    public boolean partitionCountExists(String partitionId){
        return partitionCountMap.containsKey(partitionId);
    }

    public int getMemberCount(String partitionId){
        return partitionCountMap.get(partitionId);
    }

    public void setMemberContextMap(Map<String, MemberContext> memberContextMap) {

        this.memberContextMap = memberContextMap;
    }

    public String getServiceId() {
        return serviceId;
    }

    public int getCurrentPartitionIndex() {
        return currentPartitionIndex;
    }

    public void setCurrentPartitionIndex(int currentPartitionIndex) {
        this.currentPartitionIndex = currentPartitionIndex;
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }
    
    public int getCurrentPartitionGroupIndex() {
        return currentPartitionGroupIndex;
    }

    public void setCurrentPartitionGroupIndex(int currentPartitionGroupIndex) {
        this.currentPartitionGroupIndex = currentPartitionGroupIndex;
    }
}
