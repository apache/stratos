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
package org.apache.stratos.rest.endpoint.handlers;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.cxf.security.SecurityContext;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.context.AuthenticationContext;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.security.StratosSecurityContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.common.AuthenticationException;
import org.wso2.carbon.core.util.AnonymousSessionUtil;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Here we are doing the request authentication within a {@link RequestHandler}. The request handlers
 * are get invoked just before the actual method invocation. This authentication handler make use
 * of HTTP basic auth headers as the authentication mechanism.
 */
public class StratosAuthenticationHandler extends AbstractAuthenticationAuthorizationHandler {
    private static Log log = LogFactory.getLog(StratosAuthenticationHandler.class);
    private static String SUPPORTED_AUTHENTICATION_TYPE = "Basic";

    public boolean canHandle(String authHeaderPrefix){
        return SUPPORTED_AUTHENTICATION_TYPE.equals(authHeaderPrefix);
    }

    /**
     * Authenticate the user against the user store. Once authenticate, populate the {@link org.wso2.carbon.context.CarbonContext}
     * to be used by the downstream code.
     * @param message
     * @param classResourceInfo
     * @return
     */
    public Response handle(Message message, ClassResourceInfo classResourceInfo) {
        if(log.isDebugEnabled()) {
            log.debug(String.format("Authenticating request: [message-id] %s", message.getId()));
        }

    	// If Mutual SSL is enabled
        HttpServletRequest request = (HttpServletRequest) message.get("HTTP.REQUEST");
        Object certObject = request.getAttribute("javax.servlet.request.X509Certificate");
        
        AuthorizationPolicy policy = message.get(AuthorizationPolicy.class);
        String username = policy.getUserName().trim();
        String password = policy.getPassword().trim();

        //sanity check
        if (StringUtils.isEmpty(username)) {
            log.error("username is seen as null/empty values");
            return Response.status(Response.Status.UNAUTHORIZED)
                           .header("WWW-Authenticate", "Basic").type(MediaType.APPLICATION_JSON)
                           .entity(Utils.buildMessage("Username cannot be null")).build();
        } else if (certObject == null && (StringUtils.isEmpty(password))) {
            log.error("password is seen as null/empty values");
            return Response.status(Response.Status.UNAUTHORIZED)
                           .header("WWW-Authenticate", "Basic").type(MediaType.APPLICATION_JSON)
                           .entity(Utils.buildMessage("password cannot be null")).build();
        }
        
        try {
            RealmService realmService = ServiceHolder.getRealmService();
            RegistryService registryService = ServiceHolder.getRegistryService();
            String tenantDomain = MultitenantUtils.getTenantDomain(username);
            int tenantId = realmService.getTenantManager().getTenantId(tenantDomain);
            
            UserRealm userRealm = null;
            if (certObject == null) {
                userRealm = AnonymousSessionUtil.getRealmByTenantDomain(registryService, realmService, tenantDomain);
                if (userRealm == null) {
                    log .error("Invalid domain or unactivated tenant login");
                    // is this the correct HTTP code for this scenario ? (401)
                    return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic").
                            type(MediaType.APPLICATION_JSON).entity(Utils.buildMessage("Tenant not found")).build();
                }
            }
            username = MultitenantUtils.getTenantAwareUsername(username);
            if (certObject != null || userRealm.getUserStoreManager().authenticate(username, password)) {  // if authenticated

                // setting the correct tenant info for downstream code..
                PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                carbonContext.setTenantDomain(tenantDomain);
                carbonContext.setTenantId(tenantId);
                carbonContext.setUsername(username);
                //populate the secuirtyContext of authenticated user
                SecurityContext securityContext = new StratosSecurityContext(username);
                message.put(SecurityContext.class, securityContext);

                // set the authenticated flag and let the request to continue
                AuthenticationContext.setAuthenticated(true);
                if (log.isDebugEnabled()) {
                    log.debug("Authenticated using the " + CookieBasedAuthenticationHandler.class.getName() + "for username  :" +
                            username + "tenantDomain : " + tenantDomain + " tenantId : " + tenantId);
                }
                return null;
            } else {
                log.warn(String.format("Unable to authenticate the request: [message-id] %s", message.getId()));
                // authentication failed, request the authetication, add the realm name if needed to the value of WWW-Authenticate
                return Response.status(Response.Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic").
                        type(MediaType.APPLICATION_JSON).entity(Utils.buildMessage("Authentication failed. Please " +
                        "check your username/password")).build();
            }
        } catch (Exception exception) {
            log.error(String.format("Authentication failed: [message-id] %s", message.getId()),exception);
            // server error in the eyes of the client. Hence 5xx HTTP code.
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).
                    entity(Utils.buildMessage("Unexpected error. Please contact the system admin")).build();
        }
    }
}
