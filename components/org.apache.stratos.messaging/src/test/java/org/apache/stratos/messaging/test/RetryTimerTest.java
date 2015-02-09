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

package org.apache.stratos.messaging.test;

import org.apache.stratos.messaging.broker.connect.RetryTimer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Retry timer test.
 */
public class RetryTimerTest {

    @Test
    public void testNextValue() {
        List<Integer> valueList = Arrays.asList(1000, 1000, 2000, 2000, 5000, 5000, 10000, 10000, 20000, 20000,
                30000, 30000, 40000, 40000, 50000, 50000, 60000);
        RetryTimer retryTimer = new RetryTimer(valueList);

        long value = retryTimer.getNextInterval();
        assertEquals(1000, value);

        value = retryTimer.getNextInterval();
        assertEquals(1000, value);

        value = retryTimer.getNextInterval();
        assertEquals(2000, value);

        value = retryTimer.getNextInterval();
        assertEquals(2000, value);

        value = retryTimer.getNextInterval();
        assertEquals(5000, value);

        value = retryTimer.getNextInterval();
        assertEquals(5000, value);

        value = retryTimer.getNextInterval();
        assertEquals(10000, value);

        value = retryTimer.getNextInterval();
        assertEquals(10000, value);

        value = retryTimer.getNextInterval();
        assertEquals(20000, value);

        value = retryTimer.getNextInterval();
        assertEquals(20000, value);

        value = retryTimer.getNextInterval();
        assertEquals(30000, value);

        value = retryTimer.getNextInterval();
        assertEquals(30000, value);

        value = retryTimer.getNextInterval();
        assertEquals(40000, value);

        value = retryTimer.getNextInterval();
        assertEquals(40000, value);

        value = retryTimer.getNextInterval();
        assertEquals(50000, value);

        value = retryTimer.getNextInterval();
        assertEquals(50000, value);

        value = retryTimer.getNextInterval();
        assertEquals(60000, value);

        value = retryTimer.getNextInterval();
        assertEquals(60000, value);

        value = retryTimer.getNextInterval();
        assertEquals(60000, value);

        value = retryTimer.getNextInterval();
        assertEquals(60000, value);
    }
}
