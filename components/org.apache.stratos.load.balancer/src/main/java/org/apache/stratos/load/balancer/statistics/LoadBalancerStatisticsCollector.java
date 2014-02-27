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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is the load balancer statistics collector.
 */
public class LoadBalancerStatisticsCollector implements LoadBalancerStatisticsReader {
    private static final Log log = LogFactory.getLog(LoadBalancerStatisticsCollector.class);

    private static volatile LoadBalancerStatisticsCollector instance;
    // Map<ClusterId, ArrayList<Date>>
    private Map<String, List<Date>> inFlightRequestToDateListMap;

    private LoadBalancerStatisticsCollector() {
        inFlightRequestToDateListMap = new ConcurrentHashMap<String, List<Date>>();
    }

    public static LoadBalancerStatisticsCollector getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerStatisticsCollector.class) {
                if (instance == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Load balancer in-flight request count collector instance created");
                    }
                    instance = new LoadBalancerStatisticsCollector();
                }
            }
        }
        return instance;
    }

    public int getInFlightRequestCountOfSlidingWindow(String clusterId) {
        synchronized (LoadBalancerStatisticsCollector.class) {
            // Sliding window in milliseconds
            int slidingWindow = 10000; // TODO Move this to loadbalancer.conf

            if (inFlightRequestToDateListMap.containsKey(clusterId)) {
                List<Date> dateList = inFlightRequestToDateListMap.get(clusterId);
                List<Date> updatedList = Collections.synchronizedList(new ArrayList<Date>());
                Date currentDate = new Date();
                long slidingWindStart = currentDate.getTime() - slidingWindow;
                int count = 0;
                for (Date date : dateList) {
                    if (date.getTime() > slidingWindStart) {
                        count++;
                    }
                    else {
                        updatedList.add(date);
                    }
                }
                // Remove dates counted
                inFlightRequestToDateListMap.put(clusterId, updatedList);
                return count;
            }
            return 0;
        }
    }

    void incrementInFlightRequestCount(String clusterId) {
        synchronized (LoadBalancerStatisticsCollector.class) {
            if (StringUtils.isBlank(clusterId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cluster id is null, could not increment in-flight request count");
                }
                return;
            }
            List<Date> dateList;
            if (inFlightRequestToDateListMap.containsKey(clusterId)) {
                dateList = inFlightRequestToDateListMap.get(clusterId);
            } else {
                dateList = Collections.synchronizedList(new ArrayList<Date>());
                inFlightRequestToDateListMap.put(clusterId, dateList);
            }
            // Add current date to cluster date list
            dateList.add(new Date());
            if (log.isDebugEnabled()) {
                log.debug(String.format("In-flight request count incremented: [cluster] %s [count] %s ", clusterId,
                        dateList.size()));

            }
        }
    }

    void decrementInFlightRequestCount(String clusterId) {
        synchronized (LoadBalancerStatisticsCollector.class) {
            if (StringUtils.isBlank(clusterId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cluster id is null, could not decrement in-flight request count");
                }
                return;
            }

            if (!inFlightRequestToDateListMap.containsKey(clusterId)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("In-flight request date list not found for cluster: [cluster] %s ", clusterId));
                }
            } else {
                List<Date> dateList = inFlightRequestToDateListMap.get(clusterId);
                if (!dateList.isEmpty()) {
                    int index = dateList.size() - 1;
                    if (index >= 0) {
                        dateList.remove(index);
                    }
                }

                if (log.isDebugEnabled()) {
                    log.debug(String.format("In-flight request count decremented: [cluster] %s [count] %s ", clusterId,
                            dateList.size()));
                }
            }
        }
    }
}