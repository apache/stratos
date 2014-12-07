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

package org.apache.stratos.cloud.controller.iaases.mock;

import org.jclouds.compute.domain.ComputeType;
import org.jclouds.compute.domain.Hardware;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.OperatingSystem;
import org.jclouds.domain.Location;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.domain.ResourceMetadata;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * Created by imesh on 12/6/14.
 */
public class MockNodeMetadata implements NodeMetadata {
    private String id;

    @Override
    public String getHostname() {
        return null;
    }

    @Override
    public String getGroup() {
        return null;
    }

    @Override
    public Hardware getHardware() {
        return null;
    }

    @Override
    public String getImageId() {
        return null;
    }

    @Override
    public OperatingSystem getOperatingSystem() {
        return null;
    }

    @Override
    public int getLoginPort() {
        return 0;
    }

    @Override
    public LoginCredentials getCredentials() {
        return null;
    }

    @Override
    public Set<String> getPublicAddresses() {
        return null;
    }

    @Override
    public Set<String> getPrivateAddresses() {
        return null;
    }

    @Override
    public Status getStatus() {
        return null;
    }

    @Override
    public String getBackendStatus() {
        return null;
    }

    @Override
    public ComputeType getType() {
        return null;
    }

    @Override
    public String getProviderId() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Location getLocation() {
        return null;
    }

    @Override
    public URI getUri() {
        return null;
    }

    @Override
    public Map<String, String> getUserMetadata() {
        return null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Set<String> getTags() {
        return null;
    }

    @Override
    public int compareTo(ResourceMetadata<ComputeType> computeTypeResourceMetadata) {
        return 0;
    }

    public void setId(String id) {
        this.id = id;
    }
}
