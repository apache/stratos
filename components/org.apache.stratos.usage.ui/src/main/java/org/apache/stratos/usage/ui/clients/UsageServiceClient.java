/*
 *Licensed to the Apache Software Foundation (ASF) under one
 *or more contributor license agreements.  See the NOTICE file
 *distributed with this work for additional information
 *regarding copyright ownership.  The ASF licenses this file
 *to you under the Apache License, Version 2.0 (the
 *"License"); you may not use this file except in compliance
 *with the License.  You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing,
 *software distributed under the License is distributed on an
 *"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *KIND, either express or implied.  See the License for the
 *specific language governing permissions and limitations
 *under the License.
 */
package org.apache.stratos.usage.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.apache.stratos.usage.stub.beans.xsd.InstanceUsageStatics;
import org.apache.stratos.usage.stub.beans.xsd.PaginatedInstanceUsage;
import org.apache.stratos.usage.stub.beans.xsd.PaginatedTenantUsageInfo;
import org.apache.stratos.usage.stub.beans.xsd.TenantUsage;
import org.apache.stratos.usage.stub.services.UsageServiceStub;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class UsageServiceClient {
    private static final Log log = LogFactory.getLog(UsageServiceClient.class);

    private UsageServiceStub stub;
    private String epr;

    public UsageServiceClient(
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws RegistryException {

        epr = backendServerURL + "UsageService";

        try {
            stub = new UsageServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate UsageService service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public UsageServiceClient(ServletConfig config, HttpSession session)
            throws RegistryException {

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "UsageService";

        try {
            stub = new UsageServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate UsageService service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public TenantUsage retrieveCurrentTenantUsage(String yearMonth) throws Exception {
        return stub.retrieveCurrentTenantUsage(yearMonth);
    }

    public TenantUsage[] retrieveTenantUsages(String yearMonth) throws Exception {
        return stub.retrieveTenantUsages(yearMonth);
    }

    public PaginatedTenantUsageInfo retrievePaginatedTenantUsages(String yearMonth, int pageNumber,
                                                                  int entriesPerPage) throws Exception {
        return stub.retrievePaginatedTenantUsages(yearMonth, pageNumber, entriesPerPage);
    }

    public TenantUsage retrieveTenantUsage(String yearMonth, int tenantId) throws Exception {
        return stub.retrieveTenantUsage(yearMonth, tenantId);
    }

    public InstanceUsageStatics[] retrieveInstanceUsage() throws Exception{
            return stub.retrieveInstanceUsage();
    }

    public PaginatedInstanceUsage retrievePaginatedInstanceUsage (
            String yearMonth, int pageNumber, int numbersPerPage) throws Exception {
            return stub.retrievePaginatedInstanceUsage(yearMonth, pageNumber, numbersPerPage);
    }
}
