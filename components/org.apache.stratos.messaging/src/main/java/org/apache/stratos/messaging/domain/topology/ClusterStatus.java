/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.domain.topology;

import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum ClusterStatus implements LifeCycleState {

    Created(0) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ClusterStatus.Created,
                    ClusterStatus.Active, ClusterStatus.Terminating));
        }
    },
    Active(1) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ClusterStatus.Active, ClusterStatus.Inactive,
                    ClusterStatus.Patching, ClusterStatus.Terminating));
        }
    },
    Patching(2) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ClusterStatus.Patching, ClusterStatus.Active));
        }
    },
    Inactive(3) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ClusterStatus.Inactive, ClusterStatus.Active,
                    ClusterStatus.Terminating));
        }
    },
    Terminating(4) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ClusterStatus.Terminating,
                    ClusterStatus.Terminated, ClusterStatus.Created));
        }
    },
    Terminated(5) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(ClusterStatus.Terminated));
        }
    };

    private int code;

    private ClusterStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
