<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one
  ~ or more contributor license agreements. See the NOTICE file
  ~ distributed with this work for additional information
  ~ regarding copyright ownership. The ASF licenses this file
  ~ to you under the Apache License, Version 2.0 (the
  ~ "License"); you may not use this file except in compliance
  ~ with the License. You may obtain a copy of the License at
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
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.utils.ThemeUtil" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
response.setHeader("Pragma", "no-cache");
%>

<jsp:include page="../dialog/display_messages.jsp"/>

<!-- YUI inculudes for rich text editor -->
<link rel="stylesheet" type="text/css"
      href="../yui/build/editor/assets/skins/sam/simpleeditor.css"/>
<link type="text/css" href="css/tenent-theme.css" rel="stylesheet" />
<link rel="stylesheet" type="text/css" href="../yui/assets/yui.css" >
<link rel="stylesheet" type="text/css" href="../yui/build/menu/assets/skins/sam/menu.css" />
<link rel="stylesheet" type="text/css" href="../yui/build/button/assets/skins/sam/button.css" />
<link rel="stylesheet" type="text/css" href="../yui/build/container/assets/skins/sam/container.css" />
<link rel="stylesheet" type="text/css" href="../yui/build/autocomplete/assets/skins/sam/autocomplete.css" />
<link rel="stylesheet" type="text/css" href="../yui/build/editor/assets/skins/sam/editor.css" />

<script type="text/javascript" src="../yui/build/yahoo-dom-event/yahoo-dom-event.js"></script>
<script type="text/javascript" src="../yui/build/element/element-beta-min.js"></script>
<script type="text/javascript" src="../yui/build/container/container_core-min.js"></script>
<script type="text/javascript" src="../yui/build/editor/simpleeditor-min.js"></script>

<script type="text/javascript" src="../admin/js/jquery.js"></script>
<script type="text/javascript" src="../admin/js/jquery.form.js"></script>
<script type="text/javascript" src="../dialog/js/jqueryui/jquery-ui.min.js"></script>

<!-- other includes -->
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="../tenant-theme/js/theme_resource_util.js"></script>
<jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../resources/js/resource_util.js"></script>
<script type="text/javascript" src="../tenant-theme/js/theme_resource_util.js"></script>
<link rel="stylesheet" type="text/css" href="../resources/css/registry.css"/>
<link rel="stylesheet" type="text/css" href="../tenant-theme/css/tenant.css"/>
<link rel="stylesheet" type="text/css" href="../tenant-theme/css/theme-mgt.css"/>

<script language="javascript" type="text/javascript">
function setChecked(value,id){
	var itemLength = document.forms['ThemeMgtForm'].elements['theme'].length; 
	setCheckedValue(document.forms['ThemeMgtForm'].elements['theme'], value);
	for(var i=0;i<=itemLength;i++){
		YAHOO.util.Dom.removeClass('themeObj'+i,'sel-box');
		YAHOO.util.Dom.addClass('themeObj'+i,'nor-box');
	}	
	YAHOO.util.Dom.removeClass('themeObj'+id,'nor-box');
	YAHOO.util.Dom.addClass('themeObj'+id,'sel-box');
	
}
function setCheckedValue(radioObj, newValue) {
	if(!radioObj)
		return;
	var radioLength = radioObj.length;
	if(radioLength == undefined) {
		radioObj.checked = (radioObj.value == newValue.toString());
		return;
	}
	for(var i = 0; i < radioLength; i++) {
		radioObj[i].checked = false;
		if(radioObj[i].value == newValue.toString()) {
			radioObj[i].checked = true;
		}
	}
}

</script>
<%--Syntax hylighter include--%>
<script language="javascript" type="text/javascript" src="../tenant-theme/js/editarea/edit_area/edit_area_full.js"></script>

<%
    // the redirectWithStr will be used in the following operations
    String redirectWithStr = request.getParameter("redirectWith");
    if (redirectWithStr == null) {
        redirectWithStr = "";
    }
%>

<fmt:bundle basename="org.apache.stratos.theme.mgt.ui.i18n.Resources">

<%

    String updateThemeVal = request.getParameter("updateTheme");
     if("Failed".equals(updateThemeVal)){
%>

    <script type="text/javascript">
        jQuery(document).ready(function() {
            CARBON.showErrorDialog('Error in updating the theme, Please try again setting the theme again.');
        });
    </script>
<%
    }

    String onceMessageShowed = (String)session.getAttribute("once-message-showed");
    if ("Success".equals(updateThemeVal) && !"true".equals(onceMessageShowed)) {
%>
    <script type="text/javascript">
        // we need to refresh the page to load the theme
        jQuery(document).ready(function() {
            CARBON.showWarningDialog('The theme successfully updated. Please refresh the page to load the theme.');
        });
    </script>
<%
        session.setAttribute("once-message-showed", "true");
    }
    if ("Success".equals(updateThemeVal) && "true".equals(onceMessageShowed)) {
        session.setAttribute("once-message-showed", "false");
%>
    <script type="text/javascript">
        // we need to refresh the page to load the theme
        window.location.href="theme_mgt.jsp?redirectWith=<%=redirectWithStr%>";
    </script>
<%
    }


    ThemeMgtServiceClient client;
    String[] allThemes;
    try {
        client = new ThemeMgtServiceClient(config, session);

        allThemes = client.getAllThemes(redirectWithStr);
    } catch (Exception e) {
%>
<jsp:forward page="../admin/error.jsp?<%=e.getMessage()%>"/>
<%
        return;
    }
    String parentPathToUpload = "/";
    String logoFilename = "logo.gif";
    
    if (allThemes.length < 2) {
        return;
    }

    // to derive the thumbnail url
    //Customization of UI theming per tenant


%>


<carbon:breadcrumb label="resources"
                       resourceBundle="org.wso2.carbon.registry.resource.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>
<div id="middle">

<h2> Theme Management </h2>

<div id="workArea">
    
<div class="subtopic">Select a Theme</div>
<div class="subcontent">
<form id="ThemeMgtForm" name="ThemeMgtForm" action="theme_mgt_ajaxprocessor.jsp?redirectWith=<%=redirectWithStr%>" method="post">

<%
String selectedTheme = allThemes[0];

for (int i = 1; i < allThemes.length; i ++) {
    String theme = allThemes[i];
    String checkedStr = "";
    String thumbUrl = ThemeUtil.getThumbUrl(request, theme);
    if (theme.equals(selectedTheme)) {
        checkedStr = "checked";
    }
%>
<a onclick="setChecked('<%=theme%>','<%=i%>')">
<div <%if (theme.equals(selectedTheme)) {%>class="sel-box"<% } else { %>class="nor-box"<%}%> id="themeObj<%=i%>">
	<span><%=theme%></span>
	<div>
		<input style="display:none" type="radio"  name="theme" checked="<%=checkedStr%>" value="<%=theme%>"/>
		<div><img src="<%=thumbUrl%>"/></div>
	</div>
</div>
</a>
<%
}
%>
<div style="clear:both;"></div>
<p>
<%
    if (redirectWithStr.equals("")) {
        // mean the user is logged in
%>
<input type="submit" value="Update"/>
<%
    } else {
%>
<input type="submit" value="Apply theme"/>
<input type="button" value="Skip" onclick="javascript:location.href='../admin/login.jsp'"/>
<%
    }
%>
</p>
</form>
</div>

<%
    if (redirectWithStr.equals("")) {
        // mean the user is logged in
%>
    
<div class="subtopic">Customize the theme</div>
<div class="subcontent">
<a href="theme_advanced.jsp">Click here</a>  to customize the current theme
</div>
</div>

<%
    }
%>
    
</div>

</fmt:bundle>
