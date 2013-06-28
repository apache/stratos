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
package org.wso2.carbon.usage.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.local.LocalTransportReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.usage.util.Util;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.ndatasource.core.DataSourceService;

/**
 * @scr.component name="org.wso2.carbon.usage" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default" 
 * interface="org.wso2.carbon.user.core.service.RealmService" cardinality="1..1" 
 * policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 * @scr.reference name="config.context.service" 
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1" 
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="datasources.service"
 * interface="org.wso2.carbon.ndatasource.core.DataSourceService"
 * cardinality="1..1" policy="dynamic"
 * bind="setDataSourceService" unbind="unsetDataSourceService"
 */
public class UsageServiceComponent {
    private static Log log = LogFactory.getLog(UsageServiceComponent.class);


    protected void activate(ComponentContext context) {
        try {
            Util.registerRetrieverServices(context.getBundleContext());
            log.debug("******* Usage bundle is activated ******* ");    
        } catch (Exception e) {
            log.error("******* Error in activating Usage bundle ******* ");
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Usage is deactivated ******* ");
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
    
    protected void setConfigurationContextService(ConfigurationContextService ccService) {
        //commented to work with the local transport
	/*ConfigurationContext serverCtx = ccService.getServerConfigContext();
        AxisConfiguration serverConfig = serverCtx.getAxisConfiguration();
        LocalTransportReceiver.CONFIG_CONTEXT = new ConfigurationContext(serverConfig);
        LocalTransportReceiver.CONFIG_CONTEXT.setServicePath("services");
        LocalTransportReceiver.CONFIG_CONTEXT.setContextRoot("local:/");
	*/
        Util.setConfigurationContextService(ccService);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService ccService) {
        Util.setConfigurationContextService(null);
    }

    protected void setDataSourceService(DataSourceService dataSourceService){
        Util.setDataSourceService(dataSourceService);
    }
    
    protected void unsetDataSourceService(DataSourceService dataSourceService){
        Util.setDataSourceService(null);
    }
}
