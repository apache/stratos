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

import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.stratos.load.balancer.common.statistics.WSO2CEPStatsPublisher;

public class WSO2CEPStatsObserver implements Observer{
    private WSO2CEPStatsPublisher statsPublisher;

    public WSO2CEPStatsObserver() {
        this.statsPublisher = new WSO2CEPStatsPublisher();
    }

    public void update(Observable arg0, Object arg1) {
        if(arg1 != null && arg1 instanceof Map<?, ?>) {
            Map<String, Integer> stats = (Map<String, Integer>)arg1;
            statsPublisher.publish(stats);
        }
    }
}
