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
<%@ page import="java.util.List" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.clients.TenantActivityServiceClient" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.utils.TenantMonitorUtil" %>
<%@ page import="java.util.Map" %>
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


    <div id="middle">
        <div id="top">
            <h2><fmt:message key="active.tenants.services"/></h2>

            <form id="findTenantForm" action="find-tenant.jsp" method="post">
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <tr style="border:0; !important">
                        <td style="border:0; !important">
                            <nobr>
                                &nbsp;&nbsp;&nbsp;
                                Enter Tenant Domain
                                <input type="text" name="domain" id="domain" value="">&nbsp;
                            </nobr>
                        </td>
                        <td style="border:0; !important">
                            <a class="icon-link" href="#" style="background-image: url(images/search.gif);"
                               onclick="domainSelected();" alt="Search"></a>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </div>

        <div id="workArea">
            <table id="serviceStatTable1" cellpadding="0" cellspacing="0" border="0" style="width:100%"
                   class="styledLeft">
                <thead>
                <tr>
                    <th style="padding-left:5px;text-align:left;width:40%"><fmt:message key="service.name"/></th>
                    <th style="padding-left:5px;text-align:left;width:150px"><fmt:message
                            key="active.tenant.count"/></th>
                    <th style="padding-left:5px;text-align:left;"><fmt:message key="view.list"/></th>
                </tr>
                </thead>
            </table>
        </div>
    </div>
</fmt:bundle>

<%
    Map<String, CloudServiceConfig> cloudServicesConfigs = TenantMonitorUtil.getCloudServiceConfigMap();
%>


<script type="text/javascript">

    function getStats() {
        console.log("In the getStat() function");
        var serviceArray = new Array();

    <%
    for(String serviceName : cloudServicesConfigs.keySet()){

    %>

        var serviceObj = new Object();
        serviceObj.backEndUrl = '<%=cloudServicesConfigs.get(serviceName).getLink()%>';
        serviceObj.serviceName = '<%=serviceName%>';
        serviceArray.push(serviceObj);

    <% }
    %>

        for (var i = 0; i < serviceArray.length; i++) {

            jQuery.ajax({
                type: 'GET',
                url: 'get-data-ajaxprocessor.jsp',
                data: 'backEndUrl=' + serviceArray[i].backEndUrl + '&serviceName=' + serviceArray[i].serviceName,
                dataType: 'json',
                async: true,
                success: function(msg) {
                    var resp = msg;
                    var view = 'View';
                    if (resp.count > 0) {
                        document.getElementById('serviceStatTable1').innerHTML += '<tr><td><a href="javascript:showTenants(\'' + resp.service + '\');">' + resp.service + '</a></td><td>' + resp.count + '</td><td><a href="javascript:showTenants(\'' + resp.service + '\');">' + view + '</a></td></tr>';
                    }
                    else {
                        document.getElementById('serviceStatTable1').innerHTML += '<tr><td>' + resp.service + '</td><td>' + resp.count + '</td><td>-</td></tr>';
                    }
                },
                error:function () {
                    CARBON.showErrorDialog('Could not connect to server');
                    //document.getElementById('serviceStatTable').innerHTML += '<tr><td>' + eresp.servic + '</td><td>' + resp.count + '</td></tr>';
                    document.getElementById('serviceStatTable').innerHTML += 'Not Available';
                }
            });

        }
    }
    function showTenants(serviceNameKey) {
        document.getElementById("serviceName").value = serviceNameKey;
        document.myform.submit();
    }

    function domainSelected() {
        var findDomainForm = document.getElementById('findTenantForm');
        var domain = document.getElementById("domain").getValue();
        if(domain.length > 3){
            findDomainForm.submit();
        }
        else{
          CARBON.showErrorDialog('Enter valid domain name');
        }

    }

</script>

<body onload="getStats();">
<div>
    <table id="serviceStatTable"></table>
    <form name="myform" method="post" action="show-active-tenants.jsp">
        <input type='hidden' name='serviceName' id="serviceName"/>
    </form>
</div>
</body>
