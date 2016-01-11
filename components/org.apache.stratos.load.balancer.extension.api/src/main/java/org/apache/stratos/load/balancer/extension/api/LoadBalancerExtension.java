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

package org.apache.stratos.load.balancer.extension.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.domain.Cluster;
import org.apache.stratos.load.balancer.common.domain.Service;
import org.apache.stratos.load.balancer.common.domain.Topology;
import org.apache.stratos.load.balancer.common.event.receivers.LoadBalancerCommonApplicationSignUpEventReceiver;
import org.apache.stratos.load.balancer.common.event.receivers.LoadBalancerCommonDomainMappingEventReceiver;
import org.apache.stratos.load.balancer.common.event.receivers.LoadBalancerCommonTopologyEventReceiver;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.statistics.notifier.LoadBalancerStatisticsNotifier;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.load.balancer.extension.api.exception.LoadBalancerExtensionException;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingAddedEventListener;
import org.apache.stratos.messaging.listener.domain.mapping.DomainMappingRemovedEventListener;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyMemberFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.domain.mapping.DomainMappingEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;

import java.util.concurrent.ExecutorService;

/**
 * Load balancer extension thread for executing load balancer life-cycle according to the topology updates
 * received from the message broker.
 */
public class LoadBalancerExtension {

    private static final Log log = LogFactory.getLog(LoadBalancerExtension.class);

    private LoadBalancer loadBalancer;
    private LoadBalancerStatisticsReader statsReader;
    private boolean loadBalancerStarted;
    private LoadBalancerStatisticsNotifier statisticsNotifier;
    private ExecutorService executorService;

    private TopologyProvider topologyProvider;
    private LoadBalancerCommonTopologyEventReceiver topologyEventReceiver;
    private LoadBalancerCommonDomainMappingEventReceiver domainMappingEventReceiver;
    private LoadBalancerCommonApplicationSignUpEventReceiver applicationSignUpEventReceiver;
    private ApplicationsEventReceiver applicationsEventReceiver;

    /**
     * Load balancer extension constructor.
     *
     * @param loadBalancer Load balancer instance: Mandatory.
     * @param statsReader  Statistics reader: If null statistics notifier thread will not be started.
     */
    public LoadBalancerExtension(LoadBalancer loadBalancer, LoadBalancerStatisticsReader statsReader,
                                 TopologyProvider topologyProvider) {

        this.loadBalancer = loadBalancer;
        this.statsReader = statsReader;
        this.topologyProvider = topologyProvider;
    }


    /**
     * Set executor service and invoke execute() method to start the load balancer extension.
     */
    public void execute() {
        try {
            if (log.isInfoEnabled()) {
                log.info("Load balancer extension started");
            }

            // Start topology receiver thread
            startTopologyEventReceiver(topologyProvider);
            startApplicationEventReceiver();
            startApplicationSignUpEventReceiver(topologyProvider);
            startDomainMappingEventReceiver(topologyProvider);

            if (statsReader != null) {
                // Start stats notifier thread
                statisticsNotifier = new LoadBalancerStatisticsNotifier(statsReader, topologyProvider);
                Thread statsNotifierThread = new Thread(statisticsNotifier);
                statsNotifierThread.start();
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Load balancer statistics reader not found");
                }
            }
            log.info("Waiting for complete topology event...");
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not start load balancer extension", e);
            }
        }
    }

    /**
     * Start topology event receiver thread.
     *
     * @param topologyProvider topology provider instance
     */
    private void startTopologyEventReceiver(TopologyProvider topologyProvider) {
        // Enforcing the listener order in order execute extension listener later
        topologyEventReceiver = new LoadBalancerCommonTopologyEventReceiver(topologyProvider, false);
        // Add load-balancer extension event listener
        addTopologyEventListeners(topologyEventReceiver);
        // Add default topology provider event listeners
        topologyEventReceiver.addEventListeners();

        if (log.isInfoEnabled()) {
            log.info("Topology receiver thread started");
        }

        if (log.isInfoEnabled()) {
            if (TopologyServiceFilter.getInstance().isActive()) {
                log.info(String.format("Service filter activated: [filter] %s",
                        TopologyServiceFilter.getInstance().toString()));
            }

            if (TopologyClusterFilter.getInstance().isActive()) {
                log.info(String.format("Cluster filter activated: [filter] %s",
                        TopologyClusterFilter.getInstance().toString()));
            }

            if (TopologyMemberFilter.getInstance().isActive()) {
                log.info(String.format("Member filter activated: [filter] %s",
                        TopologyMemberFilter.getInstance().toString()));
            }
        }
    }

    private void startApplicationEventReceiver() {
        applicationsEventReceiver = ApplicationsEventReceiver.getInstance();

        if (log.isInfoEnabled()) {
            log.info("Application event receiver thread started");
        }
    }

    /**
     * Start domain mapping event receiver thread.
     *
     * @param topologyProvider topology receiver instance
     */
    private void startDomainMappingEventReceiver(TopologyProvider topologyProvider) {
        // Enforcing the listener order in order execute extension listener later
        domainMappingEventReceiver = new LoadBalancerCommonDomainMappingEventReceiver(topologyProvider, false);
        // Add extension event listeners
        addDomainMappingsEventListeners(domainMappingEventReceiver);
        // Add default domain mapping event listeners
        domainMappingEventReceiver.addEventListeners();

        if (log.isInfoEnabled()) {
            log.info("Domain mapping event receiver thread started");
        }
    }

    private void addDomainMappingsEventListeners(final LoadBalancerCommonDomainMappingEventReceiver
                                                         domainMappingEventReceiver) {
        DomainMappingEventReceiver.getInstance().addEventListener(new DomainMappingAddedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });

        DomainMappingEventReceiver.getInstance().addEventListener(new DomainMappingRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
    }

    /**
     * Start application signup event receiver thread.
     *
     * @param topologyProvider topology provider instance
     */
    private void startApplicationSignUpEventReceiver(TopologyProvider topologyProvider) {
        applicationSignUpEventReceiver = new LoadBalancerCommonApplicationSignUpEventReceiver(topologyProvider);
        if (log.isInfoEnabled()) {
            log.info("Application signup event receiver thread started");
        }
    }

    /**
     * Add topology event listeners to the topology event receiver.
     *
     * @param topologyEventReceiver topology event receiver instance
     */
    private void addTopologyEventListeners(final LoadBalancerCommonTopologyEventReceiver topologyEventReceiver) {
        TopologyEventReceiver.getInstance().addEventListener(new CompleteTopologyEventListener() {

            @Override
            protected void onEvent(Event event) {
                try {
                    if (!loadBalancerStarted) {
                        configureAndStart();
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Could not start load balancer", e);
                    }
                }
            }
        });
        TopologyEventReceiver.getInstance().addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        TopologyEventReceiver.getInstance().addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        TopologyEventReceiver.getInstance().addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        TopologyEventReceiver.getInstance().addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        TopologyEventReceiver.getInstance().addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
        TopologyEventReceiver.getInstance().addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
    }

    /**
     * Configure and start the load balancer
     *
     * @throws LoadBalancerExtensionException if configuration or start process fails
     */
    private void configureAndStart() throws LoadBalancerExtensionException {
        // Initialize topology
        if (!topologyEventReceiver.isInitialized()) {
            topologyEventReceiver.initializeTopology();
        }

        // Configure load balancer
        Topology topology = topologyProvider.getTopology();
        if (topologyPopulated(topology) && loadBalancer.configure(topology)) {
            // Start load balancer
            loadBalancer.start();
            loadBalancerStarted = true;
        }
    }

    /**
     * Configure and reload the load balancer
     *
     * @throws LoadBalancerExtensionException if the configuration or reload process fails
     */
    private void configureAndReload() throws LoadBalancerExtensionException {
        // Configure load balancer
        if (loadBalancer.configure(topologyProvider.getTopology())) {
            // Reload the load balancer
            loadBalancer.reload();
        }
    }

    /**
     * Returns true if topology has populated with at least one member.
     *
     * @param topology topology to be validated
     * @return true if at least one member was found else false
     */
    private boolean topologyPopulated(Topology topology) {
        for (Service service : topology.getServices()) {
            for (Cluster cluster : service.getClusters()) {
                if (cluster.getMembers().size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Configure and reload load balancer configuration.
     */
    private void reloadConfiguration() {
        try {
            if (!loadBalancerStarted) {
                configureAndStart();
            } else {
                configureAndReload();

            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not reload load balancer configuration", e);
            }
        }
    }

    /**
     * Stop load balancer instance.
     */
    public void stop() {
//        try {
//            if (topologyEventReceiver != null) {
//                topologyEventReceiver.terminate();
//            }
//        } catch (Exception ignore) {
//        }

        try {
            if (statisticsNotifier != null) {
                statisticsNotifier.terminate();
            }
        } catch (Exception ignore) {
        }

        try {
            loadBalancer.stop();
        } catch (Exception ignore) {
        }
    }

    /**
     * Get executor service of the load balancer extension.
     *
     * @return executor service
     */
    public ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Set executor service for the load balancer extension.
     *
     * @param executorService executor service instance
     */
    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }
}
