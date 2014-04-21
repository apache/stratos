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
<%@ page contentType="text/html;charset=UTF-8" language="java" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<fmt:bundle basename="org.wso2.carbon.i18n.Resources">
	<table class="page-header-links-table" cellspacing="0">
		<tr>
			<td class="breadcrumbs">
			<table class="breadcrumb-table" cellspacing="0">
				<tr>								 
				    <td>
					    <div id="breadcrumb-div"></div>
                    </td>
				</tr>

			</table>
			</td>
<%
    String requestURI = request.getHeader("Referer");
    if (requestURI != null && requestURI.indexOf("?") > 0) {
        requestURI = requestURI.substring(0, requestURI.indexOf("?"));
    } else {
        requestURI = "";
    }
    requestURI = "blah";
    if (requestURI.endsWith("/admin/login.jsp")) { %>
            <td class="page-header-help"><a href="../docs/signin_userguide.html"
				target="_blank"><fmt:message key="component.help" /></a></td>
<% } else if (requestURI.endsWith("/admin/error.jsp")) { %>
            <td class="page-header-help"></td>
<% } else { %>
			<td class="page-header-help"><a href="./docs/userguide.html"
				target="_blank"><fmt:message key="component.help" /></a></td>
<% } %>
		</tr>
	</table>
</fmt:bundle>