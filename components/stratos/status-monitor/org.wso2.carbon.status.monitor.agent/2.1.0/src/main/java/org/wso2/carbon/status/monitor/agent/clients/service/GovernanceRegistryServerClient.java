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
import org.wso2.carbon.registry.app.RemoteRegistry;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.status.monitor.agent.clients.common.ServiceLoginClient;
import org.wso2.carbon.status.monitor.agent.constants.StatusMonitorAgentConstants;
import org.wso2.carbon.status.monitor.agent.internal.core.MySQLConnector;
import org.wso2.carbon.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.wso2.carbon.status.monitor.core.beans.AuthConfigBean;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Governance Registry
 */
public class GovernanceRegistryServerClient extends Thread{
    private static final Log log = LogFactory.getLog(GovernanceRegistryServerClient.class);
    private static final AuthConfigBean authConfigBean =
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
            } catch (ParseException e) {
                log.error(e);
            }
        }
    }

    static RemoteRegistry registry = null;

    private static void executeService() throws IOException, SQLException, ParseException {
        boolean getValue = false;
        boolean putValue = false;
        boolean deleteValue = false;
        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.GOVERNANCE);

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.GOVERNANCE_HOST, serviceID)) {
            try {

                registry = new RemoteRegistry(new URL(StatusMonitorConstants.GOVERNANCE_HTTP +
                        "/t/" + authConfigBean.getTenant() + "/registry"),
                        authConfigBean.getUserName(), authConfigBean.getPassword());
            } catch (RegistryException e) {
                log.error(e);
            } catch (MalformedURLException e) {
                log.error(e);
            } catch (Exception e) {
                log.error(e);
            }

            /*put resource */
            Resource r1;

            try {
                r1 = registry.newResource();
                r1.setContent("test content".getBytes());
                r1.setMediaType("text/plain");
                String pathValue = registry.put(
                        StatusMonitorAgentConstants.GREG_SAMPLE_TEST_RESOURCE_PATH, r1);

                if (pathValue.equalsIgnoreCase(
                        StatusMonitorAgentConstants.GREG_SAMPLE_TEST_RESOURCE_PATH)) {
                    putValue = true;
                }
            } catch (RegistryException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            } catch (Exception e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            }

            /*get resource */
            try {
                if (putValue) {
                    Resource r2 = registry.get(
                            StatusMonitorAgentConstants.GREG_SAMPLE_TEST_RESOURCE_PATH);
                    if (log.isDebugEnabled()) {
                        log.debug("Media Type: " + r2.getMediaType());
                    }

                    if (r2.getMediaType().equalsIgnoreCase("text/plain")) {
                        getValue = true;
                    }
                }
            } catch (RegistryException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            } catch (Exception e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            }

            /*Delete resource */
            try {
                if (getValue) {
                    registry.delete(StatusMonitorAgentConstants.GREG_SAMPLE_TEST_RESOURCE_PATH);

                    if (!registry.resourceExists(
                            StatusMonitorAgentConstants.GREG_SAMPLE_TEST_RESOURCE_PATH)) {
                        deleteValue = true;
                    }
                }

            } catch (RegistryException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            } catch (Exception e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            }

            //write to mysql db
            try {
                if (getValue & putValue & deleteValue) {
                    if (log.isDebugEnabled()) {
                        log.debug("Governance Registry Status Monitor agent: Writing to the database");
                    }
                    MySQLConnector.insertStats(serviceID, true);
                    MySQLConnector.insertState(serviceID, true, "");
                }
            } catch (SQLException e) {
                String msg = "Error in writing to the database for Governance Registry - status monitor agent";
                log.error(msg, e);
            }
        }
    }
}



