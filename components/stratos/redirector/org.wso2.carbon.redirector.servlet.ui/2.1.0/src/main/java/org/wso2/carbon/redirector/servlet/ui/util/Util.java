/*
 * Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.redirector.servlet.ui.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wso2.carbon.redirector.servlet.ui.filters.AllPagesFilter;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.servlet.Filter;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

public class Util {

    private static final Log log = LogFactory.getLog(Util.class);

    private static ServiceRegistration redirectorServiceRegistration;

    public static void domainRegisterServlet(BundleContext bundleContext) throws Exception {
        if (!CarbonUtils.isRemoteRegistry()) {
            HttpServlet reirectorServlet = new HttpServlet() {
                // the redirector filter will forward the request before this servlet is hit
                protected void doGet(HttpServletRequest request, HttpServletResponse response)
                        throws ServletException, IOException {
                }
            };

            Filter redirectorFilter = new AllPagesFilter();

            Dictionary redirectorServletAttributes = new Hashtable(2);
            Dictionary redirectorParams = new Hashtable(2);
            redirectorParams.put("url-pattern", "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX);
            redirectorParams.put("associated-filter", redirectorFilter);
            redirectorParams.put("servlet-attributes", redirectorServletAttributes);

            redirectorServiceRegistration = bundleContext.registerService(Servlet.class.getName(),
                    reirectorServlet, redirectorParams);

        }
    }
}
