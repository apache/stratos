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
package org.apache.stratos.autoscaler.event.receiver.health;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.VMClusterMonitor;
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
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfLoadAverageEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfMemoryConsumptionEvent;
import org.apache.stratos.messaging.event.health.stat.SecondDerivativeOfRequestsInFlightEvent;
import org.apache.stratos.messaging.event.health.stat.AverageRequestsServingCapabilityEvent;
import org.apache.stratos.messaging.listener.health.stat.AverageLoadAverageEventListener;
import org.apache.stratos.messaging.listener.health.stat.AverageMemoryConsumptionEventListener;
import org.apache.stratos.messaging.listener.health.stat.AverageRequestsInFlightEventListener;
import org.apache.stratos.messaging.listener.health.stat.AverageRequestsServingCapabilityEventListener;
import org.apache.stratos.messaging.listener.health.stat.GradientOfLoadAverageEventListener;
import org.apache.stratos.messaging.listener.health.stat.GradientOfMemoryConsumptionEventListener;
import org.apache.stratos.messaging.listener.health.stat.GradientOfRequestsInFlightEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberAverageLoadAverageEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberAverageMemoryConsumptionEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberFaultEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberGradientOfLoadAverageEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberGradientOfMemoryConsumptionEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberSecondDerivativeOfLoadAverageEventListener;
import org.apache.stratos.messaging.listener.health.stat.MemberSecondDerivativeOfMemoryConsumptionEventListener;
import org.apache.stratos.messaging.listener.health.stat.SecondDerivativeOfLoadAverageEventListener;
import org.apache.stratos.messaging.listener.health.stat.SecondDerivativeOfMemoryConsumptionEventListener;
import org.apache.stratos.messaging.listener.health.stat.SecondDerivativeOfRequestsInFlightEventListener;
import org.apache.stratos.messaging.message.receiver.health.stat.HealthStatEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class AutoscalerHealthStatEventReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(AutoscalerHealthStatEventReceiver.class);
    private boolean terminated = false;

    private HealthStatEventReceiver healthStatEventReceiver;

    public AutoscalerHealthStatEventReceiver() {
		this.healthStatEventReceiver = new HealthStatEventReceiver();
        addEventListeners();
    }

    @Override
    public void run() {
        //FIXME this activated before autoscaler deployer activated.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ignore) {
        }
        Thread thread = new Thread(healthStatEventReceiver);
        thread.start();
        if(log.isInfoEnabled()) {
            log.info("Autoscaler health stat event receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated){
        	try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
        }
        if(log.isInfoEnabled()) {
            log.info("Autoscaler health stat event receiver thread terminated");
        }
    }

    private void addEventListeners() {
        // Listen to health stat events that affect clusters
        healthStatEventReceiver.addEventListener(new AverageLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                AverageLoadAverageEvent averageLoadAverageEvent = (AverageLoadAverageEvent) event;
                String clusterId = averageLoadAverageEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleAverageLoadAverageEvent(averageLoadAverageEvent);
            }

        });

        healthStatEventReceiver.addEventListener(new AverageMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                AverageMemoryConsumptionEvent averageMemoryConsumptionEvent = (AverageMemoryConsumptionEvent) event;
                String clusterId = averageMemoryConsumptionEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleAverageMemoryConsumptionEvent(averageMemoryConsumptionEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new AverageRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                AverageRequestsInFlightEvent averageRequestsInFlightEvent = (AverageRequestsInFlightEvent) event;
                String clusterId = averageRequestsInFlightEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleAverageRequestsInFlightEvent(averageRequestsInFlightEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new AverageRequestsServingCapabilityEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                AverageRequestsServingCapabilityEvent averageRequestsServingCapabilityEvent = (AverageRequestsServingCapabilityEvent) event;
                String clusterId = averageRequestsServingCapabilityEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                if(monitor instanceof VMClusterMonitor) {
                    VMClusterMonitor vmClusterMonitor = (VMClusterMonitor) monitor;
                    vmClusterMonitor.handleAverageRequestsServingCapabilityEvent(averageRequestsServingCapabilityEvent);
                }
            }
        });

        healthStatEventReceiver.addEventListener(new GradientOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                GradientOfLoadAverageEvent gradientOfLoadAverageEvent = (GradientOfLoadAverageEvent) event;
                String clusterId = gradientOfLoadAverageEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleGradientOfLoadAverageEvent(gradientOfLoadAverageEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new GradientOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                GradientOfMemoryConsumptionEvent gradientOfMemoryConsumptionEvent = (GradientOfMemoryConsumptionEvent) event;
                String clusterId = gradientOfMemoryConsumptionEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleGradientOfMemoryConsumptionEvent(gradientOfMemoryConsumptionEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new GradientOfRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                GradientOfRequestsInFlightEvent gradientOfRequestsInFlightEvent = (GradientOfRequestsInFlightEvent) event;
                String clusterId = gradientOfRequestsInFlightEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleGradientOfRequestsInFlightEvent(gradientOfRequestsInFlightEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberAverageLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberAverageLoadAverageEvent memberAverageLoadAverageEvent = (MemberAverageLoadAverageEvent) event;
                String memberId = memberAverageLoadAverageEvent.getMemberId();
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
                                                + "Therefore ignoring" + " the health stat", memberId));
                    }
                    return;
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                String clusterId = member.getClusterId();
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleMemberAverageLoadAverageEvent(memberAverageLoadAverageEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberAverageMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberAverageMemoryConsumptionEvent memberAverageMemoryConsumptionEvent = (MemberAverageMemoryConsumptionEvent) event;
                String memberId = memberAverageMemoryConsumptionEvent.getMemberId();
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
                                                + "Therefore ignoring" + " the health stat", memberId));
                    }
                    return;
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                String clusterId = member.getClusterId();
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleMemberAverageMemoryConsumptionEvent(memberAverageMemoryConsumptionEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberFaultEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberFaultEvent memberFaultEvent = (MemberFaultEvent) event;
                String clusterId = memberFaultEvent.getClusterId();
                String memberId = memberFaultEvent.getMemberId();
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Member fault event: [member] %s ", memberId));
                }
                if (memberId == null || memberId.isEmpty()) {
                    log.error("Member id not found in received message");
                    return;
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleMemberFaultEvent(memberFaultEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberGradientOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberGradientOfLoadAverageEvent memberGradientOfLoadAverageEvent = (MemberGradientOfLoadAverageEvent) event;
                String memberId = memberGradientOfLoadAverageEvent.getMemberId();
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
                                                + "Therefore ignoring" + " the health stat", memberId));
                    }
                    return;
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                String clusterId = member.getClusterId();
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleMemberGradientOfLoadAverageEvent(memberGradientOfLoadAverageEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberGradientOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberGradientOfMemoryConsumptionEvent memberGradientOfMemoryConsumptionEvent = (MemberGradientOfMemoryConsumptionEvent) event;
                String memberId = memberGradientOfMemoryConsumptionEvent.getMemberId();
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
                                                + "Therefore ignoring" + " the health stat", memberId));
                    }
                    return;
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                String clusterId = member.getClusterId();
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleMemberGradientOfMemoryConsumptionEvent(memberGradientOfMemoryConsumptionEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberSecondDerivativeOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberSecondDerivativeOfLoadAverageEvent memberSecondDerivativeOfLoadAverageEvent = (MemberSecondDerivativeOfLoadAverageEvent) event;
                String memberId = memberSecondDerivativeOfLoadAverageEvent.getMemberId();
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
                                                + "Therefore ignoring" + " the health stat", memberId));
                    }
                    return;
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                String clusterId = member.getClusterId();
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleMemberSecondDerivativeOfLoadAverageEvent(memberSecondDerivativeOfLoadAverageEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new MemberSecondDerivativeOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

            }
        });

        healthStatEventReceiver.addEventListener(new SecondDerivativeOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                SecondDerivativeOfLoadAverageEvent secondDerivativeOfLoadAverageEvent = (SecondDerivativeOfLoadAverageEvent) event;
                String clusterId = secondDerivativeOfLoadAverageEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleSecondDerivativeOfLoadAverageEvent(secondDerivativeOfLoadAverageEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new SecondDerivativeOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                SecondDerivativeOfMemoryConsumptionEvent secondDerivativeOfMemoryConsumptionEvent = (SecondDerivativeOfMemoryConsumptionEvent) event;
                String clusterId = secondDerivativeOfMemoryConsumptionEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleSecondDerivativeOfMemoryConsumptionEvent(secondDerivativeOfMemoryConsumptionEvent);
            }
        });

        healthStatEventReceiver.addEventListener(new SecondDerivativeOfRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                SecondDerivativeOfRequestsInFlightEvent secondDerivativeOfRequestsInFlightEvent = (SecondDerivativeOfRequestsInFlightEvent) event;
                String clusterId = secondDerivativeOfRequestsInFlightEvent.getClusterId();
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractClusterMonitor monitor;
                monitor = asCtx.getClusterMonitor(clusterId);
                if (null == monitor) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("A cluster monitor is not found in autoscaler context "
                                                + "[cluster] %s", clusterId));
                    }
                    return;
                }
                monitor.handleSecondDerivativeOfRequestsInFlightEvent(secondDerivativeOfRequestsInFlightEvent);
            }
        });
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

    public void terminate() {
        this.terminated = true;
    }
}
