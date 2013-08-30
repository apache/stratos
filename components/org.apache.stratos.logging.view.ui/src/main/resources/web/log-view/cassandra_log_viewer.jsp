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
<%-- <%@ page --%>
<!-- 	import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent"%> -->
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>

<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent"%>
<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>View System Logs</title>
</head>
<body>
	<%
		String backendServerURL = CarbonUIUtil
				.getServerURL(config.getServletContext(), session);
		ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
				.getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
		String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
		LogViewerClient logViewerClient;
		LogEvent[] events;
		String tenantDomain = "";
		String serviceName = "WSO2 Stratos Manager";
		boolean isLogsFromSyslog;
		boolean isSTSyslog = false;
		String serviceNames[];
		boolean isManager = false;
		boolean showManager = false;
		String logIndex = request.getParameter("logIndex");
		String start = request.getParameter("start");
		
		String end = request.getParameter("end");
		boolean showTenantDomain = false;
		boolean isStratosService = false;
		tenantDomain = request.getParameter("tenantDomain");
		serviceName = request.getParameter("serviceName");
		Boolean isDateGiven=false;
		String priority = request.getParameter("priority");
		String logger = request.getParameter("logger");
		String keyWord = request.getParameter("keyword");
// 		LogEvent logs[];
		if (start != null && !start.equals("null")&& !start.equals("")) {
			isDateGiven=true;
		}
		int returnRows;
		logIndex = (logIndex == null) ? "20" : logIndex;
		tenantDomain = (tenantDomain == null) ? "" : tenantDomain;
		start = (start == null) ? "" : start;
		end = (end == null) ? "" : end;
		priority = (priority == null) ? "ALL" : priority;
		keyWord = (keyWord == null) ? "" : keyWord;
		logger = (logger == null) ? "" : logger;
		try {
			returnRows = (logIndex == null) ? 20 : Integer.parseInt(logIndex);
			logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);
			isLogsFromSyslog = logViewerClient.isDataFromSysLog(tenantDomain);
			isSTSyslog = logViewerClient.isSTSyslogConfig(tenantDomain);
			isManager = logViewerClient.isManager();
			serviceNames = logViewerClient.getServiceNames();;
			serviceName = (serviceName == null) ? "WSO2 Stratos Manager" : serviceName;
			tenantDomain = (tenantDomain == null) ? "" : tenantDomain;
			isStratosService = logViewerClient.isStratosService();
			showTenantDomain = (isSTSyslog && isLogsFromSyslog && isStratosService);
			showManager = (isManager && isLogsFromSyslog);
			events = logViewerClient.getSystemLogs(start, end,
					logger, priority, "", serviceName, "", Integer.parseInt(logIndex));
			System.out.println("events "+events);
			System.out.println("Return Rows "+logIndex);
			System.out.println("isDateGiven "+isDateGiven);
			System.out.println("start "+start+" : "+start);
			System.out.println("end "+end+" : "+end);
			System.out.println("Priority "+priority);
			System.out.println("Logger "+logger);
			System.out.println("Keyword "+keyWord);
		
			System.out.println();
			System.out.println();
			
			//	logs = logViewerClient.getAllLogs();
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
	<fmt:bundle basename="org.apache.stratos.logging.view.ui.i18n.Resources">

		<carbon:breadcrumb label="system.logs"
			resourceBundle="org.wso2.carbon.logging.ui.i18n.Resources"
			topPage="true" request="<%=request%>" />
		<div id="middle">
			<h2>
				<fmt:message key="system.logs" />
			</h2>
			<div id="workArea">
			
			<%
				if (showTenantDomain || showManager) {
			%>	<br /> <br />
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
			</table><br /> <br />
			<%
				}
			%>
			
				<table border="0" class="styledLeft">

					<tbody>
						<tr>
							<td class="middle-header" colspan="2"><fmt:message key="filter.system.logs" />
							</td>
						</tr>
						<tr>
							<td>
								<table class="normal-nopadding" width="100%">
									<tr>
										<td>
											<table width="50%">
												<tr>
													<td><fmt:message key="return.rows" />
													</td>
													<td><select class="log-select" id="logIndex"
														size="3">

															<%
													for (int i = 1; i < 201; i++) {
													if (returnRows == i) {
												%>
															<option value="<%=i%>" selected="true"><%=i%></option>
															<%
													} else {
												%>
															<option value="<%=i%>"><%=i%></option>
															<%
													}
														}
												%>
													</select>
													</br></td>
												</tr>
												<tr>

													<td><nobr>
															<fmt:message key="show.at.or.before" />
														</nobr></td>
													<td style="padding-right: 2px !important;">
											<%
												if (!isDateGiven) {
											%> <input type="radio" id="NowradioDate" name="radioDate" value="Now" checked>Now<br>
											 <nobr>  <input type="radio" name="radioDate" id="NotNowradioDate"
														value="<%=start+"_"+end%>"><input type="text"
														id="start" name="start" value="<%=start%>" size="8" /> - <input
														type="text" id=end name="end" value="<%=end%>"
														size="8" /><fmt:message key="date.format" /> <br> <fmt:message key="date.example" /></nobr> <br>  <%
 										} else {
 										
										 %> <input type="radio" id="NowradioDate"  name="radioDate" value="Now"> Now<br>
										<nobr>	<input type="radio" name="radioDate"  id="NotNowradioDate"
														value="<%=start+"_"+end%>" checked><input type="text"
														id="start" name="start" value="<%=start%>" size="8" /> - <input
														type="text" id="end" name="end" value="<%=end%>"
														size="8" /><fmt:message key="date.format" />  <br> <fmt:message key="date.example" /></nobr> <br><%
									
												}
											%>
													</td>


												</tr>

											</table>
										</td>
									</tr>
									<tr>
										<td class="middle-header" colspan="2"><a
											class="icon-link"
											style="background-image: url(images/plus.gif);"
											href="javascript:showQueryProperties()"
											id="propertySymbolMax"></a> <fmt:message
												key="advanced.properties" /></td>
									</tr>
									<tr id="propertyTable" style="display: none">
										<td>
											<table width="50%">
												<tr>
													<td><fmt:message key="logger" /></td>
													<td><input type="text" size="30" id="logger"
														name="logger" value="<%=logger%>" /></td>
												</tr>
												<tr>
													<td><fmt:message key="priority" /></td>
													<td><select class="log-select" id="logLevelID"
														onchange="javascript:viewSingleSysLogLevel(); return false;">

															<%
															String[] logLevels = logViewerClient.getLogLevels();
																	for (String logLevel : logLevels) {
																		if (logLevel.equals(priority)) {
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
													</select></td>
												</tr>
												<tr>
													<td><fmt:message key="keyword" /></td>
													<td><input type="text" id="keyword" name="keyword"
														value="<%=keyWord%>" /></td>
												</tr>

											</table>
										</td>
									</tr>
									<tr>
										<td style="padding-left: 0px !important;"><input
											type="button" value="Search"
											onclick="javascript:getFilteredLogs(); return false;"
											class="button">
										</td>
									</tr>
								</table></td>
									<br />
<%-- 				  	<carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>" --%>
<%--                                   page="view.jsp" pageNumberParameterName="pageNumber" parameters="<%=parameter%>"/>   --%>
				<%
				if (!isDateGiven) {
			%>
			<font color="blue"><fmt:message
					key="view.logs.within.the.past.thirty.mins" />
			</font>
			<%
				} else {
			%>
			<font color="blue"><fmt:message
					key="view.logs.between" /><%=start%> and <%=end%>
			</font>
			<%
				}
			%><br/>
			<table border="1" class="styledLeft">
		
				<tbody>

					<tr>
						<td class="formRow">

							<table  class="styledLeft">
							<thead>
									<tr>
										<th><b><fmt:message key="log.type" />
										</b>
										</th>
										<th><b><fmt:message key="date" />
										</b>
										</th>
										<th colspan="2"><b><fmt:message key="log.message" />
										</b>
										</th>
									</tr>
								</thead>
							<%
								if (events == null || events.length==0 || events[0] == null) {%>
								 <fmt:message key="no.logs" /> 
							<%} else {
								int index=0;
								for (LogEvent logMessage : events) {
									index++;
							    if (index % 2 != 0) {
								%>
								<tr>
									<%
										} else {
									%>
								
								<tr bgcolor="#eeeffb">
									<%
										}%>
								   <td border-bottom="gray" width="2%"><img
										style="margin-right: 10px;"
										src="<%=logViewerClient.getImageName(logMessage.getPriority().trim())%>">
									</td>
									<td><nobr><%=logMessage.getLogTime()%></nobr></td>
									<td><%=logMessage.getMessage()%></td>
										<%String imgId = "traceSymbolMax"+index; %>
									<td><a
											class="icon-link"
											style="background-image: url(images/plus.gif);"
											href="javascript:showTrace(<%=index%>)"
											id="<%=imgId%>"></a> <fmt:message
												key="view.stack.trace" /></td>
								</tr>
								
							<%String id = "traceTable"+index;   if (index % 2 != 0) {
								%>
									<tr id="<%=id%>" style="display: none" >
									<%
										} else {
									%>
								
									<tr id="<%=id%>" style="display: none" bgcolor="#eeeffb">
									<%
									}%>
								
									<td colspan="4" width="100%">[<%=logMessage.getServerName()%>] [<%=logMessage.getLogTime()%>] <%=logMessage.getPriority().trim()%> {<%=logMessage.getLogger()%>} - <%=logMessage.getMessage()%> 
										<%=logMessage.getStacktrace()%><br/>
									</td>
									</tr>
							<%}
								}
							%>
							
							</table>
					</tr>
<%-- 					<tr><carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>" --%>
<%--                                   page="view.jsp" pageNumberParameterName="pageNumber" parameters="<%=parameter%>"/> <tr>  --%>
					
				</tbody>
			</table>
						</tr>
					</tbody>
				</table>
			</div>
		</div>
	</fmt:bundle>
</body>
</html>