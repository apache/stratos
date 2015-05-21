/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.logging.view.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.LogViewerStub;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogEvent;
import org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogInfo;

import javax.activation.DataHandler;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.rmi.RemoteException;

public class LogViewerClient {
    private static final Log log = LogFactory.getLog(LogViewerClient.class);
    public LogViewerStub stub;

    public LogViewerClient(String cookie, String backendServerURL, ConfigurationContext configCtx)
            throws AxisFault {
        String serviceURL = backendServerURL + "LogViewer";

        stub = new LogViewerStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        option.setProperty(Constants.Configuration.ENABLE_MTOM, Constants.VALUE_TRUE);
    }


    public void cleaLogs() throws Exception {
        stub.clearLogs();
    }

    public void downloadArchivedLogFiles(String logFile, HttpServletResponse response, String domain, String serverKey)
            throws Exception {
        try {
            logFile = logFile.replace(".gz", "");
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("application/txt");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + logFile.replaceAll("\\s", "_"));
            DataHandler data = stub.downloadArchivedLogFiles(logFile, domain, serverKey);
            InputStream fileToDownload = data.getInputStream();
            int c;
            while ((c = fileToDownload.read()) != -1) {
                outputStream.write(c);
            }
            outputStream.flush();
            outputStream.flush();
        } catch (Exception e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public int getLineNumbers(String logFile)
            throws Exception {
        try {
            return stub.getLineNumbers(logFile);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public PaginatedLogInfo getPaginatedLogInfo(int pageNumber, String tenantDomain,
                                                String serviceName) throws Exception {
        try {
            return stub.getPaginatedLogInfo(pageNumber, tenantDomain, serviceName);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public LogEvent[] getLogs(String type, String keyword, String domain, String serverkey) throws Exception {
        if (type == null || type.equals("")) {
            type = "ALL";
        }
        try {
            return stub.getLogs(type, keyword, domain, serverkey);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public LogEvent[] getApplicationLogs(String type, String keyword, String applicationName, String domain, String serverKey)
            throws Exception {
        if (type == null || type.equals("")) {
            type = "ALL";
        }
        if (applicationName == null || applicationName.equals("")) {
            applicationName = "FIRST";
        }
        try {
            return stub.getApplicationLogs(type, keyword, applicationName, domain, serverKey);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public String[] getLogLinesFromFile(String logFile, int maxLogs, int start, int end) throws Exception {
        try {
            return stub.getLogLinesFromFile(logFile, maxLogs, start, end);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    /*public String[] getSubscribedCartridgeList() throws Exception {
        try {
            return appMgtStub.getSubscribedCartridgeAliases();
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }*/

    public String[] getApplicationNames(String domain, String serverKey) throws LogViewerLogViewerException, RemoteException {
        try {
            return stub.getApplicationNames(domain, serverKey);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public int getNoOfLogEvents() throws Exception {
        try {
            return 20;// stub.getNoOfLogEvents();
        } catch (Exception e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public String[] getServiceNames() throws RemoteException, LogViewerLogViewerException {
        try {
            return stub.getServiceNames();
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public boolean isFileAppenderConfiguredForST() throws RemoteException {
        try {
            return stub.isFileAppenderConfiguredForST();
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public PaginatedLogEvent getPaginatedLogEvents(int pageNumber, String type, String keyword, String domain, String serverKey)
            throws Exception {
        try {
            return stub.getPaginatedLogEvents(pageNumber, type, keyword, domain, serverKey);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public PaginatedLogEvent getPaginatedApplicationLogEvents(int pageNumber, String type,
                                                              String keyword, String appName, String domain, String serverKey) throws Exception {
        try {
            return stub.getPaginatedApplicationLogEvents(pageNumber, type, keyword, appName, domain, serverKey);
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public PaginatedLogInfo getLocalLogFiles(int pageNo, String domain, String serverKey) throws Exception {

        try {
            return stub.getLocalLogFiles(pageNo, domain, serverKey);
        } catch (Exception e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }

    }

    public boolean isLogEventReciverConfigured() throws RemoteException {
        try {
            return stub.isLogEventReciverConfigured();
        } catch (RemoteException e) {
            String msg = "Error occurred while getting logger data. Backend service may be unavailable";
            log.error(msg, e);
            throw e;
        }
    }

    public String getImageName(String type) {
        if (type.equals("INFO")) {
            return "images/information.gif";
        } else if (type.equals("ERROR")) {
            return "images/error.png";
        } else if (type.equals("WARN")) {
            return "images/warn.png";
        } else if (type.equals("DEBUG")) {
            return "images/debug.png";
        } else if (type.equals("TRACE")) {
            return "images/trace.png";
        } else if (type.equals("FATAL")) {
            return "images/fatal.png";
        }
        return "";
    }

    public String[] getLogLevels() {
        return new String[]{"ALL", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"};
    }

    public boolean isManager() throws RemoteException {
        return stub.isManager();
    }

    public boolean isValidTenant(String domain) throws RemoteException {
        return stub.isValidTenant(domain);

    }

}

