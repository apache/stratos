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
package org.apache.stratos.redirector.servlet.util;

import org.wso2.carbon.registry.core.service.RegistryService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.user.api.RealmConfiguration;
import org.osgi.framework.ServiceRegistration;
import org.apache.stratos.activation.service.ActivationService;

import java.io.*;
import java.util.*;

public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    private static RegistryService registryService;
    private static RealmService realmService;
    private static ServiceRegistration redirectorServiceRegistration = null;
    private static ActivationService activationService = null;

    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }
    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setActivationService(ActivationService service) {
        if (activationService == null) {
            activationService = service;
        }
    }
    public static ActivationService getActivationService() {
        return activationService;
    }

    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }

    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static RealmConfiguration getBootstrapRealmConfiguration() {
        return realmService.getBootstrapRealmConfiguration();
    }
}
