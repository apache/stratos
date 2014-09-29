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
 */INSTANCE_STATUS_TOPIC

package org.apache.stratos.messaging.broker.publish;

import org.apache.stratos.messaging.event.Event;

/**
 * Defines logic for publishing events to a given topic in the message broker.
 * A message header will be used to send the event class name to be used by the
 * subscriber to identify the event.
 */
public class EventPublisher extends TopicPublisher {

	/**
	 * @param topicName
	 *            topic name of this publisher instance.
	 */
	EventPublisher(String topicName) {
		super(topicName);
	}

	/**
	 * 
	 * @param event
	 *            event to be published
	 */
	public void publish(Event event) {
		publish(event, true);
	}

	public void publish(Event event, boolean retry) {
		synchronized (EventPublisher.class) {

			super.publish(event, retry);
		}
	}
}
