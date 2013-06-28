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

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<%@ page import="org.wso2.carbon.registry.common.ui.UIConstants" %>
<%@ page import="org.wso2.carbon.registry.common.ui.utils.UIUtil" %>
<%@ page import="org.wso2.carbon.registry.core.RegistryConstants" %>
<%@ page import="org.wso2.carbon.theme.mgt.stub.registry.resource.stub.beans.xsd.CollectionContentBean" %>
<%@ page import="org.wso2.carbon.theme.mgt.stub.registry.resource.stub.common.xsd.ResourceData" %>
<%@ page import="org.wso2.carbon.registry.resource.ui.Utils" %>
<%@ page import="org.wso2.carbon.registry.resource.ui.clients.ResourceServiceClient" %>
<%@ page import="org.wso2.carbon.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.wso2.carbon.theme.mgt.ui.utils.ThemeUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String viewMode = Utils.getResourceViewMode(request);
    String actionStyle = "inlined".equals(viewMode) ? "display:none;" : "";
    String resourceConsumer = Utils.getResourceConsumer(request);
    String targetDivID = Utils.getTargetDivID(request);
    String requestedPage = request.getParameter(UIConstants.REQUESTED_PAGE);

    CollectionContentBean ccb;
    ResourceServiceClient resourceServiceClient;
    ThemeMgtServiceClient client;
    try {
        resourceServiceClient = new ResourceServiceClient(cookie, config, session);

        client = new ThemeMgtServiceClient(cookie, config, session);
        ccb = client.getCollectionContent(request);
    } catch (Exception e) {
%>
<jsp:forward page="../admin/error.jsp?<%=e.getMessage()%>"/>
<%
        return;
    }
%>
<fmt:bundle basename="org.wso2.carbon.registry.resource.ui.i18n.Resources">

<div id="whileUpload" style="display:none;padding-top:0px;margin-top:20px;margin-bottom:20px;" class="ajax-loading-message">
	<img align="top" src="../resources/images/ajax-loader.gif"/>
	<span>Process may take some time. Please wait..</span>
</div>
<div class="add-resource-div" id="add-resource-div" style="display:none;">


<input type="hidden" name="path" value="<%=ccb.getPathWithVersion()%>"/>

<div class="validationError" id="resourceReason" style="display:none;"></div>

<table cellpadding="0" cellspacing="0" border="0" class="styledLeft noBorders">
<thead>
<fmt:bundle basename="org.wso2.carbon.theme.mgt.ui.i18n.Resources">
<tr>
    <th colspan="2"><strong><fmt:message key="add.image"/></strong></th>
</tr>
</thead>
<tbody>
<tr>
    <td colspan="2">
    	<table class="normal">
    		<tr>
		    <td style="width:117px;*width:120px;"><fmt:message key="method"/></td>
		    <td>
		        <select id="addMethodSelector" onchange="viewAddResourceUI()">
		            <option value="upload" selected="selected"><fmt:message key="upload.image.from.file"/></option>
		            <option value="import"><fmt:message key="import.image.from.url"/></option>
                </select>
		    </td>
		</tr>

    	</table>
    </td>
</tr>
</fmt:bundle>




<!-- upload file UI -->
<tr  id="uploadContentUI">
<td colspan="2">
<form onsubmit="return submitUploadThemeContentForm();" method="post" name="resourceUploadForm"
      id="resourceUploadForm"
      action="../../fileupload/themeResource" enctype="multipart/form-data" target="_self">
    <input type="hidden" id="path" name="path" value="<%=ccb.getPathWithVersion()%>"/>


    <table class="styledLeft">
    <tr>
        <td class="middle-header" colspan="2"><fmt:message key="upload.content.from.file1"/></td>
    </tr>
    <tr>
        <td valign="top" style="width:120px;">
            <span><fmt:message key="file"/> <span class="required">*</span></span></td>
        <td>
            <input id="uResourceFile" type="file" name="upload" style="background-color:#cccccc" onkeypress="return blockManual(event)"
                   onchange="fillThemeResourceUploadDetails()"/>

            <div class="helpText" id="fileHelpText">

<fmt:bundle basename="org.wso2.carbon.theme.mgt.ui.i18n.Resources">
                <fmt:message key="image.path.help.text"/>
</fmt:bundle>                
            </div>
        </td>
    </tr>
    <tr>
        <td valign="top"><fmt:message key="name"/> <span class="required">*</span></td>

        <td><input id="uResourceName" type="text" name="filename"
                   style="margin-bottom:10px;"/>
            <input id="uResourceMediaType" type="hidden" name="mediaType"
                   style="margin-bottom:10px;"/></td>  
    </tr>
    <!--<tr>
        <td valign="top"><fmt:message key="media.type"/></td>

        <td>
            <input id="uResourceMediaType" type="text" name="mediaType"
                   style="margin-bottom:10px;"/>

        </td>
    </tr>
    <tr>
        <td valign="top"><fmt:message key="description"/></td>
        <td><textarea name="description" class="normal-textarea"></textarea></td>
    </tr>  -->
    <tr>
        <td class="buttonRow" colspan="2">
            <input type="button" class="button" value="<fmt:message key="add"/>"
                   onclick="whileThemeResourceUpload();submitUploadThemeContentForm();"/>
            <input type="button" class="button" value="<fmt:message key="cancel"/>"
                   onclick="showHide('add-resource-div')"/>
        </td>
    </tr>
    </table>

</form>
</td>
</tr>

<!-- import content UI -->

<tr id="importContentUI" style="display:none;">
<td colspan="2">
<form method="post" name="resourceImportForm"
      id="resourceImportForm"
      action="/wso2registry/system/fetchResource">
    <input type="hidden" id="irParentPath" name="path" value="<%=ccb.getPathWithVersion()%>"/>


    <table class="styledLeft">
    <tr>
        <td class="middle-header" colspan="2"><fmt:message key="import.content.from.url1"/></td>
    </tr>
    <tr>
        <td valign="top" style="width:120px;">
            <span><fmt:message key="url"/> <span class="required">*</span></span></td>
        <td>
            <input id="irFetchURL" type="text" name="fetchURL"
                   onchange="fillThemeResourceImportDetails()"/>

            <div class="helpText" id="urlHelpText" style="color:#9a9a9a;">
<fmt:bundle basename="org.wso2.carbon.theme.mgt.ui.i18n.Resources">
                <fmt:message key="image.url.help.text"/>
</fmt:bundle>
            </div>
        </td>
    </tr>
    <tr>
        <td valign="top"><fmt:message key="name"/> <span class="required">*</span></td>

        <td><input id="irResourceName" type="text" name="resourceName"
                   style="margin-bottom:10px;"/>
            <input id="irMediaType" type="hidden" name="mediaType"
                   style="margin-bottom:10px;"/></td>
    </tr>
    <!--<tr>
        <td valign="top"><fmt:message key="media.type"/></td>

        <td>
            <input id="irMediaType" type="text" name="mediaType"
                   style="margin-bottom:10px;"/>

        </td>
    </tr>
    <tr>
        <td valign="top"><fmt:message key="description"/></td>
        <td><textarea id="irDescription" name="description" class="normal-textarea"></textarea></td>
    </tr>  -->
    <tr>
        <td colspan="2" class="buttonRow"><input type="button" class="button"
                                                 value="<fmt:message key="add"/>"
                                                 onclick="whileThemeResourceUpload();submitImportThemeContentForm();"/> <input
                type="button" class="button"
                value="<fmt:message key="cancel"/>"
                style="margin-top:10px;"
                onclick="showHide('add-resource-div')"/>
        </td>
    </tr>
    </table>

</form>
</td>
</tr>

<!-- text content UI -->

<tr id="textContentUI" style="display:none;">
<td colspan="2">
<form name="textContentForm" id="textContentForm" action="/wso2registry/system/addTextResource"
      method="post">
    <input type="hidden" id="trParentPath" name="path" value="<%=ccb.getPathWithVersion()%>"/>

    <table class="styledLeft">
    <tr>
        <td class="middle-header" colspan="2"><fmt:message key="text.content1"/></td>
    </tr>
    <tr>
        <td valign="top" style="width:120px;"><fmt:message key="name"/> <span
                class="required">*</span></td>
        <td><input type="text" id="trFileName" name="filename" style="margin-bottom:10px;"/>
            <input type="hidden" id="trMediaType" name="mediaType" value="text/plain"
                   style="margin-bottom:10px;"/></td>
    </tr>
    <!--<tr>
        <td valign="top"><fmt:message key="media.type"/></td>
        <td><input type="text" id="trMediaType" name="mediaType" value="text/plain"
                   style="margin-bottom:10px;"/></td>
    </tr>
    <tr>
        <td valign="top"><fmt:message key="description"/></td>
        <td>
            <textarea id="trDescription" name="description"></textarea>
        </td>
    </tr>  -->
    <tr>
        <td style="vertical-align:top !important;"><fmt:message key="content"/></td>
        <td>
            <div>
            	<input type="radio" name="richText" checked="checked" value="rich" onclick="handleRichText()" /> Rich Text Editor
            	<input type="radio" name="richText" value="plain" onclick="handleRichText()" /> Plain Text Editor
            </div>
            <textarea id="trPlainContent" style="display:none;width:99%;height:200px"></textarea>
            <div class="yui-skin-sam" id="textAreaPanel">
                <textarea id="trContent" name="trContent" style="display:none;"></textarea>
            </div>
        </td>
    </tr>
    <tr>
        <td colspan="2" class="buttonRow">
            <input type="button" class="button" value="<fmt:message key="add"/>"
                   style="margin-top:10px;"
                   onclick="whileThemeResourceUpload();submitThemeTextContentForm();"/>
            <input type="button" class="button"
                   value="<fmt:message key="cancel"/>"
                   onclick="showHide('add-resource-div')"/>
        </td>
    </tr>
    </table>

</form>
</td>
</tr>

</tbody>
</table>
</div>


<!-- Add folder div -->
<div class="add-resource-div" id="add-folder-div" style="display:none;">

    <form name="collectionForm" method="post" action="add_collection_ajaxprocessor.jsp"
          onsubmit="return submitThemeCollectionAddForm();">
        <input id="parentPath" type="hidden" name="parentPath"
               value="<%=ccb.getPathWithVersion()%>"/>

        <div class="validationError" id="collectionReason" style="display:none;"></div>
        <table width="100%" border="0" cellspacing="0" cellpadding="0" class="styledLeft noBorders">
            <tbody>
            <tr>
                <td class="middle-header" colspan="2"><strong><fmt:message
                        key="add.collection"/></strong></td>
            </tr>
            <tr>
                <td valign="top" style="width:120px;"><fmt:message key="name"/> <span
                        class="required">*</span></td>

                <td><input type="text" id="collectionName" name="collectionName"/>
                <input id="mediaType" type="hidden" value="<fmt:message key="none"/>"/>
                </td>
            </tr>
            <!--
            <tr>
                <td valign="top"><fmt:message key="media.type1"/></td>
                <td>
                    <select id="mediaType" onchange="updateOther('mediaType', '<fmt:message key="other"/>')">
                        <option value=""><fmt:message key="none"/></option>
                    <option value="<fmt:message key="other"/>"><fmt:message key="other"/></option>
                </select>&nbsp;&nbsp;
                <span id="mediaTypeOther" style="display:none"><fmt:message key="other.display"/>&nbsp;
                    <input type="text" id="mediaTypeOtherValue"/>
                </span>
                </td>
            </tr>
            <tr>
                <td valign="top"><fmt:message key="description"/></td>
                <td><textarea name="description" id="colDesc" class="normal-textarea"></textarea>
                </td>
            </tr> -->
            <tr>
                <td class="buttonRow" colspan="2">
                    <input type="button" class="button"
                           value="<fmt:message key="add"/>"
                           onclick="submitThemeCollectionAddForm()"/>
                    <input type="button" class="button"
                           value="<fmt:message key="cancel"/>"
                           onclick="showHide('add-folder-div')"/>
                </td>
            </tr>
            </tbody>
        </table>
    </form>
</div>
<div id="entryListReason" class="validationError" style="display: none;"></div>
<div id="entryList">
<%

%>
<table cellpadding="0" cellspacing="0" border="0" style="width:100%"
       class="styledLeft" id="resourceColTable">
<%
    if (ccb.getChildCount() != 0) {
%>
<thead>
<tr>
    <th><fmt:message key="name"/></th>
    <th style="width:250px;"><fmt:message key="action"/></th>
</tr>
</thead>
<tbody>
<%
    int totalCount = ccb.getChildCount();

    int start;
    int end;
    int itemsPerPage = RegistryConstants.ITEMS_PER_PAGE;

    int pageNumber;
    if (requestedPage != null) {
        pageNumber = new Integer(requestedPage).intValue();
    } else {
        pageNumber = 1;
    }

    int numberOfPages = 1;
    if (totalCount % itemsPerPage == 0) {
        numberOfPages = totalCount / itemsPerPage;
    } else {
        numberOfPages = totalCount / itemsPerPage + 1;
    }

    if (totalCount < itemsPerPage) {
        start = 0;
        end = totalCount;
    } else {
        start = (pageNumber - 1) * itemsPerPage;
        end = (pageNumber - 1) * itemsPerPage + itemsPerPage;
    }
    String[] nodes = Utils.getSortedChildNodes(ccb.getChildPaths());
    List<String> availableNodes = new LinkedList<String>();
    for (String node : nodes) {
        try {
            if (node != null && client.getResourceTreeEntry(node) != null) {
                availableNodes.add(node);
            }
        } catch (Exception ignore) {}
    }
    String[] allChildNodes = availableNodes.toArray(new String[availableNodes.size()]);
    ResourceData[] resourceDataSet;
    try {
        resourceDataSet = client.getResourceData(UIUtil.getChildren(start, itemsPerPage, allChildNodes));
    } catch(Exception e) {
        %>
<jsp:include page="../admin/error.jsp"/>
<%
        return;
    }
    int entryNumber = 0;

    //for (int i = start; i <= end; i++) {
    for (int ri = 0; ri < resourceDataSet.length; ri++) {
        ResourceData resourceData = resourceDataSet[ri];
        //    ResourceData resourceData = (ResourceData) collection.getResourceDataList().get(i);
        entryNumber++;
%>

<tr id="1">

    <td valign="top">


	        <% if (resourceData.getResourceType().equals(UIConstants.COLLECTION)) { %>
	        <a class=<% if(resourceData.getLink()){
		        	if (!resourceData.getMounted()) {
		        	%>"folder-small-icon-link-y"<%
		        	}else{
		        	%>"folder-small-icon-link-x"<%
		        	}
	        	}else {
	        	 %>"folder-small-icon-link"<%
	        	 } %>
	           onclick="loadThemeResourcePage('<%=resourceData.getResourcePath()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"
	           id="resourceView<%=entryNumber%>"
	           title="<%=resourceData.getName()%>"><%=resourceData.getName()%>
	        </a>


	        <% } else { %>
	         <a class=<% if(resourceData.getLink()){
		        	if (!resourceData.getMounted()) {
		        	%>"resource-icon-link-y"<%
		        	}else{
		        	%>"resource-icon-link-x"<%
		        	}
	        	}else {
	        	 %>"resource-icon-link trimer"<%
	        	 } %>
	           <% if(!resourceData.getExternalLink()){ %>
               onclick="loadThemeResourcePage('<%=resourceData.getResourcePath()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"
             <% }%>
	           id="resourceView<%=entryNumber%>" title="<%=resourceData.getName()%>">
	            <%=resourceData.getName()%>
	        </a>

	        <% } %>
	 </td>
	 <td>
        	 <%
       if (!ccb.getVersionView()) { %>

            <% if (resourceData.getResourceType().equals(UIConstants.COLLECTION)) { %>

	            <% if (resourceData.getPutAllowed()){ %>
		            <a class="edit-icon-link"
		               onclick="javascript:showHideCommon('rename_panel<%=entryNumber%>');hideThemeOthers(<%=entryNumber%>,'rename');if($('rename_panel<%=entryNumber%>').style.display!='none')$('resourceEdit<%=entryNumber%>').focus();">
		                <fmt:message key="rename"/></a>
            	     <% }
                     if(resourceData.getDeleteAllowed()){%>
                <a class="delete-icon-link"
	               onclick="this.disabled = true; hideThemeOthers(<%=entryNumber%>,'del');deleteThemeResource('<%=resourceData.getResourcePath()%>', '<%=ccb.getPathWithVersion()%>'); this.disabled = false; "
	                    >
	                <fmt:message key="delete"/></a>
                <%}%>


            <% } else { %>
	            <% if(resourceData.getDeleteAllowed()){ %>
                <a class="edit-icon-link"
	               onclick="javascript:showHideCommon('rename_panel<%=entryNumber%>');hideThemeOthers(<%=entryNumber%>,'rename');if($('rename_panel<%=entryNumber%>').style.display!='none')$('resourceEdit<%=entryNumber%>').focus();">
	                <fmt:message key="rename"/></a>
                <a class="delete-icon-link" style="margin-left:5px"
	               onclick="hideThemeOthers(<%=entryNumber%>,'del');deleteThemeResource('<%=resourceData.getResourcePath()%>', '<%=ccb.getPathWithVersion()%>')">
	                <fmt:message key="delete"/></a>
	            <%} %>


            <% } %>
   <% } %>

    <% if (!ccb.getVersionView() && !resourceData.getAbsent().equals("true")) { %>
	    <% if (!resourceData.getResourceType().equals(UIConstants.COLLECTION)) {
            String path;
            if (resourceData.getRealPath() != null) {
                path = resourceData.getRealPath();
            } else {
                path = resourceData.getResourcePath();
            }
            if (path.startsWith("http")) {
                %>
                <a class="download-icon-link"
		           href="<%=path%>"
		           target="_blank"><fmt:message key="download"/></a>
       <%
            } else {
        %>
	    <a class="download-icon-link"
		           href="<%=ThemeUtil.getThemeResourceDownloadURL(path)%>"
		           target="_blank"><fmt:message key="download"/></a>
	    <% } }%>
    <% } %>


    </td>
</tr>

<!--
<tr class="copy-move-panel" id="copy_panel<%=entryNumber%>" style="display:none;">
    <td colspan="4" align="left">
        <table cellpadding="0" cellspacing="0" class="styledLeft">
            <thead>
            <tr>
                <th colspan="2">Copy <%=resourceData.getResourceType()%>
                </th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td><fmt:message key="destination.path"/><span class="required">*</span></td>
                <td><input type="text" id="copy_destination_path<%=entryNumber%>"/><input
                        type="button"
                        title="<fmt:message key="resource.tree"/>"
                        onclick="showCollectionTree('copy_destination_path<%=entryNumber%>');"
                        value=".." class="button"/></td>
            </tr>
            <tr>
                <td class="buttonRow" colspan="2">
                    <input type="button" class="button" value="<fmt:message key="copy"/>"
                           onclick="this.disabled = true; copyResource('<%=ccb.getPathWithVersion()%>', '<%=resourceData.getResourcePath()%>','copy_destination_path<%=entryNumber%>','<%=resourceData.getName()%>',<%=pageNumber%>); this.disabled = false;"/>
                    <input
                            type="button" style="margin-left:5px;" class="button"
                            value="<fmt:message key="cancel"/>"
                            onclick="showHideCommon('copy_panel<%=entryNumber%>')"/></td>
            </tr>
            </tbody>
        </table>
    </td>
</tr>
<tr class="copy-move-panel" id="move_panel<%=entryNumber%>" style="display:none;">
    <td colspan="4" align="left">
        <table cellpadding="0" cellspacing="0" class="styledLeft">
            <thead>
            <tr>
                <th colspan="2"><fmt:message key="move"/> <%=resourceData.getResourceType()%>
                </th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td><fmt:message key="destination.path"/><span class="required">*</span></td>
                <td><input type="text" id="move_destination_path<%=entryNumber%>"/> <input
                        type="button"
                        class="button"
                        title="<fmt:message key="resource.tree"/>"
                        onclick="showCollectionTree('move_destination_path<%=entryNumber%>');"
                        value=".."/></td>
            </tr>
            <tr>
                <td class="buttonRow" colspan="2">
                    <input type="button" class="button" value="<fmt:message key="move"/>"
                           onclick="this.disabled = true; moveResource('<%=ccb.getPathWithVersion()%>', '<%=resourceData.getResourcePath()%>','move_destination_path<%=entryNumber%>','<%=resourceData.getName()%>',<%=pageNumber%>); this.disabled = false;"/>
                    <input
                            type="button" style="margin-left:5px;" class="button"
                            value="<fmt:message key="cancel"/>"
                            onclick="showHideCommon('move_panel<%=entryNumber%>')"/></td>
            </tr>
            </tbody>
        </table>
    </td>
</tr> -->
<tr class="copy-move-panel" id="rename_panel<%=entryNumber%>" style="display:none;">
    <td colspan="2" align="left">
        <table cellpadding="0" cellspacing="0" class="styledLeft">
            <thead>
            <tr>
                <th><fmt:message key="editing.name"/> <%=resourceData.getResourceType()%> <fmt:message key="editing.name.name"/>
                </th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td>New <% if (resourceData.getResourceType().equals(UIConstants.COLLECTION)) { %>
                    <fmt:message key="collection"/><% } else {%><fmt:message key="resource"/><% } %>
                    Name <span class="required">*</span>  <input value="<%=resourceData.getName()%>" type="text"
                                id="resourceEdit<%=entryNumber%>"/></td>
            </tr>
            <tr>
                <td class="buttonRow">
                    <input type="button" class="button" value="<fmt:message key="rename"/>"
                           onclick="this.disabled = true; renameThemeResource('<%=ccb.getPathWithVersion()%>', '<%=resourceData.getResourcePath()%>', 'resourceEdit<%=entryNumber%>',<%=pageNumber%>, <% if (resourceData.getResourceType().equals(UIConstants.COLLECTION)) { %>'collection'<% } else { %>'resource'<% } %>);this.disabled = false;"/>
                    <input
                        type="button" style="margin-left:5px;" class="button"
                        value="<fmt:message key="cancel"/>"
                        onclick="showHideCommon('rename_panel<%=entryNumber%>')"/>
                </td>
            </tr>
            </tbody>
        </table>

    </td>
</tr>
<tr class="copy-move-panel" id="del_panel<%=entryNumber%>" style="display:none;">
    <td colspan="2" align="left">
        <table cellpadding="0" cellspacing="0" class="styledLeft">
            <thead>
            <tr>
                <th><fmt:message key="confirm.delete"/> <%=resourceData.getResourceType()%>
                </th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td><fmt:message key="confirm.remove.resource.message"/>
                    <%=resourceData.getResourceType()%> '<%=resourceData.getName()%>'
                    <br/><strong><fmt:message key="warning"/>: </strong>
                    <fmt:message key="undo.warning.message"/>
                </td>
            </tr>
            <tr>
                <td class="buttonRow">
                    <input type="button" class="button" value="<fmt:message key="yes"/>"
                           onclick="deleteThemeResource('<%=resourceData.getResourcePath()%>', '<%=ccb.getPathWithVersion()%>')"/>
                    <input style="margin-left:5px;" class="button" type="button"
                           value="<fmt:message key="no"/>"
                           onclick="showHideCommon('del_panel<%=entryNumber%>')"/>
                </td>
            </tr>
            </tbody>
        </table>
    </td>
</tr>

<% }
    if (totalCount <= itemsPerPage) {
        //No paging
    } else {
%>
<tr>
    <td colspan="2" class="pagingRow" style="padding-top:10px; padding-bottom:10px;">

        <%
            if (pageNumber == 1) {
        %>
        <span class="disableLink">< Prev</span>
        <%
        } else {
        %>
        <a class="pageLinks"
           onclick="navigateThemePages(<%=(pageNumber-1)%>, '<%=ccb.getPathWithVersion()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"><
            <fmt:message key="prev"/></a>
        <%
            }
            if (numberOfPages <= 10) {
                for (int pageItem = 1; pageItem <= numberOfPages; pageItem++) { %>

        <a class=<% if(pageNumber==pageItem){ %>"pageLinks-selected"<% } else { %>
        "pageLinks" <% } %>
        onclick="navigateThemePages(<%=pageItem%>, '<%=ccb.getPathWithVersion()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')" ><%=pageItem%></a>
        <% }
        } else {
            String place = "middle";
            int pageItemFrom = pageNumber - 2;
            int pageItemTo = pageNumber + 2;

            if (numberOfPages - pageNumber <= 5) place = "end";
            if (pageNumber <= 5) place = "start";

            if (place == "start") {
                pageItemFrom = 1;
                pageItemTo = 7;
            }
            if (place == "end") {
                pageItemFrom = numberOfPages - 7;
                pageItemTo = numberOfPages;
            }

            if (place == "end" || place == "middle") {


                for (int pageItem = 1; pageItem <= 2; pageItem++) { %>

        <a class="pageLinks"
           onclick="navigateThemePages(<%=pageItem%>, '<%=ccb.getPathWithVersion()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"><%=pageItem%>
        </a>
        <% } %>
        ...
        <%
            }

            for (int pageItem = pageItemFrom; pageItem <= pageItemTo; pageItem++) { %>

        <a class=<% if(pageNumber==pageItem){ %>"pageLinks-selected"<% } else {%>"pageLinks"<% } %>
        onclick="navigateThemePages(<%=pageItem%>, '<%=ccb.getPathWithVersion()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"><%=pageItem%></a>
        <% }

            if (place == "start" || place == "middle") {
        %>
        ...
        <%
            for (int pageItem = (numberOfPages - 1); pageItem <= numberOfPages; pageItem++) { %>

        <a class="pageLinks"
           onclick="navigateThemePages(<%=pageItem%>, '<%=ccb.getPathWithVersion()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"
           style="margin-left:5px;margin-right:5px;"><%=pageItem%>
        </a>
        <% }
        }

            if (place == "middle") {

            }
            //End middle display
        }
            if (pageNumber == numberOfPages) {
        %>
        <span class="disableLink"><fmt:message key="next"/> ></span>
        <%
        } else {
        %>
        <a class="pageLinks"
           onclick="navigateThemePages(<%=(pageNumber+1)%>, '<%=ccb.getPathWithVersion()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')">Next
            ></a>
        <%
            }
        %>
	<span id="xx<%=pageNumber%>" style="display:none" />
    </td>
</tr>
<%
        }
    }

%>
<tr>
        <%--This empty td is required to solve the bottom margin problem on IE. Do not remove!!--%>
    <td align="left" colspan="2" style="height:0px;border-bottom:0px;">
    </td>
</tr>
</tbody>
</table>

</div>
</fmt:bundle>
