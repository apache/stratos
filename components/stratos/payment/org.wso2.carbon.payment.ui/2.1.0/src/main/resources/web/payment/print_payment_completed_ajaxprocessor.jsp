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

<%
    String paymentStatus = request.getParameter("paymentStatus");
    String transactionID = request.getParameter("transactionID");
    String pendingReason = request.getParameter("pendingReason");
    String print = request.getParameter("print");
    String amount = request.getParameter("amount");
    String invoiceId = request.getParameter("invoiceId");
    /*int invoiceId =  (Integer) session.getAttribute("invoiceId");
    session.removeAttribute("invoiceId");
    */

%>
<link href="../payment/css/billing.css" rel="stylesheet" type="text/css" media="all"/>
<style type="text/css">
    ul {
        list-style: none;
        margin: 0px;
        padding: 0px;
    }
    ul li{
        list-style: none;
        margin: 0px;
        padding: 0px;
    }

</style>
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
                                            <td>Invoice ID</td>
                                            <td><%=invoiceId%></td>
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
                            </table>
                        </td>
                    </tr>
            <%
                if(print!=null && "yes".equals(print)){
                    %>
            <script type="text/javascript">
                window.print();
            </script>
            <%
                }
            %>