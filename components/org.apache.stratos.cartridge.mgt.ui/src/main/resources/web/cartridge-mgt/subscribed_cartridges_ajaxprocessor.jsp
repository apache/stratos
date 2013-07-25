<!--
     ~  Licensed to the Apache Software Foundation (ASF) under one
     ~  or more contributor license agreements.  See the NOTICE file
     ~  distributed with this work for additional information
     ~  regarding copyright ownership.  The ASF licenses this file
     ~  to you under the Apache License, Version 2.0 (the
     ~  "License"); you may not use this file except in compliance
     ~  with the License.  You may obtain a copy of the License at
     ~
     ~    http://www.apache.org/licenses/LICENSE-2.0
     ~
     ~  Unless required by applicable law or agreed to in writing,
     ~  software distributed under the License is distributed on an
     ~  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
     ~  KIND, either express or implied.  See the License for the
     ~  specific language governing permissions and limitations
     ~  under the License.
     ~
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.apache.stratos.cartridge.mgt.ui.CartridgeAdminClient"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>
<%@ page import="org.apache.stratos.adc.mgt.dto.xsd.Cartridge"%>
<%@ page import="org.apache.stratos.adc.mgt.dto.xsd.CartridgeWrapper"%>
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.util.ArrayList"%>

<jsp:include page="../dialog/display_messages.jsp" />

<%
	response.setHeader("Cache-Control", "no-cache");

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CartridgeAdminClient client;
    Cartridge[] cartridges = null;

    int numberOfPages = 0;
    String pageNumber = request.getParameter("pageNumber");
    if (pageNumber == null) {
        pageNumber = "0";
    }
    int pageNumberInt = 0;
    try {
        pageNumberInt = Integer.parseInt(pageNumber);
    } catch (NumberFormatException ignored) {
    }

    String cartridgeSearchString = request.getParameter("cartridgeSearchString");
    if (cartridgeSearchString == null) {
        cartridgeSearchString = "";
    }

    try {
        client = new CartridgeAdminClient(cookie, backendServerURL, configContext, request.getLocale());
        CartridgeWrapper cartridgeWrapper = client.getPagedSubscribedCartridges(cartridgeSearchString, pageNumberInt);
        if (cartridgeWrapper != null) {
        	numberOfPages = cartridgeWrapper.getNumberOfPages();
        	cartridges = cartridgeWrapper.getCartridges();
        }
    } catch (Exception e) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp" />
<%
	return;
    }
%>

<fmt:bundle basename="org.apache.stratos.cartridge.mgt.ui.i18n.Resources">
	<div>
		<%
			if (cartridges != null) {
				String parameters = "cartridgeSearchString=" + cartridgeSearchString;
		%>
	
		<carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=numberOfPages%>" page="subscribed_cartridges.jsp"
			pageNumberParameterName="pageNumber" resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources" prevKey="prev"
			nextKey="next" parameters="<%=parameters%>" />
		<p>&nbsp;</p>
	
		<form id="frmSubC" name="cartridgesForm" method="post">
			<input type="hidden" name="pageNumber" value="<%=pageNumber%>" />
			<table id="cartridgesTable" class="styledLeft" style="width: 100%">
				<thead>
					<tr>
						<th style="width: 10%"><fmt:message key="cartridge.display.name" /></th>
						<th style="width: 70px"><fmt:message key="cartridge.version" /></th>
						<th style="width: 125px"><fmt:message key="cartridge.tenancymodel" /></th>
						<th style="width: 10%"><fmt:message key="alias" /></th>
						<th style="width: 5%"><fmt:message key="status" /></th>
						<th style="text-align: right; padding-right: 5px; width: 125px;"><fmt:message key="instance.count" /></th>
						<th><fmt:message key="url" /></th>
						<th style="width: 12%"><fmt:message key="repo.url" /></th>
						<th style="width: 8%"><fmt:message key="action" /></th>
					</tr>
				</thead>
				<tbody>
	
					<%
						int id = 1; // To generate ID
					
						for (Cartridge cartridge : cartridges) {
							
							String popupID = "cartridge_popup_" + id;
							String rowStyleClass = ((id & 1) == 0) ? "tableEvenRow" : "tableOddRow";
							
							// Increment ID
							id++;
											
							String[] accessURLs = cartridge.getAccessURLs();
							StringBuilder urlBuilder = new StringBuilder();
							if (accessURLs != null) {
								for (int i = 0; i < accessURLs.length; i++) {
									String url = accessURLs[i];
									if (url != null) {
										if (i > 0) {
											urlBuilder.append("<br />");
										}
										urlBuilder.append("<a href=\"").append(url).append("\" target=\"_blank\">").append(url).append("</a>");
									}
								}
							}
							
							String displayName = (cartridge.getDisplayName() != null ? cartridge.getDisplayName() : "");
							String version = (cartridge.getVersion() != null ? cartridge.getVersion() : "");
							String description = (cartridge.getDescription() != null ? cartridge.getDescription() : "");
							String alias = (cartridge.getCartridgeAlias() != null ? cartridge.getCartridgeAlias() : "");
							String status = (cartridge.getStatus() != null ? cartridge.getStatus() : "");
							int activeInstances = cartridge.getActiveInstances();
							String instances = cartridge.getMultiTenant() ? "N/A" : String.valueOf(activeInstances);
							String accessURL = urlBuilder.toString();
							String repoURL = (cartridge.getRepoURL() != null ? cartridge.getRepoURL() : "");
							String ip = (cartridge.getIp() != null ? cartridge.getIp() : "");
							String dbUsername = (cartridge.getDbUserName() != null ? cartridge.getDbUserName() : "");
							String password = (cartridge.getPassword() != null ? cartridge.getPassword() : "");
							String mappedDomain = (cartridge.getMappedDomain() != null ? cartridge.getMappedDomain() : "");
							String policy = (cartridge.getPolicyDescription() != null ? cartridge.getPolicyDescription() : "");
							String tenancyModel = cartridge.getMultiTenant() ? "Multi-Tenant" : "Single-Tenant";
							
							if (repoURL.startsWith("http")) {
								StringBuilder repoURLBuilder = new StringBuilder();
								repoURL = repoURLBuilder.append("<a href=\"").append(repoURL).append("\" target=\"_blank\">").append(repoURL).append("</a>").toString();
							}
					%>
	
					<tr class="<%=rowStyleClass%>">
						<td><%=displayName%></td>
						<td><%=version%></td>
						<td><%=tenancyModel%></td>
						<td><a onclick="showCartridgeInfo('<%=popupID%>', '<%=alias%>')" style="background-image:url(../admin/images/information.gif);" class="icon-link"><%=alias%></a></td>
						<td><span <%if (!"ACTIVE".equalsIgnoreCase(status)) {%>
							style="background-image: url(images/ajax-loading.gif); background-repeat: no-repeat; background-position: left center; padding-left: 22px;"
							<%}%>><%=status%></span></td>
						<td style="text-align: right;"><%=instances%></td>
						<td><%=accessURL%></td>
						<td><%=repoURL%></td>
						<td><a onclick="unsubscribeCartridge('<%=alias%>');" style="background-image:url(images/unsubscribe.png);" class="icon-link">
	                              <fmt:message key="unsubscribe"/></a>
	                              <% if (mappedDomain.length() == 0) {	%>
	                              	<a href="./map_domain.jsp?cartridge_alias=<%=alias%>&domain=<%=mappedDomain%>" class="icon-link">
	                              	<fmt:message key="mapdomain"/></a>
	                              <% } %>
	                              <% if (repoURL.length() > 0) {	%>
	                              	<a onclick="syncRepo('<%=alias%>');" class="icon-link">
	                              	<fmt:message key="syncrepo"/></a>
	                              <% } %>
	                              <% if (mappedDomain.length() > 0) {	%>
	                              	<a onclick="removeDomain('<%=alias%>');" class="icon-link">
                      				<fmt:message key="removedomain"/></a>
	                              <% } %>
	                              <div id="<%=popupID%>" style="display: none">
	                              	<table class="popupTable" style="width: 100%">
	                              		<tbody>
	                              			<tr class="tableOddRow">
	                              				<td style="width: 40%"><fmt:message key="cartridge.display.name" /></td>
	                              				<td><%=displayName%></td>
	                              			</tr>
	                              			<tr class="tableEvenRow">
	                              				<td style="width: 40%"><fmt:message key="cartridge.version" /></td>
	                              				<td><%=version%></td>
	                              			</tr>
	                              			<tr class="tableOddRow">
	                              				<td style="width: 40%"><fmt:message key="cartridge.description" /></td>
	                              				<td><%=description%></td>
	                              			</tr>
	                              			<tr class="tableEvenRow">
	                              				<td style="width: 40%"><fmt:message key="cartridge.tenancymodel" /></td>
	                              				<td><%=tenancyModel%></td>
	                              			</tr>
	                              			<tr class="tableOddRow">
	                              				<td style="width: 40%"><fmt:message key="alias" /></td>
	                              				<td><%=alias%></td>
	                              			</tr>
	                              			<tr class="tableEvenRow">
	                              				<td style="width: 40%"><fmt:message key="status" /></td>
	                              				<td><%=status%></td>
	                              			</tr>
	                              			<tr class="tableOddRow">
	                              				<td style="width: 40%"><fmt:message key="instance.count" /></td>
	                              				<td><%=instances%></td>
	                              			</tr>
	                              			<tr class="tableEvenRow">
	                              				<td style="width: 40%"><fmt:message key="url" /></td>
	                              				<td><%=accessURL%></td>
	                              			</tr>
											<% 
											// Assign style class according to row
											int popupRowId = 0; 
											%>
	                              			<% if (policy.length() > 0) { %>
	                              			<tr class="<%=(((++popupRowId & 1) == 0) ? "tableEvenRow" : "tableOddRow")%>">
	                              				<td style="width: 40%"><fmt:message key="policy" /></td>
	                              				<td><%=policy%></td>
	                              			</tr>
	                              			<% } %>
	                              			<% if (repoURL.length() > 0) { %>
	                              			<tr class="<%=(((++popupRowId & 1) == 0) ? "tableEvenRow" : "tableOddRow")%>">
	                              				<td style="width: 40%"><fmt:message key="repo.url" /></td>
	                              				<td><%=repoURL%></td>
	                              			</tr>
	                              			<% } %>
	                              			<% if (ip.length() > 0) { %>
	                              			<tr class="<%=(((++popupRowId & 1) == 0) ? "tableEvenRow" : "tableOddRow")%>">
	                              				<td style="width: 40%"><fmt:message key="ip" /></td>
	                              				<td><%=ip%></td>
	                              			</tr>
	                              			<% } %>
	                              			<% if (dbUsername.length() > 0) { %>
	                              			<tr class="<%=(((++popupRowId & 1) == 0) ? "tableEvenRow" : "tableOddRow")%>">
	                              				<td style="width: 40%"><fmt:message key="username" /></td>
	                              				<td><%=dbUsername%></td>
	                              			</tr>
	                              			<% } %>
	                              			<% if (password.length() > 0) {	%>
	                              			<tr class="<%=(((++popupRowId & 1) == 0) ? "tableEvenRow" : "tableOddRow")%>">
	                              				<td style="width: 40%"><fmt:message key="password" /></td>
	                              				<td><%=password%></td>
	                              			</tr>
	                              			<% } %>
	                              			<% if (mappedDomain.length() > 0) {	%>
	                              			<tr class="<%=(((++popupRowId & 1) == 0) ? "tableEvenRow" : "tableOddRow")%>">
	                              				<td style="width: 40%"><fmt:message key="mappeddomain" /></td>
	                              				<td><%=mappedDomain%></td>
	                              			</tr>
	                              			<% } %>
	                              		</tbody>
	                              	</table>
	                              </div>
	                          </td>
					</tr>
					<%
						}
					%>
				</tbody>
			</table>
		</form>
		<p>&nbsp;</p>
		<carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=numberOfPages%>" page="subscribed_cartridges.jsp"
			pageNumberParameterName="pageNumber" resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources" prevKey="prev"
			nextKey="next" parameters="<%=parameters%>" />
		<%
			} else {
		%>
		<b><fmt:message key="no.cartridges.found" /></b>
		<%
			}
		%>
	</div>
</fmt:bundle>
