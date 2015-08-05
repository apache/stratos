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
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle Deployment policy CRUD operations
 */
public class DeploymentPolicyTest {
    private static final Log log = LogFactory.getLog(DeploymentPolicyTest.class);
    private static final String deploymentPolicies = "/deployment-policies/";
    private static final String deploymentPoliciesUpdate = "/deployment-policies/update/";
    private static final String entityName = "deploymentPolicy";

    public boolean addDeploymentPolicy(String deploymentPolicyId, RestClient restClient) {
        return restClient.addEntity(deploymentPolicies + "/" + deploymentPolicyId,
                RestConstants.DEPLOYMENT_POLICIES, entityName);
    }

    public DeploymentPolicyBean getDeploymentPolicy(String deploymentPolicyId,
                                                    RestClient restClient) {
        return (DeploymentPolicyBean) restClient.
                getEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId,
                        DeploymentPolicyBean.class, entityName);
    }

    public boolean updateDeploymentPolicy(String deploymentPolicyId, RestClient restClient) {
        return restClient.updateEntity(deploymentPoliciesUpdate + "/" + deploymentPolicyId,
                RestConstants.DEPLOYMENT_POLICIES, entityName);
    }

    public boolean removeDeploymentPolicy(String deploymentPolicyId, RestClient restClient) {
        return restClient.removeEntity(RestConstants.DEPLOYMENT_POLICIES, deploymentPolicyId, entityName);
    }
}
