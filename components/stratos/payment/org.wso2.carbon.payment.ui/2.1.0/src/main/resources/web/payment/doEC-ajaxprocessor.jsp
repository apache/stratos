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
<%@ page import="org.wso2.carbon.payment.stub.dto.TransactionResponse" %>
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

    String token = request.getParameter("token");
    String payerId = request.getParameter("payerId");
    String amount = request.getParameter("amount");
    String tenantDomain = request.getParameter("tenantDomain");

    TransactionResponse tr = new TransactionResponse();
    try{
        tr = paymentService.doExpressCheckout(token, payerId, amount, tenantDomain);
    }catch (Exception e){
        System.out.println("Error occurred while getting the transaction response: " + e.getMessage());
        e.printStackTrace();
    }

    JSONObject obj = new JSONObject();
    obj.put("ack", tr.getAck());
    obj.put("token", tr.getToken());
    obj.put("transactionID", tr.getTransactionId());
    obj.put("paymentStatus", tr.getPaymentStatus());
    obj.put("pendingReason", tr.getPendingReason());
    if(tr.getError()!=null){
        obj.put("error", tr.getError().getLongMessage());
    }
    out.write(obj.toString());
    
%>