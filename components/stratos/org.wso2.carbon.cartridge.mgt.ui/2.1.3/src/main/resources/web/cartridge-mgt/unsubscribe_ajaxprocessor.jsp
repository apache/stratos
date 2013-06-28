<!--
 ~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 ~
 ~ WSO2 Inc. licenses this file to you under the Apache License,
 ~ Version 2.0 (the "License"); you may not use this file except
 ~ in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~    http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 -->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
    <%@ page import="org.wso2.carbon.cartridge.mgt.ui.CartridgeAdminClient" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.adc.mgt.dto.xsd.Cartridge" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.ResourceBundle" %>
<jsp:include page="../dialog/display_messages.jsp"/>


<%
    response.setHeader("Cache-Control", "no-cache");

	ResourceBundle bundle = ResourceBundle
        .getBundle(CartridgeAdminClient.BUNDLE, request.getLocale());

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
          (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    CartridgeAdminClient client;

    String cartridgeAlias = request.getParameter("cartridge_alias");
    
    try{
        client= new CartridgeAdminClient(cookie, backendServerURL, configContext,request.getLocale());
        client.unsubscribe(cartridgeAlias);
        //CarbonUIMessage.sendCarbonUIMessage("Successfully unsubscribed "  ,
        //                                                        CarbonUIMessage.INFO, request);
        String message = "Successfully unsubscribed";
%>
<span id="responseMsg"><%=message%></span>
<%
    }catch (Exception e) {
        response.setStatus(500);
        //CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR,  e.getMessage(), e);
        //session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<span id="responseMsg"><%=e.getMessage()%></span>
<%
    }
%>