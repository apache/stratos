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

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents status of a member during its lifecycle.
 */
@XmlRootElement
public enum MemberStatus implements LifeCycleState {

    Created(0) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.Created, MemberStatus.Initialized));
        }
    },
    Initialized(1) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.Initialized, MemberStatus.Starting));
        }
    },
    Starting(2) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.Starting, MemberStatus.Active));
        }
    },
    Active(3) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.Active, MemberStatus.Suspended,
                    MemberStatus.In_Maintenance, MemberStatus.Starting));
        }
    },
    In_Maintenance(4) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.In_Maintenance,
                    MemberStatus.ReadyToShutDown));
        }
    },
    ReadyToShutDown(5) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.ReadyToShutDown,
                    MemberStatus.Terminated));
        }
    },
    Suspended(6) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.Suspended,
                    MemberStatus.Terminated));
        }
    },
    Terminated(7) {
        @Override
        public Set<LifeCycleState> getNextStates() {
            return new HashSet<LifeCycleState>(Arrays.asList(MemberStatus.Terminated));
        }
    };

    private int code;

    private MemberStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
