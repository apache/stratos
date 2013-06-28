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

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Business Rule Server
 */
public class BRSServerClient extends Thread {
    private static final Log log = LogFactory.getLog(BRSServerClient.class);
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

    private static OMElement createPayLoad() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://brs.carbon.wso2.org", "ns1");
        OMNamespace nameNs = fac.createOMNamespace("http://greeting.samples/xsd", "ns2");
        OMElement method = fac.createOMElement("greetMe", omNs);
        OMElement value = fac.createOMElement("User", omNs);
        OMElement NameValue = fac.createOMElement("name", nameNs);
        NameValue.addChild(fac.createOMText(NameValue, "QAuser"));
        value.addChild(NameValue);
        method.addChild(value);
        if (log.isDebugEnabled()) {
            log.debug("Method in createPayload(): " + method.toString());
        }
        return method;
    }

    private static void executeService() throws IOException, SQLException, ParseException {

        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.RULE);

        OMElement result;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();
        opts.setTo(new EndpointReference(StatusMonitorConstants.RULE_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES +
                authConfigBean.getTenant() + "/GreetingService"));
        opts.setAction("http://brs.carbon.wso2.org/greetMe");

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.RULE_HOST, serviceID)) {
            serviceclient.setOptions(opts);
            try {
                result = serviceclient.sendReceive(payload);
                if (log.isDebugEnabled()) {
                    log.debug("Result in BRSServerClient: " + result.toString());
                }

                if ((result.toString().indexOf("QAuser")) > 0) {
                    MySQLConnector.insertStats(serviceID, true);
                    MySQLConnector.insertState(serviceID, true, "");
                } else {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, "Service Invocation failed");
                }

            } catch (AxisFault e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "Fault when executing service: BRSServerClient: ";
                log.warn(msg, e);
            }
            catch (NullPointerException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "NPE when executing service: BRSServerClient: ";
                log.warn(msg, e);
            }
        }

    }

}
