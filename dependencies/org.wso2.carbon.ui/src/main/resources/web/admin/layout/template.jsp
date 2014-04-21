<%--
 Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.

 WSO2 Inc. licenses this file to you under the Apache License,
 Version 2.0 (the "License"); you may not use this file except
 in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 --%>
<%  response.addHeader( "X-FRAME-OPTIONS", "DENY" ); %>
<%@ page import="java.util.Locale" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.registry.core.RegistryConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page import="org.wso2.carbon.base.ServerConfiguration" %>
<%@ taglib uri="http://tiles.apache.org/tags-tiles" prefix="tiles" %>
 <%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

 <%
   Locale locale = null;
   CarbonUIUtil.setLocaleToSession(request);
   locale = CarbonUIUtil.getLocaleFromSession(request);
 %>

    <fmt:setLocale value="<%=locale%>" scope="session"/>
    <fmt:bundle basename="org.wso2.carbon.i18n.Resources">
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<%
	//Customization of UI theming per tenant
	String tenantDomain = null;
	String globalCSS = "../admin/css/global.css";
	String mainCSS = "";
	if (request.getSession()
			.getAttribute(MultitenantConstants.TENANT_DOMAIN) != null) {
		tenantDomain = (String) request.getSession().getAttribute(
				MultitenantConstants.TENANT_DOMAIN);
	} else {
		// user is not logged in or just logged out, but still they are inside url own to the domain
		tenantDomain = (String) request
				.getAttribute(MultitenantConstants.TENANT_DOMAIN);
	}
	if (tenantDomain != null && !MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
        String themeRoot = "../../../../t/" + tenantDomain
				+ "/registry/resource"
				+ RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
				+ "/repository";
		mainCSS = themeRoot + "/theme/admin/main.css";
        if (request.getSession().getAttribute(
                CarbonConstants.THEME_URL_RANDOM_SUFFIX_SESSION_KEY) != null) {
            // this random string is used to get the effect of the theme change, where-ever the
            // theme is changed, this session will be changed
            mainCSS += "?rsuffix=" + request.getSession().getAttribute(
                CarbonConstants.THEME_URL_RANDOM_SUFFIX_SESSION_KEY);
        }
    } else {
        if ("true".equals(ServerConfiguration.getInstance().getFirstProperty(CarbonConstants.IS_CLOUD_DEPLOYMENT))) {
            mainCSS = "../../registry/resource"
                      + RegistryConstants.GOVERNANCE_REGISTRY_BASE_PATH
                      + "/repository/components/org.wso2.carbon.all-themes/Default/admin/main.css";
        } else {
            mainCSS = "../styles/css/main.css";
        }
	}
	//read web application's title if it's set on product.xml
    String webAdminConsoleTitle = (String) config.getServletContext().getAttribute(CarbonConstants.PRODUCT_XML_WSO2CARBON +
            CarbonConstants.PRODUCT_XML_WEB_ADMIN_CONSOLE_TITLE);
%>
<head>
    <meta http-equiv="content-type" content="text/html;charset=utf-8"/>
    <%if(webAdminConsoleTitle != null && webAdminConsoleTitle.trim().length() > 0){ %>
    <title><%=webAdminConsoleTitle%></title>
    <%}else{ %>
    <title><tiles:getAsString name="title"/></title>
    <%}%>
    <link href="<%=globalCSS%>" rel="stylesheet" type="text/css" media="all"/>
<%
	Object param = session.getAttribute("authenticated");
	if (param != null && (Boolean) param) {
%>
    <link href="../admin/jsp/registry_styles_ajaxprocessor.jsp" rel="stylesheet" type="text/css"
          media="all"/>
<%
	}
%>
    <link href="<%=mainCSS%>" rel="stylesheet" type="text/css" media="all"/>
    <link href="../dialog/css/jqueryui/jqueryui-themeroller.css" rel="stylesheet" type="text/css"
          media="all"/>
    <link href="../dialog/css/dialog.css" rel="stylesheet" type="text/css" media="all"/>
    <link rel="stylesheet" href="../admin/css/carbonFormStyles.css">
    <!--[if gte IE 8]>
    <link href="../dialog/css/dialog-ie8.css" rel="stylesheet" type="text/css" media="all"/>        
    <![endif]-->
    <!--[if gte IE 7]>
    <link href="../dialog/css/dialog-ie8.css" rel="stylesheet" type="text/css" media="all"/>
    <![endif]-->
    <link rel="icon" href="../admin/images/favicon.ico" type="image/x-icon"/>
    <link rel="shortcut icon" href="../admin/images/favicon.ico" type="image/x-icon"/>

    <script type="text/javascript" src="../admin/js/jquery-1.5.2.min.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.form.js"></script>
    <script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.validate.js"></script>    
    <script type="text/javascript" src="../admin/js/jquery.cookie.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.ui.core.min.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.ui.widget.min.js"></script>
    <script type="text/javascript" src="../admin/js/jquery.ui.tabs.min.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <script type="text/javascript" src="../admin/js/WSRequest.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>

    <script type="text/javascript" src="../admin/js/customControls.js"></script>
</head>
<%
	//set cookie containing collapsed menu items
	Object o = config.getServletContext().getAttribute(
			CarbonConstants.PRODUCT_XML_WSO2CARBON + "collapsedmenus");
	if (o != null) {
		ArrayList collapsedMenuItems = (ArrayList) o;
		Iterator itrCollapsedMenuItems = collapsedMenuItems.iterator();
		%>
		<script type="text/javascript">
		<%
		while (itrCollapsedMenuItems.hasNext()) {
			String menuItem = (String) itrCollapsedMenuItems.next();
			out.print("if(getCookie('" + menuItem + "') == null){\n");
			out.print("  setCookie('" + menuItem + "', 'none');\n");
			out.print("}\n");
		}
		%>
		</script>
		<%
	}
%>
<body>
<jsp:include page="../../admin/jsp/browser_checker.jsp" />
<div id="dcontainer"></div>
<script type="text/javascript" src="../dialog/js/dialog.js"></script>

<!-- JS imports for collapsible menu -->
<script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
<script src="../yui/build/animation/animation-min.js" type="text/javascript"></script>
<script src="../admin/js/template.js" type="text/javascript"></script>
<script src="../yui/build/yahoo/yahoo-min.js" type="text/javascript"></script>
<script src="../yui/build/selector/selector-min.js" type="text/javascript"></script>

<table id="main-table" border="0" cellspacing="0">
    <tr>
        <td id="header" colspan="3"><tiles:insertAttribute name="header"/>
        </td>
    </tr>
    <tr>
        <td class="vertical-menu-container" id="vertical-menu-container" style="display:none;">
            <div id="menu-panel-button0"></div>
            <div id="menu-panel-button1" class="menu-panel-buttons"></div>
            <div id="menu-panel-button2" class="menu-panel-buttons"></div>
            <div id="menu-panel-button3" class="menu-panel-buttons"></div>
            <div id="menu-panel-button4" class="menu-panel-buttons"></div>
            <div id="menu-panel-button5" class="menu-panel-buttons"></div>
            <div id="menu-panel-button_dummy" style="display:none"></div>
        </td>
        <td id="menu-panel" valign="top">
            <table id="menu-table" border="0" cellspacing="0">
                <tr>
                    <td id="region1"><tiles:insertAttribute name="region1"/></td>
                </tr>
                <tr>
                    <td id="region2"><tiles:insertAttribute name="region2"/></td>
                </tr>
                <tr>
                    <td id="region3"><tiles:insertAttribute name="region3"/></td>
                </tr>
                <tr>
                    <td id="region4"><tiles:insertAttribute name="region4"/></td>
                </tr>
                <tr>
                    <td id="region5"><tiles:insertAttribute name="region5"/></td>
                </tr>
                <tr>
                    <td><img src="../admin/images/1px.gif" width="225px" height="1px"/></td>
                </tr>
            </table>
        </td>
        <td id="middle-content">
            <table id="content-table" border="0" cellspacing="0">
                <tr>
                    <td id="page-header-links"><tiles:insertAttribute name="breadcrumb"/></td>
                </tr>
                <tr>
                    <td id="body">
                        <img src="../admin/images/1px.gif" width="735px" height="1px"/>
                        <tiles:insertAttribute name="body"/>
                    </td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td id="footer" colspan="3"><tiles:insertAttribute name="footer"/></td>
    </tr>
</table>
<script type="text/javascript">
if (Function('/*@cc_on return document.documentMode===10@*/')()){
    document.documentElement.className+=' ie10';
}
</script>
</body>
</html>
</fmt:bundle>
