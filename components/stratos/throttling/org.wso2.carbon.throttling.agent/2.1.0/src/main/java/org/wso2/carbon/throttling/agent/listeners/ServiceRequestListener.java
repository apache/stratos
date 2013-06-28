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
package org.wso2.carbon.throttling.agent.listeners;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.core.transports.metering.MeteredServletRequest;
import org.wso2.carbon.throttling.agent.cache.ThrottlingActionInfo;
import org.wso2.carbon.throttling.agent.cache.ThrottlingInfoCache;
import org.wso2.carbon.throttling.agent.internal.ThrottlingAgentServiceComponent;

/**
 * Checks whether the axis2 operations (service calls) are allowed. 
 */
public class ServiceRequestListener extends AbstractHandler {
    private static final Log log = LogFactory.getLog(ServiceRequestListener.class);

    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Staring throttling handler invocation. Incoming message: " +
                    messageContext.getEnvelope().toString());
        }
        AxisService service = messageContext.getAxisService();
        Parameter param = service.getParameter("adminService");
        
        if (param != null && "true".equals(param.getValue())) {
            //We will allow admin services to proceed.
            return InvocationResponse.CONTINUE;
        }
        
        int tenantId = getTenantId(messageContext);
        if(tenantId <= 0){
            //We can allow all super tenant actions
            return InvocationResponse.CONTINUE;
        }
        
        ThrottlingInfoCache throttlingInfoCache = ThrottlingAgentServiceComponent.getThrottlingAgent().getThrottlingInfoCache();
        String[] actions = new String[]{StratosConstants.THROTTLING_SERVICE_IN_BANDWIDTH_ACTION, 
                                        StratosConstants.THROTTLING_SERVICE_OUT_BANDWIDTH_ACTION,
                                        StratosConstants.THROTTLING_SERVICE_REQUEST_ACTION,
                                        StratosConstants.THROTTLING_SERVICE_RESPONSE_ACTION
                                        };
        ThrottlingActionInfo actionInfo = throttlingInfoCache.getTenantThrottlingInfo(tenantId).getThrottlingActionInfo(actions);

        if (actionInfo.isBlocked()) {
            String blockedMsg = actionInfo.getMessage();
            String msg = "The throttling action is blocked. message: " + blockedMsg;
            log.error(msg);
            // we are only throwing the blocked exception, as it is a error message for the user
            throw new AxisFault(blockedMsg);
        }

        return InvocationResponse.CONTINUE;
    }
    
    private int getTenantId(MessageContext messageContext) {
        Object obj = messageContext.getProperty("transport.http.servletRequest");
        if (obj == null) {
            // TODO: check for cause of the error.
            log.debug("Servlet request is null. Skip monitoring.");
            return 0;
        }
        if (!(obj instanceof MeteredServletRequest)) {
            log.debug("HttpServletRequest is not of type MeteredServletRequest. Skip monitoring.");
            return 0;
        }

        MeteredServletRequest servletRequest = (MeteredServletRequest) obj;
        String address = servletRequest.getRequestURI();
        String servicesPrefix = "/services/t/";
        if (address != null && address.contains(servicesPrefix)) {
            int domainNameStartIndex =
                    address.indexOf(servicesPrefix) + servicesPrefix.length();
            int domainNameEndIndex = address.indexOf('/', domainNameStartIndex);
            String domainName = address.substring(domainNameStartIndex,
                    domainNameEndIndex == -1 ? address.length() : domainNameEndIndex);

            // return tenant id if domain name is not null
            if (domainName != null) {
                try {
                    return ThrottlingAgentServiceComponent.getThrottlingAgent().getRealmService().getTenantManager().getTenantId(domainName);
                } catch (org.wso2.carbon.user.api.UserStoreException e) {
                    log.error("An error occurred while obtaining the tenant id.", e);
                }
            }
        }

        // return 0 if the domain name is null
        return 0;
    }

}
