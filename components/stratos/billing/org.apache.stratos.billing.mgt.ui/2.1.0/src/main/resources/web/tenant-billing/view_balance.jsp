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
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.PaginatedBalanceInfoBean" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.OutstandingBalanceInfoBean" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>

<carbon:jsi18n
        resourceBundle="org.wso2.carbon.billing.mgt.ui.i18n.JSResources"
        request="<%=request%>"/>

<fmt:bundle basename="org.wso2.carbon.billing.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="view.balance.menu"
            resourceBundle="org.wso2.carbon.billing.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <div id="top">
        <form id="findTenantForm" action="view_balance.jsp" method="post">
            <table class="normal-nopadding" cellspacing="0">
                <tbody>
                <tr>
                    <td><fmt:message key="enter.tenant.domain"/></td>
                    <td colspan="2"><input type="text" name="tenantDomain" id="tenantDomain"
                                           style="width:300px"/>
                        <input type="button" onclick="javascript:document.getElementById
                    ('findTenantForm').submit();" value="Find"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </form>
    </div>

    <div id="middle">

        <%
            String tenantDomain = request.getParameter("tenantDomain");
            String pageNumberStr = request.getParameter("pageNumber");
            if (pageNumberStr == null) {
                pageNumberStr = "0";
            }
            int pageNumber = 0;
            try {
                pageNumber = Integer.parseInt(pageNumberStr);
            } catch (NumberFormatException ignored) {
                // page number format exception
            }
            int numberOfPages = 0;
            int noOfPageLinksToDisplay = 5;  //default value is set to 5

            PaginatedBalanceInfoBean paginatedBalance;
            OutstandingBalanceInfoBean[] balanceInfoArr;
            try {
                if (tenantDomain == null || "".equals(tenantDomain.trim())) {
                    paginatedBalance = BillingUtil.getPaginatedBalanceInfo(config, session, pageNumber);
                    balanceInfoArr = paginatedBalance.getBalanceInfoBeans();
                    numberOfPages = paginatedBalance.getNumberOfPages();
                } else {
                    balanceInfoArr = BillingUtil.getOutstandingBalance(config, session, tenantDomain);
                }
            } catch (UIException e) {
                String error1 = "Error in viewing balance of customers: " + e.getMessage();
                request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error1, error1, null));
        %>

        <jsp:forward page="../admin/error.jsp"/>
        <%
                return;
            }
        %>
        <br/>
        <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                          noOfPageLinksToDisplay="<%=noOfPageLinksToDisplay%>"
                          page="view_balance.jsp" pageNumberParameterName="pageNumber"/>
        <br/>

        <h2><fmt:message key="invoice.summary"/></h2>

        <div id="workArea">

            <table cellpadding="0" cellspacing="0" border="0" style="width:100%" class="styledLeft">
                <thead>
                <tr>
                    <th style="padding-left:5px;text-align:left;">Domain</th>
                    <th style="padding-left:5px;text-align:left;">Active Usage Plan</th>
                    <th style="padding-left:5px;text-align:left;">Balance at Last Invoice</th>
                    <th style="padding-left:5px;text-align:left;">Last Invoice Date</th>
                    <th style="padding-left:5px;text-align:left;">Last Payment Date</th>
                    <th style="padding-left:5px;text-align:left;">Last Paid Amount</th>
                </tr>
                </thead>
                <tbody>
                <%
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
                    if (balanceInfoArr != null && balanceInfoArr.length > 0) {
                        for (OutstandingBalanceInfoBean balanceInfo : balanceInfoArr) {
                            if (balanceInfo == null) {
                                continue;
                            }
                            String carriedForwardBalance = "Not Available";
                            String lastInvoiceDate = "Not Available";
                            String lastPaymentDate = "Not Available";
                            String lastPaidAmount = "Not Available";
                            if (balanceInfo.getCarriedForward() != null) {
                                carriedForwardBalance = balanceInfo.getCarriedForward();
                            }
                            if (balanceInfo.getLastInvoiceDate() != null) {
                                lastInvoiceDate = dateFormat.format(balanceInfo.getLastInvoiceDate());
                            }
                            if (balanceInfo.getLastPaymentDate() != null) {
                                lastPaymentDate = dateFormat.format(balanceInfo.getLastPaymentDate());
                            }
                            if (balanceInfo.getLastPaidAmount() != null) {
                                lastPaidAmount = balanceInfo.getLastPaidAmount();
                            }
                %>
                <tr>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=balanceInfo.getCustomerName()%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=balanceInfo.getSubscription()%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=carriedForwardBalance%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=lastInvoiceDate%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=lastPaymentDate%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=lastPaidAmount%>
                    </td>
                </tr>
                <% }
                } else {
                %>
                <tr>
                    <td colspan="6" style="padding-left:5px;padding-top:3px;text-align:center;">
                        Could not find information for domain <%=tenantDomain%>
                    </td>
                </tr>
                <%
                    }
                %>
                <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                                  noOfPageLinksToDisplay="<%=noOfPageLinksToDisplay%>"
                                  page="view_balance.jsp" pageNumberParameterName="pageNumber"/>
                </tbody>
            </table>
        </div>
    </div>
</fmt:bundle>
