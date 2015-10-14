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

package org.apache.stratos.mock.iaas.domain;

import org.apache.stratos.messaging.adapters.MapAdapter;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.Serializable;
import java.util.Map;

/**
 * Mock instance metadata.
 */
@XmlRootElement(name = "mockInstanceMetadata")
public class MockInstanceMetadata implements Serializable {
    private static final long serialVersionUID = -1323605022799409426L;
    private String instanceId;
    private String defaultPrivateIp;
    private String defaultPublicIp;
    private String payload;

    public MockInstanceMetadata() {
    }

    public MockInstanceMetadata(MockInstanceContext mockInstanceContext) {
        this.instanceId = mockInstanceContext.getInstanceId();
        this.defaultPrivateIp = mockInstanceContext.getDefaultPrivateIP();
        this.defaultPublicIp = mockInstanceContext.getDefaultPublicIP();
        this.payload = mockInstanceContext.getPayload();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getDefaultPrivateIp() {
        return defaultPrivateIp;
    }

    public void setDefaultPrivateIp(String defaultPrivateIp) {
        this.defaultPrivateIp = defaultPrivateIp;
    }

    public String getDefaultPublicIp() {
        return defaultPublicIp;
    }

    public void setDefaultPublicIp(String defaultPublicIp) {
        this.defaultPublicIp = defaultPublicIp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
