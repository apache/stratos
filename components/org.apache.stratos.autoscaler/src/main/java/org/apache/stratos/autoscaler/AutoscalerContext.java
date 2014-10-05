/*
 *
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
 *
 */
package org.apache.stratos.autoscaler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.monitor.AbstractClusterMonitor;

/**
 * It holds all cluster monitors which are active in stratos.
 */
public class AutoscalerContext {

    private static final Log log = LogFactory.getLog(AutoscalerContext.class);
    private static final AutoscalerContext INSTANCE = new AutoscalerContext();

    private AutoscalerContext() {
        try {
            setClusterMonitors(new HashMap<String, AbstractClusterMonitor>());
        } catch (Exception e) {
            log.error("Rule evaluateMinCheck error", e);
        }
    }

    // Map<ClusterId, AbstractClusterMonitor>
    private Map<String, AbstractClusterMonitor> clusterMonitors;

    public static AutoscalerContext getInstance() {
        return INSTANCE;
    }

    public void addClusterMonitor(AbstractClusterMonitor clusterMonitor) {
        clusterMonitors.put(clusterMonitor.getClusterId(), clusterMonitor);
    }

    public AbstractClusterMonitor getClusterMonitor(String clusterId) {
        return clusterMonitors.get(clusterId);
    }

    public Map<String, AbstractClusterMonitor> getClusterMonitors() {
        return clusterMonitors;
    }

    public void setClusterMonitors(Map<String, AbstractClusterMonitor> clusterMonitors) {
        this.clusterMonitors = clusterMonitors;
    }

    public AbstractClusterMonitor removeClusterMonitor(String clusterId) {

        AbstractClusterMonitor monitor = clusterMonitors.remove(clusterId);
        if (monitor == null) {
            log.fatal("ClusterMonitor not found for cluster id: " + clusterId);
        } else {
            log.info("Removed ClusterMonitor [cluster id]: " + clusterId);
        }
        return monitor;
    }
}
