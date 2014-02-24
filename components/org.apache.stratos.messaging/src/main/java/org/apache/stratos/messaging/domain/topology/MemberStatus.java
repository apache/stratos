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

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents status of a member during its lifecycle.
 */
@XmlRootElement
public enum MemberStatus {
    Created(1),
    Starting(2),
    Activated(3),
    In_Maintenance(4),
    ReadyToShutDown(5),
    Terminated(6),
    Suspended(0),
    ShuttingDown(0);

    private int code;

    private MemberStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
