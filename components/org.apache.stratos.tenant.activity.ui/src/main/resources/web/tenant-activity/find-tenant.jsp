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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.utils.TenantMonitorUtil" %>
<%@ page import="org.apache.stratos.common.config.CloudServiceConfig" %>
<%@ page import="java.util.Map" %>
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
        String domainNameStr = request.getParameter("domain");
    %>
    <div id="middle">
        <h2><fmt:message key="tenant.state.on.services.for.tenant"/> "<%=domainNameStr%>"</h2>

        <div id="workArea">
            <table id="activeStateTable" cellpadding="0" cellspacing="0" border="0" style="width:100%"
                   class="styledLeft">
                <thead>
                <tr>
                    <th style="padding-left:5px;text-align:left;width:40%"><fmt:message key="service.name"/></th>
                    <th style="padding-left:5px;text-align:left;"><fmt:message key="tenant.status"/></th>
                </tr>
                </thead>
            </table>
        </div>
    </div>
</fmt:bundle>
<%
    String domainName = request.getParameter("domain");
    Map<String, CloudServiceConfig> cloudServicesConfigs = TenantMonitorUtil.getCloudServiceConfigMap();
%>

<script type="text/javascript">

    function getStats() {
        var serviceArray = new Array();
    <%
    for(String serviceName : cloudServicesConfigs.keySet()){
    %>
        var serviceObj = new Object();
        serviceObj.backEndUrl = '<%=cloudServicesConfigs.get(serviceName).getLink()%>';
        serviceObj.serviceName = '<%=serviceName%>';
        serviceObj.domainName = '<%=domainName%>';
        serviceArray.push(serviceObj);
    <% }
    %>

        for (var i = 0; i < serviceArray.length; i++) {

            jQuery.ajax({
                type: 'GET',
                url: 'find-tenant-ajaxprocessor.jsp',
                data: 'backEndUrl=' + serviceArray[i].backEndUrl + '&serviceName=' + serviceArray[i].serviceName + '&domainName=' + serviceArray[i].domainName,
                dataType: 'json',
                async: true,
                success: function(msg) {
                    var resp = msg;
                    var active = 'Active';
                    var inActive = 'Inactive';
                    if (resp.isActive === true) {
                        document.getElementById('activeStateTable').innerHTML += '<tr><td>' + resp.service + '</td><td>' + active + '</td></tr>';
                    }
                    else {
                        document.getElementById('activeStateTable').innerHTML += '<tr><td>' + resp.service + '</td><td>' + inActive + '</td></tr>';
                    }
                },
                error:function () {
                    CARBON.showErrorDialog('Could not connect to server');
                    //document.getElementById('serviceStatTable').innerHTML += '<tr><td>' + eresp.servic + '</td><td>' + resp.count + '</td></tr>';
                    document.getElementById('activeStateTable').innerHTML += 'Not Available';
                }
            });
        }
    }
</script>

<body onload="getStats();">
<div>
</div>
</body>
