package org.apache.stratos.metadataservice.listener;

/*
 * 
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.topology.MemberSuspendedEvent;
import org.apache.stratos.messaging.event.topology.MemberTerminatedEvent;
import org.apache.stratos.messaging.listener.topology.MemberSuspendedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberTerminatedEventListener;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.metadataservice.services.MetaDataAdmin;

/**
 * Cartridge agent runnable.
 */
public class TopologyAgent implements Runnable {

	private static final Log log = LogFactory.getLog(TopologyAgent.class);

	private boolean terminated;

	@Override
	public void run() {
		if (log.isInfoEnabled()) {
			log.info("Topology agent started");
		}

		// Start topology event receiver thread
		registerTopologyEventListeners();

	}

	protected void registerTopologyEventListeners() {
		if (log.isDebugEnabled()) {
			log.debug("Starting topology event message receiver thread");
		}
		TopologyEventReceiver topologyEventReceiver = new TopologyEventReceiver();

		topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				try {
					log.info("Member terminated event received");
					TopologyManager.acquireReadLock();
					if (log.isDebugEnabled()) {
						log.debug("Member terminated event received");
					}
					MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
					if(log.isDebugEnabled()){
						log.debug("Terminated event :::::::::::::::::::: " +
					                   memberTerminatedEvent.getServiceName());
					}
					new MetaDataAdmin().removeCartridgeMetaDataDetails("appA", "php");

				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Error processing member terminated event", e);
					}
				} finally {
					TopologyManager.releaseReadLock();
				}
			}
		});

		topologyEventReceiver.addEventListener(new MemberSuspendedEventListener() {
			@Override
			protected void onEvent(Event event) {
				try {
					log.info("Member suspended event received");
					TopologyManager.acquireReadLock();
					if (log.isDebugEnabled()) {
						log.debug("Member suspended event received");
					}
					MemberSuspendedEvent memberSuspendedEvent = (MemberSuspendedEvent) event;
					// extensionHandler.onMemberSuspendedEvent(memberSuspendedEvent);
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error("Error processing member suspended event", e);
					}
				} finally {
					TopologyManager.releaseReadLock();
				}
			}
		});

		Thread thread = new Thread(topologyEventReceiver);
		thread.start();
		if (log.isDebugEnabled()) {
			log.info("Cartridge Agent topology receiver thread started");
		}
	}

}
