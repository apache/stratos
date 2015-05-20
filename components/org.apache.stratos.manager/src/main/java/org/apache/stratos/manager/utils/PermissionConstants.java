/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License", Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing",
 * software distributed under the License is distributed on an
 * "AS IS" BASIS", WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND", either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.utils;

public class PermissionConstants {

    public static final String[] INTERNAL_USER_ROLE_PERMISSIONS = new String[] {
            "/permission/protected/restlogin",
            "/permission/admin/manage/addCartridge",
            "/permission/admin/manage/updateCartridge",
            "/permission/admin/manage/getCartridges",
            "/permission/admin/manage/getCartridgesByFilter",
            "/permission/admin/manage/removeCartridge",
            "/permission/admin/manage/addCartridgeGroup",
            "/permission/admin/manage/getCartridgeGroups",
            "/permission/admin/manage/removeServiceGroup",
            "/permission/admin/manage/addDeploymentPolicy",
            "/permission/admin/manage/getDeploymentPolicies",
            "/permission/admin/manage/updateDeploymentPolicy",
            "/permission/admin/manage/removeDeploymentPolicy",
            "/permission/admin/manage/addNetworkPartition",
            "/permission/admin/manage/updateNetworkPartition",
            "/permission/admin/manage/getNetworkPartitions",
            "/permission/admin/manage/removeNetworkPartition",
            "/permission/admin/manage/getAutoscalingPolicies",
            "/permission/admin/manage/addAutoscalingPolicy",
            "/permission/admin/manage/updateAutoscalingPolicy",
            "/permission/admin/manage/removeAutoscalingPolicy",
            "/permission/admin/manage/addKubernetesHostCluster",
            "/permission/admin/manage/addKubernetesHost",
            "/permission/admin/manage/updateKubernetesMaster",
            "/permission/admin/manage/updateKubernetesHost",
            "/permission/admin/manage/getKubernetesHostClusters",
            "/permission/admin/manage/removeKubernetesHostCluster",
            "/permission/admin/manage/getApplicationPolicy",
            "/permission/admin/manage/addApplication",
            "/permission/admin/manage/getApplications",
            "/permission/admin/manage/deployApplication",
            "/permission/admin/manage/getApplicationDeploymentPolicy",
            "/permission/admin/manage/addApplicationSignUp",
            "/permission/admin/manage/getApplicationSignUp",
            "/permission/admin/manage/removeApplicationSignUp",
            "/permission/admin/manage/addDomainMappings",
            "/permission/admin/manage/removeDomainMappings",
            "/permission/admin/manage/getDomainMappings",
            "/permission/admin/manage/undeployApplication",
            "/permission/admin/manage/getApplicationRuntime",
            "/permission/admin/manage/removeApplication",
            "/permission/admin/manage/addTenant",
            "/permission/admin/manage/updateTenant",
            "/permission/admin/manage/getTenantForDomain",
            "/permission/admin/manage/removeTenant",
            "/permission/admin/manage/getTenants",
            "/permission/admin/manage/activateTenant",
            "/permission/admin/manage/deactivateTenant",
            "/permission/admin/manage/notifyRepository"
    };
}
