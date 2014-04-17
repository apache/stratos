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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.ui.transports.fileupload.FileUploadExecutorManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class FileUploadServlet extends javax.servlet.http.HttpServlet {
    private static final Log log = LogFactory.getLog(org.wso2.carbon.ui.transports.FileUploadServlet.class);

    private static final long serialVersionUID = 8089568022124816379L;

    private FileUploadExecutorManager fileUploadExecutorManager;

    private BundleContext bundleContext;

    private ConfigurationContext configContext;

    private String webContext;

    private ServletConfig servletConfig;

    public FileUploadServlet(BundleContext context, ConfigurationContext configCtx, String webContext) {
        this.bundleContext = context;
        this.configContext = configCtx;
        this.webContext = webContext;
    }

    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws ServletException, IOException {

        try {
            fileUploadExecutorManager.execute(request, response);
        } catch (Exception e) {
            String msg = "File upload failed ";
            log.error(msg, e);
            throw new ServletException(e);
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
    }


    public void init(ServletConfig servletConfig) throws ServletException {
        this.servletConfig = servletConfig;
        try {
            fileUploadExecutorManager = new FileUploadExecutorManager(bundleContext, configContext, webContext);
            //Registering FileUploadExecutor Manager as an OSGi service
            bundleContext.registerService(FileUploadExecutorManager.class.getName(), fileUploadExecutorManager, null);
        } catch (CarbonException e) {
            log.error("Exception occurred while trying to initialize FileUploadServlet", e);
            throw new ServletException(e);
        }
    }

    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }
}