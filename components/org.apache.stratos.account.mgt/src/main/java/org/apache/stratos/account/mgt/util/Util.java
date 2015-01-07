/*
 *     Licensed to the Apache Software Foundation (ASF) under one
 *     or more contributor license agreements.  See the NOTICE file
 *     distributed with this work for additional information
 *     regarding copyright ownership.  The ASF licenses this file
 *     to you under the Apache License, Version 2.0 (the
 *     "License"); you may not use this file except in compliance
 *     with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing,
 *     software distributed under the License is distributed on an
 *     "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *     KIND, either express or implied.  See the License for the
 *     specific language governing permissions and limitations
 *     under the License.
 */

package org.apache.stratos.account.mgt.util;

import org.wso2.carbon.email.verification.util.EmailVerifcationSubscriber;
import org.wso2.carbon.email.verification.util.EmailVerifierConfig;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.exception.ApacheStratosException;
import org.apache.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.CarbonUtils;

import org.apache.axis2.context.MessageContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Util methods for AccountMgt
 */
public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    private static RegistryService registryService;
    private static RealmService realmService;
    private static EmailVerifcationSubscriber emailVerificationService = null;
    private static EmailVerifierConfig emailVerifierConfig = null;
    private static List<TenantMgtListener> tenantMgtListeners = new ArrayList<TenantMgtListener>();

    public static synchronized void setRegistryService(RegistryService service) {
        if (registryService == null) {
            registryService = service;
        }
    }

    
    public static RealmService getRealmService() {
        return realmService;
    }


    public static RegistryService getRegistryService() {
        return registryService;
    }

    public static synchronized void setEmailVerificationService(EmailVerifcationSubscriber service) {
        if (emailVerificationService == null) {
            emailVerificationService = service;
        }
    }

    public static EmailVerifcationSubscriber getEmailVerificationService() {
        return emailVerificationService;
    }


    public static synchronized void setRealmService(RealmService service) {
        if (realmService == null) {
            realmService = service;
        }
    }


    public static TenantManager getTenantManager() {
        return realmService.getTenantManager();
    }

    public static UserRegistry getGovernanceSystemRegistry(int tenantId) throws RegistryException {
        return registryService.getGovernanceSystemRegistry(tenantId);
    }

    public static HttpSession getRequestSession() throws RegistryException {
        MessageContext messageContext = MessageContext.getCurrentMessageContext();
        if (messageContext == null) {
            String msg = "Could not get the user's session. Message context not found.";
            log.error(msg);
            throw new RegistryException(msg);
        }

        HttpServletRequest request =
                (HttpServletRequest) messageContext.getProperty("transport.http.servletRequest");

        return request.getSession();
    }

    public static void loadEmailVerificationConfig() {
        String configXml = CarbonUtils.getCarbonConfigDirPath()+ File.separator
                           + StratosConstants.EMAIL_CONFIG +File.separator +"email-update.xml";
        emailVerifierConfig = org.wso2.carbon.email.verification.util.Util.loadeMailVerificationConfig(configXml);
    }

    public static EmailVerifierConfig getEmailVerifierConfig() {
        return emailVerifierConfig;
    }

    public static void addTenantMgtListenerService(TenantMgtListener tenantMgtListener) {
        tenantMgtListeners.add(tenantMgtListener);
        sortTenantMgtListeners();
    }

    public static void removeTenantMgtListenerService(TenantMgtListener tenantMgtListener) {
        tenantMgtListeners.remove(tenantMgtListener);
        sortTenantMgtListeners();
    }
    
    private static void sortTenantMgtListeners() {
        Collections.sort(tenantMgtListeners, new Comparator<TenantMgtListener>() {
            public int compare(TenantMgtListener o1, TenantMgtListener o2) {
                return o1.getListenerOrder() - o2.getListenerOrder();
            }
        });
    }
    
    public static void alertTenantRenames(int tenantId, String oldName, 
                                          String newName) throws ApacheStratosException {

        for (TenantMgtListener tenantMgtLister : tenantMgtListeners) {
            tenantMgtLister.onTenantRename(tenantId, oldName, newName);
        }
    }
    
    public static void alertTenantDeactivation(int tenantId) throws ApacheStratosException {

        for (TenantMgtListener tenantMgtLister : tenantMgtListeners) {
            tenantMgtLister.onTenantDeactivation(tenantId);
        }
    }
    
    public static void alertTenantInitialActivation(int tenantId) throws ApacheStratosException {

        for (TenantMgtListener tenantMgtLister : tenantMgtListeners) {
            tenantMgtLister.onTenantInitialActivation(tenantId);
        }
    }
    
    public static void alertTenantUpdate(TenantInfoBean tenantInfoBean) throws ApacheStratosException {

        for (TenantMgtListener tenantMgtLister : tenantMgtListeners) {
            tenantMgtLister.onTenantUpdate(tenantInfoBean);
        }
    }
    
}
