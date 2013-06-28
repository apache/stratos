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
<%@ page import="org.wso2.carbon.stratos.common.constants.StratosConstants" %>
<%@ page import="org.wso2.carbon.stratos.common.util.CommonUtil" %>
<%@ page import="org.wso2.carbon.register.ui.utils.TenantConfigUtil" %>
<%@ page import="org.wso2.carbon.tenant.register.stub.beans.xsd.CaptchaInfoBean" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/select_domain.js"></script>
<script type="text/javascript" src="../admin/js/jquery.js"></script>
<script type="text/javascript" src="../admin/js/jquery.form.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
<script type="text/javascript" src="../googleanalytics/js/googleAnalyticsProcessor.js"></script>
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.register.ui.i18n.JSResources"
        request="<%=request%>"/>
<fmt:bundle basename="org.wso2.carbon.register.ui.i18n.Resources">

<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<link href="../tenant-register/css/tenant-register.css" rel="stylesheet" type="text/css" media="all"/>
<script type="text/javascript" src="js/register_config.js"></script>
<script type="text/javascript">

<%
    boolean chargeOnRegistration = CommonUtil.isChargedOnRegistration();
    String regDomain = (String)session.getAttribute("regTenantDomain");
%>
    var packageInfo;
    function showRentalMessage() {

        if (packageInfo == null) {
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
        }

        var plan = document.getElementById('selectedUsagePlan').
                options[document.getElementById('selectedUsagePlan').selectedIndex].value;
        var charge;
        for (var i = 0; i < packageInfo.length; i++) {
            if (packageInfo[i].name == plan) {
                charge = packageInfo[i].subscriptionCharge;
                break;
            }

        }

        document.getElementById('packagePrice').innerHTML = '<b>' + '<fmt:message key="billing.currency"/>' + charge + " per month" + '</b>';

    }

    function makePayment() {
        var selectEl = document.getElementById("selectedUsagePlan");
        var selectedUsagePlan = selectEl.options[selectEl.selectedIndex].value;
        var regTenantDomain = '<%= regDomain%>';


        for (var i = 0; i < packageInfo.length; i++) {
        <% if (chargeOnRegistration) { %>
            if (packageInfo[i].name == selectedUsagePlan && packageInfo[i].subscriptionCharge != "0") {
                CARBON.showConfirmationDialog('<fmt:message key="redirect.to.paypal.msg1"/>' + " " + '<fmt:message key="billing.currency"/>' + packageInfo[i].subscriptionCharge +
                                              ". " + '<fmt:message key="redirect.to.paypal.msg2"/>', function() {
                    document.getElementById('waitMessage').style.display = 'block';
                    document.forms["selectUsagePlan"].submit();
                }, function () {
                    document.getElementById('submit-button').removeAttribute('disabled');
                })

            } else if (packageInfo[i].name == selectedUsagePlan && packageInfo[i].subscriptionCharge == "0") {
                location.href = "../tenant-register/success_register.jsp";
                break;
            }
            <% } else { %>
                if(packageInfo[i].name == selectedUsagePlan && packageInfo[i].subscriptionCharge != "0") {
                    jQuery.ajax({
                        type: 'POST',
                        url: '../payment/upgrade_registration_usage_plan_ajaxprocessor.jsp',
                        data: {selectedUsagePlan: selectedUsagePlan, regTenantDomain: regTenantDomain},
                        async: false,
                        success: function(msg) {
                    }});
                    location.href = "../tenant-register/success_register.jsp";
                    break;
                } else if (packageInfo[i].name == selectedUsagePlan && packageInfo[i].subscriptionCharge == "0") {
                    location.href = "../tenant-register/success_register.jsp";
                    break;
                }
        <% } %>

        }
    }

    function cancelPaymet() {
        location.href = "../tenant-register/success_register.jsp";
    }

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
                                  option = document.createElement("option");
                                  option.value = name;
                                  option.innerHTML = name;
                                  document.getElementById('selectedUsagePlan').appendChild(option);

                              }
                          }
            );
</script>


<%
    boolean isPublicCloud = CommonUtil.isPublicCloudSetup();

    session.setAttribute(StratosConstants.ORIGINATED_SERVICE,
            request.getParameter(StratosConstants.ORIGINATED_SERVICE));
    String domain = (String) session.getAttribute("temp-domain-to-register");
    if (domain == null) {
        domain = "";
    }
    session.removeAttribute("temp-domain-to-register");
    // hm, i'm not sure whether we really need to clear the success key, if someone in the same session
    // have succeeded validating the domain, we should let them ignore the validation step. but if
    // someone look at this without much investigation, he may think this is a bug, so thought of clearing
    // it anyway...
    session.removeAttribute("validate-domain-success-key");
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
<link href="../tenant-register/css/tenant-register.css" rel="stylesheet" type="text/css" media="all"/>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<script type="text/javascript">
    jQuery(document).ready(function() {

        jQuery(".toggle_container").show();
        /*Hide (Collapse) the toggle containers on load use show() instead of hide() in the
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
<div id="middle">

    <h2><fmt:message key="register.new.organization"/></h2>

    <div id="workArea">
        <div class="registration_help"><fmt:message key="required.msg"/></div>
        <div id="activityReason" style="display: none;"></div>
        <form id="selectUsagePlan" action="init_payment_ajaxprocessor.jsp" method="post">

            <table class="styledLeft">
                <tbody>
                <tr>
                    <td class="nopadding">
                        <h2 class="trigger"><a href="#"><fmt:message key="usage.plan.information"/></a></h2>

                        <div class="toggle_container">
                            <table class="normal-nopadding" cellspacing="0">
                                <tbody>
                                <tr>
                                    <td class="leftCol-med">
                                        <fmt:message key="select.usage.plan.for.tenant"/><span class="required">*</span>
                                    </td>
                                    <td colspan="2">
                                        <select name="selectedUsagePlan" id="selectedUsagePlan"
                                                onchange="showRentalMessage();">
                                        </select>
                                        <a href="<%=CommonUtil.getStratosConfig().getUsagePlanURL()%>" target="_blank">
                                            <b>Pricing Info</b>
                                        </a>
                                    </td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td colspan="2" id="packagePrice" class="registration_help"><b><fmt:message
                                            key="demo.package.price"/></b></td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td colspan="2" class="registration_help"><fmt:message
                                            key="select.package.message"/>
                                    </td>
                                </tr>
                                </tbody>
                            </table>
                        </div>                        
                    </td>
                </tr>
                <tr id="buttonRow">
                    <td class="buttonRow">
                        <input class="button" id="submit-button" type="button" style="float:left; margin-top:4px; margin-right:4px;"
                               value="Submit" onclick="makePayment();jQuery(this).attr('disabled', true)"/>
                        <input class="button" id="cancel-button" type="button" style="float:left; margin-top:4px"
                               value="Cancel" onclick="cancelPaymet()"/>
                        <div id="waitMessage" style="font-size:13px !important;margin-top:5px; float:left; display:none"><img
                                src="images/ajax-loader.gif" align="left" hspace="20"/>Connecting to PayPal ...
                        </div>
                        <div style="clear:both"></div>
                    </td>
                </tr>
                </tbody>
            </table>
        </form>
        <br/>
    </div>
</div>
</fmt:bundle>


