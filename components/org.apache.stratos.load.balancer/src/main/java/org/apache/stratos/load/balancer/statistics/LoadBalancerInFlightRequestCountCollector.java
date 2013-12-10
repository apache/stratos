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
package org.apache.stratos.load.balancer.statistics;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.statistics.observers.WSO2CEPInFlightRequestCountObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the load balancing in-flight request count collector and any observer can get registered here
 * and receive notifications periodically.
 * This is a Singleton object.
 *
 * @author nirmal
 */
public class LoadBalancerInFlightRequestCountCollector extends Observable {
    private static final Log log = LogFactory.getLog(LoadBalancerInFlightRequestCountCollector.class);

    private static LoadBalancerInFlightRequestCountCollector collector;
    // Map<ClusterId, Map<PartitionId, InFlightRequestCount>
    private Map<String, Map<String, Integer>> inFlightRequestCountMap;
    private Thread notifier;

    private LoadBalancerInFlightRequestCountCollector() {
        inFlightRequestCountMap = new ConcurrentHashMap<String, Map<String, Integer>>();
        if (notifier == null || (notifier != null && !notifier.isAlive())) {
            notifier = new Thread(new ObserverNotifier());
            notifier.start();
        }
    }

    public static LoadBalancerInFlightRequestCountCollector getInstance() {
        if (collector == null) {
            synchronized (LoadBalancerInFlightRequestCountCollector.class) {
                if (collector == null) {
                    collector = new LoadBalancerInFlightRequestCountCollector();
                    // add observers
                    collector.addObserver(new WSO2CEPInFlightRequestCountObserver());
                }
            }
        }
        return collector;
    }

    public int getInFlightRequestCount(String clusterId, String partitionId) {
        if (StringUtils.isBlank(clusterId) || StringUtils.isBlank(partitionId)) {
            return -1;
        }

        Map<String, Integer> partitionMap = inFlightRequestCountMap.get(clusterId);
        if (partitionMap == null) {
            return 0;
        }
        if (partitionMap.containsKey(partitionId)) {
            return partitionMap.get(partitionId);
        }
        return 0;
    }

    public void setInFlightRequestCount(String clusterId, String partitionId, int value) {
        if (StringUtils.isBlank(clusterId) || StringUtils.isBlank(partitionId)) {
            return;
        }

        Map<String, Integer> partitionMap = inFlightRequestCountMap.get(clusterId);
        if (partitionMap == null) {
            partitionMap = new HashMap<String, Integer>();
            inFlightRequestCountMap.put(clusterId, partitionMap);
        }
        partitionMap.put(partitionId, value);
        if(log.isDebugEnabled()) {
            log.debug(String.format("In-flight request count updated: [cluster] %s [partition] $s [value] %d", clusterId, partitionId, value));
        }
        setChanged();
    }

    public void incrementInFlightRequestCount(String clusterId, String partitionId) {
        incrementInFlightRequestCount(clusterId, partitionId, 1);
    }

    public void incrementInFlightRequestCount(String clusterId, String partitionId, int value) {
        if (StringUtils.isBlank(clusterId) || StringUtils.isBlank(partitionId)) {
            return;
        }

        int count = getInFlightRequestCount(clusterId, partitionId);
        setInFlightRequestCount(clusterId, partitionId, (count + value));
    }

    public void decrementInFlightRequestCount(String clusterId, String partitionId) {
        decrementInFlightRequestCount(clusterId, partitionId, 1);
    }

    public void decrementInFlightRequestCount(String clusterId, String partitionId, int value) {
        if (StringUtils.isBlank(clusterId) || StringUtils.isBlank(partitionId)) {
            return;
        }

        int count = getInFlightRequestCount(clusterId, partitionId);
        int newValue = (count - value) < 0 ? 0 : (count - value);
        setInFlightRequestCount(clusterId, partitionId, newValue);
    }


    /**
     * This thread will notify all the observers of this subject.
     *
     * @author nirmal
     */
    private class ObserverNotifier implements Runnable {

        @Override
        public void run() {
            if (log.isInfoEnabled()) {
                log.info("Load balancing statistics notifier thread started");
            }
            while (true) {
                try {
                    Thread.sleep(15000);
                } catch (InterruptedException ignore) {
                }
                LoadBalancerInFlightRequestCountCollector.getInstance().notifyObservers(new HashMap<String, Map<String, Integer>>(inFlightRequestCountMap));
            }
        }
    }
}
