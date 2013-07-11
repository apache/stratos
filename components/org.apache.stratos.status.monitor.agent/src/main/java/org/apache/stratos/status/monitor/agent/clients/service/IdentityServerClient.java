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

package org.apache.stratos.status.monitor.agent.clients.service;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ServiceContext;
import org.apache.stratos.status.monitor.agent.constants.StatusMonitorAgentConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.apache.stratos.status.monitor.agent.internal.core.MySQLConnector;
import org.apache.stratos.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.apache.stratos.status.monitor.core.beans.AuthConfigBean;
import org.apache.stratos.status.monitor.core.constants.StatusMonitorConstants;
import org.apache.stratos.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Status Monitor Agent client class for Identity Server
 */
public class IdentityServerClient extends Thread{
    private static final Log log = LogFactory.getLog(IdentityServerClient.class);
    private static AuthConfigBean authConfigBean =
            StatusMonitorConfigurationBuilder.getAuthConfigBean();

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
            }
        }
    }

    private static void executeService() throws SQLException, IOException {

        System.setProperty(StatusMonitorAgentConstants.TRUST_STORE, authConfigBean.getJksLocation());
        System.setProperty(StatusMonitorAgentConstants.TRUST_STORE_PASSWORD, "wso2carbon");
        System.setProperty(StatusMonitorAgentConstants.TRUST_STORE_TYPE, "JKS");

        File newFile = new File(authConfigBean.getJksLocation());
        if(log.isDebugEnabled()){
            log.debug("Canonical Path: " + newFile.getCanonicalPath());
        }

        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.IDENTITY);
        String authenticationServiceURL = StatusMonitorConstants.IDENTITY_HTTPS +
                StatusMonitorAgentConstants.AUTHENTICATION_ADMIN_PATH;
        AuthenticationAdminStub authenticationAdminStub;
        try {
            authenticationAdminStub = new AuthenticationAdminStub(authenticationServiceURL);
            ServiceClient client = authenticationAdminStub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);

            Boolean status;
            status = authenticationAdminStub.login(authConfigBean.getUserName(),
                    authConfigBean.getPassword(), StatusMonitorConstants.IDENTITY_HOST);
            ServiceContext serviceContext = authenticationAdminStub.
                    _getServiceClient().getLastOperationContext().getServiceContext();
            // String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);

            if (status) {
                MySQLConnector.insertStats(serviceID, true);
                MySQLConnector.insertState(serviceID, true, "");
            }
        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Fault in executing the service for IS client - Status Monitor Agent";
            log.warn(msg, e);

        } catch (Exception e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Exception in executing the service for IS client - Status Monitor Agent";
            log.warn(msg, e);
        }
    }
}
