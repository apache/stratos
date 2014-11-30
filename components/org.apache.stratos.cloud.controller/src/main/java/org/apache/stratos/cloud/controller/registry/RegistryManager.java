package org.apache.stratos.cloud.controller.registry;
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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.internal.ServiceReferenceHolder;
import org.apache.stratos.messaging.domain.topology.Topology;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.exceptions.ResourceNotFoundException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.Serializable;

/**
 * Registry manager provides functionality for persisting resources in the registry and reading them back.
 */
public class RegistryManager {
    private final static Log log = LogFactory.getLog(RegistryManager.class);

    private static class Holder {
        static final RegistryManager instance = new RegistryManager();
    }

    public static RegistryManager getInstance() {
        return Holder.instance;
    }
    
    private RegistryManager() {
        try {
            Registry registry = ServiceReferenceHolder.getInstance().getRegistry();
            if ((registry != null) && (!registry.resourceExists(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE))) {
                registry.put(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE, registry.newCollection());
            }
        } catch (RegistryException e) {
            String msg = "Failed to create the registry resource " +
                            CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE;
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }

    /**
     * Persist an object in the registry.
     *
     * @param serializableObject object to be persisted.
     */
    public synchronized void persist(String resourcePath, Serializable serializableObject) throws RegistryException {
        String absoluteResourcePath = CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE + resourcePath;
        if(log.isDebugEnabled()) {
            log.debug(String.format("Persisting resource in registry: [resource-path] %s", absoluteResourcePath));
        }
        Registry registry = ServiceReferenceHolder.getInstance().getRegistry();

        try {
        	PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        	ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        	ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);

            registry.beginTransaction();

            Resource nodeResource = registry.newResource();
            nodeResource.setContent(Serializer.serializeToByteArray(serializableObject));
            registry.put(absoluteResourcePath, nodeResource);

            registry.commitTransaction();

            if(log.isDebugEnabled()) {
                log.debug(String.format("Resource persisted successfully in registry: [resource-path] %s",
                        absoluteResourcePath));
            }
        } catch (Exception e) {
            String msg = "Failed to persist resource in registry: " + absoluteResourcePath;
            registry.rollbackTransaction();
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }

    public synchronized Object read(String resourcePath) {
        try {
            Registry registry = ServiceReferenceHolder.getInstance().getRegistry();
            if(registry == null) {
                return null;
            }

            PrivilegedCarbonContext ctx = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        	ctx.setTenantId(MultitenantConstants.SUPER_TENANT_ID);
        	ctx.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
            Resource resource = registry.get(CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE + resourcePath);
            return resource.getContent();
        } catch (ResourceNotFoundException ignore) {
            // this means, we've never persisted CC info in registry
            return null;
        } catch (RegistryException e) {
            String msg = "Failed to read resource from registry: " +
                    CloudControllerConstants.CLOUD_CONTROLLER_RESOURCE + resourcePath;
            log.error(msg, e);
            throw new CloudControllerException(msg, e);
        }
    }
}
