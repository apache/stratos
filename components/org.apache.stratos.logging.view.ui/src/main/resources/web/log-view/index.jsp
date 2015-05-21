<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements. See the NOTICE file
distributed with this work for additional information
regarding copyright ownership. The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied. See the License for the
specific language governing permissions and limitations
under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.apache.stratos.logging.view.ui.LogViewerClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogInfo" %>
<%-- <%@ page --%>
<!-- import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent"%> -->
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogInfo" %>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogEvent" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent" %>
<%@ page import="java.util.regex.Matcher" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.TimeZone" %>
<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>View System Logs</title>
    <script type="text/JavaScript">
        function refresh(time) {
            setTimeout("location.reload(true);", time);
        }
    </script>
</head>
<body onload="JavaScript:refresh(120000);">
<%!
    private boolean isArchiveFile(String fileName) {
        String archivePattern = "[a-zA-Z]*\\.log";
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
    LogEvent[] events = null;
    String type;
    String keyword;
    String action;
    boolean showLogFiles;
    String pageNumberStr = request.getParameter("pageNumber");
    String pageIndexNumberStr = request.getParameter("pageIndexNumber");
    String showMaxStr = request.getParameter("showMax");
    int pageNumber = 0;
    int pageIndexNumber = 0;
    int numberOfIndexPages = 0;
    int numberOfPages = 0;
    int noOfRows = 0;
    boolean showMax = false;
    boolean isSuperTenant = CarbonUIUtil.isSuperTenant(request);
    boolean isManager = false;
    String tenantDomain = request.getParameter("tenantDomain");
    String serviceName = request.getParameter("serviceName");
    String serviceNames[];
    LogInfo[] logInfo = null;
    PaginatedLogInfo paginatedLogInfo = null;
    PaginatedLogEvent paginatedLogEvents = null;
    String parameter = "";
    String indexParameter = "";
    boolean isValidTenant = true;
    String collapseAdv = request.getParameter("collapse");
    boolean isCollapse = Boolean.parseBoolean(collapseAdv);
    showMax = Boolean.parseBoolean(showMaxStr);
    try {
        pageNumber = Integer.parseInt(pageNumberStr);
    } catch (NumberFormatException ignored) {
        // page number format exception
    }
    try {
        pageIndexNumber = Integer.parseInt(pageIndexNumberStr);
    } catch (NumberFormatException ignored) {
        // page number format exception
    }
    try {
        type = CharacterEncoder.getSafeText(request.getParameter("type"));
        type = (type == null) ? "" : type;
        if (tenantDomain == null) {
            tenantDomain = "";
        }
        if (serviceName == null) {
            serviceName = "";
        }
        //tenantDomain = (tenantDomain == null) ? "" : tenantDomain;
        keyword = CharacterEncoder.getSafeText(request.getParameter("keyword"));
        keyword = (keyword == null) ? "" : keyword;
        action = CharacterEncoder.getSafeText(request.getParameter("action"));
        logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);
        isValidTenant = logViewerClient.isValidTenant(tenantDomain);
        if (isValidTenant) {
            paginatedLogEvents = logViewerClient.getPaginatedLogEvents(pageNumber, type,
                    keyword, tenantDomain, serviceName);
            paginatedLogInfo = logViewerClient.getLocalLogFiles(pageIndexNumber, tenantDomain, serviceName);
        }

        if (paginatedLogEvents != null) {
            noOfRows = paginatedLogEvents.getNumberOfPages() * 15;
            events = paginatedLogEvents.getLogInfo();
            numberOfPages = paginatedLogEvents.getNumberOfPages();
        }
        if (paginatedLogInfo != null) {
            logInfo = paginatedLogInfo.getLogInfo();
            numberOfIndexPages = paginatedLogInfo.getNumberOfPages();
        }
        isManager = logViewerClient.isManager();
        if (isManager) {
            serviceNames = logViewerClient.getServiceNames();
        } else {
            serviceNames = logViewerClient.getServiceNames();
        }
        showLogFiles = (logInfo != null);
        if (isManager) {
            if (isSuperTenant) {
                parameter = "type=" + type + "&keyword=" + keyword + "&serviceName=" + serviceName +
                        "&tenantDomain=" + tenantDomain + "&collapse=" + isCollapse;
                indexParameter = "type=" + type + "&keyword=" + keyword + "&showMax=" + true + "&serviceName=" + serviceName +
                        "&tenantDomain=" + tenantDomain + "&collapse=" + isCollapse;
            } else {
                parameter = "type=" + type + "&keyword=" + keyword + "&serviceName=" + serviceName +
                        "&collapse=" + isCollapse;
                indexParameter = "type=" + type + "&keyword=" + keyword + "&showMax=" + true + "&serviceName=" + serviceName +
                        "&collapse=" + isCollapse;
            }
        } else {
            parameter = "type=" + type + "&keyword=" + keyword;
            indexParameter = "type=" + type + "&keyword=" + keyword + "&showMax=" + true;
        }

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
                       topPage="true" request="<%=request%>"/>
    <div id="middle">
        <h2>
            <fmt:message key="system.logs"/>
        </h2>

        <div id="workArea">

            <%
                if (isManager) {
            %>

            <table border="0" class="styledLeft">
                <tbody>
                <tr>

                    <%
                        if (isCollapse) {
                    %>
                    <td class="middle-header" colspan="4"><a
                            class="icon-link"
                            style="background-image: url(images/minus.gif);"
                            href="javascript:showQueryPropertiesSearch()"
                            id="propertySymbolMaxSearch"></a> <fmt:message
                            key="adv.search"/></td>
                </tr>
                <tr id="propertyTableSearch" style="">

                    <%
                    } else {
                    %>
                    <td class="middle-header" colspan="4"><a
                            class="icon-link"
                            style="background-image: url(images/plus.gif);"
                            href="javascript:showQueryPropertiesSearch()"
                            id="propertySymbolMaxSearch"></a> <fmt:message
                            key="adv.search"/></td>
                </tr>
                <tr id="propertyTableSearch" style="display: none">
                    <%
                        }
                    %>


                    <td>
                        <table class="normal">
                            <td style="padding-right: 2px !important;">
                                <nobr>
                                    <fmt:message key="service.name"/>
                                </nobr>
                            </td>
                            <td style="padding-right: 0px !important;"><select
                                    name="serviceName" id="serviceName">
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
                                if (isSuperTenant) {
                            %>
                            <td style="padding-right: 2px !important;">
                                <nobr>
                                    <fmt:message key="tenant.domain"/>
                                </nobr>
                            </td>
                            <td style="padding-right: 2px !important;"><input
                                    value="<%=tenantDomain%>" id="tenantDomain"
                                    name="tenantDomain" size="20" type="text"></td>
                            <td><input type="hidden" id="tenantDomain"
                                       name="tenantDomain" value="<%=tenantDomain%>"/>
                            </td>
                            <%
                                }
                            %>


                        </table>

                    </td>
                </tr>

                </tbody>
            </table>
            <%
            } else {
            %>

            <input type="hidden" id="serviceName" name="serviceName"
                   value="<%=serviceName%>"/>

            <%
                }

                if (noOfRows > 40000) {
            %>
            <br/>
            <font color="red">Maximum log limit exceeded!!!. <br/>

                We only list 40 000 logs through the log viewer(your latest logs will be omitted in the log display),
                Please download the daily archived logs, for the full log report.
            </font> <br/>
            <%
                }
            %>
            <br/>
            <table border="0" class="styledLeft">
                <tbody>
                <tr>
                    <td>
                        <table class="normal">
                            <tr>
                                <td style="padding-right: 2px !important;">
                                    <nobr><fmt:message key="log.level"/></nobr>
                                </td>
                                <td><select class="log-select" id="logLevelID" return false>
                                    <%
                                        String[] logLevels = logViewerClient.getLogLevels();
                                        if (keyword != null && !keyword.equals("")) {
                                            type = "Custom";
                                    %>

                                    <option value="<%=type%>" selected="true"><%=type%>
                                    </option>

                                    <%
                                        }
                                        for (String logLevel : logLevels) {
                                            if (logLevel.equals(type)) {
                                    %>

                                    <option value="<%=logLevel%>" selected="true"><%=logLevel%>
                                    </option>

                                    <%
                                    } else {
                                    %>

                                    <option value="<%=logLevel%>"><%=logLevel%>
                                    </option>

                                    <%
                                            }
                                        }
                                    %>
                                </select></td>
                                <td style="padding-right: 2px !important;">
                                    <nobr>
                                        <fmt:message key="search.logs"/>
                                    </nobr>
                                </td>
                                <%
                                    if (isManager && isSuperTenant) {
                                %>
                                <td style="padding-right: 2px !important;"><input onkeypress="submitenter(event)"
                                                                                  value="<%=keyword%>" id="logkeyword"
                                                                                  size="20" type="text"></td>
                                <td style="padding-right: 2px !important;">
                                    <a
                                            class="icon-link"
                                            style="background-image: url(images/search.gif);"
                                            href="javascript:searchLogs()">
                                    </a>
                                </td>
                                <td><input type="hidden" id="keyWord"
                                           name="keyword" value="<%=keyword%>"/>

                                </td>
                                <%
                                } else if (isManager && !isSuperTenant) {

                                %>
                                <td style="padding-right: 2px !important;"><input
                                        onkeypress="submitenterNormalManager(event)"
                                        value="<%=keyword%>" id="logkeyword"
                                        size="20" type="text"></td>
                                <td style="padding-right: 2px !important;">
                                    <a
                                            class="icon-link"
                                            style="background-image: url(images/search.gif);"
                                            href="javascript:searchNormalManager()"></a>
                                </td>
                                <td><input type="hidden" id="keyWord"
                                           name="keyword" value="<%=keyword%>"/>

                                </td>
                                <%
                                } else if (!isManager) {
                                %>
                                <td style="padding-right: 2px !important;"><input onkeypress="submitenterNormal(event)"
                                                                                  value="<%=keyword%>" id="logkeyword"
                                                                                  size="20" type="text"></td>
                                <td style="padding-right: 2px !important;">
                                    <a
                                            class="icon-link"
                                            style="background-image: url(images/search.gif);"
                                            href="javascript:searchNormal()"
                                            ></a>
                                </td>
                                <td><input type="hidden" id="keyWord"
                                           name="keyword" value="<%=keyword%>"/>

                                </td>
                                <td style="width: 37%;"></td>
                                <td style="padding-right: 2px !important;">
                                    <nobr>
                                        <fmt:message key="current.time"/>
                                    </nobr>
                                </td>
                                <%
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("Z");
                                    String date = dateFormat.format(System.currentTimeMillis());
                                %>
                                <td>
                                    <%= "GMT " + date%>
                                </td>
                                <%

                                    }
                                %>

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

                        <table class="styledLeft">
                            <thead>
                            <tr>
                                <th><b><fmt:message key="log.type"/>
                                </b>
                                </th>
                                <th><b><fmt:message key="date"/>
                                </b>
                                </th>
                                <th colspan="2"><b><fmt:message key="log.message"/>
                                </b>
                                </th>
                            </tr>
                            </thead>
                            <%
                                if (!isValidTenant) { %>
                            <fmt:message key="invalid.tenant"/>

                            <%
                            } else {


                                if (events == null || events.length == 0 || events[0] == null) {
                            %>
                            <fmt:message key="no.logs"/>
                            <%
                            } else {
                                int index = 0;
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
                                    }
                                %>
                                <td border-bottom="gray" width="2%"><img
                                        style="margin-right: 10px;"
                                        src="<%=logViewerClient.getImageName(logMessage.getPriority().trim())%>">
                                </td>
                                <td>
                                    <nobr><%=logMessage.getLogTime()%>
                                    </nobr>
                                </td>
                                <td><%=CharacterEncoder.getSafeText(logMessage.getMessage())%>
                                </td>
                                <%
                                    String imgId = "traceSymbolMax" + index;
                                %>
                                <td><a
                                        class="icon-link"
                                        style="background-image: url(images/plus.gif);"
                                        href="javascript:showTrace(<%=index%>)"
                                        id="<%=imgId%>"></a> <fmt:message
                                        key="view.stack.trace"/></td>
                            </tr>

                            <%
                                String id = "traceTable" + index;
                                if (index % 2 != 0) {
                            %>
                            <tr id="<%=id%>" style="display: none">
                                        <%
										} else {
									%>

                            <tr id="<%=id%>" style="display: none" bgcolor="#eeeffb">
                                <%
                                    }
                                %>

                                <td colspan="4" width="100%">TID[<%=logMessage.getTenantId()%>]
                                    [<%=logMessage.getServerName()%>] [<%=logMessage.getLogTime()%>
                                    ] <%=logMessage.getPriority().trim()%> {<%=logMessage.getLogger()%>}
                                    - <%=CharacterEncoder.getSafeText(logMessage.getMessage())%>
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
                                              page="index.jsp" pageNumberParameterName="pageNumber"
                                              prevKey="prev" nextKey="next"
                                              parameters="<%= parameter%>"/>
                </tr>


                <%
                    if (showLogFiles) {
                %>
                <tr>
                    <%
                        if (showMax) {
                    %>
                    <td class="middle-header" colspan="2"><a
                            class="icon-link"
                            style="background-image: url(images/minus.gif);"
                            href="javascript:showQueryProperties()"
                            id="propertySymbolMax"></a> <fmt:message
                            key="archived.logs"/></td>
                    <%
                    } else {

                    %>
                    <td class="middle-header" colspan="2"><a
                            class="icon-link"
                            style="background-image: url(images/plus.gif);"
                            href="javascript:showQueryProperties()"
                            id="propertySymbolMax"></a> <fmt:message
                            key="archived.logs"/></td>
                    <%
                        }
                    %>
                </tr>
                <tr id="propertyTable" style="<%=(showMax) ? "" : "display:none"%>">


                    <td>


                        <table border="1" class="styledLeft">

                            <tbody>

                            <tr>
                                <td class="formRow">
                                    <table class="styledLeft">
                                        <thead>
                                        <tr>
                                            <th><b><fmt:message key="file.name"/> </b></th>
                                            <th><b><fmt:message key="date"/> </b></th>
                                            <th><b><fmt:message key="file.size"/> </b></th>
                                            <th><b><fmt:message key="action"/> </b></th>
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

                                            <td colspan="4"><fmt:message key="no.logs"/></td>
                                            <%
                                            } else {
                                                String logFile = logMessage.getLogName();
                                                String logDate = logMessage.getLogDate();
                                                String logSize = logMessage.getFileSize();
                                            %>

                                            <td><%=logFile%>
                                            </td>
                                            <td><%=logDate%>
                                            </td>
                                            <td><%=logSize%>
                                            </td>
                                            <td>
                                                <%
                                                    if (isArchiveFile(logFile)) {
                                                %>
                                                <a class="icon-link"
                                                   style="background-image: url(images/download.gif);"
                                                   onclick="startDownload()"
                                                   href="downloadgz-ajaxprocessor.jsp?logFile=<%=logFile%>&tenantDomain=<%=tenantDomain%>&serviceName=<%=serviceName%>"><fmt:message
                                                        key="download"/> </a>
                                                <%
                                                } else {
                                                %>
                                                <a class="icon-link"
                                                   style="background-image: url(images/download.gif);"
                                                   onclick="startDownload()"
                                                   href="download-ajaxprocessor.jsp?logFile=<%=logFile%>&tenantDomain=<%=tenantDomain%>&serviceName=<%=serviceName%>"><fmt:message
                                                        key="download"/> </a>
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
                                    <carbon:paginator pageNumber="<%=pageIndexNumber%>"
                                                      numberOfPages="<%=numberOfIndexPages%>"
                                                      page="index.jsp" pageNumberParameterName="pageIndexNumber"
                                                      prevKey="prev" nextKey="next"
                                                      parameters="<%=indexParameter%>"/>
                                </td>
                            </tr>

                            </tbody>
                        </table>
                    </td>
                </tr>

                <%
                    }
                %>
                </tbody>
            </table>
        </div>
    </div>
</fmt:bundle>
</body>
</html>

