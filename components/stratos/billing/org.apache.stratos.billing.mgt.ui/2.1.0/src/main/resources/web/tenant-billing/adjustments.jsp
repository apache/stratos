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

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>

<carbon:jsi18n
        resourceBundle="org.wso2.carbon.billing.mgt.ui.i18n.JSResources"
        request="<%=request%>"/>

<fmt:bundle basename="org.wso2.carbon.billing.mgt.ui.i18n.Resources">
    <carbon:breadcrumb
            label="adjustment.menu"
            resourceBundle="org.wso2.carbon.billing.mgt.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>
    <script type="text/javascript">

        function findInvoices(){
            document.getElementById("invoiceDetailTable").style.display="none";
            var tenantDomain = document.getElementById('tenantDomain').value;
            if(tenantDomain=="" || tenantDomain==null){
                CARBON.showErrorDialog("Please enter a tenant domain");
            }else{
                jQuery.ajax({
                    type: 'GET',
                    url: '../tenant-billing/find_invoices_ajaxprocessor.jsp',
                    data: 'tenantDomain=' + tenantDomain,
                    dataType: 'json',
                    async: false,
                    success: function(msg) {
                        var invoiceList = document.getElementById('invoices');
                        clearOptions(invoiceList);
                        var invoices = msg.invoices;
                        for(i=0; i < invoices.length; i++){
                            addToOptionList(invoiceList, invoices[i].invoiceId, invoices[i].invoiceDate);
                        }
                        if(invoices.length>0){
                            document.getElementById("invoiceDetailTable").style.display="";
                        }else{
                            CARBON.showInfoDialog("There are no invoices for this tenant");
                        }
                    },
                    error:function (xhr) {
                        CARBON.showErrorDialog('Could not get the invoices of tenant: ' + tenantDomain );
                    }
                });
            }

        }

        function makeAdjustment(){
            var invoiceList = document.getElementById('invoices');
            var invoiceId = invoiceList.options[invoiceList.selectedIndex].value;
            if(invoiceId>0){
                document.adjustmentForm.invoiceId.value = invoiceId;
            }else{
                CARBON.showErrorDialog("Please select an invoice");
                return;
            }

            var amount = document.getElementById("amount").value;
            amount = $.trim(amount);
            if(amount==null || amount==""){
                CARBON.showErrorDialog("Please enter an amount to be adjusted");
                return;
            }
            var description = document.getElementById("description").value;
            if(description==null || description==""){
                CARBON.showErrorDialog("Please enter a description for the adjustment");
                return;
            }

            document.adjustmentForm.submit();


        }

        function clearOptions(OptionList) {

            // Always clear an option list from the last entry to the first
            for (x = OptionList.length; x >= 0; x = x - 1) {
                OptionList[x] = null;
            }
        }

        function addToOptionList(OptionList, OptionValue, OptionText) {
            // Add option to the bottom of the list
            OptionList[OptionList.length] = new Option(OptionText, OptionValue);
        }

        function cancel(){
            location.href = "adjustments.jsp";
        }
    </script>

    <%
        String adjustmentIdStr=null;
        if(session.getAttribute("adjustmentId")!=null){
            adjustmentIdStr = String.valueOf(session.getAttribute("adjustmentId"));
            session.removeAttribute("adjustmentId");
        }

        if(adjustmentIdStr!=null && !"".equals(adjustmentIdStr)){
            int adjustmentId = Integer.parseInt(adjustmentIdStr);
            if(adjustmentId>0){
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showInfoDialog("Adjustment was made successfully");
        });
    </script>
    <%
    }else{
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showErrorDialog("An error occurred while making the adjustment");
        });
    </script>
    <%
            }

        }
    %>

    <div id="middle">
        <h2><fmt:message key="invoice.adjustments"/></h2>
        <div id="workArea">
            <form name="adjustmentForm" action="make_adjustment_ajaxprocessor.jsp" method="post">
                <input type="hidden" name="invoiceId" id="invoiceId" />
                <table class="styledLeft" cellspacing="0">
                    <thead>
                    <tr>
                        <th colspan="3"><fmt:message key="find.invoices"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td style="width:180px"><fmt:message key="enter.tenant.domain"/></td>
                        <td colspan="2"><input type="text" name="tenantDomain" id="tenantDomain"
                                               style="width:300px"/>
                            <input type="button" onclick="javascript:findInvoices();" value="<fmt:message key="find.invoices"/>"/>
                        </td>
                    </tr>
                    <tr><td colspan="3"></td></tr>
                    </tbody>
                </table>

                <table id="invoiceDetailTable" class="styledLeft" cellspacing="0" style="display: none">
                    <thead>
                    <tr>
                        <th colspan="3"><fmt:message key="adjustment.information"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td style="width:180px"><fmt:message key="select.invoice"/></td>
                        <td colspan="2"><select id="invoices"></select></td>
                    </tr>
                    <tr>
                        <td style="width:180px"><fmt:message key="amount"/></td>
                        <td colspan="2"><input type="text" name="amount" id="amount"></td>
                    </tr>
                    <tr>
                        <td style="width:180px"><fmt:message key="description"/></td>
                        <td colspan="2"><input type="text" name="description" id="description"></td>
                    </tr>
                    <tr>
                        <td colspan="3">
                            <input type="button" class="button" value="<fmt:message key="submit"/>" onclick="makeAdjustment();">
                            <input type="button" class="button" value="<fmt:message key="cancel"/>" onclick="cancel();">
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </div>
    </div>
</fmt:bundle>
