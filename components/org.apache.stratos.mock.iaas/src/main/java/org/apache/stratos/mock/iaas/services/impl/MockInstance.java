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
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;
import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.event.publisher.MockMemberEventPublisher;
import org.apache.stratos.mock.iaas.statistics.publisher.MockHealthStatisticsNotifier;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Mock member definition.
 */
public class MockInstance implements Runnable, Serializable {

    private static final Log log = LogFactory.getLog(MockInstance.class);
    private static final ExecutorService eventListenerExecutorService =
            StratosThreadPool.getExecutorService("mock.iaas.event.listener.thread.pool", 20);
    private static final ScheduledExecutorService healthStatNotifierExecutorService =
            StratosThreadPool.getScheduledExecutorService("mock.iaas.health.statistics.notifier.thread.pool", 20);
    private static final int HEALTH_STAT_INTERVAL = 15; // 15 seconds

    private final MockInstanceContext mockMemberContext;
    private boolean terminated;
    private transient ScheduledFuture<?> healthStatNotifierScheduledFuture;
    private transient InstanceNotifierEventReceiver instanceNotifierEventReceiver;

    public MockInstance(MockInstanceContext mockMemberContext) {
        this.mockMemberContext = mockMemberContext;
    }

    @Override
    public void run() {
        if (log.isInfoEnabled()) {
            log.info(String.format("Mock member started: [member-id] %s", mockMemberContext.getMemberId()));
        }

        sleep(5000);
        MockMemberEventPublisher.publishInstanceStartedEvent(mockMemberContext);

        sleep(5000);
        MockMemberEventPublisher.publishInstanceActivatedEvent(mockMemberContext);

        startInstanceNotifierReceiver();
        startHealthStatisticsPublisher();

        while (!terminated) {
            sleep(1000);
        }

        stopInstanceNotifierReceiver();
        stopHealthStatisticsPublisher();

        if (log.isInfoEnabled()) {
            log.info(String.format("Mock member terminated: [member-id] %s", mockMemberContext.getMemberId()));
        }
    }

    private void startInstanceNotifierReceiver() {
        if (log.isDebugEnabled()) {
            log.debug("Starting instance notifier event message receiver");
        }

        instanceNotifierEventReceiver = new InstanceNotifierEventReceiver();
        instanceNotifierEventReceiver.addEventListener(new InstanceCleanupClusterEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceCleanupClusterEvent instanceCleanupClusterEvent = (InstanceCleanupClusterEvent) event;
                if (mockMemberContext.getClusterId().equals(instanceCleanupClusterEvent.getClusterId()) &&
                        mockMemberContext.getClusterInstanceId().equals(
                                instanceCleanupClusterEvent.getClusterInstanceId())) {
                    handleMemberTermination();
                }
            }
        });

        instanceNotifierEventReceiver.addEventListener(new InstanceCleanupMemberEventListener() {
            @Override
            protected void onEvent(Event event) {
                InstanceCleanupMemberEvent instanceCleanupMemberEvent = (InstanceCleanupMemberEvent) event;
                if (mockMemberContext.getMemberId().equals(instanceCleanupMemberEvent.getMemberId())) {
                    handleMemberTermination();
                }
            }
        });

        eventListenerExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                instanceNotifierEventReceiver.execute();
            }
        });

        if (log.isDebugEnabled()) {
            log.debug("Instance notifier event message receiver started");
        }
    }

    private void handleMemberTermination() {
        MockMemberEventPublisher.publishMaintenanceModeEvent(mockMemberContext);
        sleep(2000);
        MockMemberEventPublisher.publishInstanceReadyToShutdownEvent(mockMemberContext);
    }

    private void startHealthStatisticsPublisher() {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Starting health statistics notifier: [member-id] %s", mockMemberContext.getMemberId()));
        }

        healthStatNotifierScheduledFuture = healthStatNotifierExecutorService.scheduleAtFixedRate(new MockHealthStatisticsNotifier(mockMemberContext),
                0, HEALTH_STAT_INTERVAL, TimeUnit.SECONDS);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Health statistics notifier started: [member-id] %s", mockMemberContext.getMemberId()));
        }
    }

    private void stopHealthStatisticsPublisher() {
        if (healthStatNotifierScheduledFuture != null) {
            healthStatNotifierScheduledFuture.cancel(true);
        }
    }

    private void stopInstanceNotifierReceiver() {
        if (instanceNotifierEventReceiver != null) {
            instanceNotifierEventReceiver.terminate();
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
            terminate();
        }
    }

    public MockInstanceContext getMockInstanceContext() {
        return mockMemberContext;
    }

    public void terminate() {
        terminated = true;
    }
}
