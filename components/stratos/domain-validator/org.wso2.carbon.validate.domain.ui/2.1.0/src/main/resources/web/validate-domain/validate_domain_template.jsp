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
<%@ page
	import="org.wso2.carbon.validate.domain.ui.clients.ValidateDomainClient"%>
<%@ page import="org.wso2.carbon.validate.domain.ui.utils.Util"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page
	import="org.wso2.carbon.utils.multitenancy.MultitenantConstants"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>
<link rel="stylesheet" type="text/css" href="../yui/assets/yui.css">
<link rel="stylesheet" type="text/css"
	href="../yui/build/menu/assets/skins/sam/menu.css" />
<link rel="stylesheet" type="text/css"
	href="../yui/build/button/assets/skins/sam/button.css" />
<link rel="stylesheet" type="text/css"
	href="../yui/build/container/assets/skins/sam/container.css" />
<link rel="stylesheet" type="text/css"
	href="../yui/build/autocomplete/assets/skins/sam/autocomplete.css" />
<link rel="stylesheet" type="text/css"
	href="../yui/build/editor/assets/skins/sam/editor.css" />
<link rel="stylesheet" type="text/css"
	href="../validate-domain/css/validate_domain.css" />
<jsp:include
	page="../registry_common/registry_common-i18n-ajaxprocessor.jsp" />
<script type="text/javascript"
	src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript"
	src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.validate.domain.ui.i18n.JSResources"
        request="<%=request%>" namespace="org.wso2.carbon.validate.domain.ui"/>

<script type="text/javascript" src="../validate-domain/js/validate_domain.js"></script>

<fmt:bundle
	basename="org.wso2.carbon.validate.domain.ui.i18n.Resources">
	<%
	    String domain = request.getParameter("domain");
		String status = request.getParameter("status");
	        // get the validation key
	        ValidateDomainClient client =
	                Util.getValidateDomainClient(request, config, session);
	        String domainValidationKey = client.getDomainValidationKey(domain);
	        if ("unavailable".equals(domainValidationKey)) {
           %>
               <script type="text/javascript">
                   jQuery(document).ready(function() {
                       CARBON.showErrorDialog('The domain you are trying to validate ' +
                               'is already taken. Please retry with another domain.');
                   });
               </script>

           <%
               }
	        //String serverUrl = CarbonUIUtil.getServerURL(config.getServletContext(), session);
	        String adminConsoleURL = CarbonUIUtil.getAdminConsoleURL("/");
	        String serverUrl = adminConsoleURL.replace("carbon","services");
	        // remove the services directory.
	        String serverRoot =
	                serverUrl.substring(0, serverUrl.length() - "/services/".length());
	        // we have to remove the port as well.
	        if (serverRoot == null) {
	            serverRoot = "stratoslive.wso2.com";
	        }
	        // server root without https
	        serverRoot = serverRoot.substring(8);
	        int semiCol = serverRoot.indexOf(':');
	        if (semiCol >= 0) {
	            // need to remove the port
	            serverRoot = serverRoot.substring(0, semiCol);
	        }

	        // we have additional statement
	        String statementToIgnoreValidate = "";
	        if (session.getAttribute(MultitenantConstants.TENANT_DOMAIN) == null) {
	            statementToIgnoreValidate =
	                    "or <input type='button' onclick='gotoRegister()' value='Skip the validation step'/>";
	        }
	%>
	<div id="middle">

    <carbon:breadcrumb label="validate"
        resourceBundle="org.wso2.carbon.validate.domain.ui.i18n.Resources"
        topPage="false" request="<%=request%>" />
    
	<div id="workArea">
	<h2><fmt:message key="domain.ownership.validation" /></h2>

	<div id="validation-introduction">
	<p>You can validate your domain using the following method <%=statementToIgnoreValidate%></p>
	<ul>
		<li class="catagory">Creating a text file in the domain web
		ROOT</li>
<!-- 		<li class="catagory">2. Setting up a CNAME entry in your DNS</li> -->
	</ul>
	</div>

	<div id="with-text">

	<table class="styledLeft">
		<thead>
			<tr>
				<th>Validate domain using a text file</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td>
				<p>Add a text file inside the web root of your domain with the following configuration:</p>
				<div class="conf-info">
				<%
				    // the filename is hard coded now, a change in this need to be reflected at validateDomainService.jsp
				%>
				<p>Filename: <strong>wso2multitenancy.txt</strong></p>
				<p>Text content: <strong><%=domainValidationKey%></strong></p>
				</div>
				<p class="validate-button"><em> Click the 'Validate'
				button, after you complete the above step </em> <input type="button"
					value="Validate" onclick="validateWithText('<%=domain%>', '<%=status%>')" /> <span
					id="busyText"></span></p>
				<p>
				<div id="with-text-msg"></div>
				</p>
				<p>
				<div id="with-text-continue" style="visibility: hidden">
				<form id="validateWithTextForm"
					onsubmit="return submitValidateWithTextForm('<%=status%>');" 
					action="submit_validate_domain_ajaxprocessor.jsp" method="post">
				<input type="hidden" id="with-text-success-key" name="successKey"
					value="" /> <input type="hidden" name="domain" value="<%=domain%>" />
				<input type="hidden" id="with-text-status" name="status" value="<%=status%>"/>
				<input type="submit" value="Continue" /></form>
				</div>
				</p>
				</td>
			</tr>
		</tbody>
	</table>
	</div>


<!-- 	<div id="with-dns"> -->

<!-- 	<table class="styledLeft"> -->
<!-- 		<thead> -->
<!-- 			<tr> -->
<!-- 				<th>Validate domain using a CName entry</th> -->
<!-- 			</tr> -->
<!-- 		</thead> -->
<!-- 		<tbody> -->
<!-- 			<tr> -->
<!-- 				<td> -->
<!-- 				<p>Put a CNAME entry in your DNS with following configuration: -->
<!-- 				(Note that if you refresh this page, the parameters will be changed) -->
<!-- 				</p> -->
<!-- 				<div class="conf-info"> -->
<%-- 				<p>Name: <strong><%=domainValidationKey%>.<%=domain%></strong></p> --%>
<!-- 				<p>Type: <strong>CNAME</strong></p> -->
<%-- 				<p>Value: <strong><%=serverRoot%></strong></p> --%>
<!-- 				</div> -->
<!-- 				<p class="validate-button"><em> Click the 'Validate' -->
<!-- 				button, after you finish setting the CNAME entry </em> <input -->
<!-- 					type="button" value="Validate" -->
<%-- 					onclick="validateWithDNS('<%=domain%>', '<%=status%>')" /> <span id="busyDNS"></span> --%>
<!-- 				</p> -->
<!-- 				<p> -->
<!-- 				<div id="with-dns-msg"></div> -->
<!-- 				</p> -->
<!-- 				<p> -->
<!-- 				<div id="with-dns-continue" style="visibility: hidden"> -->
<!-- 				<form id="validateWithDNSForm" -->
<!-- 					action="submit_validate_domain_ajaxprocessor.jsp" -->
<%-- 					onsubmit="return submitValidateWithDNSForm('<%=status%>');" method="post"> --%>
<!-- 				<input type="hidden" id="with-dns-success-key" name="successKey" -->
<%-- 					value="" /> <input type="hidden" name="domain" value="<%=domain%>" /> --%>
<%-- 				<input type="hidden" id="with-dns-status" name="status" value="<%=status%>"/> --%>
<!-- 				<input type="submit" value="Continue" /></form> -->
<!-- 				</div> -->
<!-- 				</p> -->
<!-- 				</td> -->
<!-- 			</tr> -->
<!-- 		</tbody> -->
<!-- 	</table> -->
<!-- 	</div> -->
	</div>
</fmt:bundle>