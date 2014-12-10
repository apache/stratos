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
package org.apache.stratos.autoscaler.context.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.LoadAverage;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.MemoryConsumption;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.RequestsInFlight;
import org.apache.stratos.messaging.domain.topology.Member;

import java.util.ArrayList;
import java.util.List;

/*
 * It holds the runtime data of a VM cluster
 */
public class ClusterInstanceContext extends InstanceContext {

    private static final Log log = LogFactory.getLog(ClusterInstanceContext.class);

    private final String partitionAlgorithm;
    // Map<PartitionId, Partition Context>
    protected List<ClusterLevelPartitionContext> partitionCtxts;
    //boolean values to keep whether the requests in flight parameters are reset or not
    private boolean rifReset = false, averageRifReset = false, gradientRifReset = false, secondDerivativeRifRest = false;
    //boolean values to keep whether the memory consumption parameters are reset or not
    private boolean memoryConsumptionReset = false, averageMemoryConsumptionReset = false,
            gradientMemoryConsumptionReset = false, secondDerivativeMemoryConsumptionRest = false;
    //boolean values to keep whether the load average parameters are reset or not
    private boolean loadAverageReset = false, averageLoadAverageReset = false, gradientLoadAverageReset = false,
            secondDerivativeLoadAverageRest = false;
    //boolean values to keep whether average requests served per instance parameters are reset or not
    private boolean averageRequestServedPerInstanceReset = false;
    //Following information will keep events details
    private RequestsInFlight requestsInFlight;
    private MemoryConsumption memoryConsumption;
    private LoadAverage loadAverage;
    private int scaleDownRequestsCount = 0;
    private float averageRequestsServedPerInstance;
    private float requestsServedPerInstance;
    private int minInstanceCount = 0, maxInstanceCount = 0;
    private int requiredInstanceCountBasedOnStats;
    private int requiredInstanceCountBasedOnDependencies;
    //details required for partition selection algorithms
    private int currentPartitionIndex;

    private String networkPartitionId;

    public ClusterInstanceContext(String clusterInstanceId, String partitionAlgo,
                                  int min, int max, String networkPartitionId) {

        super(clusterInstanceId);
        this.networkPartitionId = networkPartitionId;
        this.minInstanceCount = min;
        this.maxInstanceCount = max;
        partitionCtxts = new ArrayList<ClusterLevelPartitionContext>();
        this.partitionAlgorithm = partitionAlgo;
        //partitionCtxts = new HashMap<String, ClusterLevelPartitionContext>();
        requestsInFlight = new RequestsInFlight();
        loadAverage = new LoadAverage();
        memoryConsumption = new MemoryConsumption();
        requiredInstanceCountBasedOnStats = minInstanceCount;
        requiredInstanceCountBasedOnDependencies = minInstanceCount;


    }

    public List<ClusterLevelPartitionContext> getPartitionCtxts() {
        return partitionCtxts;
    }

    public void setPartitionCtxts(List<ClusterLevelPartitionContext> partitionCtxt) {
        this.partitionCtxts = partitionCtxt;
    }

//    public ClusterLevelPartitionContext getNetworkPartitionCtxt(String PartitionId) {
//        return partitionCtxts.get(PartitionId);
//    }

    public ClusterLevelPartitionContext[] getPartitionCtxtsAsAnArray() {

        return partitionCtxts.toArray(new ClusterLevelPartitionContext[0]);
    }

    public boolean partitionCtxtAvailable(String partitionId) {

        for (ClusterLevelPartitionContext partitionContext : partitionCtxts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                return true;
            }
        }
        return false;
    }

    public void addPartitionCtxt(ClusterLevelPartitionContext ctxt) {
        this.partitionCtxts.add(ctxt);
    }

    public ClusterLevelPartitionContext getPartitionCtxt(String id) {

        for (ClusterLevelPartitionContext partitionContext : partitionCtxts) {
            if (partitionContext.getPartitionId().equals(id)) {
                return partitionContext;
            }
        }
        return null;
    }

    public ClusterLevelPartitionContext getPartitionCtxt(Member member) {
        log.info("Getting [Partition] " + member.getPartitionId());
        String partitionId = member.getPartitionId();

        for (ClusterLevelPartitionContext partitionContext : partitionCtxts) {
            if (partitionContext.getPartitionId().equals(partitionId)) {
                log.info("Returning partition context, of [partition] " + partitionId);
                return partitionContext;
            }
        }
        return null;
    }

    public int getNonTerminatedMemberCount() {

        int nonTerminatedMemberCount = 0;
        for (ClusterLevelPartitionContext partitionContext : partitionCtxts) {

            nonTerminatedMemberCount += partitionContext.getNonTerminatedMemberCount();
        }
        return nonTerminatedMemberCount;
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

    @Override
    public String toString() {
        return "NetworkPartitionContext [id=" + id + "partitionAlgorithm=" + partitionAlgorithm + ", minInstanceCount=" +
                minInstanceCount + ", maxInstanceCount=" + maxInstanceCount + "]";
    }

    public int getCurrentPartitionIndex() {
        return currentPartitionIndex;
    }

    public void setCurrentPartitionIndex(int currentPartitionIndex) {
        this.currentPartitionIndex = currentPartitionIndex;
    }

    public float getAverageRequestsServedPerInstance() {
        return averageRequestsServedPerInstance;
    }

    public void setAverageRequestsServedPerInstance(float averageRequestServedPerInstance) {
        this.averageRequestsServedPerInstance = averageRequestServedPerInstance;
        averageRequestServedPerInstanceReset = true;

        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Requesets Served Per Instance stats are reset, ready to do scale check " +
                    "[network partition] %s", this.id));

        }
    }

    public float getRequestsServedPerInstance() {
        return requestsServedPerInstance;
    }

    public float getAverageRequestsInFlight() {
        return requestsInFlight.getAverage();
    }

    public void setAverageRequestsInFlight(float averageRequestsInFlight) {
        requestsInFlight.setAverage(averageRequestsInFlight);
        averageRifReset = true;
        if (secondDerivativeRifRest && gradientRifReset) {
            rifReset = true;
            if (log.isDebugEnabled()) {
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
        if (averageRifReset && gradientRifReset) {
            rifReset = true;
            if (log.isDebugEnabled()) {
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
        if (secondDerivativeRifRest && averageRifReset) {
            rifReset = true;
            if (log.isDebugEnabled()) {
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
        if (secondDerivativeMemoryConsumptionRest && gradientMemoryConsumptionReset) {
            memoryConsumptionReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Memory consumption stats are reset, ready to do scale check [network partition] %s"
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
        if (averageMemoryConsumptionReset && gradientMemoryConsumptionReset) {
            memoryConsumptionReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Memory consumption stats are reset, ready to do scale check [network partition] %s"
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
        if (secondDerivativeMemoryConsumptionRest && averageMemoryConsumptionReset) {
            memoryConsumptionReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Memory consumption stats are reset, ready to do scale check [network partition] %s"
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
        if (secondDerivativeLoadAverageRest && gradientLoadAverageReset) {
            loadAverageReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Load average stats are reset, ready to do scale check [network partition] %s"
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
        if (averageLoadAverageReset && gradientLoadAverageReset) {
            loadAverageReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Load average stats are reset, ready to do scale check [network partition] %s"
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
        if (secondDerivativeLoadAverageRest && averageLoadAverageReset) {
            loadAverageReset = true;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Load average stats are reset, ready to do scale check [network partition] %s"
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

   /* public Map<String, ClusterLevelPartitionContext> getPartitionCtxts() {
        return partitionCtxts;
    }

    public ClusterLevelPartitionContext getPartitionCtxt(String partitionId) {
        return partitionCtxts.get(partitionId);
    }

    public void addPartitionContext(ClusterLevelPartitionContext partitionContext) {
        partitionCtxts.put(partitionContext.getPartitionId(), partitionContext);
    }*/

    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
    }

    /*public int getNonTerminatedMemberCountOfPartition(String partitionId) {
        if (partitionCtxts.containsKey(partitionId)) {
            return getPartitionCtxt(partitionId).getNonTerminatedMemberCount();
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {
        if (partitionCtxts.containsKey(currentPartitionId)) {
            return getPartitionCtxt(currentPartitionId).getActiveMemberCount();
        }
        return 0;
    }
*/
    public int getScaleDownRequestsCount() {
        return scaleDownRequestsCount;
    }

    public void resetScaleDownRequestsCount() {
        this.scaleDownRequestsCount = 0;
    }

    public void increaseScaleDownRequestsCount() {
        this.scaleDownRequestsCount += 1;
    }

    public float getRequiredInstanceCountBasedOnStats() {
        return requiredInstanceCountBasedOnStats;
    }

    public void setRequiredInstanceCountBasedOnStats(int requiredInstanceCountBasedOnStats) {
        this.requiredInstanceCountBasedOnStats = requiredInstanceCountBasedOnStats;
    }

    public int getRequiredInstanceCountBasedOnDependencies() {
        return requiredInstanceCountBasedOnDependencies;
    }

    public void setRequiredInstanceCountBasedOnDependencies(int requiredInstanceCountBasedOnDependencies) {
        this.requiredInstanceCountBasedOnDependencies = requiredInstanceCountBasedOnDependencies;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public int getActiveMembers() {
        int activeMembers = 0;
        for (ClusterLevelPartitionContext partitionContext : this.partitionCtxts) {
            activeMembers += partitionContext.getActiveInstanceCount();
        }
        return activeMembers;
    }

    public boolean isAverageRequestServedPerInstanceReset() {
        return averageRequestServedPerInstanceReset;
    }
}
