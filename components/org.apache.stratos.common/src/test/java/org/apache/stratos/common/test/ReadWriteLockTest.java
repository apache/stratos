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

package org.apache.stratos.common.test;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.common.exception.InvalidLockRequestedException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Read write lock test.
 */
public class ReadWriteLockTest {

    public static class TestAppender extends AppenderSkeleton {

        public List<LoggingEvent> getEvents() {
            return events;
        }

        private List<LoggingEvent> events = new ArrayList<LoggingEvent>();

        public void close() {}

        public boolean requiresLayout() {
            return false;
        }

        @Override
        protected void append(LoggingEvent event) {
            events.add(event);
        }
    }


    @Test
    public void testInvalidLock() {
        Exception exception = null;
        try {
            ReadWriteLock lock = new ReadWriteLock("lock-test");
            lock.acquireReadLock();
            lock.acquireWriteLock();
        } catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof InvalidLockRequestedException);
    }

    @Test
    public void testUnreleasedLocks() {
        final TestAppender testAppender = new TestAppender();
        Logger.getRootLogger().addAppender(testAppender);

        System.setProperty("read.write.lock.monitor.enabled", "true");
        System.setProperty("read.write.lock.monitor.interval", "2000");
        System.setProperty("read.write.lock.timeout", "2000");

        Runnable r1 = new Runnable() {
            @Override
            public void run() {
                ReadWriteLock lock = new ReadWriteLock("unrelased-lock-test");
                lock.acquireReadLock();
                sleep(5000);
                List<LoggingEvent> events = testAppender.getEvents();
                assertTrue(containsMessage(events, "System error, lock has not released"));
            }
        };
        Thread thread = new Thread(r1);
        thread.start();

        try {
            thread.join();
        } catch (InterruptedException ignore) {
        }
    }

    private boolean containsMessage(List<LoggingEvent> events, String message) {
        for(LoggingEvent event : events) {
            if((event.getMessage() != null) && (event.getMessage().toString().contains(message))) {
                return true;
            }
        }
        return false;
    }

    private void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException ignore) {
        }
    }
}
