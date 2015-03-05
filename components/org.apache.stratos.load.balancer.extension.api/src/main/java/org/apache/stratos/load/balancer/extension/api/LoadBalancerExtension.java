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
import org.apache.stratos.load.balancer.common.event.receivers.LoadBalancerCommonApplicationSignUpEventReceiver;
import org.apache.stratos.load.balancer.common.event.receivers.LoadBalancerCommonDomainMappingEventReceiver;
import org.apache.stratos.load.balancer.common.event.receivers.LoadBalancerCommonTopologyEventReceiver;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.statistics.notifier.LoadBalancerStatisticsNotifier;
import org.apache.stratos.load.balancer.common.topology.TopologyProvider;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyMemberFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;

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

    /**
	 * Load balancer extension constructor.
	 *
	 * @param loadBalancer Load balancer instance: Mandatory.
	 * @param statsReader  Statistics reader: If null statistics notifier thread will not be started.
	 */
	public LoadBalancerExtension(LoadBalancer loadBalancer, LoadBalancerStatisticsReader statsReader) {
		this.loadBalancer = loadBalancer;
		this.statsReader = statsReader;
        this.topologyProvider = new TopologyProvider();
	}


	public void execute() {
		try {
			if (log.isInfoEnabled()) {
				log.info("Load balancer extension started");
			}

			// Start topology receiver thread
			startTopologyEventReceiver(executorService, topologyProvider);
            startApplicationSignUpEventReceiver(executorService, topologyProvider);
            startDomainMappingEventReceiver(executorService, topologyProvider);

			if (statsReader != null) {
				// Start stats notifier thread
				statisticsNotifier = new LoadBalancerStatisticsNotifier(statsReader);
				Thread statsNotifierThread = new Thread(statisticsNotifier);
				statsNotifierThread.start();
			} else {
				if (log.isWarnEnabled()) {
					log.warn("Load balancer statistics reader not found");
				}
			}

		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("Could not start load balancer extension", e);
			}
		}
	}

    private void startTopologyEventReceiver(ExecutorService executorService, TopologyProvider topologyProvider) {

        topologyEventReceiver = new LoadBalancerCommonTopologyEventReceiver(topologyProvider);
        addTopologyEventListeners(topologyEventReceiver);
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();
        if (log.isInfoEnabled()) {
            log.info("Topology receiver thread started");
        }

        if (log.isInfoEnabled()) {
            if(TopologyServiceFilter.getInstance().isActive()) {
                log.info(String.format("Service filter activated: [filter] %s",
                        TopologyServiceFilter.getInstance().toString()));
            }

            if(TopologyClusterFilter.getInstance().isActive()) {
                log.info(String.format("Cluster filter activated: [filter] %s",
                        TopologyClusterFilter.getInstance().toString()));
            }

            if(TopologyMemberFilter.getInstance().isActive()) {
                log.info(String.format("Member filter activated: [filter] %s",
                        TopologyMemberFilter.getInstance().toString()));
            }
        }
    }

    private void startDomainMappingEventReceiver(ExecutorService executorService, TopologyProvider topologyProvider) {
        domainMappingEventReceiver = new LoadBalancerCommonDomainMappingEventReceiver(topologyProvider);
        domainMappingEventReceiver.setExecutorService(executorService);
        domainMappingEventReceiver.execute();
        if (log.isInfoEnabled()) {
            log.info("Domain mapping event receiver thread started");
        }
    }

    private void startApplicationSignUpEventReceiver(ExecutorService executorService, TopologyProvider topologyProvider) {
        applicationSignUpEventReceiver = new LoadBalancerCommonApplicationSignUpEventReceiver(topologyProvider);
        applicationSignUpEventReceiver.setExecutorService(executorService);
        applicationSignUpEventReceiver.execute();
        if (log.isInfoEnabled()) {
            log.info("Application signup event receiver thread started");
        }
    }

    private void addTopologyEventListeners(LoadBalancerCommonTopologyEventReceiver topologyEventReceiver) {
		topologyEventReceiver.addEventListener(new CompleteTopologyEventListener() {

            @Override
            protected void onEvent(Event event) {
                try {
                    if (!loadBalancerStarted) {
                        // Configure load balancer
                        loadBalancer.configure(topologyProvider.getTopology());

                        // Start load balancer
                        loadBalancer.start();
                        loadBalancerStarted = true;
                    }
                } catch (Exception e) {
                    if (log.isErrorEnabled()) {
                        log.error("Could not start load balancer", e);
                    }
                    terminate();
                }
            }
        });
		topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
		topologyEventReceiver.addEventListener(new MemberSuspendedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
		topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
		topologyEventReceiver.addEventListener(new ClusterRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
		topologyEventReceiver.addEventListener(new ServiceRemovedEventListener() {
            @Override
            protected void onEvent(Event event) {
                reloadConfiguration();
            }
        });
	}

	private void reloadConfiguration() {
		try {
			if (loadBalancerStarted) {
                // Configure load balancer
                loadBalancer.configure(topologyProvider.getTopology());

                // Reload the load balancer
				loadBalancer.reload();
			}
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("Could not reload load balancer configuration", e);
			}
		}
	}

	public void terminate() {
		if (topologyEventReceiver != null) {
			topologyEventReceiver.terminate();
		}
		if (statisticsNotifier != null) {
			statisticsNotifier.terminate();
		}
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}
}
