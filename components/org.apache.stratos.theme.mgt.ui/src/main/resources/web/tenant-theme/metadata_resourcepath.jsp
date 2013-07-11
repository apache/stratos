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
<%@ page import="org.wso2.carbon.registry.core.RegistryConstants" %>
<%@ page import="org.apache.stratos.theme.mgt.stub.registry.resource.stub.beans.xsd.MetadataBean" %>
<%@ page import="org.apache.stratos.theme.mgt.stub.registry.resource.stub.common.xsd.WebResourcePath" %>
<%@ page import="org.wso2.carbon.registry.resource.ui.Utils" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    String viewMode = Utils.getResourceViewMode(request);
    String synapseRegistryMetaDataRootPath =
            RegistryConstants.ROOT_PATH + "carbon" + RegistryConstants.PATH_SEPARATOR + "synapse-registries";
    String resourceConsumer = Utils.getResourceConsumer(request);
    String targetDivID = Utils.getTargetDivID(request);
    MetadataBean metadata;
    try {
        if(request.getParameter("item") != null){
            ThemeMgtServiceClient client = new ThemeMgtServiceClient(cookie, config, session);
            metadata = client.getMetadata(request,request.getParameter("item"));
//            metadata = client.getMetadata(request);

        }
        else{

            ThemeMgtServiceClient client = new ThemeMgtServiceClient(cookie, config, session);
            metadata = client.getMetadata(request);
        }
    } catch (Exception e) {
        request.setAttribute(CarbonUIMessage.ID,new CarbonUIMessage(null,null,e));
%>
<jsp:forward page="../admin/error.jsp?<%=e.getMessage()%>"/>
<%
        return;
    }
%>

<table cellspacing="0" cellpadding="0" border="0" style="width:100%">
    <tr class="top-toolbar-back">
        <td valign="middle" style="width:35px;">

            <!-- Page resource path prints here -->

            <%
                WebResourcePath[] iNavPaths = metadata.getNavigatablePaths();
                String path = "";
                if (iNavPaths.length > 0) {
                    WebResourcePath rootPath = iNavPaths[0];
                    path = RegistryConstants.ROOT_PATH;

            %>
            <a href="#" style="font-size:10px;font-weight:bold;position:absolute;margin-top:-10px;margin-left:5px;"
               onclick="loadThemeResourcePage('<%=rootPath.getNavigatePath()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"
               title="Go to root resource">Root<br/><img
                    src="../resources/images/to-root.gif" border="0" align="top"/></a>
        </td>
        <td valign="middle" style="padding-left:0px;"><a class="registry-breadcrumb"
                                                         href="#"
                                                         onclick="loadThemeResourcePage('<%=rootPath.getNavigatePath()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"
                                                         title="root">/</a><%


            if (iNavPaths.length > 1) {
                WebResourcePath childPath = iNavPaths[1];
                path = childPath.getNavigatePath();

        %><a class="registry-breadcrumb" href="#"
             onclick="loadThemeResourcePage('<%=childPath.getNavigatePath()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"><%=childPath.getNavigateName()%></a><%


            }

            if (iNavPaths.length > 2) {
                for (int i = 2; i < iNavPaths.length; i++) {
                    WebResourcePath resourcePath = iNavPaths[i];
                    path = resourcePath.getNavigatePath();

        %>/<a class="registry-breadcrumb" href="#"
              onclick="loadThemeResourcePage('<%=resourcePath.getNavigatePath()%>','<%=viewMode%>','<%=resourceConsumer%>','<%=targetDivID%>')"><div style="display:inline" id=<%="pathResult"+i %>><%=resourcePath.getNavigateName()%></div></a><%
                }
            }
                }
            %>

            <span style="clear:both;"/>
        </td>

    </tr>
</table>
