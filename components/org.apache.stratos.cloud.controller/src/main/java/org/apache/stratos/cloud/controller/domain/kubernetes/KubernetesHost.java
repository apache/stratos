/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

package org.apache.stratos.cloud.controller.domain.kubernetes;

import org.apache.stratos.common.Properties;

import java.io.Serializable;

/**
 * The model class for KubernetesHost beans. This represents a Kubernetes host instance.
 */
public class KubernetesHost implements Serializable {

    private static final long serialVersionUID = 1798748592432690645L;

    private String hostId;
    private String hostname;
    private String privateIPAddress;
    private String publicIPAddress;
    private Properties properties = new Properties();

    public KubernetesHost() {
    }

    public KubernetesHost(String hostId, String hostname, String privateIPAddress, String publicIPAddress) {
        this.hostId = hostId;
        this.hostname = hostname;
        this.privateIPAddress = privateIPAddress;
        this.publicIPAddress = publicIPAddress;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getPrivateIPAddress() {
        return privateIPAddress;
    }

    public void setPrivateIPAddress(String hostIpAddress) {
        this.privateIPAddress = hostIpAddress;
    }

    public String getPublicIPAddress() {
        return publicIPAddress;
    }

    public void setPublicIPAddress(String publicIPAddress) {
        this.publicIPAddress = publicIPAddress;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public String toString() {
        return "KubernetesHost [hostId=" + hostId +
                " hostname=" + hostname +
                " privateIPAddress=" + privateIPAddress +
                " publicIPAddress=" + publicIPAddress +
                " properties=" + properties + "]";
    }

    @Override
    public boolean equals(Object anObject) {
        if (anObject == null) {
            return false;
        }
        if (this == anObject) {
            return true;
        }

        if (!(anObject instanceof KubernetesHost)) {
            return false;
        }
        KubernetesHost kubernetesHostObj = (KubernetesHost) anObject;

        if (this.hostId == null || kubernetesHostObj.getHostId() == null) {
            return false;
        } else if (!this.hostId.equals(kubernetesHostObj.getHostId())) {
            return false;
        }

        if (this.privateIPAddress == null || kubernetesHostObj.getPrivateIPAddress() == null) {
            return false;
        } else if (!this.privateIPAddress.equals(kubernetesHostObj.getPrivateIPAddress())) {
            return false;
        }

        if (this.publicIPAddress == null || kubernetesHostObj.getPublicIPAddress() == null) {
            return false;
        } else if (!this.publicIPAddress.equals(kubernetesHostObj.getPublicIPAddress())) {
            return false;
        }

        if (this.hostname == null) {
            if (kubernetesHostObj.getHostname() != null) {
                return false;
            }
        } else if (!this.hostname.equals(kubernetesHostObj.getHostname())) {
            return false;
        }

        if (this.properties == null) {
            if (kubernetesHostObj.getProperties() != null) {
                return false;
            }
        } else if (this.properties.equals(kubernetesHostObj.getProperties())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.hostId == null) ? 0 : this.hostId.hashCode());
        result = prime * result + ((this.hostname == null) ? 0 : this.hostname.hashCode());
        result = prime * result + ((this.privateIPAddress == null) ? 0 : this.privateIPAddress.hashCode());
        result = prime * result + ((this.publicIPAddress == null) ? 0 : this.publicIPAddress.hashCode());
        result = prime * result + ((this.properties == null) ? 0 : this.properties.hashCode());
        return result;
    }
}
