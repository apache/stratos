<%@ page import="org.wso2.carbon.ui.clients.RegistryAdminServiceClient" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%--
 * Copyright 2008 WSO2, Inc. http://www.wso2.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
--%>
<%@ page contentType="text/css" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<c:catch var="e">
<%
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        RegistryAdminServiceClient client = new RegistryAdminServiceClient(cookie, config, session);
        if (client.isRegistryReadOnly()) {
%>
.registryWriteOperation {
    display:none !important;
    height:0 !important;
    font-size:0 !important;
}
<%
        } else {
%>
.registryNonWriteOperation {
    display:none !important;
    height:0 !important;
    font-size:0 !important;
}
<%
        }
%>

</c:catch>