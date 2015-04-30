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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;
import org.apache.stratos.common.beans.StatusResponseBean;
import org.apache.stratos.rest.endpoint.context.AuthenticationContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*
* authenticate an incoming request using the session availability. Session is first established using the
* basic auth authentication. This handler will be the last to get executed in the current handler chain. Hence failure
* too provide a session would result in an authentication failure.
* */
public class CookieBasedAuthenticationHandler implements RequestHandler {
    private Log log = LogFactory.getLog(CookieBasedAuthenticationHandler.class);

    public Response handleRequest(Message message, ClassResourceInfo classResourceInfo) {
        if (AuthenticationContext.isAthenticated()) {
            return null;
        }

        HttpServletRequest httpServletRequest = (HttpServletRequest) message.get("HTTP.REQUEST");
        HttpSession httpSession = httpServletRequest.getSession(false);
        if (httpSession != null && isUserLoggedIn(httpSession)) { // if sesion is avaialble
            String userName = (String) httpSession.getAttribute("userName");
            String tenantDomain = (String) httpSession.getAttribute("tenantDomain");
            int tenantId = (Integer) httpSession.getAttribute("tenantId");
            // the following will get used by the authorization handler..
            PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            carbonContext.setUsername(userName);
            carbonContext.setTenantDomain(tenantDomain);
            carbonContext.setTenantId(tenantId);

            AuthenticationContext.setAuthenticated(true);
            if (log.isDebugEnabled()) {
                log.debug("authenticated using the " + CookieBasedAuthenticationHandler.class.getName() + "for username  :" +
                        userName + "tenantDomain : " + tenantDomain + " tenantId : " + tenantId);
            }
            return null;

        }
        return Response.status(Response.Status.FORBIDDEN).
                type(MediaType.APPLICATION_JSON).entity(
                new StatusResponseBean(Response.Status.FORBIDDEN.getStatusCode(),
                        "The endpoint requires authentication")).build();
    }

    /*
    * if the userName and tenantDomain is present in the session, we conclude this as an authenticated session.
    * Thos params get set by the AuthenticationAdmin endpoint.
    * */
    private boolean isUserLoggedIn(HttpSession httpSession) {
        String userName = (String) httpSession.getAttribute("userName");
        String tenantDomain = (String) httpSession.getAttribute("tenantDomain");
        Integer tenantId = (Integer) httpSession.getAttribute("tenantId");
        if (userName != null && tenantDomain != null && tenantId != null) {
            return true;
        }
        return false;
    }

}
