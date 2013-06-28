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
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.MultitenancyInvoice" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Payment" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Invoice" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Cash" %>
<%@ page import="java.util.Date" %>
<%
    int invoiceId = Integer.parseInt(request.getParameter("invoiceId"));
    String amount = request.getParameter("amount").trim();
    String description = request.getParameter("description").trim();

    //We are making the adjustment as a payment...
    Payment adjustment = new Payment();
    Invoice invoice = new Invoice();
    invoice.setId(invoiceId);
    adjustment.setInvoice(invoice);
    adjustment.setDescription(description);
    adjustment.setDate(new Date(System.currentTimeMillis()));

    int adjustmentId=0;
    try{
        adjustmentId = BillingUtil.makeAdjustment(config, session, adjustment,  amount);
    }catch(Exception e){
        e.printStackTrace();
    }

    session.setAttribute("adjustmentId", adjustmentId);


%>

<jsp:forward page="adjustments.jsp"/>