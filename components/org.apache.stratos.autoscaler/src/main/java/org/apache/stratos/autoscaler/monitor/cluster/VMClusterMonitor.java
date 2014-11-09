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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Is responsible for monitoring a service cluster. This runs periodically
 * and perform minimum instance check and scaling check using the underlying
 * rules engine.
 */
abstract public class VMClusterMonitor extends AbstractClusterMonitor {

    private static final Log log = LogFactory.getLog(VMClusterMonitor.class);
    protected VMClusterMonitor(String clusterId, AutoscalerRuleEvaluator autoscalerRuleEvaluator,
                               VMClusterContext vmClusterContext) {
        super(clusterId, autoscalerRuleEvaluator, vmClusterContext);
    }

    @Override
    public void handleAverageLoadAverageEvent(
            AverageLoadAverageEvent averageLoadAverageEvent) {

        String networkPartitionId = averageLoadAverageEvent.getNetworkPartitionId();
        String clusterId = averageLoadAverageEvent.getClusterId();
        float value = averageLoadAverageEvent.getValue();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg load avg event: [cluster] %s [network-partition] %s [value] %s",
                                    clusterId, networkPartitionId, value));
        }

        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setAverageLoadAverage(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }

    }

    @Override
    public void handleGradientOfLoadAverageEvent(
            GradientOfLoadAverageEvent gradientOfLoadAverageEvent) {

        String networkPartitionId = gradientOfLoadAverageEvent.getNetworkPartitionId();
        String clusterId = gradientOfLoadAverageEvent.getClusterId();
        float value = gradientOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of load avg event: [cluster] %s [network-partition] %s [value] %s",
                                    clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setLoadAverageGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfLoadAverageEvent(
            SecondDerivativeOfLoadAverageEvent secondDerivativeOfLoadAverageEvent) {

        String networkPartitionId = secondDerivativeOfLoadAverageEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfLoadAverageEvent.getClusterId();
        float value = secondDerivativeOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of load avg event: [cluster] %s "
                                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setLoadAverageSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleAverageMemoryConsumptionEvent(
            AverageMemoryConsumptionEvent averageMemoryConsumptionEvent) {

        String networkPartitionId = averageMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = averageMemoryConsumptionEvent.getClusterId();
        float value = averageMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg Memory Consumption event: [cluster] %s [network-partition] %s "
                                    + "[value] %s", clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setAverageMemoryConsumption(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String
                                  .format("Network partition context is not available for :"
                                          + " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleGradientOfMemoryConsumptionEvent(
            GradientOfMemoryConsumptionEvent gradientOfMemoryConsumptionEvent) {

        String networkPartitionId = gradientOfMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = gradientOfMemoryConsumptionEvent.getClusterId();
        float value = gradientOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of Memory Consumption event: [cluster] %s "
                                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setMemoryConsumptionGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfMemoryConsumptionEvent(
            SecondDerivativeOfMemoryConsumptionEvent secondDerivativeOfMemoryConsumptionEvent) {

        String networkPartitionId = secondDerivativeOfMemoryConsumptionEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfMemoryConsumptionEvent.getClusterId();
        float value = secondDerivativeOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of Memory Consumption event: [cluster] %s "
                                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setMemoryConsumptionSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    public void handleAverageRequestsServingCapabilityEvent(AverageRequestsServingCapabilityEvent averageRequestsServingCapabilityEvent) {

        String clusterId = averageRequestsServingCapabilityEvent.getClusterId();
        String networkPartitionId = averageRequestsServingCapabilityEvent.getNetworkPartitionId();
        Float floatValue = averageRequestsServingCapabilityEvent.getValue();

        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Requests Served per Instance event: [cluster] %s [network-partition] %s [value] %s",
                    clusterId, networkPartitionId, floatValue));
        }

        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if(null != networkPartitionContext){
            networkPartitionContext.setAverageRequestsServedPerInstance(floatValue);

        } else {
            if(log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                        " [network partition] %s", networkPartitionId));
            }
        }

    }

    @Override
    public void handleAverageRequestsInFlightEvent(
            AverageRequestsInFlightEvent averageRequestsInFlightEvent) {

        String networkPartitionId = averageRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = averageRequestsInFlightEvent.getClusterId();
        Float servedCount = averageRequestsInFlightEvent.getServedCount();
        Float activeInstances = averageRequestsInFlightEvent.getActiveInstances();
        Float requestsServedPerInstance = servedCount/activeInstances;
        if(requestsServedPerInstance.isInfinite()){
            requestsServedPerInstance = 0f;
        }
        float value = averageRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Rif event: [cluster] %s [network-partition] %s [value] %s",
                                    clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setAverageRequestsInFlight(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleGradientOfRequestsInFlightEvent(
            GradientOfRequestsInFlightEvent gradientOfRequestsInFlightEvent) {

        String networkPartitionId = gradientOfRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = gradientOfRequestsInFlightEvent.getClusterId();
        float value = gradientOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Gradient of Rif event: [cluster] %s [network-partition] %s [value] %s",
                                    clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setRequestsInFlightGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfRequestsInFlightEvent(
            SecondDerivativeOfRequestsInFlightEvent secondDerivativeOfRequestsInFlightEvent) {

        String networkPartitionId = secondDerivativeOfRequestsInFlightEvent.getNetworkPartitionId();
        String clusterId = secondDerivativeOfRequestsInFlightEvent.getClusterId();
        float value = secondDerivativeOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second derivative of Rif event: [cluster] %s "
                                    + "[network-partition] %s [value] %s", clusterId, networkPartitionId, value));
        }
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        if (null != networkPartitionContext) {
            networkPartitionContext.setRequestsInFlightSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Network partition context is not available for :" +
                                        " [network partition] %s", networkPartitionId));
            }
        }
    }

    @Override
    public void handleMemberAverageMemoryConsumptionEvent(
            MemberAverageMemoryConsumptionEvent memberAverageMemoryConsumptionEvent) {

        String memberId = memberAverageMemoryConsumptionEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberAverageMemoryConsumptionEvent.getValue();
        memberStatsContext.setAverageMemoryConsumption(value);
    }

    @Override
    public void handleMemberGradientOfMemoryConsumptionEvent(
            MemberGradientOfMemoryConsumptionEvent memberGradientOfMemoryConsumptionEvent) {

        String memberId = memberGradientOfMemoryConsumptionEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberGradientOfMemoryConsumptionEvent.getValue();
        memberStatsContext.setGradientOfMemoryConsumption(value);
    }

    @Override
    public void handleMemberSecondDerivativeOfMemoryConsumptionEvent(
            MemberSecondDerivativeOfMemoryConsumptionEvent memberSecondDerivativeOfMemoryConsumptionEvent) {

    }

    @Override
    public void handleMemberAverageLoadAverageEvent(
            MemberAverageLoadAverageEvent memberAverageLoadAverageEvent) {

        String memberId = memberAverageLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberAverageLoadAverageEvent.getValue();
        memberStatsContext.setAverageLoadAverage(value);
    }

    @Override
    public void handleMemberGradientOfLoadAverageEvent(
            MemberGradientOfLoadAverageEvent memberGradientOfLoadAverageEvent) {

        String memberId = memberGradientOfLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberGradientOfLoadAverageEvent.getValue();
        memberStatsContext.setGradientOfLoadAverage(value);
    }

    @Override
    public void handleMemberSecondDerivativeOfLoadAverageEvent(
            MemberSecondDerivativeOfLoadAverageEvent memberSecondDerivativeOfLoadAverageEvent) {

        String memberId = memberSecondDerivativeOfLoadAverageEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        String networkPartitionId = getNetworkPartitionIdByMemberId(memberId);
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionCtxt = networkPartitionCtxt.getPartitionCtxt(member.getPartitionId());
        MemberStatsContext memberStatsContext = partitionCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        float value = memberSecondDerivativeOfLoadAverageEvent.getValue();
        memberStatsContext.setSecondDerivativeOfLoadAverage(value);
    }

    @Override
    public void handleMemberFaultEvent(MemberFaultEvent memberFaultEvent) {

        String memberId = memberFaultEvent.getMemberId();
        Member member = getMemberByMemberId(memberId);
        if (null == member) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member not found in the Topology: [member] %s", memberId));
            }
            return;
        }
        if (!member.isActive()) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member activated event has not received for the member %s. "
                                        + "Therefore ignoring" + " the member fault health stat", memberId));
            }
            return;
        }

        NetworkPartitionContext nwPartitionCtxt;
        nwPartitionCtxt = getNetworkPartitionCtxt(member);
        String partitionId = getPartitionOfMember(memberId);
        PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);
        if (!partitionCtxt.activeMemberExist(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Could not find the active member in partition context, "
                                        + "[member] %s ", memberId));
            }
            return;
        }
        // terminate the faulty member
        CloudControllerClient ccClient = CloudControllerClient.getInstance();
        try {
            ccClient.terminate(memberId);
        } catch (TerminationException e) {
            String msg = "TerminationException " + e.getLocalizedMessage();
            log.error(msg, e);
        }
        // remove from active member list
        partitionCtxt.removeActiveMemberById(memberId);
        if (log.isInfoEnabled()) {
            String clusterId = memberFaultEvent.getClusterId();
            log.info(String.format("Faulty member is terminated and removed from the active members list: "
                                   + "[member] %s [partition] %s [cluster] %s ", memberId, partitionId, clusterId));
        }

        StatusChecker.getInstance().onMemberFaultEvent(memberFaultEvent.getClusterId(), partitionId);
    }

    @Override
    public void handleMemberStartedEvent(
            MemberStartedEvent memberStartedEvent) {

    }

    @Override
    public void handleMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {

        String networkPartitionId = memberActivatedEvent.getNetworkPartitionId();
        String partitionId = memberActivatedEvent.getPartitionId();
        String memberId = memberActivatedEvent.getMemberId();
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionContext;
        partitionContext = networkPartitionCtxt.getPartitionCtxt(partitionId);
        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isInfoEnabled()) {
            log.info(String.format("Member stat context has been added successfully: "
                                   + "[member] %s", memberId));
        }
        partitionContext.movePendingMemberToActiveMembers(memberId);
        StatusChecker.getInstance().onMemberStatusChange(memberActivatedEvent.getClusterId());
    }

    @Override
    public void handleMemberMaintenanceModeEvent(
            MemberMaintenanceModeEvent maintenanceModeEvent) {

        String networkPartitionId = maintenanceModeEvent.getNetworkPartitionId();
        String partitionId = maintenanceModeEvent.getPartitionId();
        String memberId = maintenanceModeEvent.getMemberId();
        NetworkPartitionContext networkPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionContext = networkPartitionCtxt.getPartitionCtxt(partitionId);
        partitionContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isDebugEnabled()) {
            log.debug(String.format("Member has been moved as pending termination: "
                                    + "[member] %s", memberId));
        }
        partitionContext.moveActiveMemberToTerminationPendingMembers(memberId);
    }

    @Override
    public void handleMemberReadyToShutdownEvent(
            MemberReadyToShutdownEvent memberReadyToShutdownEvent) {

        NetworkPartitionContext nwPartitionCtxt;
        String networkPartitionId = memberReadyToShutdownEvent.getNetworkPartitionId();
        nwPartitionCtxt = getNetworkPartitionCtxt(networkPartitionId);

        // start a new member in the same Partition
        String memberId = memberReadyToShutdownEvent.getMemberId();
        String partitionId = getPartitionOfMember(memberId);
        PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);
        // terminate the shutdown ready member
        CloudControllerClient ccClient = CloudControllerClient.getInstance();
        try {
            ccClient.terminate(memberId);
            // remove from active member list
            partitionCtxt.removeActiveMemberById(memberId);

            String clusterId = memberReadyToShutdownEvent.getClusterId();
            log.info(String.format("Member is terminated and removed from the active members list: "
                                   + "[member] %s [partition] %s [cluster] %s ", memberId, partitionId, clusterId));
        } catch (TerminationException e) {
            String msg = "TerminationException" + e.getLocalizedMessage();
            log.error(msg, e);
        }
    }

    @Override
    public void handleMemberTerminatedEvent(
            MemberTerminatedEvent memberTerminatedEvent) {

        String networkPartitionId = memberTerminatedEvent.getNetworkPartitionId();
        String memberId = memberTerminatedEvent.getMemberId();
        String clusterId = memberTerminatedEvent.getClusterId();
        String partitionId = memberTerminatedEvent.getPartitionId();
        NetworkPartitionContext networkPartitionContext = getNetworkPartitionCtxt(networkPartitionId);
        PartitionContext partitionContext = networkPartitionContext.getPartitionCtxt(partitionId);
        partitionContext.removeMemberStatsContext(memberId);

        if (partitionContext.removeTerminationPendingMember(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member is removed from termination pending members list: "
                                        + "[member] %s", memberId));
            }
        } else if (partitionContext.removePendingMember(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member is removed from pending members list: "
                                        + "[member] %s", memberId));
            }
        } else if (partitionContext.removeActiveMemberById(memberId)) {
            log.warn(String.format("Member is in the wrong list and it is removed from "
                                   + "active members list", memberId));
        } else if (partitionContext.removeObsoleteMember(memberId)) {
            log.warn(String.format("Member's obsolated timeout has been expired and "
                                   + "it is removed from obsolated members list", memberId));
        } else {
            log.warn(String.format("Member is not available in any of the list active, "
                                   + "pending and termination pending", memberId));
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Member stat context has been removed successfully: "
                                   + "[member] %s", memberId));
        }
        //Checking whether the cluster state can be changed either from in_active to created/terminating to terminated
		StatusChecker.getInstance().onMemberTermination(clusterId);
    }

    @Override
    public void handleClusterRemovedEvent(
            ClusterRemovedEvent clusterRemovedEvent) {

    }

    @Override
    public void handleDynamicUpdates(Properties properties) throws InvalidArgumentException {

    }

    private String getNetworkPartitionIdByMemberId(String memberId) {
        for (Service service : TopologyManager.getTopology().getServices()) {
            for (Cluster cluster : service.getClusters()) {
                if (cluster.memberExists(memberId)) {
                    return cluster.getMember(memberId).getNetworkPartitionId();
                }
            }
        }
        return null;
    }

    private Member getMemberByMemberId(String memberId) {
        try {
            TopologyManager.acquireReadLock();
            for (Service service : TopologyManager.getTopology().getServices()) {
                for (Cluster cluster : service.getClusters()) {
                    if (cluster.memberExists(memberId)) {
                        return cluster.getMember(memberId);
                    }
                }
            }
            return null;
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    public String getPartitionOfMember(String memberId) {
        for (Service service : TopologyManager.getTopology().getServices()) {
            for (Cluster cluster : service.getClusters()) {
                if (cluster.memberExists(memberId)) {
                    return cluster.getMember(memberId).getPartitionId();
                }
            }
        }
        return null;
    }

    @Override
    public void terminateAllMembers() {

        Thread memberTerminator = new Thread(new Runnable(){
            public void run(){

                for (NetworkPartitionContext networkPartitionContext : getNetworkPartitionCtxts().values()) {
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
                        for  (MemberContext pendingMemberCtxt : partitionContext.getPendingMembers()) {
                            log.info("Terminating pending member [member id] " + pendingMemberCtxt.getMemberId());
                            terminateMember(pendingMemberCtxt.getMemberId());
                        }

                        // obsolete members
                        for (String obsoleteMemberId : partitionContext.getObsoletedMembers().keySet()) {
                            log.info("Terminating obsolete member [member id] " + obsoleteMemberId);
                            terminateMember(obsoleteMemberId);
                        }

//                terminateAllFactHandle = AutoscalerRuleEvaluator.evaluateTerminateAll
//                        (terminateAllKnowledgeSession, terminateAllFactHandle, partitionContext);
                    }
                }
            }
        }, "Member Terminator - [cluster id] " + getClusterId());

        memberTerminator.start();
    }

    public Map<String, NetworkPartitionContext> getNetworkPartitionCtxts() {

        VMClusterContext vmClusterContext = (VMClusterContext) clusterContext;
        return vmClusterContext.getNetworkPartitionCtxts();
    }
    protected NetworkPartitionContext getNetworkPartitionCtxt(String id) {

        VMClusterContext vmClusterContext = (VMClusterContext) clusterContext;
        return vmClusterContext.getNetworkPartitionCtxt(id);
    }

    protected NetworkPartitionContext getNetworkPartitionCtxt(Member member) {

        VMClusterContext vmClusterContext = (VMClusterContext) clusterContext;
        return vmClusterContext.getNetworkPartitionCtxt(member);
    }

    private static void terminateMember (String memberId) {
        try {
            CloudControllerClient.getInstance().terminate(memberId);

        } catch (TerminationException e) {
            log.error("Unable to terminate member [member id ] " + memberId, e);
        }
    }


}