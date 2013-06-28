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
import org.apache.axiom.om.util.AXIOMUtil;
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
import org.wso2.carbon.status.monitor.core.beans.SampleTenantConfigBean;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Data
 */
public class DataServerClient extends Thread{
    private static final Log log = LogFactory.getLog(DataServerClient.class);
    private static final AuthConfigBean authConfigBean =
            StatusMonitorConfigurationBuilder.getAuthConfigBean();
    private static final SampleTenantConfigBean sampleTenantConfigBean =
            StatusMonitorConfigurationBuilder.getSampleTenantConfigBean();

    private static OMElement createPayLoad() {
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://ws.wso2.org/dataservice", "ns1");
        return fac.createOMElement("getCustomers", omNs);
    }

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

    private static void executeService() throws IOException, SQLException, ParseException {
        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.DATA);

        OMElement result;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();
        opts.setTo(new EndpointReference(StatusMonitorConstants.DATA_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES +
                 authConfigBean.getTenant() + "/GSpreadSample"));
        opts.setAction("http://ws.wso2.org/dataservice/getCustomers");

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.DATA_HOST, serviceID)) {
            serviceclient.setOptions(opts);
            try {
                result = serviceclient.sendReceive(payload);
                if (log.isDebugEnabled()) {
                    log.debug(result);
                }

                if ((result.toString().indexOf("Signal Gift Stores")) > 0) {
                    executeShoppingCartDSPlatformSample();

                } else {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, "Service Invocation failed");
                }

            } catch (AxisFault e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "Error in executing service for DSS Server client";
                log.warn(msg, e);
            }
            catch (NullPointerException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "NPE in executing the service for DSS client";
                log.warn(msg, e);
            }
        }
    }

    private static boolean executeShoppingCartDSPlatformSample() throws IOException, SQLException, ParseException {
        Boolean sampleStatus = false;
        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.DATA);
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body/>\n" +
                "</soapenv:Envelope>";
        String action = "getAllCategories";

        try {
            OMElement result;
            result = sendRequest(payload, action, new EndpointReference(
                    StatusMonitorConstants.DATA_HTTP + StatusMonitorAgentConstants.TENANT_SERVICES +
                            sampleTenantConfigBean.getTenant() + "/ShoppingCartDS"));

            if ((result.toString().indexOf("Compact Lens-Shutter Cameras")) > 0) {
                sampleStatus = true;
                MySQLConnector.insertStats(serviceID, true);
                MySQLConnector.insertState(serviceID, true, "");
            } else {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, "Platform sample ShoppingCartDS invocation failed");
            }
        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform sample ShoppingCartDS: " + e.getMessage());
            String msg = "Fault in executing the Shopping cart sample";
            log.warn(msg, e);
        }
        catch (NullPointerException e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform sample ShoppingCartDS: " + e.getMessage());
            String msg = "NPE in executing the shopping cart sample";
            log.warn(msg, e);
        } catch (XMLStreamException e) {
            String msg = "XMLStreamException in executing the shopping cart sample";
            log.warn(msg, e);
        }
        return sampleStatus;
    }

    private static OMElement sendRequest(String payloadStr, String action, EndpointReference targetEPR)
            throws XMLStreamException, AxisFault {
        OMElement payload = AXIOMUtil.stringToOM(payloadStr);
        Options options = new Options();
        options.setTo(targetEPR);
        options.setAction("urn:" + action); //since soapAction = ""

        //Blocking invocation
        ServiceClient sender = new ServiceClient();
        sender.setOptions(options);
        if (log.isDebugEnabled()){
            log.debug("Request: " + payload.toString());
        }
        OMElement result = sender.sendReceive(payload);
        if (log.isDebugEnabled()){
            log.debug("Response: "+ payload.toString());
        }
        return result;
    }
}
