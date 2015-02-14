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

package org.apache.stratos.messaging.domain.topology;

import java.io.Serializable;

/**
 * Kubernetes service definition.
 */
public class KubernetesService implements Serializable {

    private static final long serialVersionUID = -2329017659002353186L;

    private String id;
    private String[] publicIPs;
    private String portalIP;
    private String protocol;
    private int port;
    private int containerPort;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String[] getPublicIPs() {
        return publicIPs;
    }

    public void setPublicIPs(String[] publicIPs) {
        this.publicIPs = publicIPs;
    }

    public String getPortalIP() {
        return portalIP;
    }

    public void setPortalIP(String portalIP) {
        this.portalIP = portalIP;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setContainerPort(int containerPort) {
        this.containerPort = containerPort;
    }

    public int getContainerPort() {
        return containerPort;
    }
}
