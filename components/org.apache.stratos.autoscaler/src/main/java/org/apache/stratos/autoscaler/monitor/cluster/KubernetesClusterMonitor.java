/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import org.apache.stratos.autoscaler.KubernetesClusterContext;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.AverageLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.AverageMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.AverageRequestsInFlightEvent;
import org.apache.stratos.messaging.event.health.stat.GradientOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.GradientOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.GradientOfRequestsInFlightEvent;
import org.apache.stratos.messaging.event.health.stat.MemberAverageLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.MemberAverageMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.MemberFaultEvent;
import org.apache.stratos.messaging.event.health.stat.MemberGradientOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.MemberGradientOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.MemberSecondDerivativeOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.MemberSecondDerivativeOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfRequestsInFlightEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberReadyToShutdownEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/*
 * Every kubernetes cluster monitor should extend this class
 */
public abstract class KubernetesClusterMonitor extends AbstractClusterMonitor {

    private static final Log log = LogFactory.getLog(KubernetesClusterMonitor.class);

    private KubernetesClusterContext kubernetesClusterCtxt;

    protected KubernetesClusterMonitor(String clusterId,
                                       AutoscalerRuleEvaluator autoscalerRuleEvaluator,
                                       KubernetesClusterContext kubernetesClusterContext) {

        super(clusterId, autoscalerRuleEvaluator, kubernetesClusterContext);
        this.kubernetesClusterCtxt = kubernetesClusterContext;
    }

    @Override
    public void handleAverageLoadAverageEvent(
            AverageLoadAverageEvent averageLoadAverageEvent) {

        String clusterId = averageLoadAverageEvent.getClusterId();
        float value = averageLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg load avg event: [cluster] %s [value] %s",
                                    clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setAverageLoadAverage(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }

    }

    @Override
    public void handleGradientOfLoadAverageEvent(
            GradientOfLoadAverageEvent gradientOfLoadAverageEvent) {

        String clusterId = gradientOfLoadAverageEvent.getClusterId();
        float value = gradientOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of load avg event: [cluster] %s [value] %s",
                                    clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setLoadAverageGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfLoadAverageEvent(
            SecondDerivativeOfLoadAverageEvent secondDerivativeOfLoadAverageEvent) {

        String clusterId = secondDerivativeOfLoadAverageEvent.getClusterId();
        float value = secondDerivativeOfLoadAverageEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of load avg event: [cluster] %s "
                                    + "[value] %s", clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setLoadAverageSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleAverageMemoryConsumptionEvent(
            AverageMemoryConsumptionEvent averageMemoryConsumptionEvent) {

        String clusterId = averageMemoryConsumptionEvent.getClusterId();
        float value = averageMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Avg Memory Consumption event: [cluster] %s "
                                    + "[value] %s", averageMemoryConsumptionEvent.getClusterId(), value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setAverageMemoryConsumption(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleGradientOfMemoryConsumptionEvent(
            GradientOfMemoryConsumptionEvent gradientOfMemoryConsumptionEvent) {

        String clusterId = gradientOfMemoryConsumptionEvent.getClusterId();
        float value = gradientOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Grad of Memory Consumption event: [cluster] %s "
                                    + "[value] %s", clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setMemoryConsumptionGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfMemoryConsumptionEvent(
            SecondDerivativeOfMemoryConsumptionEvent secondDerivativeOfMemoryConsumptionEvent) {

        String clusterId = secondDerivativeOfMemoryConsumptionEvent.getClusterId();
        float value = secondDerivativeOfMemoryConsumptionEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second Derivation of Memory Consumption event: [cluster] %s "
                                    + "[value] %s", clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setMemoryConsumptionSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleAverageRequestsInFlightEvent(
            AverageRequestsInFlightEvent averageRequestsInFlightEvent) {

        float value = averageRequestsInFlightEvent.getValue();
        String clusterId = averageRequestsInFlightEvent.getClusterId();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Average Rif event: [cluster] %s [value] %s",
                                    clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setAverageRequestsInFlight(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleGradientOfRequestsInFlightEvent(
            GradientOfRequestsInFlightEvent gradientOfRequestsInFlightEvent) {

        String clusterId = gradientOfRequestsInFlightEvent.getClusterId();
        float value = gradientOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Gradient of Rif event: [cluster] %s [value] %s",
                                    clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setRequestsInFlightGradient(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleSecondDerivativeOfRequestsInFlightEvent(
            SecondDerivativeOfRequestsInFlightEvent secondDerivativeOfRequestsInFlightEvent) {

        String clusterId = secondDerivativeOfRequestsInFlightEvent.getClusterId();
        float value = secondDerivativeOfRequestsInFlightEvent.getValue();
        if (log.isDebugEnabled()) {
            log.debug(String.format("Second derivative of Rif event: [cluster] %s "
                                    + "[value] %s", clusterId, value));
        }
        KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterCtxt();
        if (null != kubernetesClusterContext) {
            kubernetesClusterContext.setRequestsInFlightSecondDerivative(value);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes cluster context is not available for :" +
                                        " [cluster] %s", clusterId));
            }
        }
    }

    @Override
    public void handleMemberAverageMemoryConsumptionEvent(
            MemberAverageMemoryConsumptionEvent memberAverageMemoryConsumptionEvent) {

        String memberId = memberAverageMemoryConsumptionEvent.getMemberId();
        KubernetesClusterContext kubernetesClusterCtxt = getKubernetesClusterCtxt();
        MemberStatsContext memberStatsContext = kubernetesClusterCtxt.getMemberStatsContext(memberId);
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
        KubernetesClusterContext kubernetesClusterCtxt = getKubernetesClusterCtxt();
        MemberStatsContext memberStatsContext = kubernetesClusterCtxt.getMemberStatsContext(memberId);
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

        KubernetesClusterContext kubernetesClusterCtxt = getKubernetesClusterCtxt();
        String memberId = memberAverageLoadAverageEvent.getMemberId();
        float value = memberAverageLoadAverageEvent.getValue();
        MemberStatsContext memberStatsContext = kubernetesClusterCtxt.getMemberStatsContext(memberId);
        if (null == memberStatsContext) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return;
        }
        memberStatsContext.setAverageLoadAverage(value);
    }

    @Override
    public void handleMemberGradientOfLoadAverageEvent(
            MemberGradientOfLoadAverageEvent memberGradientOfLoadAverageEvent) {

        String memberId = memberGradientOfLoadAverageEvent.getMemberId();
        KubernetesClusterContext kubernetesClusterCtxt = getKubernetesClusterCtxt();
        MemberStatsContext memberStatsContext = kubernetesClusterCtxt.getMemberStatsContext(memberId);
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
        KubernetesClusterContext kubernetesClusterCtxt = getKubernetesClusterCtxt();
        MemberStatsContext memberStatsContext = kubernetesClusterCtxt.getMemberStatsContext(memberId);
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
    	// kill the container
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

        if (!getKubernetesClusterCtxt().activeMemberExist(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Could not find the active member in kubernetes cluster context, "
                                        + "[member] %s ", memberId));
            }
            return;
        }
        // terminate the faulty member
        CloudControllerClient ccClient = CloudControllerClient.getInstance();
        try {
            ccClient.terminateContainer(memberId);
            // remove from active member list
            getKubernetesClusterCtxt().removeActiveMemberById(memberId);
            if (log.isInfoEnabled()) {
                String clusterId = memberFaultEvent.getClusterId();
                String kubernetesClusterID = getKubernetesClusterCtxt().getKubernetesClusterID();
				log.info(String.format("Faulty member is terminated and removed from the active members list: "
                                       + "[member] %s [kub-cluster] %s [cluster] %s ", memberId, kubernetesClusterID, clusterId));
            }
        } catch (TerminationException e) {
            String msg = "Cannot delete a container " + e.getLocalizedMessage();
            log.error(msg, e);
        }
    }

    @Override
    public void handleMemberStartedEvent(
            MemberStartedEvent memberStartedEvent) {

    }

    @Override
    public void handleMemberActivatedEvent(
            MemberActivatedEvent memberActivatedEvent) {

        KubernetesClusterContext kubernetesClusterContext;
        kubernetesClusterContext = getKubernetesClusterCtxt();
        String memberId = memberActivatedEvent.getMemberId();
        kubernetesClusterContext.addMemberStatsContext(new MemberStatsContext(memberId));
        if (log.isInfoEnabled()) {
            log.info(String.format("Member stat context has been added successfully: "
                                   + "[member] %s", memberId));
        }
        kubernetesClusterContext.movePendingMemberToActiveMembers(memberId);
    }

    @Override
    public void handleMemberMaintenanceModeEvent(
            MemberMaintenanceModeEvent maintenanceModeEvent) {

        // no need to do anything here
        // we will not be receiving this event for containers
        // we will only receive member terminated event
    }

    @Override
    public void handleMemberReadyToShutdownEvent(
            MemberReadyToShutdownEvent memberReadyToShutdownEvent) {

        // no need to do anything here
        // we will not be receiving this event for containers
    	// we will only receive member terminated event
    }

    @Override
    public void handleMemberTerminatedEvent(
            MemberTerminatedEvent memberTerminatedEvent) {

        String memberId = memberTerminatedEvent.getMemberId();
        if (getKubernetesClusterCtxt().removeTerminationPendingMember(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member is removed from termination pending members list: "
                                        + "[member] %s", memberId));
            }
        } else if (getKubernetesClusterCtxt().removePendingMember(memberId)) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Member is removed from pending members list: "
                                        + "[member] %s", memberId));
            }
        } else if (getKubernetesClusterCtxt().removeActiveMemberById(memberId)) {
            log.warn(String.format("Member is in the wrong list and it is removed from "
                                   + "active members list", memberId));
        } else if (getKubernetesClusterCtxt().removeObsoleteMember(memberId)) {
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
    }

    @Override
    public void handleClusterRemovedEvent(
            ClusterRemovedEvent clusterRemovedEvent) {
    	getKubernetesClusterCtxt().getPendingMembers().clear();
    	getKubernetesClusterCtxt().getActiveMembers().clear();
    	getKubernetesClusterCtxt().getTerminationPendingMembers().clear();
    	getKubernetesClusterCtxt().getObsoletedMembers().clear();
    }

    public KubernetesClusterContext getKubernetesClusterCtxt() {
        return kubernetesClusterCtxt;
    }

    public void setKubernetesClusterCtxt(
            KubernetesClusterContext kubernetesClusterCtxt) {
        this.kubernetesClusterCtxt = kubernetesClusterCtxt;
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

    @Override
    public void terminateAllMembers() {
        try {
            CloudControllerClient.getInstance().terminateAllContainers(getKubernetesClusterCtxt().getClusterId());
        } catch (TerminationException e) {
            log.error(String.format("Could not terminate containers: [cluster-id] %s",
                    getKubernetesClusterCtxt().getClusterId()), e);
        }
    }
}
