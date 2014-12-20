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
 * This event is fired by Event processing engine to send gradient of  memory consumption
 */
public class MemberGradientOfMemoryConsumptionEvent extends Event {

    private final String clusterInstanceId;
    private final String memberId;
    private final float value;

    public MemberGradientOfMemoryConsumptionEvent(String clusterInstanceId, String memberId, float value) {
        this.clusterInstanceId = clusterInstanceId;
        this.memberId = memberId;
        this.value = value;
    }


    public String getMemberId() {
        return memberId;
    }

    public float getValue() {
        return value;
    }

    public String getClusterInstanceId() {
        return clusterInstanceId;
    }
}
