/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.mediator.autoscale.lbautoscale.mediators;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.http.protocol.HTTP;
import org.apache.synapse.ManagedLifecycle;
import org.apache.synapse.MessageContext;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.core.axis2.Axis2MessageContext;
import org.apache.synapse.mediators.AbstractMediator;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.util.HostContext;
import org.wso2.carbon.lb.common.cache.URLMappingCache;
import org.wso2.carbon.lb.common.util.DomainMapping;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.AppDomainContext;
import org.wso2.carbon.mediator.autoscale.lbautoscale.internal.RegistryManager;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleConstants;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleUtil;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscalerTaskDSHolder;

import java.util.Map;

/**
 * This Synapse mediator generates a token per request received. These tokens are used for tracking
 * the number of requests in flight. Once a response is received, the relevant token will be removed
 * by the {@link AutoscaleOutMediator}
 *
 * @see AutoscaleOutMediator
 */
public class AutoscaleInMediator extends AbstractMediator implements ManagedLifecycle {

    private LoadBalancerConfiguration lbConfig;
    private Map<String, HostContext> hostCtxts;
    /**
     * keep the size of cache which used to keep hostNames of url mapping.
     */
    private URLMappingCache mappingCache;
    private RegistryManager registryManager;
    private int sizeOfCache;

    public AutoscaleInMediator() {

        this.lbConfig = AutoscalerTaskDSHolder.getInstance().getWholeLoadBalancerConfig();
        hostCtxts = lbConfig.getHostContextMap();
        sizeOfCache = lbConfig.getLoadBalancerConfig().getSizeOfCache();
        mappingCache = URLMappingCache.getInstance(sizeOfCache);
    }

    public boolean mediate(MessageContext synCtx) {

        if (log.isDebugEnabled()) {
            log.debug("Mediation started .......... " + AutoscaleInMediator.class.getName());

        }

        ConfigurationContext configCtx =
                                         ((Axis2MessageContext) synCtx).getAxis2MessageContext()
                                                                       .getConfigurationContext();
        String uuid = org.apache.axiom.util.UIDGenerator.generateUID();
        synCtx.setProperty(AutoscaleConstants.REQUEST_ID, uuid);

        Map<String, Map<String, ?>> appDomainContexts =
                                                                       AutoscaleUtil.getAppDomainContexts(configCtx,
                                                                                                          lbConfig);
        org.apache.axis2.context.MessageContext axis2MessageContext =
                ((Axis2MessageContext) synCtx).getAxis2MessageContext();
        Map<String, String> transportHeaders = (Map<String, String>) axis2MessageContext.
                getProperty(org.apache.axis2.context.MessageContext.TRANSPORT_HEADERS);
        String targetHost = transportHeaders.get(HTTP.TARGET_HOST);
        
        String toAddress = synCtx.getTo().getAddress();
        if (targetHost.contains(":")) {
            targetHost = targetHost.substring(0, targetHost.indexOf(':'));
        }

        int tenantId = AutoscaleUtil.getTenantId(synCtx.getTo().toString());

        String domain = null, subDomain = null;
        
        log.debug("************ Target Host: "+targetHost + " -- Tenant id : "+tenantId);

        HostContext ctxt = hostCtxts.get(targetHost);
        
        if (ctxt == null) {

            DomainMapping domainMapping = mappingCache.getMapping(targetHost);
            if (domainMapping == null) {
                registryManager = new RegistryManager();
                domainMapping = registryManager.getMapping(targetHost);
                mappingCache.addValidMapping(targetHost, domainMapping);
            }
            if (domainMapping != null) {

                String actualHost = domainMapping.getActualHost();

                // get the HostContext from the actual host name in the case of domain 
                // mapping.
                ctxt = hostCtxts.get(actualHost);

            }
        }

        if (ctxt == null) {
            log.debug("Host Context is null.");
        	// we don't need to do anything 
        	return true;
        }


        // gets the corresponding domain
        domain = ctxt.getDomainFromTenantId(tenantId);
        synCtx.setProperty(AutoscaleConstants.TARGET_DOMAIN, domain);

        // gets the corresponding sub domain
        subDomain = ctxt.getSubDomainFromTenantId(tenantId);
        synCtx.setProperty(AutoscaleConstants.TARGET_SUB_DOMAIN, subDomain);

        if (appDomainContexts.get(domain) == null) {
            // if we do not find a correct context, we just ignore
            log.debug("AppDomainContext not found for domain " + domain);

        } else {
            AppDomainContext appDomainContext = (AppDomainContext) appDomainContexts.get(domain).get(subDomain);

            if (appDomainContext != null) {
                appDomainContext.addRequestToken(uuid);
                System.setProperty(AutoscaleConstants.IS_TOUCHED, "true");

            } else {
                // if we do not find a correct context, we just ignore
                log.debug("AppDomainContext not found for sub domain: " + subDomain +
                          " of domain: " + domain);
            }
        }

        return true;
    }

    @Override
    public void destroy() {

        log.info("Mediator destroyed! " + AutoscaleInMediator.class.getName());
    }

    @Override
    public void init(SynapseEnvironment arg0) {

        if (log.isDebugEnabled()) {
            log.debug("Mediator initialized! " + AutoscaleInMediator.class.getName());
        }
    }
    
    private void throwException(String msg){
        log.error(msg);
        throw new RuntimeException(msg);
    }
}
