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

package org.apache.stratos.sso.redirector.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * @scr.component name="org.apache.stratos.sso.redirector.ui"
 * immediate="true"
 */
public class SSORedirectorServiceComponent {
    private static Log log = LogFactory.getLog(SSORedirectorServiceComponent.class);

    protected void activate(ComponentContext ctxt) {

        // register a servlet filter for SSO redirector page
        HttpServlet redirectJSPRedirectorServlet = new HttpServlet() {
            protected void doGet(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
            }
        };

        Filter redirectPageFilter = new RedirectorJSPFilter();
        Dictionary redirectorPageFilterAttrs = new Hashtable(2);
        Dictionary redirectorPageFilterParams = new Hashtable(2);
        redirectorPageFilterParams.put("url-pattern", "/carbon/sso-acs/redirect_ajaxprocessor.jsp");
        redirectorPageFilterParams.put("associated-filter", redirectPageFilter);
        redirectorPageFilterParams.put("servlet-attributes", redirectorPageFilterAttrs);
        ctxt.getBundleContext().registerService(Servlet.class.getName(), redirectJSPRedirectorServlet,
                                                redirectorPageFilterParams);
        log.debug("Stratos SSO Redirector bundle is activated..");
    }

    protected void deactivate(ComponentContext ctxt) {
        log.debug("Stratos SSO Redirector bundle is deactivated..");
    }
}
