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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.stratos.messaging.domain.topology.LifeCycleStateTransitionBehavior;
import org.apache.stratos.messaging.domain.topology.lifecycle.LifeCycleStateManager;

import java.util.*;

/**
 * Represents an Application in the Topology
 */

public class Application extends ParentComponent implements LifeCycleStateTransitionBehavior<ApplicationStatus> {

    private static final long serialVersionUID = -5092959597171649688L;
    // Unique id for the Application, defined in Application Definition
    private String id;
    // Key used for authentication (with metadata service, etc.)
    private String key;
    // tenant id
    private int tenantId;
    // tenant domain
    private String tenantDomain;
    // tenant admin user
    private String tenantAdminUserName;
    // Life cycle state manager
    protected LifeCycleStateManager<ApplicationStatus> applicationStateManager;

    public Application (String id) {
        super();
        this.id = id;
        this.key = RandomStringUtils.randomAlphanumeric(16);
        this.applicationStateManager =
                new LifeCycleStateManager<ApplicationStatus>(ApplicationStatus.Created, id);
    }

    public String getUniqueIdentifier() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantDomain() {
        return tenantDomain;
    }

    public void setTenantDomain(String tenantDomain) {
        this.tenantDomain = tenantDomain;
    }

    public String getTenantAdminUserName() {
        return tenantAdminUserName;
    }

    public void setTenantAdminUserName(String tenantAdminUserName) {
        this.tenantAdminUserName = tenantAdminUserName;
    }

    @Override
    public boolean isStateTransitionValid(ApplicationStatus newState) {
        return this.applicationStateManager.isStateTransitionValid(newState);
    }

    @Override
    public Stack<ApplicationStatus> getTransitionedStates() {
        return this.applicationStateManager.getStateStack();
    }

    @Override
    public ApplicationStatus getStatus() {
        return this.applicationStateManager.getCurrentState();
    }

    @Override
    public void setStatus(ApplicationStatus newState) {
        this.applicationStateManager.changeState(newState);
    }

    public boolean equals(Object other) {
        if(other == null || !(other instanceof Application)) {
            return false;
        }

        if(this == other) {
            return true;
        }

        Application that = (Application)other;
        return this.id.equals(that.id);
    }

    public int hashCode () {
        return id.hashCode();
    }
}
