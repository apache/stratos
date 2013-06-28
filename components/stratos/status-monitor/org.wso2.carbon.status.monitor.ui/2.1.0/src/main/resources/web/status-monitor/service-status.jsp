<!--
~ Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.wso2.carbon.status.monitor.stub.beans.xsd.ServiceStateInfoBean" %>
<%@ page import="org.wso2.carbon.status.monitor.ui.clients.HealthMonitorServiceClient" %>
<%@ page import="org.wso2.carbon.status.monitor.ui.utils.StatusMonitorUtil" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Date" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../registry_common/registry_common-i18n-ajaxprocessor.jsp"/>
<script type="text/javascript" src="../registry_common/js/registry_validation.js"></script>
<script type="text/javascript" src="../registry_common/js/registry_common.js"></script>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="js/status_config.js"></script>


<carbon:jsi18n
        resourceBundle="org.wso2.carbon.status.monitor.ui.i18n.JSResources"
        request="<%=request%>"/>

<fmt:bundle basename="org.wso2.carbon.status.monitor.ui.i18n.Resources">
    <carbon:breadcrumb
            label="monitor.service_status.menu"
            resourceBundle="org.wso2.carbon.status.monitor.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>


    <div id="middle">

        <%
            String backendServerURL = CarbonUIUtil.getServerURL(
                    config.getServletContext(), session);
            ConfigurationContext configContext = (ConfigurationContext) config
                    .getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

            String cookie = (String) session.getAttribute
                    (ServerConstants.ADMIN_SERVICE_COOKIE);
            HealthMonitorServiceClient client;
            ServiceStateInfoBean[] serviceStateInfoBeanArr;
            try {
                client = new HealthMonitorServiceClient(cookie, backendServerURL, configContext);

                serviceStateInfoBeanArr = client.retrieveStatuses();

            } catch (UIException e) {
                String error1 = "Error in retrieving service status";
                request.setAttribute(CarbonUIMessage.ID, new CarbonUIMessage(error1, error1, null));
        %>

        <jsp:forward page="../admin/error.jsp"/>

        <%
                return;
            }
        %>

        <br/>

        <h2><fmt:message key="services.list"/></h2>

        <div id="workArea">
            <table cellpadding="0" cellspacing="0" border="0" style="width:100%" class="styledLeft">
                <thead>
                <tr>
                    <th style="padding-left:5px;text-align:left;"><fmt:message
                            key="service.name"/></th>
                    <th style="padding-left:5px;text-align:left;"><fmt:message
                            key="service.state"/></th>
                    <th style="padding-left:5px;text-align:left;"><fmt:message key="logged.time"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

                    if (serviceStateInfoBeanArr != null) {
                        for (ServiceStateInfoBean serviceStateInfoBean : serviceStateInfoBeanArr) {
                            if (serviceStateInfoBean == null) {
                                continue;
                            }

                            String service = serviceStateInfoBean.getService();
                            String state = serviceStateInfoBean.getServiceState();
                            Date loggedDate = new Date(serviceStateInfoBean.getDate());
                            String loggedDateStr = dateFormat.format(loggedDate);
                %>
                <tr id="1">
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=service%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=state%>
                    </td>
                    <td style="padding-left:5px;padding-top:3px;text-align:left;"><%=loggedDateStr%>
                    </td>
                </tr>
                <% }
                }
                %>
                </tbody>
            </table>
        </div>
    </div>
</fmt:bundle>