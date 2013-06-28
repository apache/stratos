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
package org.wso2.carbon.db.keep.alive.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.billing.core.BillingManager;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.db.keep.alive.util.Util;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.mgt.UserMgtConstants;

/**
 * @scr.component name="org.wso2.carbon.db.keep.alive"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="billingManager.service"
 * interface="org.wso2.carbon.billing.core.BillingManager" cardinality="1..1"
 * policy="dynamic" bind="setBillingManager" unbind="unsetBillingManager"
 */
public class DBKeepAliveServiceComponent {
    private static Log log = LogFactory.getLog(DBKeepAliveServiceComponent.class);


    protected void activate(ComponentContext context) {
        Util.registerJob();
        log.debug("******* DBKeepAlive bundle is activated ******* ");
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* DBKeepAlive is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        Util.setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        Util.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        Util.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        Util.setRealmService(null);
    }

    public void setBillingManager(BillingManager billingManager) {
        log.debug("Receiving billingManager service");
        Util.setBillingManager(billingManager);
    }

    public void unsetBillingManager(BillingManager billingManager) {
        log.debug("Unsetting billingManager service");
        Util.setBillingManager(null);
    }
}
