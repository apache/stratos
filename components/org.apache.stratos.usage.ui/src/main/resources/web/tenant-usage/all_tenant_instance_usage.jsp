<!--
  ~  Licensed to the Apache Software Foundation (ASF) under one
  ~  or more contributor license agreements.  See the NOTICE file
  ~  distributed with this work for additional information
  ~  regarding copyright ownership.  The ASF licenses this file
  ~  to you under the Apache License, Version 2.0 (the
  ~  "License"); you may not use this file except in compliance
  ~  with the License.  You may obtain a copy of the License at

  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing,
  ~  software distributed under the License is distributed on an
  ~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~  KIND, either express or implied.  See the License for the
  ~  specific language governing permissions and limitations
  ~  under the License.
  -->
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.InstanceUsageStatics" %>
<%@ page import="org.wso2.carbon.usage.stub.beans.xsd.PaginatedInstanceUsage" %>
<%@ page import="org.wso2.carbon.usage.ui.utils.UsageUtil" %>
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
        String yearMonth = request.getParameter("year-month");
        session.setAttribute("year-month", yearMonth);
        int pageNumber=0;
        int rowCount = 5;
        int numberOfPages=0;
        InstanceUsageStatics[] instanceUsages=null;
        if (yearMonth == null) {
            // get the current year month
            yearMonth = UsageUtil.getCurrentYearMonth();
        }
        try{
        PaginatedInstanceUsage instanceInfo =
                UsageUtil.retrievePaginatedInstanceUsages(request, config, session);
        instanceUsages = instanceInfo.getInstanceUsages();
        pageNumber = instanceInfo.getPageNumber();
        numberOfPages = instanceInfo.getNumberOfPages();

        String currentYearMonth = UsageUtil.getCurrentYearMonth();

        if (yearMonth.equals(currentYearMonth)) {
            rowCount = 7;
        }
        }
        catch (Exception e){
        //No need to handle here it here.Error will show in next try block
        //To avoid dead page
        }
    %>

    <div id="middle">

        <h2><fmt:message key="instance.usage.report"/> for the Month - <%=yearMonth%> (With All
            Running Instances)</h2>

        <div id="report_ui">
            <carbon:reportNew
                    component="org.wso2.carbon.usage"
                    template="all_tenant_usage_report"
                    pdfReport="true"
                    htmlReport="true"
                    excelReport="true"
                    reportDataSession="all_tenant_usage_data"
                    jsFunction="getUsageReportData()"/>
        </div>

        <div id="workArea">

            <form id="usageForm" action="all_tenant_instance_usage.jsp" method="post">


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
                                            key="instances.data"/></td>
                                </tr>
                                <tr>
                                    <th><fmt:message key="instance.id"/></th>
                                    <th><fmt:message key="server.name"/></th>
                                    <th><fmt:message key="start.time"/></th>
                                    <th><fmt:message key="stop.time"/></th>
                                    <th><fmt:message key="used.time.in.hours"/></th>
                                </tr>
                                <%                
                                    try{
                                    InstanceUsageStatics[] iu = UsageUtil.retrieveInstanceUsage(request, config, session);
                                    java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("yyyy.MM.dd 'at' hh:mm:ss a zzz");
                                    if (!iu.equals(null)) {
                                        for (InstanceUsageStatics usage : instanceUsages) {
                                            String endDate;
                                            if (usage.getRunning() == true) {
                                                endDate="Instance Still Running";
                                            }
                                            else{
                                                endDate=dateFormatter.format(usage.getStopTime().getTime());
                                            }
                                            String startDate=dateFormatter.format(usage.getStartTime().getTime());
                                            long usedHours;
                                            long usedTimeInSeconds=usage.getUsedTimeInSeconds();
                                            if(usedTimeInSeconds%3600==0){
                                                usedHours=usedTimeInSeconds/3600;
                                            }
                                            else{
                                                usedHours=(usedTimeInSeconds/3600)+1;
                                            }
                                %>
                                <tr>
                                    <td>
                                        <%=usage.getInstanceID()%>
                                    </td>
                                    <td>
                                        <%=usage.getInstanceURL().toString()%>
                                    </td>
                                    <td>
                                        <%=startDate%>
                                    </td>
                                    <td>
                                        <%=endDate%>
                                    </td>
                                    <td>

                                        <%=usedHours%>
                                    </td>
                                </tr>
                                <%

                                        }
                                    }
                                    else{
                                      %>
                                     <td><fmt:message key="empty.instance.data"/></td>
                                <%
                                 }
                                    }
                                    catch (Exception e){ %>
                                      <td><fmt:message key="failed.to.get.instance.data"/></td>
                                   <% }
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


