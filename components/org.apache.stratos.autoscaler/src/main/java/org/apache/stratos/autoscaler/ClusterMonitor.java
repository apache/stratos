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
package org.apache.stratos.autoscaler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 * @author nirmal
 *
 */
public class ClusterMonitor implements Runnable{

    private static final Log log = LogFactory.getLog(ClusterMonitor.class);
    private String clusterId;
    private ClusterContext clusterCtxt;
    private List<MemberStatsContext> memberCtxt;
    private Map<String, PartitionContext> partitionCtxts;
    private StatefulKnowledgeSession ksession;
    private boolean isDestroyed;
    
    private FactHandle facthandle;
    
    public ClusterMonitor(String clusterId, ClusterContext ctxt, StatefulKnowledgeSession ksession) {
        this.clusterId = clusterId;
        this.clusterCtxt = ctxt;
        this.ksession = ksession;
        partitionCtxts = new ConcurrentHashMap<String, PartitionContext>();
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public ClusterContext getClusterCtxt() {
        return clusterCtxt;
    }

    public void setClusterCtxt(ClusterContext clusterCtxt) {
        this.clusterCtxt = clusterCtxt;
    }

    public List<MemberStatsContext> getMemberCtxt() {
        return memberCtxt;
    }

    public void setMemberCtxt(List<MemberStatsContext> memberCtxt) {
        this.memberCtxt = memberCtxt;
    }

    public Map<String, PartitionContext> getPartitionCtxt() {
        return partitionCtxts;
    }

    public void setPartitionCtxt(Map<String, PartitionContext> partitionCtxt) {
        this.partitionCtxts = partitionCtxt;
    }
    
    public void addPartitionCtxt(PartitionContext ctxt) {
        this.partitionCtxts.put(ctxt.getPartitionId(), ctxt);
    }
    
    public PartitionContext getPartitionCtxt(String id) {
        return this.partitionCtxts.get(id);
    }

    public StatefulKnowledgeSession getKsession() {
        return ksession;
    }

    public void setKsession(StatefulKnowledgeSession ksession) {
        this.ksession = ksession;
    }

    public FactHandle getFacthandle() {
        return facthandle;
    }

    public void setFacthandle(FactHandle facthandle) {
        this.facthandle = facthandle;
    }

    @Override
    public void run() {

        while (!isDestroyed()) {
            log.debug("Cluster monitor is running..");
            try {
                minInstanceCountCheck();
            } catch (Exception e) {
                log.error("Cluster monitor: min instance count check failed.", e);
            }
            // TODO scale
            try {
                // TODO make this configurable
                Thread.sleep(30000);
            } catch (InterruptedException ignore) {
            }
        }
    }
    
    private void minInstanceCountCheck() {
        if(clusterCtxt != null ) {
            ksession.setGlobal("clusterId", clusterId);
            //TODO make this concurrent
            for (Partition partition : clusterCtxt.getAllPartitions()) {
                String id = partition.getId();
                PartitionContext ctxt = partitionCtxts.get(id);
                if(ctxt == null) {
                    ctxt = new PartitionContext(partition);
                    partitionCtxts.put(id, ctxt);
                }
                ctxt.setMinimumMemberCount(partition.getPartitionMin());
                
                facthandle = AutoscalerRuleEvaluator.evaluate(ksession, facthandle, ctxt);
            }
        }
    }

    public void destroy() {
        ksession.dispose();
        setDestroyed(true);
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean isDestroyed) {
        this.isDestroyed = isDestroyed;
    }
}
