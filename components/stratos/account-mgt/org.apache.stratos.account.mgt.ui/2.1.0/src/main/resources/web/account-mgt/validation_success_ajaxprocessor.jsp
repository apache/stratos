<!--
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
 -->
<%@ page import="org.wso2.carbon.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
try {
    String domainToRegister = (String)session.getAttribute("temp-domain-to-register");
    String successKey = (String)session.getAttribute("validate-domain-success-key");
    AccountMgtClient client = new AccountMgtClient(config, session);
    if (client.finishedDomainValidation(domainToRegister, successKey)) {

        //session.setAttribute(MultitenantConstants.TENANT_DOMAIN, domainToRegister);
        session.setAttribute("domain-validation-success", "true");
        // redirect to the login page with new domain name
        response.sendRedirect("/t/" + domainToRegister + "/carbon/account-mgt/account_mgt.jsp");
        return;
    } else {
        session.removeAttribute("temp-domain-to-register");
        session.removeAttribute("validate-domain-success-key");

        // now redirect to the account_mgt.js with the failure
        session.setAttribute("domain-validation-failure", "true");
        response.sendRedirect("../account-mgt/account_mgt.jsp");
        return;
    }
} catch (RegistryException e) {
%>
  <div>Error in finishing the validation.</div>
<%
    return;
}
%>