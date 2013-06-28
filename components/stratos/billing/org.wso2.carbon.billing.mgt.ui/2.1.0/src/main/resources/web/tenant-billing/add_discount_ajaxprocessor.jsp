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
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Invoice" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Payment" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="java.util.Date" %>
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.Discount" %>
<%@ page import="java.text.DateFormat" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%

    String tenantDomain = request.getParameter("tenantDomain");
    String percentageStr = request.getParameter("percentage");
    String amountStr = request.getParameter("amount");
    String startDateStr = request.getParameter("startDate");
    String endDateStr = request.getParameter("endDate");
    String discountType = request.getParameter("discountType");

    Discount discount = new Discount();
    
    if("percentage".equals(discountType)){
        discount.setPercentageType(true);
        discount.setPercentage(Float.parseFloat(percentageStr));
    }else if("amount".equals(discountType)){
        discount.setPercentageType(false);
        discount.setAmount(Float.parseFloat(amountStr));
    }

    DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
    discount.setStartDate(df.parse(startDateStr));
    if(endDateStr!=null){
        discount.setEndDate(df.parse(endDateStr));
    }
    
    boolean added = false;
    try{
        added = BillingUtil.addDiscount(config, session, discount, tenantDomain);
    }catch (Exception e){
        e.printStackTrace();
    }

    if(added){
        session.setAttribute("discountAdded", "true");
    }else{
        session.setAttribute("discountAdded", "false");
    }


%>

<jsp:forward page="discounts.jsp"/>