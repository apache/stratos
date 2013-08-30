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
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogEvent"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent"%>
<%@ page import="java.text.SimpleDateFormat" %>
<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<script type="text/JavaScript">
	function refresh(time) {
		setTimeout("location.reload(true);", time);
	}
</script>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>View Application Logs</title>
</head>
<body onload="JavaScript:refresh(120000);">
	
	<%
		String backendServerURL = CarbonUIUtil
				.getServerURL(config.getServletContext(), session);
		ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
				.getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
		String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
		LogViewerClient logViewerClient;
		LogEvent[] events = null;
		String type;
		String keyword;
		String action;
		String pageNumberStr = request.getParameter("pageNumber");
		int pageNumber = 0;
		int numberOfPages = 0;
		PaginatedLogEvent paginatedLogEvents = null;
		String parameter = "";
        boolean isSuperTenant = CarbonUIUtil.isSuperTenant(request);
        boolean isManager = false;
        String tenantDomain = request.getParameter("tenantDomain");
        String serviceName = request.getParameter("serviceName");
        String showMaxStr = request.getParameter("showMax");
        String serviceNames[];
        boolean isValidTenant = true;
        boolean showMax= false;
        showMax = Boolean.parseBoolean(showMaxStr);
        try {
			pageNumber = Integer.parseInt(pageNumberStr);
		} catch (NumberFormatException ignored) {
			// page number format exception
		}
		String appName;
		String applicationNames[] = null;
		try {
			type = CharacterEncoder.getSafeText(request.getParameter("type"));
			type = (type == null) ? "":type;
			keyword = CharacterEncoder.getSafeText(request.getParameter("keyword"));
			keyword = (keyword == null )? "":keyword;
			action = CharacterEncoder.getSafeText(request.getParameter("action"));
			appName = request.getParameter("appName");
			logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);
            isValidTenant = logViewerClient.isValidTenant(tenantDomain);
            if (tenantDomain == null) {
                tenantDomain = "";
            }
            if (serviceName == null) {
                serviceName = "";
            }
            if(isValidTenant) {
                applicationNames = logViewerClient.getApplicationNames(tenantDomain, serviceName);
                if (appName == null && applicationNames !=null  && applicationNames.length > 0) {
                    appName = applicationNames[0];
                }
                if (applicationNames == null) {
                    applicationNames = new String[]{"No applications"};
                    appName = applicationNames[0];
                } else if(applicationNames != null && applicationNames.length == 0) {
                    applicationNames = new String[]{"No applications"};
                    appName = applicationNames[0];
                }
                paginatedLogEvents = logViewerClient.getPaginatedApplicationLogEvents(pageNumber,type,keyword,appName, tenantDomain, serviceName);
            }
            if (applicationNames == null) {
                applicationNames = new String[]{"No applications"};
                appName = applicationNames[0];
            }
            isManager = logViewerClient.isManager();
            serviceNames = logViewerClient.getServiceNames();
			if (paginatedLogEvents != null) {
				events = paginatedLogEvents.getLogInfo();
				numberOfPages = paginatedLogEvents.getNumberOfPages();
			}
			
			parameter = "type=" + type + "&keyword=" + keyword+ "&appName="+appName;
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

		<carbon:breadcrumb label="app.logs"
			resourceBundle="org.wso2.carbon.logging.ui.i18n.Resources"
			topPage="true" request="<%=request%>" />
		<div id="middle">
			<h2>
				<fmt:message key="app.logs" />
			</h2>
			<div id="workArea">



            <table border="0" class="styledLeft">
                <tbody>
                <tr>
                    <td>
                        <table class="normal">
                            <tr>
                                <td style="padding-right: 2px !important;">
                                    <nobr><fmt:message key="log.level"/></nobr>
                                </td>
                                <td><select class="log-select" id="logLevelID" return false;">
                                    <%
                                        String[] logLevels = logViewerClient.getLogLevels();
                                        if (keyword != null && !keyword.equals("")) {
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
                                    </select></td>
                                <td style="padding-right: 2px !important;">
                                    <nobr><fmt:message key="application.name"/></nobr>
                                </td>
                                <td ><select
                                        name="appName" id="appName" return false;">
                                    <%
                                        for (String name : applicationNames) {
                                    %>
                                    <%
                                        if (name.equals(appName)) {
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
                                <td style="padding-right: 2px !important;"><nobr>
                                    <fmt:message key="search.logs" />
                                </nobr>
                                </td>
                                <td style="padding-right: 2px !important;">
                                    <input onkeypress="appSubmitenter(event)" value="<%=keyword%>"
                                           class="log-select" size="20" id="logkeyword" type="text"></td>
                                <td style="padding-right: 2px !important;">
                                    <a
                                            class="icon-link"
                                            style="background-image: url(images/search.gif);"
                                            href="javascript:searchAppLogs()"
                                            ></a>
                                </td>
                                <!--td style="padding-right: 2px !important;"><input type="button"
                                                                                 value="Search"
                                                                                 onclick="javascript:searchAppLogs(); return false;"
                                                                                 class="button"-->
                                </td>
                                <td><input type="hidden" id="logkeyword"
                                           name="KeyWord" value="<%=keyword%>" />
                                </td>
                                <td style="width: 17%;"></td>
                                <td style="padding-right: 2px !important;"><nobr>
                                    <fmt:message key="current.time" />
                                </nobr>
                                </td>
                                <%
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("Z");
                                    String date = dateFormat.format(System.currentTimeMillis());
                                %>
                                <td>
                                    <%= "GMT " + date%>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
                </tbody>
            </table>



                   <br/>
			<table border="1" class="styledLeft">
		
				<tbody>

					<tr>
						<td class="formRow">

							<table  class="styledLeft">

                                <%
                                    if(!isValidTenant) { %>
                                <fmt:message key="invalid.tenant" />

                                <%} else {


								if (events == null || events.length == 0 || events[0] == null) {
							%>
								 <fmt:message key="no.logs" /> 
							<%
 								} else {
 										int index = 0;
 										for (LogEvent logMessage : events) {
 											index++;
 											if (index % 2 != 0) {
 							%>
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
								<tr>
									<%
										} else {
									%>
								
								<tr bgcolor="#eeeffb">
									<%
										}
									%>
								   <td border-bottom="gray" width="2%"><img
										style="margin-right: 10px;"
										src="<%=logViewerClient.getImageName(logMessage.getPriority().trim())%>">
									</td>
									<td><nobr><%=logMessage.getLogTime()%></nobr></td>
									<td><%=CharacterEncoder.getSafeText(logMessage.getMessage())%></td>
										<%
											String imgId = "traceSymbolMax" + index;
										%>
									<td><a
											class="icon-link"
											style="background-image: url(images/plus.gif);"
											href="javascript:showTrace(<%=index%>)"
											id="<%=imgId%>"></a> <fmt:message
												key="view.stack.trace" /></td>
								</tr>
								
							<%
																String id = "traceTable" + index;
																			if (index % 2 != 0) {
															%>
									<tr id="<%=id%>" style="display: none" >
									<%
										} else {
									%>
								
									<tr id="<%=id%>" style="display: none" bgcolor="#eeeffb">
									<%
										}
									%>

                                    <td colspan="4" width="100%">TID[<%=logMessage.getTenantId()%>] AppID[<%=logMessage.getAppName()%>] [<%=logMessage.getServerName()%>] [<%=logMessage.getLogTime()%>] <%=logMessage.getPriority().trim()%> {<%=logMessage.getLogger()%>} - <%=CharacterEncoder.getSafeText(logMessage.getMessage())%>
                                        <%=logMessage.getStacktrace()%><br/>
                                    </td>
									</tr>
							<%
								}
									}
                                }
							%>
							
							</table>
							 <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                      page="application_log_viewer.jsp" pageNumberParameterName="pageNumber"
                      prevKey="prev" nextKey="next"
                      parameters="<%= parameter%>"/>
					</tr>
					
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