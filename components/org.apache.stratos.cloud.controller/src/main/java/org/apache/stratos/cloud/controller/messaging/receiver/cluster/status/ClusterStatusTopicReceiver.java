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
package org.apache.stratos.cloud.controller.messaging.receiver.cluster.status;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.messaging.topology.TopologyBuilder;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.cluster.status.*;
import org.apache.stratos.messaging.listener.cluster.status.*;
import org.apache.stratos.messaging.message.receiver.cluster.status.ClusterStatusEventReceiver;

import java.util.concurrent.ExecutorService;

public class ClusterStatusTopicReceiver {
	private static final Log log = LogFactory.getLog(ClusterStatusTopicReceiver.class);

	private ClusterStatusEventReceiver statusEventReceiver;
	private boolean terminated;
	private ExecutorService executorService;

	public ClusterStatusTopicReceiver() {
		this.statusEventReceiver = new ClusterStatusEventReceiver();

		addEventListeners();
	}

	public void execute() {
		statusEventReceiver.setExecutorService(executorService);
		statusEventReceiver.execute();
		if (log.isInfoEnabled()) {
			log.info("Cloud controller Cluster status thread started");
		}

	}

	private void addEventListeners() {
		// Listen to topology events that affect clusters
		statusEventReceiver.addEventListener(new ClusterStatusClusterResetEventListener() {
			@Override
			protected void onEvent(Event event) {
				TopologyBuilder.handleClusterReset((ClusterStatusClusterResetEvent) event);
			}
		});

		statusEventReceiver.addEventListener(new ClusterStatusClusterInstanceCreatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				//TopologyBuilder.handleClusterInstanceCreated((ClusterStatusClusterInstanceCreatedEvent) event);
			}
		});

		statusEventReceiver.addEventListener(new ClusterStatusClusterCreatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				TopologyBuilder.handleClusterCreated((ClusterStatusClusterCreatedEvent) event);
			}
		});

		statusEventReceiver.addEventListener(new ClusterStatusClusterActivatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				TopologyBuilder.handleClusterActivatedEvent((ClusterStatusClusterActivatedEvent) event);
			}
		});

		statusEventReceiver.addEventListener(new ClusterStatusClusterTerminatedEventListener() {
			@Override
			protected void onEvent(Event event) {
				TopologyBuilder.handleClusterTerminatedEvent((ClusterStatusClusterTerminatedEvent) event);
			}
		});

		statusEventReceiver.addEventListener(new ClusterStatusClusterTerminatingEventListener() {
			@Override
			protected void onEvent(Event event) {
				TopologyBuilder.handleClusterTerminatingEvent((ClusterStatusClusterTerminatingEvent) event);
			}
		});

		statusEventReceiver.addEventListener(new ClusterStatusClusterInactivateEventListener() {
			@Override
			protected void onEvent(Event event) {
				TopologyBuilder.handleClusterInactivateEvent((ClusterStatusClusterInactivateEvent) event);
			}
		});
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
}
