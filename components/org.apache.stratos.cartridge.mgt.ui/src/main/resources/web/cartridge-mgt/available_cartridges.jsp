<%--
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
 --%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.apache.stratos.cartridge.mgt.ui.CartridgeAdminClient"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>
<%@ page import="org.apache.stratos.adc.mgt.dto.Cartridge"%>
<%@ page import="org.apache.stratos.adc.mgt.dto.CartridgeWrapper"%>
<%@ page import="org.apache.stratos.adc.mgt.stub.ApplicationManagementServiceADCExceptionException"%>
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
    
    String item = request.getParameter("item");
    boolean multiTenant = "available_multitenant_cartridges".equals(item);

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
        CartridgeWrapper cartridgeWrapper = client.getPagedAvailableCartridges(cartridgeSearchString, pageNumberInt, multiTenant);
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
    
    ResourceBundle bundle = ResourceBundle.getBundle(CartridgeAdminClient.BUNDLE, request.getLocale());
%>

<fmt:bundle basename="org.apache.stratos.cartridge.mgt.ui.i18n.Resources">
	<carbon:breadcrumb label="cartrigdes.available.header" resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources"
		topPage="false" request="<%=request%>" />

	<script type="text/javascript">
		function searchCartridges() {
			document.searchForm.submit();
		}
	</script>

	<div id="middle">
		<h2>
			<fmt:message key="cartrigdes.available.header" />
		</h2>

		<div id="workArea">
			<form action="available_cartridges.jsp" name="searchForm">
				<table class="styledLeft">
					<tr>
						<td>
							<table style="border: 0;">
								<tbody>
									<tr style="border: 0;">
										<td style="border: 0;"><fmt:message key="search.cartrigdes" /> <input type="text"
											name="cartridgeSearchString" value="<%=cartridgeSearchString != null ? cartridgeSearchString : ""%>" />&nbsp;
										</td>
										<td style="border: 0;"><a class="icon-link" href="#" style="background-image: url(images/search.gif);"
											onclick="searchCartridges(); return false;" alt="<fmt:message key="search"/>"> </a></td>
									</tr>
								</tbody>
							</table>
						</td>
					</tr>
				</table>
				<input type="hidden" name="item" value="<%=item%>" />
			</form>

			<p>&nbsp;</p>
			<%
				if (cartridges != null) {
					String parameters = "cartridgeSearchString=" + cartridgeSearchString + "&item=" + item;
			%>

			<carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=numberOfPages%>" page="available_cartridges.jsp"
				pageNumberParameterName="pageNumber" resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources" prevKey="prev"
				nextKey="next" parameters="<%=parameters%>" />
			<p>&nbsp;</p>

			<form name="cartridgesForm" method="post">
				<input type="hidden" name="pageNumber" value="<%=pageNumber%>" />
				<table class="styledLeft" id="cartridgesTable" style="width: 100%">
					<thead>
						<tr>
							<th style="width: 15%"><fmt:message key="cartridge.display.name" /></th>
							<th style="width: 80px"><fmt:message key="cartridge.version" /></th>
							<th><fmt:message key="cartridge.description" /></th>
							<th style="width: 15%"><fmt:message key="action" /></th>
						</tr>
					</thead>
					<tbody>

						<%
							for (Cartridge cartridge : cartridges) {
								boolean subscribed = "SUBSCRIBED".equals(cartridge.getStatus());
						%>

						<tr>
							<td><%=(cartridge.getDisplayName() != null ? cartridge.getDisplayName() : "")%></td>
							<td><%=(cartridge.getVersion() != null ? cartridge.getVersion() : "")%></td>
							<td><%=(cartridge.getDescription() != null ? cartridge.getDescription() : "")%></td>
							<td>
							<% if (!subscribed) { %>
								<a href="./subscribe.jsp?cartridgeType=<%=cartridge.getCartridgeType()%>&cartridgeProvider=<%=cartridge.getProvider()%>&multiTenant=<%=cartridge.getMultiTenant()%>&item=<%=item%>"
									style="background-image: url(images/subscribe.gif);" class="icon-link"> <fmt:message key="subscribe" /></a>
							<% } else { %>
								<fmt:message key="already.subscribed" />
							<% } %>
							</td>
						</tr>
						<%
							}
						%>
					</tbody>
				</table>
			</form>
			<p>&nbsp;</p>
			<carbon:paginator pageNumber="<%=pageNumberInt%>" numberOfPages="<%=numberOfPages%>" page="available_cartridges.jsp"
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
	</div>
	<script type="text/javascript">
		alternateTableRows('cartridgesTable', 'tableEvenRow', 'tableOddRow');
	</script>
</fmt:bundle>
