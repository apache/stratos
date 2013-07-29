<!--
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
  -->

<%@ page import="org.wso2.carbon.tenant.mgt.ui.clients.TenantServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<script type="text/javascript" src="../admin/js/jquery.js"></script>
<script type="text/javascript" src="../admin/js/jquery.form.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>

<%--<carbon:jsi18n--%>
		<%--resourceBundle="org.apache.stratos.tenant.mgt.ui.i18n.JSResources"--%>
		<%--request="<%=request%>" />--%>

<div id="middle">
<%
    String error = "Error in updating the tenant activation status.";
    String tenantDomain = "";
    Boolean activated = (Boolean)session.getAttribute("isActivatedTenant");

    TenantServiceClient serviceClient = new TenantServiceClient(config, session);

    try {
        tenantDomain = request.getParameter("activatingDomain");

        if(activated){
            serviceClient.deactivateTenant(tenantDomain);
        } else if (!activated){
            serviceClient.activateTenant(tenantDomain);
        }

        response.sendRedirect("../tenant-mgt/view_tenants.jsp");

    } catch (Exception e) {
            CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
            request.setAttribute(CarbonUIMessage.ID, uiMsg);
                 %>
                <jsp:forward page="../admin/error.jsp"/>
                 <%
             return;
    }
%>
    </div>
