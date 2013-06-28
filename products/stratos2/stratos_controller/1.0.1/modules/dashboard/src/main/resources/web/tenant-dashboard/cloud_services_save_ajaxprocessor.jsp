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
<%@ page import="org.wso2.stratos.manager.dashboard.ui.clients.CloudManagerServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %><%
    String[] activeCloudServiceNames = request.getParameterValues("cloudServices");

    String error = "Error in saving the results of the activities";
    try {
        CloudManagerServiceClient cloudServiceClient = new CloudManagerServiceClient(request, config, session);
        cloudServiceClient.saveCloudServicesActivity(activeCloudServiceNames);
    } catch (Exception e) {
        request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error,error,null));
%>
        <jsp:include page="../admin/error.jsp?<%=error%>"/>
<%
        return;
    }
    response.sendRedirect("../tenant-dashboard/cloud_services_configuration.jsp");
%>