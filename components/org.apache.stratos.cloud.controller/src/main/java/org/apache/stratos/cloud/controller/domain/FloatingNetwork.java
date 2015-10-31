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
 * Every {@link NetworkInterface} can have
 * an array of {@link FloatingNetwork}
 * to which the network interface is associated
 *
 * @author rajkumar
 */
public class FloatingNetwork implements Serializable {

    private static final long serialVersionUID = -432127317992004321L;
    private String name;
    private String networkUuid;
    private String floatingIP;

    public FloatingNetwork() {
    }

    public FloatingNetwork(String name, String networkUuid, String floatingIP) {
        this.name = name;
        this.networkUuid = networkUuid;
        this.floatingIP = floatingIP;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public void setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
    }

    public String getFloatingIP() {
        return floatingIP;
    }

    public void setFloatingIP(String floatingIP) {
        this.floatingIP = floatingIP;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder('{');
        String delimeter = "";
        if (name != null) {
            sb.append(delimeter).append("name : ").append(name);
            delimeter = ", ";
        }
        if (networkUuid != null) {
            sb.append(delimeter).append("networkUuid : ").append(networkUuid);
            delimeter = ", ";
        }
        if (floatingIP != null) {
            sb.append(delimeter).append("floatingIP : ").append(floatingIP);
            delimeter = ", ";
        }
        sb.append('}');
        return sb.toString();
    }
}