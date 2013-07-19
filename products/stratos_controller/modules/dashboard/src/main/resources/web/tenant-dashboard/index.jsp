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
<%@ page import="org.wso2.stratos.manager.dashboard.ui.clients.CloudManagerServiceClient" %>
<%@ page import="org.wso2.carbon.stratos.manager.dashboard.stub.xsd.CloudService" %>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page import="org.wso2.stratos.manager.dashboard.ui.utils.Util" %>

<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../tenant-dashboard/js/dashboard.js"></script>
<link href="../tenant-dashboard/css/dashboard.css" rel="stylesheet" type="text/css" media="all"/>
<script type="text/javascript" src="../tenant-dashboard/js/configurations.js"></script>



<%

    CloudManagerServiceClient cloudManagerClient;
    CloudService[] cloudServices = null;
    String error = "Error in getting the available and active service details.";
    try {
        cloudManagerClient = new CloudManagerServiceClient(request, config, session);
        cloudServices = cloudManagerClient.retrieveCloudServiceInfo();
    } catch (Exception e) {
        request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error, CarbonUIMessage.ERROR, e));
%>
<jsp:include page="../admin/error.jsp"/>
<%
        return;
    }
%>
<fmt:bundle basename="org.wso2.stratos.manager.dashboard.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.stratos.manager.dashboard.ui.i18n.JSResources"
            request="<%=request%>" namespace="org.wso2.stratos.manager.dashboard.ui"/>

    <div id="middle">
        <h2><fmt:message key="cloud.services"/></h2>
        <%
            try {
                // if the account management permission there, we are showing the message to validate email address
                if (CarbonUIUtil.isUserAuthorized(request, "/permission/admin/configure/account")) {
                    try {
                        AccountMgtClient client = new AccountMgtClient(config, session);
                        boolean isEmailValidated = client.isEmailValidated();

                        if (!isEmailValidated) {
        %>

        <div class="green-note">Your organization has not validated the contact email address
            yet. Please validate it following
            the instructions in the <a href="../account-mgt/account_mgt.jsp">Account
                Management</a> page (Accessible from Configure-&gt;Account menu).
        </div>
        <%
            }else{
			%>
			<div class="green-note"></div>
			<%
			}
        } catch (Exception e) {
            request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error, error, null));
        %>
        <jsp:include page="../admin/error.jsp?<%=error%>"/>
        <%
                    return;
                }
            }
            String tenantDomain;
            if (session.getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
                tenantDomain = (String) session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
            } else {
                // user is not logged in or just logged out, but still they are inside url own to the domain
                tenantDomain = (String) request.getAttribute(MultitenantConstants.TENANT_DOMAIN);
            }
            String linkSuffix = "";
            if (tenantDomain != null) {
                linkSuffix = "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + tenantDomain;
            }
            %> <script type="text/javascript">
            linkSuffix = '<%=linkSuffix%>';
           </script> <%
	  
            for (CloudService cloudService : cloudServices) {
                String name = cloudService.getName();
                String projectPageUrl = cloudService.getProductPageURL();
                String link = cloudService.getLink() + linkSuffix;
                if (name.equals("WSO2 Stratos Application Server")) {
        %>
        <script type="text/javascript">
            asUrl = '<%=link%>';
        </script>
        <%
        } else if (name.equals("WSO2 Stratos Enterprise Service Bus")) {

        %>
        <script type="text/javascript">
            esbUrl = '<%=link%>';
        </script>
        <%

        } else if (name.equals("WSO2 Stratos Data Services Server")) {
        %>
        <script type="text/javascript">
            dssUrl = '<%=link%>';

        </script>
        <%

        } else if (name.equals("WSO2 Stratos Governance")) {

        %>
        <script type="text/javascript">
            gregUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Identity")) {
        %>
        <script type="text/javascript">
            isUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Business Process Server")) {
        %>
        <script type="text/javascript">
            bpsUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Business Rules Server")) {
        %>
        <script type="text/javascript">
            brsUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Mashup Server")) {
        %>
        <script type="text/javascript">
            msUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Gadget Server")) {
        %>
        <script type="text/javascript">
            gsUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Complex Event Processing Server")) {
        %>
        <script type="text/javascript">
            cepUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Message Broker")) {
        %>
        <script type="text/javascript">
            mbUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Business Activity Monitor")) {
        %>
        <script type="text/javascript">
            bamUrl = '<%=link%>';

        </script>
        <%
        } else if (name.equals("WSO2 Stratos Cloud Gateway")) {
        %>
        <script type="text/javascript">
            csgUrl = '<%=link%>';

        </script>
        <%
        } else {
        %>
        <script type="text/javascript">
            managerUrl = '<%=link%>';
        </script>
        <%
                }
            }
        } catch (Exception e) {
            request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error, error, null));
        %>
        <jsp:include page="../admin/error.jsp?<%=error%>"/>
        <%
                return;
            }

        %>

        <div id="workArea">


            <div id="dashboard">

            </div>

            <script type="text/javascript">

                jQuery(document).ready(function() {
                    jQuery('#dashboard').load('../../../../features-dashboard/tenant-dashboard/index.jsp');
                });

                var menuPanel = document.getElementById('menu-panel');
                var menuSlider0 = document.getElementById('menu-panel-button0');
                jQuery(menuPanel).hide();
                jQuery(menuSlider0).removeClass('showToHidden');
                jQuery(menuSlider0).addClass('hiddenToShow');
                document.cookie = "menuPanel=none;path=/;expires=" + cookie_date.toGMTString();
            </script>
        </div>
    </div>
</fmt:bundle>
