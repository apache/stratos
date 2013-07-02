<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ page import="org.wso2.carbon.billing.mgt.stub.beans.xsd.*" %>
<%@ page import="org.wso2.carbon.billing.mgt.ui.utils.BillingUtil" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>
<%@ page import="java.util.Enumeration" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<carbon:jsi18n
		resourceBundle="org.wso2.carbon.billing.mgt.ui.i18n.JSResources"
		request="<%=request%>" />



<fmt:bundle basename="org.wso2.carbon.billing.mgt.ui.i18n.Resources">
<carbon:breadcrumb
            label="view.invoice.menu"
            resourceBundle="org.wso2.carbon.billing.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>" />

<script type="text/javascript">

    function setExpressCheckout(paypalUrl) {
        var successUrl = document.startPaymentForm.successUrl.value;
        var cancelUrl = document.startPaymentForm.cancelUrl.value;
        var amount = document.startPaymentForm.amount.value;
	    if(amount=='$0.00'){
            CARBON.showInfoDialog('Your total payment is '+amount+'. You are not allowed to connect to Paypal');
        }else{
            document.getElementById('messageTd').style.display='';
            jQuery.ajax({
            type: 'GET',
            url: '../payment/setEC-ajaxprocessor.jsp',
            data: 'successUrl=' + successUrl + '&cancelUrl=' + cancelUrl + '&amount=' + amount,
            dataType: 'json',
            async: false,
            success: function(msg) {
                var resp = msg;
                if(resp.ack=='Success'){
                    location.href = paypalUrl + resp.token;
                }else{
                    if(resp.error){
                        CARBON.showErrorDialog('Could not connect to Paypal at the moment: ' + resp.error);
                    }else{
                        CARBON.showErrorDialog('Could not connect to Paypal at the moment');
                    }
                    document.getElementById('messageTd').style.display='none';
                }
            },
            error:function () {
                CARBON.showErrorDialog('Could not connect to PayPal site at the moment' );
                document.getElementById('messageTd').style.display='none';
            }
            });
	    }
    }

    function getInvoice(){

        var index = document.getElementById('yearMonth').selectedIndex;
        var invoiceId = document.getElementById('yearMonth').options[index].value;
        if(invoiceId<0){
            CARBON.showInfoDialog('Please select a month to view the invoice');
        }else{
            jQuery.ajax({
                type: 'POST',
                url: 'get_invoice_by_id_ajaxprocessor.jsp',
                data: 'invoiceId=' + invoiceId,
                success: function() {
                    location.href = 'past_invoice.jsp'
                },
                error:function (xhr, ajaxOptions, thrownError) {
                    CARBON.showErrorDialog('Could not get the invoice.');
                }
            });    
        }


        
    }
</script>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/register_config.js"></script>
<link href="../tenant-billing/css/billing.css" rel="stylesheet" type="text/css" media="all"/>
<style>
    h2.trigger{
    -moz-box-shadow:none;
    box-shadow:none;
    }
    .toggle_container {
    -moz-box-shadow:none;
    box-shadow:none;
}
</style>
<script type="text/javascript">
    jQuery(document).ready(function() {

      jQuery(".toggle_container").show();
      /*Hide (Collapse) the toggle containers on load use show() insted of hide() in the
      above code if you want to keep the content section expanded. */

      jQuery("h2.trigger").click(function() {
          if (jQuery(this).next().is(":visible")) {
              this.className = "active trigger";
          } else {
              this.className = "trigger";
          }

          jQuery(this).next().slideToggle("fast");
          return false; //Prevent the browser jump to the link anchor
      });
  });
</script>
<%
    String paypalUrl = "";
    if(CommonUtil.getStratosConfig()!=null){
        paypalUrl = CommonUtil.getStratosConfig().getPaypalUrl();
    }

    String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL(request);
    adminConsoleURL = adminConsoleURL.substring(0, adminConsoleURL.indexOf("carbon"));
    String tenantDomain = (String) session.getAttribute("tenantDomain");
    String successUrl = adminConsoleURL + "t/" + tenantDomain + "/carbon/payment/success.jsp";
    String cancelUrl = adminConsoleURL + "t/" + tenantDomain + "/carbon/tenant-billing/past_invoice.jsp";

    BillingPeriod[] billingPeriods = BillingUtil.getAvailableBillingPeriods(config, session);
    String[] billingMonths = BillingUtil.getAvailableBillingMonths(billingPeriods);

    //invoice is retrieved at get_invoice_by_id_ajaxprocessor.jsp and it is added to session
    MultitenancyInvoice invoice = (MultitenancyInvoice) session.getAttribute("invoice");
    session.removeAttribute("invoice");
    session.removeAttribute("invoiceId");
    if(invoice!=null){
        session.setAttribute("invoiceId", invoice.getInvoiceId());
    }

%>
<div id="middle">
        <h2><fmt:message key="view.invoice"/></h2>

        <div id="workArea">
            <%
                if(billingMonths.length>0){
            %>
            <table class="styledLeft">
				<thead>
				<tr>
				<th colspan="3" width="100%">
					<fmt:message key="select.invoice"/>
				</th>
				</tr>
				</thead>
				<tbody>
				         <tr class="packagerow">
				            <td class="packageCol" width="20%">
				                <fmt:message key="invoice"/>
				            </td>
				            <td colspan="2">
				                <select name="yearMonth" id="yearMonth">
				                    <option value="-1">--Select the Invoice--</option>
				                    <%
				                        for(int i=0; i< billingMonths.length; i++){
				                    %>
				                    <option value="<%=billingPeriods[i].getInvoiceId()%>"><%=billingMonths[i]%></option>
				                    <%
				                        }
				                    %>
				                </select>
				                <input type="button" class="button" value="View Invoice" onclick="getInvoice();"/>
				            </td>
				        </tr>
				</tbody>
			</table>
            <%
                    }else{
            %>
                <table class="styledLeft">
                    <thead>
                        <tr>
                            <th>
                                <fmt:message key="invoice.information.head"/>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr class="packagerow">
                            <td class="packageCol">
                                <fmt:message key="no.invoice.at.the.moment"/>
                            </td>
                        </tr>
                    </tbody>
                </table>
            <%
                    }
            %>

			
			 <%
        if(invoice!=null){
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
            SimpleDateFormat titleDateFormat = new SimpleDateFormat("MMM yyyy");
            int invoiceId = invoice.getInvoiceId();
            Date billingDate = invoice.getBillingDate();
            Date startDate = invoice.getStartDate();
            Date endDate = invoice.getEndDate();
            String broughtForward = invoice.getBoughtForward();
            String carriedForward = invoice.getCarriedForward();
            String totalPayments = invoice.getTotalPayments();
            String totalCost = invoice.getTotalCost();
            boolean lastInvoice = invoice.getLastInvoice();
            MultitenancySubscription[] subscriptions = invoice.getSubscriptions();
            MultitenancyPurchaseOrder[] purchaseOrders = invoice.getPurchaseOrders();

    %>
    <div style="border:solid 1px #ccc;margin-top:20px;padding:15px !important">
            <table style="width:600px;" class="invoiceTable">
                <tr>
                    <td>
                        <img alt="WSO2" src="images/logo.png" align="top" style="display:block;"/>
                        <div style="margin-top:10px;">Federal Tax ID 87-0750575</div>

                    </td>
                    <td>
                        <div class="invoice-heading">Invoice</div>
                        <div class="invoice-sub-heading"><%=titleDateFormat.format(billingDate)%></div>
                        <div class="invoice-sub-heading">INVOICE NO: <%=invoiceId%></div>

                    </td>
                </tr>
                <tr>
                    <td> <ul class="invoice-inside-listing">
                            <li>4131, El Camino Real Suite 200,
                            </li>
                            <li>Palo Alto, CA 94306
                            </li>
                            <li>Tel: (408) 754 7388
                            </li>
                            <li>Fax: (408) 689 4328
                            </li>
                            <li>Email: billing@wso2.com
                            </li>
                        </ul>
					</td>
                    <td>
                   
                
<!--                         <ul class="invoice-inside-listing"> -->
<%--                             <li><fmt:message key="invoice.date"/>: <%=dateFormat.format(billingDate)%></li> --%>
<%--                             <li><fmt:message key="start.date"/>: <%=dateFormat.format(startDate)%></li> --%>
<%--                             <li><fmt:message key="end.date"/>: <%=dateFormat.format(endDate)%></li> --%>
                            
<!--                          </ul> -->
						  <table border="0" class="normal">
								<tr> <td><fmt:message key="invoice.date"/> </td>
								     <td>: <%=dateFormat.format(billingDate)%></td>
								</tr>
									<tr> <td><fmt:message key="start.date"/> </td>
								     <td>: <%=dateFormat.format(startDate)%></td>
								</tr>
									<tr> <td><fmt:message key="end.date"/></td>
								     <td>: <%=dateFormat.format(endDate)%></td>
								</tr>
						</table>		
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                        
                        <table class="invice-data-table">
                        <tr class="invoice-header">
                                <td>Particulars</td>
                                 <td style="text-align:right">Value (USD)</td>
                            </tr>
           <tr class="invoice-content-row">
                	<td colspan="2"><strong style="color:#555"><fmt:message key="charges.subscriptions"/></strong></td>
                </tr>
                <%
                    if(subscriptions!=null){
                    for (MultitenancySubscription subscription: subscriptions) {

                        String subscribedPackage = subscription.getSubscribedPackage();
                        BilledEntry[] billedEntries = subscription.getBilledEntries();
                        Date activeSince = subscription.getActiveSince();
                        Date activeUntil = subscription.getActiveUntil();
                %>
                
                <tr class="invoice-content-row">
                    <td class="leftCol-med" style="padding-top:20px !important" colspan="2">
                        <fmt:message key="subscription"/> Type: <strong><%=subscribedPackage%></strong>
                    </td>
                </tr>
                <tr class="invoice-content-row">
                    <td colspan="2">
                        <fmt:message key="active.since"/>: <%=dateFormat.format(activeSince)%>
                    </td>
                </tr>
                <%
                    if(subscription.getActive()){
                %>
                <tr class="invoice-content-row">
                    <td colspan="2">
                        <fmt:message key="active.until"/>: <fmt:message key="this.is.active.subscription"/>
                    </td>
                </tr>
                <%
                    }
                    else{
                %>
                <tr class="invoice-content-row">
                    <td colspan="2">
                        <fmt:message key="active.until"/>: <%=dateFormat.format(activeUntil)%> 
                    </td>
                </tr>
                <%
                    }
                %>

                <%
                    if(billedEntries!=null && billedEntries.length>0){
                       for (BilledEntry billedEntry: billedEntries) {
                           if(billedEntry!=null){
                            String name = billedEntry.getName();
                            String cost = billedEntry.getCost();
                %>
                <tr class="invoice-content-row">
                    <td>
                        <%=name%>
                    </td>
                    <td style="text-align:right;font-weight:bold;">
                        <%=cost%>
                    </td>
                </tr>
                <%
                                    }
                                }
                            }
                        }
                    }
                %>
                	<tr class="invoice-content-row">
                		<td colspan="2" style="padding-top:20px !important"><strong style="color:#555"><fmt:message key="payment.details"/></strong></td>
                	</tr>
                <%
                if (purchaseOrders == null || purchaseOrders.length == 0) {
                %>
                 
                 <tr class="invoice-content-row">
                		<td colspan="2">You don't have any payment details for this period.</td></tr>                 
                <%
                } else {
                    for (MultitenancyPurchaseOrder purchaseOrder: purchaseOrders) {
                        if (purchaseOrder == null) {
                            continue;
                        }
                        int id = purchaseOrder.getId();
                        Date paymentDate = purchaseOrder.getPaymentDate();
                        String payment = purchaseOrder.getPayment();
                %>
				<tr class="invoice-content-row">
				<td class="leftCol-med"><fmt:message key="payment"/> : <strong><%=purchaseOrder.getTransactionId()%></strong> on <%=dateFormat.format(paymentDate)%></td>
				<td style="text-align:right;font-weight:bold;"><%=payment%></td>
				</tr>
                
                <%
                    }
                }
                %>
			
				<tr class="invoice-content-row"><td colspan="2" style="padding-top:20px !important"><strong style="color:#555"><fmt:message key="invoice.summary"/></strong></td></tr>


                <tr class="invoice-content-row">
                    <td>
                        <fmt:message key="brought.forward"/>
                    </td>
                
                    <td style="text-align:right;font-weight:bold;">
                        <%=broughtForward%>
                    </td>
                </tr>

                <tr class="invoice-content-row">
                    <td>
                        <fmt:message key="total.cost"/>
                    </td>
                    <td style="text-align:right;font-weight:bold;">
                        <%=totalCost%>
                    </td>
                </tr>


                <tr class="invoice-content-row">
                    <td>
                        <fmt:message key="total.payments"/>
                    </td>

                    <td style="text-align:right;font-weight:bold;">
                        <%=totalPayments%>
                    </td>
                </tr>

                <tr class="invoice-content-row">
                    <td>
                        <fmt:message key="carried.forward"/>
                    </td>

                    <td style="text-align:right;font-weight:bold;">
                        <%=carriedForward%>
                    </td>
                </tr>
                <!--<tr class="invoice-content-row">
                    <td colspan="2" id="messageTd">&nbsp;</td>
                </tr>-->
            </table>
                
                
                    </td>
                </tr>
                
                <tr>
                    <td colspan="2">
                        <div class="name-field">Padmika Dissanaike,</div>
                        Director- Finance
                    </td>
                </tr>
            </table>
                    <div style="background-color:#e2edf9;padding:10px;">
                        <form name="startPaymentForm" method='POST'>
                            <input type="hidden" name="successUrl" id="successUrl" value="<%=successUrl%>"/>
                            <input type="hidden" name="cancelUrl" id="cancelUrl" value="<%=cancelUrl%>"/>
                            <input type="hidden" name="amount" id="amount" value="<%=carriedForward%>"/>
                            <a href="#"><img src='http://images.paypal.com/images/x-click-but6.gif'
                                             border='0' align='top' alt='PayPal' onclick="setExpressCheckout('<%=paypalUrl%>');"/>
                            </a>
                        </form>
                        <div style="color:#494949">Pay the invoice securely online</div>
                    </div>
                    <div style="color:#494949; display:none;" id="messageTd">Please wait until you are connected to Paypal <img src="../admin/images/loading-small.gif" /></div>
                        </div>	
             <%
        		}
    		%>
    		
        </div>
</div>

 </fmt:bundle>
