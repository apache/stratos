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

import java.io.Serializable;

/**
 * The model class for KubernetesMaster beans. This represents a Kubernetes master host instance.
 */
public class KubernetesMaster extends KubernetesHost implements Serializable {

    private static final long serialVersionUID = -4369535909362724532L;

    public KubernetesMaster() {
    }

    public KubernetesMaster(String hostId, String hostname, String privateIPAddress, String publicIPAddress, String endpoint) {
        super(hostId, hostname, privateIPAddress, publicIPAddress);
    }

    @Override
    public String toString() {
        return "KubernetesMaster [hostId=" + getHostId() +
                " hostname=" + getHostname() +
                " privateIPAddress=" + getPrivateIPAddress() +
                " properties=" + getProperties() + "]";
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this == object) {
            return true;
        }

        if (!(object instanceof KubernetesMaster)) {
            return false;
        }

        return super.equals(object);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result;
        return result;
    }
}
