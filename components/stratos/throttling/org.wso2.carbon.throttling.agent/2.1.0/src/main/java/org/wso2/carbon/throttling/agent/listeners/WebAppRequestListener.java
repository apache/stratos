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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.stratos.common.constants.StratosConstants;
import org.wso2.carbon.throttling.agent.ThrottlingAgent;
import org.wso2.carbon.throttling.agent.cache.TenantThrottlingInfo;
import org.wso2.carbon.throttling.agent.cache.ThrottlingActionInfo;
import org.wso2.carbon.tomcat.ext.valves.CarbonTomcatValve;
import org.wso2.carbon.user.api.UserStoreException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebAppRequestListener implements CarbonTomcatValve {

    private static final Log log = LogFactory.getLog(WebAppRequestListener.class);

    private static final Pattern servicesURLPattern = Pattern.compile("\\/services\\/t\\/(.*?)\\/");
    private static final Pattern webAppsURLPattern = Pattern.compile("\\/t\\/(.*?)\\/webapps\\/");

    private static final String CONTEXT_SERVICES = "services";
    private static final String CONTEXT_WEBAPPS = "webapps";

    private ThrottlingAgent throttlingAgent;

    public WebAppRequestListener(ThrottlingAgent throttlingAgent) {
        this.throttlingAgent = throttlingAgent;
    }

    public void invoke(HttpServletRequest request, HttpServletResponse response) {
        String requestURI = request.getRequestURI();
        String tenantDomainName = CarbonContext.getCurrentContext().
                                  getTenantDomain();
        String urlContext = getContext(requestURI);
        if (tenantDomainName != null && urlContext != null) {
            try {
                int tenantId = throttlingAgent.getRealmService().getTenantManager().
                        getTenantId(tenantDomainName);
                if (tenantId <= 0) {
                    //Allow to proceed
                } else {
                    if (!throttlingAgent.getRealmService().getTenantManager().getTenant(tenantId).
                            isActive()) {
                        //Check weather activated tenant or not
                        String msg = "You are sending request to a deactivated tenant. for Domain: "
                                     + tenantDomainName;
                        log.error(msg);
                        try {
                            response.sendError(403, msg);
                        } catch (IOException e) {
                            String message = "Error in sending throttling rule violation message by an inactive tenant." +
                                             " Tenant Domain: " + tenantDomainName;
                            log.error(message, e);
                        }
                    } else {
                        //check weather request come to webapps
                        if (CONTEXT_WEBAPPS.equals(urlContext)) {
                            //if tenant is active we will throttle other parameters such as bandwidth in/out
                            try {
                                TenantThrottlingInfo throttlingInfo =
                                        throttlingAgent.getThrottlingInfoCache().
                                                getTenantThrottlingInfo(tenantId);
                                if (throttlingInfo != null) {
                                    String[] actions =
                                            new String[]{StratosConstants.THROTTLING_WEBAPP_IN_BANDWIDTH_ACTION,
                                                         StratosConstants.THROTTLING_WEBAPP_OUT_BANDWIDTH_ACTION};
                                    ThrottlingActionInfo actionInfo;

                                    actionInfo = throttlingInfo.getThrottlingActionInfo(actions);
                                    if (actionInfo != null && actionInfo.isBlocked()) {
                                        String blockedMsg = actionInfo.getMessage();
                                        String msg = "This action is blocked. Reason: "
                                                     + blockedMsg;
                                        log.error(msg);
                                        response.sendError(509, msg);
                                    }
                                }
                            } catch (Exception ex) {
                                String msg = "Error in sending throttling rule violation message." +
                                             " Tenant Domain: " + tenantDomainName;
                                log.error(msg, ex);
                                return;
                            }
                        } else if (CONTEXT_SERVICES.equals(urlContext)) {
                            try {
                                TenantThrottlingInfo throttlingInfo =
                                        throttlingAgent.getThrottlingInfoCache().
                                                getTenantThrottlingInfo(tenantId);
                                if (throttlingInfo != null) {
                                    String[] actions =
                                            new String[]{StratosConstants.THROTTLING_SERVICE_IN_BANDWIDTH_ACTION,
                                                         StratosConstants.THROTTLING_SERVICE_OUT_BANDWIDTH_ACTION};
                                    ThrottlingActionInfo actionInfo;

                                    actionInfo = throttlingInfo.getThrottlingActionInfo(actions);
                                    if (actionInfo != null && actionInfo.isBlocked()) {
                                        String blockedMsg = actionInfo.getMessage();
                                        String msg = "This action is blocked. Reason: " +
                                                     blockedMsg;
                                        log.error(msg);
                                        response.sendError(509, msg);
                                    }
                                }
                            } catch (Exception ex) {
                                String msg = "Error in sending throttling rule violation message." +
                                             " Tenant Domain: " + tenantDomainName;
                                log.error(msg, ex);
                            }
                        }

                    }
                }
            } catch (UserStoreException e) {
                String msg = "Error in getting tenant id to evaluate throttling rule. " +
                             "Tenant Domain: " + tenantDomainName;
                log.error(msg, e);
            }

        }
    }

    /**
     * Extract tenant domain from request url
     *
     * @return tenant domain
     */
    public String getTenantName(String requestUrl) {
        Matcher matcher = servicesURLPattern.matcher(requestUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }

        matcher = webAppsURLPattern.matcher(requestUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return MultitenantConstants.SUPER_TENANT_DOMAIN_NAME;
    }

    /**
     * Extract context from the request url
     *
     * @return context string
     */
    public String getContext(String requestUrl) {
        if (requestUrl.contains("/services") && requestUrl.contains("/t")) {
            return CONTEXT_SERVICES;
        }

        if (requestUrl.contains("/t") && requestUrl.contains("/webapps")) {
            return CONTEXT_WEBAPPS;
        }
        return null;
    }
}
