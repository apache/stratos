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
<%@ page import="org.wso2.stratos.manager.dashboard.ui.clients.CloudManagerServiceClient" %>
<%@ page import="org.wso2.carbon.stratos.manager.dashboard.stub.xsd.CloudService" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page import="org.wso2.carbon.stratos.common.constants.StratosConstants" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.wso2.stratos.manager.dashboard.ui.utils.Util" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>


<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="../tenant-dashboard/js/dashboard.js"></script>
<link href="../tenant-dashboard/css/dashboard.css" rel="stylesheet" type="text/css" media="all"/>


<%

    CloudManagerServiceClient cloudManagerClient;
    CloudService[] cloudServices = null;
    String error = "Error in getting the available and active service details.";
    try {
        cloudManagerClient = new CloudManagerServiceClient(request, config, session);
        cloudServices = cloudManagerClient.retrieveCloudServiceInfo();
    } catch (Exception e) {
        request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error,error,null)); 
%>
        <jsp:include page="../admin/error.jsp?<%=error%>"/>
<%
        return;
    }
%>
<fmt:bundle basename="org.wso2.stratos.manager.dashboard.ui.i18n.Resources">
<carbon:jsi18n
        resourceBundle="org.wso2.stratos.manager.dashboard.ui.i18n.JSResources"
        request="<%=request%>" namespace="org.wso2.stratos.manager.dashboard.ui"/>

<div id="middle">
<h2><fmt:message key="activate.your.cloud.services"/></h2>
<div id="workArea">
<%
// if the account management permission there, we are showing the message to validate email address
if (CarbonUIUtil.isUserAuthorized(request, "/permission/admin/configure/account")) {

    AccountMgtClient client = new AccountMgtClient(config, session);
    boolean isEmailValidated = client.isEmailValidated();

    if (!isEmailValidated) {
%>

<div class="green-note">Your organization has not validated the contact email address yet. Please validate it following
the instructions in the <a href="../account-mgt/account_mgt.jsp">Account Management</a> page (Accessible from Configure-&gt;Account menu). </div>
<%
    }
}
String tenantDomain = null;
if (session.getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
    tenantDomain = (String)session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
} else {
    // user is not logged in or just logged out, but still they are inside url own to the domain
    tenantDomain = (String)request.getAttribute(MultitenantConstants.TENANT_DOMAIN);
}
String linkSuffix = "";
    if (tenantDomain != null) {
        linkSuffix = "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + tenantDomain;
    }

boolean isUserAuthorized = CarbonUIUtil.isUserAuthorized(request, "/permission/admin/configure/cloud-services");    
%>
<form id="cloudService" action="cloud_services_save_ajaxprocessor.jsp" method="post">
<div>
    <table cellspacing="0" class="tips">
<%
    List<String> activeServices = new ArrayList<String>();
    int index = 1;

    for (CloudService cloudService: cloudServices) {
        index++;
        String name = cloudService.getName();       
        String imagePath = "images/" + name.replace(" ", "-") + "-logo.gif";
        String projectPageUrl = cloudService.getProductPageURL();
        String link = cloudService.getLink() + linkSuffix;
        String icon = cloudService.getIcon();
        String iconPath = icon.substring(0, icon.lastIndexOf('/'));
        String iconName = icon.substring(icon.lastIndexOf('/') + 1);
        String inactiveIcon = iconPath + "/" + "inactive-" + iconName;
        String description = cloudService.getDescription();
        String label = cloudService.getLabel();
        boolean active = name.equals(StratosConstants.CLOUD_IDENTITY_SERVICE) ||
                name.equals(StratosConstants.CLOUD_GOVERNANCE_SERVICE) ||
                cloudService.getActive();
        String disabledStr = (name.equals(StratosConstants.CLOUD_IDENTITY_SERVICE) ||
                name.equals(StratosConstants.CLOUD_GOVERNANCE_SERVICE))?" disabled ": "";
        boolean disabledBtn = (name.equals(StratosConstants.CLOUD_IDENTITY_SERVICE) ||
                name.equals(StratosConstants.CLOUD_GOVERNANCE_SERVICE));
        if (!isUserAuthorized) {
            disabledStr = " disabled ";
        }
        /*if (name.equals(StratosConstants.CLOUD_ESB_SERVICE)) {
            active = false;
            disabledStr = " disabled "; // we show an inactive and disabled link
        }*/
        if (active) {
            activeServices.add(label);
        }
%>
            <tr class="tips-row">

            <td class="tip-checkbox">
                <input type="checkbox" <%=disabledStr%> <%=active ? " checked=\"true\"" : ""%>
                       name="cloudServices" id="cloudServices<%=index%>"  value="<%=name%>" style="display:none;"/>
            </td>

            <td>
            	<img src="<%=icon%>" class="iconImage">
                <% if (active) { %>
            	<a target="_blank" href="<%=link%>" class="linkToService"><%=label%></a>
                 <% } else { %>
                <a class="linkToService"><%=label%></a>
                <% } %>

            </td>
	<td class="access">
        <% if (!disabledBtn) { %>          
                <% if (active) { %>
                <a target="_blank" input type="button" onclick="document.getElementById('cloudServices<%=index%>').checked=false;onChangeServiceSubscription()" class="deactivate-button"> Deactivate
                </a>
                <% } else { %>
                <a target="_blank" input type="button" onclick="document.getElementById('cloudServices<%=index%>').checked=true;onChangeServiceSubscription()" class="activate-button"> Activate
                <% } %>
        <% } %>
    </td>
                
	<td class="powered">Powered by<br/><a href="<%=projectPageUrl%>" target="_blank"><img src="<%=imagePath%>"/></a></td>
                 </tr>
               <% index++;
            } %>

                <td>&nbsp;</td>
                </tr>
    </table>
    </div>
</form>
<%
    List<String> oldActiveServices = (List<String>)session.getAttribute("active-statos-services");
    if (oldActiveServices != null) {
        List<String> newlyActivatedServices = Util.getNewlyActivatedServices(oldActiveServices, activeServices);
        List<String> newlyDeactivatedServices = Util.getNewlyDeactivatedServices(oldActiveServices, activeServices);
        String msg = null;
        if (newlyActivatedServices.size() > 0) {
            msg = "<li>";
            for (String service: newlyActivatedServices) {
                msg += "<ul>" + service + " <strong>activated</strong></ul>";
            }
            msg += "</li>";
        }
        if (newlyDeactivatedServices.size() > 0) {
            if (msg == null) {
                msg = "<li>";
            }
            for (String service: newlyDeactivatedServices) {
                msg += "<ul>" + service + " <strong>deactivated</strong></ul>";
            }
            msg += "</li>";
        }
        if (msg != null) {
%>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showInfoDialog('<%=msg%>');
        });
    </script>
<%
        }
    }
    session.setAttribute("active-statos-services", activeServices);
%>
</div>
</div>
</fmt:bundle>
