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

import org.apache.stratos.messaging.domain.application.ApplicationStatus;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateTransitionBehavior;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateManager;

import java.util.Stack;

public class ApplicationInstance extends Instance<ApplicationStatus> implements LifeCycleStateTransitionBehavior<ApplicationStatus> {

    public ApplicationInstance(String alias, String instanceId) {
        super(alias, instanceId);
        this.lifeCycleStateManager = new LifeCycleStateManager<ApplicationStatus>(ApplicationStatus.Created,
                alias + "_" + instanceId);
    }

    @Override
    public boolean isStateTransitionValid(ApplicationStatus newState) {
        return lifeCycleStateManager.isStateTransitionValid(newState);
    }

    @Override
    public Stack<ApplicationStatus> getTransitionedStates() {
        return lifeCycleStateManager.getStateStack();
    }

    @Override
    public ApplicationStatus getStatus() {
        return lifeCycleStateManager.getCurrentState();
    }

    @Override
    public boolean setStatus(ApplicationStatus newState) {
        return this.lifeCycleStateManager.changeState(newState);
    }
}
