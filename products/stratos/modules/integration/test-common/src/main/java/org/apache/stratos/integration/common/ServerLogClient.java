/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.common;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;

import java.rmi.RemoteException;

public class ServerLogClient {

    private static final Log log = LogFactory.getLog(ServerLogClient.class);
    private LogViewerClient logViewerClient;
    private int logCount;

    public ServerLogClient (String backEndUrl, String username, String password) throws
            AutomationUtilException {
        logCount = 0;
        createlogViewerClient(backEndUrl, username, password);
    }

    private void createlogViewerClient(String backEndUrl, String username, String password) throws AutomationUtilException {
        try {
            logViewerClient = new LogViewerClient(backEndUrl, username, password);
        } catch (AxisFault e) {
            String errorMsg = "Error in creating LogViewerClient";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

    public LogEvent[] getLogLines () throws AutomationUtilException {
        try {
            return logViewerClient.getAllRemoteSystemLogs();

        } catch (RemoteException e) {
            String errorMsg = "Error in creating getting remote system logs";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);

        } catch (LogViewerLogViewerException e) {
            String errorMsg = "Error in creating getting remote system logs";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

    public LogEvent[] getLogLines (String logType, String searchKey, String domain, String serverKey)
            throws AutomationUtilException {
        try {
            return logViewerClient.getRemoteLogs(logType, searchKey, domain, serverKey);
        } catch (RemoteException e) {
            String errorMsg = "Error in creating getting remote system logs";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);

        } catch (LogViewerLogViewerException e) {
            String errorMsg = "Error in creating getting remote system logs";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

//    public String[] getLogLines() throws AutomationUtilException {
//
//        LogEvent[] allLogs = null;
//        try {
//            allLogs = logViewerClient.getAllRemoteSystemLogs();
//
//       } catch (RemoteException e) {
//            String errorMsg = "Error in creating getting remote system logs";
//            log.error(errorMsg, e);
//            throw new AutomationUtilException(errorMsg, e);
//
//       } catch (LogViewerLogViewerException e) {
//            String errorMsg = "Error in creating getting remote system logs";
//            log.error(errorMsg, e);
//            throw new AutomationUtilException(errorMsg, e);
//        }
//
//        if (allLogs.length == 0) {
//            allLogs = new LogEvent[0];
//        }
//
//        if (logCount > allLogs.length) {
//            // cannot happen, return
//            return getLogsAsStrings(allLogs);
//        }
//
//        log.info("Total no. of log lines: " + Integer.toString(allLogs.length));
//        log.info("Previously returned count : " + Integer.toString(logCount));
//        log.info("Current count : " + Integer.toString(allLogs.length - logCount));
//
//        LogEvent[] selectedLogs = Arrays.copyOfRange(allLogs, logCount, allLogs.length);
//        logCount += (allLogs.length - logCount);
//
//        return getLogsAsStrings(selectedLogs);
//    }
//
//    private String[] getLogsAsStrings(LogEvent[] selectedLogs) {
//
//        String [] logs = new String[selectedLogs.length];
//        for (int i = 0 ; i < selectedLogs.length ; i++) {
//            logs [i] = selectedLogs [i].getMessage();
//        }
//        return logs;
//    }

}
