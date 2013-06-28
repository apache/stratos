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
package org.wso2.carbon.usage.agent.listeners.axis2;

import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.core.transports.metering.MeteredServletRequest;
import org.wso2.carbon.usage.agent.util.Util;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * this class is used to obtain tenant id from MeteredServletRequest or MessageContext
 */

public class RequestMeteringHandler extends AbstractHandler {
    private static final Log log = LogFactory.getLog(RequestMeteringHandler.class);

    /**
     * this method  invoke  MeteredServletRequest and return a InvocationResponse
     * @param messageContext  MessageContext
     * @return InvocationResponse
     * @throws AxisFault
     */

    public InvocationResponse invoke(MessageContext messageContext) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("Staring metering handler invocation. Incoming message: " +
                    messageContext.getEnvelope().toString());
        }
        AxisService service = messageContext.getAxisService();
        Parameter param = service.getParameter("adminService");

        Object obj = messageContext.getProperty("transport.http.servletRequest");
        if (obj == null) {
            // TODO: check for cause of the error.
            log.debug("Servlet request is null. Skip monitoring.");
            return InvocationResponse.CONTINUE;
        }
        if (!(obj instanceof MeteredServletRequest)) {
            log.debug("HttpServletRequest is not of type MeteredServletRequest. Skip monitoring.");
            return InvocationResponse.CONTINUE;
        }

        MeteredServletRequest servletRequest = (MeteredServletRequest) obj;

        if (param != null && "true".equals(param.getValue())) {
            servletRequest.setAttribute(StratosConstants.ADMIN_SERVICE_SERVLET_ATTR, "true");
            return InvocationResponse.CONTINUE;
        }
        servletRequest.setAttribute(StratosConstants.SERVICE_NAME_SERVLET_ATTR, service.getName());

        int tenantId = getTenantId(servletRequest);
        servletRequest.setAttribute(StratosConstants.TENANT_ID_SERVLET_ATTR, tenantId);

        return InvocationResponse.CONTINUE;
    }

    /**
     * method to get tenant id from MeteredServletRequest
     * @param servletRequest MeteredServletRequest
     * @return tenant id
     */

    private int getTenantId(MeteredServletRequest servletRequest) {
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
                    return Util.getRealmService().getTenantManager().getTenantId(domainName);
                } catch (org.wso2.carbon.user.api.UserStoreException e) {
                    log.error("An error occurred while obtaining the tenant id.", e);
                }
            }
        }

        // return 0 if the domain name is null
        return 0;
    }
}
