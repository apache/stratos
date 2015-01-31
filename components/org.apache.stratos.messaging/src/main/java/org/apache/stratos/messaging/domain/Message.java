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

package org.apache.stratos.messaging.domain;

import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Message definition.
 */
public class Message {
    private final String topicName;
    private final String text;
    private final String eventClassName;

    public Message(String topicName, String text) {
        this.topicName = topicName;
        this.text = text;
        this.eventClassName = MessagingUtil.getEventClassNameForTopic(topicName);
    }

    public String getTopicName() {
        return topicName;
    }

    public String getText() {
        return text;
    }

    public String getEventClassName() {
        return eventClassName;
    }
}
