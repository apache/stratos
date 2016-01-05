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

package org.apache.stratos.mock.iaas.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.topology.MemberInitializedEvent;
import org.apache.stratos.messaging.event.topology.MemberMaintenanceModeEvent;
import org.apache.stratos.messaging.event.topology.MemberStartedEvent;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.listener.topology.MemberInitializedEventListener;
import org.apache.stratos.messaging.listener.topology.MemberMaintenanceListener;
import org.apache.stratos.messaging.listener.topology.MemberStartedEventListener;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.event.publisher.MockMemberEventPublisher;
import org.apache.stratos.mock.iaas.statistics.publisher.MockHealthStatisticsNotifier;

import java.io.Serializable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Mock member instance definition.
 */
public class MockInstance implements Serializable {
    private static final Log log = LogFactory.getLog(MockInstance.class);
    private static final int HEALTH_STAT_INTERVAL = 15; // 15 seconds

    private transient ScheduledFuture<?> healthStatNotifierScheduledFuture;
    private transient InstanceNotifierEventReceiver instanceNotifierEventReceiver;
    private transient TopologyEventReceiver topologyEventReceiver;
    private transient MockHealthStatisticsNotifier mockHealthStatisticsNotifier;

    // this is the mock iaas instance runtime status, do not persist this state
    private transient MemberStatus memberStatus = MemberStatus.Created;

    private final MockInstanceContext mockInstanceContext;
    private final AtomicBoolean hasGracefullyShutdown = new AtomicBoolean(false);

//    private static final ThreadPoolExecutor eventListenerExecutor = StratosThreadPool
//            .getExecutorService("mock.iaas.event.listener.thread.pool", 35, 100);
    private static final ScheduledThreadPoolExecutor healthStatNotifierExecutor = StratosThreadPool
            .getScheduledExecutorService("mock.iaas.health.statistics.notifier.thread.pool", 100);

    public MockInstance(MockInstanceContext mockInstanceContext) {
        this.mockInstanceContext = mockInstanceContext;
    }

    public synchronized void initialize() {
        if (MemberStatus.Created.equals(memberStatus) || memberStatus == null) {
            startTopologyEventReceiver();
            startInstanceNotifierEventReceiver();
            startHealthStatisticsPublisher();
            memberStatus = MemberStatus.Initialized;
            if (log.isInfoEnabled()) {
                log.info(String.format("Mock instance initialized: [member-id] %s", mockInstanceContext.getMemberId()));
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Mock instance cannot be initialized since it is not in created state: [member-id] %s [status] "
                                + "%s", mockInstanceContext.getMemberId(), memberStatus));
            }
        }
    }

    private void startHealthStatisticsPublisher() {
        mockHealthStatisticsNotifier = new MockHealthStatisticsNotifier(mockInstanceContext);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Starting health statistics notifier: [member-id] %s",
                    mockInstanceContext.getMemberId()));
        }
        healthStatNotifierScheduledFuture = healthStatNotifierExecutor
                .scheduleAtFixedRate(mockHealthStatisticsNotifier, 0, HEALTH_STAT_INTERVAL, TimeUnit.SECONDS);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Health statistics notifier started: [member-id] %s",
                    mockInstanceContext.getMemberId()));
        }
    }

    private void startTopologyEventReceiver() {
        topologyEventReceiver = TopologyEventReceiver.getInstance();
        topologyEventReceiver.addEventListener(new MemberInitializedEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberInitializedEvent memberInitializedEvent = (MemberInitializedEvent) event;
                if (memberInitializedEvent.getMemberId().equals(mockInstanceContext.getMemberId())) {
                    MockMemberEventPublisher.publishInstanceStartedEvent(mockInstanceContext);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Mock member started event published for [member-id] %s",
                                mockInstanceContext.getMemberId()));
                    }
                }
            }
        });
        topologyEventReceiver.addEventListener(new MemberStartedEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberStartedEvent memberStartedEvent = (MemberStartedEvent) event;
                if (memberStartedEvent.getMemberId().equals(mockInstanceContext.getMemberId())) {
                    MockMemberEventPublisher.publishInstanceActivatedEvent(mockInstanceContext);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Mock member activated event published for [member-id] %s",
                                mockInstanceContext.getMemberId()));
                    }
                }
            }
        });
        topologyEventReceiver.addEventListener(new MemberMaintenanceListener() {
            @Override
            protected void onEvent(Event event) {
                MemberMaintenanceModeEvent memberMaintenanceModeEvent = (MemberMaintenanceModeEvent) event;
                if (memberMaintenanceModeEvent.getMemberId().equals(mockInstanceContext.getMemberId())) {
                    MockMemberEventPublisher.publishInstanceReadyToShutdownEvent(mockInstanceContext);
                    hasGracefullyShutdown.set(true);
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Mock member ready to shutdown event published for [member-id] %s",
                                mockInstanceContext.getMemberId()));
                    }
                }
            }
        });
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Mock instance topology event message receiver started for mock member [member-id] %s",
                    mockInstanceContext.getMemberId()));
        }
    }

    private void startInstanceNotifierEventReceiver() {
        instanceNotifierEventReceiver = InstanceNotifierEventReceiver.getInstance();
        instanceNotifierEventReceiver.addEventListener(new InstanceCleanupClusterEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceCleanupClusterEvent instanceCleanupClusterEvent = (InstanceCleanupClusterEvent) event;
                if (mockInstanceContext.getClusterId().equals(instanceCleanupClusterEvent.getClusterId())
                        && mockInstanceContext.getClusterInstanceId()
                        .equals(instanceCleanupClusterEvent.getClusterInstanceId())) {
                    handleMemberTermination();
                }
            }
        });

        instanceNotifierEventReceiver.addEventListener(new InstanceCleanupMemberEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceCleanupMemberEvent instanceCleanupMemberEvent = (InstanceCleanupMemberEvent) event;
                if (mockInstanceContext.getMemberId().equals(instanceCleanupMemberEvent.getMemberId())) {
                    handleMemberTermination();
                }
            }
        });
    }

    private void handleMemberTermination() {
        if (!hasGracefullyShutdown.get()) {
            MockMemberEventPublisher.publishMaintenanceModeEvent(mockInstanceContext);
        } else {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Mock instance is already gracefully shutdown [member-id] %s",
                        mockInstanceContext.getMemberId()));
            }
        }
    }

    private void stopHealthStatisticsPublisher() {
        healthStatNotifierScheduledFuture.cancel(true);
    }

//    private void stopInstanceNotifierReceiver() {
//        instanceNotifierEventReceiver.terminate();
//    }

    public MockInstanceContext getMockInstanceContext() {
        return mockInstanceContext;
    }

    public synchronized void terminate() {
        if (MemberStatus.Initialized.equals(memberStatus)) {
            //stopInstanceNotifierReceiver();
            stopHealthStatisticsPublisher();
            memberStatus = MemberStatus.Terminated;
            if (log.isInfoEnabled()) {
                log.info(String.format("Mock instance stopped: [member-id] %s", mockInstanceContext.getMemberId()));
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Mock instance cannot be terminated since it is not in initialized state: [member-id] %s ",
                        mockInstanceContext.getMemberId()));
            }
        }
    }

    public MockHealthStatisticsNotifier getMockHealthStatisticsNotifier() {
        return mockHealthStatisticsNotifier;
    }
}
