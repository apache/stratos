/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.account.mgt.ui.clients;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.account.mgt.stub.beans.xsd.AccountInfoBean;
import org.wso2.carbon.account.mgt.stub.services.AccountMgtServiceStub;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class AccountMgtClient {
    private static final Log log = LogFactory.getLog(AccountMgtClient.class);

    private AccountMgtServiceStub stub;
    private String epr;

    public AccountMgtClient(String cookie, String backendServerURL,
            ConfigurationContext configContext) throws RegistryException {

        epr = backendServerURL + "AccountMgtService";

        try {
            stub = new AccountMgtServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate AccountMgt service client.";
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public AccountMgtClient(ServletConfig config, HttpSession session) throws RegistryException {

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(
                        CarbonConstants.CONFIGURATION_CONTEXT);
        epr = backendServerURL + "AccountMgtService";

        try {
            stub = new AccountMgtServiceStub(configContext, epr);

            ServiceClient client = stub._getServiceClient();
            Options option = client.getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

        } catch (AxisFault axisFault) {
            String msg = "Failed to initiate AccountMgt service client.";
            log.error(msg, axisFault);
            throw new RegistryException(msg, axisFault);
        }
    }

    public void updateContact(String contactEmail) throws RegistryException {
        try {
            stub.updateContact(contactEmail);
        } catch (Exception e) {
            String msg = "Failed to update contact.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public String getContact() throws RegistryException {
        try {
            return stub.getContact();
        } catch (Exception e) {
            String msg = "Failed to get contact.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

   public boolean updateFullname(AccountInfoBean fullname) throws RegistryException {
        try {
            return stub.updateFullname(fullname);
        } catch (Exception e) {
            String msg = "Failed to update Fullname.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public AccountInfoBean getFullname() throws RegistryException {
        try {
            return stub.getFullname();
        } catch (Exception e) {
            String msg = "Failed to get administrator full name.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public void deactivate() throws RegistryException {
        try {
            stub.deactivate();
        } catch (Exception e) {
            String msg = "Failed to deactivate.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public boolean isDomainValidated() throws RegistryException {
        try {
            return stub.isDomainValidated();
        } catch (Exception e) {
            String msg = "Failed to check the domain validation.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public boolean finishedDomainValidation(String validatedDomain, String successKey)
            throws RegistryException {
        try {
            return stub.finishedDomainValidation(validatedDomain, successKey);
        } catch (Exception e) {
            String msg = "Failed to finish the domain validation.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public boolean checkDomainAvailability(String domainName) throws RegistryException {
        try {
            return stub.checkDomainAvailability(domainName);
        } catch (Exception e) {
            String msg = "Failed to finish the domain availability.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }

    public boolean isEmailValidated() throws RegistryException {
        try {
            return stub.isEmailValidated();
        } catch (Exception e) {
            String msg = "Failed to check the email validation.";
            log.error(msg, e);
            throw new RegistryException(msg, e);
        }
    }
}
