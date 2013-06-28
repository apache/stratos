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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.tenant.mgt.stub.beans.xsd.PaginatedTenantInfoBean" %>
<%@ page import="org.wso2.carbon.tenant.mgt.stub.beans.xsd.TenantInfoBean" %>
<%@ page import="org.wso2.carbon.tenant.mgt.ui.clients.TenantServiceClient" %>
<%@ page import="org.wso2.carbon.tenant.mgt.ui.utils.TenantMgtUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/tenant_config.js"></script>


<%
    if ("Success".equals(request.getParameter("addTenant"))) {
%>
<script type="text/javascript">showSuccessRegisterMessage()</script>
<%
    }
    if ("SuccessUpdate".equals(request.getParameter("addTenant"))) {
%>
<script type="text/javascript">showSuccessUpdateMessage()</script>
<%
    }

%>
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.tenant.mgt.ui.i18n.JSResources"
        request="<%=request%>"/>

<fmt:bundle basename="org.wso2.carbon.tenant.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="govern.view_tenants.menu"
            resourceBundle="org.wso2.carbon.tenant.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <div id="top">
        <form id="findTenantForm" action="view_tenants.jsp?action=search" method="post">
            <table class="normal-nopadding" cellspacing="0">
                <tbody>
                <tr>
                    <td><fmt:message key="enter.tenant.domain"/></td>
                    <td colspan="2"><input type="text" name="domain" id="domain"
                                           style="width:400px"/>
                        <input type="button" onclick="domainSelected()" value="Find"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </form>
    </div>

    <br/>

    <div id="middle">

        <%
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String activatingDomain = request.getParameter("activate.domain");
            String action = request.getParameter("action");
            String domainName = request.getParameter("domain");
            if (activatingDomain != null) {
                // try to activate deactive the tenant
                String activate = request.getParameter("activate");
                try {
                    if (activate != null && activate.equalsIgnoreCase("on")) {
                        TenantMgtUtil.activateTenant(request, config, session);
                    } else {
                        TenantMgtUtil.deactivateTenant(request, config, session);
                    }
                } catch (Exception e) {
                    String error1 = "Error in activating/deactivating tenant";
                    request.setAttribute(CarbonUIMessage.ID,
                            new CarbonUIMessage(error1, error1, null));

        %>

        <jsp:forward page="../admin/error.jsp?<%=error1%>"/>

        <%
                    return;
                }
            }

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
            int numberOfPages;
            int noOfPageLinksToDisplay = 5;  //default value is set to 5

            String backendServerURL = CarbonUIUtil.getServerURL(
                    config.getServletContext(), session);
            ConfigurationContext configContext = (ConfigurationContext) config
                    .getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

            String cookie = (String) session.getAttribute
                    (ServerConstants.ADMIN_SERVICE_COOKIE);
            TenantServiceClient client;
            PaginatedTenantInfoBean tenantsInfo;
            TenantInfoBean[] tenantInfoArr;
            try {
                client = new TenantServiceClient(cookie, backendServerURL, configContext);
                if (action != null && action.equals("search")) {
                	   tenantsInfo = client.retrievePaginatedPartialSearchTenants(domainName,pageNumber);
                } else {
                	   tenantsInfo = client.retrievePaginatedTenants(pageNumber);
                }
             

                tenantInfoArr = tenantsInfo.getTenantInfoBeans();
                numberOfPages = tenantsInfo.getNumberOfPages();

            } catch (Exception e) {
                String error1 = "Error in retrieving tenants";
                request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error1, error1, null));
        %>

        <jsp:forward page="../admin/error.jsp"/>

        <%
                return;
            }
        %>

        <h2><fmt:message key="tenants.list"/></h2>
        <br/>
        <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                          noOfPageLinksToDisplay="<%=noOfPageLinksToDisplay%>"
                          page="view_tenants.jsp" pageNumberParameterName="pageNumber"/>

        <div id="workArea">


                <%
                    if (tenantInfoArr != null && tenantInfoArr.length>0 && tenantInfoArr[0]!=null) {
                %>

                <table cellpadding="0" cellspacing="0" border="0" style="width:100%" class="styledLeft">
                    <thead>
                    <tr>
                        <th style="padding-left:5px;text-align:left;"><fmt:message key="domain"/></th>
                        <th style="padding-left:5px;text-align:left;"><fmt:message
                                key="admin.email"/></th>
                        <th style="padding-left:5px;text-align:left;"><fmt:message
                                key="created.date"/></th>
                        <th style="padding-left:5px;text-align:left;"><fmt:message key="active"/></th>
                        <th style="padding-left:5px;text-align:left;"><fmt:message key="edit"/></th>
                    </tr>
                    </thead>
                    <tbody>

                <%
                        for (TenantInfoBean tenantInfo : tenantInfoArr) {
                            if (tenantInfo == null) {
                                continue;
                            }
                            String tenantDomain = TenantMgtUtil.removeHtmlElements(
                                    tenantInfo.getTenantDomain());
                            String email = TenantMgtUtil.removeHtmlElements(tenantInfo.getEmail());
                            boolean isActive = tenantInfo.getActive();
                            Calendar createdDateCal = tenantInfo.getCreatedDate();
                            Date createdDate = new Date(createdDateCal.getTimeInMillis());
                            String createdDateStr = dateFormat.format(createdDate);
                %>

                <tr id="1">
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=tenantDomain%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=email%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=createdDateStr%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;">
                        <form id="<%=tenantDomain%>_form" action="view_tenants.jsp" method="post">
                            <input type="checkbox" name="activate"
                                   onchange="javascript:activationChanged(this, '<%=tenantDomain%>')"
                                   <%if (isActive) {%>checked="true"<%}%>/>
                            <input type="hidden" name="activate.domain" value="<%=tenantDomain%>"/>
                        </form>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><a
                            href="add_tenant.jsp?domain=<%=tenantInfo.getTenantDomain()%>">Edit</a>
                    </td>
                </tr>
                <% }
                %>
                    </tbody>
                </table>
                <%
                }else{
                %>
            <div><fmt:message key="no.tenants.available"/></div>
            <%
                }
            %>

        </div>
                <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                                  noOfPageLinksToDisplay="<%=noOfPageLinksToDisplay%>"
                                  page="view_tenants.jsp" pageNumberParameterName="pageNumber"/>
    </div>
</fmt:bundle>