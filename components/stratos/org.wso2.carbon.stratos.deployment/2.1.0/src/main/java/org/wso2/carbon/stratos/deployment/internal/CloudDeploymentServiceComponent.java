/*                                                                             
 * Copyright 2004,2005 The Apache Software Foundation.                         
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
package org.wso2.carbon.stratos.deployment.internal;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.AxisObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.stratos.deployment.CloudDeploymentInterceptor;
import org.wso2.carbon.stratos.deployment.SuperTenantRolePlayer;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @scr.component name="org.wso2.carbon.stratos.deployment.internal.CloudDeploymentServiceComponent" immediate="true"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class CloudDeploymentServiceComponent {
    private static final Log log = LogFactory.getLog(CloudDeploymentServiceComponent.class);

    protected void activate(ComponentContext ctxt) {
        //TODO: Modify the permission in the UI
        try {
            ConfigurationContext configContext = DataHolder.getInstance().getServerConfigContext();
            Dictionary props = new Hashtable();
            props.put(CarbonConstants.AXIS2_CONFIG_SERVICE, AxisObserver.class.getName());
            ctxt.getBundleContext().registerService(AxisObserver.class.getName(), new CloudDeploymentInterceptor(),props);
            
            // register the role player for this configuration
            AxisConfiguration axisConfiguration = configContext.getAxisConfiguration();
            axisConfiguration.addParameter(new Parameter("rolePlayer", new SuperTenantRolePlayer()));
        } catch (Exception e) {
            log.error("CloudDeploymentServiceComponent activation failed", e);
        }
    }

    protected void deactivate(ComponentContext ctxt) {

    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.getInstance().setServerConfigContext(contextService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.getInstance().setServerConfigContext(null);
    }
}
