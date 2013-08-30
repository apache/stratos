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
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogMessage"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>
<fmt:bundle basename="org.apache.stratos.logging.view.ui.i18n.Resources">
	<carbon:breadcrumb label="View Logs"
		resourceBundle="org.wso2.carbon.logging.ui.i18n.Resources"
		topPage="false" request="<%=request%>" />
<%
	String backendServerURL = CarbonUIUtil
			.getServerURL(config.getServletContext(), session);
	ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
			.getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

	String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
	LogViewerClient logViewerClient;
	String logIndex = "";
	String keyword;
	String action;
	String logFile;
	String type;
	String keyWordString = "";
	String serviceName = "WSO2 Stratos Manager";
	String tenantDomain = "";
	LogMessage[] logMessages;
	PaginatedLogMessage paginatedLogMsgs;
	String pageNumberStr = request.getParameter("pageNumber");
	logFile = request.getParameter("logFile");
	String parameter = "";
	if (pageNumberStr == null) {
		pageNumberStr = "0";
	}
	int pageNumber = 0;
	int numberOfPages = 0;
	try {
		pageNumber = Integer.parseInt(pageNumberStr);
	} catch (NumberFormatException ignored) {
		// page number format exception
	}
	String numberOfBottomLines = "";
	try {
		logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);
		logIndex = request.getParameter("logIndex");
		keyword = request.getParameter("keyword");
		serviceName = request.getParameter("serviceName");
		tenantDomain = request.getParameter("tenantDomain");
		keyWordString = (keyword == null) ? "" : keyword;
		keyWordString = (keyWordString.equals("null")) ? "" : keyWordString;
		serviceName = (serviceName == null) ? "WSO2 Stratos Manager" : serviceName;
		if (keyWordString.trim().equals("")) {
			keyword = null;
		}
		action = request.getParameter("action");
		type = request.getParameter("type");
		type = (type == null) ? "ALL" : type;
		tenantDomain = (tenantDomain == null) ? "" : tenantDomain;
		int start = (pageNumber * 200) + 1;
		int end = (pageNumber * 200) + 200;
		int maxLines = logViewerClient.getLineNumbers(logFile, tenantDomain,serviceName);
		int maxLenPerItr = (pageNumber + 1) * 1000;
		if (maxLines > 1000) {

		}
		String logIndex1 = Integer.toString(maxLines);
		if (logIndex != null && !logIndex.equals("null") && !logIndex.equals("")) {
			//int index = Integer.parseInt(logIndex);
			paginatedLogMsgs = logViewerClient.getPaginatedLogMessage(pageNumber, type,
					keyword, logFile, logIndex, maxLines, start, end, tenantDomain,serviceName);
		} else {
			paginatedLogMsgs = logViewerClient.getPaginatedBottomUpLogMessage(pageNumber,
					type, keyword, logFile, maxLines, start, end, tenantDomain,serviceName);
			//paginatedLogMsgs = logViewerClient.getPaginatedLogMessage(pageNumber, type, keyword, logFile, logIndex1, maxLines, start, end);	
		}
		logMessages = paginatedLogMsgs.getLogInfo();
		numberOfPages = paginatedLogMsgs.getNumberOfPages();
		if (logIndex != null && !logIndex.endsWith("null")) {
			numberOfBottomLines = logIndex;
		}
		parameter = "logFile=" + logFile + "&logIndex=" + logIndex + "&type=" + type
				+ "&keyword=" + keyword + "&tenantDomain=" + tenantDomain+ "&serviceName=" + serviceName;

	} catch (Exception e) {
		CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request,
				e);
%>
<script type="text/javascript">
               location.href = "../admin/error.jsp";
        </script>
<%
	return;
	}
%>



	<div id="middle">
		<h2>
			<fmt:message key="system.logs.view" />
		</h2>

		<div id="workArea">
		    <input type="hidden" id="serviceName" name="serviceName"
               value="<%=serviceName%>"/>
               <input type="hidden" id="logFile" name="logFile"
               value="<%=logFile%>"/>
                 <input type="hidden" id="tenantDomain" name="tenantDomain"
               value="<%=tenantDomain%>"/>
			<table border="0" class="styledLeft">
				<tbody>
					<tr>					 
						<td>
							<table class="normal-nopadding" >				
								<tr>
									<td><fmt:message key="view" />
									</td>
									<td><select class="log-select" id="logLevelID"
										onchange="javascript:viewSingleSysLogLevel(); return false;">
											<%
												String[] logLevels = logViewerClient.getLogLevels();
													if (keyword != null) {
														type = "Custom";
											%>

											<option value="<%=type%>" selected="true"><%=type%></option>

											<%
												}
													for (String logLevel : logLevels) {
														if (logLevel.equals(type)) {
											%>

											<option value="<%=logLevel%>" selected="true"><%=logLevel%></option>

											<%
												} else {
											%>

											<option value="<%=logLevel%>"><%=logLevel%></option>

											<%
												}
													}
											%>
									</select>
									</td>
									<td style="width: 100%;"></td>
									<td><nobr>
											<fmt:message key="search.logs" />
										</nobr></td>
									<td style="padding-right: 2px !important;"><input
										onkeypress="submitenter(event)" value="<%=keyWordString%>" class="log-select"
										size="30" id="keyword" type="text">
									</td>
									<td style="padding-left: 0px !important;"><input
										type="button" value="Search"
										onclick="javascript:searchTenantLog(); return false;" class="button">
									</td>
									<td style="width: 100%;"></td>
									<td style="width: 100%;"></td>
									<td style="width: 100%;"></td>
									<td class="leftCol-med"><nobr><fmt:message key="log.head" /></nobr> 
									</td>
									<td style="padding-left: 0px !important;"><carbon:tooltips image="images/trace.png" key='log.head.help' noOfWordsPerLine="6"/></td>
									<td style="padding-left: 0px !important;"><input onkeypress="submitenterbottomUp(event)" value="<%=numberOfBottomLines%>" id="logIndex" name="logIndex"
										size="10" type="text">
									</td>
									<td style="padding-left: 0px !important;"><input
										type="button" value="Search"
										onclick="javascript:searchLogBottomLogs(); return false;"
										class="button">
									
									</td>
								</tr>


							</table></td>
					</tr>
				</tbody>
			</table>

			<br />
<%-- 				  	<carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>" --%>
<%--                                   page="view.jsp" pageNumberParameterName="pageNumber" parameters="<%=parameter%>"/>   --%>
			<table border="1" class="styledLeft">
		
				<tbody>

					<tr>
						<td class="formRow">

							<table class="styledLeft">

								<thead>
									<tr>
										<th><b><fmt:message key="log.type" />
										</b>
										</th>
										<th><b><fmt:message key="log.message" />
										</b>
										</th>
									</tr>
								</thead>
								<%
									int index = -1;
										int i = 0;
										for (LogMessage logMessage : logMessages) {
											++index;
											i++;
											if (index % 2 != 0) {
								%>
								<tr>
									<%
										} else {
									%>
								
								<tr bgcolor="#eeeffb">
									<%
										}
												if (logMessage != null) {
													String logLevel = logMessage.getType();
													if (logLevel == null || logLevel.equals("")) {
									%>
									<td border-bottom="gray" width="2%"></td>
									<%
										} else {
									%>
									<td border-bottom="gray" width="2%"><img
										style="margin-right: 10px;"
										src="<%=logViewerClient.getImageName(logMessage.getType().trim())%>">
									</td>
									<%
										}
									%>
									<td>
										<%
											if (logMessage.getLogMessage().equalsIgnoreCase("NO_LOGS")) {
										%> <fmt:message key="no.logs" /> <%
 						} else {
 						%> <%=CharacterEncoder.getSafeText(logMessage.getLogMessage())%> <%
 						}
						 %>
									</td>
								</tr>

								<%
									}
										}
																	%>

							</table><%
								if (i >= 199) {
										%>
										
										<tr bgcolor="#eeeffb">
										<td> Only 200 log messages are displayed, please <a 
										href="download-ajaxprocessor.jsp?logFile=<%=logFile%>&tenantDomain=<%=tenantDomain%>&serviceName=<%=serviceName%>"><fmt:message
												key="download" /></a>  for more ...
										<td>
										</tr>
										
											<%
									}
								%>
						</td>
					</tr>
<%-- 					<tr><carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>" --%>
<%--                                   page="view.jsp" pageNumberParameterName="pageNumber" parameters="<%=parameter%>"/> <tr>  --%>
					
				</tbody>
			</table>
	
		</div>
	</div>
</fmt:bundle>

