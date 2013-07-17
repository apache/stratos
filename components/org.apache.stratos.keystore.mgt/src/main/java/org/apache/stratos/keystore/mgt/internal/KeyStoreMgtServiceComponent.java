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
package org.apache.stratos.keystore.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.apache.stratos.keystore.mgt.KeystoreTenantMgtListener;
import org.apache.stratos.keystore.mgt.util.RealmServiceHolder;
import org.apache.stratos.keystore.mgt.util.RegistryServiceHolder;

/**
 * @scr.component name="org.apache.stratos.keystore.mgt"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 */
public class KeyStoreMgtServiceComponent {

    private static Log log = LogFactory.getLog(KeyStoreMgtServiceComponent.class);


    protected void activate(ComponentContext ctxt){
        KeystoreTenantMgtListener keystoreTenantMgtListener = new KeystoreTenantMgtListener();
        ctxt.getBundleContext().registerService(
                org.apache.stratos.common.listeners.TenantMgtListener.class.getName(),
                keystoreTenantMgtListener, null);
        if (log.isDebugEnabled()) {
            log.debug("*************Stratos Keystore mgt component is activated.**************");
        }
    }

    protected void deactivate(ComponentContext ctxt) {
        if(log.isDebugEnabled()){
            log.debug("************Stratos keystore mgt component is decativated.*************");
        }
    }

    protected void setRegistryService(RegistryService registryService){
        RegistryServiceHolder.setRegistryService(registryService);
        if (log.isDebugEnabled()) {
            log.debug("Registry Service is set for KeyStoreMgtServiceComponent.");
        }
    }

    protected void unsetRegistryService(RegistryService registryService){
        RegistryServiceHolder.setRegistryService(null);
        if(log.isDebugEnabled()){
            log.debug("Registry Service is unset for KeyStoreMgtServiceComponent.");
        }
    }

    protected void setRealmService(RealmService realmService){
        RealmServiceHolder.setRealmService(realmService);
        if (log.isDebugEnabled()) {
            log.debug("Realm Service is set for KeyStoreMgtServiceComponent.");
        }
    }

    protected void unsetRealmService(RealmService realmService){
        RealmServiceHolder.setRealmService(null);
        if(log.isDebugEnabled()){
            log.debug("Realm Service is unset for KeyStoreMgtServiceComponent.");
        }
    }
}
