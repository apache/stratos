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

package org.apache.stratos.messaging.broker.publish;

import com.google.gson.Gson;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.connect.TopicPublisher;
import org.apache.stratos.messaging.broker.connect.TopicPublisherFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * A topic publisher for publishing messages to a message broker topic.
 * Messages will be published in JSON format.
 */
public class EventPublisher {

    private static final Log log = LogFactory.getLog(EventPublisher.class);

    private final String topicName;
    private final TopicPublisher topicPublisher;

    /**
     * @param topicName topic name of this publisher instance.
     */
    EventPublisher(String topicName) {
        this.topicName = topicName;
        String protocol = MessagingUtil.getMessagingProtocol();
        this.topicPublisher = TopicPublisherFactory.createTopicPublisher(protocol, topicName);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Topic publisher created: [protocol] %s [topic] %s", protocol, topicName));
        }
    }

    /**
     * Publish event.
     *
     * @param event event to be published
     */
    public void publish(Event event) {
        publish(event, true);
    }

    /**
     * Convert the object to its JSON representation and publish to the given topic.
     */

    public void publish(Object messageObj, boolean retry) {
        synchronized (EventPublisher.class) {
            Gson gson = new Gson();
            String message = gson.toJson(messageObj);

            topicPublisher.connect();
            topicPublisher.publish(message, retry);
            topicPublisher.disconnect();

        }
    }
}
