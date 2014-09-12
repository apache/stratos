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

package org.apache.stratos.common.kubernetes;

import java.io.Serializable;
import java.util.List;

/**
 * The model class for KubernetesGroup definition.
 */
public class KubernetesGroup implements Serializable {

    private String groupId;
    private List<KubernetesHost> kubernetesHosts;
    private List<KubernetesMaster> kubernetesMasters;
    private PortRange portRange;
    private String description;
    private Properties properties = new Properties();

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<KubernetesHost> getKubernetesHosts() {
        return kubernetesHosts;
    }

    public void setKubernetesHosts(List<KubernetesHost> kubernetesHosts) {
        this.kubernetesHosts = kubernetesHosts;
    }

    public List<KubernetesMaster> getKubernetesMasters() {
        return kubernetesMasters;
    }

    public void setKubernetesMasters(List<KubernetesMaster> kubernetesMasters) {
        this.kubernetesMasters = kubernetesMasters;
    }

    public PortRange getPortRange() {
        return portRange;
    }

    public void setPortRange(PortRange portRange) {
        this.portRange = portRange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}
