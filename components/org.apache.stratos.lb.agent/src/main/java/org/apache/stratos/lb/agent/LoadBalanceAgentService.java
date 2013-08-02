/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.lb.agent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.management.ManagementFactory;

/**
 * Agent to get the system properties of the instances. Hence will be used in auto scaling, and
 * also potentially in load balancing. Instances will be scaling up and down based on these params.
 */
public class LoadBalanceAgentService {

    private static final Log log = LogFactory.getLog(LoadBalanceAgentService.class);


    /**
     * gets the load average of the system
     *
     * @return load average. Returns zero if the load average couldn't be read.
     * Zero is treated as load-average not read, in the relevant places and ignored in the
     * load balancer and autoscalar algorithms appropriately.
     */
    public double getLoadAverage() {
        double systemLoadAverage = 0;
        try {
            systemLoadAverage =
                    ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Error in retrieving the load average of the instance");
            }
        }
        return systemLoadAverage;
    }
}
