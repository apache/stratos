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

package org.wso2.carbon.ui.util;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.wso2.carbon.CarbonException;
import org.wso2.carbon.core.util.MIMEType2FileExtensionMap;
import org.wso2.carbon.core.commons.stub.filedownload.FileDownloadServiceStub;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.utils.CarbonUtils;


import javax.activation.DataHandler;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Map;

/**
 *
 */
public class FileDownloadUtil {
    private static Log log = LogFactory.getLog(FileDownloadUtil.class);

    private MIMEType2FileExtensionMap mimeMap;

    public FileDownloadUtil(BundleContext context) {
        mimeMap = new MIMEType2FileExtensionMap();
        mimeMap.init(context);
    }

    public synchronized boolean acquireResource(ConfigurationContextService configCtxService,
                                                HttpServletRequest request,
                                                HttpServletResponse response)
            throws CarbonException {

        OutputStream out;
        try {
            out = response.getOutputStream();
        } catch (IOException e) {
            String msg = "Unable to retrieve file ";
            log.error(msg, e);
            throw new CarbonException(msg, e);
        }
        String fileID = request.getParameter("id");
        String fileName = getFileName(configCtxService, request, fileID);

        if (fileName == null) {
            String serverURL = CarbonUIUtil.getServerURL(request.getSession().
                    getServletContext(), request.getSession());

            String serviceEPR = serverURL + "FileDownloadService";
            try {

                FileDownloadServiceStub stub;
                if(CarbonUtils.isRunningOnLocalTransportMode()) {
                    stub = new FileDownloadServiceStub(configCtxService.getServerConfigContext(), serviceEPR);
                } else {
                    stub = new FileDownloadServiceStub(configCtxService.getClientConfigContext(), serviceEPR);
                }
                DataHandler dataHandler = stub.downloadFile(fileID);
                if (dataHandler != null) {
                    response.setHeader("Content-Disposition", "filename=" + fileID);
                    response.setContentType(dataHandler.getContentType());
                    InputStream in = dataHandler.getDataSource().getInputStream();
                    int nextChar;
                    while ((nextChar = in.read()) != -1) {
                        out.write((char) nextChar);
                    }
                    out.flush();
                    out.close();
                    in.close();
                    return true;
                }
                out.write("The requested file was not found on the server".getBytes());
                out.flush();
                out.close();
            } catch (IOException e) {
                String msg = "Unable to write output to HttpServletResponse OutputStream ";
                log.error(msg, e);
                throw new CarbonException(msg, e);
            }
            return false;
        }


        try {
            File file = new File(fileName);
            FileInputStream in = new FileInputStream(file);
            byte[] b = new byte[(int) file.length()];
            response.setContentType(mimeMap.getMIMEType(file));
            response.setContentLength((int) file.length());
            response.setHeader("Content-Disposition", "filename=" + file.getName());
            int lengthRead = in.read(b);
            if (lengthRead != -1) {
                out.write(b);
            }
            out.flush();
            out.close();
            in.close();
            return true;
        } catch (IOException e) {
            String msg = "Unable to retrieve file ";
            log.error(msg, e);
            throw new CarbonException(msg, e);
        }
    }

    private String getFileName(ConfigurationContextService configCtxService,
                               HttpServletRequest request,
                               String fileID) {
        //Trying to get the fileName from the client-configuration context
        Map fileResourcesMap =
                (Map) configCtxService.getClientConfigContext().
                        getProperty(ServerConstants.FILE_RESOURCE_MAP);
        String fileName = (String) fileResourcesMap.get(fileID);

        if (fileName == null) {
            String requestURI = request.getRequestURI();
            ConfigurationContext configContext = configCtxService.getServerConfigContext();
            fileResourcesMap = (Map) configContext.getProperty(ServerConstants.FILE_RESOURCE_MAP);
            fileName = (String) fileResourcesMap.get(fileID);
        }
        return fileName;
    }
}
