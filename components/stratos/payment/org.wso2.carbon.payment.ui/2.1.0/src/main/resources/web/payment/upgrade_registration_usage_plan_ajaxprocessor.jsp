<%--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~    http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied.  See the License for the
~ specific language governing permissions and limitations
~ under the License.
--%>
<%@ page import="org.apache.stratos.account.mgt.UsagePlanClient" %>
<%@ page import="org.wso2.carbon.utils.CarbonUtils" %>
<%@ page import="org.apache.stratos.account.mgt.BillingDataAccessServiceStub" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>

<%
    String usagePlan = request.getParameter("selectedUsagePlan");
    String regTenantDomain = request.getParameter("regTenantDomain");
    String adminUserName = CommonUtil.getAdminUserName();
    String adminPassword = CommonUtil.getAdminPassword();

    BillingDataAccessServiceStub stub;
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
        CarbonUtils.setBasicAccessSecurityHeaders(adminUserName, adminPassword, client);
        stub.changeSubscriptionForTenant(usagePlan, regTenantDomain);

    } catch (AxisFault axisFault) {
        String msg = "Failed to upgrade the usage plan for tenant " + regTenantDomain +
                     " to " + usagePlan + " during registration. " + axisFault.getMessage();
        throw new Exception(msg, axisFault);
    }

%>