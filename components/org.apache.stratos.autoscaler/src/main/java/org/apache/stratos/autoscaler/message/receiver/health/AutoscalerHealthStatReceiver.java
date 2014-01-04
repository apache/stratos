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
import org.apache.stratos.autoscaler.exception.SpawningException;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.monitor.AbstractMonitor;
import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;
import org.apache.stratos.autoscaler.rule.AutoscalerRuleEvaluator;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.listener.health.stat.*;
import org.apache.stratos.messaging.message.processor.health.stat.HealthStatMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.health.stat.HealthStatEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.health.stat.HealthStatReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;


/**
 * A thread for processing topology messages and updating the topology data structure.
 */
public class AutoscalerHealthStatReceiver implements Runnable {

    private static final Log log = LogFactory.getLog(AutoscalerHealthStatReceiver.class);
    private boolean terminated = false;

    private HealthStatReceiver healthStatReceiver;
//    @Override
//    public void run() {
//        if(log.isInfoEnabled()) {
//            log.info("Health event message delegator started");
//        }
//
//        if(log.isDebugEnabled()) {
//            log.debug("Waiting for topology to be initialized");
//        }
//        while(!TopologyManager.getTopology().isInitialized());
//
//        while (!terminate) {
//            try {
//                TextMessage message = HealthStatEventMessageQueue.getInstance().take();
//
//                String messageText = message.getText();
//                if (log.isDebugEnabled()) {
//                    log.debug("Health event message received: [message] " + messageText);
//                }
//                Event event = jsonToEvent(messageText);
//                String eventName = event.getEventName();
//
//                if (log.isInfoEnabled()) {
//                    log.info(String.format("Received event: [event-name] %s", eventName));
//                }
//
//                if (Constants.AVERAGE_REQUESTS_IN_FLIGHT.equals(eventName)) {
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setAverageRequestsInFlight(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//
//                } else if (Constants.GRADIENT_OF_REQUESTS_IN_FLIGHT.equals(eventName)) {
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setRequestsInFlightGradient(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//
//                } else if (Constants.SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT.equals(eventName)) {
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setRequestsInFlightSecondDerivative(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                } else if (Constants.MEMBER_FAULT_EVENT_NAME.equals(eventName)) {
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String memberId = event.getProperties().get("member_id");
//
//                    if (memberId == null || memberId.isEmpty()) {
//                        if(log.isErrorEnabled()) {
//                            log.error("Member id not found in received message");
//                        }
//                    } else {
//                        handleMemberFaultEvent(clusterId, memberId);
//                    }
//                } else if(Constants.MEMBER_AVERAGE_LOAD_AVERAGE.equals(eventName)) {
//                    LoadAverage loadAverage = findLoadAverage(event);
//                    if(loadAverage != null) {
//                        String value = event.getProperties().get("value");
//                        Float floatValue = Float.parseFloat(value);
//                        loadAverage.setAverage(floatValue);
//
//                        if (log.isDebugEnabled()) {
//                            log.debug(String.format("%s event: [member] %s [value] %s", event, event.getProperties().get("member_id"), value));
//                        }
//                    }
//                } else if(Constants.MEMBER_SECOND_DERIVATIVE_OF_LOAD_AVERAGE.equals(eventName)) {
//                    LoadAverage loadAverage = findLoadAverage(event);
//                    if(loadAverage != null) {
//                        String value = event.getProperties().get("value");
//                        Float floatValue = Float.parseFloat(value);
//                        loadAverage.setSecondDerivative(floatValue);
//
//                        if (log.isDebugEnabled()) {
//                            log.debug(String.format("%s event: [member] %s [value] %s", event, event.getProperties().get("member_id"), value));
//                        }
//                    }
//                } else if(Constants.MEMBER_GRADIENT_LOAD_AVERAGE.equals(eventName)) {
//                    LoadAverage loadAverage = findLoadAverage(event);
//                    if(loadAverage != null) {
//                        String value = event.getProperties().get("value");
//                        Float floatValue = Float.parseFloat(value);
//                        loadAverage.setGradient(floatValue);
//
//                        if (log.isDebugEnabled()) {
//                            log.debug(String.format("%s event: [member] %s [value] %s", event, event.getProperties().get("member_id"), value));
//                        }
//                    }
//                } else if(Constants.MEMBER_AVERAGE_MEMORY_CONSUMPTION.equals(eventName)) {
//                    MemoryConsumption memoryConsumption = findMemoryConsumption(event);
//                    if(memoryConsumption != null) {
//                        String value = event.getProperties().get("value");
//                        Float floatValue = Float.parseFloat(value);
//                        memoryConsumption.setAverage(floatValue);
//
//                        if (log.isDebugEnabled()) {
//                            log.debug(String.format("%s event: [member] %s [value] %s", event, event.getProperties().get("member_id"), value));
//                        }
//                    }
//                } else if(Constants.MEMBER_SECOND_DERIVATIVE_OF_MEMORY_CONSUMPTION.equals(eventName)) {
//                    MemoryConsumption memoryConsumption = findMemoryConsumption(event);
//                    if(memoryConsumption != null) {
//                        String value = event.getProperties().get("value");
//                        Float floatValue = Float.parseFloat(value);
//                        memoryConsumption.setSecondDerivative(floatValue);
//
//                        if (log.isDebugEnabled()) {
//                            log.debug(String.format("%s event: [member] %s [value] %s", event, event.getProperties().get("member_id"), value));
//                        }
//                    }
//                } else if(Constants.MEMBER_GRADIENT_MEMORY_CONSUMPTION.equals(eventName)) {
//                    MemoryConsumption memoryConsumption = findMemoryConsumption(event);
//                    if(memoryConsumption != null) {
//                        String value = event.getProperties().get("value");
//                        Float floatValue = Float.parseFloat(value);
//                        memoryConsumption.setGradient(floatValue);
//
//                        if (log.isDebugEnabled()) {
//                            log.debug(String.format("%s event: [member] %s [value] %s",event, event.getProperties().get("member_id"), value));
//                        }
//                    }
//
//                } else if(Constants.AVERAGE_LOAD_AVERAGE.equals(eventName)) {
//
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setAverageLoadAverage(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                } else if(Constants.SECOND_DERIVATIVE_OF_LOAD_AVERAGE.equals(eventName)) {
//
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setLoadAverageSecondDerivative(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                } else if(Constants.GRADIENT_LOAD_AVERAGE.equals(eventName)) {
//
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setLoadAverageGradient(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                } else if(Constants.AVERAGE_MEMORY_CONSUMPTION.equals(eventName)) {
//
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setAverageMemoryConsumption(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                } else if(Constants.SECOND_DERIVATIVE_OF_MEMORY_CONSUMPTION.equals(eventName)) {
//
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setMemoryConsumptionSecondDerivative(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                } else if(Constants.GRADIENT_MEMORY_CONSUMPTION.equals(eventName)) {
//
//                    String clusterId = event.getProperties().get("cluster_id");
//                    String networkPartitionId = event.getProperties().get("network_partition_id");
//                    String value = event.getProperties().get("value");
//                    Float floatValue = Float.parseFloat(value);
//
//                    if (log.isDebugEnabled()) {
//                        log.debug(String.format("%s event: [cluster] %s [network-partition] %s [value] %s", eventName,
//                                clusterId, networkPartitionId, value));
//                    }
//                    AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
//                    if(null != monitor){
//                        NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
//                        if(null != networkPartitionContext){
//                            networkPartitionContext.setMemoryConsumptionGradient(floatValue);
//                        } else {
//                            if(log.isErrorEnabled()) {
//                               log.error(String.format("Network partition context is not available for :" +
//                                       " [network partition] %s", networkPartitionId));
//                            }
//                        }
//                    } else {
//
//                        if(log.isErrorEnabled()) {
//                           log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                log.error("Failed to retrieve the health stat event message.", e);
//            }
//        }
//        log.warn("Health event Message delegater is terminated");
//    }
//


    @Override
    public void run() {
        //FIXME this activated before autoscaler deployer activated.
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ignore) {
        }
        Thread thread = new Thread(healthStatReceiver);
        thread.start();
        if(log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread started");
        }

        // Keep the thread live until terminated
        while (!terminated);
        if(log.isInfoEnabled()) {
            log.info("Autoscaler topology receiver thread terminated");
        }
    }

    private HealthStatEventMessageDelegator createMessageDelegator() {
        HealthStatMessageProcessorChain processorChain = createEventProcessorChain();
        return new HealthStatEventMessageDelegator(processorChain);
    }

    private HealthStatMessageProcessorChain createEventProcessorChain() {
        // Listen to health stat events that affect clusters
        HealthStatMessageProcessorChain processorChain = new HealthStatMessageProcessorChain();
        processorChain.addEventListener(new AverageLoadAverageEventListener() {
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
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setAverageLoadAverage(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }

            }

        });
        processorChain.addEventListener(new AverageMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                AverageMemoryConsumptionEvent e = (AverageMemoryConsumptionEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Avg MC event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setAverageMemoryConsumption(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });
        processorChain.addEventListener(new AverageRequestsInFlightEventListener() {
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

                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setAverageRequestsInFlight(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });
        processorChain.addEventListener(new GradientOfLoadAverageEventListener() {
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
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setLoadAverageGradient(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });
        processorChain.addEventListener(new GradientOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                GradientOfMemoryConsumptionEvent e = (GradientOfMemoryConsumptionEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Grad of MC event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setMemoryConsumptionGradient(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });
        processorChain.addEventListener(new GradientOfRequestsInFlightEventListener() {
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
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setRequestsInFlightGradient(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });
        processorChain.addEventListener(new MemberAverageLoadAverageEventListener() {
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
        processorChain.addEventListener(new MemberAverageMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberAverageMemoryConsumptionEvent e = (MemberAverageMemoryConsumptionEvent) event;
                MemoryConsumption memoryConsumption = findMemoryConsumption(e.getMemberId());
                if(memoryConsumption != null) {

                    Float floatValue = e.getValue();
                    memoryConsumption.setAverage(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member avg MC event: [member] %s [value] %s", e.getMemberId(),
                                floatValue));
                    }
                }

            }

        });
        processorChain.addEventListener(new MemberFaultEventListener() {
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
                    handleMemberFaultEvent(clusterId, memberId);
                }
            }

        });
        processorChain.addEventListener(new MemberGradientOfLoadAverageEventListener() {
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
        processorChain.addEventListener(new MemberGradientOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberGradientOfMemoryConsumptionEvent e = (MemberGradientOfMemoryConsumptionEvent) event;
                MemoryConsumption memoryConsumption = findMemoryConsumption(e.getMemberId());
                if(memoryConsumption != null) {

                    Float floatValue = e.getValue();
                    memoryConsumption.setGradient(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Meber grad of MC event: [member] %s [value] %s", e.getMemberId(),
                                floatValue));
                    }
                }

            }

        });
        processorChain.addEventListener(new MemberSecondDerivativeOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                MemberSecondDerivativeOfLoadAverageEvent e = (MemberSecondDerivativeOfLoadAverageEvent) event;
                LoadAverage loadAverage = findLoadAverage(e.getMemberId());
                if(loadAverage != null) {

                    Float floatValue = e.getValue();
                    loadAverage.setSecondDerivative(floatValue);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Member SD of load avg event: [member] %s [value] %s", e.getMemberId()
                                , floatValue));
                    }
                }
            }

        });
        processorChain.addEventListener(new MemberSecondDerivativeOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
            }

        });
        processorChain.addEventListener(new SecondDerivativeOfLoadAverageEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                SecondDerivativeOfLoadAverageEvent e = (SecondDerivativeOfLoadAverageEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("SD of load avg event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setLoadAverageSecondDerivative(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });
        processorChain.addEventListener(new SecondDerivativeOfMemoryConsumptionEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {

                SecondDerivativeOfMemoryConsumptionEvent e = (SecondDerivativeOfMemoryConsumptionEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();

                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("SD of MC event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setMemoryConsumptionSecondDerivative(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }

            }

        });
        processorChain.addEventListener(new SecondDerivativeOfRequestsInFlightEventListener() {
            @Override
            protected void onEvent(org.apache.stratos.messaging.event.Event event) {
                SecondDerivativeOfRequestsInFlightEvent e = (SecondDerivativeOfRequestsInFlightEvent) event;
                String clusterId = e.getClusterId();
                String networkPartitionId = e.getNetworkPartitionId();
                Float floatValue = e.getValue();

                if (log.isDebugEnabled()) {
                    log.debug(String.format("Second dericvative of Rif event: [cluster] %s [network-partition] %s [value] %s",
                            clusterId, networkPartitionId, floatValue));
                }
                AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(clusterId);
                if(null != monitor){
                    NetworkPartitionContext networkPartitionContext = monitor.getNetworkPartitionCtxt(networkPartitionId);
                    if(null != networkPartitionContext){
                        networkPartitionContext.setRequestsInFlightSecondDerivative(floatValue);
                    } else {
                        if(log.isErrorEnabled()) {
                           log.error(String.format("Network partition context is not available for :" +
                                   " [network partition] %s", networkPartitionId));
                        }
                    }
                } else {

                    if(log.isErrorEnabled()) {
                       log.error(String.format("Cluster monitor is not available for : [cluster] %s", clusterId));
                    }
                }
            }

        });

        return processorChain;
    }


    private LoadAverage findLoadAverage(String memberId) {
//        String memberId = event.getProperties().get("member_id");
        Member member = findMember(memberId);
        
        if(null == member){
        	if(log.isErrorEnabled()) {
                log.error(String.format("Member not found: [member] %s", memberId));
            }
        	return null;
        }
        AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(member.getClusterId());
        if(null == monitor){
            if(log.isErrorEnabled()) {
               log.error(String.format("Cluster monitor is not available for : [member] %s", memberId));
            }
            return null;
        }
        String networkPartitionId = findNetworkPartitionId(memberId);
        MemberStatsContext memberStatsContext = monitor.getNetworkPartitionCtxt(networkPartitionId)
                        .getPartitionCtxt(member.getPartitionId())
                        .getMemberStatsContext(memberId);
        if(null == memberStatsContext){
            if(log.isErrorEnabled()) {
               log.error(String.format("Member context is not available for : [member] %s", memberId));
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
        	if(log.isErrorEnabled()) {
                log.error(String.format("Member not found: [member] %s", memberId));
            }
        	return null;
        }
        
        AbstractMonitor monitor = AutoscalerContext.getInstance().getMonitor(member.getClusterId());
        if(null == monitor){
            if(log.isErrorEnabled()) {
               log.error(String.format("Cluster monitor is not available for : [member] %s", memberId));
            }
            return null;
        }
        
        
        String networkPartitionId = findNetworkPartitionId(memberId);
        MemberStatsContext memberStatsContext = monitor.getNetworkPartitionCtxt(networkPartitionId)
                        .getPartitionCtxt(member.getPartitionId())
                        .getMemberStatsContext(memberId);
        if(null == memberStatsContext){
            if(log.isErrorEnabled()) {
               log.error(String.format("Member context is not available for : [member] %s", memberId));
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
        	
        	if(asCtx.moniterExist(clusterId)){
        		monitor = asCtx.getMonitor(clusterId);
        	}else if(asCtx.lbMoniterExist(clusterId)){
        		monitor = asCtx.getLBMonitor(clusterId);
        	}else{
        		String errMsg = "A monitor is not found for this custer";
        		log.error(errMsg);
        		throw new RuntimeException(errMsg);
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
            // terminate the faulty member
            CloudControllerClient ccClient = CloudControllerClient.getInstance();
            ccClient.terminate(memberId);

            // start a new member in the same Partition
            String partitionId = monitor.getPartitionOfMember(memberId);
            Partition partition = monitor.getDeploymentPolicy().getPartitionById(partitionId);
            PartitionContext partitionCtxt = nwPartitionCtxt.getPartitionCtxt(partitionId);
            
            String lbClusterId = AutoscalerRuleEvaluator.getLbClusterId(partitionCtxt, nwPartitionCtxt);
            ccClient.spawnAnInstance(partition, clusterId, lbClusterId, nwPartitionCtxt.getId());
            if (log.isInfoEnabled()) {
                log.info(String.format("Instance spawned for fault member: [partition] %s [cluster] %s [lb cluster] %s ", 
                                       partitionId, clusterId, lbClusterId));
            }                       

        } catch (TerminationException e) {
            log.error(e);
        } catch (SpawningException e) {
            log.error(e);
        }
    }

    public void terminate(){
    	this.terminated = true;
    }
}
