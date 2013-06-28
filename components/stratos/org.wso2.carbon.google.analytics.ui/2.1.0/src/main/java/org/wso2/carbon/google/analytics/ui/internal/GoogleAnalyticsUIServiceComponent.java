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
package org.wso2.carbon.google.analytics.ui.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;

import javax.servlet.ServletContext;

/**
 * @scr.component name="org.wso2.carbon.google.analyzer.ui" immediate="true"
 * @scr.reference name="servlet.context.service"
 * interface="javax.servlet.ServletContext"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setServletContextService"
 * unbind="unsetServletContextService"
 */

/**
 * org.wso2.carbon.google.analytics.ui bundle has implemented for giving google-analytics support
 * for stratos.
 */

public class GoogleAnalyticsUIServiceComponent {

    private static final Log log = LogFactory.getLog(GoogleAnalyticsUIServiceComponent.class);

    private static ServletContext servletCtx = null;

    protected void activate(ComponentContext context) {
        try {
            log.debug("Google Analytics UI Component bundle is activated");
        } catch (Exception e) {
            log.debug("Failed to activate Google Analytics UI Component bundle");
        }
    }

    protected void deactivate(ComponentContext context) {
        log.debug("Google Analytics UI Component bundle is deactivated");
    }

    protected void setServletContextService(ServletContext servletContext) {
        this.servletCtx = servletContext;

    }

    protected void unsetServletContextService(ServletContext servletContext) {
    }
}


