<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
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
<%@ page import="org.wso2.carbon.ui.menu.stratos.Util" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
<head></head>
<body>
<%--Redirect the browser to the Manager Homepage --%>
<form action="<%=Util.getManagerHomepageURL()%>" method="GET" id="manager_redirector"></form>
<script type="text/javascript">
    document.forms[0].submit();
</script>
</body>
</html>