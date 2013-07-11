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
package org.apache.stratos.status.monitor.agent.clients.service;

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
import org.apache.stratos.status.monitor.agent.clients.common.ServiceLoginClient;
import org.apache.stratos.status.monitor.agent.constants.StatusMonitorAgentConstants;
import org.apache.stratos.status.monitor.agent.internal.core.MySQLConnector;
import org.apache.stratos.status.monitor.core.StatusMonitorConfigurationBuilder;
import org.apache.stratos.status.monitor.core.beans.AuthConfigBean;
import org.apache.stratos.status.monitor.core.constants.StatusMonitorConstants;
import org.apache.stratos.status.monitor.core.jdbc.MySQLConnectionInitializer;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Business Process Server
 */
public class BPSServerClient extends Thread{
    private static final Log log = LogFactory.getLog(BPSServerClient.class);
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
        OMNamespace omNs = fac.createOMNamespace("http://ode/bpel/unit-test.wsdl", "ns1");
        OMElement method = fac.createOMElement("hello", omNs);
        OMElement value = fac.createOMElement("TestPart", null);
        value.addChild(fac.createOMText(value, "Hello"));
        method.addChild(value);
        return method;
    }

    private static void executeService() throws IOException, SQLException, ParseException {

        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.PROCESS);

        OMElement result;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();
        opts.setTo(new EndpointReference(StatusMonitorConstants.PROCESS_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES +
                authConfigBean.getTenant() + "/HelloService"));
        opts.setAction("http://ode/bpel/unit-test.wsdl/hello");

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.PROCESS_HOST, serviceID)) {
            serviceclient.setOptions(opts);
            try {
                result = serviceclient.sendReceive(payload);
                if(log.isDebugEnabled()){
                    log.debug("Result of BPS: " + result);
                }

                if ((result.toString().indexOf("Hello World")) > 0) {
                    MySQLConnector.insertStats(serviceID, true);
                    MySQLConnector.insertState(serviceID, true, "");
                } else {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, "Service Invocation failed");
                }

            } catch (AxisFault e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            }
            catch (NullPointerException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                log.warn(e);
            }
        }
    }
}
