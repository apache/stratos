/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
 * Defines an application port.
 */
public class Port implements Serializable {

    private static final long serialVersionUID = -2530288421360188256L;

    private String protocol;
    private int value;
    private int proxy;

    public Port(String protocol, int value, int proxy) {
        setProtocol(protocol);
        setValue(value);
        setProxy(proxy);
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getProxy() {
        return proxy;
    }

    public void setProxy(int proxy) {
        this.proxy = proxy;
    }

    @Override
    public String toString() {
        return "Port [protocol=" + protocol + ", value=" + value + ", proxy=" + proxy + "]";
    }
}
