<%--
 Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 WSO2 Inc. licenses this file to you under the Apache License,
 Version 2.0 (the "License"); you may not use this file except
 in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@page import="org.wso2.carbon.ui.MenuAdminClient" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<c:if test="${!empty param.locale}">
    <fmt:setLocale value="${param.locale}" scope="page"/>
</c:if>
<%
    try {
        MenuAdminClient menuAdminClient = new MenuAdminClient();
%>
        <%=menuAdminClient.getMenuContent("region1", request)%>
<%
        request.getSession().setAttribute("menuadminClient", menuAdminClient);
        menuAdminClient.setBreadCrumbMap(request);
    } catch (Throwable e) {
        e.printStackTrace();
    }
%>

