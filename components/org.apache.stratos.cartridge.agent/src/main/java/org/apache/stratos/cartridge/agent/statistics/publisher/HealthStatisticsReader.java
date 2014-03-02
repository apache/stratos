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

package org.apache.stratos.cartridge.agent.statistics.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

import java.lang.management.ManagementFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * Health statistics reader.
 */
public class HealthStatisticsReader {
    private static final int MB = 1024 * 1024;
    private static final Log log = LogFactory.getLog(HealthStatisticsReader.class);

    public static double getMemoryConsumption() {
    	OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double totalMemory = (double)(osBean.getTotalPhysicalMemorySize()/ MB);
        double usedMemory = (double)((totalMemory - (osBean.getFreePhysicalMemorySize() / MB) ));
        
        if(log.isDebugEnabled()) {
        	log.debug("Calculating memory consumption: [totalMemory] "+totalMemory+" [usedMemory] "+usedMemory);
        }
        double memoryConsumption = (usedMemory / totalMemory) * 100;
        if(log.isDebugEnabled()) {
        	log.debug("Calculating memory consumption: [percentage] "+memoryConsumption);
        }
        return memoryConsumption;
    }

    public static double getLoadAverage() {
    	double loadAvg = (double)ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
    	// assume system cores = available cores to JVM
    	int cores = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    	
    	if(log.isDebugEnabled()) {
        	log.debug("Calculating load average consumption: [loadAverage] "+loadAvg+" [cores] "+cores);
        }
    	
        double loadAvgPercentage = (loadAvg/cores) * 100;
        if(log.isDebugEnabled()) {
        	log.debug("Calculating load average consumption: [percentage] "+loadAvgPercentage);
        }
		return loadAvgPercentage;
    }

    public static boolean allPortsActive() {
        return CartridgeAgentUtils.checkPortsActive("localhost", CartridgeAgentConfiguration.getInstance().getPorts());
    }
}