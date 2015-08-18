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
package org.wso2.carbon.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.wso2.carbon.ui.action.ActionHelper;
import org.wso2.carbon.ui.deployment.beans.CarbonUIDefinitions;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

public class TilesJspServlet extends JspServlet {
    private static final long serialVersionUID = 1L;
    private static Log log = LogFactory.getLog(TilesJspServlet.class);

    public TilesJspServlet(Bundle bundle, UIResourceRegistry uiResourceRegistry) {
        super(bundle, uiResourceRegistry);
    }

    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String actionUrl = request.getRequestURI();

        //This is the layout page defined in
        //"/org.wso2.carbon.component/src/main/resources/web/WEB-INF/tiles/main_defs.xml"
        //Need to serve http requests other than to tiles body page,
        //using the normal OSGi way

        //retrieve urls that should be by-passed from tiles servlet
        String resourceURI = actionUrl.replaceFirst(request.getContextPath() + "/", "../");

        HashMap<String, String> urlsToBeByPassed = new HashMap<String, String>();
        if (bundle != null) {
            ServiceReference reference = bundle.getBundleContext()
                    .getServiceReference(CarbonUIDefinitions.class.getName());
            CarbonUIDefinitions carbonUIDefinitions = null;
            if (reference != null) {
                carbonUIDefinitions =
                        (CarbonUIDefinitions) bundle.getBundleContext().getService(reference);
                if (carbonUIDefinitions != null) {
                    urlsToBeByPassed = carbonUIDefinitions.getSkipTilesUrls();
                }
            }
        }

        //if the current uri is marked to be by-passed, let it pass through
        if (!urlsToBeByPassed.isEmpty()) {
            if (urlsToBeByPassed.containsKey(resourceURI)) {
                super.service(request, response);
                return;
            }
        }


        if ((actionUrl.lastIndexOf("/admin/layout/template.jsp") > -1)
                || actionUrl.lastIndexOf("ajaxprocessor.jsp") > -1
                || actionUrl.indexOf("gadgets/js") > -1) {
            super.service(request, response);
        } else if (actionUrl.startsWith("/carbon/registry/web/resources/foo/bar")) {
            //TODO : consider the renamed ROOT war scenario
            String resourcePath = actionUrl.replaceFirst("/carbon/registry/web/", "");
            String pathToRegistry = "path=" + resourcePath;
            if (log.isTraceEnabled()) {
                log.trace("Forwarding to registry : " + pathToRegistry);
            }
            RequestDispatcher dispatcher =
                    request.getRequestDispatcher("../registry/registry-web.jsp?" + pathToRegistry);
            dispatcher.forward(request, response);
        } else {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("request.getContextPath() : " + request.getContextPath());
                    log.debug("actionUrl : " + actionUrl);
                }
                String newPath = actionUrl.replaceFirst(request.getContextPath(), "");

                //todo: Insert filter work here

                ActionHelper.render(newPath, request, response);
            } catch (Exception e) {
                log.fatal("Fatal error occurred while rendering UI using Tiles.", e);
            }
        }
    }
}
