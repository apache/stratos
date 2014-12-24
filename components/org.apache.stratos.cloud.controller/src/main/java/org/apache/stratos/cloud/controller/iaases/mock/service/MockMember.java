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

package org.apache.stratos.cloud.controller.iaases.mock.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.iaases.mock.service.statistics.publisher.MockHealthStatisticsNotifier;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupClusterEventListener;
import org.apache.stratos.messaging.listener.instance.notifier.InstanceCleanupMemberEventListener;
import org.apache.stratos.messaging.message.receiver.instance.notifier.InstanceNotifierEventReceiver;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock member definition.
 */
public class MockMember implements Runnable, Serializable {

    private static final Log log = LogFactory.getLog(MockMember.class);
    private static final ExecutorService instanceNotifierExecutorService =
            StratosThreadPool.getExecutorService("MOCK_MEMBER_INSTANCE_NOTIFIER_EXECUTOR_SERVICE", 20);
    private static final ScheduledExecutorService healthStatNotifierExecutorService =
            StratosThreadPool.getScheduledExecutorService("MOCK_MEMBER_HEALTH_STAT_NOTIFIER_EXECUTOR_SERVICE", 20);
    private static final int HEALTH_STAT_INTERVAL = 15; // 15 seconds

    private final MockMemberContext mockMemberContext;
    private boolean terminated;

    public MockMember(MockMemberContext mockMemberContext) {
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

        if (log.isInfoEnabled()) {
            log.info(String.format("Mock member terminated: [member-id] %s", mockMemberContext.getMemberId()));
        }
    }

    private void startInstanceNotifierReceiver() {
        if (log.isDebugEnabled()) {
            log.debug("Starting instance notifier event message receiver");
        }

        final InstanceNotifierEventReceiver instanceNotifierEventReceiver = new InstanceNotifierEventReceiver();
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
                InstanceCleanupMemberEvent instanceCleanupClusterEvent = (InstanceCleanupMemberEvent) event;
                if (mockMemberContext.getMemberId().equals(instanceCleanupClusterEvent.getMemberId())) {
                    handleMemberTermination();
                }
            }
        });

        instanceNotifierExecutorService.submit(new Runnable() {
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

        healthStatNotifierExecutorService.scheduleAtFixedRate(new MockHealthStatisticsNotifier(mockMemberContext),
                0, HEALTH_STAT_INTERVAL, TimeUnit.SECONDS);

        if (log.isDebugEnabled()) {
            log.debug(String.format("Health statistics notifier started: [member-id] %s", mockMemberContext.getMemberId()));
        }
    }

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
            terminate();
        }
    }

    public MockMemberContext getMockMemberContext() {
        return mockMemberContext;
    }

    public void terminate() {
        terminated = true;
    }
}
