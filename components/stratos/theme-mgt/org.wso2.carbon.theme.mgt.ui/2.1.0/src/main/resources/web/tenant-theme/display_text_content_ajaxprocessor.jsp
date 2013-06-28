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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<fmt:bundle basename="org.wso2.carbon.registry.resource.ui.i18n.Resources">
    <%
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        String textContent;
        try {
            ThemeMgtServiceClient client = new ThemeMgtServiceClient(cookie, config, session);
            textContent = client.getTextContent(request);
        } catch (Exception e) {
            response.setStatus(500);
    %><%=e.getMessage()%><%
        return;
    }
%>

    <br/>
    <textarea rows="15" cols="70" readonly="readonly"><%=textContent%></textarea>
    <br/>
    <input type='button' class='button' id="cancelContentButtonID"
           onclick='cancelTextContentDisplay()'
           value='<fmt:message key="done"/>'/>
    <br/>

</fmt:bundle>
