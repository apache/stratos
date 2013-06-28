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
package org.wso2.stratos.identity.saml2.sso.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager;
import org.wso2.stratos.identity.saml2.sso.mgt.SSOServiceProviderUpdateManager;

/**
 * @scr.component name="sso.mgt.StratosSAML2SSOManagementServiceComponent" immediate="true"
 * @scr.reference name="identity.sso.sp.config.manager"
 * interface="org.wso2.carbon.identity.sso.saml.SSOServiceProviderConfigManager"
 * cardinality="1..1" policy="dynamic" bind="setSSOServiceProviderConfigManager" unbind="unsetSSOServiceProviderConfigManager"
 */
public class StratosSSOMgtServiceComponent {

    private static SSOServiceProviderConfigManager ssoServiceProviderConfigManager;
    private static Log log = LogFactory.getLog(StratosSSOMgtServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        // Load the service provider details from the sso-idp-config.xml
        SSOServiceProviderUpdateManager serviceProviderUpdateManager = new SSOServiceProviderUpdateManager();
        serviceProviderUpdateManager.addServiceProviders();

        if (log.isDebugEnabled()) {
            log.debug("********************* Stratos SAML2 SS OManagement Service" +
                      "Component is activated..************");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if(log.isDebugEnabled()){
            log.debug("Stratos SAML2 SSO Management Service Component is deactivated.");
        }
    }

    protected void setSSOServiceProviderConfigManager(SSOServiceProviderConfigManager configManager){
        StratosSSOMgtServiceComponent.ssoServiceProviderConfigManager = configManager;
        if(log.isDebugEnabled()){
            log.debug("SSOServiceProviderConfigManager is set for Stratos SAML2 SSO Management Service Component.");
        }
    }

    protected void unsetSSOServiceProviderConfigManager(SSOServiceProviderConfigManager configManager){
        StratosSSOMgtServiceComponent.ssoServiceProviderConfigManager = null;
        if(log.isDebugEnabled()){
            log.debug("SSOServiceProviderConfigManager is unset for Stratos SAML2 SSO Management Service Component.");
        }
    }

    public static SSOServiceProviderConfigManager getSSOServiceProviderConfigManager(){
        return ssoServiceProviderConfigManager;
    }
}
