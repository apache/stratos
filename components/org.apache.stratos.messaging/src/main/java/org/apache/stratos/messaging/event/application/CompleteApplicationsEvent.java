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

package org.apache.stratos.messaging.event.application;

import org.apache.stratos.messaging.domain.application.Applications;
import org.apache.stratos.messaging.event.topology.TopologyEvent;

import java.io.Serializable;

/**
 * This event is fired periodically with the complete topology. It would be a
 * starting point for subscribers to initialize the current state of the topology
 * before receiving other topology events.
 */
public class CompleteApplicationsEvent extends TopologyEvent implements Serializable {

    private static final long serialVersionUID = 8580862188444892004L;

    private final Applications applications;

    public CompleteApplicationsEvent(Applications applications) {
        this.applications = applications;
    }

    public Applications getApplications() {
        return applications;
    }
}
