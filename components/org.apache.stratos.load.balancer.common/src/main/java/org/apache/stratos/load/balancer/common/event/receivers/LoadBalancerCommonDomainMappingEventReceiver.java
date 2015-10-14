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

package org.apache.stratos.load.balancer.common.event.receivers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingRemovedEvent;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingAddedEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingRemovedEventListener;
import org.apache.stratos.messaging.message.receiver.domain.mapping.DomainMappingEventReceiver;

/**
 * Load balancer common domain mapping event receiver updates the topology in the given topology provider
 * with the domains found in domain mapping events.
 */
public class LoadBalancerCommonDomainMappingEventReceiver extends DomainMappingEventReceiver {

    private static final Log log = LogFactory.getLog(LoadBalancerCommonDomainMappingEventReceiver.class);

    private TopologyProvider topologyProvider;

    public LoadBalancerCommonDomainMappingEventReceiver(TopologyProvider topologyProvider) {
        this.topologyProvider = topologyProvider;
        addEventListeners();
    }

    public LoadBalancerCommonDomainMappingEventReceiver(TopologyProvider topologyProvider, boolean addListeners) {
        this.topologyProvider = topologyProvider;
        if(addListeners) {
            addEventListeners();
        }
    }

    /**
     * Add default event listeners for updating the topology with
     * domain mapping events.
     */
    public void addEventListeners() {
        addEventListener(new DomainMappingAddedEventListener() {
            @Override
            protected void onEvent(Event event) {
                DomainMappingAddedEvent domainMappingAddedEvent = (DomainMappingAddedEvent) event;

                String domainName = domainMappingAddedEvent.getDomainName();
                String contextPath = domainMappingAddedEvent.getContextPath();

                String clusterId = domainMappingAddedEvent.getClusterId();
                Cluster cluster = topologyProvider.getClusterByClusterId(clusterId);
                if (cluster == null) {
                    log.warn(String.format("Could not add domain mapping, cluster not found: [cluster] %s", clusterId));
                }

                addDomainMapping(cluster, domainName, contextPath);
            }
        });

        addEventListener(new DomainMappingRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                DomainMappingRemovedEvent domainMappingRemovedEvent = (DomainMappingRemovedEvent) event;

                String clusterId = domainMappingRemovedEvent.getClusterId();
                Cluster cluster = topologyProvider.getClusterByClusterId(clusterId);
                if (cluster == null) {
                    log.warn(String.format("Could not remove domain mapping, cluster not found: [cluster] %s", clusterId));
                }

                String domainName = domainMappingRemovedEvent.getDomainName();
                removeDomainMapping(cluster, domainName);
            }
        });
    }

    /**
     * Add domain mapping.
     *
     * @param cluster
     * @param domainName
     * @param contextPath
     */
    protected void addDomainMapping(Cluster cluster, String domainName, String contextPath) {
        cluster.addHostName(domainName, contextPath);
        log.info(String.format("Domain mapping added: [cluster] %s [domain] %s [context-path]", cluster.getClusterId(), domainName));
    }

    /**
     * Remove domain mapping.
     *
     * @param cluster
     * @param domainName
     */
    protected void removeDomainMapping(Cluster cluster, String domainName) {
        cluster.removeHostName(domainName);
        log.info(String.format("Domain mapping removed: [cluster] %s [domain] %s", cluster.getClusterId(), domainName));
    }
}
