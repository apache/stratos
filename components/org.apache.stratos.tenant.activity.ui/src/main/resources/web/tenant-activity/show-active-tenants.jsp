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
<%@ page import="java.util.List" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.clients.TenantActivityServiceClient" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.utils.TenantMonitorUtil" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.wso2.carbon.tenant.reg.agent.client.util.Util" %>
<%@ page import="org.apache.stratos.common.config.CloudServiceConfig" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="org.apache.stratos.tenant.activity.stub.TenantActivityServiceStub" %>
<%@ page import="org.apache.stratos.tenant.activity.stub.beans.xsd.TenantDataBean" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/tenant_config.js"></script>

<carbon:jsi18n
        resourceBundle="org.apache.stratos.tenant.activity.ui.i18n.JSResources"
        request="<%=request%>"/>

<fmt:bundle basename="org.apache.stratos.tenant.activity.ui.i18n.Resources">
    <carbon:breadcrumb
            label="govern.view_tenants.menu"
            resourceBundle="org.apache.stratos.tenant.activity.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>
    <%
        String serviceNameStr = request.getParameter("serviceName");
    %>

    <div id="middle">
        <h2><fmt:message key="active.tenants.on"/> <%=serviceNameStr%>
        </h2>

        <div id="workArea">
            <table id="serviceStatTable1" cellpadding="0" cellspacing="0" border="0" style="width:100%"
                   class="styledLeft">
                <thead>
                <tr>
                    <th style="padding-left:5px;text-align:left;width:30%"><fmt:message key="tenant.domain"/></th>
                    <th style="padding-left:5px;text-align:left;"><fmt:message
                            key="active.status"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    String pageNumberStr = request.getParameter("pageNumber");
                    if (pageNumberStr == null) {
                        pageNumberStr = "0";

                    }
                    int pageNumber = 1;
                    try {
                        pageNumber = Integer.parseInt(pageNumberStr);
                    } catch (NumberFormatException ignored) {
                        // page number format exception
                    }
                    int numberOfPages = 1;
                    try {
                        String backEndUrl = TenantMonitorUtil.getCloudServiceConfigMap().get(request.getParameter("serviceName")).getLink();
                        TenantActivityServiceClient client;
                        if (backEndUrl == null || "null".equals(backEndUrl.trim())) {
                            client = new TenantActivityServiceClient(config, session);
                        } else {
                            client = new TenantActivityServiceClient(backEndUrl, config, session);
                        }
                        for (TenantDataBean bean : client.getPaginatedActiveTenantList(pageNumber).getTenantInfoBeans()) {
                %>
                <tr>
                    <td><%=bean.getDomain()%>
                    </td>
                    <td>Active</td>
                </tr>
                <%
                    }
                } catch (Exception e) {
                %>
                <tr>
                    <td>No Data Available for this Service</td>
                </tr>
                <% }
                    String reDirectPage = "show-active-tenants.jsp?serviceName=" + serviceNameStr + "&";
                %>
                <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                                  page="<%=reDirectPage%>" pageNumberParameterName="pageNumber"/>
                </tbody>
            </table>
            <input type='hidden' name='serviceName' id="serviceName"/>
        </div>
    </div>
</fmt:bundle>

<script type="text/javascript">

    function getStats() {
        console.log("In the getStat() function");

    }
    function showTenants(serviceNameKey) {
    }
</script>
