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
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
String successKey = request.getParameter("successKey");
String domain = request.getParameter("domain");
String status = request.getParameter("status");
session.setAttribute("validate-domain-success-key", successKey);
session.setAttribute("temp-domain-to-register", domain);

// redirect to the add registry domain page
if ("logged_in".equals(status)) {
    // we have already logged in so redirect to the account management page
    response.sendRedirect("../account-mgt/validation_success_ajaxprocessor.jsp");
}
else {
    // if not logged in we will redirect to the register org page
    response.sendRedirect("../tenant-register/success_register.jsp");
}
%>