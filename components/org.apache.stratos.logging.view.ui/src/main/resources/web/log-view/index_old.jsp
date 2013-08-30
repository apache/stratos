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
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.stratos.logging.view.ui.LogViewerClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.logging.view.ui.LogViewerStub.LogMessage" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>


<%
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    LogViewerClient logViewerClient;
    String type;
    String keyword;
    String action;
    LogMessage[] logMessages;
    try {
        logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);

        type = request.getParameter("type");
        keyword = request.getParameter("keyword");
        action = request.getParameter("action");
        if (action != null && action.equals("clear-logs")) {
            logViewerClient.cleaLogs();
        }
        logMessages = logViewerClient.getLogs(type, keyword);

    } catch (Exception e) {
        CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
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
        <h2><fmt:message key="system.logs"/></h2>

        <div id="workArea">
           
                    <table border="0" class="styledLeft">
                        <tbody>
                        <tr>
                            <td>
                            <table class="normal">
                            <tr>
                            <td><fmt:message key="view"/></td>
                            <td><select class="log-select" id="logLevelID"
                                        onchange="javascript:viewSingleLogLevel(); return false;">
                                <%
                                    String[] logLevels = logViewerClient.getLogLevels();
                                    if(keyword != null){
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

                                <% }
                                }
                                %>
                            </select></td>
                            <td style="width: 100%;"></td>
                            <td>
                                <nobr><fmt:message key="search.logs"/></nobr>
                            </td>
                                <td style="padding-right: 2px !important;">
                                    <input onkeypress="submitenter(event)" value="" class="log-select"
                                        size="40" id="logkeyword" type="text"></td>
                                <td style="padding-left: 0px !important;"><input type="button"
                                                                                 value="Search"
                                                                                 onclick="javascript:searchLog(); return false;"
                                                                                 class="button">
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
                            <table class="styledLeft">


                        <%
                                int index = -1;
                                for (LogMessage logMessage : logMessages) {
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

                                String logLevel = logMessage.getType();
                                if (logLevel == null || logLevel.equals("")) {
                            %>
                                <td border-bottom="gray" width="2%"></td>
                            <%
                            } else {
                            %>
                                <td border-bottom="gray" width="2%">
                                    <img style="margin-right: 10px;"
                                                              src="<%=logViewerClient.getImageName(logMessage.getType())%>">
                            </td>
                            <%
                                }
                            %>
                            <td>
                                <%
                                    if(logMessage.getLogMessage().equalsIgnoreCase("NO_LOGS")) {
                                %>
                                        <fmt:message key="no.logs"/>
                                <%
                                    } else {
                                %>
                                        <%=logMessage.getLogMessage()%>
                                <% } %>
                            </td>
                        </tr>

                        <%
                            }
                        %>
                         </table>
                        </td>
                        </tr>

                    <tr>
                     <td class="buttonRow">
                            <input onclick="javascript:clearLogEntries('<fmt:message key="clear.all.log.message"/>?');return false;"
                           value="<fmt:message key="clear.all"/>" class="button"
                           name="clearLogs" type="button">
                     </td>
                    </tr>

                        </tbody>
                    </table>
                </div>
    </div>
</fmt:bundle>
