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

package org.apache.stratos.messaging.message.receiver.health.stat;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;

/**
 * A thread for receiving health stat information from message broker
 */
public class HealthStatReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(HealthStatReceiver.class);
    private HealthStatEventMessageDelegator messageDelegator;
    private TopicSubscriber topicSubscriber;
    private boolean terminated;

    public HealthStatReceiver() {
        this.messageDelegator = new HealthStatEventMessageDelegator();
    }

    public HealthStatReceiver(HealthStatEventMessageDelegator messageDelegator) {
        this.messageDelegator = messageDelegator;
    }

    @Override
    public void run() {
        try {
            // Start topic subscriber thread
            topicSubscriber = new TopicSubscriber(Constants.HEALTH_STAT_TOPIC);
            topicSubscriber.setMessageListener(new HealthStatEventMessageListener());
            Thread subscriberThread = new Thread(topicSubscriber);
            subscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Health stst event message receiver thread started");
            }

            // Start health stat event message delegator thread
            Thread receiverThread = new Thread(messageDelegator);
            receiverThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Health stst event message delegator thread started");
            }

            // Keep the thread live until terminated
            while (!terminated) {
            	try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Topology receiver failed", e);
            }
        }
    }

    public void terminate() {
        topicSubscriber.terminate();
        messageDelegator.terminate();
        terminated = true;
    }
}
