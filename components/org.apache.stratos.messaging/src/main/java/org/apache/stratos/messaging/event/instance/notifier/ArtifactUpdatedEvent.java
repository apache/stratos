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
package org.apache.stratos.messaging.event.instance.notifier;

import org.apache.stratos.messaging.event.tenant.TenantEvent;

import java.io.Serializable;

/**
 * This event is fired to a cluster when an artifact notification received from the git repository.
 */

public class ArtifactUpdatedEvent extends InstanceNotifierEvent implements Serializable {
    private String clusterId;
    private String status;
    private String repoUserName;
    private String repoPassword;
    private String repoURL;
    private String tenantId;
    private boolean commitEnabled;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRepoUserName() {
        return repoUserName;
    }

    public void setRepoUserName(String repoUserName) {
        this.repoUserName = repoUserName;
    }

    public String getRepoPassword() {
        return repoPassword;
    }

    public void setRepoPassword(String repoPassword) {
        this.repoPassword = repoPassword;
    }

    public String getRepoURL() {
        return repoURL;
    }

    public void setRepoURL(String repoURL) {
        this.repoURL = repoURL;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return String.format("[cluster] %s [repo-url] %s [repo-username] %s [tenant] %s",
                getClusterId(), getRepoURL(), getRepoUserName(), getTenantId());
    }

    public boolean isCommitEnabled() {
        return commitEnabled;
    }

    public void setCommitEnabled(boolean commitEnabled) {
        this.commitEnabled = commitEnabled;
    }
}
