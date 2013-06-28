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
<%@page import="com.google.gdata.data.appsforyourdomain.provisioning.UserEntry"%>
<%@page import="com.google.gdata.client.appsforyourdomain.UserService"%>
<%@page import="java.net.URL"%>
<%@page import="com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer"%>
<%@page import="com.google.gdata.client.authn.oauth.GoogleOAuthParameters"%>
<html>
<head>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDConsumer"%>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest"%>
<%@page import="org.wso2.carbon.gapp.registration.ui.GAppRegistrationUIConstants"%>
<%@page import="org.wso2.carbon.stratos.common.packages.stub.PackageInfo"%>
<%@page import="org.json.JSONArray"%>
<%@page import="org.wso2.carbon.common.util.CommonUtil"%>
<%@page import="org.wso2.carbon.utils.ServerConstants"%>
<%@page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@page import="org.wso2.carbon.gapp.registration.ui.GoolgeAppsRegistrationClient" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@page import="com.google.step2.Step2.AxSchema"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%
    String domain = request.getParameter("domain");
    String callback = request.getParameter("callback");
    String service = request.getParameter("service");

    PackageInfo[] packageInfo = new PackageInfo[0];

    if (service != null && service.trim().length() > 0) {
        session.setAttribute(GAppRegistrationUIConstants.ALLOWED, null);
        session.setAttribute("service", service.trim());
    } else {
        service = (String) session.getAttribute("service");
    }

    if (domain != null && domain.trim().length() > 0) {
        session.setAttribute(GAppRegistrationUIConstants.ALLOWED, null);
        session.setAttribute("domain", domain.trim());
    } else {
        domain = (String) session.getAttribute("domain");
    }

    if (callback != null && callback.trim().length() > 0) {
        session.setAttribute(GAppRegistrationUIConstants.ALLOWED, null);
        session.setAttribute("callback", callback);
    } else {
        callback = (String) session.getAttribute("callback");
    }
%>
<script type="text/javascript" src="../admin/js/main.js"></script>

<link href="../dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>
<link href="../admin/css/global.css" rel="stylesheet" type="text/css" media="all"/>
<link href="../admin/css/main.css" rel="stylesheet" type="text/css" media="all"/>
<link href="../tenant-register/css/tenant-register.css" rel="stylesheet" type="text/css" media="all"/>
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
    <script type="text/javascript">
function validateGAppSetupInfo() {
    var error = validateEmpty('admin-email');
    if (error) {
        CARBON.showWarningDialog("Email address cannot be empty.");
        return false;
    }
    
    var fld = document.getElementById('admin-email');
    var value = fld.value;
    if (value.indexOf("@") > -1 ) {
    	var emaildomain = value.substring(value.indexOf("@")+1, value.length);
    	if (emaildomain == "<%=domain%>") {
			//do nothing
    	} else {
    		CARBON.showWarningDialog("Admin's email address must be in the same domain.");
            return false;
    	}
    } else {
    	CARBON.showWarningDialog("Invalid email address.");
        return false;
    }
    
    var error = validateEmpty('admin-firstname');
    if (error) {
        CARBON.showWarningDialog("First name cannot be empty.");
        return false; 
    }
    var error = validateEmpty('admin-lastname');
    if (error) {
        CARBON.showWarningDialog("Last name cannot be empty.");
        return false;
    }
    document.gappsetupForm.submit();
};
</script>
</head>
<body>
<div id="dcontainer"></div>
<script type="text/javascript">
    function forward() {
        location.href = "<%=callback%>";
    }
</script>
<%
    try {
        Object isAllowed = session.getAttribute(GAppRegistrationUIConstants.ALLOWED);
        if (isAllowed == null) {
            //this is the first call 
            ConfigurationContext configContext =
                                                 (ConfigurationContext) config.getServletContext()
                                                                              .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            GoolgeAppsRegistrationClient client = new GoolgeAppsRegistrationClient();
            
            if (domain == null) {
                throw new Exception("Cannot proceed since domain is invalid"); 
            }
            
            boolean isRegistered =
                                   client.isRegisteredAsGoogleAppDomain(configContext, domain);
            if (isRegistered) {
%>
		     	<script type="text/javascript">
			        jQuery(document).ready(function() {
			            CARBON.showInfoDialog("Strato domain has been setup for the domain " + <%=domain%>, forward, forward);
			        });
                </script>	
		     	<%
			     	    return;
			     	            }

			     	            OpenIDAuthenticationRequest openIDAuthRequest =
			     	                                                            new OpenIDAuthenticationRequest(
			     	                                                                                            request,
			     	                                                                                            response);
			     	            openIDAuthRequest.setOpenIDUrl(domain.trim());
			     	            String returnUrl =
			     	                               OpenIDConsumer.getInstance().getAdminConsoleURL(request) +
			     	                                       "gappregistration/openidaccept_ajaxprocessor.jsp";
			     	            openIDAuthRequest.setReturnUrl(returnUrl);
			     	            openIDAuthRequest.addRequiredClaims(AxSchema.EMAIL.getUri());
			     	            openIDAuthRequest.setRequestClaimsFromIdP(true);
			     	            String forwardTo =
			     	                               OpenIDConsumer.getInstance()
			     	                                             .doOpenIDAuthentication(openIDAuthRequest);
			     	            response.sendRedirect(forwardTo);
			     	        } else {
			     	            boolean value = (Boolean) isAllowed;
			     	            if (!value) {
			     	                CarbonUIMessage carbonMessage =
			     	                                                (CarbonUIMessage) session.getAttribute(CarbonUIMessage.ID);
			     	%>
		     	<script type="text/javascript">
			        jQuery(document).ready(function() {
			            CARBON.showErrorDialog("<%=carbonMessage.getMessage()%>", forward, forward);
			        });
                </script>	
		     	<%
			     	    return;
			     	            }
			     	        }

			     	        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
			     	        ConfigurationContext configContext =
			     	                                             (ConfigurationContext) config.getServletContext()
			     	                                                                          .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
			     	        GoolgeAppsRegistrationClient client = new GoolgeAppsRegistrationClient();
			     	        packageInfo = client.getPackageInfo(serverURL, configContext);
			     	    } catch (Exception e) {
			     	%>
     	<script type="text/javascript">
	        jQuery(document).ready(function() {
	            CARBON.showErrorDialog("Unable to setup Stratos. <%=e.getMessage()%> ", forward, forward);
	        });
        </script>	
     	<%
	     	    return;
	     	    }
	     	%>

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
				    <div id="middle">
				        <h2>Setting up the Stratos Domain for your Google Apps Domain</h2>
				        <div id="workArea">
				            <form method="post" name="gappsetupForm" action="done_ajaxprocessor.jsp"> 
				                <div class="toggle_container">
							        <table class="normal-nopadding" cellspacing="0" >
							            <tbody>
							              <tr>
				                                    <td><fmt:message key="admin.email"/><span
				                                            class="required">*</span></td>
				                                    <td><input type="text" name="admin-email"
				                                                           id="admin-email" style="width:400px"/></td>
				                            </tr>
							                <tr>
				                                    <td><fmt:message key="admin.firstname"/><span
				                                            class="required">*</span></td>
				                                    <td><input type="text" name="admin-firstname"
				                                                           id="admin-firstname" style="width:400px"/></td>
				                            </tr>
				                            <tr>
				                                    <td><fmt:message key="admin.lastname"/><span
				                                            class="required">*</span></td>
				                                    <td colspan="2"><input type="text" name="admin-lastname"
				                                                           id="admin-lastname" style="width:400px"/></td>
				                            </tr>
							                <tr>
							                    <td>
							                       <fmt:message key="select.usage.plan.for.tenant"/><span class="required">*</span>
							                    </td>
							                    <td><p>
							               <select name="usage-plan-name" id="usage-plan-name">               
							               <%
               							                   for (int i = 0; i < packageInfo.length; i++) {
               							               %>
							                   <%
							                       if (i == 0) {
							                   %>
							                   <option value="<%=packageInfo[i].getName()%>" selected="selected"><%=packageInfo[i].getName()%></option>
							                   <%
							                       } else {
							                   %>
							                   <option value="<%=packageInfo[i].getName()%>"><%=packageInfo[i].getName()%></option>  
							               <%
  							                   }
  							                       }
  							               %>
							               </select>
							                   <a href=<%=CommonUtil.getStratosConfig().getUsagePlanURL()%>
							                           target=<%=CommonUtil.getStratosConfig().getUsagePlanURL()%>>
							                       <b>More info</b>
							                   </a>
							                   </p>
							                   </td>
							                 </tr>
							                 <tr>
							                 <td class="buttonRow" colspan="2"><input type="button" class="button" value="OK" onclick="validateGAppSetupInfo();"/></td>
							               </tr>
							            </tbody>
							        </table>
							      </div>
				            </form>    
				        </div>
				    </div>
				</fmt:bundle>
                      </td>
                  </tr>
              </table>
          </td>
      </tr>
      <tr>
          <td id="footer"><jsp:include page="../admin/layout/footer.jsp" /></td>
      </tr>
  </table>
  
    
</body>
</html>