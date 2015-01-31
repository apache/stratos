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
package org.apache.stratos.manager.messaging.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.util.MessagingUtil;

/**
 * Creating the relevant instance notification event and publish it to the
 * instances.
 */
public class InstanceNotificationPublisher {

	private static final Log log = LogFactory.getLog(InstanceNotificationPublisher.class);

	public InstanceNotificationPublisher() {
	}

	private void publish(Event event) {
		String topic = MessagingUtil.getMessageTopicName(event);
		EventPublisher eventPublisher = EventPublisherPool.getPublisher(topic);
		eventPublisher.publish(event);
	}

	/**
	 * Publishing the artifact update event to the instances
	 *
	 * @param clusterId
	 * @param tenantId
	 * @param repoUrl
	 * @param repoUsername
	 * @param repoPassword
	 * @param isCommitEnabled
	 */
	public void publishArtifactUpdatedEvent(String clusterId, String tenantId, String repoUrl, String repoUsername,
											String repoPassword, boolean isCommitEnabled) {

		ArtifactUpdatedEvent artifactUpdateEvent = new ArtifactUpdatedEvent();
		artifactUpdateEvent.setClusterId(clusterId);
		artifactUpdateEvent.setRepoUserName(repoUsername);
		artifactUpdateEvent.setRepoPassword(repoPassword);
		artifactUpdateEvent.setRepoURL(repoUrl);
		artifactUpdateEvent.setTenantId(tenantId);
		artifactUpdateEvent.setCommitEnabled(isCommitEnabled);

		publish(artifactUpdateEvent);
	}
}
