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
package org.apache.stratos.autoscaler.monitor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 *
 */
public class ClusterMonitor extends AbstractMonitor{

    private static final Log log = LogFactory.getLog(ClusterMonitor.class);    
    private String lbReferenceType;

    public ClusterMonitor(String clusterId, String serviceId, DeploymentPolicy deploymentPolicy,
                          AutoscalePolicy autoscalePolicy) {
        this.clusterId = clusterId;
        this.serviceId = serviceId;

        this.autoscalerRuleEvaluator = new AutoscalerRuleEvaluator();
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getScaleCheckStatefulSession();
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getMinCheckStatefulSession();

        this.deploymentPolicy = deploymentPolicy;
        this.autoscalePolicy = autoscalePolicy;
        networkPartitionCtxts = new ConcurrentHashMap<String, NetworkPartitionContext>();
    }



    @Override
    public void run() {

        while (!isDestroyed()) {
            if (log.isDebugEnabled()) {
                log.debug("Cluster monitor is running.. "+this.toString());
            }
            try {
                monitor();
            } catch (Exception e) {
                log.error("Cluster monitor: Monitor failed."+this.toString(), e);
            }
            try {
                // TODO make this configurable
                Thread.sleep(30000);
            } catch (InterruptedException ignore) {
            }
        }
    }
    
    private void monitor() {
//        if(clusterCtxt != null ) {
            //TODO make this concurrent
        for (NetworkPartitionContext networkPartitionContext : networkPartitionCtxts.values()) {

            //minimum check per partition
            for(PartitionContext partitionContext: networkPartitionContext.getPartitionCtxts().values()){

                minCheckKnowledgeSession.setGlobal("clusterId", clusterId);
                minCheckKnowledgeSession.setGlobal("lbRef", lbReferenceType);
                
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Running minimum check for partition %s ", partitionContext.getPartitionId()));
                }

                minCheckFactHandle = AutoscalerRuleEvaluator.evaluateMinCheck(minCheckKnowledgeSession
                        , minCheckFactHandle, partitionContext);

            }

            boolean rifReset = networkPartitionContext.isRifReset();
            boolean memoryConsumptionReset = networkPartitionContext.isMemoryConsumptionReset();
            boolean loadAverageReset = networkPartitionContext.isLoadAverageReset();
            if(rifReset || memoryConsumptionReset || loadAverageReset){

                scaleCheckKnowledgeSession.setGlobal("clusterId", clusterId);
                //scaleCheckKnowledgeSession.setGlobal("deploymentPolicy", deploymentPolicy);
                scaleCheckKnowledgeSession.setGlobal("autoscalePolicy", autoscalePolicy);
                scaleCheckKnowledgeSession.setGlobal("rifReset", rifReset);
                scaleCheckKnowledgeSession.setGlobal("mcReset", memoryConsumptionReset);
                scaleCheckKnowledgeSession.setGlobal("laReset", loadAverageReset);
                scaleCheckKnowledgeSession.setGlobal("lbRef", lbReferenceType);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Running scale check for network partition %s ", networkPartitionContext.getId()));
                }

                scaleCheckFactHandle = AutoscalerRuleEvaluator.evaluateScaleCheck(scaleCheckKnowledgeSession
                        , scaleCheckFactHandle, networkPartitionContext);

                networkPartitionContext.setRifReset(false);
                networkPartitionContext.setMemoryConsumptionReset(false);
                networkPartitionContext.setLoadAverageReset(false);
            } else if(log.isDebugEnabled()){
                    log.debug(String.format("Scale rule will not run since the LB statistics have not received before this " +
                            "cycle for network partition %s", networkPartitionContext.getId()) );
            }
        }
    }

    @Override
    public String toString() {
        return "ClusterMonitor [clusterId=" + clusterId + ", serviceId=" + serviceId +
               ", deploymentPolicy=" + deploymentPolicy + ", autoscalePolicy=" + autoscalePolicy +
               ", lbReferenceType=" + lbReferenceType + "]";
    }

    public String getLbReferenceType() {
        return lbReferenceType;
    }

    public void setLbReferenceType(String lbReferenceType) {
        this.lbReferenceType = lbReferenceType;
    }
}
