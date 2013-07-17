<!-- 
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
  -->
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.apache.stratos.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page import="org.apache.stratos.account.mgt.ui.utils.Util" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<carbon:jsi18n
		resourceBundle="org.wso2.carbon.register.ui.i18n.JSResources"
		request="<%=request%>" />
<link href="css/update_profile.css" rel="stylesheet" type="text/css" media="all"/>


<%
    Boolean isUpdated;

    try {
    isUpdated = Util.updateFullname(request, config, session);
    }
    catch (Exception e) {
    isUpdated  = false;
    }

%>
<%        if (isUpdated) { %>

    <div id="middle">

       <h2>
           Profile Successfully Updated.
       </h2>


   <%   }  else { %>

    <div id="middle">

       <h2>
           Profile Update Failed
       </h2>

        <% } %>
        <p>
            Go to  <a href="../admin/index.jsp"> home page. </a>
        </p>
      </div>
