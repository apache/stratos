/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.autoscaler;

import org.apache.stratos.autoscaler.policy.model.LoadAverage;
import org.apache.stratos.autoscaler.policy.model.MemoryConsumption;

/**
 * This class will keep additional parameters such as load average and memory consumption
 */

public class MemberStatsContext {
    private LoadAverage loadAverage;
    private MemoryConsumption memoryConsumption;
    private String memberId;

    public MemberStatsContext(String memberId){
        this.memberId = memberId;
        memoryConsumption = new MemoryConsumption();
        loadAverage =  new LoadAverage();
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }


    public LoadAverage getLoadAverage() {
        return loadAverage;
    }
//
//    public void setLoadAverage(LoadAverage loadAverage) {
//        this.loadAverage = loadAverage;
//    }

    public MemoryConsumption getMemoryConsumption() {
        return memoryConsumption;
    }
//
//    public void setMemoryConsumption(MemoryConsumption memoryConsumption) {
//        this.memoryConsumption = memoryConsumption;
//    }
}
