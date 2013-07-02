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

        function submitDiscountForm(){
            var tenantDomain = document.getElementById("tenantDomain").value;
            if(tenantDomain==null || tenantDomain==""){
                CARBON.showErrorDialog("Please provide a tenant domain.");
                return;
            }

            var discountType = document.getElementById("discountType").value;
            var discountRadio = document.discountForm.discountTypeRadio;
            for(var i=0; i<discountRadio.length; i++){
                if(discountRadio[i].checked){
                    discountType = discountRadio[i].value;
                    break;
                }
            }
            //if the hidden variable is not set properly, we should set it
            //just making sure it is set..
            document.getElementById("discountType").value = discountType;

            if(discountType=="percentage"){
                var percentage = document.getElementById("percentage").value;
                if(percentage==null || percentage==""){
                    CARBON.showErrorDialog("Please enter a valid discount percentage.");
                    return;
                }
            }else if(discountType=="amount"){
                var amount = document.getElementById("amount").value;
                if(amount==null || amount==""){
                    CARBON.showErrorDialog("Please enter a valid discount amount.");
                    return;
                }
            }

            var startDate = document.getElementById("startDate").value;
            if(startDate==null || startDate==""){
                CARBON.showErrorDialog("Please enter a valid start date.");
                return;
            }

            var endDate = document.getElementById("endDate").value;
            if(endDate==null || endDate==""){
                CARBON.showErrorDialog("Please enter a valid end date.");
                return;
            }

            document.discountForm.submit();
        }

        function manageDiscountTypeRow(discountType){
            document.discountForm.discountType.value = discountType;

            if(discountType=="percentage"){
                document.getElementById("percentageRow").style.display="";
                document.getElementById("amountRow").style.display="none";
            }else if(discountType=="amount"){
                document.getElementById("percentageRow").style.display="none";
                document.getElementById("amountRow").style.display="";
            }else{
                //do nothing
            }
        }

        function cancel(){
            location.href = "discounts.jsp";
        }
    </script>

    <%
        String discountAdded = null;
        if(session.getAttribute("discountAdded")!=null){
            discountAdded = String.valueOf(session.getAttribute("discountAdded"));
            session.removeAttribute("discountAdded");
        }

        if(discountAdded!=null){
            if("true".equals(discountAdded)){

    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showInfoDialog("Discount was added successfully");
        });
    </script>
    <%
    }else{
    %>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showErrorDialog("An error occurred while adding the discount");
        });
    </script>
    <%
            }

        }
    %>

    <div id="middle">
        <h2><fmt:message key="discounts"/></h2>
        <div id="workArea">
            <form name="discountForm" action="add_discount_ajaxprocessor.jsp" method="post">
                <input type="hidden" name="discountType" id="discountType"/>
                <table class="styledLeft" cellspacing="0">
                    <thead>
                    <tr>
                        <th colspan="3"><fmt:message key="discounts.table.title"/></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td style="width:180px"><fmt:message key="tenant.domain"/></td>
                        <td colspan="2"><input type="text" name="tenantDomain" id="tenantDomain"></td>
                    </tr>
                    <tr>
                        <td style="width:180px"><fmt:message key="discount.type"/></td>
                        <td colspan="2">
                            <input type="radio" name="discountTypeRadio" value="percentage" checked onclick="manageDiscountTypeRow(this.value);" ><fmt:message key="type.percentage"/>
                            <input type="radio" name="discountTypeRadio" value="amount" onclick="manageDiscountTypeRow(this.value);" ><fmt:message key="type.amount"/>
                        </td>
                    </tr>
                    <tr id="percentageRow">
                        <td style="width:180px"><fmt:message key="discount.percentage"/></td>
                        <td colspan="2"><input type="text" name="percentage" id="percentage"></td>
                    </tr>
                    <tr id="amountRow" style="display: none;">
                        <td style="width:180px"><fmt:message key="amount"/></td>
                        <td colspan="2"><input type="text" name="amount" id="amount"></td>
                    </tr>
                    <tr>
                        <td style="width:180px"><fmt:message key="discount.start.date"/></td>
                        <td colspan="2"><input type="text" name="startDate" id="startDate"></td>
                    </tr>
                    <tr>
                        <td style="width:180px"><fmt:message key="discount.end.date"/></td>
                        <td colspan="2"><input type="text" name="endDate" id="endDate"></td>
                    </tr>
                    <tr>
                        <td colspan="3">
                            <input type="button" class="button" value="<fmt:message key="submit"/>" onclick="submitDiscountForm();">
                            <input type="button" class="button" value="<fmt:message key="cancel"/>" onclick="cancel();">
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </div>
    </div>
</fmt:bundle>
