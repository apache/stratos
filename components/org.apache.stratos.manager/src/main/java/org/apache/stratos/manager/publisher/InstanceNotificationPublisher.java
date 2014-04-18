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
package org.apache.stratos.manager.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.repository.Repository;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.util.Constants;

/**
 * Creating the relevant instance notification event and publish it to the instances.
 */
public class InstanceNotificationPublisher {
    private static final Log log = LogFactory.getLog(InstanceNotificationPublisher.class);

    public InstanceNotificationPublisher() {
    }

    private void publish(Event event) {
        EventPublisher depsyncEventPublisher = EventPublisherPool.getPublisher(Constants.INSTANCE_NOTIFIER_TOPIC);
        depsyncEventPublisher.publish(event);
    }

    /**
     * Publishing the artifact update event to the instances
     *
     * @param repository
     * @param clusterId
     * @param tenantId
     */
    public void sendArtifactUpdateEvent(Repository repository, String clusterId, String tenantId) {
        ArtifactUpdatedEvent artifactUpdateEvent = new ArtifactUpdatedEvent();
        artifactUpdateEvent.setClusterId(clusterId);
        artifactUpdateEvent.setRepoUserName(repository.getUserName());
        artifactUpdateEvent.setRepoPassword(repository.getPassword());
        artifactUpdateEvent.setRepoURL(repository.getUrl());
        artifactUpdateEvent.setTenantId(tenantId);

        log.info(String.format("Publishing artifact updated event: [cluster] %s " +
                "[repo-URL] %s [repo-username] %s [repo-password] %s [tenant-id] %s",
                clusterId, repository.getUrl(), repository.getUserName(), repository.getPassword(), tenantId));
        publish(artifactUpdateEvent);
    }

    /**
     * Publishing the instance termination notification to the instances
     *
     * @param memberId
     */
    public void sendInstanceCleanupEventForMember(String memberId) {
        log.info(String.format("Publishing Instance Cleanup Event: [member] %s", memberId));
        publish(new InstanceCleanupMemberEvent(memberId));
    }

    public void sendInstanceCleanupEventForCluster(String clusterId) {
         log.info(String.format("Publishing Instance Cleanup Event: [cluster] %s", clusterId));
        publish(new InstanceCleanupClusterEvent(clusterId));
    }
}
