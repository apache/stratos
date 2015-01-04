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
package org.apache.stratos.autoscaler.context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.registry.RegistryManager;

/**
 * It holds all cluster monitors which are active in stratos.
 */
public class AutoscalerContext {

    private static final AutoscalerContext INSTANCE = new AutoscalerContext();

    // Map<ApplicationId, ApplicationContext>
    private Map<String, ApplicationContext> applicationContextMap;
    // Map<ClusterId, AbstractClusterMonitor>
    private Map<String, ClusterMonitor> clusterMonitors;
    // Map<ApplicationId, ApplicationMonitor>
    private Map<String, ApplicationMonitor> applicationMonitors;
    //pending application monitors
    private List<String> pendingApplicationMonitors;

    private AutoscalerContext() {
        applicationContextMap = readApplicationContextsFromRegistry();
        if(applicationContextMap == null) {
            applicationContextMap = new ConcurrentHashMap<String, ApplicationContext>();
        }
        setClusterMonitors(new HashMap<String, ClusterMonitor>());
        setApplicationMonitors(new HashMap<String, ApplicationMonitor>());
        pendingApplicationMonitors = new ArrayList<String>();
    }

    private Map<String, ApplicationContext> readApplicationContextsFromRegistry() {
        String[] resourcePaths = RegistryManager.getInstance().getApplicationContextResourcePaths();
        if((resourcePaths == null) || (resourcePaths.length == 0)) {
            return null;
        }

        Map<String, ApplicationContext> applicationContextMap = new ConcurrentHashMap<String, ApplicationContext>();
        for(String resourcePath : resourcePaths) {
            ApplicationContext applicationContext = RegistryManager.getInstance().getApplicationContextByResourcePath(resourcePath);
            applicationContextMap.put(applicationContext.getApplicationId(), applicationContext);
        }
        return applicationContextMap;
    }

    public static AutoscalerContext getInstance() {
        return INSTANCE;
    }

    public void addClusterMonitor(ClusterMonitor clusterMonitor) {
        getClusterMonitors().put(clusterMonitor.getClusterId(), clusterMonitor);
    }

    public ClusterMonitor getClusterMonitor(String clusterId) {
        return getClusterMonitors().get(clusterId);
    }

    public ClusterMonitor removeClusterMonitor(String clusterId) {
        return getClusterMonitors().remove(clusterId);
    }

    public void addAppMonitor(ApplicationMonitor applicationMonitor) {
        getApplicationMonitors().put(applicationMonitor.getId(), applicationMonitor);
    }

    public ApplicationMonitor getAppMonitor(String applicationId) {
        return getApplicationMonitors().get(applicationId);
    }

    public void removeAppMonitor(String applicationId) {
        getApplicationMonitors().remove(applicationId);
    }

    public Map<String, ClusterMonitor> getClusterMonitors() {
        return clusterMonitors;
    }

    public void setClusterMonitors(Map<String, ClusterMonitor> clusterMonitors) {
        this.clusterMonitors = clusterMonitors;
    }

    public Map<String, ApplicationMonitor> getApplicationMonitors() {
        return applicationMonitors;
    }

    public void setApplicationMonitors(Map<String, ApplicationMonitor> applicationMonitors) {
        this.applicationMonitors = applicationMonitors;
    }

    public List<String> getPendingApplicationMonitors() {
        return pendingApplicationMonitors;
    }

    public void setPendingApplicationMonitors(List<String> pendingApplicationMonitors) {
        this.pendingApplicationMonitors = pendingApplicationMonitors;
    }

    public void addPendingMonitor(String appId) {
        this.pendingApplicationMonitors.add(appId);
    }

    public void removeFromPendingMonitors(String appId) {
        this.pendingApplicationMonitors.remove(appId);
    }

    public boolean containsPendingMonitor(String appId) {
        return this.pendingApplicationMonitors.contains(appId);
    }

    public boolean monitorExists(String appId) {
        return this.applicationMonitors.containsKey(appId);
    }

    public void addApplicationContext(ApplicationContext applicationContext) {
        applicationContextMap.put(applicationContext.getApplicationId(), applicationContext);
        RegistryManager.getInstance().persistApplicationContext(applicationContext);
    }

    public void removeApplicationContext(String applicationId) {
        if(applicationContextMap.containsKey(applicationId)) {
            applicationContextMap.remove(applicationId);
            RegistryManager.getInstance().removeApplicationContext(applicationId);
        }
    }

    public ApplicationContext getApplicationContext(String applicationId) {
        if(applicationContextMap.containsKey(applicationId)) {
            return applicationContextMap.get(applicationId);
        }
        return null;
    }

    public Collection<ApplicationContext> getApplicationContexts() {
        return applicationContextMap.values();
    }

    public void updateApplicationContext(ApplicationContext applicationContext) {
        applicationContextMap.put(applicationContext.getApplicationId(), applicationContext);
        RegistryManager.getInstance().persistApplicationContext(applicationContext);
    }
}
