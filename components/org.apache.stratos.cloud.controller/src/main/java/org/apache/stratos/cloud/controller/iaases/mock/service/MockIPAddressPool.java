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

package org.apache.stratos.cloud.controller.iaases.mock.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.registry.RegistryManager;
import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mock IP address pool is a singleton class for managing mocked private and public IP addresses.
 */
public class MockIPAddressPool {
    private static final Log log = LogFactory.getLog(MockIPAddressPool.class);

    private static final String MOCK_IAAS_PRIVATE_IP_SEQUENCE = "/cloud.controller/mock/iaas/private-ip-sequence";
    private static final String MOCK_IAAS_PUBLIC_IP_SEQUENCE = "/cloud.controller/mock/iaas/public-ip-sequence";
    private static final String PRIVATE_IP_PREFIX = "10.0.0.";
    private static final String PUBLIC_IP_PREFIX = "20.0.0.";

    private static volatile MockIPAddressPool instance;

    private AtomicInteger privateIpSequence;
    private AtomicInteger publicIpSequence;

    private MockIPAddressPool() {
        try {
            privateIpSequence = (AtomicInteger) RegistryManager.getInstance().read(MOCK_IAAS_PRIVATE_IP_SEQUENCE);
            if (privateIpSequence == null) {
                privateIpSequence = new AtomicInteger();
            }
        } catch (RegistryException e) {
            String message = "Could not read private ip sequence from registry";
            log.error(message, e);
            throw new RuntimeException(e);
        }

        try {
            publicIpSequence = (AtomicInteger) RegistryManager.getInstance().read(MOCK_IAAS_PUBLIC_IP_SEQUENCE);
            if (publicIpSequence == null) {
                publicIpSequence = new AtomicInteger();
            }
        } catch (RegistryException e) {
            String message = "Could not read public ip sequence from registry";
            log.error(message, e);
            throw new RuntimeException(e);
        }
    }

    public static MockIPAddressPool getInstance() {
        if (instance == null) {
            synchronized (MockIPAddressPool.class) {
                if (instance == null) {
                    instance = new MockIPAddressPool();
                }
            }
        }
        return instance;
    }

    public String getNextPrivateIpAddress() {
        int nextSequence = privateIpSequence.incrementAndGet();
        String ipAddress = PRIVATE_IP_PREFIX + nextSequence;
        persistInRegistry(MOCK_IAAS_PRIVATE_IP_SEQUENCE, privateIpSequence);
        if (log.isInfoEnabled()) {
            log.info("Mock private IP address allocated: " + ipAddress);
        }
        return ipAddress;
    }

    public String getNextPublicIpAddress() {
        int nextSequence = publicIpSequence.incrementAndGet();
        String ipAddress = PUBLIC_IP_PREFIX + nextSequence;
        persistInRegistry(MOCK_IAAS_PRIVATE_IP_SEQUENCE, publicIpSequence);
        if (log.isInfoEnabled()) {
            log.info("Mock public IP address allocated: " + ipAddress);
        }
        return ipAddress;
    }

    private void persistInRegistry(String resourcePath, Serializable serializable) {
        try {
            RegistryManager.getInstance().persist(resourcePath, serializable);
        } catch (RegistryException e) {
            log.error(String.format("Could not persist mock iaas ip sequence [%s] in registry", resourcePath), e);
        }
    }
}
