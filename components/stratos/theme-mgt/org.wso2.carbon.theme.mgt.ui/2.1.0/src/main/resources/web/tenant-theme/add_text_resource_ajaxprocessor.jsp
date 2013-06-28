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
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.wso2.carbon.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.wso2.carbon.registry.resource.ui.processors.AddTextResourceProcessor" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String errorMessage = null;
    try {

        String parentPath = request.getParameter("parentPath");
        String fileName = request.getParameter("fileName");
        String mediaType = request.getParameter("mediaType");
        String description = request.getParameter("description");
        String content = request.getParameter("content");

        String cookie = (String) request.
                getSession().getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        try {
            ThemeMgtServiceClient client =
                    new ThemeMgtServiceClient(cookie, config, request.getSession());
            client.addTextResource(parentPath, fileName, mediaType, description, content);
        } catch (Exception e) {
            String msg = "Failed to add new text resource " + fileName +
                    " to the parent collection " + parentPath + ". " + e.getMessage();
            throw new UIException(msg, e);
        }

    } catch (Exception e) {
        errorMessage = e.getMessage();
    }
%>

<% if (errorMessage != null) { %>

<script type="text/javascript">
	location.href='../error.jsp?errorMsg=<%=errorMessage%>'
</script>

<% } %>
