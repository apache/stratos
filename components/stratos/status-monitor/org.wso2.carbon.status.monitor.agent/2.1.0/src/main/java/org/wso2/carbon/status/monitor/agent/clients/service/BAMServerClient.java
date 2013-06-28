/*
* Copyright (c) 2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.status.monitor.agent.clients.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.status.monitor.agent.clients.common.ServiceLoginClient;
import org.wso2.carbon.status.monitor.agent.internal.core.MySQLConnector;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Business Activity Monitor
 */
public class BAMServerClient extends Thread {

    public static int serviceID;
    private static final Log log = LogFactory.getLog(BAMServerClient.class);

    public void run() {
        while (true) {
            try {
                executeService();

                // return from while loop if the thread is interrupted
                if (isInterrupted()) {
                    break;
                }
                // let the thread sleep for 15 mins
                try {
                    sleep(StatusMonitorConstants.SLEEP_TIME);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } catch (IOException e) {
                log.error(e);
            } catch (SQLException e) {
                log.error(e);
            } catch (ParseException e) {
                log.error(e);
            }
        }
    }

    private static void executeService() throws SQLException, IOException, ParseException {
        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.MONITOR);

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.MONITOR_HOST, serviceID)) {
            MySQLConnector.insertStats(serviceID, true);
            MySQLConnector.insertState(serviceID, true, "");
        }
    }
}
