/*
 * Copyright 2005,2006 WSO2, Inc. http://www.wso2.org
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
package org.wso2.carbon.ui.transports.fileupload;

import org.apache.commons.collections.bidimap.TreeBidiMap;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handles Jar, Zip file upload and creating a service archive out of it
 * This class is totally comply with add_new_jar_zip.xsl. Thus, should not use in general purpose
 * activities.
 */
public class JarZipUploadExecutor extends org.wso2.carbon.ui.transports.fileupload.AbstractFileUploadExecutor {

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
                Map fileResourceMap =
                        (Map) configurationContext
                                .getProperty(ServerConstants.FILE_RESOURCE_MAP);
                if (fileResourceMap == null) {
                    fileResourceMap = new TreeBidiMap();
                    configurationContext.setProperty(ServerConstants.FILE_RESOURCE_MAP,
                                                     fileResourceMap);
                }
                List resourceUUID = new ArrayList();
                String main = null;
                for (Iterator iterator = items.iterator(); iterator.hasNext();) {
                    DiskFileItem item = (DiskFileItem) iterator.next();
                    if (!item.isFormField()) {
                        String uuid = String.valueOf(System.currentTimeMillis() + Math.random());
                        String extraFileLocation =
                                configurationContext.getProperty(ServerConstants.WORK_DIR) +
                                File.separator + "extra" +
                                File.separator + uuid + File.separator;
                        String fieldName = item.getFieldName();
                        if (fieldName != null && fieldName.equals("jarZipFilename")) {
                            File dirs = new File(extraFileLocation);
                            dirs.mkdirs();
                            String fileName = item.getName();
                            String fileExtension = fileName;
                            checkServiceFileExtensionValidity(fileExtension,
                                                              new String[]{".jar", ".zip"});
                            File uploadedFile = new File(extraFileLocation,
                                                         getFileName(fileName));
                            item.write(uploadedFile);
                            main = uuid;
                            resourceUUID.add(uuid);
                            fileResourceMap.put(uuid, uploadedFile.getAbsolutePath());
                        }

                        if (fieldName != null && fieldName.equals("jarResource")) {
                            String fileName = item.getName();
                            if (fileName.toLowerCase().endsWith(".jar")) {
                                File dirs = new File(extraFileLocation);
                                dirs.mkdirs();
                                File uploadedFile = new File(extraFileLocation,
                                                             getFileName(fileName));
                                item.write(uploadedFile);
                                resourceUUID.add(uuid);
                                fileResourceMap.put(uuid, uploadedFile.getAbsolutePath());
                            }
                        }
                    }
                }
                if (main == null) {
                    out.write("<script type=\"text/javascript\">" +
                              "top.wso2.wsf.Util.alertWarning('Please upload a jar or a zip file.');" +
                              "</script>");
                }

                String s = "var uObj = new Object();";
                for (int i = 0; i < resourceUUID.size(); i++) {
                    s += "uObj[" + i + "]=\"" + resourceUUID.get(i) + "\";\n";
                }
                out.write("<script type=\"text/javascript\">" +
                          s +
                          "top." + "jarZipFileUploadExecutor" + "(\"" + main + "\",uObj);" +
                          "</script>");
                out.flush();
            }
        } catch (Exception e) {
            log.error("File upload FAILED", e);
            out.write("<script type=\"text/javascript\">" +
                      "top.wso2.wsf.Util.alertWarning('File upload FAILED. File may be non-existent or invalid.');" +
                      "</script>");
        } finally {
            out.close();
        }
        return true;

    }
}
