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
package org.apache.stratos.rest.endpoint.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.rest.endpoint.Utils;
import org.apache.stratos.rest.endpoint.annotation.AuthorizationAction;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/auth/")
public class AuthenticationApi extends AbstractApi {
    Log log = LogFactory.getLog(AuthenticationApi.class);
    // TODO: investigate on the thread safety of this apprach...
    @Context
    HttpServletRequest httpServletRequest;


    /*
    This method gets called by the client who are interested in using session mechanism to authenticate themselves in
    subsequent calls. This method call get authenticated by the basic authenticator.
    Once the authenticated call received, the method creates a session.

     */
    @GET
    @Path("/cookie")
    @Produces("application/json")
    @Consumes("application/json")
    @AuthorizationAction("/permission/protected/manage/monitor/tenants")
    public Response getCookie(){
       HttpSession httpSession = httpServletRequest.getSession(true);//create session if not found
       PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
       httpSession.setAttribute("userName",carbonContext.getUsername());
       httpSession.setAttribute("tenantDomain",carbonContext.getTenantDomain());
       httpSession.setAttribute("tenantId",carbonContext.getTenantId());

       String sessionId = httpSession.getId();
        return Response.ok().header("WWW-Authenticate", "Basic").type(MediaType.APPLICATION_JSON).
                entity(Utils.buildAuthenticationSuccessMessage(sessionId)).build();
    }


}
