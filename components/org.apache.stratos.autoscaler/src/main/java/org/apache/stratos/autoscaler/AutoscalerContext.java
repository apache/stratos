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

import org.apache.stratos.autoscaler.monitor.application.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.AbstractClusterMonitor;

/**
 * It holds all cluster monitors which are active in stratos.
 */
public class AutoscalerContext {

    private static final AutoscalerContext INSTANCE = new AutoscalerContext();

    // Map<ClusterId, AbstractClusterMonitor>
    private Map<String, AbstractClusterMonitor> clusterMonitors;
    // Map<ApplicationId, ApplicationMonitor>
    private Map<String, ApplicationMonitor> applicationMonitors;

    private AutoscalerContext() {
        clusterMonitors = new HashMap<String, AbstractClusterMonitor>();
        applicationMonitors = new HashMap<String, ApplicationMonitor>();
    }

    public static AutoscalerContext getInstance() {
        return INSTANCE;
    }

    public void addClusterMonitor(AbstractClusterMonitor clusterMonitor) {
        clusterMonitors.put(clusterMonitor.getClusterId(), clusterMonitor);
    }

    public AbstractClusterMonitor getClusterMonitor(String clusterId) {
        return clusterMonitors.get(clusterId);
    }

    public AbstractClusterMonitor removeClusterMonitor(String clusterId) {
        return clusterMonitors.remove(clusterId);
    }

    public void addAppMonitor(ApplicationMonitor applicationMonitor) {
        applicationMonitors.put(applicationMonitor.getId(), applicationMonitor);
    }

    public ApplicationMonitor getAppMonitor(String applicationId) {
        return applicationMonitors.get(applicationId);
    }

    public void removeAppMonitor(String applicationId) {
        applicationMonitors.remove(applicationId);
    }
}
