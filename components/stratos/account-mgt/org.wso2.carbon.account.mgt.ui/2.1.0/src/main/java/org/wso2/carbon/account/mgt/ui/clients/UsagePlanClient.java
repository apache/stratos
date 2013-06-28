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
import org.wso2.carbon.account.mgt.stub.services.BillingDataAccessServiceStub;
import org.wso2.carbon.account.mgt.stub.services.beans.xsd.Subscription;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpSession;

public class UsagePlanClient {
    private BillingDataAccessServiceStub stub;
    private static final Log log = LogFactory.getLog(UsagePlanClient.class);

    public UsagePlanClient(ServletConfig config, HttpSession session)
            throws RegistryException {

        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.
                getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String epr = backendServerURL + "BillingDataAccessService";

        try {
            stub = new BillingDataAccessServiceStub(configContext, epr);
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


    public boolean updateUsagePlan(String usagePlanName) {
        try {
            stub.changeSubscriptionByTenant(usagePlanName);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public String getUsagePlanName(String tenantDomain) throws Exception{
        Subscription subscription;
        try {
            subscription=stub.getActiveSubscriptionOfCustomerByTenant();
            if(subscription!=null){
                return subscription.getSubscriptionPlan();
            } else {
                return "";
            }
        } catch (Exception e) {
            String msg = "Error occurred while getting the usage plan for tenant: " + tenantDomain;
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    public boolean deactivateActiveUsagePlan(String tenantDomain){
        log.info("Deactivating tenant domain: " + tenantDomain);
        boolean deactivated = false;
        try{
            deactivated = stub.deactivateActiveSubscriptionByTenant();
            if(deactivated){
                log.info("Active subscription deactivated after deactivating the tenant: " + tenantDomain);
            }
        }catch (Exception e){
            log.error("Error occurred while deactivating active subscription of: " +
                        tenantDomain + " " + e.getMessage(), e);
        }

        return deactivated;
    }
}
