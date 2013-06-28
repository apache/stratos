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
 * Status Monitor Agent client class for ESB
 */
public class ESBServerClient extends Thread{

    private static final Log log = LogFactory.getLog(ESBServerClient.class);
    private static final AuthConfigBean authConfigBean =
            StatusMonitorConfigurationBuilder.getAuthConfigBean();
    private static final SampleTenantConfigBean sampleTenantConfigBean =
            StatusMonitorConfigurationBuilder.getSampleTenantConfigBean();

    private static int serviceID;

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
        OMNamespace omNs = fac.createOMNamespace("http://service.carbon.wso2.org", "ns1");
        OMElement method = fac.createOMElement("echoString", omNs);
        OMElement value = fac.createOMElement("s", omNs);
        value.addChild(fac.createOMText(value, "Hello World"));

        method.addChild(value);
        return method;
    }

    private static void executeService() throws IOException, SQLException, ParseException {

        OMElement result = null;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();
        opts.setTo(new EndpointReference(StatusMonitorConstants.ESB_HTTP + ":" +
                StatusMonitorConstants.ESB_NHTTP_PORT +
                StatusMonitorAgentConstants.TENANT_SERVICES +
                authConfigBean.getTenant() + "/DemoProxy"));
        opts.setAction("http://service.carbon.wso2.org/echoString");
        serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.ESB);

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.ESB_HOST, serviceID)) {
            serviceclient.setOptions(opts);
            try {
                result = serviceclient.sendReceive(payload);

                if ((result.toString().indexOf("Hello World")) > 0) {
                    executeProductPlatformSample();
                } else {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, "Service Invocation failed");
                }

            } catch (AxisFault e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "Error in executing service";
                log.error(msg, e);
            }
            catch (NullPointerException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "NPE in executing the service";
                log.error(msg, e);
            }
        }
    }


    private static boolean executeProductPlatformSample() throws IOException, SQLException, ParseException {
        Boolean sampleStatus = false;
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body/>\n" +
                "</soapenv:Envelope>";
        String action = "getAllCategories";

        try {
            OMElement result = null;
            result = sendRequest(payload, action, new EndpointReference(
                    StatusMonitorConstants.ESB_HTTP + ":" + StatusMonitorConstants.ESB_NHTTP_PORT +
                            StatusMonitorAgentConstants.TENANT_SERVICES +
                            sampleTenantConfigBean.getTenant() +
                            "/ProductService"));

            if ((result.toString().indexOf("Compact Lens-Shutter Cameras")) > 0) {
                executeAdminServicePlatformSample();
                sampleStatus = true;
            } else {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, "Platform sample ProductService invocation failed");
            }

        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform sample: " + e.getMessage());
            String msg = "Error in executing the product platform sample";
            log.error(msg, e);
        }
        catch (NullPointerException e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform sample: " + e.getMessage());
            String msg = "NPE in executing the product platform sample";
            log.error(msg, e);
        } catch (XMLStreamException e) {
            String msg = "XMLStream exception in executing the product platform sample";
            log.error(msg, e);
        }
        return sampleStatus;
    }

    private static boolean executeAdminServicePlatformSample() throws IOException, SQLException, ParseException {
        Boolean sampleStatus = false;
        int serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.ESB);
        String payload = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body/>\n" +
                "</soapenv:Envelope>";
        String action = "getAllCategories";

        try {
            OMElement result;
            result = sendRequest(payload, action, new EndpointReference(
                    StatusMonitorConstants.ESB_HTTP + ":" + StatusMonitorConstants.ESB_NHTTP_PORT +
                            StatusMonitorAgentConstants.TENANT_SERVICES +
                            sampleTenantConfigBean.getTenant() +
                            "/AdminService"));

            if ((result.toString().indexOf("Compact Lens-Shutter Cameras")) > 0) {
                sampleStatus = true;
                MySQLConnector.insertStats(serviceID, true);
                MySQLConnector.insertState(serviceID, true, "");
            } else {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, "Platform sample AdminService invocation failed");
            }

        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState( serviceID, false, "Platform sample AdminService: " + e.getMessage());
            String msg = "Executing Admin Service Platform Sample failed";
            log.error(msg, e);
        }
        catch (NullPointerException e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform sample AdminService: " + e.getMessage());
            String msg = "NPE in executing the admin service platform sample";
            log.error(msg, e);
        } catch (XMLStreamException e) {
            String msg = "XMLStreamException in executing the admin service platform sample";
            log.error(msg, e);
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
        if (log.isDebugEnabled()) {
            log.debug("Request: "+ payload.toString());
        }
        OMElement result = sender.sendReceive(payload);
        if (log.isDebugEnabled()) {
            log.debug("Response:" + payload.toString());
        }

        return result;
    }
}
