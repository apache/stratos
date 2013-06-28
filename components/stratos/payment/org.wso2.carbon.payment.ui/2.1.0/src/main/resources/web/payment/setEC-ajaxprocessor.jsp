<%--
  ~  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
  ~
  ~  Licensed under the Apache License, Version 2.0 (the "License");
  ~  you may not use this file except in compliance with the License.
  ~  You may obtain a copy of the License at
  ~
  ~        http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~  Unless required by applicable law or agreed to in writing, software
  ~  distributed under the License is distributed on an "AS IS" BASIS,
  ~  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~  See the License for the specific language governing permissions and
  ~  limitations under the License.
  --%>
<%@page contentType="text/html" pageEncoding="UTF-8"
        import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.payment.stub.dto.ECResponse" %>
<%@ page import="org.wso2.carbon.payment.ui.client.PaymentServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().
            getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    PaymentServiceClient paymentService = new PaymentServiceClient(configContext, serverURL, cookie);
    String successUrl = request.getParameter("successUrl");
    String cancelUrl = request.getParameter("cancelUrl");
    String amount = request.getParameter("amount");

    try{
        ECResponse resp = paymentService.initExpressCheckout(amount, successUrl, cancelUrl);

        String token = resp.getToken();

        JSONObject obj = new JSONObject();
        obj.put("ack", resp.getAck());
        obj.put("token", resp.getToken());
        if(resp.getError()!=null){
            obj.put("error", resp.getError().getLongMessage());
        }
        out.write(obj.toString());
    }catch (Exception e){
        System.out.println("Error occurred while trying to init the payment process: " + e.getMessage());
        e.printStackTrace();
    }

%>