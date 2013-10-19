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

package org.apache.stratos.autoscaler.internal;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.event.topology.ClusterCreatedEvent;
import org.apache.stratos.messaging.event.topology.ClusterRemovedEvent;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.event.topology.MemberActivatedEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.event.topology.MemberSuspendedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.event.topology.ServiceCreatedEvent;
import org.apache.stratos.messaging.event.topology.ServiceRemovedEvent;
import org.apache.stratos.messaging.message.EventMessageHeader;
import org.apache.stratos.messaging.message.MessageProcessor;
import org.apache.stratos.messaging.message.TopologyEventMessage;
import org.apache.stratos.messaging.util.Constants;

/**
 * A client uses a TopicSubscriber object to receive messages that have been published to topology topic
 *
 */
public class EventSubscriber extends MessageProcessor implements MessageListener {
	
	private static final Log log = LogFactory.getLog(EventSubscriber.class);
	
	private TopicSubscriber topicSubscriber = null;
	
	
	public EventSubscriber(){
		topicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
	}
	
	public void init() throws Exception{
		topicSubscriber.setMessageListener(this);
		Thread subscriberThread = new Thread(topicSubscriber);
		subscriberThread.start();
		log.info("Event-subscriber started");
	}

	@Override
	public void onMessage(Message msg) {
		if (msg instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) msg;
            try {
				String text = textMessage.getText();
				TopologyEventMessage message = getEventMessage(text, TopologyEventMessage.class);
				EventMessageHeader header = message.getHeader();
				try {
					Object type = Class.forName(header.getEventClassName()).newInstance();
					if (type instanceof ClusterCreatedEvent) {
					} else if (type instanceof ClusterRemovedEvent) {
						ClusterCreatedEvent event = getEventMessage(message.getBody(), ClusterCreatedEvent.class);
						log.info("Cluster-removed-event received : " + event );
						//TODO
					} else if (type instanceof CompleteTopologyEvent) {
						CompleteTopologyEvent event = getEventMessage(message.getBody(), CompleteTopologyEvent.class);
						log.info("Complete-topology-event received : " + event );
						//TODO
					} else if (type instanceof MemberActivatedEvent) {
						MemberActivatedEvent event = getEventMessage(message.getBody(), MemberActivatedEvent.class);
						log.info("Member-activated-event received : " + event );
						//TODO
					} else if (type instanceof MemberStartedEvent) {
						MemberStartedEvent event = getEventMessage(message.getBody(), MemberStartedEvent.class);
						log.info("Member-started-event received : " + event );
						//TODO
					} else if (type instanceof MemberSuspendedEvent) {
						MemberSuspendedEvent event = getEventMessage(message.getBody(), MemberSuspendedEvent.class);
						log.info("Member-suspended-event received : " + event );
						//TODO
					} else if (type instanceof MemberTerminatedEvent) {
						MemberTerminatedEvent event = getEventMessage(message.getBody(), MemberTerminatedEvent.class);
						log.info("Member-terminated-event received : " + event );
						//TODO
					} else if (type instanceof ServiceCreatedEvent) {
						ServiceCreatedEvent event = getEventMessage(message.getBody(), ServiceCreatedEvent.class);
						log.info("Service-created-event received : " + event );
						//TODO
					} else if (type instanceof ServiceRemovedEvent) {
						ServiceRemovedEvent event = getEventMessage(message.getBody(), ServiceRemovedEvent.class);
						log.info("ServiceRemoved-event received : " + event );
						//TODO
					}
				} catch (Exception e) {
					log.error("unrecognized event type received : " + header.getEventClassName());
				}
				
			} catch (JMSException e) {
				log.error("An error occurred", e);
			}
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public <T> T getEventMessage(String json,Class<T> type) {
		return (T)super.jsonToObject(json, type);
	}



}
