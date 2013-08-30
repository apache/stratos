<!--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<%@ page import="org.apache.stratos.logging.view.ui.LogViewerClient"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page
	import="org.wso2.carbon.logging.view.stub.types.carbon.LogMessage"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogInfo"%>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent"%>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogInfo"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="java.util.regex.Matcher"%>
<%@ page import="java.util.regex.Pattern"%>
<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>

<script type="text/javascript">
function startDownload()
{
	var url='download-ajaxprocessor.jsp'; 
	//jQuery.get(url);
}
</script>
<%!
private boolean isArchiveFile (String fileName) {
	String archivePattern = "[a-zA-Z]*\\.gz";
	CharSequence inputStr = fileName;
	Pattern pattern = Pattern.compile(archivePattern);
	Matcher matcher = pattern.matcher(inputStr);
	return matcher.find();
}
%>
<%
	String backendServerURL = CarbonUIUtil
			.getServerURL(config.getServletContext(), session);
	ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
			.getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

	String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
	LogViewerClient logViewerClient;
	String action;
	LogMessage[] logMessages;
	LogInfo[] logInfo;
	String tenantDomain = "";
	String serviceName = "WSO2 Stratos Manager";
	boolean isLogsFromSyslog;
	PaginatedLogInfo paginatedLogInfo;
	boolean isStratosService = false;
	String pageNumberStr = request.getParameter("pageNumber");
	boolean isSTSyslog = false;
	String serviceNames[];
	boolean isManager = false;
	boolean showManager = false;
	if (pageNumberStr == null) {
		pageNumberStr = "0";
	}
	boolean showTenantDomain = false;
	int pageNumber = 0;
	int numberOfPages = 0;
	try {
		pageNumber = Integer.parseInt(pageNumberStr);
	} catch (NumberFormatException ignored) {
		// page number format exception
	}
	try {

		logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);
		action = request.getParameter("action");
		tenantDomain = request.getParameter("tenantDomain");
		serviceName = request.getParameter("serviceName");
		isLogsFromSyslog = logViewerClient.isDataFromSysLog(tenantDomain);
		isSTSyslog = logViewerClient.isSTSyslogConfig(tenantDomain);
		isManager = logViewerClient.isManager();
		
		serviceName = (serviceName == null) ? "WSO2 Stratos Manager" : serviceName;
		tenantDomain = (tenantDomain == null) ? "" : tenantDomain;
		serviceNames = logViewerClient.getServiceNames();
		isStratosService = logViewerClient.isStratosService();
		showTenantDomain = (isSTSyslog && isLogsFromSyslog && isStratosService);
		showManager = (isManager && isLogsFromSyslog);
		if (logViewerClient.isLogsConfigured(tenantDomain)) {
			paginatedLogInfo = logViewerClient.getPaginatedLogInfo(pageNumber,
					tenantDomain, serviceName);
			if (paginatedLogInfo != null) {
				logInfo = paginatedLogInfo.getLogInfo();
				numberOfPages = paginatedLogInfo.getNumberOfPages();
				//logMessages = logViewerClient.getTenentLogs();

				//logMessages = logViewerClient.getLogs(type, keyword);

				for (int i = 0; i < logInfo.length; i++) {
					String logFile = logInfo[i].getLogName();
					String logDate = logInfo[i].getLogDate();
					String logSize = logInfo[i].getLogDate();
				}
			}
			else {
					logViewerClient.getPaginatedLogInfo(pageNumber,
							tenantDomain, serviceName);
					CarbonUIMessage.sendCarbonUIMessage(
							"Please configure syslog in order to view tenant specific logs.",
							CarbonUIMessage.ERROR, request);
		%>
		<script type="text/javascript">
			               location.href = "../admin/error.jsp";
			        </script>
		<%
			return;
				}
			
		} else {
			logViewerClient.getPaginatedLogInfo(pageNumber,
					tenantDomain, serviceName);
			CarbonUIMessage.sendCarbonUIMessage(
					"Please configure syslog in order to view tenant specific logs.",
					CarbonUIMessage.ERROR, request);
%>
<script type="text/javascript">
	               location.href = "../admin/error.jsp";
	        </script>
<%
	return;
		}

	} catch (Exception e) {
		CarbonUIMessage.sendCarbonUIMessage(e.getLocalizedMessage(), CarbonUIMessage.ERROR, request,
				e);
%>
<script type="text/javascript">
               location.href = "../admin/error.jsp";
        </script>
<%
	return;
	}
%>

<fmt:bundle basename="org.apache.stratos.logging.view.ui.i18n.Resources">
	<carbon:breadcrumb label="system.logs"
		resourceBundle="org.wso2.carbon.logging.ui.i18n.Resources"
		topPage="true" request="<%=request%>" />
	<div id="middle">
		<h2>
			<fmt:message key="system.logs" />
		</h2>

		<div id="workArea">
			<br />
			<%
				if (isLogsFromSyslog) {
			%>
			<font color="blue"><fmt:message
					key="remote.log.location.information" />
			</font>
			<%
				} else {
			%>
			<font color="blue"><fmt:message
					key="local.log.location.information" />
			</font>
			<%
				}
			%>

			<br /> <br />
			<%
				if (showTenantDomain || showManager) {
			%>
			<table border="0" class="styledLeft">
				<tbody>
					<tr>
						<td>

							<table class="normal">
								<tr>
									<%
										if (showTenantDomain) {
									%>

									<td style="padding-right: 2px !important;"><nobr>
											<fmt:message key="tenant.domain" />
										</nobr>
									</td>
									<td style="padding-right: 2px !important;"><input
										value="<%=tenantDomain%>" id="tenantDomain"
										name="tenantDomain" size="20" type="text"></td>

									<td style="padding-left: 0px !important;"><input
										type="button" value="Get Tenant Logs"
										onclick="javascript:getTenantSpecificIndex(); return false;"
										class="button"></td>
									<%
										} else {
									%>
									<td><input type="hidden" id="tenantDomain"
										name="tenantDomain" value="<%=tenantDomain%>" />
									</td>
									<%
										}
												if (showManager) {
									%>
									<td style="width: 100%;"></td>
									<td style="width: 100%;"></td>
									<td style="width: 100%;"></td>
									<td style="padding-left: 0px !important;"><nobr>
											<fmt:message key="service.name" />
										</nobr>
									</td>
									<td style="width: 100%;"></td>
									<td style="padding-left: 0px !important;"><select
										name="serviceName" id="serviceName"
										onchange="getProductTenantSpecificIndex()">
											<%
												for (String name : serviceNames) {
											%>
											<%
												if (name.equals(serviceName)) {
											%>
											<option selected="selected" value="<%=name%>">
												<%=name%>
											</option>
											<%
												} else {
											%>
											<option value="<%=name%>">
												<%=name%>
											</option>
											<%
												}
											%>
											<%
												}
											%>

									</select></td>
									<%
										} else {
									%>
									<input type="hidden" id="serviceName" name="serviceName"
										value="<%=serviceName%>" />
									<%
										}
									%>
								</tr>


							</table></td>
					</tr>
				</tbody>
			</table>
			<%
				}
			%>
			<br /> <br />
			<carbon:paginator pageNumber="<%=pageNumber%>"
				numberOfPages="<%=numberOfPages%>" page="index.jsp"
				pageNumberParameterName="pageNumber" />
			<table border="1" class="styledLeft">
				<tbody>

					<tr>
						<td class="formRow">
							<table class="styledLeft">
								<thead>
									<tr>
										<th><b><fmt:message key="file.name" /> </b></th>
										<th><b><fmt:message key="date" /> </b></th>
										<th><b><fmt:message key="file.size" /> </b></th>
										<th><b><fmt:message key="action" /> </b></th>
									</tr>
								</thead>
								<%
									int index = -1;
										for (LogInfo logMessage : logInfo) {
											++index;
											if (index % 2 != 0) {
								%>
								<tr>
									<%
										} else {
									%>
								
								<tr bgcolor="#eeeffb">
									<%
										}
												if (logMessage.getLogName().trim().equalsIgnoreCase("NO_LOG_FILES")) {
									%>

									<td colspan="4"><fmt:message key="no.logs" /></td>
									<%
										} else {
													String logFile = logMessage.getLogName().replace("0_","");
													String logDate = logMessage.getLogDate().replace("0_","");
													String logSize = logMessage.getFileSize();
									%>

									<td><%=logFile%></td>
									<td><%=logDate%></td>
									<td><%=logSize%></td>
									<td>
									   <% if(!isArchiveFile(logFile)) {	%>
									   <a class="icon-link"
										style="background-image: url(images/view.gif);"
										href="view.jsp?logFile=<%=logFile%>&tenantDomain=<%=tenantDomain%>&serviceName=<%=serviceName%>"><fmt:message
												key="view" /> </a> 
									    <%
										}
									    %>
									    <% if(isArchiveFile(logFile)) {	%>
									    <a class="icon-link"
										style="background-image: url(images/download.gif);"
										onclick="startDownload()"
										href="downloadgz-ajaxprocessor.jsp?logFile=<%=logFile%>&tenantDomain=<%=tenantDomain%>&serviceName=<%=serviceName%>"><fmt:message
												key="download" /> </a>
									       <%
										} else {
									    %>
										<a class="icon-link"
										style="background-image: url(images/download.gif);"
										onclick="startDownload()"
										href="download-ajaxprocessor.jsp?logFile=<%=logFile%>&tenantDomain=<%=tenantDomain%>&serviceName=<%=serviceName%>"><fmt:message
												key="download" /> </a>
										   <%
										}
									    %>
									</td>

									<%
										}
									%>

								</tr>

								<%
									}
								%>
							</table>
						</td>
					</tr>
				</tbody>
			</table>
			<carbon:paginator pageNumber="<%=pageNumber%>"
			numberOfPages="<%=numberOfPages%>" page="index.jsp"
			pageNumberParameterName="pageNumber" />
		</div>
	</div>
</fmt:bundle>
