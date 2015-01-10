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

package org.apache.stratos.common.beans.kubernetes;

import org.apache.stratos.common.beans.cartridge.PropertyBean;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class KubernetesClusterBean {

    private String clusterId;
    private String description;
    private List<KubernetesHostBean> kubernetesHosts;
    private KubernetesMasterBean kubernetesMaster;
    private PortRangeBean portRange;
    private List<PropertyBean> property;

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public List<KubernetesHostBean> getKubernetesHosts() {
        return kubernetesHosts;
    }

    public void setKubernetesHosts(List<KubernetesHostBean> kubernetesHosts) {
        this.kubernetesHosts = kubernetesHosts;
    }

    public KubernetesMasterBean getKubernetesMaster() {
        return kubernetesMaster;
    }

    public void setKubernetesMaster(KubernetesMasterBean kubernetesMaster) {
        this.kubernetesMaster = kubernetesMaster;
    }

    public PortRangeBean getPortRange() {
        return portRange;
    }

    public void setPortRange(PortRangeBean portRange) {
        this.portRange = portRange;
    }

    public List<PropertyBean> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyBean> property) {
        this.property = property;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
