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

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.stratos.messaging.domain.instance.ApplicationInstance;
import org.apache.stratos.messaging.domain.instance.Instance;

import java.util.HashMap;
import java.util.Stack;

/**
 * Represents an Application in the Topology
 */

public class Application extends ParentComponent<ApplicationInstance> {

    private static final long serialVersionUID = -5092959597171649688L;

    // Unique id for the Application, defined in Application Definition
    private String id;
    private String name;
    private String description;
    // Key used for authentication (with metadata service, etc.)
    private String key;
    // tenant id
    private int tenantId;
    // tenant domain
    private String tenantDomain;
    // tenant admin user
    private String tenantAdminUserName;
    // Life cycle state manager
    //protected LifeCycleStateManager<ApplicationStatus> applicationStateManager;

    // application policy id
    private String applicationPolicyId;

    public Application(String id) {
        super();
        this.id = id;
        this.key = RandomStringUtils.randomAlphanumeric(16);
        this.setInstanceIdToInstanceContextMap(new HashMap<String, ApplicationInstance>());
        //this.applicationStateManager =
        //new LifeCycleStateManager<ApplicationStatus>(ApplicationStatus.Created, id);
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

    public boolean isStateTransitionValid(ApplicationStatus newState, String applicationInstancetId) {
        return this.getInstanceIdToInstanceContextMap().get(applicationInstancetId).isStateTransitionValid(newState);
    }

    public Stack<ApplicationStatus> getTransitionedStates(String applicationInstancetId) {
        return this.getInstanceIdToInstanceContextMap().get(applicationInstancetId).getTransitionedStates();
    }

    public ApplicationStatus getStatus() {
        if ((getInstanceIdToInstanceContextMap() != null) && (getInstanceIdToInstanceContextMap().size() > 0)) {
            boolean applicationActive = true;
            for (ApplicationInstance applicationInstance : getInstanceIdToInstanceContextMap().values()) {
                if (applicationInstance.getStatus() != ApplicationStatus.Active) {
                    applicationActive = false;
                }
            }
            if (applicationActive) {
                return ApplicationStatus.Active;
            }
        }
        return ApplicationStatus.Inactive;
    }

    public ApplicationStatus getStatus(String applicationInstanceId) {
        return this.getInstanceIdToInstanceContextMap().get(applicationInstanceId).getStatus();
    }

    public boolean setStatus(ApplicationStatus newState, String applicationInstanceId) {
        return this.getInstanceIdToInstanceContextMap().get(applicationInstanceId).setStatus(newState);
    }

    public boolean equals(Object other) {
        if (other == null || !(other instanceof Application)) {
            return false;
        }

        if (this == other) {
            return true;
        }

        Application that = (Application) other;
        return this.id.equals(that.id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public Instance getInstanceByNetworkPartitionId(String networkPartitionId) {
        // if map is empty, return null
        if (getInstanceIdToInstanceContextMap().isEmpty()) {
            return null;
        }

        for (Instance instance : getInstanceIdToInstanceContextMap().values()) {
            if (instance.getNetworkPartitionId().equals(networkPartitionId)) {
                return instance;
            }
        }

        return null;

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApplicationPolicyId() {
        return applicationPolicyId;
    }

    public void setApplicationPolicyId(String applicationPolicyId) {
        this.applicationPolicyId = applicationPolicyId;
    }
}
