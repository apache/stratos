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

package org.apache.stratos.mock.iaas.services;

import org.apache.stratos.mock.iaas.domain.MockInstanceContext;
import org.apache.stratos.mock.iaas.domain.MockInstanceMetadata;
import org.apache.stratos.mock.iaas.exceptions.MockIaasException;

import java.util.List;

/**
 * Mock iaas service interface.
 */
public interface MockIaasService {

    /**
     * Start mock instance.
     *
     * @param mockInstanceContext
     * @return
     * @throws MockIaasException
     */
    MockInstanceMetadata startInstance(MockInstanceContext mockInstanceContext) throws MockIaasException;

    /**
     * Terminate mock instance.
     *
     * @param instanceId
     */
    void terminateInstance(String instanceId);

    /**
     * Get mock instances.
     *
     * @return
     */
    List<MockInstanceMetadata> getInstances();

    /**
     * Get mock instance by instance id.
     *
     * @param instanceId
     * @return
     */
    MockInstanceMetadata getInstance(String instanceId);

    /**
     * Allocate ip address to mock instance.
     *
     * @param instanceId
     * @return
     */
    MockInstanceMetadata allocateIpAddress(String instanceId) throws MockIaasException;
}
