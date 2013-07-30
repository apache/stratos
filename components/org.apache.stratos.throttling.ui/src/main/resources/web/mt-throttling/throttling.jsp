<%--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements.  See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership.  The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License.  You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing,
  ~ software distributed under the License is distributed on an
  ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  ~ KIND, either express or implied.  See the License for the
  ~ specific language governing permissions and limitations
  ~ under the License.
  --%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page
	import="org.apache.stratos.throttling.ui.clients.ThrottlingRuleEditorClient"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
	prefix="carbon"%>

<carbon:jsi18n
	resourceBundle="org.apache.stratos.throttling.ui.i18n.JSResources"
	request="<%=request%>" />



<fmt:bundle basename="org.apache.stratos.throttling.ui.i18n.Resources">
	<carbon:breadcrumb label="tenant.menu"
		resourceBundle="org.apache.stratos.throttling.ui.i18n.Resources"
		topPage="true" request="<%=request%>" />
	<jsp:include
		page="../registry_common/registry_common-i18n-ajaxprocessor.jsp" />
	<script type="text/javascript"
		src="../registry_common/js/registry_validation.js"></script>
	<script type="text/javascript"
		src="../registry_common/js/registry_common.js"></script>
	<script type="text/javascript" src="../ajax/js/prototype.js"></script>
	<script type="text/javascript" src="js/register_config.js"></script>
	<%
	String error = request.getParameter("error");
	if ("true".equals(error)) {
%>
    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showErrorDialog('Error in updating the rules. ' +
                    'Please make sure the syntax of the rules are correct.');
        });
    </script>

<%	    
	} else if("false".equals(error)) {
	    %>
	    <script type="text/javascript">
	        jQuery(document).ready(function() {
	            CARBON.showInfoDialog('The rules are updated successfully.');
	        });
	    </script>

	<%	 	    
	}
	%>
	<%
	    String ruleContent;
	        try {
	            ThrottlingRuleEditorClient client =
	                    new ThrottlingRuleEditorClient(config, session);
	            ruleContent = client.retrieveThrottlingRules();
	        } catch (Exception e) {
	            String error1 = e.getMessage();
	            request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error1, error1, null));
	 %>
                <jsp:include page="../admin/error.jsp"/>
     <%
	    return;
	        }
	%>

	<div id="middle">

	<h2><fmt:message key="throttling.rules" /></h2>

	<div id="workArea">

	<form id="throttlingForm" action="throttling_ajaxprocessor.jsp" method="post">

	<div>
	<textarea rows="25" cols="110" name="content" id="content"><%=ruleContent%></textarea>
	</div>

	<div>
	<input type="submit" value="Update"/>
	</div>
	</form>
	</div>
	</div>
</fmt:bundle>

