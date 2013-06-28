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
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.wso2.carbon.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="java.util.UUID" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<carbon:jsi18n
		resourceBundle="org.wso2.carbon.theme.mgt.ui.i18n.JSResources"
		request="<%=request%>" />
<%

    String redirectWithStr = request.getParameter("redirectWith");
    if (redirectWithStr == null) {
        redirectWithStr = "";
    }
    try {
        ThemeMgtServiceClient client = new ThemeMgtServiceClient(config, session);
        String themeName = request.getParameter("theme");
        request.getSession().setAttribute(
                CarbonConstants.THEME_URL_RANDOM_SUFFIX_SESSION_KEY, UUID.randomUUID().toString());        
        client.applyTheme(themeName, redirectWithStr);
        if (redirectWithStr.equals("")) {
            response.sendRedirect("../tenant-theme/theme_mgt.jsp?updateTheme=Success&redirectWith=" + redirectWithStr);
        } else {
            response.sendRedirect("../admin/login.jsp");
        }
    } catch (UIException e) {
        response.sendRedirect("../tenant-theme/theme_mgt.jsp?updateTheme=Failed&redirectWith=" + redirectWithStr);
    }
%>
