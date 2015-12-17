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

package org.apache.stratos.messaging.message.receiver.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.event.initializer.CompleteTopologyRequestEvent;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * A thread for receiving topology information from message broker and
 * build topology in topology manager.
 */
public class TopologyEventReceiver extends StratosEventReceiver {

    private static final Log log = LogFactory.getLog(TopologyEventReceiver.class);

    private TopologyEventMessageDelegator messageDelegator;
    private TopologyEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile TopologyEventReceiver instance;

    private TopologyEventReceiver() {
        // TODO: make pool size configurable
        this.executorService = StratosThreadPool.getExecutorService("topology-event-receiver", 100);
        TopologyEventMessageQueue messageQueue = new TopologyEventMessageQueue();
        this.messageDelegator = new TopologyEventMessageDelegator(messageQueue);
        this.messageListener = new TopologyEventMessageListener(messageQueue);
        execute();
    }

    public static TopologyEventReceiver getInstance () {
        if (instance == null) {
            synchronized (TopologyEventReceiver.class) {
                if (instance == null) {
                    instance = new TopologyEventReceiver();
                }
            }
        }

        return instance;
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }

    private void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.TOPOLOGY_TOPIC.getTopicName(), messageListener);
            executorService.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("Topology event message receiver thread started");
            }

            // Start topology event message delegator thread
            executorService.execute(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("Topology event message delegator thread started");
            }

            initializeCompleteTopology();
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Topology receiver failed", e);
            }
        }
    }

    public void terminate() {
        eventSubscriber.terminate();
        messageDelegator.terminate();
    }

    public void initializeCompleteTopology() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!eventSubscriber.isSubscribed()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                }

                CompleteTopologyRequestEvent completeTopologyRequestEvent = new CompleteTopologyRequestEvent();
                String topic = MessagingUtil.getMessageTopicName(completeTopologyRequestEvent);
                EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
                eventPublisher.publish(completeTopologyRequestEvent);
            }
        });
    }
}
