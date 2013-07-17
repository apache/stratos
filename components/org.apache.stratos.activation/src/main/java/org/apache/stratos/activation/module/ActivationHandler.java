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
package org.apache.stratos.activation.module;

import javax.xml.namespace.QName;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.soap.SOAPBody;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.engine.Handler;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.activation.utils.ActivationManager;
import org.apache.stratos.activation.utils.Util;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.multitenancy.utils.TenantAxisUtils;
import org.apache.stratos.common.util.CloudServicesUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

public class ActivationHandler extends AbstractHandler implements Handler {

    private static final Log log = LogFactory.getLog(ActivationHandler.class);

    private String name = "ActivationHandler";

    /**
     * {@inheritDoc}
     */
    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        String serviceName = Util.getServiceName();
        //TODO - tenant id and domain should retrieve from thread local carbon context
        //If service is api manager then we will pass activation handler check
        if(serviceName.equalsIgnoreCase("WSO2 API Manager")) {
            return InvocationResponse.CONTINUE;
        }
        if(log.isDebugEnabled()){
            log.debug("Starting Activation Handler invocation. Incoming Message: " +
                    messageContext.getEnvelope().toString());
        }

        AxisService service = messageContext.getAxisService();
        int tenantId = getTenantId(messageContext);
        if (service != null && "ActivationService".equals(service.getName())) {
            log.debug("Granted access to the Activation Service");
            if (tenantId > 0) {
                TenantAxisUtils.getTenantAxisConfiguration(getTenantDomain(messageContext),
                        messageContext.getConfigurationContext());
                log.debug("Loaded Tenant Configuration");
            }
            return InvocationResponse.CONTINUE;
        }
        if (tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            log.debug("Granted access for super tenant");
            return InvocationResponse.CONTINUE;
        }
        if (ActivationManager.activationRecorded(tenantId)) {
            if (ActivationManager.getActivation(tenantId)) {
                TenantAxisUtils.getTenantAxisConfiguration(getTenantDomain(messageContext),
                        messageContext.getConfigurationContext());
                log.debug("Loaded Tenant Configuration");
                return InvocationResponse.CONTINUE;
            } else {
                if (log.isWarnEnabled()) {
                    log.warn("Failed attempt to access " + serviceName + " by tenant " + tenantId);
                }
                return InvocationResponse.ABORT;
            }
        }

        try {
            if (CloudServicesUtil.isCloudServiceActive(serviceName, tenantId)) {
                log.debug("Successful attempt to access " + serviceName + " by tenant " + tenantId);
                ActivationManager.setActivation(tenantId, true);
                TenantAxisUtils.getTenantAxisConfiguration(getTenantDomain(messageContext),
                        messageContext.getConfigurationContext());
                log.debug("Loaded Tenant Configuration");
                return InvocationResponse.CONTINUE;
            }
        } catch (Exception e) {
            throw new AxisFault("Failed to determine Activation status.", e);
        }
        log.warn("Failed attempt to access " + serviceName + " by tenant " + tenantId);
        ActivationManager.setActivation(tenantId, false);
        return InvocationResponse.ABORT;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return name;
    }

    /**
     * Method to set the name of the activation handler.
     *
     * @param name the name.
     */
    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    private String getTenantDomain(MessageContext messageContext) {
        //Ideally this should take from thread local carbon context change this temporary
        return PrivilegedCarbonContext.getCurrentContext(messageContext).getTenantDomain();
    }

    private int getTenantId(MessageContext messageContext) {
        //Ideally this should take from thread local carbon context change this temporary
        PrivilegedCarbonContext carbonContext =
                PrivilegedCarbonContext.getCurrentContext(messageContext);
        int tenantId = carbonContext.getTenantId();
        if (tenantId > -1 || tenantId == MultitenantConstants.SUPER_TENANT_ID) {
            return tenantId;
        }
        String domain = carbonContext.getTenantDomain();
        if (domain == null) {
            SOAPBody soapBody = messageContext.getEnvelope().getBody();
            if (soapBody != null && soapBody.getFirstElement() != null) {
                OMElement usernameElem = soapBody.getFirstElement().getFirstChildWithName(
                        new QName(ServerConstants.AUTHENTICATION_SERVICE_NS,
                                ServerConstants.AUTHENTICATION_SERVICE_USERNAME));
                if (usernameElem != null) {
                    String userName = usernameElem.getText();
                    domain = MultitenantUtils.getTenantDomain(userName);
                }
            }
        }
        if (domain != null) {
            try {
                tenantId = Util.getRealmService().getTenantManager().getTenantId(domain);
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                log.error("An error occurred while obtaining the tenant id.", e);
            }
        }
        return tenantId;
    }

}
