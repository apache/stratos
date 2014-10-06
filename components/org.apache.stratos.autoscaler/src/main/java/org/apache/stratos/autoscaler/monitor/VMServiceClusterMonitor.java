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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class VMServiceClusterMonitor extends VMClusterMonitor {

    private static final Log log = LogFactory.getLog(VMServiceClusterMonitor.class);
    private String lbReferenceType;
    private boolean hasPrimary;

    public VMServiceClusterMonitor(String clusterId, String serviceId,
                                   DeploymentPolicy deploymentPolicy,
                                   AutoscalePolicy autoscalePolicy) {
        super(clusterId, serviceId, new AutoscalerRuleEvaluator(),
              deploymentPolicy, autoscalePolicy,
              new ConcurrentHashMap<String, NetworkPartitionContext>());
        readConfigurations();
    }

    @Override
    public void run() {

        try {
            // TODO make this configurable,
            // this is the delay the min check of normal cluster monitor to wait until LB monitor is added
            Thread.sleep(60000);
        } catch (InterruptedException ignore) {
        }

            if (log.isDebugEnabled()) {
                log.debug("VMServiceClusterMonitor is running.. " + this.toString());
            }
            try {
                if (!ClusterStatus.In_Maintenance.equals(getStatus())) {
                    monitor();
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("VMServiceClusterMonitor is suspended as the cluster is in " +
                                  ClusterStatus.In_Maintenance + " mode......");
                    }
                }
            } catch (Exception e) {
                log.error("VMServiceClusterMonitor : Monitor failed." + this.toString(), e);
            }
    }

    @Override
    protected void monitor() {

        //TODO make this concurrent
        for (NetworkPartitionContext networkPartitionContext : networkPartitionCtxts.values()) {
            // store primary members in the network partition context
            List<String> primaryMemberListInNetworkPartition = new ArrayList<String>();

            //minimum check per partition
            for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts().values()) {
                // store primary members in the partition context
                List<String> primaryMemberListInPartition = new ArrayList<String>();
                // get active primary members in this partition context
                for (MemberContext memberContext : partitionContext.getActiveMembers()) {
                    if (isPrimaryMember(memberContext)) {
                        primaryMemberListInPartition.add(memberContext.getMemberId());
                    }
                }
                // get pending primary members in this partition context
                for (MemberContext memberContext : partitionContext.getPendingMembers()) {
                    if (isPrimaryMember(memberContext)) {
                        primaryMemberListInPartition.add(memberContext.getMemberId());
                    }
                }
                primaryMemberListInNetworkPartition.addAll(primaryMemberListInPartition);
                getMinCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                getMinCheckKnowledgeSession().setGlobal("lbRef", lbReferenceType);
                getMinCheckKnowledgeSession().setGlobal("isPrimary", hasPrimary);
                getMinCheckKnowledgeSession().setGlobal("primaryMemberCount", primaryMemberListInPartition.size());

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Running minimum check for partition %s ", partitionContext.getPartitionId()));
                }

                minCheckFactHandle = AutoscalerRuleEvaluator.evaluateMinCheck(getMinCheckKnowledgeSession()
                        , minCheckFactHandle, partitionContext);

            }

            boolean rifReset = networkPartitionContext.isRifReset();
            boolean memoryConsumptionReset = networkPartitionContext.isMemoryConsumptionReset();
            boolean loadAverageReset = networkPartitionContext.isLoadAverageReset();
            if (log.isDebugEnabled()) {
                log.debug("flag of rifReset: " + rifReset + " flag of memoryConsumptionReset" + memoryConsumptionReset
                          + " flag of loadAverageReset" + loadAverageReset);
            }
            if (rifReset || memoryConsumptionReset || loadAverageReset) {
                getScaleCheckKnowledgeSession().setGlobal("clusterId", getClusterId());
                //scaleCheckKnowledgeSession.setGlobal("deploymentPolicy", deploymentPolicy);
                getScaleCheckKnowledgeSession().setGlobal("autoscalePolicy", autoscalePolicy);
                getScaleCheckKnowledgeSession().setGlobal("rifReset", rifReset);
                getScaleCheckKnowledgeSession().setGlobal("mcReset", memoryConsumptionReset);
                getScaleCheckKnowledgeSession().setGlobal("laReset", loadAverageReset);
                getScaleCheckKnowledgeSession().setGlobal("lbRef", lbReferenceType);
                getScaleCheckKnowledgeSession().setGlobal("isPrimary", false);
                getScaleCheckKnowledgeSession().setGlobal("primaryMembers", primaryMemberListInNetworkPartition);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Running scale check for network partition %s ", networkPartitionContext.getId()));
                    log.debug(" Primary members : " + primaryMemberListInNetworkPartition);
                }

                scaleCheckFactHandle = AutoscalerRuleEvaluator.evaluateScaleCheck(getScaleCheckKnowledgeSession()
                        , scaleCheckFactHandle, networkPartitionContext);

                networkPartitionContext.setRifReset(false);
                networkPartitionContext.setMemoryConsumptionReset(false);
                networkPartitionContext.setLoadAverageReset(false);
            } else if (log.isDebugEnabled()) {
                log.debug(String.format("Scale rule will not run since the LB statistics have not received before this " +
                                        "cycle for network partition %s", networkPartitionContext.getId()));
            }
        }
    }

    private boolean isPrimaryMember(MemberContext memberContext) {
        Properties props = memberContext.getProperties();
        if (log.isDebugEnabled()) {
            log.debug(" Properties [" + props + "] ");
        }
        if (props != null && props.getProperties() != null) {
            for (Property prop : props.getProperties()) {
                if (prop.getName().equals("PRIMARY")) {
                    if (Boolean.parseBoolean(prop.getValue())) {
                        log.debug("Adding member id [" + memberContext.getMemberId() + "] " +
                                  "member instance id [" + memberContext.getInstanceId() + "] as a primary member");
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void readConfigurations() {
        XMLConfiguration conf = ConfUtil.getInstance(null).getConfiguration();
        int monitorInterval = conf.getInt(AutoScalerConstants.AUTOSCALER_MONITOR_INTERVAL, 90000);
        setMonitorIntervalMilliseconds(monitorInterval);
        if (log.isDebugEnabled()) {
            log.debug("VMServiceClusterMonitor task interval: " + getMonitorIntervalMilliseconds());
        }
    }

    @Override
    public void destroy() {
        getMinCheckKnowledgeSession().dispose();
        getScaleCheckKnowledgeSession().dispose();
        setDestroyed(true);
        stopScheduler();
        if (log.isDebugEnabled()) {
            log.debug("VMServiceClusterMonitor Drools session has been disposed. " + this.toString());
        }
    }

    @Override
    public String toString() {
        return "VMServiceClusterMonitor [clusterId=" + getClusterId() + ", serviceId=" + getServiceId() +
               ", deploymentPolicy=" + deploymentPolicy + ", autoscalePolicy=" + autoscalePolicy +
               ", lbReferenceType=" + lbReferenceType +
               ", hasPrimary=" + hasPrimary + " ]";
    }

    public String getLbReferenceType() {
        return lbReferenceType;
    }

    public void setLbReferenceType(String lbReferenceType) {
        this.lbReferenceType = lbReferenceType;
    }

    public boolean isHasPrimary() {
        return hasPrimary;
    }

    public void setHasPrimary(boolean hasPrimary) {
        this.hasPrimary = hasPrimary;
    }
}
