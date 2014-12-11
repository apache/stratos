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

package org.apache.stratos.cloud.controller.iaases.mock.statistics;

import org.apache.stratos.cloud.controller.iaases.mock.MockAutoscalingFactor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Mock health statistics singleton class.
 */
public class MockHealthStatistics {
    private final static int DEFAULT_MEMORY_CONSUMPTION = 20;
    private final static int DEFAULT_LOAD_AVERAGE = 20;
    private final static int DEFAULT_REQUESTS_IN_FLIGHT = 1;

    private static volatile MockHealthStatistics instance;

    private Map<String, ReentrantReadWriteLock> lockMap;
    private Map<String, Map<String, Integer>> statisticsMap;

    private MockHealthStatistics() {
        lockMap = new ConcurrentHashMap<String, ReentrantReadWriteLock>();
        statisticsMap = new ConcurrentHashMap<String, Map<String, Integer>>();
    }

    public static MockHealthStatistics getInstance() {
        if (instance == null) {
            synchronized (MockHealthStatistics.class) {
                if (instance == null) {
                    instance = new MockHealthStatistics();
                }
            }
        }
        return instance;
    }

    public void acquireReadLock(String cartridgeType) {
        ReentrantReadWriteLock lock = getLock(cartridgeType);
        lock.readLock().lock();
    }

    public void acquireWriteLock(String cartridgeType) {
        ReentrantReadWriteLock lock = getLock(cartridgeType);
        lock.writeLock().lock();
    }

    public void releaseReadLock(String cartridgeType) {
        ReentrantReadWriteLock lock = getLock(cartridgeType);
        lock.readLock().unlock();
    }

    public void releaseWriteLock(String cartridgeType) {
        ReentrantReadWriteLock lock = getLock(cartridgeType);
        lock.writeLock().unlock();
    }

    private ReentrantReadWriteLock getLock(String cartridgeType) {
        ReentrantReadWriteLock lock = lockMap.get(cartridgeType);
        if(lock == null) {
            synchronized (MockHealthStatistics.class) {
                if(lock == null) {
                    lock = new ReentrantReadWriteLock();
                    lockMap.put(cartridgeType, lock);
                }
            }
        }
        return lock;
    }

    public void addStatistics(String cartridgeType, MockAutoscalingFactor autoscalingFactor, Integer value) {
        Map<String, Integer> factorValueMap = statisticsMap.get(cartridgeType);
        if(factorValueMap == null) {
            factorValueMap = new ConcurrentHashMap<String, Integer>();
            statisticsMap.put(cartridgeType, factorValueMap);
        }
        factorValueMap.put(autoscalingFactor.toString(), value);
    }

    public int getStatistics(String cartridgeType, MockAutoscalingFactor autoscalingFactor) {
        Map<String, Integer> factorValueMap = statisticsMap.get(cartridgeType);
        if((factorValueMap != null) && (factorValueMap.containsKey(autoscalingFactor.toString())) ){
            return factorValueMap.get(autoscalingFactor.toString());
        }
        return findDefault(autoscalingFactor);
    }

    private int findDefault(MockAutoscalingFactor autoscalingFactor) {
        if(autoscalingFactor == MockAutoscalingFactor.MemoryConsumption) {
            return DEFAULT_MEMORY_CONSUMPTION;
        } else if(autoscalingFactor == MockAutoscalingFactor.LoadAverage) {
            return DEFAULT_LOAD_AVERAGE;
        } else if(autoscalingFactor == MockAutoscalingFactor.RequestInFlight) {
            return DEFAULT_REQUESTS_IN_FLIGHT;
        }
        throw new RuntimeException("An unknown autoscaling factor found: " + autoscalingFactor);
    }
}
