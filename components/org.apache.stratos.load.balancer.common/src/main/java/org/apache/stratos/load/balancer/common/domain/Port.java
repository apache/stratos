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

package org.apache.stratos.load.balancer.common.domain;

/**
 * Load balancer port definition.
 */
public class Port {

    private String protocol;
    private int value;
    private int proxy;

    public Port(String protocol, int value, int proxy) {
        this.protocol = protocol;
        this.value = value;
        this.proxy = proxy;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getValue() {
        return value;
    }

    public int getProxy() {
        return proxy;
    }

    @Override
    public String toString() {
        return "Port [protocol=" + protocol + ", value=" + value + ", proxy=" + proxy + "]";
    }
}
