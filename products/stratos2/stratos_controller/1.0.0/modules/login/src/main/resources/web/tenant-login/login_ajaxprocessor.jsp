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
<%@ page import="org.wso2.carbon.CarbonConstants" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>

<%--Copyright 2004 The Apache Software Foundation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>

 

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">


<head>
    <meta http-equiv="content-type" content="text/html;charset=utf-8"/>
    <title>WSO2 Stratos Manager</title>
    <link href="../admin/css/global.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="../styles/css/main.css" rel="stylesheet" type="text/css" media="all"/>

    <link href="../dialog/css/jqueryui/jqueryui-themeroller.css" rel="stylesheet" type="text/css"
          media="all"/>
    <link href="../dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>
    <link rel="icon" href="../admin/images/favicon.ico" type="image/x-icon"/>
    <link rel="shortcut icon" href="../admin/images/favicon.ico" type="image/x-icon"/>

    <script type="text/javascript" src="../admin/js/jquery.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.form.js"></script>
    <script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>

    <script type="text/javascript" src="../admin/js/main.js"></script>
    <script type="text/javascript" src="../admin/js/WSRequest.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../tenant-login/js/login.js"></script>
    <script type="text/javascript" src="../ajax/js/prototype.js"></script>

</head>

<body>
<div id="dcontainer"></div>
<script type="text/javascript" src="../dialog/js/dialog.js"></script>

<%
    String registerPath = "../tenant-register/select_domain.jsp";
    String forgotPasswordPath = "../admin-mgt/forgot_password.jsp";
    //register path changes if the tenant is in others domain
    if (request.getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
        registerPath = "/carbon/tenant-register/select_domain.jsp";
    }

    if("true".equals(session.getAttribute("domain-validation-success"))) {
        session.removeAttribute("domain-validation-success");
        %>

    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showWarningDialog("You have succesfully validated your domain name. " + 
            "Please re-login to your account to continue working.");
        });
    </script>
<%
    }
    if("true".equals(session.getAttribute("temp-email-verfication"))){
        session.removeAttribute("temp-email-verfication");
        %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showWarningDialog('Please login to your email and confirm the given email to proceed with the registration.');
        });
    </script>
<%
    }
    String tenantdomain = (String)request.getAttribute(MultitenantConstants.TENANT_DOMAIN);

    if ("true".equals(session.getAttribute("temp-suffixed-trial"))) {
        session.removeAttribute("temp-suffixed-trial");
%>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            // we know the tenant domain 
            var newTenantDomain = "<%=tenantdomain%>";
            var exitCode = function() {
                var redirectUrl = "/t/" + newTenantDomain + "/carbon/admin/login.jsp";
                window.location.href = redirectUrl;
            };
            CARBON.showWarningDialog("The domain name of your account is renamed to " + newTenantDomain + " "  +
                      "as the ownership of the domain is not confirmed. You can login to the account under the " +
                      "changed domain name and confirm the ownership of your domain from the 'Account management' page", exitCode, exitCode);
        });
    </script>    
<%
    }

    String tip = "";
    if(tenantdomain == null){
        tenantdomain = "";
        tip = "Enter the username in the format: user  name@yourdomain, Example: foo@abc.com";
    }
    else{
        tenantdomain = "@" + tenantdomain;
    }
    String userForumURL = (String) config.getServletContext().getAttribute(CarbonConstants.PRODUCT_XML_WSO2CARBON + CarbonConstants.PRODUCT_XML_USERFORUM);
    String userGuideURL = (String) config.getServletContext().getAttribute(CarbonConstants.PRODUCT_XML_WSO2CARBON + CarbonConstants.PRODUCT_XML_USERGUIDE);
    String mailinglistURL = (String) config.getServletContext().getAttribute(CarbonConstants.PRODUCT_XML_WSO2CARBON + CarbonConstants.PRODUCT_XML_MAILINGLIST);
    String issuetrackerURL = (String) config.getServletContext().getAttribute(CarbonConstants.PRODUCT_XML_WSO2CARBON + CarbonConstants.PRODUCT_XML_ISSUETRACKER);
    if (userForumURL == null) {
        userForumURL = "#";
    }
    if (userGuideURL == null) {
        userGuideURL = "#";
    }
    if (mailinglistURL == null) {
        mailinglistURL = "#";
    }
    if (issuetrackerURL == null) {
        issuetrackerURL = "#";
    }
%>
<fmt:bundle basename="org.wso2.carbon.i18n.Resources">
    <%
        String loginStatus = request.getParameter("loginStatus");
        if (loginStatus != null && "false".equalsIgnoreCase(loginStatus)) {
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showWarningDialog('<fmt:message key="login.fail.message"/>');
        });
    </script>
    <%
        }
                                          
        if (loginStatus != null && "failed".equalsIgnoreCase(loginStatus)) {
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showWarningDialog('<fmt:message key="login.fail.message"/>');
        });
    </script>
    <%
        }
        String backendURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    %>
    <%--New page design begings--%>
    <link rel="stylesheet" type="text/css" href="../tenant-login/css/loginpage.css"/>
		<div id="login-content">
					<div class="main-text">
						WSO2 Stratos deploys the powerful WSO2 Carbon platform as Platform-as-a-Service on cloud infrastructure, thereby providing all the advantages expected from the cloud: world-class time-to-market, optimum resource utilization and automatic governance and monitoring.
					</div>
					<div class="clear"></div>
					<div class="feature feature-left">
						<img src="../tenant-login/images/feature-01-icon.gif"/>
						<h2>Elasticity</h2>
						<p>
							Stratos manages your underlying cloud infrastructure to seamlessly handle the scalability demands of your application. 
						</p>
					</div>
					<div class="feature">
						<img src="../tenant-login/images/feature-02-icon.gif"/>
						<h2>Multi-tenancy</h2>
						<p>
							Departments, developer groups, or projects run fully independently, but share the same middleware platform for maximum resource utilization. 
						</p>
					</div>
					<div class="feature">
					 	<img src="../tenant-login/images/feature-03-icon.gif"/>
						<h2>Self Provisioning</h2>
						<p>
							Authorized users can provision new tenants from a web portal in moments.
						</p>
					</div>
					<div class="clear"></div>
							<a class="register-button" href="<%=registerPath%>">Get Started Now for FREE!</a>
				       	<table class="adv">
						<tr>
						<td>

			                                <h2 class="stratos-Signin"><fmt:message key="sign.in"/></h2>
			                                <div class="sign-in-box">
			                                    <table style="width:100%">
			    `                                   <tr>
			                                            <td class="user_pass_td">
			                                                <h3>Login Using Username & Password</h3>
			
			                                                <form action='../admin/login_action.jsp' method="POST" target="_self">
		                                    			<!-- making the backend url a hidden text box -->
		                                    			<input type="hidden" id="txtbackendURL" name="backendURL" class="user" tabindex="1" value="<%=backendURL%>"/>
			                                                    <table class="user_pass_table">
			                                                    <tr>
											<td><label for="txtUserName"><fmt:message key="username"/></label></td>
											<td><input type="text" id="txtUserName" name="username" tabindex="1"/> <%=tenantdomain%></td>
										</tr>
										<tr>
											<td></td>
											<td><%=tip%></td>
										</tr>
										<tr>
											<td><label for="txtPassword"><fmt:message key="password"/></label></td>
											<td><input type="password" id="txtPassword" name="password" tabindex="2"/></td>
										</tr>
										<tr>
											<td></td>
											<td><input type="submit" value="<fmt:message key="sign.in"/>" tabindex="3"/></td>
										</tr>
										<%
										if (registerPath != null) {
										%>
										                                <tr>
										                                    <td colspan="2">
										                                        If your organization doesn't have an account, please <a href="<%=registerPath%>" alt="Go To Register Page">register</a>.
										                                    </td>
										                                </tr>
										<%
										}
										%>
										<tr>
			                                                            <td colspan="2"><a tabindex="4" href="../tenant-login/docs/userguide.html" target="_blank" class="help"><fmt:message key="sign.in.help"/></a>&nbsp;|&nbsp;<a tabindex="5" class="help" href="<%=forgotPasswordPath%>" target="_blank" class="password">Forgot Password</a></td>
			                                                        </tr>
			                                                    </table>
                                                    

                                                    			</form>
                                            			</td>
                                        			<td class="google-app-td">
			                                            <h3>Or login using Google Apps Account</h3>
			
			                                            <img alt="Google App Account"
			                                                 src="../tenant-login/images/google-apps-login.gif"
			                                                 onclick="showHidePanel()"/>
			
			                                            <div id="loginBoxGoogle">
			                                                <div class="help_txt"> Enter Google App domain</div>
			                                                <form action="../relyingparty/openid.jsp" name="googleAppLogin"
			                                                      method="POST">
			                                                    <table class="styledLeft noBorders">
			                                                        <tbody>
			                                                        <tr>
			                                                            <td><input type="text" name="gAppDomainOpenId"
			                                                                       tabindex="3"/></td>
			                                                            <td><input type="button" class="button" value="GO" style="background-image:none"
			                                                                       onclick="document.googleAppLogin.submit()"/></td>
			                                                        </tr>
			                                                        </tbody>
			                                                    </table>
			                                                </form>
			                                            </div>
			                                            <script type="text/javascript">
			                                                function showHidePanel() {
			                                                    jQuery('#loginBoxGoogle').slideToggle("fast");
			                                                }
			                                                jQuery(document).ready(function() {
			                                                    jQuery('#loginBoxGoogle').hide();
			                                                    document.getElementById('txtUserName').focus();
			                                                });
			
			                                            </script>
                                        			</td>
                                    				</tr>
                                    			</table>
                                			</div>
						</td>
						</tr>
					</table>
				<div class="clear"></div>
				<div id="footer">
					<div class="powered">
						<span>Powered by</span><a target="_blank" href="http://wso2.org/projects/stratos"><img src="../tenant-login/images/powered.gif"/></a>
					</div>
					&copy; appserver.stratoslive.wso2.com copyright 2010 WSO2, Inc. &nbsp;
					<a target="_blank" href="http://www.wso2.com/cloud/services/terms-of-use">Terms of Service</a>
					|
					<a target="_blank"  href="http://www.wso2.com/cloud/services/privacy-policy">Privacy Policy</a>
					|
					<a target="_blank"  href="http://www.wso2.com/cloud/services/support">Support</a>
				</div>
			</div>




		
    <script type="text/javascript">

 function checkform()
{
    var username=document.getElementById('txtUserName').value;
    username=username.trim();
    document.getElementById('txtUserName').value=username;
    return true;
}                 
 String.prototype.trim = function ()
 {
     return this.replace(/^\s*/, "").replace(/\s*$/, "");
 }

        function init(loginStatus) {
            // intialize the code and call to the back end
            /*wso2.wsf.Util.initURLs();*/
            /*Initialize the XSLT cache*/
            /*wso2.wsf.XSLTHelper.init();*/

            if (loginStatus == 'true') {
            } else if (loginStatus == 'null') {
            } else if (loginStatus == 'false') {
                wso2.wsf.Util.alertWarning("Login failed. Please recheck the username and password and try again")
            }
        }
        document.getElementById('txtUserName').focus();
    </script>

</fmt:bundle>

</body>
</html>
