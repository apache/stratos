/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.throttling.agent.internal;

import org.apache.axis2.AxisFault;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.throttling.agent.ThrottlingAgent;
import org.wso2.carbon.throttling.agent.cache.Axis2ConfigurationContextObserverImpl;
import org.wso2.carbon.throttling.agent.cache.ThrottlingInfoCache;
import org.wso2.carbon.throttling.agent.listeners.WebAppRequestListener;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.tomcat.ext.valves.TomcatValveContainer;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.stratos.common.util.StratosConfiguration;

import java.util.ArrayList;

/**
 * @scr.component name="org.wso2.carbon.throttling.agent"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"  bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="stratos.config.service"
 * interface="org.wso2.carbon.stratos.common.util.StratosConfiguration" cardinality="1..1"
 * policy="dynamic" bind="setStratosConfigurationService" unbind="unsetStratosConfigurationService"
 */
public class ThrottlingAgentServiceComponent {
    private static Log log = LogFactory.getLog(ThrottlingAgentServiceComponent.class);

    private static ThrottlingAgent throttlingAgent;
    private static RealmService realmService;
    private static RegistryService registryService;
    private static ConfigurationContextService contextService;
    private static StratosConfiguration stratosConfiguration;

    protected void activate(ComponentContext context) {
        try {
            BundleContext bundleContext = context.getBundleContext();
            throttlingAgent = new ThrottlingAgent(bundleContext);
            throttlingAgent.setConfigurationContextService(contextService);
            throttlingAgent.setRealmService(realmService);
            throttlingAgent.setRegistryService(registryService);
            throttlingAgent.setStratosConfiguration(stratosConfiguration);

            try {
                // Throttling agent initialization require registry service.
                throttlingAgent.init();
            } catch (RegistryException e) {
                String errMessage = "Failed to initialize throttling agent.";
                log.error(errMessage, e);
                throw new RuntimeException(errMessage, e);
            }

            if("true".equals(ServerConfiguration.getInstance().getFirstProperty("EnableMetering"))){
                // Register the Tomcat Valve
                ArrayList<CarbonTomcatValve> valves = new ArrayList<CarbonTomcatValve>();
                valves.add(new WebAppRequestListener(throttlingAgent));
                TomcatValveContainer.addValves(valves);

                registerAxis2ConfigurationContextObserver(bundleContext, throttlingAgent.getThrottlingInfoCache());
            }else{
                log.debug("WebAppRequestListener valve was not added because metering is disabled in the configuration");
                log.debug("Axis2ConfigurationContextObserver was not registered because metering is disabled");
            }

            registerThrottlingAgent(bundleContext);

            log.debug("Multitenancy Throttling Agent bundle is activated.");
        } catch (Throwable e) {
            log.error("Multitenancy Throttling Agent bundle failed activating.", e);
        }

    }

    private void registerAxis2ConfigurationContextObserver(BundleContext bundleContext, ThrottlingInfoCache cache) {
        bundleContext.registerService(Axis2ConfigurationContextObserver.class.getName(),
                new Axis2ConfigurationContextObserverImpl(cache),
                null);
    }

    /**
     * Register throttling agent service that use to update throttling rules when users try to
     * upgrade down grade usage plans
     *
     * @param bundleContext bundle context that need to initialize throttling agent
     */
    public void registerThrottlingAgent(BundleContext bundleContext) {
        try {
            bundleContext.registerService(ThrottlingAgent.class.getName(),
                    throttlingAgent,
                    null);
        }
        catch (Exception e) {

        }
    }

    protected void deactivate(ComponentContext context) {
        //Util.uninitializeThrottlingRuleInvokerTracker();
        log.debug("******* Multitenancy Throttling Agent bundle is deactivated ******* ");
    }

    protected void setRegistryService(RegistryService registryService) {
        ThrottlingAgentServiceComponent.registryService = registryService;
    }

    protected void unsetRegistryService(RegistryService registryService) {
        ThrottlingAgentServiceComponent.registryService = null;
        throttlingAgent.setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        ThrottlingAgentServiceComponent.realmService = realmService;
    }

    protected void unsetRealmService(RealmService realmService) {
        ThrottlingAgentServiceComponent.realmService = null;
        throttlingAgent.setRealmService(null);
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        ThrottlingAgentServiceComponent.contextService = contextService;
        
        //this module is not necessary when we have the WebAppRequestListerner.
        //It takes care of webapps and services. But this is not working for ESb
        //When a solution for ESB is found, this module can be engaged again
        /*try {
            contextService.getServerConfigContext().getAxisConfiguration().engageModule(
                    "usagethrottling");
        } catch (AxisFault e) {
            log.error("Failed to engage usage throttling module", e);
        }*/
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        /*try {
            AxisConfiguration axisConfig =
                    contextService.getServerConfigContext().getAxisConfiguration();
            axisConfig.disengageModule(axisConfig.getModule("usagethrottling"));
        } catch (AxisFault e) {
            log.error("Failed to disengage usage throttling module", e);
        }*/
        ThrottlingAgentServiceComponent.contextService = null;
        throttlingAgent.setConfigurationContextService(null);
    }

    public static ThrottlingAgent getThrottlingAgent() {
        return throttlingAgent;
    }

    protected void setStratosConfigurationService(StratosConfiguration stratosConfigService) {
        ThrottlingAgentServiceComponent.stratosConfiguration=stratosConfigService;
    }

    protected void unsetStratosConfigurationService(StratosConfiguration ccService) {
        ThrottlingAgentServiceComponent.stratosConfiguration = null;
        throttlingAgent.setStratosConfiguration(null);
    }
}