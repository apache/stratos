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
package org.wso2.carbon.migration.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.migration.impl.Alpha1;
import org.wso2.carbon.migration.impl.ManagementPermissionsAdder;
import org.wso2.carbon.migration.util.Util;

/**
 * @scr.component name="org.wso2.carbon.migration" immediate="true"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 */
public class MigrationServiceComponent {
    private static Log log = LogFactory.getLog(MigrationServiceComponent.class);

    protected void activate(ComponentContext context) {
        try {
            new Alpha1().migrate();
            ManagementPermissionsAdder uiPermissionAdder = new ManagementPermissionsAdder();
            context.getBundleContext().addBundleListener(uiPermissionAdder);
            Bundle[] bundles = context.getBundleContext().getBundles();
            for (Bundle bundle : bundles) {
                if (bundle.getState() == Bundle.ACTIVE) {
                    uiPermissionAdder.addUIPermissionFromBundle(bundle);
                }
            }
            
            log.debug("******* Migration bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Migration bundle failed to activate ******* ");
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Migration is deactivated ******* ");
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
}
