<%--
 Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 WSO2 Inc. licenses this file to you under the Apache License,
 Version 2.0 (the "License"); you may not use this file except
 in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 --%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.net.URL" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page import="org.wso2.carbon.utils.CarbonUtils" %>

<%
    String userGuideURL = (String) config.getServletContext().getAttribute(CarbonConstants.PRODUCT_XML_WSO2CARBON +
                                                                           CarbonConstants.PRODUCT_XML_USERGUIDE);
    if(userGuideURL == null){
        userGuideURL = "#";
    }

	String serverURL = (String) session.getAttribute(CarbonConstants.SERVER_URL);
    if (serverURL == null) {
        serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        session.setAttribute(CarbonConstants.SERVER_URL, serverURL);
    }

%>
<!--[IF IE 7]>
	<style>
		div#header-div div.right-links{
			position:absolute;
		}
	</style>
<![endif]-->
<fmt:bundle basename="org.wso2.carbon.i18n.Resources">

    <div id="header-div">
        <div class="right-logo"><fmt:message key="management.console"/></div>
        <div class="left-logo">
            <a href="../admin/index.jsp" class="header-home"><img src="../admin/images/1px.gif" width="300px" height="32px"/></a>
        </div>
        <div class="middle-ad">
            <%@include file="announcements.jsp"%>
        </div>
        <div class="header-links">
		<div class="right-links">            
			<ul>
		                <%
		                    Boolean authenticated = (Boolean) request.getSession().getAttribute("authenticated");
		                    if (authenticated != null && authenticated.booleanValue()) {
		                        String signedInAs = (String) request.getSession().getAttribute("logged-user");
//		                        String serverURL = (String) request.getSession().getAttribute(CarbonConstants.SERVER_URL);
		                        String domainName = (String) request.getSession().getAttribute(MultitenantConstants.TENANT_DOMAIN);
		                        // Now that super.tenant domain is carbon.super we are showing the domain name itself.
                                // showing localhost makes no-sense when local transport is enabled and accessed from
                                // a remote browser
		                        /*if (domainName == null ||
		                        		MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domainName)) {
                                    //TODO Hack checking whether this is the local transport.
                                    if(serverURL.startsWith("local")){
                                        domainName = "localhost";
                                    } else {
                                        URL url = new URL(serverURL);

                                        // this is the super tenant, we are showing the host name instead of the domain name,
                                        domainName = url.getHost();
                                        int port = url.getPort();
                                        if (port != -1) {
                                            domainName += ":" + port;
                                        }
                                    }
		                        } */
                                if (CarbonUIUtil.isContextRegistered(config, "/worklist/")) {
		                %>
                        <jsp:include page="../../worklist/header.jsp"/>
                        <%
                                }
                        %>
                        <%
                            if (authenticated != null && authenticated.booleanValue() && 
                                MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(domainName)) {
                                if (!CarbonUtils.isRunningOnLocalTransportMode()) {
                                    String remoteServerURL = (String) request.getSession().getAttribute(CarbonConstants.SERVER_URL);
                                    /*server url comes in the form: https://localhost:9443/services/
                                     we need to make it localhost:9443 and display in the header
                                     */
                                    if (remoteServerURL != null) {
                                        URL remoteURL = new URL(remoteServerURL);
                                        String host = remoteURL.getHost();
                                        int port = remoteURL.getPort();
                                        if (host != null && port > 0) {
                                            remoteServerURL = host + ":" + port;
                                       }
                                        String frontEndServerURL = CarbonUIUtil.getAdminConsoleURL(request);
                                        if (frontEndServerURL != null) {
                                            URL localURL = new URL(frontEndServerURL);
                                            String frontEndHost = localURL.getHost();
                                            int frontEndPort = localURL.getPort();
                                            if (frontEndHost != null && frontEndPort > 0) {
                                                frontEndServerURL = frontEndHost + ":" + frontEndPort;
                                            }
                                        }
                                        if (!remoteServerURL.equals(frontEndServerURL)) {
   
                        %>
                                <li class="middle">
                                    <label id="logged-user">
                                        <strong><fmt:message key="remote.server.url"/>:</strong>&nbsp;<%=remoteServerURL%>
                                    </label>
                                </li>
                                <li class="middle">|</li>
                        <%
                                        }
                                    }
                                }
                            }
                        %>

		                <li class="middle">
		                    <label id="logged-user">
		                        <strong><fmt:message key="signed.in.as"/>:</strong>&nbsp;<%=signedInAs%>@<%=domainName%>
		                    </label>
		                </li>
				<li class="middle">|</li>
		                <li class="right">
		                    <a href="../admin/logout_action.jsp"><fmt:message key="sign.out"/></a>
		                </li>
		                <%  } else { %>
		                <li class="right">
		                    <a href="../admin/login.jsp"><fmt:message key="sign.in"/></a>
		                </li>
		                <%  } %>
		                <li class="middle">|</li>
		                <li class="middle">
		                    <a target="_blank" href="<%=userGuideURL %>"><fmt:message key="docs"/></a>
		                </li>
				<li class="middle">|</li>
				<%
				String aboutPageURL = "";
				if(CarbonUIUtil.isContextRegistered(config,"/product/")){
					aboutPageURL = "../product/about.html";
				}else{
					//switch to carbon about page
					aboutPageURL = "../docs/about.html";
				}
				%>
		                <li class="left">
		                    <a target="_blank" href="<%=aboutPageURL %>"><fmt:message key="about"/></a>
		                </li>
		            </ul>
		</div>
        </div>
    </div>
</fmt:bundle>
