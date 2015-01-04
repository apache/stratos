/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
*/

package org.apache.stratos.common.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.internal.ServiceReferenceHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.Serializable;

/**
 * Registry manager provides functionality for persisting resources in the registry and reading them back.
 */
public class RegistryManager {
    private final static Log log = LogFactory.getLog(RegistryManager.class);

    private static final RegistryManager instance = new RegistryManager();

    public static RegistryManager getInstance() {
        return instance;
    }
    
    private RegistryManager() {
    }

    /**
     * Persist a serializable object in the registry with the given resource path.
     *
     * @param serializableObject object to be persisted.
     */
    public synchronized void persist(String resourcePath, Serializable serializableObject) throws RegistryException {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Persisting resource in registry: [resource-path] %s", resourcePath));
        }

        Registry registry = getRegistry();

        try {
            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        	ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        	ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            registry.beginTransaction();

            Resource nodeResource = registry.newResource();
            nodeResource.setContent(Serializer.serializeToByteArray(serializableObject));
            registry.put(resourcePath, nodeResource);

            registry.commitTransaction();

            if(log.isDebugEnabled()) {
                log.debug(String.format("Resource persisted successfully in registry: [resource-path] %s",
                        resourcePath));
            }
        } catch (Exception e) {
            String msg = "Failed to persist resource in registry: " + resourcePath;
            registry.rollbackTransaction();
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    /**
     * Returns an object stored in the given resource path.
     * @param resourcePath
     * @return
     * @throws RegistryException
     */
    public synchronized Object read(String resourcePath) throws RegistryException {
        try {
            Registry registry = getRegistry();

            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        	ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        	ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Resource resource = registry.get(resourcePath);
            Object content = resource.getContent();
            if (content != null) {
                try {
                    return Deserializer.deserializeFromByteArray((byte[])content);
                } catch (Exception e) {
                    log.error("Could not de-serialize object stored in registry", e);
                    throw new RuntimeException(e);
                }
            }
            return null;
        } catch (ResourceNotFoundException ignore) {
            return null;
        } catch (RegistryException e) {
            String msg = "Failed to read resource from registry: " + resourcePath;
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    private UserRegistry getRegistry() throws RegistryException {
        return ServiceReferenceHolder.getInstance().getRegistryService().getGovernanceSystemRegistry();
    }
}
