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
<%@page import="org.apache.commons.httpclient.HttpMethod"%>
<%@page import="org.apache.axis2.transport.http.HTTPConstants"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>


<%@page import="org.wso2.carbon.utils.ServerConstants"%><jsp:include page="../dialog/display_messages.jsp"/>

<fmt:bundle basename="org.wso2.carbon.i18n.Resources">

<%
                Object param = session.getAttribute("authenticated");
				String passwordExpires = (String) session
						.getAttribute(ServerConstants.PASSWORD_EXPIRATION);
				boolean loggedIn = false;
				if (param != null) {
					loggedIn = (Boolean) param;
				}
				boolean serverAdminComponentFound = CarbonUIUtil
						.isContextRegistered(config, "/server-admin/");
				
				if (CharacterEncoder.getSafeText(request.getParameter("skipLoginPage"))!=null){
					response.sendRedirect("../admin/login_action.jsp");
					return;
				}
%>
    <div id="middle">
        <%
            String serverName = CarbonUIUtil
        						.getServerConfigurationProperty("Name");
        %>
        <h2>
            <fmt:message key="carbon.server.home">
                <fmt:param value="<%= serverName%>"/>
            </fmt:message>
        </h2>

        <p>
            <fmt:message key="carbon.console.welcome">
                <fmt:param value="<%= serverName%>"/>
            </fmt:message>
        </p>

        <p>&nbsp;</p>

        <div id="workArea">
        <div id="systemInfoDiv">
            <%
                if (loggedIn && passwordExpires != null) {
            %>
                 <div class="info-box"><p>Your password expires at <%=passwordExpires%>. Please change by visiting <a href="../user/change-passwd.jsp?isUserChange=true&returnPath=../admin/index.jsp">here</a></p></div>
            <%
                }
            				if (loggedIn && serverAdminComponentFound) {
            %>
            <div id="result"></div>
            <script type="text/javascript">
                jQuery.noConflict();
                var refresh;
                function refreshStats() {
                    var url = "../server-admin/system_status_ajaxprocessor.jsp";
                    var data = null;
                    try {
                        jQuery.ajax({
                            url: "../admin/jsp/session-validate.jsp",
                            type: "GET",
                            dataType: "html",
                            data: data,
                            complete: function(res, status){
                                if (res.responseText.search(/----valid----/) != -1) {
                                    jQuery("#result").load(url, null, function (responseText, status, XMLHttpRequest) {
                                        if (status != "success") {
                                            stopRefreshStats();
                                        }
                                    });
                                } else {
                                    stopRefreshStats();
                                }
                            },error: function(res, status, error){
                            	stopRefreshStats();
                            }
                        });
                    } catch (e) {
                    	stopRefreshStats();
                    }
                }
                function stopRefreshStats() {
                    if (refresh) {
                        clearInterval(refresh);
                    }
                }
                try {
                    jQuery(document).ready(function() {
                        refreshStats();
                        if (document.getElementById('systemInfoDiv').style.display == '') {
                            refresh = setInterval("refreshStats()", 6000);
                        }
                    });
                } catch (e) {
                } // ignored
            </script>
            <%
                }
            %>
        </div>
        </div>
    </div>
</fmt:bundle>
