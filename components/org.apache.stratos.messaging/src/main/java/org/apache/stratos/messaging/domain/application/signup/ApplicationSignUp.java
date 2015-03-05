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

package org.apache.stratos.messaging.domain.application.signup;

import java.io.Serializable;

/**
 * Application signup.
 */
public class ApplicationSignUp implements Serializable {

    private static final long serialVersionUID = -3055522170914869018L;

    private int tenantId;
    private String applicationId;
    private ArtifactRepository[] artifactRepositories;
    private DomainMapping[] domainMappings;
    private String[] clusterIds;

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public ArtifactRepository[] getArtifactRepositories() {
        return artifactRepositories;
    }

    public void setArtifactRepositories(ArtifactRepository[] artifactRepositories) {
        this.artifactRepositories = artifactRepositories;
    }

    public DomainMapping[] getDomainMappings() {
        return domainMappings;
    }

    public void setDomainMappings(DomainMapping[] domainMappings) {
        this.domainMappings = domainMappings;
    }

    public void setClusterIds(String[] clusterIds) {
        this.clusterIds = clusterIds;
    }

    public String[] getClusterIds() {
        return clusterIds;
    }
}
