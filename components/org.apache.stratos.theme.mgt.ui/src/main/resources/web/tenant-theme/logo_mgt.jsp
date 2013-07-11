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
<%@ page import="org.wso2.carbon.registry.common.utils.RegistryUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.utils.ThemeUtil" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
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

</script>
<%--Syntax hylighter include--%>
<script language="javascript" type="text/javascript" src="../tenant-theme/js/editarea/edit_area/edit_area_full.js"></script>


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
        window.location.href="logo_mgt.jsp";
    </script>
<%
    }


    String parentPathToUpload = "/";
    String logoFilename = "logo.gif";

    // to derive the thumbnail url
    //Customization of UI theming per tenant


%>


<carbon:breadcrumb label="resources"
                       resourceBundle="org.apache.stratos.theme.mgt.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>
<div id="middle">
<h2><fmt:message key="logo.management"/></h2>
<div id="workArea">


<div class="subtopic"><fmt:message key="change.the.logo"/></div>
<div class="subcontent">

<table>
<tbody>
<tr>
    <td colspan="2">
    	<table class="normal">
    		<tr>
		    <td style="width:117px;*width:120px;">
                <img width="300px" src="<%=ThemeUtil.getLogoURL(config, session)%>"></td>
		    <td>
            <!-- we are creating another sub table here -->
            <table></tr></td>
		        <select id="addMethodSelector" onchange="viewAddResourceUI()">
		            <option value="upload" selected="selected"><fmt:message key="update.logo.image.from.file"/></option>
		            <option value="import"><fmt:message key="update.logo.image.from.url"/></option>
                </select>
		    </td>
            </tr>
            <tr>
                <td>

<table>
<!-- upload file UI -->
<tr  id="uploadContentUI">
<td colspan="2">
<form onsubmit="return submitUploadThemeContentForm();" method="post" name="resourceUploadForm"
      id="resourceUploadForm"
      action="../../fileupload/themeResource?redirectto=logo_mgt" enctype="multipart/form-data" target="_self">
    <input type="hidden" id="path" name="path" value="<%=parentPathToUpload%>"/>
    <input id="uResourceFile" type="file" name="upload" style="background-color:#cccccc" onkeypress="return blockManual(event)"
                   onchange="fillThemeResourceUploadMediaTypes();"/>
    <input type="button" value="Update" onclick="whileThemeResourceUpload();submitUploadThemeContentForm();"/>
            <div class="helpText" id="fileHelpText">

<fmt:bundle basename="org.apache.stratos.theme.mgt.ui.i18n.Resources">
                <fmt:message key="image.path.help.text"/>
</fmt:bundle>
            </div>
            <input id="uResourceName" name="filename" type="hidden" value="<%=logoFilename%>"
                   style="margin-bottom:10px;"/>
            <input id="uResourceMediaType" type="hidden" name="mediaType"
                   style="margin-bottom:10px;"/>
</form>
</td>
</tr>

<!-- import content UI -->

<tr id="importContentUI" style="display:none;">
<td colspan="2">
<form method="post" name="resourceImportForm"
      id="resourceImportForm"
      action="/wso2registry/system/fetchResource">
    <input type="hidden" id="irParentPath" name="path" value="<%=parentPathToUpload%>"/>
    <input id="irFetchURL" type="text" name="fetchURL"
           onchange="fillThemeResourceImportMediaTypes();"/>
    <input type="button" value="Update" onclick="whileThemeResourceUpload();submitImportThemeContentForm();"/>

            <div class="helpText" id="urlHelpText" style="color:#9a9a9a;">
                <fmt:message key="image.url.help.text"/>
            </div>
            <input id="irResourceName" name="resourceName" type="hidden" value="<%=logoFilename%>"
                   style="margin-bottom:10px;"/>
            <input id="irMediaType" type="hidden" name="mediaType"
                   style="margin-bottom:10px;"/>
</form>
</td>
</tr>

</table>    
            </td>
        </tr>
            </table>
            </td>
            </tr>
    	</table>

        <div id="whileUpload" style="display:none;padding-top:0px;margin-top:20px;margin-bottom:20px;" class="ajax-loading-message">
            <img align="top" src="../resources/images/ajax-loader.gif"/>
            <span><fmt:message key="wait.message"/></span>
        </div>
    </td>
</tr>


</tbody>
</table>
</div>
   
</div>

</fmt:bundle>
