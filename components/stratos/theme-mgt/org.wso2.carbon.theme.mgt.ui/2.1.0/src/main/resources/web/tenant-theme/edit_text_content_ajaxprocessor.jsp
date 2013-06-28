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

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<fmt:bundle basename="org.wso2.carbon.registry.resource.ui.i18n.Resources">

    <%
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        String path = request.getParameter("path");
        String textContent;
        try {
            ThemeMgtServiceClient client = new ThemeMgtServiceClient(cookie, config, session);
            textContent = client.getTextContent(request);
        } catch (Exception e) {
            response.setStatus(500);
    %><%=e.getMessage()%><%
        return;
    }
    Boolean displayCssEditor = false;
    if(path.endsWith(".css")){
           displayCssEditor = true;
    }
%>

     <% if (displayCssEditor){ %>
    <div class="csseditor-box">
        <div class="csseditor-top-line"></div>
        <table cellpadding="0" cellspacing="0" style="width:100%">
            <tr>
                <td>
                    <div class="csseditor-leftbox">
                        <div class="csseditor-leftbox-top">
                            Css Editor
                        </div>
                        <textarea id="editTextContentID" name="content" style="width:100%;*width:98%;height:300px;"><%=textContent%></textarea>
                    </div>
                </td>
                <td>
                    <div class="csseditor-rightbox">
                        <div class="csseditor-rightbox-title">Image Search</div>
                        <div class="csseditor-searchbox"> Text: <input type="text" id="flickr_search"
                                                                       onkeydown="filterImagePathPicker()"
                                                                       style="margin-left:10px;">
                            <p style="margin-top:10px;">Enter * to see all the available images.</p>
                        </div>



                        <div id="flickr_results" style="margin-top:10px;">
                        </div>
                        <div style="padding:5px;">

                            Selected Image
                            <div id="flickr_selected" class="flicker_selected"></div>
                            <table>
                                <tr>
                                    <td><input class="button" type="button" value="Insert image path to css file" style="margin-right:5px;" onclick="insertImagePath()" /></td>
                                    <td><input class="button" type="button" value="Preview Image" onclick="previewImagePath()" /></td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </td>
            </tr>
        </table>
    </div>
    <% }else { %>
    <textarea id='editTextContentID' rows='15' cols='70'><%=textContent%></textarea>
    <% }  %>
    <br/>
    <input type='button' class='button' id="saveContentButtonID" onclick='updateThemeTextContent("<%=path%>",<%=displayCssEditor%>)'
           value='<fmt:message key="save.content"/>'/>
    <input type='button' class='button' id="cancelContentButtonID" onclick='cancelTextContentEdit(<%=displayCssEditor%>)'
           value='<fmt:message key="cancel"/>'/>
    <br/>

</fmt:bundle>
