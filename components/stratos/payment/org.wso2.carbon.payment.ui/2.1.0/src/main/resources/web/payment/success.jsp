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
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.payment.stub.dto.Address" %>
<%@ page import="org.wso2.carbon.payment.stub.dto.ECDetailResponse" %>
<%@ page import="org.wso2.carbon.payment.stub.dto.Payer" %>
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

    String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
    adminConsoleURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("carbon"));
    String tenantDomain = (String) session.getAttribute("tenantDomain");
    String cancelUrl = adminConsoleURL + "t/" + tenantDomain + "/carbon/tenant-billing/past_invoice.jsp";

    ECDetailResponse ecdResponse = paymentService.getExpressCheckoutDetails(token);
    Payer payer = ecdResponse.getPayer();
    Address address = ecdResponse.getAddress();

    //this is necessary when updating the payment table
    int invoiceId =  (Integer) session.getAttribute("invoiceId");
    session.removeAttribute("invoiceId");

%>


<fmt:bundle basename="org.wso2.carbon.payment.ui.i18n.Resources">

<carbon:breadcrumb
        label="payment.continued"
        resourceBundle="org.wso2.carbon.payment.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<script type="text/javascript">

    var updatePaymentTableResponse;

    function doPayment() {
        var token = '<%=ecdResponse.getToken()%>';
        var payerId = '<%=ecdResponse.getPayer().getPayerId()%>';
        var amount = '<%=ecdResponse.getOrderTotal()%>';
        var tenantDomain = '<%=tenantDomain%>';
        document.getElementById('messageTd').style.display='';
        jQuery.ajax({
            type: 'POST',
            url: 'doEC-ajaxprocessor.jsp',
            data: 'token=' + token + '&payerId=' + payerId + '&amount=' + amount
                    + '&tenantDomain=' + tenantDomain,
            async: false,
            success: function(msg) {
                var resp = eval('(' + msg + ')');
                if(resp.ack=='Success'){
                    updatePaymentTable(resp, amount);
                    if(updatePaymentTableResponse.status=='success'){
                        location.href = 'completed.jsp?paymentStatus=' + resp.paymentStatus + '&transactionID=' +
                                    resp.transactionID + '&pendingReason=' + resp.pendingReason +
                                    '&amount=' + amount + '&invoiceId=' + updatePaymentTableResponse.invoiceId;
                    }
                }else if(resp.ack=='Failure'){
                    CARBON.showErrorDialog('Transaction was not completed. ' + resp.error);
                    document.getElementById('messageTd').style.display='none';
                }
            }
        });
    }

    function updatePaymentTable(paymentresponse, amount){
        var transactionId = paymentresponse.transactionID;
        jQuery.ajax({
            type: 'GET',
            url: '../tenant-billing/add_payment-ajaxprocessor.jsp',
            data: 'transactionId=' + transactionId + '&amount=' + amount + '&invoiceId=' + <%=invoiceId%>,
            async: false,
            success: function(msg) {
                updatePaymentTableResponse = eval('(' + msg + ')');
            },
            error:function (error) {
                CARBON.showErrorDialog('Your payment was done. But we could not update our database due to a failure.' +
                        'Please contact WSO2 cloud Service with your transaction ID: ' + transactionId);
            }
        });
    }

    function cancelAndGoBack(){
        location.href = '<%=cancelUrl%>';
    }

</script>

<div id="middle">
    <h2><fmt:message key="payment.process.continued"/></h2>
    <br/>
    <div id="workArea">
        <table class="styledLeft" width="50%" cellpadding="1" id="paymentDetails">
            <thead>
                <tr>
                    <th colspan="2">Payment Details</th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td>PayerId</td>
                    <td><%=payer.getPayerId()%></td>
                </tr>
                <tr>
                    <td>First name</td>
                    <td><%=payer.getFirstName()%></td>
                </tr>
                <tr>
                    <td>Last Name</td>
                    <td><%=payer.getLastName()%></td>
                </tr>
                <tr>
                    <td>Amount</td>
                    <td><%=ecdResponse.getOrderTotal()%> USD</td>
                </tr>
            </tbody>
        </table>
        <br/>
        <table cellpadding="1">
            <tr><td colspan="2">Please click Confirm to confirm your payment or Cancel to go back to the invoice page</td></tr>
            <tr><td colspan="2"><br/></td></tr>
            <tr><td colspan="2"><input type="button" class="button" onclick="doPayment();" value="Confirm"/>
                <input type="button" class="button" onclick="cancelAndGoBack();" value="Cancel"/></td>
            </tr>
        </table>
        <div style="color:#494949; display:none;" id="messageTd">Please wait. Your transaction is in progress <img src="../admin/images/loading-small.gif" /></div>
        <form name="paymentComplete">
            <input type="hidden" name="payedAmount"/>
            <input type="hidden" name="transactionId"/>
            <input type="hidden" name="invoiceId"/>
        </form>
    </div>
</div>
</fmt:bundle>