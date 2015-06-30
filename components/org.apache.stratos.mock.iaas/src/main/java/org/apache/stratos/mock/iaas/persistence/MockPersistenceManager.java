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

package org.apache.stratos.mock.iaas.persistence;

import org.wso2.carbon.registry.core.exceptions.RegistryException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock persistence manager for managing objects in memory.
 */
public class MockPersistenceManager implements PersistenceManager {

    private Map<String, Object> resourcePathToObjectMap;

    MockPersistenceManager() {
        resourcePathToObjectMap = new ConcurrentHashMap<String, Object>();
    }

    /**
     * Persist an object in memory with the given resource path.
     * @param resourcePath
     * @param object
     * @throws RegistryException
     */
    @Override
    public void persist(String resourcePath, Object object) throws RegistryException {
        resourcePathToObjectMap.put(resourcePath, object);
    }

    /**
     * Read an object in memory with the given resource path.
     * @param resourcePath
     * @return
     * @throws RegistryException
     */
    @Override
    public Object read(String resourcePath) throws RegistryException {
        return resourcePathToObjectMap.get(resourcePath);
    }

    /**
     * Remove an object in memory with the given resource path.
     * @param resourcePath
     * @throws RegistryException
     */
    @Override
    public void remove(String resourcePath) throws RegistryException {
        resourcePathToObjectMap.remove(resourcePath);
    }
}
