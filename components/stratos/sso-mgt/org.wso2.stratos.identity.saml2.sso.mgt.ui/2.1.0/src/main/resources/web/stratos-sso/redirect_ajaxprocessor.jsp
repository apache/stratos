<%@page import="org.wso2.carbon.identity.base.IdentityConstants"%>
<%@page import="org.wso2.carbon.identity.core.util.IdentityUtil"%>
<%@page import="org.wso2.carbon.identity.sso.saml.ui.SAMLSSOProviderConstants" %>
<%@page import="org.wso2.carbon.identity.sso.saml.ui.session.mgt.FESessionBean" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.ui.session.mgt.FESessionManager" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSOReqValidationResponseDTO" %>
<%@ page import="org.wso2.carbon.identity.sso.saml.stub.types.SAMLSSORespDTO" %>
<%@ page import="java.net.URLDecoder" %>
<%@page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants"%>
<%@page import="org.wso2.carbon.utils.multitenancy.MultitenantUtils"%>
<%@ page import="org.wso2.carbon.stratos.common.util.StratosConfiguration" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>

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
    StratosConfiguration stratosConfig = CommonUtil.getStratosConfig();
    if(stratosConfig == null){
        stratosConfig = CommonUtil.loadStratosConfiguration();
    }
    
    String loadUrl = stratosConfig.getSsoLoadingMessage();
    if("".equals(loadUrl)){
        loadUrl = "Loading...";
    }
    String assertionConsumerURL = (String) request.getAttribute(SAMLSSOProviderConstants.ASSRTN_CONSUMER_URL);
    String samlResponse = (String) request.getAttribute(SAMLSSOProviderConstants.SAML_RESP);
    String relayState = (String) request.getAttribute(SAMLSSOProviderConstants.RELAY_STATE);
    String subject = (String) request.getAttribute(SAMLSSOProviderConstants.SUBJECT);
    
    String domain = null;
    if(subject != null && MultitenantUtils.getTenantDomain(subject) != null){
           domain = MultitenantUtils.getTenantDomain(subject);
    }

    String postURL = "the service";
    if(assertionConsumerURL != null){
        postURL = assertionConsumerURL.substring(0,assertionConsumerURL.lastIndexOf("/"));
    }

    // the fix for the LB SSO issue
    if("true".equals(IdentityUtil.getProperty((IdentityConstants.ServerConfig.SSO_TENANT_PARTITIONING_ENABLED)))){
        assertionConsumerURL = assertionConsumerURL+"?"+MultitenantConstants.TENANT_DOMAIN+"="+domain;
    }
    
    relayState = URLDecoder.decode(relayState, "UTF-8");
    relayState = relayState.replaceAll("&", "&amp;").replaceAll("\"", "&quot;").replaceAll("'", "&apos;").
            replaceAll("<", "&lt;").replaceAll(">", "&gt;").replace("\n", "");
%>


<div class="loading-text"><%=loadUrl%></div>
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


<form method="post" action="<%=assertionConsumerURL%>" class="redirectForm">
	<input type="hidden" name="SAMLResponse" value="<%=samlResponse%>"/>
	<input type="hidden" name="RelayState" value="<%=relayState%>"/>
</form>

<script type="text/javascript">
	document.forms[0].submit();
</script>

		

</body>
</html>
