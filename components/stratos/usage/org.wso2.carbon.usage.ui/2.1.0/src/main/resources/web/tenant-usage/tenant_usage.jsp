<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.BandwidthStatistics" %>
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.RequestStatistics" %>
<%@ page import="org.wso2.carbon.usage.ui.report.UsageReport" %>
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.TenantUsage" %>
<%@ page import="java.util.List" %>
<%@ page import="org.wso2.carbon.usage.ui.utils.UsageUtil" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<carbon:jsi18n
        resourceBundle="org.wso2.carbon.usage.ui.i18n.JSResources"
        request="<%=request%>"/>


<fmt:bundle basename="org.wso2.carbon.usage.ui.i18n.Resources">
<carbon:breadcrumb
        label="tenant.menu"
        resourceBundle="org.wso2.carbon.usage.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/register_config.js"></script>
<script type="text/javascript" src="js/usage-report.js"></script>
<%

    TenantUsage usage = UsageUtil.retrieveCurrentTenantUsage(request, config, session);
    int numberOfUsers = usage.getNumberOfUsers();
    String yearMonth = request.getParameter("year-month");
    session.setAttribute("year-month", yearMonth);


    if (yearMonth == null) {
        // get the current year month
        yearMonth = UsageUtil.getCurrentYearMonth();
    }
    String username = (String) request.getSession().getAttribute("logged-user");
    String tenantName = usage.getDomain();
    int tenantId = usage.getTenantId();
    String currentYearMonth = UsageUtil.getCurrentYearMonth();
%>

<%

    UsageReport usageReport = new UsageReport(config, session, request);
	List<String>  reportDataArray = usageReport.getUsageReportData();
    request.getSession().setAttribute("usage_data", reportDataArray);
%>

<div id="middle">

<h2><fmt:message key="tenant.usage.report"/> for the Month - <%=yearMonth%> (Tenant: <%=tenantName%>
    )</h2>

<%--<div id="report_ui">
    <carbon:report
            component="org.wso2.carbon.usage"
            template="usage_report"
            pdfReport="true"
            htmlReport="true"
            excelReport="true"
            reportDataSession="usage_data"
               />
</div>--%>
<div id="workArea">

<form id="usageForm" action="tenant_usage.jsp" method="post">

<table class="styledLeft">
<thead>
<tr>
    <th>
        <fmt:message key="report.duration"/>
    </th>
</tr>
</thead>
<tbody>
<tr>
<td class="nopadding">
<table class="normal-nopadding" cellspacing="0">
<tbody>
<tr>
    <td><fmt:message key="year.month"/></td>
    <td colspan="3">
        <select onchange="this.form.submit()" name="year-month" id="year-month" style="width:200px">
            <%
                for (String ym : UsageUtil.getYearMonths()) {
                    String selectedStr = "";
                    if (ym.equals(yearMonth)) {
                        selectedStr = "selected=\"true\" ";
                    }
            %>
            <option <%=selectedStr%> value="<%=ym%>"><%=ym%>
            </option>
            <%
                }
            %>
        </select>
    </td>
</tr>
<%
    if (currentYearMonth.equals(yearMonth)) {
%>
<tr>
    <td colspan="4" class="middle-header"><fmt:message key="users"/></td>
</tr>
<tr>
    <td><fmt:message key="number.of.users"/></td>
    <td colspan="3">
        <input readonly="1" type="text" name="userCount" id="userCount" style="width:200px"
               value="<%=numberOfUsers%>"/>
    </td>
</tr>

<tr>
    <td colspan="4" class="middle-header"><fmt:message key="storage.usage"/></td>
</tr>

<tr>
    <td><fmt:message key="data.storage.name"/></td>
    <td><fmt:message key="current.data.storage"/></td>
    <td><fmt:message key="history.data.storage"/></td>
    <td><fmt:message key="total.data.storage"/></td>
</tr>
<%
    String totalDataStorage = UsageUtil.getTotalDataStorage(usage);
    String currentDataStorage = UsageUtil.getCurrentDataStorage(usage);
    String historyDataStorage = UsageUtil.getHistoryDataStorage(usage);
%>
<tr>
    <td><fmt:message key="registry.content.storage.name"/></td>
    <td colspan="1">
        <input readonly="1" type="text" name="currentData" id="currentData" style="width:200px"
               value="<%=currentDataStorage%>"/>
    </td>
    <td colspan="1">
        <input readonly="1" type="text" name="historyData" id="historyData" style="width:200px"
               value="<%=historyDataStorage%>"/>
    </td>
    <td colspan="1">
        <input readonly="1" type="text" name="totalData" id="totalData" style="width:200px"
               value="<%=totalDataStorage%>"/>
    </td>
</tr>
<%
    }
%>

<%--<tr>
    <td colspan="4" class="middle-header"><fmt:message key="registry.bandwidth.usage"/></td>
</tr>
<tr>
    <td><fmt:message key="server.name"/></td>
    <td><fmt:message key="incoming.bandwidth"/></td>
    <td><fmt:message key="outgoing.bandwidth"/></td>
    <td><fmt:message key="total.bandwidth"/></td>
</tr>
<%
    String totRegInBandwidth = UsageUtil.getIncomingBandwidth(usage.getTotalRegistryBandwidth());
    String totRegOutBandwidth = UsageUtil.getOutgoingBandwidth(usage.getTotalRegistryBandwidth());
    String totRegBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalRegistryBandwidth());
    BandwidthStatistics[] regBWStats = usage.getRegistryBandwidthStatistics();
    if (regBWStats != null && regBWStats.length > 0 && regBWStats[0] != null) {
        for (BandwidthStatistics stat : regBWStats) {
            String regInBandwidth = UsageUtil.getIncomingBandwidth(stat);
            String regOutBandwidth = UsageUtil.getOutgoingBandwidth(stat);
            String regBandwidth = UsageUtil.getTotalBandwidth(stat);
%>
<tr>
    <td><%=stat.getKey()%>
    </td>
    <td>
        <input readonly="1" type="text" name="registryIncomingBW" id="registryIncomingBW"
               style="width:200px" value="<%=regInBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="registryOutgoingBW" id="registryOutgoingBW"
               style="width:200px" value="<%=regOutBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="registryTotalBW" id="registryTotalBW"
               style="width:200px" value="<%=regBandwidth%>"/>
    </td>
</tr>
<%
        }
    }
%>
<tr>
    <td><fmt:message key="all.server.name"/></td>
    <td>
        <input readonly="1" type="text" name="totRegistryIncomingBW" id="totRegistryIncomingBW"
               style="width:200px" value="<%=totRegInBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totRegistryOutgoingBW" id="totRegistryOutgoingBW"
               style="width:200px" value="<%=totRegOutBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totRegistryTotalBW" id="totRegistryTotalBW"
               style="width:200px" value="<%=totRegBandwidth%>"/>
    </td>
</tr>
--%>

<tr>
    <td colspan="4" class="middle-header"><fmt:message key="service.bandwidth.usage"/></td>
</tr>
<tr>
    <td><fmt:message key="server.name"/></td>
    <td><fmt:message key="incoming.bandwidth"/></td>
    <td><fmt:message key="outgoing.bandwidth"/></td>
    <td><fmt:message key="total.bandwidth"/></td>
</tr>
<%
    String totSvcInBandwidth = UsageUtil.getIncomingBandwidth(usage.getTotalServiceBandwidth());
    String totSvcOutBandwidth = UsageUtil.getOutgoingBandwidth(usage.getTotalServiceBandwidth());
    String totSvcBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalServiceBandwidth());
    BandwidthStatistics[] svcBWStats = usage.getServiceBandwidthStatistics();
    if (svcBWStats != null && svcBWStats.length > 0 && svcBWStats[0] != null) {
        for (BandwidthStatistics stat : svcBWStats) {
            String svcInBandwidth = UsageUtil.getIncomingBandwidth(stat);
            String svcOutBandwidth = UsageUtil.getOutgoingBandwidth(stat);
            String svcBandwidth = UsageUtil.getTotalBandwidth(stat);
%>
<tr>
    <td><%=stat.getKey()%>
    </td>
    <td>
        <input readonly="1" type="text" name="serviceIncomingBW" id="serviceIncomingBW"
               style="width:200px" value="<%=svcInBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="serviceOutgoingBW" id="serviceOutgoingBW"
               style="width:200px" value="<%=svcOutBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="serviceTotalBW" id="serviceTotalBW"
               style="width:200px" value="<%=svcBandwidth%>"/>
    </td>
</tr>
<%
        }
    }
%>
<tr>
    <td><fmt:message key="all.server.name"/></td>
    <td>
        <input readonly="1" type="text" name="totServiceIncomingBW" id="totServiceIncomingBW"
               style="width:200px" value="<%=totSvcInBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totServiceOutgoingBW" id="totServiceOutgoingBW"
               style="width:200px" value="<%=totSvcOutBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totServiceTotalBW" id="totServiceTotalBW"
               style="width:200px" value="<%=totSvcBandwidth%>"/>
    </td>
</tr>


<tr>
    <td colspan="4" class="middle-header"><fmt:message key="webapp.bandwidth.usage"/></td>
</tr>
<tr>
    <td><fmt:message key="server.name"/></td>
    <td><fmt:message key="incoming.bandwidth"/></td>
    <td><fmt:message key="outgoing.bandwidth"/></td>
    <td><fmt:message key="total.bandwidth"/></td>
</tr>
<%
    String totWebappInBandwidth = UsageUtil.getIncomingBandwidth(usage.getTotalWebappBandwidth());
    String totWebappOutBandwidth = UsageUtil.getOutgoingBandwidth(usage.getTotalWebappBandwidth());
    String totWebappBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalWebappBandwidth());
    BandwidthStatistics[] webappBWStats = usage.getWebappBandwidthStatistics();
    if (webappBWStats != null && webappBWStats.length > 0 && webappBWStats[0] != null) {
        for (BandwidthStatistics stat : webappBWStats) {
            String webappInBandwidth = UsageUtil.getIncomingBandwidth(stat);
            String webappOutBandwidth = UsageUtil.getOutgoingBandwidth(stat);
            String webappBandwidth = UsageUtil.getTotalBandwidth(stat);
%>
<tr>
    <td><%=stat.getKey()%>
    </td>
    <td>
        <input readonly="1" type="text" name="webappIncomingBW" id="webappIncomingBW"
               style="width:200px" value="<%=webappInBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="webappOutgoingBW" id="webappOutgoingBW"
               style="width:200px" value="<%=webappOutBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="webappTotalBW" id="webappTotalBW" style="width:200px"
               value="<%=webappBandwidth%>"/>
    </td>
</tr>
<%
        }
    }
%>
<tr>
    <td><fmt:message key="all.server.name"/></td>
    <td>
        <input readonly="1" type="text" name="totWebappIncomingBW" id="totWebappIncomingBW"
               style="width:200px" value="<%=totWebappInBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totWebappOutgoingBW" id="totWebappOutgoingBW"
               style="width:200px" value="<%=totWebappOutBandwidth%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totWebappTotalBW" id="totWebappTotalBW"
               style="width:200px" value="<%=totWebappBandwidth%>"/>
    </td>
</tr>


<tr>
    <td colspan="4" class="middle-header"><fmt:message key="service.usage.stat"/></td>
</tr>
<tr>
    <td><fmt:message key="server.name"/></td>
    <td><fmt:message key="service.usage.request"/></td>
    <td><fmt:message key="service.usage.response"/></td>
    <td><fmt:message key="service.usage.fault"/></td>
</tr>
<%
    long totSvcReqCount = usage.getTotalRequestStatistics().getRequestCount();
    long totSvcRespCount = usage.getTotalRequestStatistics().getResponseCount();
    long totSvcFaultCount = usage.getTotalRequestStatistics().getFaultCount();
    RequestStatistics[] svcStats = usage.getRequestStatistics();
    if (svcStats != null && svcStats.length > 0 && svcStats[0] != null) {
        for (RequestStatistics stat : svcStats) {
            long svcReqCount = stat.getRequestCount();
            long svcResCount = stat.getResponseCount();
            long svcFaultCount = stat.getFaultCount();
%>
<tr>
    <td><%=stat.getKey()%>
    </td>
    <td>
        <input readonly="1" type="text" name="serviceRequestCount" id="serviceRequestCount"
               style="width:200px" value="<%=svcReqCount%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="serviceResponseCount" id="serviceResponseCount"
               style="width:200px" value="<%=svcResCount%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="serviceFaultCount" id="serviceFaultCount"
               style="width:200px" value="<%=svcFaultCount%>"/>
    </td>
</tr>
<%
        }
    }
%>
<tr>
    <td><fmt:message key="all.server.name"/></td>
    <td>
        <input readonly="1" type="text" name="totServiceRequestCount" id="totServiceRequestCount"
               style="width:200px" value="<%=totSvcReqCount%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totServiceResponseCount" id="totServiceResponseCount"
               style="width:200px" value="<%=totSvcRespCount%>"/>
    </td>
    <td>
        <input readonly="1" type="text" name="totServiceFaultCount" id="totServiceFaultCount"
               style="width:200px" value="<%=totSvcFaultCount%>"/>
    </td>
</tr>


</tbody>
</table>
</td>
</tr>
</tbody>
</table>
</form>
<br/>
</div>
</div>
</fmt:bundle>

