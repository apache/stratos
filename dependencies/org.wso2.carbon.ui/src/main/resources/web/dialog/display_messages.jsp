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
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.util.CharacterEncoder" %>

<script type="text/javascript">
    var msgId;
    <%
    if(CharacterEncoder.getSafeText(request.getParameter("msgId")) == null){
    %>
    msgId = '<%="MSG" + System.currentTimeMillis() + Math.random()%>';
    <%
    } else {
    %>
    msgId = '<%=CharacterEncoder.getSafeText(request.getParameter("msgId"))%>';
    <%
    }
    %>
</script>

<%
    //First checks whether there is a CarbonUIMessage in the request
    CarbonUIMessage carbonMessage = (CarbonUIMessage) session.getAttribute(CarbonUIMessage.ID);

    if(carbonMessage == null){
        carbonMessage = (CarbonUIMessage) request.getAttribute(CarbonUIMessage.ID);
    } else {
        session.removeAttribute(CarbonUIMessage.ID);
    }

    if (carbonMessage != null) {
        String message = carbonMessage.getMessage();
        String messageType = carbonMessage.getMessageType();
        if (message == null || message.equals("") || messageType == null) {
        } else {
            if (messageType.equals(CarbonUIMessage.INFO)) {
%>
            <script type="text/javascript">
                jQuery(document).ready(function() {
                    if (getCookie(msgId) == null) {
                        CARBON.showInfoDialog("<%= carbonMessage.getMessage()%>");
                        setCookie(msgId, 'true');
                    }                    
                });

            </script>
<%
            } else if (messageType.equals(CarbonUIMessage.WARNING)) {
%>
            <script type="text/javascript">
                jQuery(document).ready(function() {
                    if (getCookie(msgId) == null) {
                        CARBON.showWarningDialog("<%= carbonMessage.getMessage()%>");
                        setCookie(msgId, 'true');
                    }
                });
            </script>
<%
            } else if (messageType.equals(CarbonUIMessage.ERROR)) {
%>
            <script type="text/javascript">
                jQuery(document).ready(function() {
                    if (getCookie(msgId) == null) {
                        CARBON.showErrorDialog("<%= carbonMessage.getMessage()%>");
                        setCookie(msgId, 'true');
                    }
                });
            </script>
<%
            }
        }
    }
%>

