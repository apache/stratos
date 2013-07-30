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
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    java.lang.String[] imagePaths = {"test1", "test2"};

    String cookie = (String) request.
            getSession().getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    try {
        ThemeMgtServiceClient client =
                new ThemeMgtServiceClient(cookie, config, request.getSession());
        imagePaths = client.getAllPaths();


    } catch (Exception e) {
        String type = "resource";
        response.setStatus(500);
        out.write("Fail to get image paths");
        return;
    }
%>
<script type="text/javascript">
    var imagePaths = new Array();
    <%
    for(int i=0;i<imagePaths.length;i++){
    %>
    imagePaths.push("<%=imagePaths[i]%>");
    <%
    } %>
</script>

<div class="box1-head">
</div>
<div class="box1-mid">
    <div id="cssEditorDiv">
        <script type="text/javascript">
            cssEditorDisplay("/main.css", "cssEditorDiv",imagePaths);
        </script>
    </div>
</div>
