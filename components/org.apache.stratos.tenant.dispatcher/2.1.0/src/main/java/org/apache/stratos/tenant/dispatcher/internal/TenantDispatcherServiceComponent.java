/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
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
package org.apache.stratos.tenant.dispatcher.internal;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.AxisServiceGroup;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.InOutAxisOperation;
import org.apache.axis2.description.PhaseRule;
import org.apache.axis2.dispatchers.HTTPLocationBasedDispatcher;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.engine.Phase;
import org.apache.axis2.phaseresolver.PhaseException;
import org.apache.axis2.phaseresolver.PhaseMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.core.multitenancy.MultitenantDispatcher;
import org.wso2.carbon.core.multitenancy.MultitenantMessageReceiver;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;

/**
 * @scr.component
 *                name="org.apache.stratos.tenant.dispatcher.internal.TenantDispatcherServiceComponent"
 *                immediate="true"
 * @scr.reference name="org.wso2.carbon.configurationContextService"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContext"
 *                unbind="unsetConfigurationContext"
 */
public class TenantDispatcherServiceComponent {
    private Log log = LogFactory.getLog(TenantDispatcherServiceComponent.class);

    private ConfigurationContext configCtx;

    protected void activate(ComponentContext ctxt) {
        try {
            deployMultitenantService(configCtx.getAxisConfiguration());
            addDispatchers(configCtx.getAxisConfiguration());
        } catch (Throwable e) {
            log.error("Failed to activate the TenantDispatcherServiceComponent", e);
        }
    }


    private void deployMultitenantService(AxisConfiguration axisCfg) throws AxisFault {
        AxisService service = new AxisService(MultitenantConstants.MULTITENANT_DISPATCHER_SERVICE);
        AxisOperation operation =
                new InOutAxisOperation(MultitenantConstants.MULTITENANT_DISPATCHER_OPERATION);
        operation.setMessageReceiver(new MultitenantMessageReceiver());
        service.addOperation(operation);
        AxisServiceGroup multitenantSvcGroup = new AxisServiceGroup(axisCfg);
        multitenantSvcGroup.setServiceGroupName(MultitenantConstants.MULTITENANT_DISPATCHER_SERVICE);
        multitenantSvcGroup.addParameter(CarbonConstants.HIDDEN_SERVICE_PARAM_NAME, "true");
        multitenantSvcGroup.addService(service);
        axisCfg.addServiceGroup(multitenantSvcGroup);
		if(log.isDebugEnabled()){
			log.debug("Deployed " + MultitenantConstants.MULTITENANT_DISPATCHER_SERVICE);
		}
    }

    /**
     * Add the MultitenantDispatcher to the inFlow phase.
     *
     * @param mainAxisConfig super-tenant AxisConfiguration
     * @throws org.apache.axis2.AxisFault if an error occurs while adding the dispatcher
     */
    private void addDispatchers(AxisConfiguration mainAxisConfig) throws AxisFault {
        HandlerDescription handlerDescription = new HandlerDescription(MultitenantDispatcher.NAME);
        PhaseRule rule = new PhaseRule(PhaseMetadata.PHASE_DISPATCH);
        rule.setAfter(HTTPLocationBasedDispatcher.NAME);
        rule.setBefore("SynapseDispatcher");
        handlerDescription.setRules(rule);

        MultitenantDispatcher multitenantDispatcher = new MultitenantDispatcher();
        multitenantDispatcher.initDispatcher();
        handlerDescription.setHandler(multitenantDispatcher);

        List<Phase> inflowPhases
                = mainAxisConfig.getInFlowPhases();
        for (Phase inPhase : inflowPhases) {
            // we are interested about the Dispatch phase in the inflow
            if (PhaseMetadata.PHASE_DISPATCH.equals(inPhase.getPhaseName())) {
                boolean handlerFound = false;
                for (Handler handler : inPhase.getHandlers()) {
                    if (handler.getHandlerDesc().getName() != null &&
                            handler.getHandlerDesc().getName().equals(MultitenantDispatcher.NAME)) {
                        handlerFound = true;
                    }
                }
                if (!handlerFound) {
                    try {
                        inPhase.addHandler(handlerDescription);
                    } catch (PhaseException e) {
                        String msg = "Couldn't start Carbon, Cannot add " +
                                "the required Carbon handlers";
                        log.fatal(msg, e);
                        throw new AxisFault(msg);
                    }
                }
            }
        }
		if (log.isDebugEnabled()) {
			log.info("Added multitenant dispatchers");
		}
    }

    protected void deactivate(ComponentContext ctxt) {
        if (log.isDebugEnabled()) {
            log.debug("Deactivated TenantDispatcherServiceComponent");
        }
    }

    protected void setConfigurationContext(ConfigurationContextService configCtx) {
        this.configCtx = configCtx.getServerConfigContext();
    }

    protected void unsetConfigurationContext(ConfigurationContextService configCtx) {
        this.configCtx = null;
    }
}
