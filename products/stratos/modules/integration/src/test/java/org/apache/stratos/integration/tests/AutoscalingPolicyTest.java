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
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.integration.tests.rest.RestClient;

/**
 * Test to handle autoscaling policy CRUD operations
 */
public class AutoscalingPolicyTest {
    private static final Log log = LogFactory.getLog(AutoscalingPolicyTest.class);
    private static final String autoscalingPolicy = "/autoscaling-policies/";
    private static final String autoscalingPolicyUpdate = "/autoscaling-policies/update/";
    private static final String entityName = "autoscalingPolicy";

    public boolean addAutoscalingPolicy(String autoscalingPolicyName, RestClient restClient) {
        return restClient.addEntity(autoscalingPolicy + "/" + autoscalingPolicyName,
                RestConstants.AUTOSCALING_POLICIES, entityName);

    }

    public AutoscalePolicyBean getAutoscalingPolicy(String autoscalingPolicyName, RestClient restClient) {
        AutoscalePolicyBean bean = (AutoscalePolicyBean) restClient.
                getEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyName,
                        AutoscalePolicyBean.class, entityName);
        return bean;
    }

    public boolean updateAutoscalingPolicy(String autoscalingPolicyName, RestClient restClient) {
        return restClient.updateEntity(autoscalingPolicyUpdate + "/" + autoscalingPolicyName,
                RestConstants.AUTOSCALING_POLICIES, entityName);

    }

    public boolean removeAutoscalingPolicy(String autoscalingPolicyName, RestClient restClient) {
        return restClient.removeEntity(RestConstants.AUTOSCALING_POLICIES, autoscalingPolicyName, entityName);

    }
}
