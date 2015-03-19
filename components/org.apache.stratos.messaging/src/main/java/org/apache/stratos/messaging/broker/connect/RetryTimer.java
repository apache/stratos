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

package org.apache.stratos.messaging.broker.connect;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Retry timer for generating a time value in an increasing mode.
 */
public class RetryTimer {

    private static final List<Integer> DEFAULT_VALUE_LIST = Arrays.asList(2000, 2000, 5000, 5000, 10000, 10000, 20000,
            20000, 30000, 30000, 40000, 40000, 50000, 50000, 60000);

    private final List<Integer> valueList;
    private final Iterator<Integer> iterator;

    public RetryTimer() {
        this.valueList = DEFAULT_VALUE_LIST;
        this.iterator = valueList.iterator();
    }

    public RetryTimer(List<Integer> valueList) {
        this.valueList = valueList;
        this.iterator = valueList.iterator();
    }

    public synchronized long getNextInterval() {
        if (iterator.hasNext()) {
            // Return next value
            return iterator.next();
        }
        // Return last value
        return valueList.get(valueList.size() - 1);
    }
}
