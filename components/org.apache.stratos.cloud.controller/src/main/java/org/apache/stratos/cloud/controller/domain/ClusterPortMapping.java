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
package org.apache.stratos.cloud.controller.domain;

import java.io.Serializable;

/**
 * Cluster port mappings keep track of the generated kubernetes service ports for each
 * port mapping defined in the corresponding cartridge.
 */
public class ClusterPortMapping extends PortMapping implements Serializable {

    private static final long serialVersionUID = -5387564414633460306L;

    private String applicationId;
    private String clusterId;
    private boolean kubernetes;
    private int kubernetesServicePort;

    public ClusterPortMapping() {
    }

    public ClusterPortMapping(String applicationId, String clusterId, String name, String protocol, int port, int proxyPort) {
        super(protocol, port, proxyPort);
        super.setName(name);

        this.applicationId = applicationId;
        this.clusterId = clusterId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public int getKubernetesServicePort() {
        return kubernetesServicePort;
    }

    public void setKubernetesServicePort(int kubernetesServicePort) {
        this.kubernetesServicePort = kubernetesServicePort;
        kubernetes = true;
    }

    public boolean isKubernetes() {
        return kubernetes;
    }

    @Override
    public boolean equals(Object obj) {
        ClusterPortMapping portMappingObj = (ClusterPortMapping) obj;
        return this.getName().equals(portMappingObj.getName());
    }

    public String toString() {
        return "[application-id] " + getApplicationId() + " [cluster-id] " + getClusterId() + ", " +
                super.toString() + " [kubernetes-service-port] "+ getKubernetesServicePort();
    }
}
