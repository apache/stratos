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

package org.apache.stratos.common.kubernetes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * The model class for KubernetesMaster beans. This represents a Kubernetes CoreOS master host instance
 */
public class KubernetesMaster extends KubernetesHost implements Serializable {
    private static final Log log = LogFactory.getLog(KubernetesMaster.class);

    private String endpoint;

    public KubernetesMaster() {

    }

    public KubernetesMaster(String hostId, String hostname, String hostIpAddress, String endpoint) {
        this.hostId = hostId;
        this.hostname = hostname;
        this.hostIpAddress = hostIpAddress;
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String toString() {
        return "KubernetesMaster [hostId=" + hostId +
                " hostname=" + hostname +
                " hostIpAddress=" + hostIpAddress +
                " endpoint=" + endpoint +
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

        if (!(anObject instanceof KubernetesMaster)) {
            return false;
        }

        return super.equals(anObject);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((this.endpoint == null) ? 0 : this.endpoint.hashCode());
        return result;
    }
}
