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

import org.apache.stratos.common.Properties;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The model class for KubernetesGroup definition.
 */
public class KubernetesGroup implements Serializable {

    private String groupId;
    private KubernetesHost[] kubernetesHosts;
    private KubernetesMaster kubernetesMaster;
    private PortRange portRange;
    private String description;
    private Properties properties = new Properties();

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
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
        return "KubernetesGroup [groupId=" + groupId +
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
        if (!(anObject instanceof KubernetesGroup)) {
            return false;
        }
        KubernetesGroup kubernetesGroupObj = (KubernetesGroup) anObject;
        if (this.groupId == null || kubernetesGroupObj.getGroupId() == null) {
            return false;
        } else if (!this.groupId.equals(kubernetesGroupObj.getGroupId())) {
            return false;
        }

        if (this.portRange == null || kubernetesGroupObj.getPortRange() == null) {
            return false;
        } else if (!this.portRange.equals(kubernetesGroupObj.getPortRange())) {
            return false;
        }

        if (this.properties == null) {
            if (kubernetesGroupObj.getProperties() != null) {
                return false;
            }
        } else if (!this.properties.equals(kubernetesGroupObj.getProperties())) {
            return false;
        }

        if (this.description == null) {
            if (kubernetesGroupObj.description != null) {
                return false;
            }
        } else if (!this.description.equals(kubernetesGroupObj.getDescription())) {
            return false;
        }

        if (this.kubernetesMaster == null || kubernetesGroupObj.getKubernetesMaster() == null) {
            return false;
        } else if (!this.kubernetesMaster.equals(kubernetesGroupObj.getKubernetesMaster())) {
            return false;
        }

        if (this.getKubernetesHosts() == null) {
            if (kubernetesGroupObj.getKubernetesHosts() != null) {
                return false;
            }
        } else if (!Arrays.equals(this.kubernetesHosts, kubernetesGroupObj.getKubernetesHosts())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.portRange == null) ? 0 : this.portRange.hashCode());
        result = prime * result + ((this.groupId == null) ? 0 : this.groupId.hashCode());
        result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
        result = prime * result + ((this.kubernetesMaster == null) ? 0 : this.kubernetesMaster.hashCode());
        result = prime * result + ((this.kubernetesHosts == null) ? 0 : Arrays.hashCode(this.kubernetesHosts));
        result = prime * result + ((this.properties == null) ? 0 : this.properties.hashCode());
        return result;
    }
}
