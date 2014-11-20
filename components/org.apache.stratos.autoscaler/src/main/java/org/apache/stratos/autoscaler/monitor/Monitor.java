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
    //has startup dependents
    protected boolean hasStartupDependents;
    //has scaling dependents
    protected boolean hasScalingDependents;

    /**
     * Return the id of the monitor
     *
     * @return id
     */
    public String getId() {
        return id;
    }

    /**
     * Set the id of the monitor
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * To get the appId of the monitor
     *
     * @return app id
     */
    public String getAppId() {
        return appId;
    }

    /**
     * To set the app id of the monitor
     *
     * @param appId
     */
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * To get the parent of the monitor
     *
     * @return the parent
     */
    public ParentComponentMonitor getParent() {
        return parent;
    }

    /**
     * To set the parent of the monitor
     *
     * @param parent
     */
    public void setParent(ParentComponentMonitor parent) {
        this.parent = parent;
        this.appId = parent.getAppId();
    }

    /**
     * Return whether this monitor has startup dependencies
     *
     * @return hasStartupDependents
     */
    public boolean hasStartupDependents() {
        return hasStartupDependents;
    }

    /**
     * Return whether this monitor has scaling dependencies
     *
     * @return startup dependencies exist or not
     */
    public boolean hasScalingDependents() {
        return hasScalingDependents;
    }

    /**
     * To set whether monitor has any startup dependencies
     *
     * @param hasDependent
     */
    public void setHasStartupDependents(boolean hasDependent) {
        this.hasStartupDependents = hasDependent;
    }

    /**
     * To set whether monitor has any scaling dependencies
     *
     * @param hasDependent
     */
    public void setHasScalingDependents(boolean hasDependent) {
        this.hasScalingDependents = hasDependent;
    }
}
