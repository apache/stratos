/*
 *  Copyright (c) WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.apache.stratos.custom.handlers.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.custom.handlers.authentication.SignedJWTAuthenticator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.services.authentication.CarbonServerAuthenticator;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Hashtable;


/**
 * @scr.component name=
 * "signedjwt.SignedJWTAuthenticatorServiceComponent"
 * immediate="true"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic"
 * bind="setRealmService"
 * unbind="unsetRealmService"
 */
public class SignedJWTAuthenticatorServiceComponent {
    private static final Log log = LogFactory.getLog(SignedJWTAuthenticatorServiceComponent.class);
    private static RealmService realmService = null;
    private static BundleContext bundleContext = null;

    public static RealmService getRealmService() {
        return realmService;
    }

    protected void setRealmService(RealmService realmService) {
        if (log.isDebugEnabled()) {
            log.debug("RealmService acquired");
        }
        SignedJWTAuthenticatorServiceComponent.realmService = realmService;
    }

    public static BundleContext getBundleContext() {
        return bundleContext;
    }

    public static void setBundleContext(BundleContext bundleContext) {
        SignedJWTAuthenticatorServiceComponent.bundleContext = bundleContext;
    }

    protected void activate(ComponentContext cxt) {
        if (log.isDebugEnabled()) {
            log.debug("Activating SignedJWTAuthenticatorServiceComponent...");
        }
        try {
            SignedJWTAuthenticator authenticator = new SignedJWTAuthenticator();
            SignedJWTAuthenticatorServiceComponent.setBundleContext(cxt.getBundleContext());
            Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(CarbonConstants.AUTHENTICATOR_TYPE, authenticator.getAuthenticatorName());
            cxt.getBundleContext().registerService(CarbonServerAuthenticator.class.getName(),
                    authenticator, props);
        }
        catch (Exception e) {
            log.error(e.getMessage(), e);
            // throwing so that server will not start
            throw new RuntimeException("Failed to start the Signed JWT Authenticator Bundle" +
                    e.getMessage(), e);
        }
        log.info("Signed JWT Authenticator is activated");
    }

    protected void deactivate(ComponentContext context) {
        log.debug("Signed JWT Authenticator is deactivated");
    }

    protected void unsetRealmService(RealmService realmService) {
        SignedJWTAuthenticatorServiceComponent.realmService = null;
    }

}

