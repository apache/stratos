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
package org.apache.stratos.redirector.servlet.ui.internal;

import org.apache.stratos.redirector.servlet.ui.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;

/**
 * @scr.component name="org.apache.stratosredirector.servlet.ui"
 * immediate="true"
 */
public class RedirectorServletUIComponent {
     private static Log log = LogFactory.getLog(RedirectorServletUIComponent.class);

    protected void activate(ComponentContext context) {
        try {
            Util.domainRegisterServlet(context.getBundleContext());
            log.debug("******* Multitenancy Redirector Servlet UI bundle is activated ******* ");
        } catch (Exception e) {
            log.error("******* Multitenancy Redirector Servlet UI bundle failed activating ****", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("******* Multitenancy Redirector Servlet UI bundle is deactivated ******* ");
    }
}
