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

<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.stratos.common.constants.StratosConstants" %>

<%@ page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConstants" %>
<%@ page import="org.wso2.stratos.identity.saml2.sso.mgt.ui.Util" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="content-type" content="text/html;charset=utf-8"/>
    <title>WSO2 Stratos Identity</title>
    <link href="../carbon/admin/css/global.css" rel="stylesheet" type="text/css" media="all"/>
    <link href="../carbon/styles/css/main.css" rel="stylesheet" type="text/css" media="all"/>
    <link media="all" type="text/css" rel="stylesheet"
          href="../carbon/stratos-cep/css/stratos-loginpage.css"/>

    <link href="../carbon/dialog/css/jqueryui/jqueryui-themeroller.css" rel="stylesheet" type="text/css"
          media="all"/>
    <link href="../carbon/dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>
    <link rel="icon" href="../carbon/admin/images/favicon.ico" type="image/x-icon"/>
    <link rel="shortcut icon" href="../carbon/admin/images/favicon.ico" type="image/x-icon"/>

    <script type="text/javascript" src="../carbon/admin/js/jquery.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/jquery.form.js"></script>
    <script type="text/javascript" src="../carbon/dialog/js/jqueryui/jquery-ui.min.js"></script>

    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/WSRequest.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script src="../carbon/yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
    <script src="../carbon/admin/js/widgets.js" type="text/javascript"></script>

</head>
<body>
<jsp:include page="../carbon/admin/jsp/browser_checker.jsp" />
<div id="dcontainer"></div>
<script type="text/javascript" src="../carbon/dialog/js/dialog.js"></script>
<fmt:bundle basename="org.wso2.stratos.identity.saml2.sso.mgt.ui.i18n.Resources">
    <%
        String errorMessage = "login.fail.message";
        String tenantRegistrationPageURL = Util.getTenantRegistrationPageURL();

        if (request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE) != null &&
            (Boolean)request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE)) {
            if(request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE_MSG) != null){
                errorMessage = (String) request.getAttribute(SAMLSSOProviderConstants.AUTH_FAILURE_MSG);
            }
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showWarningDialog('<fmt:message key="<%=errorMessage%>"/>');
        });
    </script>
    <%
        }  else if (request.getSession().getAttribute(CarbonUIMessage.ID) !=null) {
            CarbonUIMessage carbonMsg = (CarbonUIMessage)request.getSession().getAttribute(CarbonUIMessage.ID);
            %>
            
                <script type="text/javascript">
                    jQuery(document).ready(function() {
                        CARBON.showErrorDialog("<%=carbonMsg.getMessage()%>");
                    });
                </script>
      <%}
    %>
    <script type="text/javascript">
        function doLogin() {
            var loginForm = document.getElementById('loginForm');
            loginForm.submit();
        }
        function doRegister() {
            document.getElementById('registrationForm').submit();
        }
    </script>
		<div id="login-content">
					<div class="main-text">
                        WSO2 CEP is a extremely high performing complex event processor that identifies the most meaningful events within the event cloud, analyzes their impacts, and acts on them in real time by triggering output events.
					</div>
					<div class="clear"></div>
					<div class="feature feature-left">
						<img src="../carbon/stratos-cep/images/feature-01-icon.gif"/>
						<h2>Real-Time Processing</h2>
						<p>
                            Contains different queries for processing input event streams and trigger output events in real-time.
						</p>
					</div>
					<div class="feature">
						<img src="../carbon/stratos-cep/images/feature-02-icon.gif"/>
						<h2>Enterprise Integration</h2>
						<p>
                            Receive and publish multiple events types via brokers such as, Local, WS-Event, JMS and Agent.
						</p>
					</div>
					<div class="feature">
					 	<img src="../carbon/stratos-cep/images/feature-03-icon.gif"/>
						<h2>Monitoring CEP</h2>
						<p>
                            Shows real-time query execution statistics of all CEP operations in pre Bucket, per Broker and per Topic based charts.
						</p>
					</div>
					<div class="clear"></div>
					<form action="<%=tenantRegistrationPageURL%>" id="registrationForm">
				                    <input type="hidden" name="<%=StratosConstants.ORIGINATED_SERVICE%>"
				                           value=<%=Util.getStratosServiceName(request.getRequestURI())%>/>
							<a class="register-button" onclick="doRegister()">Get Started Now for FREE!</a>
				        </form>
					<table class="ad">
						<tr>
						<td>

			                                <h2 class="stratos-Signin"><fmt:message key="sign.in"/></h2>
			                                <div class="sign-in-box">
			                                    <table style="width:100%">
			    `                                   <tr>
			                                            <td class="user_pass_td">
			                                                <h3>Login Using Username & Password</h3>
			
			                                                <form action="../samlsso" method="post" id="loginForm">

			                                                    <table class="user_pass_table">
			                                                    <tr>
										                            <td><label for="txtUserName"><fmt:message key='username'/></label></td>
										                            <td><input type="text" id='username' name="username" size='30'/></td>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL %>"
										                                   value="<%= request.getAttribute(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL) %>"/>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.ISSUER %>"
                                                                           value="<%= request.getAttribute(SAMLSSOProviderConstants.ISSUER) %>"/>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.REQ_ID %>"
                                                                           value="<%= request.getAttribute(SAMLSSOProviderConstants.REQ_ID) %>"/>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.SUBJECT %>"
                                                                           value="<%= request.getAttribute(SAMLSSOProviderConstants.SUBJECT) %>"/>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.RP_SESSION_ID %>"
                                                                           value="<%= request.getAttribute(SAMLSSOProviderConstants.RP_SESSION_ID) %>"/>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.ASSERTION_STR %>"
                                                                           value="<%= request.getAttribute(SAMLSSOProviderConstants.ASSERTION_STR) %>"/>
                                                                    <input type="hidden" name="<%= SAMLSSOProviderConstants.RELAY_STATE %>"
                                                                           value="<%= request.getAttribute(SAMLSSOProviderConstants.RELAY_STATE) %>"/>
										                        </tr>
    <%
           							String tip = "Enter the username in the format: adminname@yourdomain. Example: foo@abc.com";
    %>
                                                                <tr>
                                                                    <td></td>
                                                                    <td><%=tip%>
                                                                    </td>
                                                                </tr>
			                                                    <tr>
										                            <td><label for="txtPassword"><fmt:message key='password'/></label></td>
										                            <td><input type="password" id='password' name="password" size='30'/>
										                            </td>
										                        </tr>
			                                                    <tr>
										                            <td></td>
										                            <td><input type="submit" value="<fmt:message key='login'/>"/></td>
										                        </tr>
			                                                       
			                                                        <tr>
			                                                            <td colspan="2"><a tabindex="4" href="../carbon/stratos-sso/docs/userguide.html" target="_blank" class="help"><fmt:message key="sign.in.help"/></a>&nbsp;|&nbsp;<a tabindex="5" class="help" href="<%=Util.getForgetPasswordLink()%>" target="_blank" class="password">Forgot Password</a></td>
			                                                        </tr>
			                                                    </table>
                                                    

                                                    			</form>
                                            			</td>
                                        			<td class="google-app-td">
			                                            <h3>Or login using Google Apps Account</h3>
			
			                                            <img alt="Google App Account" style="cursor:pointer"
			                                                 src="../carbon/tenant-login/images/google-apps-login.gif"
			                                                 onclick="showHidePanel()"/>
			
			                                            <div id="loginBoxGoogle">
			                                                <div class="help_txt"> Enter Google App domain</div>
			                                                <form action="../carbon/relyingparty/openid.jsp" name="googleAppLogin"
			                                                      method="POST">
			                                                    <table class="styledLeft noBorders">
			                                                        <tbody>
			                                                        <tr>
			                                                            <td><input type="text" name="gAppDomainOpenId" id="gAppDomainOpenId"
			                                                                       tabindex="3" alt="example.com"  value="" /></td>
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
			                                                    if(document.getElementById('username')!= null){
			                                                    	document.getElementById('username').focus();
			                                                    }
			                                                });
									enableDefaultText("gAppDomainOpenId");
			                                            </script>
                                        			</td>
                                    				</tr>
                                    			</table>
                                			</div>
						</td>
						</tr>
					</table>
					<%--<div class="espermsg">Esper<sup>TM</sup> is a registered trademark of EsperTech.</div>--%>
					<div class="clear"></div>
				<div id="footer">
					<div class="powered">
						<span>Powered by</span><img src="../carbon/stratos-cep/images/powered-logo.gif" alt="WSO2 Data Services Server"/>
					</div>
					&copy;stratoslive.wso2.com copyright 2010-2012 WSO2, Inc. &nbsp;
					<a href="http://www.wso2.com/cloud/services/terms-of-use" target="_blank">Terms of Service</a>
			|
			<a href="http://www.wso2.com/cloud/services/privacy-policy" target="_blank">Privacy Policy</a>
			|
			<a href="http://www.wso2.com/cloud/services/support" target="_blank">Support</a>
				</div>
			</div>
</fmt:bundle>
</body>
</html>
