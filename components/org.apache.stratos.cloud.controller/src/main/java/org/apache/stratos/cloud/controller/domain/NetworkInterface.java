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
 * The model class for NetworkInterface definition.
 */
public class NetworkInterface implements Serializable {

    private static final long serialVersionUID = 3979879787250775211L;
    private String networkUuid;
    private String fixedIp;
    private String portUuid;
    private FloatingNetworks floatingNetworks;

    public NetworkInterface() {
    }

    public NetworkInterface(String networkUuid, String fixedIp, String portUuid) {
        this.networkUuid = networkUuid;
        this.fixedIp = fixedIp;
        this.portUuid = portUuid;
    }

    /**
     * @return the networkUuid
     */
    public String getNetworkUuid() {
        return networkUuid;
    }

    /**
     * @param networkUuid the networkUuid to set
     */
    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }

    /**
     * @return the fixedIp
     */
    public String getFixedIp() {
        return fixedIp;
    }

    /**
     * @param fixedIp the fixedIp to set
     */
    public void setFixedIp(String fixedIp) {
        this.fixedIp = fixedIp;
    }

    /**
     * @return the portUuid
     */
    public String getPortUuid() {
        return portUuid;
    }

    /**
     * @param portUuid the portUuid to set
     */
    public void setPortUuid(String portUuid) {
        this.portUuid = portUuid;
    }

    /**
     * @return {@link FloatingNetworks}
     */
    public FloatingNetworks getFloatingNetworks() {
        return floatingNetworks;
    }

    /**
     * @param floatingNetworks the {@link FloatingNetworks} to be set
     */
    public void setFloatingNetworks(FloatingNetworks floatingNetworks) {
        this.floatingNetworks = floatingNetworks;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder('{');
        String delimeter = "";
        if (networkUuid != null) {
            sb.append(delimeter).append("networkUuid : ").append(networkUuid);
            delimeter = ", ";
        }
        if (fixedIp != null) {
            sb.append(delimeter).append("fixedIp : ").append(fixedIp);
            delimeter = ", ";
        }
        if (portUuid != null) {
            sb.append(delimeter).append("portUuid : ").append(portUuid);
            delimeter = ", ";
        }
        sb.append('}');
        return sb.toString();
    }
}
