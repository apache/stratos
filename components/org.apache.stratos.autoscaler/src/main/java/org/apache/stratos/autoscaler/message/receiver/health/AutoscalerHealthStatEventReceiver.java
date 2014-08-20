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
package org.apache.stratos.autoscaler.message.receiver.health;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.AutoscalerContext;
import org.apache.stratos.autoscaler.MemberStatsContext;
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.PartitionContext;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.monitor.AbstractMonitor;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.listener.health.stat.*;
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
                AverageLoadAverageEvent e = (AverageLoadAverageEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Avg load avg event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setAverageLoadAverage(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }

            }

        });
        healthStatEventReceiver.addEventListener(new AverageMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                AverageMemoryConsumptionEvent e = (AverageMemoryConsumptionEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Avg Memory Consumption event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }

                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setAverageMemoryConsumption(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
        healthStatEventReceiver.addEventListener(new AverageRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                AverageRequestsInFlightEvent e = (AverageRequestsInFlightEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();
                Float floatValue = e.getValue();


                if (log.isDebugEnabled()) {
                    log.debug(String.format("Average Rif event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setAverageRequestsInFlight(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
        healthStatEventReceiver.addEventListener(new GradientOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                GradientOfLoadAverageEvent e = (GradientOfLoadAverageEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Grad of load avg event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setLoadAverageGradient(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
        healthStatEventReceiver.addEventListener(new GradientOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                GradientOfMemoryConsumptionEvent e = (GradientOfMemoryConsumptionEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Grad of Memory Consumption event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                };
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setMemoryConsumptionGradient(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
        healthStatEventReceiver.addEventListener(new GradientOfRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                GradientOfRequestsInFlightEvent e = (GradientOfRequestsInFlightEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Gradient of Rif event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setRequestsInFlightGradient(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
        healthStatEventReceiver.addEventListener(new MemberAverageLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberAverageLoadAverageEvent e = (MemberAverageLoadAverageEvent) event;
                LoadAverage loadAverage = findLoadAverage(e.getMemberId());
                if(loadAverage != null) {

                    Float floatValue = e.getValue();
                    loadAverage.setAverage(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member avg of load avg event: [member] %s [value] %s", e.getMemberId()
                                , floatValue));
                    }
                }

            }

        });
        healthStatEventReceiver.addEventListener(new MemberAverageMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberAverageMemoryConsumptionEvent e = (MemberAverageMemoryConsumptionEvent) event;
                MemoryConsumption memoryConsumption = findMemoryConsumption(e.getMemberId());
                if(memoryConsumption != null) {

                    Float floatValue = e.getValue();
                    memoryConsumption.setAverage(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member avg Memory Consumption event: [member] %s [value] %s", e.getMemberId(),
                                floatValue));
                    }
                }

            }

        });
        healthStatEventReceiver.addEventListener(new MemberFaultEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberFaultEvent e = (MemberFaultEvent) event;
                String clusterId = e.getClusterId();
                String memberId = e.getMemberId();

                if (memberId == null || memberId.isEmpty()) {
                    if(log.isErrorEnabled()) {
                        log.error("Member id not found in received message");
                    }
                } else {

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member fault event: [member] %s ", e.getMemberId()));
                    }
                    handleMemberFaultEvent(clusterId, memberId);
                }
            }

        });
        healthStatEventReceiver.addEventListener(new MemberGradientOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberGradientOfLoadAverageEvent e = (MemberGradientOfLoadAverageEvent) event;
                LoadAverage loadAverage = findLoadAverage(e.getMemberId());
                if(loadAverage != null) {

                    Float floatValue = e.getValue();
                    loadAverage.setGradient(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member grad of load avg event: [member] %s [value] %s", e.getMemberId(),
                                floatValue));
                    }
                }

            }

        });
        healthStatEventReceiver.addEventListener(new MemberGradientOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberGradientOfMemoryConsumptionEvent e = (MemberGradientOfMemoryConsumptionEvent) event;
                MemoryConsumption memoryConsumption = findMemoryConsumption(e.getMemberId());
                if(memoryConsumption != null) {

                    Float floatValue = e.getValue();
                    memoryConsumption.setGradient(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member grad of Memory Consumption event: [member] %s [value] %s", e.getMemberId(),
                                floatValue));
                    }
                }

            }

        });
        healthStatEventReceiver.addEventListener(new MemberSecondDerivativeOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberSecondDerivativeOfLoadAverageEvent e = (MemberSecondDerivativeOfLoadAverageEvent) event;
                LoadAverage loadAverage = findLoadAverage(e.getMemberId());
                if(loadAverage != null) {

                    Float floatValue = e.getValue();
                    loadAverage.setSecondDerivative(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member Second Derivation of load avg event: [member] %s [value] %s", e.getMemberId()
                                , floatValue));
                    }
                }
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

                SecondDerivativeOfLoadAverageEvent e = (SecondDerivativeOfLoadAverageEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Second Derivation of load avg event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setLoadAverageSecondDerivative(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
        healthStatEventReceiver.addEventListener(new SecondDerivativeOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                SecondDerivativeOfMemoryConsumptionEvent e = (SecondDerivativeOfMemoryConsumptionEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Second Derivation of Memory Consumption event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setMemoryConsumptionSecondDerivative(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }

            }

        });
        healthStatEventReceiver.addEventListener(new SecondDerivativeOfRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                SecondDerivativeOfRequestsInFlightEvent e = (SecondDerivativeOfRequestsInFlightEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();
                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Second derivative of Rif event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AutoscalerContext asCtx = AutoscalerContext.getInstance();
                AbstractMonitor monitor;

                if(asCtx.monitorExist(clusterId)){
                    monitor = asCtx.getMonitor(clusterId);
                }else if(asCtx.lbMonitorExist(clusterId)){
                    monitor = asCtx.getLBMonitor(clusterId);
                }else{
                    if(log.isDebugEnabled()){
                        log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                    }
                    return;
                }
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setRequestsInFlightSecondDerivative(floatValue);
                    } else {
                        if(log.isDebugEnabled()) {
                           log.debug(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                }
            }

        });
    }


    private LoadAverage findLoadAverage(String memberId) {
//        String memberId = event.getProperties().get("member_id");
        Member member = findMember(memberId);
        
        if(null == member){
        	if(log.isDebugEnabled()) {
                log.debug(String.format("Member not found in the Topology: [member] %s", memberId));
            }
        	return null;
        }
        String clusterId = member.getClusterId();

        AutoscalerContext asCtx = AutoscalerContext.getInstance();
        AbstractMonitor monitor;

        if(asCtx.monitorExist(clusterId)){
            monitor = asCtx.getMonitor(clusterId);
        }else if(asCtx.lbMonitorExist(clusterId)){
            monitor = asCtx.getLBMonitor(clusterId);
        }else{
            if(log.isDebugEnabled()){
                log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
            }
            return null;
        }
        String networkPartitionId = findNetworkPartitionId(memberId);
        MemberStatsContext memberStatsContext = monitor.getNetworkPartitionCtxt(networkPartitionId)
                        .getPartitionCtxt(member.getPartitionId())
                        .getMemberStatsContext(memberId);
        if(null == memberStatsContext){
            if(log.isDebugEnabled()) {
               log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return null;
        }
        else if(!member.isActive()){
            if(log.isDebugEnabled()){
                log.debug(String.format("Member activated event has not received for the member %s. Therefore ignoring" +
                        " the health stat", memberId));
            }
            return null;
        }

        LoadAverage loadAverage = memberStatsContext.getLoadAverage();
        return loadAverage;
    }

    private MemoryConsumption findMemoryConsumption(String memberId) {
//        String memberId = event.getProperties().get("member_id");
        Member member = findMember(memberId);
        
        if(null == member){
        	if(log.isDebugEnabled()) {
                log.debug(String.format("Member not found in the Topology : [member] %s", memberId));
            }
        	return null;
        }
        AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(member.getClusterId());
        if(null == monitor){

            monitor = AutoscalerContext.getInstance().getLBMonitor(member.getClusterId());
            if(null == monitor){
                if(log.isDebugEnabled()) {
                   log.debug(String.format("Cluster monitor is not available for : [member] %s", memberId));
                }
            }
            return null;
        }

        String networkPartitionId = findNetworkPartitionId(memberId);
        MemberStatsContext memberStatsContext = monitor.getNetworkPartitionCtxt(networkPartitionId)
                        .getPartitionCtxt(member.getPartitionId())
                        .getMemberStatsContext(memberId);
        if(null == memberStatsContext){
            if(log.isDebugEnabled()) {
               log.debug(String.format("Member context is not available for : [member] %s", memberId));
            }
            return null;
        }else if(!member.isActive()){
            if(log.isDebugEnabled()){
                log.debug(String.format("Member activated event has not received for the member %s. Therefore ignoring" +
                        " the health stat", memberId));
            }
            return null;
        }
        MemoryConsumption memoryConsumption = memberStatsContext.getMemoryConsumption();

        return memoryConsumption;
    }

    private String findNetworkPartitionId(String memberId) {
        for(Service service: TopologyManager.getTopology().getServices()){
            for(Cluster cluster: service.getClusters()){
                if(cluster.memberExists(memberId)){
                    return cluster.getMember(memberId).getNetworkPartitionId();
                }
            }
        }
        return null;
    }

    private Member findMember(String memberId) {
        try {
            TopologyManager.acquireReadLock();
            for(Service service : TopologyManager.getTopology().getServices()) {
                for(Cluster cluster : service.getClusters()) {
                    if(cluster.memberExists(memberId)) {
                        return cluster.getMember(memberId);
                    }
                }
            }
            return null;
        }
        finally {
            TopologyManager.releaseReadLock();
        }
    }

    private void handleMemberFaultEvent(String clusterId, String memberId) {
        try {
        	AutoscalerContext asCtx = AutoscalerContext.getInstance();
        	AbstractMonitor monitor;
        	
        	if(asCtx.monitorExist(clusterId)){
        		monitor = asCtx.getMonitor(clusterId);
        	}else if(asCtx.lbMonitorExist(clusterId)){
        		monitor = asCtx.getLBMonitor(clusterId);
        	}else{
                if(log.isDebugEnabled()){
                    log.debug(String.format("A cluster monitor is not found in autoscaler context [cluster] %s", clusterId));
                }
                return;
        	}
        	
        	NetworkPartitionContext nwPartitionCtxt;
            try{
            	TopologyManager.acquireReadLock();
            	Member member = findMember(memberId);
            	
            	if(null == member){
            		return;
            	}
                if(!member.isActive()){
                    if(log.isDebugEnabled()){
                        log.debug(String.format("Member activated event has not received for the member %s. Therefore ignoring" +
                                " the member fault health stat", memberId));
                    }
                    return;
                }
	            
	            nwPartitionCtxt = monitor.getNetworkPartitionCtxt(member);
	            
            }finally{
            	TopologyManager.releaseReadLock();
            }
            // start a new member in the same Partition
            String partitionId = monitor.getPartitionOfMember(memberId);
            PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);

            if(!partitionCtxt.activeMemberExist(memberId)){
                if(log.isDebugEnabled()){
                    log.debug(String.format("Could not find the active member in partition context, [member] %s ", memberId));
                }
                return;
            }
            // terminate the faulty member
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            ccClient.terminate(memberId);

            // remove from active member list
            partitionCtxt.removeActiveMemberById(memberId);

            if (log.isInfoEnabled()) {
                log.info(String.format("Faulty member is terminated and removed from the active members list: [member] %s [partition] %s [cluster] %s ",
                                       memberId, partitionId, clusterId));
            }


        } catch (TerminationException e) {
            log.error(e);
        }
    }

    public void terminate(){
    	this.terminated = true;
    }
}
