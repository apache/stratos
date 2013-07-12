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

<%@ page import="org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.ContentBean" %>
<%@ page import="org.wso2.carbon.registry.resource.ui.Utils" %>
<%@ page import="org.wso2.carbon.registry.resource.ui.clients.CustomUIHandler" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.utils.ThemeUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<fmt:bundle basename="org.wso2.carbon.registry.resource.ui.i18n.Resources">

    <%
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String viewMode = Utils.getResourceViewMode(request);
        boolean isInlinedView = "inlined".equals(viewMode);
        ContentBean cb;
        ThemeMgtServiceClient client;


        try {
            client = new ThemeMgtServiceClient(cookie, config, session);
            cb = client.getContent(request);
        } catch (Exception e) {
    %>
    <jsp:forward page="../admin/error.jsp?<%=e.getMessage()%>"/>
    <%
            return;
        }

        String cuiURL = CustomUIHandler.getCustomViewUI(cb.getMediaType(), request.getSession());
        String mode = request.getParameter("mode");
        boolean cui = false;
        if (cuiURL != null && !"standard".equals(mode)) {
            cui = true;
        }
if(!cb.getAbsent().equals("true")){
    %>

    <div class="box1-head">
        <table cellspacing="0" cellpadding="0" border="0" style="width:100%">
            <tr>
                <td valign="top">
                    <h2 class="sub-headding-entries"><%if (cb.getCollection()) { %>
                        <fmt:message key="entries"/> <% } else { %>
                        <fmt:message key="content"/> <% } %></h2>
                </td>
                <td align="right" valign="top" class="expanIconCell">

                    <a onclick="javascript: showHideCommon('entriesIconExpanded');showHideCommon('entriesIconMinimized');showHideCommon('entriesExpanded');showHideCommon('entriesMinimized');">
                        <img src="../resources/images/icon-expanded.gif" border="0" align="top"
                             id="entriesIconExpanded"/>
                        <img src="../resources/images/icon-minimized.gif" border="0" align="top"
                             id="entriesIconMinimized" style="display:none;"/>
                    </a>


                </td>

            </tr>
        </table>
    </div>
    <div class="box1-mid-fill" id="entriesMinimized" style="display:none"></div>
    <div class="box1-mid" id="entriesExpanded">
        <%
            if (!isInlinedView) {
        %>

        <% if (cb.getCollection() && cb.getPutAllowed() && !cb.getVersionView() && cb.getLoggedIn() && !cui) { %>

<fmt:bundle basename="org.apache.stratos.theme.mgt.ui.i18n.Resources">
        <div style="display:block;height:30px;">
            <a class="add-resource-icon-link"
               onclick="showHide('add-resource-div');resetThemeResourceForms()"><fmt:message
                    key="add.image"/></a>
        </div>

</fmt:bundle>
        <div style="display:block;height:30px;">
            <a class="add-collection-icon-link"
               onclick="javascript: showHide('add-folder-div');expandIfNot('entries');if($('add-folder-div').style.display!='none')$('collectionName').focus();">
                <fmt:message key="add.collection"/></a>
        </div>
        <% }
        }%>

        <!-- all the content goes here -->
        <% if (cui) { %>
        <a onclick="viewStandardThemeContentSection('<%=cb.getPathWithVersion()%>')">Standard
            view</a><br/>

        <div id="customViewUIDiv">
            <jsp:include page="<%=cuiURL%>"/>
        </div>
        <%
        } else {

            if (cuiURL != null) {
        %>
        <a onclick="refreshThemeContentSection('<%=cb.getPathWithVersion()%>')"><fmt:message
                key="custom.view"/></a><br/>
        <%
            }
            boolean isImage = false;
            String contentPathLower = (cb.getContentPath() == null)? "" : cb.getContentPath().toLowerCase();
            String[] expectedImageExts = {".jpg", ".jpeg", ".png", ".gif"};

            for (String ext: expectedImageExts) {
               if (contentPathLower.endsWith(ext)) {
                   isImage = true;
                   break;
               }
            }
            if (cb.getCollection()) {
        %>

        <jsp:include page="raw-collection-content.jsp"/>

        <% } else if(isImage) {
        %>

        <table style="*width:340px !important;">
        <tr>
        <!--<td>
        <a onclick="displayThemeContentAsText('<%=cb.getPathWithVersion()%>')" class="icon-link" style="background-image:url(../admin/images/view.gif);"><fmt:message
                key="display.as.text"/></a>
        </td>
        <% if (cb.getPutAllowed() && !cb.getVersionView()) { %>
        <td style="vertical-align:middle;padding-left:5px;">|</td>
        <td>
        <a onclick="displayEditThemeContentAsText('<%=cb.getPathWithVersion()%>')" class="icon-link" style="background-image:url(../admin/images/edit.gif);"><fmt:message
                key="edit.as.text"/></a>
        </td>
        <% } %>
        <td style="vertical-align:middle;padding-left:5px;">|</td>  -->
        <td>
            <%
                String path;
            if (cb.getRealPath() != null) {
                path = cb.getRealPath();
            } else {
                path = cb.getPathWithVersion();
            }
            if (path.startsWith("http")) {
                %>
                <a class="icon-link" style="background-image:url(../resources/images/icon-download.jpg);"
		           href="<%=path%>"
		           target="_blank"><fmt:message key="download"/></a>
       <%
            } else {
        %>
	    <a class="icon-link" style="background-image:url(../resources/images/icon-download.jpg);"
		           href="<%=ThemeUtil.getThemeResourceDownloadURL(path)%>"
		           target="_blank"><fmt:message key="download"/></a>
	    <% }
            %>
        </td>
        </tr>
        </table>
        <br/>
        <br/>

        <div id="generalContentDiv" style="display:block;">
            <%
                String imageViewLink = path;
                if (!path.startsWith("http")) {
                    imageViewLink = ThemeUtil.getThemeResourceViewAsImageURL(path);     
                }
            %>
            <img src="<%=imageViewLink%>" alt="The Image for <%=path%>"/> 
        </div>

        <% } else {
        %>

        <table style="*width:340px !important;">
        <tr>
        <td>
        <a onclick="displayThemeContentAsText('<%=cb.getPathWithVersion()%>')" class="icon-link" style="background-image:url(../admin/images/view.gif);"><fmt:message
                key="display.as.text"/></a>
        </td>
        <% if (cb.getPutAllowed() && !cb.getVersionView()) { %>
        <td style="vertical-align:middle;padding-left:5px;">|</td>
        <td>
        <a onclick="displayEditThemeContentAsText('<%=cb.getPathWithVersion()%>')" class="icon-link" style="background-image:url(../admin/images/edit.gif);"><fmt:message
                key="edit.as.text"/></a>
        </td>
        <% } %>
        <td style="vertical-align:middle;padding-left:5px;">|</td>
        <td>
            <%
                String path;
            if (cb.getRealPath() != null) {
                path = cb.getRealPath();
            } else {
                path = cb.getPathWithVersion();
            }
            if (path.startsWith("http")) {
                %>
                <a class="icon-link" style="background-image:url(../resources/images/icon-download.jpg);"
		           href="<%=path%>"
		           target="_blank"><fmt:message key="download"/></a>
       <%
            } else {
        %>
	    <a class="icon-link" style="background-image:url(../resources/images/icon-download.jpg);"
		           href="<%=ThemeUtil.getThemeResourceDownloadURL(path)%>"
		           target="_blank"><fmt:message key="download"/></a>
	    <% }
            %>
        </td>
        </tr>
        </table>
        <br/>
        <br/>

        <div id="generalContentDiv" style="display:none;">
        </div>

        <% }
        } %>

    </div>
 <%}%>
</fmt:bundle>
