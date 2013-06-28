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
 * Status Monitor Agent client class for Gadget Server
 */
public class GadgetServerClient extends Thread{

    private static final Log log = LogFactory.getLog(GadgetServerClient.class);
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
        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.GADGET);

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.GADGETS_HOST, serviceID)) {
            try {
                registry = new RemoteRegistry(new URL(StatusMonitorConstants.GADGETS_HTTP +
                        "/t/" + authConfigBean.getTenant() + "/registry"),
                        authConfigBean.getUserName(), authConfigBean.getPassword());
            } catch (RegistryException e) {
                log.error(e);
            } catch (MalformedURLException e) {
                log.error(e);
            }

            /*get resource */
            try {
                Resource r2 = registry.get(StatusMonitorAgentConstants.GS_SAMPLE_TEST_RESOURCE_PATH);
                if(log.isDebugEnabled()) {
                    log.debug("MediaType in the executeService() of GadgetServerClient in Status" +
                            " Monitor Agent: " + r2.getMediaType());
                }

                if (r2.getMediaType().equalsIgnoreCase("application/vnd.wso2-gadget+xml")) {
                    MySQLConnector.insertStats(serviceID, true);
                    MySQLConnector.insertState(serviceID, true, "");
                }
            } catch (RegistryException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "Exception in executing the service for GadgetServerClient - Status Monitor Agent";
                log.error(msg, e);
            }
        }
    }
}
