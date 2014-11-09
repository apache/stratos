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
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds runtime data of a network partition.
 *
 */
public class NetworkPartitionContext implements Serializable{

	private static final Log log = LogFactory.getLog(NetworkPartitionContext.class);
    private static final long serialVersionUID = 572769304374110159L;
    private final String id;
    private int scaleDownRequestsCount = 0;
    private float averageRequestsServedPerInstance;
    private float requestsServedPerInstance;

    private int minInstanceCount = 0, maxInstanceCount = 0;
    private float  requiredInstanceCount;


    private final String partitionAlgorithm;

    //boolean values to keep whether the requests in flight parameters are reset or not
    private boolean rifReset = false, averageRifReset = false, gradientRifReset = false, secondDerivativeRifRest = false;
    //boolean values to keep whether the memory consumption parameters are reset or not
    private boolean memoryConsumptionReset = false, averageMemoryConsumptionReset = false,
            gradientMemoryConsumptionReset = false, secondDerivativeMemoryConsumptionRest = false;
    //boolean values to keep whether the load average parameters are reset or not
    private boolean loadAverageReset = false, averageLoadAverageReset = false, gradientLoadAverageReset = false,
            secondDerivativeLoadAverageRest = false;
    //boolean values to keep whether average requests served per instance parameters are reset or not
    private boolean averageRequestServedPerInstanceReset= false;

    private final Partition[] partitions;

    //Following information will keep events details
    private RequestsInFlight requestsInFlight;
    private MemoryConsumption memoryConsumption;
    private LoadAverage loadAverage;

    //details required for partition selection algorithms
    private int currentPartitionIndex;

    //partitions of this network partition
    private final Map<String, PartitionContext> partitionCtxts;

    public NetworkPartitionContext(String id, String partitionAlgo, Partition[] partitions) {

        super();
        this.id = id;
        this.partitionAlgorithm = partitionAlgo;
        if(partitions == null) {
            this.partitions = new Partition[0];
        } else {
            this.partitions = Arrays.copyOf(partitions, partitions.length);
        }
        partitionCtxts = new HashMap<String, PartitionContext>();
        requestsInFlight = new RequestsInFlight();
        loadAverage = new LoadAverage();
        memoryConsumption = new MemoryConsumption();
        for(Partition partition : partitions){
            minInstanceCount += partition.getPartitionMin();
            maxInstanceCount += partition.getPartitionMax();
        }
        requiredInstanceCount = minInstanceCount;

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
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
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
        return "NetworkPartitionContext [id=" + id + "partitionAlgorithm=" + partitionAlgorithm + ", minInstanceCount=" +
               minInstanceCount + ", maxInstanceCount=" + maxInstanceCount + "]";
    }

    public int getCurrentPartitionIndex() {
        return currentPartitionIndex;
    }

    public void setCurrentPartitionIndex(int currentPartitionIndex) {
        this.currentPartitionIndex = currentPartitionIndex;
    }

    public float getAverageRequestsServedPerInstance() { return averageRequestsServedPerInstance;}

    public void setAverageRequestsServedPerInstance(float averageRequestServedPerInstance) {
        this.averageRequestsServedPerInstance = averageRequestServedPerInstance;
        averageRequestServedPerInstanceReset = true;

        if(log.isDebugEnabled()){
            log.debug(String.format("Average Requesets Served Per Instance stats are reset, ready to do scale check [network partition] %s"
                    , this.id));

        }
    }

    public float getRequestsServedPerInstance() { return requestsServedPerInstance;}

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
        if(averageMemoryConsumptionReset && gradientMemoryConsumptionReset){
            memoryConsumptionReset = true;
            if(log.isDebugEnabled()){
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
        if(secondDerivativeMemoryConsumptionRest && averageMemoryConsumptionReset){
            memoryConsumptionReset = true;
            if(log.isDebugEnabled()){
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
        if(secondDerivativeLoadAverageRest && gradientLoadAverageReset){
            loadAverageReset = true;
            if(log.isDebugEnabled()){
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
        if(averageLoadAverageReset && gradientLoadAverageReset){
            loadAverageReset = true;
            if(log.isDebugEnabled()){
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
        if(secondDerivativeLoadAverageRest && averageLoadAverageReset){
            loadAverageReset = true;
            if(log.isDebugEnabled()){
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



    public String getId() {
        return id;
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

    public int getNonTerminatedMemberCountOfPartition(String partitionId) {
        if(partitionCtxts.containsKey(partitionId)){
            return getPartitionCtxt(partitionId).getNonTerminatedMemberCount();
        }
        return 0;
    }

    public int getActiveMemberCount(String currentPartitionId) {
        if(partitionCtxts.containsKey(currentPartitionId)){
            return getPartitionCtxt(currentPartitionId).getActiveMemberCount();
        }
        return 0;
    }

    public int getScaleDownRequestsCount() {
        return scaleDownRequestsCount;
    }

    public void resetScaleDownRequestsCount() {
        this.scaleDownRequestsCount = 0;
    }
    public void increaseScaleDownRequestsCount() {
        this.scaleDownRequestsCount += 1;
    }

    public float getRequiredInstanceCount() {
        return requiredInstanceCount;
    }

    public void setRequiredInstanceCount(int requiredInstanceCount) {
        this.requiredInstanceCount = requiredInstanceCount;
    }
}