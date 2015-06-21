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

import org.apache.stratos.autoscaler.monitor.component.ParentComponentMonitor;
import org.apache.stratos.messaging.domain.instance.Instance;

import java.util.*;

/**
 * Abstract class for the monitoring functionality in Autoscaler.
 */
public abstract class Monitor implements EventHandler, Runnable {
    //Monitor types
    public enum MonitorType {
        Application, Group, Cluster
    }

    //Id of the monitor, cluster=clusterId, group=group-alias, application=app-alias
    protected String id;
    //The parent app which this monitor relates to
    protected String appId;
    //Parent monitor of this monitor, for appMonitor parent will be none.
    protected ParentComponentMonitor parent;
    //has startup dependents
    protected boolean hasStartupDependents;
    //monitors map, key=InstanceId and value=ClusterInstance/GroupInstance/ApplicationInstance
    protected Map<String, Instance> instanceIdToInstanceMap;

    public Monitor() {
        this.instanceIdToInstanceMap = new HashMap<String, Instance>();
    }

    /**
     * Implement this method to destroy the monitor thread
     */
    public abstract void destroy();

    /**
     * This will monitor the network partition context with child notifications
     */
    public abstract void monitor();

    /**
     * This will create Instance on demand as requested by monitors
     *
     * @param instanceId instance Id of the instance to be created
     * @return whether it is created or not
     */
    public abstract boolean createInstanceOnDemand(String instanceId);

    /**
     * Return the id of the monitor
     *
     * @return id identifier of the monitor
     */
    public String getId() {
        return id;
    }

    /**
     * Return the type of the monitor.
     *
     * @return monitor type
     */
    public abstract MonitorType getMonitorType();

    /**
     * Set the id of the monitor
     *
     * @param id id of the monitor
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * To get the appId of the monitor
     *
     * @return application id of the monitor
     */
    public String getAppId() {
        return appId;
    }

    /**
     * To set the app id of the monitor
     *
     * @param appId application id
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
     * @param parent parent of the monitor
     */
    public void setParent(ParentComponentMonitor parent) {
        this.parent = parent;
        this.appId = parent.getAppId();
    }

    /**
     * Return whether this monitor has startup dependencies
     *
     * @return whether the monitor has startup dependents
     */
    public boolean hasStartupDependents() {
        return hasStartupDependents;
    }

    /**
     * To set whether monitor has any startup dependencies
     *
     * @param hasDependent whether monitor has dependent or not
     */
    public void setHasStartupDependents(boolean hasDependent) {
        hasStartupDependents = hasDependent;
    }

    /**
     * This will add the instance
     *
     * @param instance instance to be added
     */
    public void addInstance(Instance instance) {
        instanceIdToInstanceMap.put(instance.getInstanceId(), instance);

    }

    /**
     * Using instanceId, instance can be retrieved
     *
     * @param instanceId instance id
     * @return the instance
     */
    public Instance getInstance(String instanceId) {
        return instanceIdToInstanceMap.get(instanceId);
    }

    /**
     * Return the instances count
     *
     * @return number of exiting instances
     */
    public int getInstanceCount() {
        return instanceIdToInstanceMap.size();
    }

    /**
     * Return the instances
     *
     * @return exiting instances
     */
    public Collection<Instance> getInstances() {
        return instanceIdToInstanceMap.values();
    }

    /**
     * This will remove the instance
     *
     * @param instanceId instance id
     */
    public void removeInstance(String instanceId) {
        instanceIdToInstanceMap.remove(instanceId);
    }

    /**
     * This will return all the instances which has the same parent id as given
     *
     * @param parentInstanceId parent instance id
     * @return all the instances
     */
    public List<String> getInstancesByParentInstanceId(String parentInstanceId) {
        List<String> instances = new ArrayList<String>();
        for (Instance instance : instanceIdToInstanceMap.values()) {
            if (instance.getParentId() != null && instance.getParentId().equals(parentInstanceId)) {
                instances.add(instance.getInstanceId());
            }
        }
        return instances;
    }

    /**
     * This will check whether instances are there in the map
     *
     * @return true/false
     */
    public boolean hasInstance() {
        return !instanceIdToInstanceMap.isEmpty();
    }
}
