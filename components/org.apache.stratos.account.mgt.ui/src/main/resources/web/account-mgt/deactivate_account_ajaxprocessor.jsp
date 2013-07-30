<%--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 --%>
<%@ page import="org.wso2.carbon.registry.core.exceptions.RegistryException" %>
<%@ page import="org.apache.stratos.account.mgt.ui.clients.AccountMgtClient" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
AccountMgtClient client;
try {
    client = new AccountMgtClient(config, session);
    client.deactivate();

    // redirect to the carbon login page
    %><script type="text/javascript">
        CARBON.showInfoDialog("Your account has been deactivated.",
                function() {window.location.href="../admin/logout_action.jsp"},
                function() {window.location.href="../admin/logout_action.jsp"});
    </script><%
} catch (RegistryException e) {
%>
  <div>Error in deactivating the account.</div>
<%
}
%>
