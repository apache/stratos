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

package org.apache.stratos.integration.common;

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.integration.common.admin.client.LogViewerClient;
import org.wso2.carbon.integration.common.utils.exceptions.AutomationUtilException;

import javax.xml.xpath.XPathExpressionException;

public class ServerLogClient {

    private static final Log log = LogFactory.getLog(ServerLogClient.class);
    private LogViewerClient logViewerClient;
    private AutomationContext automationContext;

    public ServerLogClient () throws AutomationUtilException {
        createAutomationContext();
        createlogViewerClient(getBackEndUrl(), getUsername(), getPassword());
    }

    public ServerLogClient (String backEndUrl, String username, String password) throws
            AutomationUtilException {
        createlogViewerClient(backEndUrl, username, password);
    }

    private void createlogViewerClient(String backEndUrl, String username, String password) throws AutomationUtilException {
        try {
            logViewerClient = new LogViewerClient(backEndUrl, username, password);
        } catch (AxisFault e) {
            String errorMsg = "Error in creating LogViewerClient";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

    private void createAutomationContext() throws AutomationUtilException {
        try {
            automationContext = new AutomationContext();
        } catch (XPathExpressionException e) {
            String errorMsg = "Error in creating AutomationContext";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

    private String getBackEndUrl () throws AutomationUtilException {
        try {
            return automationContext.getContextUrls().getBackEndUrl();
        } catch (XPathExpressionException e) {
            String errorMsg = "Error in getting beck end URL";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

    private String getUsername () throws AutomationUtilException {
        try {
            return automationContext.getSuperTenant().getTenantAdmin().getUserName();
        } catch (XPathExpressionException e) {
            String errorMsg = "Error in getting super tenant username";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }

    private String getPassword () throws AutomationUtilException {
        try {
            return automationContext.getSuperTenant().getTenantAdmin().getPassword();
        } catch (XPathExpressionException e) {
            String errorMsg = "Error in getting super tenant password";
            log.error(errorMsg, e);
            throw new AutomationUtilException(errorMsg, e);
        }
    }


    public LogViewerClient getLogViewerClient () {
        return logViewerClient;
    }
}
