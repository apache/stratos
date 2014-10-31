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

package org.apache.stratos.messaging.domain.applications;

import org.apache.stratos.messaging.domain.topology.LifeCycleStateTransitionBehavior;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateManager;

import java.util.Stack;

/**
 * Represents a Group/nested Group in an Application/Group
 */

public class Group extends ParentComponent implements LifeCycleStateTransitionBehavior<GroupStatus> {

    private static final long serialVersionUID = 8347096598203655846L;
    // Name of the Group, specified in Group Definition
    private String name;
    // Group alias
    private String alias;
    // Group deployment policy
    private String deploymentPolicy;
    // Group level autoscaling policy
    private String autoscalingPolicy;
    // application id
    private String applicationId;
    // Life cycle state manager
    protected LifeCycleStateManager<GroupStatus> groupStateManager;

    public Group (String applicationId, String name, String alias) {
        super();
        this.applicationId = applicationId;
        this.name = name;
        this.alias = alias;
        this.groupStateManager = new LifeCycleStateManager<GroupStatus>(GroupStatus.Created, alias);
    }

    public String getUniqueIdentifier() {
        return alias;
    }

    public String getAlias() {
        return alias;
    }

    public String getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(String deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public String getAutoscalingPolicy() {
        return autoscalingPolicy;
    }

    public void setAutoscalingPolicy(String autoscalingPolicy) {
        this.autoscalingPolicy = autoscalingPolicy;
    }

    @Override
    public boolean isStateTransitionValid(GroupStatus newState) {
        return groupStateManager.isStateTransitionValid(newState);
    }

    @Override
    public Stack<GroupStatus> getTransitionedStates() {
        return groupStateManager.getStateStack();
    }

    @Override
    public GroupStatus getStatus() {
        return groupStateManager.getCurrentState();
    }

    @Override
    public void setStatus(GroupStatus newState) {
        this.groupStateManager.changeState(newState);
    }

    public boolean equals(Object other) {
        if(other == null || !(other instanceof Group)) {
            return false;
        }

        if(this == other) {
            return true;
        }

        Group that = (Group)other;
        return this.name.equals(that.name) && this.alias.equals(that.alias);
    }

    public int hashCode () {
        return name.hashCode() + alias.hashCode();
    }

    public String getApplicationId() {
        return applicationId;
    }
}
