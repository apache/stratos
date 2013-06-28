<%@ page import="org.wso2.carbon.register.ui.utils.TenantConfigUtil" %>
<%@ page import="org.wso2.carbon.stratos.common.beans.TenantInfoBean" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.List" %>
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

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%
    String paymentStatus = request.getParameter("paymentStatus");
    String transactionID = request.getParameter("transactionID");
    String pendingReason = request.getParameter("pendingReason");
    String print = request.getParameter("print");
    String amount = request.getParameter("amount");
    String invoiceId = request.getParameter("invoiceId");
%>

<link href="../payment/css/billing.css" rel="stylesheet" type="text/css" media="all"/>
<fmt:bundle basename="org.wso2.carbon.payment.ui.i18n.Resources">

<carbon:breadcrumb
        label="payment.completed"
        resourceBundle="org.wso2.carbon.payment.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>
<script type="text/javascript">
    function proceedRegistration(){
        location.href = "../tenant-register/success_register.jsp";
    }
</script>
<div id="middle">
    <h2><fmt:message key="payment.process.continued"/></h2>
    <br/>
    <div id="workArea">
        <div style="border:solid 1px #ccc;margin-top:20px;padding:15px !important">
            <table style="width:600px;" class="invoiceTable">
                <tr>
                    <td>
                        <img alt="WSO2" src="images/logo.png" align="top" style="display:block;"/>
                        <div style="margin-top:10px;">Federal Tax ID 87-0750575</div>

                    </td>
                    <td>
                        <div class="invoice-heading"></div>
                        <div class="invoice-sub-heading"></div>
                        <div class="invoice-sub-heading"></div>

                    </td>
                </tr>
                <tr>
                    <td> <ul class="invoice-inside-listing">
                            <li>800 West El Camino Real Suite 180,
                            </li>
                            <li>Mountain View, CA 94040
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


                        <ul class="invoice-inside-listing">
                            <li></li>
                            <li></li>
                            <li></li>
                         </ul>
                    </td>
                </tr>
                <tr>
                    <td colspan="2">
                              <table class="styledLeft">
                            <!--<thead>
                                <tr>
                                    <th colspan="2">Payment Status</th>
                                </tr>
                            </thead>-->
                            <tbody>
                                <%
                                    if("Completed".equals(paymentStatus)){
                                %>
                                    <tr>
                                        <td colspan="2">Thank you. Your payment was completed successfully.</td>
                                    </tr>
                                    <tr>
                                        <td>Transaction ID</td>
                                        <td><%=transactionID%></td>
                                    </tr>
                                    <tr>
                                        <td>Amount</td>
                                        <td><%=amount%> USD</td>
                                    </tr>
                                    <tr>
                                        <td colspan="2"></td>
                                    </tr>
                                    <%
                                        }else{
                                    %>
                                    <td>We are sorry. Your payment was not successful</td>
                                    <%
                                        }
                                    %>
                                </tr>
                            </tbody>
                        </table><br/>
                        <form action="print_payment_completed_ajaxprocessor.jsp" name="printForm">
                            <input type="hidden" name="paymentStatus" value="<%=paymentStatus%>"/>
                            <input type="hidden" name="transactionID" value="<%=transactionID%>"/>
                            <input type="hidden" name="pendingReason" value="<%=pendingReason%>"/>
                            <input type="hidden" name="amount" value="<%=amount%>"/>
                            <input type="hidden" name="invoiceId" value="<%=invoiceId%>"/>
                            <input type="hidden" name="print" value="yes"/>
                        </form>
                        <%if("Completed".equals(paymentStatus)){%>
                        <a class="button" target="_blank" href="#" onclick="javascript:document.printForm.submit();">Print</a>
                        <a class="button" target="_self" href="#" onclick="javascript:proceedRegistration();">Proceed Registration</a>
                        <%
                            }
                        %>
                        </td>
                    </tr>
                </table>
            </div>
    </div>
</div>
</fmt:bundle>