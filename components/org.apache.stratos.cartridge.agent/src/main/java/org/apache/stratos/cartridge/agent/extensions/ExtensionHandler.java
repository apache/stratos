/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.extensions;

import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainAddedEvent;
import org.apache.stratos.messaging.event.tenant.SubscriptionDomainRemovedEvent;
import org.apache.stratos.messaging.event.topology.*;

public interface ExtensionHandler {
    public void onInstanceStartedEvent();

    public void onInstanceActivatedEvent();

    public void onArtifactUpdatedEvent(ArtifactUpdatedEvent event);

    public void onArtifactUpdateSchedulerEvent(String tenantId);

    public void onInstanceCleanupClusterEvent(InstanceCleanupClusterEvent instanceCleanupClusterEvent);

    public void onInstanceCleanupMemberEvent(InstanceCleanupMemberEvent instanceCleanupMemberEvent);

    public void onMemberActivatedEvent(MemberActivatedEvent memberActivatedEvent);

    public void onCompleteTopologyEvent(CompleteTopologyEvent completeTopologyEvent);

    public void onCompleteTenantEvent(CompleteTenantEvent completeTenantEvent);

    public void onMemberTerminatedEvent(MemberTerminatedEvent memberTerminatedEvent);

    public void onMemberSuspendedEvent(MemberSuspendedEvent memberSuspendedEvent);

    public void onMemberStartedEvent(MemberStartedEvent memberStartedEvent);

    public void startServerExtension();

    public void volumeMountExtension(String persistenceMappingsPayload);

    public void onSubscriptionDomainAddedEvent(SubscriptionDomainAddedEvent subscriptionDomainAddedEvent);

    public void onSubscriptionDomainRemovedEvent(SubscriptionDomainRemovedEvent subscriptionDomainRemovedEvent);

    public void onCopyArtifactsExtension(String src, String des);
}