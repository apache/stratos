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
<%@ page import="org.wso2.carbon.sample.installer.stub.SampleInformation" %>
<%@ page import="org.wso2.carbon.sample.installer.ui.SampleDeployerClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.wso2.carbon.utils.multitenancy.MultitenantConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<jsp:include page="../dialog/display_messages.jsp"/>

<carbon:breadcrumb
        label="sample.installer"
        resourceBundle="org.wso2.carbon.sample.installer.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<fmt:bundle basename="org.wso2.carbon.sample.installer.ui.i18n.Resources">
<%
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext()
                    .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    SampleDeployerClient client;
    SampleInformation[] sampleInfo = null;

    try {
        String tenantDomain = (String) session.getAttribute(MultitenantConstants.TENANT_DOMAIN);
        client = new SampleDeployerClient(cookie, serverURL, configContext);
        sampleInfo = client.getSampleInformation("all", tenantDomain);
    } catch (Exception e) {
%>
<script type="text/javascript">
    CARBON.showErrorDialog('<fmt:message key="get.sample.info.error"/>');
</script>
<%
    }
%>

    <div id="middle">
        <h2><fmt:message key="sample.installer"/></h2>

        <div id="workArea">
            <div id="output" style="display:none;"></div>
                <%
                if (sampleInfo != null) {
                %>
            <ul>
                <%
                    Set<String> requiredServiceList = new HashSet<String>();
                    Set<String> sampleNameList = new HashSet<String>();
                    StringBuffer sb = new StringBuffer("");
                    for (SampleInformation sample : sampleInfo) {
                        if (!sample.getInstallable()) {
                            requiredServiceList.addAll(Arrays.asList(sample.getRequiredServices()));
                            sampleNameList.add(sample.getSampleName());
                        }
                    }
                    if (sampleNameList.size() == 0) {
                %>
                <li>
                    <a href="../sample-installer/installer.jsp?type=all"
                       class="icon-link-nofloat"
                       style="background-image:url(images/all.gif);">
                        <fmt:message key="install.all.samples"/>
                    </a>
                </li>
                <%
                    } else {
                        for (String service : requiredServiceList) {
                            sb.append(service).append(", ");
                        }
                        String requiredServices = (sb.length() == 0) ? sb.toString() :
                                sb.delete(sb.length() - 2, sb.length()).toString();
                        sb = new StringBuffer("");
                        for (String sample : sampleNameList) {
                            sb.append(sample).append(", ");
                        }
                        String sampleList = sb.delete(sb.length() - 2, sb.length()).toString();
                %>
                <li>
                    <a class="icon-link-nofloat" href="#"
                       onclick="allSamplesNotInstallable('<%=sampleList%>','<%=requiredServices%>')"
                       style="background-image:url(images/all.gif);">
                        <fmt:message key="install.all.samples"/>
                    </a>
                </li>
                <%
                    }
                    for (SampleInformation sample : sampleInfo) {
                        String sampleFileName = sample.getFileName();
                        String sampleName = sample.getSampleName();
                        if (sample.getInstallable()) {
                %>
                <li>
                    <a href="../sample-installer/installer.jsp?type=<%=sampleFileName%>"
                       class="icon-link-nofloat" style="background-image:url(images/services.gif);">
                        <%=sampleName%>
                    </a>
                </li>
                <%
                        } else {
                            sb = new StringBuffer("");
                            for (String service : sample.getRequiredServices()) {
                                sb.append(service).append(", ");
                            }
                            String requiredServices = (sb.length() == 0) ? sb.toString() :
                                sb.delete(sb.length() - 2, sb.length()).toString();
                %>
                <li>
                    <a href="#"
                       onclick="sampleNotInstallable('<%=sampleName%>', '<%=requiredServices%>')"
                       class="icon-link-nofloat" style="background-image:url(images/services.gif);">
                        <%=sampleName%>
                    </a>
                </li>
                <%
                        }
                    }
                %>
            </ul>
                <%
                }
                %>
        </div>
    </div>

<script type="text/javascript">
    function allSamplesNotInstallable(sampleList, requiredServices) {
        CARBON.showConfirmationDialog("<fmt:message key="unable.to.install.samples"/>: " + sampleList + ". <fmt:message key="you.need.to.activate.the.following.required.services"/>: " + requiredServices + ". <fmt:message key="do.you.want.to.proceed"/>?",
                function() {
                    window.location.href = "../sample-installer/installer.jsp?type=some";
                });
    }

    function sampleNotInstallable(sampleName, requiredServices) {
        CARBON.showWarningDialog("<fmt:message key="the.sample"/> " + sampleName + " <fmt:message key="cannot.be.installed"/>. <fmt:message key="please.activate.the.following.services"/>: " + requiredServices);
    }
</script>
</fmt:bundle>