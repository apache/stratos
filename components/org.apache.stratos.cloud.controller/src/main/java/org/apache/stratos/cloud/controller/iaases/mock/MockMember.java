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

package org.apache.stratos.cloud.controller.iaases.mock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.iaases.mock.statistics.MockHealthStatisticsNotifier;

import java.io.Serializable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock member definition.
 */
public class MockMember implements Runnable, Serializable {

    private static final Log log = LogFactory.getLog(MockMember.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int HEALTH_STAT_INTERVAL = 15;

    private MockMemberContext mockMemberContext;
    private boolean terminated;

    public MockMember(MockMemberContext mockMemberContext) {
        this.mockMemberContext = mockMemberContext;
    }

    @Override
    public void run() {
        if(log.isInfoEnabled()) {
            log.info(String.format("Mock member started: [member-id] %s", mockMemberContext.getMemberId()));
        }

        sleep(5000);
        MockMemberEventPublisher.publishInstanceStartedEvent(mockMemberContext);

        sleep(5000);
        MockMemberEventPublisher.publishInstanceActivatedEvent(mockMemberContext);

        if (log.isInfoEnabled()) {
            log.info(String.format("Starting health statistics notifier: [member-id] %s", mockMemberContext.getMemberId()));
        }
        scheduler.scheduleAtFixedRate(new MockHealthStatisticsNotifier(mockMemberContext),
                HEALTH_STAT_INTERVAL, HEALTH_STAT_INTERVAL, TimeUnit.SECONDS);

        if (log.isInfoEnabled()) {
            log.info(String.format("Health statistics notifier started: [member-id] %s", mockMemberContext.getMemberId()));
        }

        while(!terminated) {
            sleep(1000);
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
        String memberId = mockMemberContext.getMemberId();
        terminated = true;
        scheduler.shutdownNow();

        if(log.isInfoEnabled()) {
            log.info(String.format("Mock member terminated: [member-id] %s", memberId));
        }
    }
}
