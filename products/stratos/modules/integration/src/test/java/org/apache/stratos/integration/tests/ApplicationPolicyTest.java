/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.integration.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle Network partition CRUD operations
 */
public class ApplicationPolicyTest {
    private static final Log log = LogFactory.getLog(ApplicationPolicyTest.class);
    private static final String applicationPolicies = "/application-policies/";
    private static final String applicationPoliciesUpdate = "/application-policies/update/";
    private static final String entityName = "applicationPolicy";


    public boolean addApplicationPolicy(String applicationPolicyId, RestClient restClient) {
        return restClient.addEntity(applicationPolicies + "/" + applicationPolicyId,
                RestConstants.APPLICATION_POLICIES, entityName);
    }

    public ApplicationPolicyBean getApplicationPolicy(String applicationPolicyId, RestClient restClient) {

        ApplicationPolicyBean bean = (ApplicationPolicyBean) restClient.
                getEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId,
                        ApplicationPolicyBean.class, entityName);
        return bean;
    }

    public boolean updateApplicationPolicy(String applicationPolicyId, RestClient restClient) {
        return restClient.updateEntity(applicationPoliciesUpdate + "/" + applicationPolicyId,
                RestConstants.APPLICATION_POLICIES, entityName);

    }

    public boolean removeApplicationPolicy(String applicationPolicyId, RestClient restClient) {
        return restClient.removeEntity(RestConstants.APPLICATION_POLICIES, applicationPolicyId, entityName);
    }
}
