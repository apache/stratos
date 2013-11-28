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
package org.apache.stratos.messaging.message.processor.topology;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.apache.stratos.messaging.event.topology.CompleteTopologyEvent;
import org.apache.stratos.messaging.message.filter.topology.ClusterFilter;
import org.apache.stratos.messaging.message.processor.MessageProcessor;
import org.apache.stratos.messaging.message.filter.topology.ServiceFilter;
import org.apache.stratos.messaging.util.Util;

public class CompleteTopologyEventProcessor extends MessageProcessor {

    private static final Log log = LogFactory.getLog(CompleteTopologyEventProcessor.class);
    private MessageProcessor nextProcessor;

    @Override
    public void setNext(MessageProcessor nextProcessor) {
        this.nextProcessor = nextProcessor;
    }

    @Override
    public boolean process(String type, String message, Object object) {
        Topology topology = (Topology) object;

        if (CompleteTopologyEvent.class.getName().equals(type)) {
            // Parse complete message and build event
            CompleteTopologyEvent event = (CompleteTopologyEvent) Util.jsonToObject(message, CompleteTopologyEvent.class);

            // Apply service filter
            if (ServiceFilter.getInstance().isActive()) {
                // Add services included in service filter
                for (Service service : event.getTopology().getServices()) {
                    if (ServiceFilter.getInstance().included(service.getServiceName())) {
                        topology.addService(service);
                    }
                    else {
                        if(log.isDebugEnabled()) {
                            log.debug(String.format("Service is excluded: [service] %s", service.getServiceName()));
                        }
                    }
                }
            } else {
                // Add all services
                topology.addServices(event.getTopology().getServices());
            }

            // Apply cluster filter
            if (ClusterFilter.getInstance().isActive()) {
                for (Service service : topology.getServices()) {
                    for (Cluster cluster : service.getClusters()) {
                        if (ClusterFilter.getInstance().excluded(cluster.getClusterId())) {
                            service.removeCluster(cluster.getClusterId());
                            if(log.isDebugEnabled()) {
                                log.debug(String.format("Cluster is excluded: [cluster] %s", cluster.getClusterId()));
                            }
                        }
                    }
                }
            }

            if (log.isInfoEnabled()) {
                log.info("Topology initialized");
            }

            // Notify event listeners
            notifyEventListeners(event);
            return true;
        } else {
            if (nextProcessor != null) {
                // ask the next processor to take care of the message.
                return nextProcessor.process(type, message, topology);
            }
            return false;
        }
    }
}
