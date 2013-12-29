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
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the load balancer statistics collector.
 */
public class LoadBalancerStatisticsCollector implements LoadBalancerStatisticsReader {
    private static final Log log = LogFactory.getLog(LoadBalancerStatisticsCollector.class);

    private static volatile LoadBalancerStatisticsCollector instance;
    // Map<ClusterId, Map<PartitionId, InFlightRequestCount>
    private Map<String, Integer> inFlightRequestCountMap;

    private LoadBalancerStatisticsCollector() {
        inFlightRequestCountMap = new ConcurrentHashMap<String, Integer>();
    }

    public static LoadBalancerStatisticsCollector getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerStatisticsCollector.class) {
                if (instance == null) {
                    if(log.isDebugEnabled()) {
                        log.debug("Load balancer in-flight request count collector instance created");
                    }
                    instance = new LoadBalancerStatisticsCollector();
                }
            }
        }
        return instance;
    }

    public int getInFlightRequestCount(String clusterId) {
        if (inFlightRequestCountMap.containsKey(clusterId)) {
            return inFlightRequestCountMap.get(clusterId);
        }
        return 0;
    }

    public void setInFlightRequestCount(String clusterId, int value) {
        if (StringUtils.isBlank(clusterId)) {
            return;
        }

        inFlightRequestCountMap.put(clusterId, value);
        if (log.isDebugEnabled()) {
            log.debug(String.format("In-flight request count updated: [cluster] %s [value] %d", clusterId, value));
        }
    }

    public void incrementInFlightRequestCount(String clusterId) {
        incrementInFlightRequestCount(clusterId, 1);
    }

    public void incrementInFlightRequestCount(String clusterId, int value) {
        if (StringUtils.isBlank(clusterId)) {
            return;
        }

        int count = getInFlightRequestCount(clusterId);
        setInFlightRequestCount(clusterId, (count + value));
    }

    public void decrementInFlightRequestCount(String clusterId) {
        decrementInFlightRequestCount(clusterId, 1);
    }

    public void decrementInFlightRequestCount(String clusterId, int value) {
        if (StringUtils.isBlank(clusterId)) {
            return;
        }

        int count = getInFlightRequestCount(clusterId);
        int newValue = (count - value) < 0 ? 0 : (count - value);
        setInFlightRequestCount(clusterId, newValue);
    }
}
