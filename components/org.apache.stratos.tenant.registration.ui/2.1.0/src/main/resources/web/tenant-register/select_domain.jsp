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

    var packageInfo;
    
    function setFreeUsagePlan() {
        var foundFreePlan = false;
        for (var i = 0; i < packageInfo.length; i++) {
            if (packageInfo[i].subscriptionCharge == "0") {
                document.getElementById("selectedUsagePlan").value = packageInfo[i].name;
                foundFreePlan = true;
                return;
            }
        }
        if(!foundFreePlan) {
            document.getElementById("selectedUsagePlan").value = packageInfo[0].name;
        }
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

                          }
            );
</script>


<%
    String domainName = "";
    String admin = "";
    String email = "";
    String firstname = "";
    String lastname = "";
    String usagePlan = "";
    String license = CommonUtil.getEula();
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

<%
    CaptchaInfoBean captchaInfoBean;
    try {
        captchaInfoBean = TenantConfigUtil.generateRandomCaptcha(config, session);
    } catch (Exception e) {
        return;
    }
    String captchaImagePath = captchaInfoBean.getImagePath();

    String captchaImageUrl = "../../" + captchaImagePath;
    String captchaSecretKey = captchaInfoBean.getSecretKey();

    if (session.getAttribute("submit-domain") != null) {
        domain = (String) session.getAttribute("submit-domain");
        session.setAttribute("submit-domain", null);
    }
    if (session.getAttribute("submit-admin") != null) {
        admin = (String) session.getAttribute("submit-admin");
        session.setAttribute("submit-admin", null);
    }
    if (session.getAttribute("submit-admin-email") != null) {
        email = (String) session.getAttribute("submit-admin-email");
        session.setAttribute("submit-admin-email", null);
    }
    if (session.getAttribute("submit-admin-firstname") != null) {
        firstname = (String) session.getAttribute("submit-admin-firstname");
        session.setAttribute("submit-admin-firstname", null);
    }
    if (session.getAttribute("submit-admin-lastname") != null) {
        lastname = (String) session.getAttribute("submit-admin-lastname");
        session.setAttribute("submit-admin-lastname", null);
    }
%>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%
    if ("failed".equals(session.getAttribute("kaptcha-status"))) {
        session.setAttribute("kaptcha-status", null);
%>

<script type="text/javascript">
    jQuery(document).ready(function() {
        CARBON.showWarningDialog('Please enter the letters shown as in the image to register.');
    });
</script>
<%
    }

    if ("true".equals(session.getAttribute("add-tenant-failed"))) {
        session.removeAttribute("add-tenant-failed");
%>

<script type="text/javascript">
    jQuery(document).ready(function() {
        CARBON.showWarningDialog('Organization registration failed. Please try again with a different domain.');
    });
</script>
<%
    }
%>
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
        <form id="addTenantForm" action="submit_tenant_ajaxprocessor.jsp" method="post">

            <table class="styledLeft">
                <tbody>
                <tr>
                    <td class="nopadding">
                        <h2 class="trigger"><a href="#"><fmt:message key="domain.information"/></a></h2>

                        <div class="toggle_container">
                            <table class="normal-nopadding" cellspacing="0">
                                <tbody>
                                <tr>
                                    <td class="leftCol-med"><fmt:message key="domain"/><span class="required">*</span>
                                    </td>
                                    <td colspan="2"><input onblur="fillAdminValue()"
                                                           value="<%=domain%>" type="text"
                                                           name="domain" id="domain"
                                                           style="width:400px" value=""/>
                                        <input type="button" value="Check Availability"
                                               onclick="checkDomainAvailability(<%=isPublicCloud?"true":"false"%>)"/> <span
                                                id="busyCheck"></span>
                                    </td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td colspan="2">Provide a domain for your tenant, in the
                                        format "example.com". This domain should be unique.
                                        If this is a legitimate domain, optionally you will be able to validate the ownership of the domain.
                                    </td>
                                </tr>

                                <tr>
                                    <td></td>
                                    <td colspan="2">
                                        <div id="domain-confirmation-msg"></div>
                                    </td>
                                </tr>

                                <tr>
                                    <td></td>
                                    <td colspan="2">
                                        <input type="checkbox" name="domain-confirmation"
                                               value="yes">Validate the domain now
                                        (optional)</input>
                                    </td>
                                </tr>

                                </tbody>
                            </table>
                        </div>

                        <h2 class="trigger"><a href="#"><fmt:message key="contact.details"/></a></h2>

                        <div class="toggle_container">
                            <table class="normal-nopadding" cellspacing="0">
                                <tbody>
                                <tr>
                                    <td class="leftCol-med"><fmt:message key="admin.firstname"/><span
                                            class="required">*</span></td>
                                    <td><input type="text" name="admin-firstname" id="admin-firstname"
                                               style="width:400px" value="<%=firstname%>"/></td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="admin.lastname"/><span class="required">*</span></td>
                                    <td><input type="text" name="admin-lastname" id="admin-lastname" style="width:400px"
                                               value="<%=lastname%>"/></td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="admin.username"/><span class="required">*</span></td>
                                    <td><input type="text" name="admin" id="admin" style="width:400px"
                                               value="<%=admin%>"/><span id="adminValue"></span></td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="admin.password"/>
                                        <span class="required">*</span></td>
                                    <td><input type="password" name="admin-password" id="admin-password"
                                               style="width:400px"/></td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td class="registration_help">(Minimum of 6 Characters in length)</td>
                                </tr>
                                <tr>
                                    <td><fmt:message key="admin.password.repeat"/>
                                        <span class="required">*</span></td>
                                    <td><input type="password" name="admin-password-repeat" id="admin-password-repeat"
                                               style="width:400px"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-med"><fmt:message key="admin.email"/><span
                                            class="required">*</span></td>
                                    <td colspan="2"><input type="text" name="admin-email" id="admin-email"
                                                           style="width:400px" value="<%=email%>"/></td>
                                </tr>
                                <tr>
                                    <td class="leftCol-med"><fmt:message key="word.verification"/><span
                                            class="required">*</span></td>
                                    <td colspan="2"><fmt:message key="captcha.message"/></td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td colspan="2">
                                        <div id="kaptchaImgDiv"></div>
                                    </td>
                                </tr>
                                <tr>
                                    <td></td>
                                    <td colspan="2" height="100"><input type="text" id="captcha-user-answer"
                                                                        name="captcha-user-answer" style="width:400px"
                                                                        value=""/></td>
                                </tr>
                                </tbody>
                            </table>
                        </div>

                        <%
                            if (isPublicCloud) {
                        %>
                        <h2 class="trigger"><a href="#"><fmt:message key="terms.of.use"/></a></h2>

                        <div class="toggle_container">
                            <table class="normal-nopadding" cellspacing="0">
                                <tbody>


                                <tr></tr>
                                <tr>
                                    <td>
                                        <textarea rows="10" readonly="readonly" name="license-text"
                                                  style="width:100%"><%=license%>
                                        </textarea>
                                    </td>
                                </tr>
                                <tr>
                                    <td><input type="checkbox" name="activateButton" id="activateButton"
                                               onclick="activateSubmit(form.activateButton.checked)"/>
                                        <label for="activateButton"><fmt:message key="accept.eula"/></label></td>
                                </tr>
                                <%
                                    }
                                %>


                                </tbody>
                            </table>
                        </div>
                    </td>
                </tr>
                <tr id="buttonRow" colspan="2">
                    <td class="buttonRow">
                        <input type="hidden" name="selectedUsagePlan" id="selectedUsagePlan"/>
                        <input type="hidden" name="captcha-secret-key" value="<%=captchaSecretKey%>"/>
                        <%
                            if (isPublicCloud) {
                        %>
                        <input class="button" id="submit-button" type="button" disabled="disabled" style="float:left; margin-top:4px"
                               value="Next >" onclick="setFreeUsagePlan();addTenant();jQuery(this).attr('disabled', true);jQuery('#waitMessage').toggle('fast')"/>
                        <%
                        } else {
                        %>
                        <input class="button" id="submit-button" type="button" style="float:left; margin-top:4px"
                               value="Next >" onclick="setFreeUsagePlan();addTenant();jQuery(this).attr('disabled', true);jQuery('#waitMessage').toggle('fast')"/>
                        <%
                            }
                        %>
                        <div id="waitMessage" style="font-size:13px !important;margin-top:5px; float:left;display:none"><img
                                src="images/ajax-loader.gif" align="left" hspace="20"/>Registering new tenant ...
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
<script type="text/javascript">
    showKaptcha('<%=captchaImageUrl%>');
</script>
</fmt:bundle>


