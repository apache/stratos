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

package org.apache.stratos.load.balancer.messaging.receiver;

import org.apache.stratos.load.balancer.context.LoadBalancerContextUtil;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingRemovedEvent;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingAddedEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingRemovedEventListener;
import org.apache.stratos.messaging.message.receiver.domain.mapping.DomainMappingEventReceiver;

/**
 * Load balancer domain mapping event receiver.
 */
public class LoadBalancerDomainMappingEventReceiver extends DomainMappingEventReceiver {

    public LoadBalancerDomainMappingEventReceiver() {
        addEventListeners();
    }

    private void addEventListeners() {

        addEventListener(new DomainMappingAddedEventListener() {
            @Override
            protected void onEvent(Event event) {
                DomainMappingAddedEvent domainMappingAddedEvent = (DomainMappingAddedEvent)event;

                LoadBalancerContextUtil.addClusterAgainstDomain(
                        domainMappingAddedEvent.getServiceName(),
                        domainMappingAddedEvent.getClusterId(),
                        domainMappingAddedEvent.getDomainName());

                LoadBalancerContextUtil.addContextPathAgainstDomain(
                        domainMappingAddedEvent.getDomainName(),
                        domainMappingAddedEvent.getContextPath());
            }
        });

        addEventListener(new DomainMappingRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                DomainMappingRemovedEvent domainMappingRemovedEvent = (DomainMappingRemovedEvent)event;

                LoadBalancerContextUtil.removeClusterAgainstDomain(
                        domainMappingRemovedEvent.getServiceName(),
                        domainMappingRemovedEvent.getClusterId(),
                        domainMappingRemovedEvent.getDomainName());

                LoadBalancerContextUtil.removeContextPathAgainstDomain(
                        domainMappingRemovedEvent.getDomainName());
            }
        });
    }
}
