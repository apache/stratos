<%--
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
 --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="org.json.JSONObject" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Invoice" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Payment" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="java.util.Date" %>
<%
    String amount = request.getParameter("amount");
    String transactionId = request.getParameter("transactionId");
    String invoiceId = request.getParameter("invoiceId");

    Payment payment = new Payment();

    Invoice invoice = new Invoice();
    invoice.setId(Integer.parseInt(invoiceId));
    payment.setInvoice(invoice);

    //We set the transaction id as the description
    payment.setDescription(transactionId);
    payment.setDate(new Date(System.currentTimeMillis()));

    try{
        int paymentId = BillingUtil.addPaymentDetails(config, session, payment, amount);

        JSONObject obj = new JSONObject();
        obj.put("paymentId", paymentId);
        if(paymentId>0){
            obj.put("status", "success");
            obj.put("amount", amount);
            obj.put("transactionId", transactionId);
            obj.put("invoiceId", invoiceId);
        }else{
            obj.put("status", "fail");
        }
        out.write(obj.toString());
    }catch (Exception e){
        e.printStackTrace();
    }
%>