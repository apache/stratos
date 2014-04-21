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

package org.wso2.carbon.ui.transports.fileupload;

import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
/*
 * 
 */

public class KeyStoreFileUploadExecutor extends org.wso2.carbon.ui.transports.fileupload.AbstractFileUploadExecutor {

    public boolean execute(HttpServletRequest request, HttpServletResponse response)
            throws CarbonException, IOException {
        ServletRequestContext servletRequestContext = new ServletRequestContext(request);
        boolean isMultipart =
                ServletFileUpload.isMultipartContent(servletRequestContext);
        PrintWriter out = response.getWriter();
        response.setContentType("text/html; charset=utf-8");
        try {
            if (isMultipart) {
                List items = parseRequest(servletRequestContext);
                // Process the uploaded items
                for (Iterator iter = items.iterator(); iter.hasNext();) {
                    FileItem item = (FileItem) iter.next();
                    if (!item.isFormField()) {
                        String uuid = String.valueOf(System.currentTimeMillis() + Math.random());
                        String ksUploadDir =
                                configurationContext.getProperty(ServerConstants.WORK_DIR) +
                                File.separator + "keystores" +
                                File.separator + uuid + File.separator;

                        File dirs = new File(ksUploadDir);
                        if (!dirs.exists()) {
                            dirs.mkdirs();
                        }
                        File uploadedFile = new File(ksUploadDir,
                                                     getFileName(item.getName()));
                        item.write(uploadedFile);
                        Map fileResourceMap =
                                (Map) configurationContext.getProperty(ServerConstants.FILE_RESOURCE_MAP);
                        if (fileResourceMap == null) {
                            fileResourceMap = new TreeBidiMap();
                            configurationContext.setProperty(ServerConstants.FILE_RESOURCE_MAP,
                                                             fileResourceMap);
                        }
                        fileResourceMap.put(uuid, uploadedFile.getAbsolutePath());
                        item.write(uploadedFile);

                        // call the javascript which will in turn call the relevant web service
                        out.write("<script type=\"text/javascript\">" +
                                  "top.getKeystoreUUID('" + uuid + "');" +
                                  "</script>");
                    }
                }
                out.flush();
            }
        } catch (Exception e) {
            log.error("KeyStore file upload failed", e);
            out.write("<script type=\"text/javascript\">" +
                      "top.wso2.wsf.Util.alertWarning('KeyStore file upload FAILED. Reason : " + e + "');" +
                      "</script>");
        } finally {
            out.close();
        }
        return true;
    }
}
