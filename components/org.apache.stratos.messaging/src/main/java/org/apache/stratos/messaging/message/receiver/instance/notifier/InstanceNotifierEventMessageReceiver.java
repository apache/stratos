/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.message.receiver.instance.notifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;

/**
 * A thread for receiving instance notifier information from message broker.
 */
public class InstanceNotifierEventMessageReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(InstanceNotifierEventMessageReceiver.class);
    private InstanceNotifierEventMessageDelegator messageDelegator;
    private TopicSubscriber topicSubscriber;
    private boolean terminated;

    public InstanceNotifierEventMessageReceiver() {
        this.messageDelegator = new InstanceNotifierEventMessageDelegator();
    }

    public InstanceNotifierEventMessageReceiver(InstanceNotifierEventMessageDelegator messageDelegator) {
        this.messageDelegator = messageDelegator;
    }

    @Override
    public void run() {
        try {
            // Start topic subscriber thread
            topicSubscriber = new TopicSubscriber(Constants.INSTANCE_NOTIFIER_TOPIC);
            topicSubscriber.setMessageListener(new InstanceNotifierEventMessageListener());
            Thread subscriberThread = new Thread(topicSubscriber);
            subscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("InstanceNotifier event message receiver thread started");
            }

            // Start instance notifier event message delegator thread
            Thread receiverThread = new Thread(messageDelegator);
            receiverThread.start();
            if (log.isDebugEnabled()) {
                log.debug("InstanceNotifier event message delegator thread started");
            }

            // Keep the thread live until terminated
            while (!terminated);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("InstanceNotifier receiver failed", e);
            }
        }
    }

    public boolean isSubscribed() {
        return ((topicSubscriber != null) && (topicSubscriber.isSubscribed()));
    }

    public void terminate() {
        topicSubscriber.terminate();
        messageDelegator.terminate();
        terminated = true;
    }
}
