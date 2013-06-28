/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.usage.agent.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.registry.core.*;
import org.wso2.carbon.registry.core.config.RegistryContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.jdbc.handlers.Handler;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerLifecycleManager;
import org.wso2.carbon.registry.core.jdbc.handlers.HandlerManager;
import org.wso2.carbon.registry.core.jdbc.handlers.RequestContext;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.Filter;
import org.wso2.carbon.registry.core.jdbc.handlers.filters.URLMatcher;
import org.wso2.carbon.registry.core.session.CurrentSession;
import org.wso2.carbon.registry.core.utils.RegistryUtils;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.usage.agent.persist.RegistryUsagePersister;
import org.wso2.carbon.usage.agent.util.MonitoredReader;
import org.wso2.carbon.usage.agent.util.MonitoredWriter;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Handler that intercept the registry calls
 */
public class RegistryUsageListener extends Handler {

    private static final Log log = LogFactory.getLog(RegistryUsageListener.class);

    public void put(RequestContext context) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID ||
                CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return;
        }

        // called only once per request
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR)
                != null) {
            return;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR, true);

        // pre triggering
        int tenantId = CurrentSession.getTenantId();

        ResourcePath path = context.getResourcePath();
        Resource resource = context.getResource();
        ((ResourceImpl) resource).setPath(path.getCompletePath());
        if (resource instanceof CollectionImpl) {
            return;
        }
        Object contentObj = resource.getContent();
        if (contentObj == null) {
            return;
        }
        int size;
        if (contentObj instanceof String) {
            size = ((String) contentObj).length();
        } else if (contentObj instanceof byte[]) {
            size = ((byte[]) contentObj).length;
        } else {
            String msg = "Unsupported type for the content.";
            log.error(msg);
            throw new RegistryException(msg);
        }


        // persisting bandwidth
        //RegistryUsagePersister.storeIncomingBandwidth(tenantId, size);
        //persisting to registry content addition
        RegistryUsagePersister.storeAddContent(tenantId, size);

        //we will pass through, so that normal registry operation will put the resource
    }

    public void importResource(RequestContext context) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID ||
                CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return;
        }
        // called only once per request..
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR)
                != null) {
            return;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR, true);

        // pre triggering
        int tenantId = CurrentSession.getTenantId();

//        ResourcePath resourcePath = context.getResourcePath();
        String sourceURL = context.getSourceURL();


        // the import resource logic
        URL url;
        try {
            if (sourceURL != null && sourceURL.toLowerCase().startsWith("file:")) {
                String msg = "The source URL must not be file in the server's local file system";
                throw new RegistryException(msg);
            }
            url = new URL(sourceURL);
        } catch (MalformedURLException e) {
            String msg = "Given source URL " + sourceURL + "is not valid.";
            throw new RegistryException(msg, e);
        }

        try {
            URLConnection uc = url.openConnection();
            InputStream in = uc.getInputStream();
            byte[] inByteArr = RegistryUtils.getByteArray(in);
            int size = inByteArr.length;

            // persisting bandwidth
            //RegistryUsagePersister.storeIncomingBandwidth(tenantId, size);

        } catch (IOException e) {

            String msg = "Could not read from the given URL: " + sourceURL;
            throw new RegistryException(msg, e);
        }
    }

    public Resource get(RequestContext context) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID ||
                CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return null;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return null;
        }
        // called only once per request..
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR)
                != null) {
            return null;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR, true);


        // pre triggering
        int tenantId = CurrentSession.getTenantId();


        // get the resource
        Resource resource = context.getResource();
        if (resource == null) {
            ResourcePath resourcePath = context.getResourcePath();
            Registry registry = context.getRegistry();
            if (registry.resourceExists(resourcePath.getPath())) {
                resource = registry.get(resourcePath.getPath());
                context.setResource(resource);
                context.setProcessingComplete(true); // nothing else to do.
            }
        }
        if (resource == null) {
            return null;
        }
        if (resource instanceof CollectionImpl) {
            return resource;
        }
        Object contentObj = resource.getContent();
        if (contentObj == null) {
            return resource;
        }
        int size;
        if (contentObj instanceof String) {
            size = ((String) contentObj).length();
        } else if (contentObj instanceof byte[]) {
            size = ((byte[]) contentObj).length;
        } else {
            String msg = "Unsupported type for the content.";
            log.error(msg);
            throw new RegistryException(msg);
        }
        // persisting bandwidth
        //RegistryUsagePersister.storeOutgoingBandwidth(tenantId, size);
        return resource;
    }

    public void dump(RequestContext requestContext) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID ||
                CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return;
        }
        // called only once per request..
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR)
                != null) {
            return;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR, true);

        long size = requestContext.getBytesWritten();

        // pre triggering
        int tenantId = CurrentSession.getTenantId();

        if (size == 0) {
            //Still not dumped
            Registry registry = requestContext.getRegistry();
            String path = requestContext.getResourcePath().getPath();
            Writer writer = requestContext.getDumpingWriter();
            // we wrap the writer with the monitored writer
            MonitoredWriter monitoredWriter = new MonitoredWriter(writer);
            registry.dump(path, monitoredWriter);
            size = monitoredWriter.getTotalWritten();
            requestContext.setProcessingComplete(true);
        }

        // persisting bandwidth
        //RegistryUsagePersister.storeOutgoingBandwidth(tenantId, size);

    }

    public void restore(RequestContext requestContext) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID ||
                CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return;
        }
        // called only once per request..
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR)
                != null) {
            return;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR, true);

        // pre triggering
        int tenantId = CurrentSession.getTenantId();
        long size = requestContext.getBytesRead();

        if (size == 0) {
            //not restored yet
            Registry registry = requestContext.getRegistry();
            String path = requestContext.getResourcePath().getPath();
            Reader reader = requestContext.getDumpingReader();
            // we wrap the reader with the monitored reader
            MonitoredReader monitoredReader = new MonitoredReader(reader);
            registry.restore(path, monitoredReader);
            size = monitoredReader.getTotalRead();
            requestContext.setProcessingComplete(true);
        }
        // persisting bandwidth
        //RegistryUsagePersister.storeIncomingBandwidth(tenantId, size);

    }

    public static void registerRegistryUsagePersistingListener(RegistryContext registryContext)
            throws RegistryException {

        //If metering is disabled, we do not need to register the handler
        if(!"true".equals(ServerConfiguration.getInstance().getFirstProperty("EnableMetering"))){
            return;
        }

        HandlerManager handlerManager = registryContext.getHandlerManager();
        RegistryUsageListener handler = new RegistryUsageListener();
        URLMatcher anyUrlMatcher = new URLMatcher();
        anyUrlMatcher.setPattern(".*");
        String[] applyingFilters = new String[]{
                Filter.PUT, Filter.IMPORT, Filter.GET, Filter.DUMP, Filter.RESTORE, Filter.DELETE};

        handlerManager.addHandlerWithPriority(applyingFilters, anyUrlMatcher, handler,
                HandlerLifecycleManager.DEFAULT_REPORTING_HANDLER_PHASE);
    }

    //===========================================================================================
    public void delete(RequestContext context) throws RegistryException {
        if (CurrentSession.getCallerTenantId() == MultitenantConstants.SUPER_TENANT_ID ||
                CurrentSession.getTenantId() == MultitenantConstants.SUPER_TENANT_ID) {
            // no limitations for the super tenant
            return;
        }
        if (CarbonConstants.REGISTRY_SYSTEM_USERNAME.equals(CurrentSession.getUser()) ||
                CarbonConstants.REGISTRY_ANONNYMOUS_USERNAME.equals(CurrentSession.getUser())) {
            // skipping tracking for anonymous and system user
            return;
        }
        // called only once per request
        if (CurrentSession.getAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR)
                != null) {
            return;
        }
        CurrentSession.setAttribute(StratosConstants.REGISTRY_USAGE_PERSISTED_SESSION_ATTR, true);
        // pre triggering
        int tenantId = CurrentSession.getTenantId();

        ResourcePath path = context.getResourcePath();
        Resource resource = context.getRegistry().get(path.getCompletePath());
        Object contentObj = resource.getContent();
        if (contentObj == null) {
            return;
        }
        int size = 0;
        if (contentObj instanceof String) {
            size = ((String) contentObj).length();
        } else if (contentObj instanceof byte[]) {
            size = ((byte[]) contentObj).length;
        } else if (contentObj instanceof String[]) {
            // of type collection
            for (String str : (String[]) contentObj) {
                size += str.length();
            }
        } else {
            String msg = "Unsupported type for the content.";
            log.error(msg);
            throw new RegistryException(msg);
        }
        RegistryUsagePersister.storeDeleteContent(tenantId, size);

        //we will pass through, so that normal registry operation will put the resource
    }
}
