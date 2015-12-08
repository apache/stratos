/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.receiver.cluster.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.subscribe.EventSubscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.message.receiver.StratosEventReceiver;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * A thread for receiving instance notifier information from message broker.
 */
public class ClusterStatusEventReceiver extends StratosEventReceiver {
    private static final Log log = LogFactory.getLog(ClusterStatusEventReceiver.class);
    private final ClusterStatusEventMessageDelegator messageDelegator;
    private final ClusterStatusEventMessageListener messageListener;
    private EventSubscriber eventSubscriber;
    private static volatile ClusterStatusEventReceiver instance;

    private ClusterStatusEventReceiver() {
        // TODO: make pool size configurable
        this.executor = StratosThreadPool.getExecutorService("messaging-event-receiver", 35, 150);
        ClusterStatusEventMessageQueue messageQueue = new ClusterStatusEventMessageQueue();
        this.messageDelegator = new ClusterStatusEventMessageDelegator(messageQueue);
        this.messageListener = new ClusterStatusEventMessageListener(messageQueue);
        execute();
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }

    public static ClusterStatusEventReceiver getInstance () {
        if (instance == null) {
            synchronized (ClusterStatusEventReceiver.class) {
                if (instance == null) {
                    instance = new ClusterStatusEventReceiver();
                }
            }
        }

        return instance;
    }

    private void execute() {
        try {
            // Start topic subscriber thread
            eventSubscriber = new EventSubscriber(MessagingUtil.Topics.CLUSTER_STATUS_TOPIC.getTopicName(), messageListener);
            executor.execute(eventSubscriber);

            if (log.isDebugEnabled()) {
                log.debug("InstanceNotifier event message receiver thread started");
            }

            // Start instance notifier event message delegator thread
            executor.execute(messageDelegator);
            if (log.isDebugEnabled()) {
                log.debug("InstanceNotifier event message delegator thread started");
            }


        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("InstanceNotifier receiver failed", e);
            }
        }
    }

    public boolean isSubscribed() {
        return ((eventSubscriber != null) && (eventSubscriber.isSubscribed()));
    }

//    public void terminate() {
//        eventSubscriber.terminate();
//        messageDelegator.terminate();
//        terminated = true;
//    }
//
//    public ExecutorService getExecutorService() {
//        return executor;
//    }
//
//    public void setExecutorService(ExecutorService executor) {
//        this.executor = executor;
//    }
}
