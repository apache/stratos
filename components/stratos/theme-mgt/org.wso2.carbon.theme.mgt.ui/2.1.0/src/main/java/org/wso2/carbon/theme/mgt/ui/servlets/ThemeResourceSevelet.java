/*
 *  Copyright (c) 2005-2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.theme.mgt.ui.servlets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.theme.mgt.stub.registry.resource.stub.beans.xsd.ContentDownloadBean;
import org.wso2.carbon.theme.mgt.ui.clients.ThemeMgtServiceClient;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

public class ThemeResourceSevelet extends HttpServlet {

    private static final Log log = LogFactory.getLog(ThemeResourceSevelet.class);

    private ServletConfig servletConfig;

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        this.servletConfig = servletConfig;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        try {
            ThemeMgtServiceClient client = new ThemeMgtServiceClient(servletConfig, request.getSession());
            String path = request.getParameter("path");
            String viewImage = request.getParameter("viewImage");
            if (path == null) {
                String msg = "Could not get the resource content. Path is not specified.";
                log.error(msg);
                response.setStatus(400);
                return;
            }

            ContentDownloadBean bean = client.getContentDownloadBean(path);

            InputStream contentStream = null;
            if (bean.getContent() != null) {
                contentStream = bean.getContent().getInputStream();
            } else {
                String msg = "The resource content was empty.";
                log.error(msg);
                response.setStatus(204);
                return;
            }

            response.setDateHeader("Last-Modified", bean.getLastUpdatedTime().getTime().getTime());
            String ext = "jpg";
            if (path.lastIndexOf(".") < path.length() -1 && path.lastIndexOf(".") > 0) {
                ext = path.substring(path.lastIndexOf(".") + 1);
            }

            if (viewImage != null && viewImage.equals("1")) {
                response.setContentType("img/" + ext);
            }
            else {
                if (bean.getMediatype() != null && bean.getMediatype().length() > 0) {
                    response.setContentType(bean.getMediatype());
                } else {
                    response.setContentType("application/download");
                }

                if (bean.getResourceName() != null) {
                    response.setHeader(
                            "Content-Disposition", "attachment; filename=\"" + bean.getResourceName() + "\"");
                }
            }

            if (contentStream != null) {

                ServletOutputStream servletOutputStream = null;
                try {
                    servletOutputStream = response.getOutputStream();

                    byte[] contentChunk = new byte[1024];
                    int byteCount;
                    while ((byteCount = contentStream.read(contentChunk)) != -1) {
                        servletOutputStream.write(contentChunk, 0, byteCount);
                    }

                    response.flushBuffer();
                    servletOutputStream.flush();

                } finally {
                    contentStream.close();

                    if (servletOutputStream != null) {
                        servletOutputStream.close();
                    }
                }
            }
        } catch (Exception e) {

            String msg = "Failed to get resource content. " + e.getMessage();
            log.error(msg, e);
            response.setStatus(500);
        }
    }
}
