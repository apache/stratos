<%--
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
 --%>
<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Invoice" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Payment" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="java.util.Date" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.clients.TenantActivityServiceClient" %>
<%@ page import="java.io.Console" %>
<%@ page import="javax.rmi.CORBA.Util" %>
<%@ page import="org.apache.stratos.tenant.activity.ui.utils.TenantMonitorUtil" %>
<%@ page import="org.apache.stratos.tenant.activity.stub.beans.xsd.PaginatedTenantDataBean" %>

<%
    String serverUrl = request.getParameter("backEndUrl");
    String serviceName = request.getParameter("serviceName");


    int count = 0;
    if (serverUrl == null || "null".equals(serverUrl.trim())) {
        try {
            TenantActivityServiceClient client = new TenantActivityServiceClient(config, session);
           // TenantMonitorUtil.tenantDataList.put(serviceName, client.getAllActiveTenantList());
           count=client.getActiveTenantCount();
        } catch (Exception e) {
        }

    } else {
        try {
            TenantActivityServiceClient client = new TenantActivityServiceClient(serverUrl, config, session);
           // count = client.getAllActiveTenantList().length;
           count=client.getActiveTenantCount();
            if (count > 0) {
               // TenantMonitorUtil.tenantDataList.put(serviceName, client.getAllActiveTenantList());
            } else {
                TenantMonitorUtil.tenantDataList.put(serviceName, null);
            }
        } catch (Exception e) {
        }
    }

    try {
        JSONObject obj = new JSONObject();
        obj.put("count", count);
        obj.put("service", serviceName);
        out.write(obj.toString());
    } catch (Exception e) {
        e.printStackTrace();
    }
%>