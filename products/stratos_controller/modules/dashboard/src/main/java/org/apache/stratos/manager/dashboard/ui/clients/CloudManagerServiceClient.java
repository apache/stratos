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
package org.apache.stratos.manager.dashboard.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;
import org.wso2.carbon.stratos.manager.dashboard.stub.CloudManagerServiceStub;
import org.wso2.carbon.stratos.manager.dashboard.stub.xsd.CloudService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;

public class CloudManagerServiceClient {
     private static final Log log = LogFactory.getLog(CloudManagerServiceClient.class);

    private CloudManagerServiceStub stub;
    private String epr;
    public static final String CLOUD_SERVICE = "cloudService";

    public CloudManagerServiceClient(
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws RegistryException {

        epr = backendServerURL + "CloudManagerService";

        try {
            stub = new CloudManagerServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate AddServices service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public CloudManagerServiceClient(ServletRequest request, ServletConfig config, HttpSession session)
            throws RegistryException {

        String cookie = (String)session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "CloudManagerService";

        try {
            stub = new CloudManagerServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate Add Services service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public CloudService[] retrieveCloudServiceInfo() throws Exception {
        try {
            if (CarbonContext.getCurrentContext().getCache(null).containsKey(CLOUD_SERVICE)) {
                return (CloudService[]) CarbonContext.getCurrentContext()
                        .getCache(null).get(CLOUD_SERVICE);
            }
        } catch (Exception ignored) {
            // TODO: this exception needs not be handled, but the situation which leads to this
            // exception needs to be.
        }
        CloudService[] cloudServices = stub.retrieveCloudServiceInfo();
        CarbonContext.getCurrentContext().getCache(null).put(CLOUD_SERVICE, cloudServices);
        return cloudServices;
    }


    public void saveCloudServicesActivity(String[] activeServiceNames) throws Exception {
        CloudService[] cloudServices =
                (CloudService[]) CarbonContext.getCurrentContext().getCache(null).get(CLOUD_SERVICE);

        for (CloudService cloudService : cloudServices) {
            for (String activeService : activeServiceNames) {
                if (cloudService.getName().equals(activeService)) {
                    cloudService.setActive(true);
                    break;
                } else {
                    cloudService.setActive(false);
                }
            }
        }

        stub.saveCloudServicesActivity(activeServiceNames);
    }
}
