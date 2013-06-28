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
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.CarbonException" %>
<%@ page import="org.wso2.carbon.sample.installer.ui.SampleDeployerClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    String status = "false";

    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    String type = request.getParameter("type");
    String successMessage;
    String failureMessage;
    if ("some".equals(type)) {
        successMessage = "install.success.some";
        failureMessage = "install.fail.all";
        type = "all";
    } else if ("all".equals(type)) {
        successMessage = "install.success.all";
        failureMessage = "install.fail.all";
    } else {
        successMessage = "install.success";
        failureMessage = "install.fail";
    }
    String waitMessage = "wait.one.hour";
    ResourceBundle resourceBundle =
            ResourceBundle.getBundle("org.wso2.carbon.sample.installer.ui.i18n.Resources",
                    request.getLocale());

    try {
        SampleDeployerClient deployerClient = new SampleDeployerClient(cookie, serverURL, configContext);
        //deployerClient.setPolicyPermission();
        //success = deployerClient.sampleDeployer(type, session);
        status = deployerClient.addToQueue(type, session);
    } catch (CarbonException ignored) {
        // We account for this error since success will be false.
    }
    if (status == "success") {
        CarbonUIMessage.sendCarbonUIMessage(resourceBundle.getString(successMessage),
                CarbonUIMessage.INFO, request);
    } else if (status == "failed") {
        CarbonUIMessage.sendCarbonUIMessage(resourceBundle.getString(failureMessage),
                CarbonUIMessage.ERROR, request);
    } else if ((status == "waiting")) {
        CarbonUIMessage.sendCarbonUIMessage(resourceBundle.getString(waitMessage),
                CarbonUIMessage.WARNING, request);
    }
%>
        <script type="text/javascript">
            location.href = '../sample-installer/index.jsp?region=region5&item=install_samples';
        </script>