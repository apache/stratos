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

package org.apache.stratos.messaging.broker.connect;

import org.apache.stratos.messaging.broker.subscribe.MessageListener;

/**
 * Message broker topic connector interface.
 */
public interface TopicConnector {
    /**
     * Return service URI
     * @return
     */
    public String getServerURI();

    /**
     * Establish a connection to the message broker.
     */
    public void connect();

    /**
     * Disconnect from the message broker.
     */
    public void disconnect();

    /**
     * Publish a message to a topic in the message broker.
     * @param topicName
     * @param message
     */
    public void publish(String topicName, String message);

    /**
     * Subscribe to a topic with a message listener.
     * @param topicName
     * @param messageListener
     */
    public void subscribe(String topicName, MessageListener messageListener);
}
