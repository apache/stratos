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
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.PaginatedTenantUsageInfo" %>
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.TenantUsage" %>
<%@ page import="org.wso2.carbon.usage.ui.utils.UsageUtil" %>
<%@ page import="org.wso2.carbon.usage.ui.report.AllTenantUsageData" %>
<%@ page import="org.wso2.carbon.usage.ui.report.AllTenantUsageReport" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<carbon:jsi18n
        resourceBundle="org.wso2.carbon.usage.ui.i18n.JSResources"
        request="<%=request%>"/>


<fmt:bundle basename="org.wso2.carbon.usage.ui.i18n.Resources">
    <carbon:breadcrumb
            label="tenants.usage.report"
            resourceBundle="org.wso2.carbon.usage.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>
    <jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
    <script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
    <script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
    <script type="text/javascript" src="../ajax/js/prototype.js"></script>
    <script type="text/javascript" src="../tenant-usage/js/tenant-usage.js"></script>
    <script type="text/javascript" src="js/all-tenant-usage-report.js"></script>
    <link rel="stylesheet" type="text/css" href="../tenant-usage/css/tenant-usage.css"/>


    <%
        PaginatedTenantUsageInfo tenantsInfo =
                UsageUtil.retrievePaginatedTenantUsages(request, config, session);
        TenantUsage[] tenantUsages = tenantsInfo.getTenantUsages();
        int pageNumber = tenantsInfo.getPageNumber();
        int numberOfPages = tenantsInfo.getNumberOfPages();
        String yearMonth = request.getParameter("year-month");
        session.setAttribute("year-month", yearMonth);


        if (yearMonth == null) {
            // get the current year month
            yearMonth = UsageUtil.getCurrentYearMonth();
        }
        String currentYearMonth = UsageUtil.getCurrentYearMonth();
        int rowCount = 5;
        if (yearMonth.equals(currentYearMonth)) {
            rowCount = 7;
        }
    %>


    <%
        //AllTenantUsageReport usageReport = new AllTenantUsageReport(config, session, request);
        //List<AllTenantUsageData> reportDataArray = usageReport.getUsageReportData();
        //request.getSession().setAttribute("all_tenant_usage_data", reportDataArray);
    %>

    <div id="middle">
        <h2><fmt:message key="tenants.usage.report"/> for the Month - <%=yearMonth%> (All
            Tenants)</h2>
    </div>

    <div id="workArea">

        <form id="usageForm" action="all_tenant_usage.jsp" method="post">


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
                                <td colspan="2">
                                    <select onchange="this.form.submit()" name="year-month"
                                            id="year-month" style="width:400px">
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
                                    <input type="hidden" name="requestedPage" id="requestedPage"
                                           value="<%=pageNumber%>"/>
                                </td>
                            </tr>

                            <tr>
                                <td colspan="<%=rowCount%>" class="middle-header"><fmt:message
                                        key="users"/></td>
                            </tr>
                            <tr>
                                <th><fmt:message key="tenant.domain"/></th>
                                <%
                                    if (yearMonth.equals(currentYearMonth)) {
                                %>
                                <th><fmt:message key="number.of.users"/></th>
                                <th><fmt:message key="storage.usage"/></th><%}%>
                                <%--<th><fmt:message key="registry.total.bandwidth"/></th>--%>
                                <th><fmt:message key="service.total.bandwidth"/></th>
                                <th><fmt:message key="webapp.total.bandwidth"/></th>
                                <th><fmt:message key="service.total.request"/></th>
                                <th><fmt:message key="full.report"/></th>
                            </tr>
                            <%
                                if (tenantUsages != null) {
                                    for (TenantUsage usage : tenantUsages) {
                                        String currentDataStorage = UsageUtil.getTotalDataStorage(usage);
                                        String regBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalRegistryBandwidth());
                                        String svcBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalServiceBandwidth());
                                        String webBandwidth = UsageUtil.getTotalBandwidth(usage.getTotalWebappBandwidth());
                                        long svcTotalRequest = usage.getTotalRequestStatistics().getRequestCount();
                                        int numberOfUsers = usage.getNumberOfUsers();

                                        String username = (String) request.getSession().getAttribute("logged-user");
                                        String tenantName = usage.getDomain();
                                        int tenantId = usage.getTenantId();
                                        String fullReportLink = "any_tenant_usage.jsp?tenant-id=" + tenantId + "&year-month=" + yearMonth;
                            %>

                            <tr>
                                <td><%=tenantName%>
                                </td>
                                <%
                                    if (yearMonth.equals(currentYearMonth)) {
                                %>
                                <td><%=numberOfUsers%>
                                </td>
                                <td><%=currentDataStorage%>
                                </td>
                                <%
                                    }
                                %>
                                <%--<td><%=regBandwidth%>
                                </td>--%>
                                <td><%=svcBandwidth%>
                                </td>
                                <td><%=webBandwidth%>
                                </td>
                                <td><%=svcTotalRequest%>
                                </td>
                                <td><a href="<%=fullReportLink%>">Full Report</a></td>
                            </tr>
                            <%

                                }
                            } else {
                            %>
                            <tr>
                                <td><fmt:message key="empty.usage.data"/></td>
                            </tr>
                            </tr>
                            <%
                                }

                            %>
                            <carbon:resourcePaginator pageNumber="<%=pageNumber%>"
                                                      numberOfPages="<%=numberOfPages%>"
                                                      resourceBundle="org.wso2.carbon.usage.ui.i18n.Resources"
                                                      nextKey="next" prevKey="prev"
                                                      tdColSpan="6"
                                                      paginationFunction="submitAllTenantPaginatedUsage({0})"/>
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

