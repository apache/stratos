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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.stratos.common.concurrent.locks.ReadWriteLock;
import org.apache.stratos.common.exception.InvalidLockRequestedException;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

/**
 * Read write lock test.
 */
public class ReadWriteLockTest {

    private static final Log log = LogFactory.getLog(ReadWriteLockTest.class);

    @Test
    public void testInvalidLock() {
        Exception exception = null;
        try {
            System.setProperty("read.write.lock.monitor.enabled", "true");

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
        final TestLogAppender testLogAppender = new TestLogAppender();
        Logger.getRootLogger().addAppender(testLogAppender);
        Logger.getRootLogger().setLevel(Level.DEBUG);

        System.setProperty("read.write.lock.monitor.enabled", "true");
        System.setProperty("read.write.lock.monitor.interval", "2000");
        System.setProperty("read.write.lock.timeout", "1500");

        ReadWriteLock lock = new ReadWriteLock("unrelased-lock-test");
        lock.acquireReadLock();
        sleep(5000);
        List<String> messages = testLogAppender.getMessages();
        assertTrue(containsMessage(messages, "System error, lock has not released"));
    }

    private boolean containsMessage(List<String> messages, String text) {
        for (String message : messages) {
            if ((message != null) && (message.contains(text))) {
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
