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
package org.wso2.carbon.ui.transports.fileupload;

import org.apache.commons.collections.bidimap.TreeBidiMap;
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
import java.util.Map;

public class ToolsAnyFileUploadExecutor extends AbstractFileUploadExecutor {

	@Override
	public boolean execute(HttpServletRequest request,
			HttpServletResponse response) throws CarbonException, IOException {
		PrintWriter out = response.getWriter();
        try {
        	Map fileResourceMap =
                (Map) configurationContext
                        .getProperty(ServerConstants.FILE_RESOURCE_MAP);
        	if (fileResourceMap == null) {
        		fileResourceMap = new TreeBidiMap();
        		configurationContext.setProperty(ServerConstants.FILE_RESOURCE_MAP,
                                             fileResourceMap);
        	}
            List<FileItemData> fileItems = getAllFileItems();
            //String filePaths = "";

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
                    dir.mkdirs();
                }
                File uploadedFile = new File(dir, fileItem.getFileItem().getFieldName());
                FileOutputStream fileOutStream = new FileOutputStream(uploadedFile);
                fileItem.getDataHandler().writeTo(fileOutStream);
                fileOutStream.flush();
                fileOutStream.close();
                response.setContentType("text/plain; charset=utf-8");
                //filePaths = filePaths + uploadedFile.getAbsolutePath() + ",";
                fileResourceMap.put(uuid, uploadedFile.getAbsolutePath());
                out.write(uuid);
            }
            //filePaths = filePaths.substring(0, filePaths.length() - 1);
            //out.write(filePaths);
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
