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

package org.apache.stratos.messaging.domain.application;

import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ApplicationStatus implements LifeCycleState {

    Created(0) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ApplicationStatus.Created,
                    ApplicationStatus.Active, ApplicationStatus.Terminating));
        }
    },
    Active(1) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ApplicationStatus.Active,
                    ApplicationStatus.Terminating, ApplicationStatus.Inactive));
        }
    },
    Inactive(2) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ApplicationStatus.Inactive, ApplicationStatus.Active,
                    ApplicationStatus.Terminating));
        }
    },
    Terminating(3) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ApplicationStatus.Terminating,
                    ApplicationStatus.Terminated));
        }
    },
    Terminated(4) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ApplicationStatus.Terminated));
        }
    };

    private int code;

    private ApplicationStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
