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

import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMFactory;
import org.apache.axiom.om.OMNamespace;
import org.apache.axis2.AxisFault;
import org.apache.axis2.addressing.EndpointReference;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.status.monitor.agent.clients.common.ServiceLoginClient;
import org.wso2.carbon.status.monitor.agent.constants.StatusMonitorAgentConstants;
import org.wso2.carbon.status.monitor.agent.internal.core.MySQLConnector;
import org.wso2.carbon.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.wso2.carbon.status.monitor.core.beans.AuthConfigBean;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Application Server service
 */
public class ApplicationServerClient extends Thread {
    private static int serviceID;
    private static final Log log = LogFactory.getLog(ApplicationServerClient.class);
    private static final AuthConfigBean authConfigBean =
            StatusMonitorConfigurationBuilder.getAuthConfigBean();

    private static OMElement createPayLoad() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace(StatusMonitorConstants.CARBON_OM_NAMESPACE, "ns1");
        OMElement method = fac.createOMElement("echoString", omNs);
        OMElement value = fac.createOMElement("s", omNs);
        value.addChild(fac.createOMText(value, "Hello World"));
        method.addChild(value);
        return method;
    }

    public void run() {
        while (true) {
            try {
                executeService();
//                LoadBalanceAgentClient.getLoadAverageFromInstances(StatusMonitorConstants.APPSERVER_HOST, serviceID);

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

    /**
     * Executing the service
     * @throws SQLException, exception in writing to the database
     * @throws ParseException, parse exception
     * @throws IOException, in initializing the service client
     */
    private static void executeService() throws SQLException, ParseException, IOException {
        serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.APPSERVER);

        OMElement result;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();

        opts.setTo(new EndpointReference(StatusMonitorConstants.APPSERVER_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES + authConfigBean.getTenant() +
                "/Axis2Service"));
        opts.setAction(StatusMonitorConstants.CARBON_OM_NAMESPACE + "echoString");
        opts.setProperty(org.apache.axis2.transport.http.HTTPConstants.CHUNKED, Boolean.FALSE);


        //check whether login success
        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.APPSERVER_HOST, serviceID)) {
            if (!webappTest()) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, "Webapp Invocation failed");
                log.warn("WebApp invocation failed");

            } else {
                serviceclient.setOptions(opts);
                try {
                    result = serviceclient.sendReceive(payload);
                    if (log.isDebugEnabled()) {
                        log.debug(result);
                    }
                    if ((result.toString().indexOf("Hello World")) > 0) {
                        if (pingPlatformTenant()) {
//                            if (new JmeterTestClient().getAarUploadResults()) {
//                                if (log.isDebugEnabled()) {
//                                    log.debug("Jmeter aar upload Fail");
//                                }
//                                MySQLConnector.insertStats(serviceID, false);
//                                MySQLConnector.insertState(serviceID, false, "UI: AAR upload test failed");
//                            } else {
                                MySQLConnector.insertStats(serviceID, true);
                                MySQLConnector.insertState(serviceID, true, "");
//                            }
                        }
                    } else {
                        MySQLConnector.insertStats(serviceID, false);
                        MySQLConnector.insertState(serviceID, false, " Service Invocation failed");
                        log.warn("Service Invocation Failed");
                    }

                } catch (AxisFault e) {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, e.getMessage());
                    String msg = "Axis Fault in invoking the Appserver Client";
                    log.warn(msg, e);
                }
                catch (NullPointerException e) {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, e.getMessage());
                    String msg = "Null Pointer Exception in invoking the Appserver client";
                    log.warn(msg, e);
                }
            }
        }
    }

    /**
     * Pings the platform tenant
     * @return true, if the ping was successful
     * @throws IOException, in creating the service client
     * @throws SQLException, in writing to the db.
     * @throws ParseException, parse exception
     */
    private static Boolean pingPlatformTenant() throws IOException,
            SQLException, ParseException {
        Boolean pingPlatformTenantStatus = false;
        OMElement result;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();

        opts.setTo(new EndpointReference(StatusMonitorConstants.APPSERVER_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES +
                StatusMonitorConfigurationBuilder.getSampleTenantConfigBean().getTenant() +
                "/Axis2Service/"));
        opts.setAction("http://service.carbon.wso2.org/echoString");
        opts.setProperty(org.apache.axis2.transport.http.HTTPConstants.CHUNKED, Boolean.FALSE);

        serviceclient.setOptions(opts);
        try {
            result = serviceclient.sendReceive(payload);

            if ((result.toString().indexOf("Hello World")) > 0) {
                if (log.isDebugEnabled()) {
                    log.debug("Appserver test client - service invocation test passed");
                }
                pingPlatformTenantStatus = true;
            } else {
                String msg = "Ping to platform sample tenant domain failed";
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, msg);
                log.warn(msg);
            }

        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            String msg = "Ping to platform sample tenant domain failed: ";
            MySQLConnector.insertState(serviceID, false, msg + e.getMessage());
            log.warn(msg, e);
        }
        catch (NullPointerException e) {
            MySQLConnector.insertStats(serviceID, false);
            String msg = "Ping to platform sample tenant domain failed: ";
            MySQLConnector.insertState(serviceID, false, msg + e.getMessage());
            log.warn(msg, e);
        }
        return pingPlatformTenantStatus;
    }

    /**
     * Connects to the given web application.
     * @return true, if successful
     */
    private static boolean webappTest() {
        URL webAppURL;
        BufferedReader in;
        boolean webappStatus = false;

        try {
            webAppURL = new URL(StatusMonitorConstants.APPSERVER_HTTP + "/t/" +
                    authConfigBean.getTenant() +
                    "/webapps/SimpleServlet/simple-servlet");
            URLConnection yc;
            yc = webAppURL.openConnection();

            in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                if (log.isDebugEnabled()) {
                    log.debug(inputLine);
                }
                if (inputLine.indexOf("Hello, World") > 1) {
                    if (log.isDebugEnabled()) {
                        log.debug("True : " + inputLine.indexOf("Hello, World"));
                    }
                    webappStatus = true;
                }
            }
            in.close();
        } catch (IOException e) {
            log.error(e);
        }
        return webappStatus;
    }
}
