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

/**
 * Constant class to keep track of rest endpoint context
 */
public class RestConstants {
    public static final String API = "api";
    public static final String AUTOSCALING_POLICIES = "/" + API + "/autoscalingPolicies";
    public static final String DEPLOYMENT_POLICIES = "/" + API + "/deploymentPolicies";
    public static final String NETWORK_PARTITIONS = "/" + API + "/networkPartitions";
    public static final String CARTRIDGES = "/" + API + "/cartridges";
    public static final String CARTRIDGE_GROUPS = "/" + API + "/cartridgeGroups";
    public static final String APPLICATION_POLICIES = "/" + API + "/applicationPolicies";
    public static final String APPLICATIONS = "/" + API + "/applications";
    public static final String APPLICATIONS_RUNTIME = "/runtime";
    public static final String APPLICATIONS_DEPLOY = "/deploy";
    public static final String APPLICATIONS_UNDEPLOY = "/undeploy";

    public static final String AUTOSCALING_POLICIES_PATH = "/autoscaling-policies/";
    public static final String AUTOSCALING_POLICIES_NAME = "autoscalingPolicy";
    public static final String CARTRIDGE_GROUPS_PATH = "/cartridges-groups/";
    public static final String CARTRIDGE_GROUPS_NAME = "cartridgeGroup";
    public static final String CARTRIDGES_PATH = "/cartridges/mock/";
    public static final String CARTRIDGES_NAME = "cartridge";
    public static final String NETWORK_PARTITIONS_PATH = "/network-partitions/mock/";
    public static final String NETWORK_PARTITIONS_NAME = "networkPartition";
    public static final String DEPLOYMENT_POLICIES_PATH = "/deployment-policies/";
    public static final String DEPLOYMENT_POLICIES_NAME = "deploymentPolicy";
    public static final String APPLICATIONS_PATH = "/applications/";
    public static final String APPLICATIONS_NAME = "application";
    public static final String APPLICATION_POLICIES_PATH = "/application-policies/";
    public static final String APPLICATION_POLICIES_NAME = "applicationPolicy";

}
