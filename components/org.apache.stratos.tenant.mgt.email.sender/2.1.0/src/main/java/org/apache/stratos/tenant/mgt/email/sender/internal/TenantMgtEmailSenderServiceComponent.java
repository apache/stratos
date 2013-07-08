/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.stratos.tenant.mgt.email.sender.internal;

import org.wso2.carbon.email.verification.util.EmailVerifcationSubscriber;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stratos.common.util.CommonUtil;
import org.apache.stratos.tenant.mgt.email.sender.listener.EmailSenderListener;
import org.apache.stratos.tenant.mgt.email.sender.util.TenantMgtEmailSenderUtil;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.osgi.service.component.ComponentContext;

/**
 * @scr.component name="org.wso2.carbon.tenant.mgt.email.sender"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default" 
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1" 
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="emailverification.service" 
 * interface= "org.wso2.carbon.email.verification.util.EmailVerifcationSubscriber"
 * cardinality="1..1" policy="dynamic"
 * bind="setEmailVerificationService" unbind="unsetEmailVerificationService"
 */
public class TenantMgtEmailSenderServiceComponent {
    private static Log log = LogFactory.getLog(TenantMgtEmailSenderServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            DataHolder.setBundleContext(context.getBundleContext());
            if (!CommonUtil.isTenantManagementEmailsDisabled()) {
                TenantMgtEmailSenderUtil.init();
                EmailSenderListener emailSenderListener = new EmailSenderListener();
                context.getBundleContext().registerService(
                        org.wso2.carbon.stratos.common.listeners.TenantMgtListener.class.getName(),
                        emailSenderListener, null);
                log.debug("******* Tenant Management Emails are enabled ******* ");
            }
            log.debug("******* Tenant Registration Email Sender bundle is activated ******* ");
        } catch (Throwable e) {
            log.error("******* Tenant Registration Email Sender bundle failed activating ****", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Email Sender bundle is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        DataHolder.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        DataHolder.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        DataHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        DataHolder.setRealmService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService service) {
        DataHolder.setConfigurationContextService(service);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService service) {
        DataHolder.setConfigurationContextService(null);
    }

    protected void setEmailVerificationService(EmailVerifcationSubscriber emailService) {
        DataHolder.setEmailVerificationService(emailService);
    }

    protected void unsetEmailVerificationService(EmailVerifcationSubscriber emailService) {
        DataHolder.setEmailVerificationService(null);
    }
}
