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
package org.apache.stratos.autoscaler.monitor;

import java.util.Map;

/**
 * Abstract class for the monitoring functionality in autoscaler.
 */
public abstract class Monitor implements EventHandler {
    //Id of the monitor, cluster=clusterId, group=group-alias, application=app-alias
    protected String id;
    //The parent app which this monitor relates to
    protected String appId;
    //Parent monitor of this monitor, for appMonitor parent will be none.
    protected ParentComponentMonitor parent;
    //monitors map, key=GroupAlias/clusterId and value=GroupMonitor/AbstractClusterMonitor
    protected Map<String, Monitor> aliasToActiveMonitorsMap;
    //monitors map, stopped monitors
    protected Map<String, Monitor> aliasToInactiveMonitorsMap;
    //flag will get set to true in MonitorTerminateAllEvent when termination of
    // this monitor decided by its parent
    protected boolean terminateChildren = false;

    protected boolean hasDependent;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public Map<String, Monitor> getAliasToActiveMonitorsMap() {
        return aliasToActiveMonitorsMap;
    }

    public void setAliasToActiveMonitorsMap(Map<String, Monitor> aliasToActiveMonitorsMap) {
        this.aliasToActiveMonitorsMap = aliasToActiveMonitorsMap;
    }

    public ParentComponentMonitor getParent() {
        return parent;
    }

    public void setParent(ParentComponentMonitor parent) {
        this.parent = parent;
        this.appId = parent.getAppId();
    }

    public boolean hasActiveMonitors() {
        boolean hasMonitor = false;
        if ((this.aliasToActiveMonitorsMap != null && !this.aliasToActiveMonitorsMap.isEmpty()) ||
                (this.aliasToInactiveMonitorsMap != null && !this.aliasToInactiveMonitorsMap.isEmpty())) {
            hasMonitor = true;
        }
        return hasMonitor;
    }

    public boolean hasMonitors() {

        return this.aliasToActiveMonitorsMap != null || this.aliasToInactiveMonitorsMap != null;
    }

    public boolean isDependent() {
        return hasDependent;
    }

    public void setHasDependent(boolean hasDependent) {
        this.hasDependent = hasDependent;
    }

    public boolean hasInDependentChild() {
        boolean hasInDepChild = false;
        for (Monitor monitor : this.aliasToActiveMonitorsMap.values()) {
            if (!monitor.isDependent()) {
                hasInDepChild = true;
                break;
            }
        }
        return hasInDepChild;
    }

    public Map<String, Monitor> getAliasToInActiveMonitorsMap() {
        return this.aliasToInactiveMonitorsMap;
    }

    public void setAliasToInActiveMonitorsMap(Map<String, Monitor> aliasToInActiveMonitorsMap) {
        this.aliasToInactiveMonitorsMap = aliasToInActiveMonitorsMap;
    }
}
