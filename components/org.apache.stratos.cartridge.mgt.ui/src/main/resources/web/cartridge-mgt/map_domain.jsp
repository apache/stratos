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
<%@ page import="java.text.SimpleDateFormat"%>
<%@ page import="java.util.ResourceBundle"%>
<jsp:include page="../dialog/display_messages.jsp" />


<%
	response.setHeader("Cache-Control", "no-cache");
	String cartridgeAlias = request.getParameter("cartridge_alias");
	String domain = request.getParameter("domain");

	if (cartridgeAlias == null || cartridgeAlias.trim().length() == 0) {
		response.setStatus(500);
		CarbonUIMessage uiMsg = new CarbonUIMessage(
				CarbonUIMessage.ERROR, "Cartridge Alias Not Specified");
		session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp" />
<%
	return;
	}
%>
<fmt:bundle basename="org.apache.stratos.cartridge.mgt.ui.i18n.Resources">
	<carbon:breadcrumb label="mapdomain" resourceBundle="org.apache.stratos.cartridge.mgt.ui.i18n.Resources" topPage="true"
		request="<%=request%>" />
	<div id="middle">
		<h2>
			<fmt:message key="mapdomain.to" />
			<%=cartridgeAlias%></h2>
		<div id="workArea">
			<form id="mapDomainForm" name="mapDomain">
				<input name="cartridge_alias" type="hidden" value="<%=cartridgeAlias%>" />
				<table id="tblMapDomain" width="100%" class="styledLeft">
					<thead>
						<tr>
							<th><fmt:message key="mapdomain.information" /></th>
						</tr>
					</thead>
					<tbody>
						<tr>
							<td class="nopadding">
								<table class="normal-nopadding" cellspacing="0">
									<tbody>
										<tr>
											<td style="width: 30%"><label>Cartridge Alias</label><span
												class="required">*</span></td>
											<td><span><%=cartridgeAlias%></span></td>
										</tr>
										<tr>
											<td><label for="domain">Domain</label><span
												class="required">*</span></td>
											<td><input id="domain" name="domain" type="text"
												style="width: 250px" maxlength="300"
												value="<%=(domain != null ? domain : "")%>" /></td>
										</tr>
									</tbody>
								</table>
							</td>
						</tr>
						<tr id="buttonRow">
							<td class="buttonRow"><input id="mapDomainSave"
								type="submit" class="button" value="Save"> <input
								id="mapDomainCancelButton" type="button" class="button"
								value="Cancel" onclick="cancelMapDomain();"></td>
						</tr>
					</tbody>
				</table>
			</form>
		</div>
	</div>

	<script type="text/javascript">
		jQuery(document).ready(
				function() {
					setStratosFormSubmitFunction("#mapDomainForm", validate,
							"map_domain_ajaxprocessor.jsp",
							"subscribed_cartridges.jsp", "#mapDomainSave");
				});

		function validate() {
			if (jQuery("#domain").val().length == 0) {
				CARBON.showErrorDialog('Please enter a value for domain.');
				return false;
			}
			return true;
		}
		
		function cancelMapDomain() {
			location.href = "subscribed_cartridges.jsp";
		}
	</script>
	<script type="text/javascript" src="js/jquery.blockUI-1.33.js"></script>
	<script type="text/javascript" src="js/common.js"></script>
</fmt:bundle>
