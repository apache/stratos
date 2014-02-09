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

package org.apache.stratos.manager.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.internal.DataHolder;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;

public class RegistryManager {

    private final static Log log = LogFactory.getLog(RegistryManager.class);

    private static final String STRATOS_MANAGER_REOSURCE = "/stratos_manager";

    private static RegistryService registryService;
    private static volatile RegistryManager registryManager;

    public static RegistryManager getInstance() {

        if (registryManager == null) {
            synchronized (RegistryManager.class) {
                if (registryManager == null) {
                    registryManager = new RegistryManager();
                }
            }
        }
        return registryManager;
    }

    private RegistryManager() {
        registryService = DataHolder.getRegistryService();
    }

    private UserRegistry initRegistry (int tenantId) throws RegistryException {

        UserRegistry tenantGovRegistry = registryService.getGovernanceSystemRegistry(tenantId);

        // check if the resource is available, else create it
        if (!tenantGovRegistry.resourceExists(STRATOS_MANAGER_REOSURCE)) {
            synchronized (RegistryManager.class) {
                try {
                    if (!tenantGovRegistry.resourceExists(STRATOS_MANAGER_REOSURCE)) {
                        tenantGovRegistry.put(STRATOS_MANAGER_REOSURCE, tenantGovRegistry.newCollection());
                    }
                } catch (RegistryException e) {
                    String errorMsg = "Failed to create the registry resource " + STRATOS_MANAGER_REOSURCE;
                    log.error(errorMsg, e);
                    throw e;
                }
            }
        }

        return tenantGovRegistry;
    }

    private UserRegistry initRegistry () throws RegistryException {

        UserRegistry govRegistry = registryService.getGovernanceSystemRegistry();

        // check if the resource is available, else create it
        if (!govRegistry.resourceExists(STRATOS_MANAGER_REOSURCE)) {
            synchronized (RegistryManager.class) {
                try {
                    if (!govRegistry.resourceExists(STRATOS_MANAGER_REOSURCE)) {
                        govRegistry.put(STRATOS_MANAGER_REOSURCE, govRegistry.newCollection());
                    }
                } catch (RegistryException e) {
                    String errorMsg = "Failed to create the registry resource " + STRATOS_MANAGER_REOSURCE;
                    log.error(errorMsg, e);
                    throw e;
                }
            }
        }

        return govRegistry;
    }

    public void persist (String path, byte [] resourceBytes, String tag) throws RegistryException {

        UserRegistry registry = initRegistry();

        try {
            registry.beginTransaction();
            Resource nodeResource = registry.newResource();
            nodeResource.setContent(resourceBytes);
            // store the resource in the registry
            registry.put(path, nodeResource);
            if (tag != null) {
                // apply the tag
                registry.applyTag(path, tag);
            }
            // commit
            registry.commitTransaction();

        } catch (RegistryException e) {
            String errorMsg = "Failed to persist the given resource in registry path " + path;
            log.error(errorMsg, e);
            // rollback
            try {
                registry.rollbackTransaction();

            } catch (RegistryException e1) {
                errorMsg = "Failed to rollback the transaction in registry path " + path;
                log.error(errorMsg, e1);
                throw e1;
            }
            throw e;
        }
    }

    public void move (String sourcePath, String targetPath) throws RegistryException {

        UserRegistry registry = initRegistry();

        try {
            registry.beginTransaction();
            registry.move(sourcePath, targetPath);
            registry.commitTransaction();

        } catch (RegistryException e) {
            String errorMsg = "Could not move the resource at "+ sourcePath + " to " + targetPath;
            log.error(errorMsg, e);
            // rollback
            try {
                registry.rollbackTransaction();

            } catch (RegistryException e1) {
                errorMsg = "Failed to rollback moving the resource at " + sourcePath + " to " + targetPath;
                log.error(errorMsg, e1);
                throw e1;
            }
            throw e;
        }
    }

    public void delete (String resourcePath) throws RegistryException {

        UserRegistry registry = initRegistry();

        try {
            registry.beginTransaction();
            registry.delete(resourcePath);
            registry.commitTransaction();

        } catch (RegistryException e) {
            String errorMsg = "Could not delete resource at "+ resourcePath;
            log.error(errorMsg, e);
            // rollback
            try {
                registry.rollbackTransaction();

            } catch (RegistryException e1) {
                errorMsg = "Failed to rollback the transaction in registry path " + resourcePath;
                log.error(errorMsg, e1);
                throw e1;
            }
            throw e;
        }
    }

    public Object retrieve (String resourcePath) throws RegistryException {

        UserRegistry registry = initRegistry();

        Resource resource;

        try {
            resource = registry.get(resourcePath);

        } catch (ResourceNotFoundException ignore) {
            // nothing has been persisted in the registry yet
            if(log.isDebugEnabled()) {
                log.debug("No resource found in the registry path " + resourcePath);
            }
            return null;

        } catch (RegistryException e) {
            String errorMsg = "Failed to retrieve the Resource at " + resourcePath + " from registry.";
            log.error(errorMsg, e);
            throw e;
        }

        return resource.getContent();
    }
}
