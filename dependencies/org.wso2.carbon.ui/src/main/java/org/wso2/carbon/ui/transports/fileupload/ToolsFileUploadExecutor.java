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

import org.wso2.carbon.CarbonException;
import org.wso2.carbon.utils.FileItemData;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class ToolsFileUploadExecutor extends AbstractFileUploadExecutor {
    public boolean execute(HttpServletRequest request, HttpServletResponse response)
            throws CarbonException, IOException {
        PrintWriter out = response.getWriter();
        try {
            List<FileItemData> fileItems = getAllFileItems();
            
            StringBuffer filePathsStrBuffer = new StringBuffer();
            
            for (FileItemData fileItem : fileItems) {
                String uuid = String.valueOf(
                        System.currentTimeMillis() + Math.random());
                String serviceUploadDir =
                        configurationContext
                                .getProperty(ServerConstants.WORK_DIR) +
                                File.separator +
                                "extra" + File
                                .separator +
                                uuid + File.separator;
                File dir = new File(serviceUploadDir);
                if (!dir.exists()) {
                    boolean dirCreated = dir.mkdirs();
                    if (!dirCreated) {
                    	log.error("Error creating dir " + dir.getPath());
                    	return false;
                    }
                }
                File uploadedFile = new File(dir, uuid);
                FileOutputStream fileOutStream = new FileOutputStream(uploadedFile);
                fileItem.getDataHandler().writeTo(fileOutStream);
                fileOutStream.flush();
                fileOutStream.close();
                response.setContentType("text/plain; charset=utf-8");
                filePathsStrBuffer.append(uploadedFile.getAbsolutePath());
                filePathsStrBuffer.append(',');                
            }

            out.write(filePathsStrBuffer.substring(0, filePathsStrBuffer.length() - 1));
            out.flush();
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
