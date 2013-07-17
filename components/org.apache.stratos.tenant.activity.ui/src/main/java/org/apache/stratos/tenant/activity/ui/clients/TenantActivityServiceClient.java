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
package org.apache.stratos.tenant.activity.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.authenticator.proxy.AuthenticationAdminClient;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.apache.stratos.tenant.activity.stub.TenantActivityServiceExceptionException;
import org.apache.stratos.tenant.activity.stub.beans.xsd.*;
import org.apache.stratos.tenant.activity.ui.internal.TenantActivityUIServiceComponent;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.utils.ServerConstants;
import org.apache.stratos.tenant.activity.stub.TenantActivityServiceStub;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;

public class TenantActivityServiceClient {
    private static final Log log = LogFactory.getLog(TenantActivityServiceClient.class);

    private TenantActivityServiceStub stub;
    private String epr;


    public TenantActivityServiceClient(
            String cookie, String backendServerURL, ConfigurationContext configContext)
            throws RegistryException {

        epr = backendServerURL + "TenantMonitorService";

        try {
            stub = new TenantActivityServiceStub(configContext, epr);
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate TenantMonitorService service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public TenantActivityServiceClient(ServletConfig config, HttpSession session)
            throws RegistryException {
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "TenantActivityService";

        try {
            stub = new TenantActivityServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate TenantMonitorService service client. " + axisFault.getMessage();
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public TenantActivityServiceClient(String url, ServletConfig config, HttpSession session)
            throws Exception {
        //String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        try {
            String cookie = login(url + "/services/", 
                    TenantActivityUIServiceComponent.stratosConfiguration.getAdminUserName(),
                    TenantActivityUIServiceComponent.stratosConfiguration.getAdminPassword(), 
                    configContext);
            epr = url + "/services/TenantActivityService";
            stub = new TenantActivityServiceStub(configContext, epr);
            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate TenantMonitorService service client. ";
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }


    public int getActiveTenantCount() throws TenantActivityServiceExceptionException, RemoteException {
        int count = stub.getActiveTenantCount();
        return count;
    }

    /*public String[] getActiveTenantList() throws TenantActivityServiceExceptionException, RemoteException {
        return stub.getActiveTenantList();
    } */

    public PaginatedTenantDataBean getPaginatedActiveTenantList(int pageNumber) throws TenantActivityServiceExceptionException, RemoteException {
        return stub.retrievePaginatedActiveTenants(pageNumber);
    }

    /*  public TenantDataBean[] getAllActiveTenantList() throws TenantActivityServiceExceptionException, RemoteException {
        return stub.getAllActiveTenantList();
    }*/

    public boolean isTenantActiveInService(String domainName) throws TenantActivityServiceExceptionException, RemoteException {
        return stub.isActiveTenantOnService(domainName);
    }
    
    private String login(String serverUrl, String userName, 
                               String password, ConfigurationContext confContext) throws UserStoreException {
        String sessionCookie = null;
        try {
            AuthenticationAdminClient client =
                    new AuthenticationAdminClient(confContext, serverUrl, null, null, false);
            //TODO : get the correct IP
            boolean isLogin = client.login(userName, password, "127.0.0.1");
            if (isLogin) {
                sessionCookie = client.getAdminCookie();
            }
        } catch (Exception e) {
            throw new UserStoreException("Error in login to the server server: " + serverUrl +
                                         "username: " + userName + ".", e);
        }
        return sessionCookie;
    }

}
