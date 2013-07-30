<%-- 
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~ 
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~ 
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.apache.stratos.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page import="org.apache.stratos.account.mgt.ui.utils.Util" %>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.apache.stratos.account.mgt.ui.clients.EmailValidationClient" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<carbon:jsi18n
        resourceBundle="org.apache.stratos.register.ui.i18n.JSResources"
        request="<%=request%>" />
<%
    String data = (String)session.getAttribute("intermediate-data");
        
    Util.readIntermediateData(request, data);

    EmailValidationClient client;
    String email;
    String domain;
    try {
        email = (String)request.getAttribute("email");
        domain = (String)request.getAttribute("tenantDomain");
        String confirmationKey = (String)request.getAttribute("confirmationKey");

        client = new EmailValidationClient(config, session);
        client.proceedUpdateContact(domain, email, confirmationKey);
    } catch (RegistryException e) {
    %>
      <div>Error in validating the contact.</div>
    <%
        return;
    }

%>

<div style="margin:20px">You have successfully validated your email address and the contact information associated with your WSO2
    Cloud Services account is updated. Click <a href="../admin/index.jsp">here</a> to access WSO2 Cloud Manager.    
</div>

