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
    // Map<ClusterId, Map<PartitionId, InFlightRequestCount>
    private Map<String, Vector<Date>> inFlightRequestToDateListMap;

    private LoadBalancerStatisticsCollector() {
        inFlightRequestToDateListMap = new ConcurrentHashMap<String, Vector<Date>>();
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

    public int getInFlightRequestCountOfSlidingWindow(String clusterId) {
        //Clear the list before returning...

        //Sliding window in Milliseconds
        int slidingWindow = 60000;//TODO get this from a config

        if (inFlightRequestToDateListMap.containsKey(clusterId)) {
            Vector<Date> vector = inFlightRequestToDateListMap.get(clusterId);
            Iterator<Date> itr = vector.iterator();
            while(itr.hasNext()){
                Date date = itr.next();
                Date currentDate = new Date();
                if((currentDate.getTime() - date.getTime()) > slidingWindow){ // we will remove the
                    itr.remove();
                } else {
                    //If the
                    break;
                }
            }
            return inFlightRequestToDateListMap.get(clusterId).size();
        }
        return 0;
    }

    public void addAnInFlightRequest(String clusterId) {
        if (StringUtils.isBlank(clusterId)) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cluster id is blank which try to set requests in flight" +
                        " : [cluster] %s", clusterId));
            }
            return;
        }
        if (!inFlightRequestToDateListMap.containsKey(clusterId)) {
            Vector<Date> list = inFlightRequestToDateListMap.get(clusterId);
            list.add(new Date());
            inFlightRequestToDateListMap.put(clusterId, list);
        } else {
            inFlightRequestToDateListMap.get(clusterId).add(new Date());
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("In-flight request added to counting list: [cluster] %s ", clusterId));
        }
    }


    public void removeAnInFlightRequest(String clusterId) {
        if (StringUtils.isBlank(clusterId)) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cluster id is blank which try to remove a requests in flight" +
                        " : [cluster] %s ", clusterId));
            }
            return;
        }
        if (!inFlightRequestToDateListMap.containsKey(clusterId)) {

            if (log.isDebugEnabled()) {
                log.debug(String.format("In-flight list not available for cluster : [cluster] %s ", clusterId));
            }
        } else {
            Vector<Date> vector = inFlightRequestToDateListMap.get(clusterId);
            vector.remove(vector.size() - 1);

            if (log.isDebugEnabled()) {
                log.debug(String.format("In-flight request removed from counting list: [cluster] %s ", clusterId));
            }
        }
    }

//    public void incrementInFlightRequestCount(String clusterId) {
//        incrementInFlightRequestCount(clusterId, 1);
//    }

//    private void incrementInFlightRequestCount(String clusterId, int value) {
//        if (StringUtils.isBlank(clusterId)) {
//            return;
//        }
//
//        int count = getInFlightRequestCount(clusterId);
//        addAnInFlightRequest(clusterId, (count + value));
//    }

//    public void decrementInFlightRequestCount(String clusterId) {
//        decrementInFlightRequestCount(clusterId, 1);
//    }

//    private void decrementInFlightRequestCount(String clusterId, int value) {
//        if (StringUtils.isBlank(clusterId)) {
//            return;
//        }
//
//        int count = getInFlightRequestCount(clusterId);
//        int newValue = (count - value) < 0 ? 0 : (count - value);
//        addAnInFlightRequest(clusterId, newValue);
//    }
}