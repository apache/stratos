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
package org.apache.stratos.autoscaler.monitor.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.grouping.topic.StatusEventPublisher;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.events.MonitorStatusEvent;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.messaging.domain.topology.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.GroupStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
public class ClusterMonitor extends AbstractClusterMonitor {

    private static final Log log = LogFactory.getLog(ClusterMonitor.class);
    private String lbReferenceType;
    private boolean hasPrimary;


    public ClusterMonitor(String clusterId, String serviceId, DeploymentPolicy deploymentPolicy,
                          AutoscalePolicy autoscalePolicy) {
        this.clusterId = clusterId;
        this.id = clusterId;
        this.serviceId = serviceId;

        this.autoscalerRuleEvaluator = new AutoscalerRuleEvaluator();
        this.scaleCheckKnowledgeSession = autoscalerRuleEvaluator.getScaleCheckStatefulSession();
        this.minCheckKnowledgeSession = autoscalerRuleEvaluator.getMinCheckStatefulSession();
        this.terminateAllKnowledgeSession = autoscalerRuleEvaluator.getTerminateAllStatefulSession();

        this.deploymentPolicy = deploymentPolicy;
        this.autoscalePolicy = autoscalePolicy;
        if (log.isDebugEnabled()) {
            log.debug("ClusterMonitor:autoScalePolicy:" + autoscalePolicy);
        }
        networkPartitionCtxts = new ConcurrentHashMap<String, NetworkPartitionContext>();
        //status = ClusterStatus.Created;
    }

    private static void terminateMember(String memberId) {
        try {
            CloudControllerClient.getInstance().terminate(memberId);

        } catch (TerminationException e) {
            log.error("Unable to terminate member [member id ] " + memberId, e);
        }
    }

    @Override
    public void run() {
        while (!isDestroyed()) {
            try {
                if (((this.status.getCode() <= ClusterStatus.Active.getCode()) ||
                        (this.status == ClusterStatus.Inactive && !hasDependent)) && !this.hasFaultyMember) {
                    if (log.isDebugEnabled()) {
                        log.debug("Cluster monitor is running.. " + this.toString());
                    }
                    monitor();

                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Cluster monitor is suspended as the cluster is in " +
                                ClusterStatus.Inactive + " mode......");
                    }
                }
            } catch (Exception e) {
                log.error("Cluster monitor: Monitor failed." + this.toString(), e);
            }
            try {
                Thread.sleep(monitorInterval);
            } catch (InterruptedException ignore) {
            }
        }


    }

    @Override
    public void terminateAllMembers() {

        Thread memberTerminator = new Thread(new Runnable() {
            public void run() {

                for (NetworkPartitionContext networkPartitionContext : networkPartitionCtxts.values()) {
                    for (PartitionContext partitionContext : networkPartitionContext.getPartitionCtxts().values()) {
                        //if (log.isDebugEnabled()) {
                        log.info("Starting to terminate all members in Network Partition [ " +
                                networkPartitionContext.getId() + " ], Partition [ " +
                                partitionContext.getPartitionId() + " ]");
                        // }
                        // need to terminate active, pending and obsolete members

                        // active members
                        for (MemberContext activeMemberCtxt : partitionContext.getActiveMembers()) {
                            log.info("Terminating active member [member id] " + activeMemberCtxt.getMemberId());
                            terminateMember(activeMemberCtxt.getMemberId());
                        }

                        // pending members
                        for (MemberContext pendingMemberCtxt : partitionContext.getPendingMembers()) {
                            log.info("Terminating pending member [member id] " + pendingMemberCtxt.getMemberId());
                            terminateMember(pendingMemberCtxt.getMemberId());
                        }

                        // obsolete members
                        for (String obsoleteMemberId : partitionContext.getObsoletedMembers()) {
                            log.info("Terminating obsolete member [member id] " + obsoleteMemberId);
                            terminateMember(obsoleteMemberId);
                        }

//                terminateAllFactHandle = AutoscalerRuleEvaluator.evaluateTerminateAll
//                        (terminateAllKnowledgeSession, terminateAllFactHandle, partitionContext);
                    }
                }
            }
        }, "Member Terminator - [cluster id] " + this.clusterId);

        memberTerminator.start();
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

    public void monitor() {
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
                minCheckKnowledgeSession.setGlobal("clusterId", clusterId);
                minCheckKnowledgeSession.setGlobal("lbRef", lbReferenceType);
                minCheckKnowledgeSession.setGlobal("isPrimary", hasPrimary);


                if (log.isDebugEnabled()) {
                    log.debug(String.format("Running minimum check for partition %s ", partitionContext.getPartitionId()));
                }

                minCheckFactHandle = AutoscalerRuleEvaluator.evaluateMinCheck(minCheckKnowledgeSession
                        , minCheckFactHandle, partitionContext);

                //checking the status of the cluster


            }

        	/*boolean rifReset = networkPartitionContext.isRifReset();
            boolean memoryConsumptionReset = networkPartitionContext.isMemoryConsumptionReset();
            boolean loadAverageReset = networkPartitionContext.isLoadAverageReset();

            if (log.isDebugEnabled()) {
                log.debug("flag of rifReset: " + rifReset + " flag of memoryConsumptionReset" + memoryConsumptionReset
                        + " flag of loadAverageReset" + loadAverageReset);
            }
            if (rifReset || memoryConsumptionReset || loadAverageReset) {

                scaleCheckKnowledgeSession.setGlobal("clusterId", clusterId);
                //scaleCheckKnowledgeSession.setGlobal("deploymentPolicy", deploymentPolicy);
                scaleCheckKnowledgeSession.setGlobal("autoscalePolicy", autoscalePolicy);
                scaleCheckKnowledgeSession.setGlobal("rifReset", rifReset);
                scaleCheckKnowledgeSession.setGlobal("mcReset", memoryConsumptionReset);
                scaleCheckKnowledgeSession.setGlobal("laReset", loadAverageReset);
                scaleCheckKnowledgeSession.setGlobal("lbRef", lbReferenceType);
                scaleCheckKnowledgeSession.setGlobal("isPrimary", false);
                scaleCheckKnowledgeSession.setGlobal("primaryMembers", primaryMemberListInNetworkPartition);

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Running scale check for network partition %s ", networkPartitionContext.getId()));
                    log.debug(" Primary members : " + primaryMemberListInNetworkPartition);
                }

                scaleCheckFactHandle = AutoscalerRuleEvaluator.evaluateScaleCheck(scaleCheckKnowledgeSession
                        , scaleCheckFactHandle, networkPartitionContext);

                networkPartitionContext.setRifReset(false);
                networkPartitionContext.setMemoryConsumptionReset(false);
                networkPartitionContext.setLoadAverageReset(false);
            } else if (log.isDebugEnabled()) {
                log.debug(String.format("Scale rule will not run since the LB statistics have not received before this " +
                        "cycle for network partition %s", networkPartitionContext.getId()));
            }*/
        }
    }

    @Override
    public String toString() {
        return "ClusterMonitor [clusterId=" + clusterId + ", serviceId=" + serviceId +
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

    @Override
    public void onChildEvent(MonitorStatusEvent statusEvent) {

    }

    @Override
    public void onParentEvent(MonitorStatusEvent statusEvent) {
        // send the ClusterTerminating event
        if (statusEvent.getStatus() == GroupStatus.Terminating || statusEvent.getStatus() ==
                ApplicationStatus.Terminating) {
            StatusEventPublisher.sendClusterTerminatingEvent(appId, serviceId, clusterId);
        }
    }
}
