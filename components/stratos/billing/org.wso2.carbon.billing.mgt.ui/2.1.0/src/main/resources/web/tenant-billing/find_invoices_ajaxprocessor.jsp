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
<%@ page contentType="text/html" pageEncoding="UTF-8"%>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.BillingPeriod" %>
<%@ page import="org.json.JSONArray" %>
<%@ page import="org.json.JSONObject" %>
<%
    String tenantDomain = request.getParameter("tenantDomain");
    BillingPeriod[] billingPeriods = BillingUtil.getAvailableBillingPeriodsBySuperTenant(config, session, tenantDomain);

    try{
        JSONArray invoiceArray = new JSONArray();

        if(billingPeriods!=null && billingPeriods.length>0){
            for(BillingPeriod bp : billingPeriods){

                JSONObject obj = new JSONObject();
                obj.put("invoiceId", bp.getInvoiceId());
                obj.put("invoiceDate", bp.getInvoiceDate().toString());

                invoiceArray.put(obj);
            }
        }

        JSONObject invoicesObj = new JSONObject();
        invoicesObj.put("invoices", invoiceArray);
        out.write(invoicesObj.toString());
    }catch(Exception e){
        e.printStackTrace();
    }
%>