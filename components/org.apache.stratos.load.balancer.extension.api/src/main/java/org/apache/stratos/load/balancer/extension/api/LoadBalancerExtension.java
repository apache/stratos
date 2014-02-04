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

package org.apache.stratos.load.balancer.extension.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.statistics.notifier.LoadBalancerStatisticsNotifier;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.processor.topology.TopologyMessageProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyReceiver;

/**
 * Load balancer extension thread for executing load balancer life-cycle according to the topology updates
 * received from the message broker.
 */
public class LoadBalancerExtension implements Runnable {
    private static final Log log = LogFactory.getLog(LoadBalancerExtension.class);

    private LoadBalancer loadBalancer;
    private LoadBalancerStatisticsReader statsReader;
    private boolean loadBalancerStarted;
    private TopologyReceiver topologyReceiver;
    private LoadBalancerStatisticsNotifier statisticsNotifier;
    private boolean terminated;

    /**
     * Load balancer extension constructor.
     * @param loadBalancer Load balancer instance: Mandatory.
     * @param statsReader Statistics reader: If null statistics notifier thread will not be started.
     */
    public LoadBalancerExtension(LoadBalancer loadBalancer, LoadBalancerStatisticsReader statsReader) {
        this.loadBalancer = loadBalancer;
        this.statsReader = statsReader;
    }

    @Override
    public void run() {
        try {
            if(log.isInfoEnabled()) {
                log.info("Load balancer extension started");
            }

            // Start topology receiver thread
            topologyReceiver = new TopologyReceiver(createMessageDelegator());
            Thread topologyReceiverThread = new Thread(topologyReceiver);
            topologyReceiverThread.start();

            if(statsReader != null) {
                // Start stats notifier thread
                statisticsNotifier = new LoadBalancerStatisticsNotifier(statsReader);
                Thread statsNotifierThread = new Thread(statisticsNotifier);
                statsNotifierThread.start();
            }
            else {
                if(log.isWarnEnabled()) {
                    log.warn("Load balancer statistics reader not found");
                }
            }

            // Keep the thread live until terminated
            while (!terminated);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not start load balancer extension", e);
            }
        }
    }

    private TopologyEventMessageDelegator createMessageDelegator() {
        TopologyMessageProcessorChain processorChain = createEventProcessorChain();
        processorChain.addEventListener(new CompleteTopologyEventListener() {

            @Override
            protected void onEvent(Event event) {
                try {
                    // Configure load balancer
                    loadBalancer.configure(TopologyManager.getTopology());

                    // Start load balancer
                    loadBalancer.start();
                    loadBalancerStarted = true;
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Could not start load balancer", e);
                    }
                    terminate();
                }
            }
        });
        return new TopologyEventMessageDelegator(processorChain);
    }

    private TopologyMessageProcessorChain createEventProcessorChain() {
        TopologyMessageProcessorChain processorChain = new TopologyMessageProcessorChain();
        processorChain.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        processorChain.addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        processorChain.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        processorChain.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        processorChain.addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        return processorChain;
    }

    private void reloadConfiguration() {
        try {
            if (loadBalancerStarted) {
                loadBalancer.reload(TopologyManager.getTopology());
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not reload load balancer configuration", e);
            }
        }
    }

    public void terminate() {
        if(topologyReceiver != null) {
            topologyReceiver.terminate();
        }
        if(statisticsNotifier != null) {
            statisticsNotifier.terminate();
        }
        terminated = true;
    }
}
