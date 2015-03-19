/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.broker.publish;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Event publisher instance pool will make sure that only one publisher
 * instance is created for each topic.
 */
public class EventPublisherPool {
    private static final Log log = LogFactory.getLog(EventPublisherPool.class);
    private static Map<String, EventPublisher> topicNameEventPublisherMap = new HashMap<String, EventPublisher>();

    public static EventPublisher getPublisher(String topicName) {
        synchronized (EventPublisherPool.class) {
            if (topicNameEventPublisherMap.containsKey(topicName)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Event publisher fetched from pool: [topic] %s", topicName));
                }
                return topicNameEventPublisherMap.get(topicName);
            }
            EventPublisher eventPublisher = new EventPublisher(topicName);
            topicNameEventPublisherMap.put(topicName, eventPublisher);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Event publisher instance created: [topic] %s", topicName));
            }
            return eventPublisher;
        }
    }

    public static void close(String topicName) {
        synchronized (EventPublisherPool.class) {
            if (topicNameEventPublisherMap.containsKey(topicName)) {
                topicNameEventPublisherMap.remove(topicName);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Event publisher closed and removed from pool: [topic] %s", topicName));
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Event publisher instance not found in pool: [topic] %s", topicName));
                }
            }
        }
    }
}
