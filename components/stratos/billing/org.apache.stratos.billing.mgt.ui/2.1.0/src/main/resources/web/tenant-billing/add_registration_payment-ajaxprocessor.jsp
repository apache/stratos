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
<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Payment" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.services.MultitenancyBillingServiceStub" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.CarbonUtils" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Date" %>
<%
    String amount = request.getParameter("amount");
    String usagePlan = request.getParameter("usagePlan");
    String regDomain = session.getAttribute("regTenantDomain").toString();
    String transactionId = request.getParameter("transactionId");

    Payment payment = new Payment();
    payment.setDescription(regDomain + " " + transactionId);
    payment.setDate(new Date(System.currentTimeMillis()));

    // Stub to call the billing service in order to add the registration payment record. 
    MultitenancyBillingServiceStub stub;
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config.
            getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String epr = backendServerURL + "MultitenancyBillingService";
    String adminUserName = CommonUtil.getAdminUserName();
    String adminPassword = CommonUtil.getAdminPassword();

    try {
        stub = new MultitenancyBillingServiceStub(configContext, epr);

        ServiceClient client = stub._getServiceClient();
        Options option = client.getOptions();
        option.setManageSession(true);
        option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        CarbonUtils.setBasicAccessSecurityHeaders(adminUserName, adminPassword, client);
        int paymentId = stub.addRegistrationPayment(payment, amount, usagePlan);

        JSONObject obj = new JSONObject();
        obj.put("paymentId", paymentId);
        if (paymentId > 0) {
            obj.put("status", "success");
            obj.put("amount", amount);
        } else {
            obj.put("status", "fail");
        }
        out.write(obj.toString());

    } catch (Exception ex) {
        String msg = "Failed to initiate BillingService service client. " + ex.getMessage();
        throw new Exception(msg, ex);
    }

%>
