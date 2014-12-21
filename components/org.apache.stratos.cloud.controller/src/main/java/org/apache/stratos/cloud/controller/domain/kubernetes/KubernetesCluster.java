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

package org.apache.stratos.cloud.controller.domain.kubernetes;

import org.apache.stratos.common.Properties;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The model class for KubernetesCluster definition.
 */
public class KubernetesCluster implements Serializable {

    private static final long serialVersionUID = 3210149484906093132L;

    private String clusterId;
    private KubernetesHost[] kubernetesHosts;
    private KubernetesMaster kubernetesMaster;
    private PortRange portRange;
    private String description;
    private Properties properties = new Properties();

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public KubernetesHost[] getKubernetesHosts() {
        return kubernetesHosts;
    }

    public void setKubernetesHosts(KubernetesHost[] kubernetesHosts) {
        if(kubernetesHosts == null) {
            this.kubernetesHosts = new KubernetesHost[0];
        } else {
            this.kubernetesHosts = Arrays.copyOf(kubernetesHosts, kubernetesHosts.length);
        }
    }

    public KubernetesMaster getKubernetesMaster() {
        return kubernetesMaster;
    }

    public void setKubernetesMaster(KubernetesMaster kubernetesMaster) {
        this.kubernetesMaster = kubernetesMaster;
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

    public String toString() {
        return "KubernetesCluster [groupId=" + clusterId +
                " , kubernetesHosts=" + Arrays.toString(kubernetesHosts) +
                " , kubernetesMaster=" + kubernetesMaster +
                " , portRange=" + portRange +
                " , description=" + description +
                " , properties=" + properties + "]";
    }

    @Override
    public boolean equals(Object anObject) {
        if (anObject == null) {
            return false;
        }
        if (this == anObject) {
            return true;
        }
        if (!(anObject instanceof KubernetesCluster)) {
            return false;
        }
        KubernetesCluster kubernetesClusterObj = (KubernetesCluster) anObject;
        if (this.clusterId == null || kubernetesClusterObj.getClusterId() == null) {
            return false;
        } else if (!this.clusterId.equals(kubernetesClusterObj.getClusterId())) {
            return false;
        }

        if (this.portRange == null || kubernetesClusterObj.getPortRange() == null) {
            return false;
        } else if (!this.portRange.equals(kubernetesClusterObj.getPortRange())) {
            return false;
        }

        if (this.properties == null) {
            if (kubernetesClusterObj.getProperties() != null) {
                return false;
            }
        } else if (!this.properties.equals(kubernetesClusterObj.getProperties())) {
            return false;
        }

        if (this.description == null) {
            if (kubernetesClusterObj.description != null) {
                return false;
            }
        } else if (!this.description.equals(kubernetesClusterObj.getDescription())) {
            return false;
        }

        if (this.kubernetesMaster == null || kubernetesClusterObj.getKubernetesMaster() == null) {
            return false;
        } else if (!this.kubernetesMaster.equals(kubernetesClusterObj.getKubernetesMaster())) {
            return false;
        }

        if (this.getKubernetesHosts() == null) {
            if (kubernetesClusterObj.getKubernetesHosts() != null) {
                return false;
            }
        } else if (!Arrays.equals(this.kubernetesHosts, kubernetesClusterObj.getKubernetesHosts())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.portRange == null) ? 0 : this.portRange.hashCode());
        result = prime * result + ((this.clusterId == null) ? 0 : this.clusterId.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.kubernetesMaster == null) ? 0 : this.kubernetesMaster.hashCode());
        result = prime * result + ((this.kubernetesHosts == null) ? 0 : Arrays.hashCode(this.kubernetesHosts));
        result = prime * result + ((this.properties == null) ? 0 : this.properties.hashCode());
        return result;
    }
}
