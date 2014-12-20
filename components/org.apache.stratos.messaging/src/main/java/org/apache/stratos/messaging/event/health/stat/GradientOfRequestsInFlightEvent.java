/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.event.health.stat;

import org.apache.stratos.messaging.event.Event;

/**
 * This event is fired by Event processing engine to send gradient of requests in flight
 */
public class GradientOfRequestsInFlightEvent extends Event {
    private final String networkPartitionId;
    private final String clusterId;
    private final String clusterInstanceId;
    private final float value;

    public GradientOfRequestsInFlightEvent(String networkPartitionId, String clusterId, String clusterInstanceId, float value) {
        this.networkPartitionId = networkPartitionId;
        this.clusterId = clusterId;
        this.clusterInstanceId = clusterInstanceId;
        this.value = value;
    }


    public String getClusterId() {
        return clusterId;
    }

    public float getValue() {
        return value;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }
}
