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
<%@page import="org.opensaml.saml2.core.AuthnRequest" %>
<%@page import="org.opensaml.saml2.core.LogoutRequest" %>
<%@ page
        import="org.wso2.carbon.identity.authenticator.saml2.sso.common.SAML2SSOAuthenticatorConstants" %>
<%@ page import="org.wso2.carbon.identity.authenticator.saml2.sso.common.SSOSessionManager" %>
<%@ page
        import="org.wso2.carbon.identity.authenticator.saml2.sso.common.builders.AuthenticationRequestBuilder" %>
<%@ page
        import="org.wso2.carbon.identity.authenticator.saml2.sso.common.builders.LogoutRequestBuilder" %>
<%@ page
        import="org.wso2.carbon.identity.authenticator.saml2.sso.common.Util" %>
<%@ page import="org.wso2.carbon.registry.core.utils.UUIDGenerator" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>WSO2 Stratos Identity</title>
    <style type="text/css" media="screen"><!--
    body {
        color: #111111;
	font: 13px/27px Arial,sans-serif;
        font-size-adjust: none;
        font-style: normal;
        font-variant: normal;
    }

    

    .redirectForm {
        display: inline;
    }
    .dots{
	border:solid 1x #ccc;
	width:10px;
	height:10px;
	float:left;
	margin:5px;
	-moz-border-radius: 4px;
	border-radius: 4px;
	-moz-box-shadow: 3px 3px 3px #888;
	-webkit-box-shadow: 3px 3px 3px #888;
	box-shadow: 3px 3px 3px #888;
    }
    .dots-pane{
	width:150px;
	margin:0px 10px;
    }
    .dot0{
	background-color:#111;
    }
    .dot1{
	background-color:#444;
    }
    .dot2{
	background-color:#777;
    }
    .dot3{
	background-color:#ddd;
    }
    .dot4{
	background-color:#eee;
    }
    .dot5{
	background-color:#fff;
    }
    .loading-text{
	padding:10px;
	font-weight:bold;
	font-size:16px;
    }
    --></style>
</head>
<body>
<%
    String encodedReq = null;
    String relayState = "";
    String domain = null;
    if (request.getParameter(SAML2SSOAuthenticatorConstants.LOG_OUT_REQ) != null) {
        LogoutRequestBuilder logoutRequestBuilder = new LogoutRequestBuilder();
        LogoutRequest logoutReq = logoutRequestBuilder.buildLogoutRequest((String) request.getAttribute(
                SAML2SSOAuthenticatorConstants.LOGGED_IN_USER), SAML2SSOAuthenticatorConstants.LOGOUT_USER);
        encodedReq = Util.encode(Util.marshall(logoutReq));
        relayState = UUIDGenerator.generateUUID();
    } else {
        AuthenticationRequestBuilder authnReqGenerator = new AuthenticationRequestBuilder();
        AuthnRequest authRequest = authnReqGenerator.buildAuthenticationRequest(null);
        encodedReq = Util.encode(Util.marshall(authRequest));
        relayState = UUIDGenerator.generateUUID();
        domain = (String) request.getAttribute(MultitenantConstants.TENANT_DOMAIN);
    }
    // add the relay state to Session Manager
    SSOSessionManager.addAuthnRequest(relayState);

%>
<div class="loading-text">Loading WSO2 StratosLive...</div>
<div class="dots-pane" id="dotsContainer">
    <div class="dots"></div>
    <div class="dots"></div>
    <div class="dots"></div>
    <div class="dots"></div>
    <div class="dots"></div>
    <div class="dots"></div>
</div>

<script>
    var t;
    var timer_is_on = 0;
    var j=0;
    var dotsContainer = document.getElementById('dotsContainer');
    var dots = dotsContainer.childNodes;
    var divdots = new Array();
    for(var i=0;i<dots.length;i++){
	if(dots[i].nodeName == "DIV" ){
	    divdots.push(dots[i]);
	}
    }
    function animateStuff(){
	for(var i=0;i<divdots.length;i++){
	    var classNumber;
	    if((i+j)<divdots.length){
		classNumber = i + j;
	    }else{
		classNumber = i + j - divdots.length;
	    }
	    divdots[i].className = "dots dot"+classNumber;
	    
	}
	if(j<=5){
	    j++;
	}else{
	    j=0;
	}
	t = setTimeout(animateStuff,200);
    }

    if(!timer_is_on){
	animateStuff();
    }
</script>


		
<form method="post" action="<%=Util.getIdentityProviderSSOServiceURL()%>" class="redirectForm">
    <input type="hidden"
              name="<%=SAML2SSOAuthenticatorConstants.HTTP_POST_PARAM_SAML2_AUTH_REQ%>"
              value="<%= encodedReq %>"/>
        <input type="hidden" name="RelayState" value="<%= relayState %>"/>
        <input type="hidden" name="<%= MultitenantConstants.TENANT_DOMAIN %>"
               value="<%= domain %>"/>
        <input type="hidden" name="<%= MultitenantConstants.SSO_AUTH_SESSION_ID %>" value="<%= session.getId() %>"/>
        
</form>
	
<script type="text/javascript">
	document.forms[0].submit();
</script>

</body>
</html>
