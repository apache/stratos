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

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock persistence manager.
 */
public class MockPersistenceManager implements PersistenceManager {

    private Map<String, Serializable> resourcePathToObjectMap;

    MockPersistenceManager() {
        resourcePathToObjectMap = new ConcurrentHashMap<String, Serializable>();
    }

    @Override
    public void persist(String resourcePath, Serializable serializableObject) throws RegistryException {
        resourcePathToObjectMap.put(resourcePath, serializableObject);
    }

    @Override
    public Object read(String resourcePath) throws RegistryException {
        return resourcePathToObjectMap.get(resourcePath);
    }

    @Override
    public void remove(String resourcePath) throws RegistryException {
        resourcePathToObjectMap.remove(resourcePath);
    }
}
