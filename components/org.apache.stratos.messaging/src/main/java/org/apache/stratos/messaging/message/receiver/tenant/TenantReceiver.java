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

package org.apache.stratos.messaging.message.receiver.tenant;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;

/**
 * A thread for receiving tenant information from message broker and
 * build tenant information in tenant manager.
 */
public class TenantReceiver implements Runnable {
    private static final Log log = LogFactory.getLog(TenantReceiver.class);
    private TenantEventMessageDelegator messageDelegator;
    private TopicSubscriber topicSubscriber;
    private boolean terminated;

    public TenantReceiver() {
        this.messageDelegator = new TenantEventMessageDelegator();
    }

    public TenantReceiver(TenantEventMessageDelegator messageDelegator) {
        this.messageDelegator = messageDelegator;
    }

    @Override
    public void run() {
        try {
            // Start topic subscriber thread
            topicSubscriber = new TopicSubscriber(Constants.TENANT_TOPIC);
            topicSubscriber.setMessageListener(new TenantEventMessageListener());
            Thread subscriberThread = new Thread(topicSubscriber);
            subscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Tenant event message receiver thread started");
            }

            // Start tenant event message delegator thread
            Thread receiverThread = new Thread(messageDelegator);
            receiverThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Tenant event message delegator thread started");
            }

            // Keep the thread live until terminated
            while (!terminated);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Tenant receiver failed", e);
            }
        }
    }

    public void terminate() {
        topicSubscriber.terminate();
        messageDelegator.terminate();
        terminated = true;
    }
}
