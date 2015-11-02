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

package org.apache.stratos.messaging.domain.instance;

import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateManager;

import java.io.Serializable;
import java.util.Properties;

public abstract class Instance<T extends LifeCycleState> implements Serializable {

    // group/cluster level alias
    protected String alias;
    // instance id
    protected String instanceId;
    // instance properties
    protected Properties instanceProperties;
    // Life cycle state manager
    protected LifeCycleStateManager<T> lifeCycleStateManager;
    //Parent instance id
    private String parentId;
    //Network partition id
    private String networkPartitionId;
    //partition id
    protected String partitionId;


    public Instance(String alias, String instanceId) {
        this.alias = alias;
        this.instanceId = instanceId;
        this.instanceProperties = new Properties();
    }

    public void addProperty(String name, String value) {
        instanceProperties.put(name, value);
    }

    public String getProperty(String name) {
        return instanceProperties.getProperty(name);
    }

    public String getAlias() {
        return alias;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof Instance)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        Instance that = (Instance) other;
        return this.alias.equals(that.alias) &&
                this.instanceId.equals(that.instanceId);
    }

    public int hashCode() {
        return alias.hashCode() + instanceId.hashCode();
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getNetworkPartitionId() {
        return networkPartitionId;
    }

    public void setNetworkPartitionId(String networkPartitionId) {
        this.networkPartitionId = networkPartitionId;
    }

    public String getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(String partitionId) {
        this.partitionId = partitionId;
    }

}

