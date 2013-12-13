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
package org.apache.stratos.load.balancer.statistics.observers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.common.statistics.WSO2CEPInFlightRequestPublisher;

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

public class WSO2CEPInFlightRequestCountObserver implements Observer {
    private static final Log log = LogFactory.getLog(WSO2CEPInFlightRequestCountObserver.class);
    private WSO2CEPInFlightRequestPublisher publisher;
    private String networkPartitionId;

    public WSO2CEPInFlightRequestCountObserver() {
        this.publisher = new WSO2CEPInFlightRequestPublisher();
        networkPartitionId = System.getProperty("network.partition.id");
        if (StringUtils.isBlank(networkPartitionId)) {
            throw new RuntimeException("network.partition.id system property was not found.");
        }
    }

    public void update(Observable observable, Object object) {
        try {
            if (publisher.isEnabled()) {
                // Map<ClusterId, Count>
                Map<String, Integer> inFlightRequestCountMap = (Map<String, Integer>) object;
                // Publish event per cluster id
                for (String clusterId : inFlightRequestCountMap.keySet()) {
                    // Publish event
                    publisher.publish(clusterId, networkPartitionId, inFlightRequestCountMap.get(clusterId));
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("In-flight request count published to cep: [cluster-id] %s [network-partition] %s [value] %d",
                                clusterId, networkPartitionId, inFlightRequestCountMap.get(clusterId)));
                    }
                }
            } else if (log.isWarnEnabled()) {
                log.warn("CEP statistics publisher is disabled");
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Could not publish in-flight request count to cep", e);
            }
        }
    }
}
