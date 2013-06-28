<!--
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
 -->

<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.tenant.mgt.ui.utils.TenantMgtUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<script type="text/javascript" src="../admin/js/jquery.js"></script>
<script type="text/javascript" src="../admin/js/jquery.form.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>

<%--<carbon:jsi18n--%>
		<%--resourceBundle="org.wso2.carbon.tenant.mgt.ui.i18n.JSResources"--%>
		<%--request="<%=request%>" />--%>

<div id="middle">
<%
//    First remove captcha images first stored in webapps
    String error1 = "Error in adding the tenant.";
    String error2 = "Error in updating the tenant.";
    boolean isUpdating = false;
    String tenantId = "";
    String tenantDomain = "";
    String paramvalue = "addTenant=Success";
    try {
        tenantId = request.getParameter("tenantId");
        tenantDomain = request.getParameter("domain");
        isUpdating = Boolean.parseBoolean(request.getParameter("isUpdating"));

        boolean isDomainAvailable=false;
        if(!isUpdating){
            isDomainAvailable = CommonUtil.isDomainNameAvailable(tenantDomain);
        }

    //if the request is for creating a new tenant and if the domain name is not available, show a warning

    if(tenantId == null && !isDomainAvailable){

%>
    <script type="text/javascript">
        jQuery(document).ready(function() {
             CARBON.showErrorDialog("Sorry!. The Domain '<%=tenantDomain%>'is already registered. Please choose a different domain.");
        });
    </script>

<%
    }else if (tenantId == null && isDomainAvailable) {

     // if the request is for creating a new tenant and if th domain name is available. add the tenant
            TenantMgtUtil.addTenantConfigBean(request,config,session);
              response.sendRedirect("../tenant-mgt/view_tenants.jsp?region=region3&item=govern_view_tenants_menu&"+paramvalue);
     }else if (tenantId !=null){
     //if the tenant id is given, it is a request to update.hence update the tenant info
            TenantMgtUtil.updateTenantConfigBean(request,config,session);
            isUpdating = true;
            paramvalue = "addTenant=SuccessUpdate";
          response.sendRedirect("../tenant-mgt/view_tenants.jsp?region=region3&item=govern_view_tenants_menu&"+paramvalue);
        }

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
