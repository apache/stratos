/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.ui.transports;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.ui.util.FileDownloadUtil;
import org.wso2.carbon.utils.ConfigurationContextService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FileDownloadServlet extends javax.servlet.http.HttpServlet {
    private static final Log log = LogFactory.getLog(org.wso2.carbon.ui.transports.FileDownloadServlet.class);

    private static final long serialVersionUID = 6074514253507510250L;

    private FileDownloadUtil fileDownloadUtil;

    private BundleContext context;

    private ConfigurationContextService configContextService;

    private ServletConfig servletConfig;

    public FileDownloadServlet(BundleContext context, ConfigurationContextService configContextService) {
        this.context = context;
        this.configContextService = configContextService;
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            fileDownloadUtil.acquireResource(configContextService, req, res);
        } catch (CarbonException e) {
            String msg = "Cannot download file";
            log.error(msg, e);
            throw new ServletException(e);
        }
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
        fileDownloadUtil = new FileDownloadUtil(context);
    }

    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }
}