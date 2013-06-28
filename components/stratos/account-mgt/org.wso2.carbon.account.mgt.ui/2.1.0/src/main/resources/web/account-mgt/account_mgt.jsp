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
<%@ page import="org.wso2.carbon.account.mgt.stub.beans.xsd.AccountInfoBean" %>
<%@ page import="org.wso2.carbon.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page import="org.wso2.carbon.account.mgt.ui.clients.UsagePlanClient" %>
<%@ page import="org.wso2.carbon.stratos.common.constants.StratosConstants" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<link rel="stylesheet" type="text/css" href="../yui/assets/yui.css">
<link rel="stylesheet" type="text/css" href="../yui/build/menu/assets/skins/sam/menu.css"/>
<link rel="stylesheet" type="text/css" href="../yui/build/button/assets/skins/sam/button.css"/>
<link rel="stylesheet" type="text/css"
      href="../yui/build/container/assets/skins/sam/container.css"/>
<link rel="stylesheet" type="text/css"
      href="../yui/build/autocomplete/assets/skins/sam/autocomplete.css"/>
<link rel="stylesheet" type="text/css" href="../yui/build/editor/assets/skins/sam/editor.css"/>
<link rel="stylesheet" type="text/css" href="../account-mgt/css/account_mgt.css"/>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="../account-mgt/js/account_mgt.js"></script>
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.account.mgt.ui.i18n.JSResources"
        request="<%=request%>"/>
<fmt:bundle basename="org.wso2.carbon.account.mgt.ui.i18n.Resources">

<%
    if ("true".equals(session.getAttribute("domain-validation-failure"))) {
        session.removeAttribute("domain-validation-failure");
%>

<script type="text/javascript">
    jQuery(document).ready(function() {
        CARBON.showWarningDialog(jsi18n["domain.validation.failed"]);
    });
</script>
<%
    }
    if ("true".equals(session.getAttribute("domain-validation-success"))) {
        session.removeAttribute("domain-validation-success");
%>

<script type="text/javascript">
    jQuery(document).ready(function() {
        CARBON.showWarningDialog(jsi18n["successfully.validated"]);
    });
</script>
<%
    }


    if ("false".equals(session.getAttribute("temp-domain-available"))) {
        session.removeAttribute("temp-domain-available");
%>

<script type="text/javascript">
    jQuery(document).ready(function() {
        CARBON.showWarningDialog(jsi18n["domain.unavailable"]);
    });
</script>
<%
    }
%>
<%

    String contactEmail = "";
    String firstname = "";
    String lastname = "";
    String tenantDomain = "";
    String usagePlan = "";
    boolean isDomainValidated;
    boolean isEmailValidated = false;
    AccountMgtClient client;
    try {
        client = new AccountMgtClient(config, session);
        contactEmail = client.getContact();
        AccountInfoBean accountInfoBean = client.getFullname();
        firstname = accountInfoBean.getFirstname();
        lastname = accountInfoBean.getLastname();
        if (firstname == null) {
            firstname = "";
        }
        if (lastname == null) {
            lastname = "";
        }
        isDomainValidated = client.isDomainValidated();
        isEmailValidated = client.isEmailValidated();


        if ("true".equals(request.getParameter("isUpdate"))) {
            usagePlan = request.getParameter("selectedUsagePlan");
            if (org.wso2.carbon.account.mgt.ui.utils.Util.updateUsagePlan(request, config, session)) {
%>
<script type="text/javascript">
    //        window.location.href="../admin/logout_action.jsp";
    CARBON.showInfoDialog("Your usage plan has been updated.");
</script>
<%
} else {
%>
<script type="text/javascript">
    //        window.location.href="../admin/logout_action.jsp";
    CARBON.showInfoDialog("Error occured while updating usage plan.");
</script>
<%
        }
    }
    usagePlan = org.wso2.carbon.account.mgt.ui.utils.Util.getUsagePlanName(config, session);
    if ("true".equals(request.getParameter("isDeactivated"))) {
        client.deactivate();
        tenantDomain = (String) session.getAttribute("tenantDomain");
%>

<script type="text/javascript">
    //        window.location.href="../admin/logout_action.jsp";
    CARBON.showInfoDialog("Your account has been deactivated.",
                         function() {
                             window.location.href = "../admin/logout_action.jsp"
                         },
                         function() {
                             window.location.href = "../admin/logout_action.jsp"
                         });
</script>
<%
    }
} catch (RegistryException e) {
%>
<div>Error in getting account management information.</div>
<%
        return;
    }

    String currentDomain = (String) session.getAttribute(MultitenantConstants.TENANT_DOMAIN);

    if (session.getAttribute("submit-firstname") != null) {
        firstname = (String) session.getAttribute("submit-firstname");
        session.setAttribute("submit-firstname", null);
    }
    if (session.getAttribute("submit-lastname") != null) {
        lastname = (String) session.getAttribute("submit-lastname");
        session.setAttribute("submit-lastname", null);
    }

%>

<carbon:breadcrumb label="resources"
                   resourceBundle="org.wso2.carbon.account.mgt.ui.i18n.Resources"
                   topPage="true" request="<%=request%>"/>
<div id="middle">

<div id="workArea">
<h2><fmt:message key="account.mgt.title"/></h2>


<table class="styledLeft">
    <thead>
    <tr>
        <th><fmt:message key="contact.information"/></th>
    </tr>
    </thead>
    <tbody>

    <tr>
        <td class="nopadding">
            <form method="POST" action="update_contact_ajaxprocessor.jsp">
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <tr>
                        <td width="200px"><fmt:message key="contact.email"/><span
                                class="required">*</span></td>
                        <td colspan="2">
                            <input type="text" name="email" id="email" style="width:400px"
                                   value="<%=contactEmail%>"/> <span id="busyContact">&nbsp;</span>
                            <span style="visibility:hidden"
                                  id="old-email"><%=isEmailValidated ? contactEmail : ""%></span>
                        </td>
                    </tr>
                    <%
                        if (isEmailValidated) {
                    %>

                    <tr>
                        <td></td>
                        <td colspan="2"><input onclick="updateContact()" type="button"
                                               value="<fmt:message key="update.contact"/>"/></td>
                    </tr>
                    <%
                    } else {
                    %>
                    <tr>
                        <td colspan="3">
                            <div class="not-validated"><img src="images/wrong.gif" alt="Validated"/>Your
                                email is not validated.
                                Please validate it from here.
                            </div>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <td colspan="2"><input onclick="updateContact()" type="button"
                                               value="<fmt:message key="validate.contact"/>"/></td>
                    </tr>
                    <%
                        }
                    %>

                    <tr>
                        <td colspan="3">
                            <div class="table-spacer">&nbsp;</div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </td>

    </tr>

    </tbody>
</table>


<table class="styledLeft">
    <thead>
    <tr>
        <th><fmt:message key="admin.information"/></th>
    </tr>
    </thead>
    <tbody>

    <tr>
        <td class="nopadding">
            <form id="update_profile_form" action="update_profile_processor.jsp" method="POST">
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <tr>
                        <td width="200px"><fmt:message key="firstname"/><span
                                class="required">*</span></td>
                        <td colspan="2">
                            <input type="text" name="firstname" id="firstname" style="width:400px"
                                   value="<%=firstname%>"/>
                            <span style="visibility:hidden" id="old-firstname"><%=firstname%></span>
                        </td>
                    </tr>
                    <tr>
                        <td width="200px"><fmt:message key="lastname"/><span
                                class="required">*</span></td>
                        <td colspan="2">
                            <input type="text" name="lastname" id="lastname" style="width:400px"
                                   value="<%=lastname%>"/>
                            <span style="visibility:hidden" id="old-lastname"><%=lastname%></span>
                        </td>
                    </tr>
                    <tr>
                        <td></td>
                        <td colspan="2"><input onclick="updateProfile()" type="button"
                                               value="<fmt:message key="update.profile"/>"/></td>
                    </tr>
                    <tr>
                        <td colspan="3">
                            <div class="table-spacer">&nbsp;</div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </td>

    </tr>

    </tbody>
</table>


<table class="styledLeft">
    <thead>
    <tr>
        <th><fmt:message key="validate.domain.title"/></th>
    </tr>
    </thead>
    <tbody>

    <tr>
        <td class="nopadding">
            <form onsubmit="return domainSelected();" id="validateDomainForm" method="POST"
                  action="validate_domain_ajaxprocessor.jsp">
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <tr>
                        <td width="200px"><fmt:message key="current.domain"/></td>
                        <td colspan="2"><input readonly="true" type="text" name="domain" id="domain"
                                               style="width:400px" value="<%=currentDomain%>"/>
                        </td>
                    </tr>

                    <%
                        if (isDomainValidated) {
                    %>
                    <tr>
                        <td colspan="3">
                            <div class="validated"><img src="images/right.gif" alt="Validated"/> The
                                ownership of your domain is already validated.
                            </div>
                        </td>
                    </tr>

                    <%
                    } else {
                    %>
                    <tr>
                        <td colspan="3">
                            <div class="not-validated"><img src="images/wrong.gif" alt="Validated"/>
                                The ownership of your
                                domain is not validated. You can validate it from here.
                            </div>
                        </td>
                    </tr>

                    <tr>
                        <td></td>
                        <td colspan="2"><input type="submit"
                                               value="<fmt:message key="validate.domain"/>"/></td>
                    </tr>
                    <tr>
                        <td colspan="3">
                            <div class="table-spacer">&nbsp;</div>
                        </td>
                    </tr>

                    <%
                        }
                    %>
                    </tbody>
                </table>
            </form>
        </td>
    </tr>

    </tbody>
</table>


<table class="styledLeft">
    <thead>
    <tr>
        <th><fmt:message key="deactivate.account.title"/></th>
    </tr>
    </thead>
    <tbody>


    <tr>
        <td class="nopadding">
            <form method="POST" id="deactivate_form" action="account_mgt.jsp">
                <input type="hidden" name="isDeactivated" value="true"/>
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <tr>
                        <td colspan="3"><fmt:message key="deactivate.account.msg"/></td>
                    </tr>
                    <tr>
                        <td width="200px"></td>
                        <td colspan="2"><input onclick="return deactivate('<%=currentDomain%>');"
                                               type="button"
                                               value="<fmt:message key="deactivate.account.btn"/>"/>
                        </td>
                    </tr>
                    <tr>
                        <td colspan="3">
                            <div class="table-spacer">&nbsp;</div>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
        </td>
    </tr>

    </tbody>
</table>


<table class="styledLeft">
    <thead>
    <tr>
        <th><fmt:message key="usage.plan.information"/>
            <% if (usagePlan.length() > 10) {
                String planName = usagePlan;
            %>
            - Your Current Usage Plan is : <%=planName%>
            <%}%>
        </th>
    </tr>
    </thead>
    <tbody>

<script type="text/javascript">
<%
    boolean chargeOnRegistration = CommonUtil.isChargedOnRegistration();
%>

function updatePlan(plan, existingPlan, regTenantDomain) {
    var newPlan = plan.options[plan.selectedIndex].value;
    if(newPlan==existingPlan){
        CARBON.showInfoDialog("Please select the new plan before updating");
    }else{
        sessionAwareFunction(function() {
        CARBON.showConfirmationDialog("Are you sure you want to update your UsagePlan ? "
                                      , function() {





            var submitForm = document.getElementById("usagePlanUpdate_form");
            submitForm.submit();
        });
    }, "Session timed out. Please login again.");
    }
}
</script>

    <tr>
        <td class="nopadding">
            <form method="POST" id="usagePlanUpdate_form"
            <% if (chargeOnRegistration){ %>
            action="init_payment_ajaxprocessor.jsp"
            <% } else { %>
            action="account_mgt.jsp"
            <% } %>
            >
                <input type="hidden" name="isUpdate" value="true"/>
                <table class="normal-nopadding" cellspacing="0">
                    <tbody>
                    <td><fmt:message key="select.usage.plan.for.tenant"/></td>
                    <td>
                        <select name="selectedUsagePlan" id="selectedUsagePlan">
                        </select>
                        <a href="<%=CommonUtil.getStratosConfig().getUsagePlanURL()%>"
                           target="_blank">
                            <b>Pricing Info</b></a>
                    </td>


                    </tr>
                    <tr>
                        <td colspan="2"><fmt:message key="select.package.message"/>
                        </td>
                    </tr>
                    <tr>
                        <td width="200px"></td>
                        <td colspan="2"><input
                                onclick="return updatePlan(document.getElementById('selectedUsagePlan'), '<%=usagePlan%>', '<%=currentDomain%>');"
                                type="button"
                                value="Update Plan"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </form>
    </tbody>
</table>
</div>
</div>
</fmt:bundle>
<script type="text/javascript">

    jQuery(document).ready(
                          function() {
                              jQuery.ajax({
                                  type: 'POST',
                                  url: 'get_package_info_ajaxprocessor.jsp',
                                  dataType: 'json',
                                  data: 'plan=0',
                                  async: false,
                                  success: function(data) {
                                      packageInfo = data;
                                  },
                                  error:function (xhr, ajaxOptions, thrownError) {
                                      CARBON.showErrorDialog('Could not get package information.');
                                  }
                              });

                              var charge;
                              var name;
                              for (var i = 0; i < packageInfo.length; i++) {
                                  charge = packageInfo[i].subscriptionCharge;
                                  name = packageInfo[i].name;
                                  if (name == '<%=org.wso2.carbon.account.mgt.ui.utils.Util.getUsagePlanName(config, session)%>') {
                                      option = document.createElement("option");
                                      option.value = name;
                                      option.selected = name;
                                      option.innerHTML = name;
                                      document.getElementById('selectedUsagePlan').appendChild(option);

                                  }
                                  else {
                                      option = document.createElement("option");
                                      option.value = name;
                                      option.innerHTML = name
                                      document.getElementById('selectedUsagePlan').appendChild(option);
                                  }
                              }
                          }
            );
</script>
