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
import org.wso2.carbon.status.monitor.core.beans.SampleTenantConfigBean;
import org.wso2.carbon.status.monitor.core.constants.StatusMonitorConstants;
import org.wso2.carbon.status.monitor.core.jdbc.MySQLConnectionInitializer;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;

/**
 * Status Monitor Agent client class for Mashup Server
 */
public class MashupServerClient extends Thread{

    private static final Log log = LogFactory.getLog(MashupServerClient.class);
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
        OMNamespace omNs = fac.createOMNamespace("http://services.mashup.wso2.org/schemaTest1", "ns1");
        OMElement method = fac.createOMElement("echoJSString", omNs);
        OMElement value = fac.createOMElement("param", null);
        value.addChild(fac.createOMText(value, "Hello World"));
        method.addChild(value);
        return method;
    }

    private static void executeService() throws IOException, SQLException, ParseException {

        serviceID = MySQLConnectionInitializer.getServiceID(StatusMonitorConstants.MASHUP);

        OMElement result;
        OMElement payload = createPayLoad();
        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();

        opts.setTo(new EndpointReference(StatusMonitorConstants.MASHUP_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES + authConfigBean.getTenant() +
                "/test123/schemaTest1/ "));
        opts.setAction("http://services.mashup.wso2.org/schemaTest1");

        if (ServiceLoginClient.loginChecker(StatusMonitorConstants.MASHUP_HOST, serviceID)) {
            serviceclient.setOptions(opts);
            try {
                result = serviceclient.sendReceive(payload);

                if ((result.toString().indexOf("Hello World")) > 0) {
                    if (executeRelatedProductsService()) {
                        MySQLConnector.insertStats(serviceID, true);
                        MySQLConnector.insertState(serviceID, true, "");
                    }
                } else {
                    MySQLConnector.insertStats(serviceID, false);
                    MySQLConnector.insertState(serviceID, false, "Service Invocation failed");
                }

            } catch (AxisFault e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "Error in executing the service - Status Monitor Agent for MashupServerClient";
                log.warn(msg, e);
            }
            catch (NullPointerException e) {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, e.getMessage());
                String msg = "NPE in executing the service - Status Monitor Agent for MashupServerClient";
                log.warn(msg, e);
            } catch (XMLStreamException e) {
                String msg = "XMLStreamException in execting the service - Status Monitor Agent for MashupServerClient";
                log.warn(msg, e);
            }
        }
    }

    private static Boolean executeRelatedProductsService() throws IOException, SQLException, ParseException, XMLStreamException {

        Boolean relatedProductsServiceStatus = false;

        OMElement result;
        OMFactory fac = OMAbstractFactory.getOMFactory();
        OMNamespace omNs = fac.createOMNamespace("http://services.mashup.wso2.org/RelatedProducts?xsd", "rel");
        OMElement payload = fac.createOMElement("getRelatedProducts", omNs);
        OMElement value1 = fac.createOMElement("query", null);
        OMElement value2 = fac.createOMElement("count", null);
        OMElement value3 = fac.createOMElement("format", null);
        value1.addChild(fac.createOMText(value1, "mac"));
        value2.addChild(fac.createOMText(value2, "2"));
        value3.addChild(fac.createOMText(value3, "xml"));

        payload.addChild(value1);
        payload.addChild(value2);
        payload.addChild(value3);

        ServiceClient serviceclient = new ServiceClient();
        Options opts = new Options();
        opts.setProperty(org.apache.axis2.transport.http.HTTPConstants.CHUNKED, Boolean.FALSE);

        opts.setTo(new EndpointReference(StatusMonitorConstants.MASHUP_HTTP +
                StatusMonitorAgentConstants.TENANT_SERVICES +
                sampleTenantConfigBean.getTenant() + "/carbon/RelatedProducts"));
        opts.setAction("http://services.mashup.wso2.org/RelatedProducts?xsd/RelatedProducts");

        serviceclient.setOptions(opts);
        try {
            result = serviceclient.sendReceive(payload);

            if ((result.toString().contains("New USB Graphics Drawing Tablet Mouse Pad"))) {
                relatedProductsServiceStatus = true;
            } else {
                MySQLConnector.insertStats(serviceID, false);
                MySQLConnector.insertState(serviceID, false, "Platform Sample: RelatedProducts service Invocation failed");
            }
        } catch (AxisFault e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform Sample: RelatedProducts - " + e.getMessage());
            String msg = "Error in executing the related products service";
            log.warn(msg, e);
        }
        catch (NullPointerException e) {
            MySQLConnector.insertStats(serviceID, false);
            MySQLConnector.insertState(serviceID, false, "Platform Sample: RelatedProducts - " + e.getMessage());
            String msg = "NPE in executing the related products service";
            log.warn(msg, e);
        }
        return relatedProductsServiceStatus;
    }
}
