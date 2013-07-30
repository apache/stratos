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
<%@ page import="org.apache.axis2.util.JavaUtils" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.registry.common.ui.UIException" %>
<%@ page import="org.apache.stratos.theme.mgt.ui.clients.ThemeMgtServiceClient" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String errorMessage = null;
    boolean displayPlainError = false;
    try {
        displayPlainError = (request.getParameter("printerror") != null);

        String parentPath = request.getParameter("parentPath");
        String resourceName = request.getParameter("resourceName");
        String mediaType = request.getParameter("mediaType");
        String description = request.getParameter("description");
        String fetchURL = request.getParameter("fetchURL");
        String isAsync = request.getParameter("isAsync");
        String symlinkLocation = request.getParameter("symlinkLocation");
        String redirectWith = request.getParameter("redirectWith");

        String cookie = (String) request.
                getSession().getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String serverURL = (String) request.getAttribute(CarbonConstants.SERVER_URL);

        try {
            /*
            // currently chroot will not work with multitenancy
            ConfigurationContext configContext = (ConfigurationContext) config.
                    getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
            IServerAdmin adminClient =
                    (IServerAdmin) CarbonUIUtil.
                            getServerProxy(new ServerAdminClient(configContext,
                                    serverURL, cookie, session), IServerAdmin.class, session);
            ServerData data = adminClient.getServerData();
            String chroot = "";
            if (data.getRegistryType().equals("remote") && data.getRemoteRegistryChroot() != null &&
                    !data.getRemoteRegistryChroot().equals(RegistryConstants.PATH_SEPARATOR)) {
                chroot = data.getRemoteRegistryChroot();
                if (!chroot.startsWith(RegistryConstants.PATH_SEPARATOR)) {
                    chroot = RegistryConstants.PATH_SEPARATOR + chroot;
                }
                if (chroot.endsWith(RegistryConstants.PATH_SEPARATOR)) {
                    chroot = chroot.substring(0, chroot.length() - RegistryConstants.PATH_SEPARATOR.length());
                }
            }
            if (symlinkLocation != null) {
                symlinkLocation = chroot + symlinkLocation;
            }
            */
            ThemeMgtServiceClient client =
                    new ThemeMgtServiceClient(cookie, config, request.getSession());
            if (JavaUtils.isTrueExplicitly(isAsync)) {
                client.importResource(parentPath, resourceName, mediaType, description, fetchURL,
                        symlinkLocation, true, redirectWith);
            } else {
                client.importResource(parentPath, resourceName, mediaType, description, fetchURL,
                        symlinkLocation, false, redirectWith);
            }
        } catch (Exception e) {
            String msg = "Failed to import resource " + resourceName +
                    " to the parent collection " + parentPath + ". " + e.getMessage();
            throw new UIException(msg, e);
        }

    } catch (Exception e) {
        response.setStatus(500);
        if (displayPlainError) {
            %><%=e.getMessage()%><%
            return;
        }

        errorMessage = e.getMessage();
    }
%>

<% if (errorMessage != null) { %>

<script type="text/javascript">
    location.href='../error.jsp?errorMsg=<%=errorMessage%>'
</script>

<% } %>
