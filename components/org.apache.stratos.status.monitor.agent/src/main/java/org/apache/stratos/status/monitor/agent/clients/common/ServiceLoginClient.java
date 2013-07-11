/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */

package org.apache.stratos.status.monitor.agent.clients.common;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ServiceContext;
import org.apache.stratos.status.monitor.agent.constants.StatusMonitorAgentConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.apache.stratos.status.monitor.agent.internal.core.MySQLConnector;
import org.apache.stratos.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.apache.stratos.status.monitor.core.beans.AuthConfigBean;

import java.sql.SQLException;

/**
 * The client class that tries to log in into each of the services.
 */
public class ServiceLoginClient {
    private static final Log log = LogFactory.getLog(ServiceLoginClient.class);
    private static AuthConfigBean authConfigBean = StatusMonitorConfigurationBuilder.getAuthConfigBean();

    static {
        System.setProperty(StatusMonitorAgentConstants.TRUST_STORE, authConfigBean.getJksLocation());
        System.setProperty(StatusMonitorAgentConstants.TRUST_STORE_PASSWORD, "wso2carbon");
        System.setProperty(StatusMonitorAgentConstants.TRUST_STORE_TYPE, "JKS");
    }

    /**
     * Checks the log in
     * @param hostName; host name of the service
     * @param serviceID: int, service ID
     * @return boolean: true, if successfully logged in
     * @throws SQLException: if writing to the database failed.
     */
    public static boolean loginChecker(String hostName, int serviceID) throws SQLException {
        if(log.isDebugEnabled()) {
            log.debug("************** TRUST STORE : " +
                    System.getProperty(StatusMonitorAgentConstants.TRUST_STORE));
        }
        String userName = authConfigBean.getUserName();
        String password = authConfigBean.getPassword();
        boolean loginStatus = false;
        String authenticationServiceURL = "https://" + hostName +
                StatusMonitorAgentConstants.AUTHENTICATION_ADMIN_PATH;
        AuthenticationAdminStub authenticationAdminStub;
        try {
            authenticationAdminStub = new AuthenticationAdminStub(authenticationServiceURL);
            ServiceClient client = authenticationAdminStub._getServiceClient();
            Options options = client.getOptions();
            options.setManageSession(true);

            loginStatus = authenticationAdminStub.login(userName, password, hostName);
            ServiceContext serviceContext = authenticationAdminStub.
                    _getServiceClient().getLastOperationContext().getServiceContext();
            // String sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
            String msg = "Log in client successfully logged in to the service: " + hostName;
            if (log.isDebugEnabled()){
                log.debug(msg);
            }
            if (!loginStatus) {
                msg = "Login Checker failed for the service: " + hostName;
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, msg);
                log.warn(msg);
            }
        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Failed to login; Inserting the stats in the db for the log in checker";
            log.warn(msg, e);
        } catch (Exception e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, e.getMessage());
            String msg = "Exception in login; Inserting the stats in the db for the log in checker";
            log.warn(msg, e);
        }
        return loginStatus;
    }
}
