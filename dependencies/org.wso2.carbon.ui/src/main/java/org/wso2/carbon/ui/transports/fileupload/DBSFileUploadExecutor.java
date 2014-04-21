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

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.servlet.ServletRequestContext;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.utils.CarbonUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
/*
 *
 */

public class DBSFileUploadExecutor extends org.wso2.carbon.ui.transports.fileupload.AbstractFileUploadExecutor {

    private static final String[] ALLOWED_FILE_EXTENSIONS = new String[]{".dbs"};

    public boolean execute(HttpServletRequest request,
                           HttpServletResponse response) throws CarbonException, IOException {
        String axis2Repo = ServerConfiguration.getInstance().
                getFirstProperty(ServerConfiguration.AXIS2_CONFIG_REPO_LOCATION);
        PrintWriter out = response.getWriter();
        if (CarbonUtils.isURL(axis2Repo)) {
            out.write("<script type=\"text/javascript\">" +
                      "alert('You are not permitted to upload services to URL repository " +
                      axis2Repo + "');" +
                      "</script>");
            out.flush();
            return false;
        }
        response.setContentType("text/html; charset=utf-8");
        ServletRequestContext servletRequestContext = new ServletRequestContext(request);
        boolean isMultipart =
                ServletFileUpload.isMultipartContent(servletRequestContext);
        if (isMultipart) {
            try {
                // Create a new file upload handler
                List items = parseRequest(servletRequestContext);

                // Process the uploaded items
                for (Iterator iter = items.iterator(); iter.hasNext();) {
                    FileItem item = (FileItem) iter.next();
                    if (!item.isFormField()) {
                        String fileName = item.getName();
                        String fileExtension = fileName;
                        fileExtension = fileExtension.toLowerCase();

                        // check whether extension is valid
                        checkServiceFileExtensionValidity(fileExtension, ALLOWED_FILE_EXTENSIONS);

                        String fileNameOnly = getFileName(fileName);
                        File uploadedFile;
                        if (fileExtension.endsWith(".dbs")) {
                            String repo =
                                    configurationContext.getAxisConfiguration().
                                            getRepository().getPath();
                            String finalFolderName;
                            if (fileExtension.endsWith(".dbs")) {
                                finalFolderName = "dataservices";
                            } else {
                                throw new CarbonException(
                                        "File with extension " + fileExtension + " is not supported!");
                            }

                            File servicesDir = new File(repo, finalFolderName);
                            if (!servicesDir.exists()) {
                                servicesDir.mkdir();
                            }
                            uploadedFile = new File(servicesDir, fileNameOnly);
                            item.write(uploadedFile);
                            //TODO: fix them
                            out.write("<script type=\"text/javascript\" src=\"../main/admin/js/main.js\"></script>");
                            out.write("<script type=\"text/javascript\">" +
                                      "alert('File uploaded successfully');" +
                                      "loadServiceListingPage();" +
                                      "</script>");
                        }
                    }
                return true;
                }
            } catch (Exception e) {
                log.error("File upload failed", e);                
                out.write("<script type=\"text/javascript\" src=\"../ds/extensions/core/js/data_service.js\"></script>");                
                out.write("<script type=\"text/javascript\">" +
                          "alert('Service file upload FAILED. You will be redirected to file upload screen. Reason :" +
                          e.getMessage().replaceFirst(",","") + "');" +
                          "loadDBSFileUploadPage();"+ //available in data_service.js
                          "</script>");
            }
        }
        return false;

    }
}
