<%--
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
  --%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>


<jsp:include page="../dialog/display_messages.jsp"/>

<!-- YUI inculudes for rich text editor -->
<link rel="stylesheet" type="text/css"
      href="../yui/build/editor/assets/skins/sam/simpleeditor.css"/>
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

<!-- other includes -->
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../resources/js/resource_util.js"></script>
<script type="text/javascript" src="../tenant-theme/js/theme_resource_util.js"></script>
<link rel="stylesheet" type="text/css" href="../resources/css/registry.css"/>
<link rel="stylesheet" type="text/css" href="../tenant-theme/css/tenant.css"/>

<%--Syntax hylighter include--%>
<script language="javascript" type="text/javascript" src="../tenant-theme/js/editarea/edit_area/edit_area_full.js"></script>

<fmt:bundle basename="org.wso2.carbon.registry.resource.ui.i18n.Resources">
<%
    ThemeMgtServiceClient client = new ThemeMgtServiceClient(config, session);
    boolean resourceExists = false;
    try {
        if (client.getResourceTreeEntry("/") != null) {
            resourceExists = true;
        }
    } catch (Exception e) {
        resourceExists = false;
    }
%>

<script type="text/javascript">
  <!--
  sessionAwareFunction(function() {
  <% if (!resourceExists) {
  //TODO: We should be able to distinguish the two scenarios below. An authorization failure
  //generates a AuthorizationFailedException which doesn't seem to arrive at this page.
  %>
      CARBON.showErrorDialog("<fmt:message key="unable.to.browse"/>",function(){
          location.href="../admin/index.jsp";
          return;
      });
  <% } else { %>
      loadMediaTypes();
  <% } %>
  }, "<fmt:message key="session.timed.out"/>");
  // -->

  </script>

<%
    if (!resourceExists) {
        return;
    }
%>

<carbon:breadcrumb label="resources"
                       resourceBundle="org.wso2.carbon.registry.resource.ui.i18n.Resources"
                       topPage="false" request="<%=request%>"/>

    <%
        String contraction = "min";
	if(session.getAttribute( "contraction" )!=null){
		contraction = (String)session.getAttribute( "contraction" );
	}
    %>
    <style type="text/css">
        .yui-skin-sam h3 {
            font-size: 10px !important;
        }

        .yui-toolbar-container .yui-toolbar-titlebar h2 a {
            font-size: 11px !important;
        }
    </style>
    <div id="middle">

<fmt:bundle basename="org.apache.stratos.theme.mgt.ui.i18n.Resources">
        <h2><fmt:message key="theme.management"/></h2>
</fmt:bundle>
        <div id="workArea">
            
            
            <jsp:include page="css-editor.jsp" />
            <div class="resource-path">
                <jsp:include page="metadata_resourcepath.jsp"/>
            </div>
            <div id="contentDiv">
                <jsp:include page="content_ajaxprocessor.jsp"/>
            </div>
        </div>

    </div>

<%--    <div id="resourceTree" style="display:none" class="resourceTreePage">
        <div class="ajax-loading-message">
            <img src="/wso2registry/admin/images/ajax-loader.gif" align="top"/>
            <span><fmt:message key="resource.tree.loading.please.wait"/> ..</span>
        </div>
    </div>
    <div id="popup-main" style="display:none" class="popup-main">
    </div>--%>
</fmt:bundle>
