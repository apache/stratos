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
package org.apache.stratos.messaging.event.application;

import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.event.Event;

import java.io.Serializable;

/**
 * This event will be fired upon the application created is detected.
 */
public class ApplicationInstanceCreatedEvent extends Event implements Serializable {
    private static final long serialVersionUID = 2625412714611885089L;

    private String applicationId;

    private ApplicationInstance applicationInstance;

    public ApplicationInstanceCreatedEvent(String applicationId, ApplicationInstance applicationInstance) {

        this.applicationId = applicationId;
        this.applicationInstance = applicationInstance;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public ApplicationInstance getApplicationInstance() {
        return applicationInstance;
    }
}
