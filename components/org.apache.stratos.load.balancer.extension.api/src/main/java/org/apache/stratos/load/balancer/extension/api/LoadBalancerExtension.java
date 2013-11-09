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
import org.apache.stratos.load.balancer.common.topology.TopologyReceiver;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.processor.MessageProcessorChain;
import org.apache.stratos.messaging.message.processor.topology.TopologyEventProcessorChain;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventMessageDelegator;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

/**
 * Load balancer extension thread for executing load balancer life-cycle according to the topology updates
 * received from the message broker.
 */
public class LoadBalancerExtension implements Runnable {
    private static final Log log = LogFactory.getLog(LoadBalancerExtension.class);

    private LoadBalancer loadBalancer;

    public LoadBalancerExtension(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
    }

    @Override
    public void run() {
        try {
            // Start topology receiver
            TopologyReceiver topologyReceiver = new TopologyReceiver(createMessageDelegator());
            Thread thread = new Thread(topologyReceiver);
            thread.start();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e);
            }
            loadBalancer.stop();
        }
    }

    private TopologyEventMessageDelegator createMessageDelegator() {
        TopologyEventProcessorChain processorChain = createEventProcessorChain();
        TopologyEventMessageDelegator messageDelegator = new TopologyEventMessageDelegator(processorChain);
        messageDelegator.addCompleteTopologyEventListener(new CompleteTopologyEventListener() {
            @Override
            protected void onEvent(Event event) {
                // Configure load balancer
                loadBalancer.configure(TopologyManager.getTopology());

                // Start load balancer
                loadBalancer.start();
            }
        });
        return messageDelegator;
    }

    private TopologyEventProcessorChain createEventProcessorChain() {
        TopologyEventProcessorChain processorChain = new TopologyEventProcessorChain();
        processorChain.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                loadBalancer.reload(TopologyManager.getTopology());
            }
        });
        processorChain.addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {
                loadBalancer.reload(TopologyManager.getTopology());
            }
        });
        processorChain.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                loadBalancer.reload(TopologyManager.getTopology());
            }
        });
        processorChain.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                loadBalancer.reload(TopologyManager.getTopology());
            }
        });
        processorChain.addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                loadBalancer.reload(TopologyManager.getTopology());
            }
        });
        return processorChain;
    }
}
