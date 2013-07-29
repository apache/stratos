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
<%@ page import="org.wso2.carbon.registry.core.RegistryConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.apache.stratos.register.ui.utils.TenantConfigUtil" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>

<%
try {
    boolean domainAvailable = TenantConfigUtil.checkDomainAvailability(request, config, session);

    if (domainAvailable) {
    %>
    ----DomainAvailable----
    <%
    }
    else {
    %>
    ----DomainUnavailable----
    <%
    }
} catch (UIException e) {

%>
    Error in checking the domain availability.
    please retry the registration from the <a href="../tenant-register/select_domain.jsp">start</a>.
<%
}

%>
