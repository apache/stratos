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
<%@page import="org.wso2.carbon.gapp.registration.ui.GoolgeAppsRegistrationClient"%>
<%@page import="com.google.step2.Step2.AxSchema"%>
<%@page import="org.wso2.carbon.identity.relyingparty.stub.dto.ClaimDTO"%>
<%@page import="com.google.gdata.data.appsforyourdomain.provisioning.UserEntry"%>
<%@page import="com.google.gdata.client.appsforyourdomain.UserService"%>
<%@page import="java.net.URL"%>
<%@page import="com.google.gdata.client.authn.oauth.OAuthHmacSha1Signer"%>
<%@page import="com.google.gdata.client.authn.oauth.GoogleOAuthParameters"%>
<%@page import="org.wso2.carbon.utils.CarbonUtils"%>
<%@page import="org.wso2.carbon.gapp.registration.ui.GAppRegistrationUIConstants"%>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDConsumer"%>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.client.RelyingPartyServiceClient"%>
<%@page import="org.wso2.carbon.identity.relyingparty.ui.openid.OpenIDAuthenticationRequest"%>
<%@ page import="org.wso2.carbon.identity.relyingparty.stub.dto.OpenIDDTO" %>
<%@page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
  try {
      
      session.setAttribute(GAppRegistrationUIConstants.ALLOWED, new Boolean("false"));
      
      // is authenticated
      String webContextRoot = CarbonUtils.getServerConfiguration().getFirstProperty("WebContextRoot");
      String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(webContextRoot);
      String recievingURL = adminConsoleURL + "gappregistration/openidaccept_ajaxprocessor.jsp";
      OpenIDDTO openID = OpenIDConsumer.getInstance().validateOpenIDAuthentication(request, recievingURL);
      ClaimDTO[] claims = openID.getClaims();
      String emailId = null;
      for(ClaimDTO claim : claims) {
          if(claim.getClaimUri().equals(AxSchema.EMAIL.getUri())) {
              emailId = claim.getClaimValue();
          }
      }
      
      if (emailId != null) {
		    //is authorized
		    String serviceName = (String)session.getAttribute("service");
		    
		    GoogleOAuthParameters oauthParameters = new GoogleOAuthParameters();
		    oauthParameters.setOAuthConsumerKey(
		                                        GoolgeAppsRegistrationClient.
		                                        getGoogleAppSetupPropery(serviceName + ".consumer.key"));
		    oauthParameters.setOAuthConsumerSecret(
		                                           GoolgeAppsRegistrationClient.
		                                           getGoogleAppSetupPropery(serviceName + ".consumer.key.secret"));
		    OAuthHmacSha1Signer signer = new OAuthHmacSha1Signer();       
		    
		    String username =  emailId.substring(0, emailId.indexOf("@"));
		    String domain = (String)session.getAttribute("domain");
		    
		    
		    URL feedUrl = new URL("https://apps-apis.google.com/a/feeds/" + domain + "/user/2.0/" + 
		                                                                                 username + 
		                                                                  "?xoauth_requestor_id=" + 
		                                                                                 emailId);
		
		    UserService service = new UserService("ProvisiongApiClient");
		    service.setOAuthCredentials(oauthParameters, signer);
		    service.useSsl();
		    UserEntry entry = service.getEntry(feedUrl, UserEntry.class);
		    if (entry.getLogin().getAdmin()) {
		        session.setAttribute(GAppRegistrationUIConstants.ALLOWED, new Boolean("true"));    
		    } else {
		        throw new Exception("You are not the admin of this google apps domain. To setup stratos you must be an Admin");
		    }
      } else {
          throw new IllegalStateException("Invalid state");
      }
      
      response.sendRedirect("setup_ajaxprocessor.jsp");
  } catch (Exception e) {
	  CarbonUIMessage.sendCarbonUIMessage("Failed to setup stratos. " + e.getMessage(), CarbonUIMessage.ERROR, request);
	  response.sendRedirect("setup_ajaxprocessor.jsp");
  }
%>