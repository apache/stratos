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

import com.sun.management.OperatingSystemMXBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;

import java.lang.management.ManagementFactory;
import java.io.IOException;

/**
 * Health statistics reader.
 */
public class HealthStatisticsReader implements IHealthStatisticsReader {

    private static final int MB = 1024 * 1024;
    private static final Log log = LogFactory.getLog(HealthStatisticsReader.class);

    public boolean init() {
        return true;
    }

    public CartridgeStatistics getCartridgeStatistics() throws IOException {
    	OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        double totalMemory = (double)(osBean.getTotalPhysicalMemorySize()/ MB);
        double usedMemory = (double)((totalMemory - (osBean.getFreePhysicalMemorySize() / MB) ));
        double loadAvg = (double)osBean.getSystemLoadAverage();
        // assume system cores = available cores to JVM
        int cores = osBean.getAvailableProcessors();
        double memoryConsumption = (usedMemory / totalMemory) * 100;
        double loadAvgPercentage = (loadAvg/cores) * 100;

        if(log.isDebugEnabled()) {
            log.debug("Memory consumption: [totalMemory] "+totalMemory+"Mb [usedMemory] "+usedMemory+"Mb: "+memoryConsumption+"%");
            log.debug("Processor consumption: [loadAverage] "+loadAvg+" [cores] "+cores+": "+loadAvgPercentage+"%");
        }
    	
        return (new CartridgeStatistics(memoryConsumption, loadAvgPercentage));
    }

    public static boolean allPortsActive() {
        return CartridgeAgentUtils.checkPortsActive(CartridgeAgentConfiguration.getInstance().getListenAddress(),
                                                    CartridgeAgentConfiguration.getInstance().getPorts());
    }

    public void delete() {
    }
}
