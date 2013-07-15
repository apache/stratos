<!--
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
 -->
<%@ page import="org.wso2.carbon.register.ui.utils.TenantConfigUtil" %>
<%@ page import="org.apache.stratos.common.constants.StratosConstants"%>
<%@ page import="org.wso2.carbon.captcha.mgt.constants.CaptchaMgtConstants"%>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<carbon:jsi18n
		resourceBundle="org.wso2.carbon.register.ui.i18n.JSResources"
		request="<%=request%>" />
<%

    try {
        String gotoConfirmDomain = request.getParameter("domain-confirmation");
        String domain = request.getParameter("domain");

        String key = TenantConfigUtil.registerTenantConfigBean(request, config, session);

        session.removeAttribute(StratosConstants.ORIGINATED_SERVICE);
        session.setAttribute("add-tenant-success", "true");
        session.setAttribute("regTenantDomain", domain);

        String contextPath = "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + request.getParameter("domain");
        //response.sendRedirect(contextPath + "/carbon/tenant-theme/theme_mgt.jsp?redirectWith=" + key);
        // now we send the login page.

       // response.sendRedirect(contextPath + "/carbon/admin/login.jsp");

        if (gotoConfirmDomain != null && gotoConfirmDomain.equals("yes")) {
            // send me to the domain validation form
            response.sendRedirect("../validate-domain/validate_domain_not_logged_in.jsp?domain=" + domain);
        }
        else {
            // send me direct to the add tenant form, without successkey they will add the suffix -unverified
            response.sendRedirect("../tenant-register/select_usage_plan.jsp");
        }

    } catch (Exception e) {
        String msg = e.getMessage();
        TenantConfigUtil.setSubmissionValuesForSession(request);
        if (msg.contains(CaptchaMgtConstants.CAPTCHA_ERROR_MSG)) {
            session.setAttribute("kaptcha-status", "failed");
        } else {
        session.setAttribute("add-tenant-failed", "true");
        }
        response.sendRedirect("../tenant-register/select_domain.jsp");
        return;
    }
%>
