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
<%@page import="org.wso2.carbon.gapp.registration.ui.GAppRegistrationUIConstants"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
<%@page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.wso2.carbon.gapp.registration.ui.GoolgeAppsRegistrationClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>

<%@page import="org.wso2.carbon.ui.util.CharacterEncoder"%><script type="text/javascript" src="../userstore/extensions/js/vui.js"></script>

<%
    String domain = (String)session.getAttribute("domain");
    String callback = (String)session.getAttribute("callback"); 
%>  
<script type="text/javascript" src="../admin/js/main.js"></script>
<link href="../admin/css/global.css" rel="stylesheet" type="text/css" media="all"/>
<link href="../admin/css/main.css" rel="stylesheet" type="text/css" media="all"/>
<link href="../tenant-register/css/tenant-register.css" rel="stylesheet" type="text/css" media="all"/>


    <link href="../dialog/css/jqueryui/jqueryui-themeroller.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="../dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>
    <link rel="icon" href="../admin/images/favicon.ico" type="image/x-icon"/>
    <link rel="shortcut icon" href="../admin/images/favicon.ico" type="image/x-icon"/>

    <script type="text/javascript" src="../admin/js/jquery.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.form.js"></script>
    <script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
    <script type="text/javascript" src="../dialog/js/dialog.js"></script>

    <script type="text/javascript" src="../admin/js/main.js"></script>
    <script type="text/javascript" src="../admin/js/WSRequest.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
    <script src="../admin/js/widgets.js" type="text/javascript"></script> 
<!--[if gte IE 8]>
<link href="../dialog/css/dialog-ie8.css" rel="stylesheet" type="text/css" media="all"/>
<![endif]-->
<!--[if gte IE 7]>
<link href="../dialog/css/dialog-ie8.css" rel="stylesheet" type="text/css" media="all"/>
<![endif]-->
<style type="text/css">
  .header-links{
      display:none !important;
  }
  body, span, td, div{
    font-size:12px;
  }
</style>
</head>
<body>
<div id="dcontainer"></div>
<script type="text/javascript">
    function forward() {
        location.href = "<%=callback%>";
    }
</script>
<%
    Object isAllowed = session.getAttribute(GAppRegistrationUIConstants.ALLOWED);
    boolean value = (Boolean)isAllowed;
    if (!value) {
		%>
	<script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showInfoDialog("Illegal access atempt!");
        });
    </script>	
		<%	
		return;
    }
    
    boolean isSuccess = true;
	try {
        String subscription = (String)request.getParameter("usage-plan-name");
        String firstName = (String)request.getParameter("admin-firstname");
        String lastName = (String)request.getParameter("admin-lastname");
        String email = (String)request.getParameter("admin-email");
        String[] users = new String[0];
    	String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                    (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);       
        GoolgeAppsRegistrationClient client = new GoolgeAppsRegistrationClient();
        client.registerTenantForGAppDomain(backendServerURL, configContext, domain, email, firstName, lastName, subscription);
    } catch (Exception e) {
        isSuccess = false;
    }
    
    if (isSuccess) {%>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showInfoDialog("Stratos setup was successful! You are being redirected back to Google", forward, forward);
        });
        
    </script>
<%} else {%>
    <script type="text/javascript">
    jQuery(document).ready(function() {
        CARBON.showErrorDialog("Stratos setup was unsuccessful! You are being redirected back to Google", forward, forward);
    });
    </script>  	
<%}%>
<table id="main-table" border="0" cellspacing="0">
      <tr>
          <td id="header"><jsp:include page="../admin/layout/header.jsp" />
          </td>
      </tr>
      <tr>


          <td id="middle-content">
              <table id="content-table" border="0" cellspacing="0">

                  <tr>
                      <td id="body">
				<fmt:bundle basename="org.wso2.carbon.gapp.registration.ui.i18n.Resources">
				   
				    <script type="text/javascript">
				
				       
				    </script>
				    <div id="middle">
				        <div id="workArea">
				        <%if (isSuccess) {%>
				            <p>Setting up Stratos was successful. You are being redirected back to <%=callback%></p>
				        <% } else { %>
				        	<p>Setting up Stratos was unsuccessful. You are being redirected back to <%=callback%></p>
				        <% } %>            
				    </div>
				    </div>				
				</fmt:bundle>
				  </td>
                  </tr>
              </table>
          </td>
      </tr>
      <tr>
          <td id="footer"<jsp:include page="../admin/layout/footer.jsp" /></td>
      </tr>
  </table>   
</body>
</html>