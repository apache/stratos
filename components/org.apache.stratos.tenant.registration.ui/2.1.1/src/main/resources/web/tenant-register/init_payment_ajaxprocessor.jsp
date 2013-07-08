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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.register.ui.clients.PackageInfoServiceClient" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="org.json.JSONException" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<carbon:jsi18n
		resourceBundle="org.wso2.carbon.register.ui.i18n.JSResources"
		request="<%=request%>" />
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/register_config.js"></script>
<script type="text/javascript" src="../admin/js/jquery.js"></script>
<script type="text/javascript" src="../admin/js/jquery.form.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
<%
    String tenantDomain = request.getParameter("domain");
    // The actual usage plan the tenant selects in select_usage_plan.jsp
    String selectedUsagePlan = request.getParameter("selectedUsagePlan");
    session.setAttribute("selectedUsagePlan", selectedUsagePlan);
    
    String backendServerUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    ConfigurationContext configContext =
            (ConfigurationContext)config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    PackageInfoServiceClient client = new PackageInfoServiceClient(cookie, backendServerUrl,configContext);
    JSONArray packageInfoArray = client.getBillingPackagesJsonArray();
    String amount = "0";

    for (int i = 0; i < packageInfoArray.length(); i++) {
        try {
            if (packageInfoArray.getJSONObject(i).getString("name").equals(selectedUsagePlan)) {
                amount = packageInfoArray.getJSONObject(i).getString("subscriptionCharge");
            }
        } catch (JSONException e) {
            //
        }
    }

    String successUrl;
    String paypalUrl = CommonUtil.getStratosConfig().getPaypalUrl();
    String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
    adminConsoleURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("carbon"));
     successUrl = adminConsoleURL + "carbon/payment/registration_payment.jsp";
    String cancelUrl = adminConsoleURL + "carbon/admin/login.jsp";
%>

<script type="text/javascript">


        var successUrl = '<%=successUrl%>';
        var cancelUrl = '<%=cancelUrl%>';
        var amount = '<%=amount%>';
        var tenantDomain = '<%=tenantDomain%>';
        jQuery.ajax({
            type: 'GET',
            url: '../payment/setEC-ajaxprocessor.jsp',
            data: 'successUrl=' + successUrl + '&cancelUrl=' + cancelUrl + '&amount=' + amount + '&tenantDomain=' + tenantDomain,
            dataType: 'json',
            async: false,
            success: function(msg) {
                var resp = msg;
                if(resp.ack=='Success'){
                    location.href = '<%=paypalUrl%>' + resp.token;
                }else{
                    location.href = cancelUrl;
                }
            },
            error:function () {
                location.href = cancelUrl;
            }
        });
</script>
