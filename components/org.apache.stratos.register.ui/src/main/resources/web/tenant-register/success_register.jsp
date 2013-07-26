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
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<link href="css/tenant-register.css" rel="stylesheet" type="text/css" media="all"/>

<%
    // String contextPath = "/" + MultitenantConstants.TENANT_AWARE_URL_PREFIX + "/" + request.getParameter("domain");
%>
<div id="middle">

    <h2>
        Account Created Successfully
    </h2>

    <div id="workArea">

       <h3>Congratulations! You have successfully created an account for your organization with WSO2 Stratos. </h3>

        <p style="font-size:16px" align="left">
        A confirmation email has been sent to your email address for this account. Please follow the instructions in the email to activate the account.
        After that login from the <a href="/carbon/admin/login.jsp"> <b>login page</b></a>.
        </p>
    </div>


    <div class="clear"></div>
    <div class="features">
        <div class="feature">
            <img src="images/feature-01-icon.gif"/>

            <h2>Elasticity</h2>

            <p>
                Stratos manages your underlying cloud infrastructure to seamlessly handle the scalability demands of
                your application.
            </p>
        </div>
        <div class="feature">
            <img src="images/feature-02-icon.gif"/>

            <h2>Multi-tenancy</h2>

            <p>
                Departments, developer groups, or projects run fully independently, but share the same middleware
                platform for maximum resource utilization.
            </p>
        </div>
        <div class="feature">
            <img src="images/feature-03-icon.gif"/>

            <h2>Self Provisioning</h2>

            <p>
                Authorized users can provision new tenants from a web portal in moments.
            </p>
        </div>
        <div class="clear"></div>
    </div>



