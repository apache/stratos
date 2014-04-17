/*
 *  Copyright (c) 2005-2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.wso2.carbon.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.core.commons.stub.registry.service.RegistryAdminServiceStub;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class RegistryAdminServiceClient {

    private static final Log log = LogFactory.getLog(RegistryAdminServiceClient.class);
    private RegistryAdminServiceStub stub;
    private HttpSession session;

    public RegistryAdminServiceClient(String cookie, ServletConfig config, HttpSession session)
            throws AxisFault {
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                    session);
        ConfigurationContext ctx = (ConfigurationContext) config.
                    getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        this.session = session;
        String serviceEPR = serverURL + "RegistryAdminService";
        stub = new RegistryAdminServiceStub(ctx, serviceEPR);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        if (cookie != null) {
            options.setProperty(HTTPConstants.COOKIE_STRING, cookie);
        }
    }

    public boolean isRegistryReadOnly() {

        try {
            return stub.isRegistryReadOnly();
        } catch (Exception e) {
            String msg = "Error occurred while checking registry mode";
            log.error(msg, e);
        }

        return false;
    }

    public String getRegistryHTTPURL() {
        try {
            String httpPermalink = stub.getHTTPPermalink("/");
            if (httpPermalink != null) {
                return httpPermalink.substring(0, httpPermalink.length() - "/".length());
            }
        } catch (Exception e) {
            log.error("Unable to get permalink", e);
        }
        return "#";
    }

    public String getRegistryHTTPSURL() {
        try {
            String httpsPermalink = stub.getHTTPSPermalink("/");
            if (httpsPermalink != null) {
                return httpsPermalink.substring(0, httpsPermalink.length() - "/".length());
            }
        } catch (Exception e) {
            log.error("Unable to get permalink", e);
        }
        return "#";
    }

}
