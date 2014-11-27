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
import org.apache.stratos.messaging.broker.subscribe.Subscriber;
import org.apache.stratos.messaging.listener.EventListener;
import org.apache.stratos.messaging.util.Util;



/**
 * A thread for receiving topology information from message broker and
 * build topology in topology manager.
 */
public class TopologyEventReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(TopologyEventReceiver.class);
    private TopologyEventMessageDelegator messageDelegator;
    private TopologyEventMessageListener messageListener;
    private Subscriber subscriber;
    private boolean terminated;

    public TopologyEventReceiver() {
        TopologyEventMessageQueue messageQueue = new TopologyEventMessageQueue();
        this.messageDelegator = new TopologyEventMessageDelegator(messageQueue);
        this.messageListener = new TopologyEventMessageListener(messageQueue);
    }

    public void addEventListener(EventListener eventListener) {
        messageDelegator.addEventListener(eventListener);
    }

	@Override
	public void run() {
		try {
			// Start topic subscriber thread
			subscriber = new Subscriber(Util.Topics.TOPOLOGY_TOPIC.getTopicName(), messageListener);
			// subscriber.setMessageListener(messageListener);

			Thread subscriberThread = new Thread(subscriber);
			subscriberThread.start();

            if (log.isDebugEnabled()) {
                log.debug("Topology event message receiver thread started");
            }

            // Start topology event message delegator thread
            Thread receiverThread = new Thread(messageDelegator);
            receiverThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology event message delegator thread started");
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
        subscriber.terminate();
        messageDelegator.terminate();
        terminated = true;
    }
}
