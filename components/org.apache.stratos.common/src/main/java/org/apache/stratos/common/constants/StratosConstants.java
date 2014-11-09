/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.common.constants;


public class StratosConstants {

    public static final String CLOUD_SERVICE_IS_ACTIVE_PROP_KEY = "active";

    public static final String CLOUD_SERVICE_INFO_STORE_PATH =
            "/repository/components/org.apache.stratos/cloud-manager/cloud-services";

    public static final String TENANT_USER_VALIDATION_STORE_PATH =
            "/repository/components/org.wso2.carbon.validations";
    public static final String ADMIN_EMAIL_VERIFICATION_FLAG_PATH =
            "/repository/components/org.wso2.carbon.email-confirmation-flag";
    public static final String TENANT_DOMAIN_VERIFICATION_FLAG_PATH =
            "/repository/components/org.wso2.carbon.domain-confirmation-flag";

    public static final String DOMAIN_VALIDATOR_INFO_PATH =
            "/repository/components/org.wso2.carbon.domain-validator-info";

    public static final String TENANT_CREATION_THEME_PAGE_TOKEN =
            "/repository/components/org.wso2.carbon.theme-page-token";

    public static final String TENANT_PACKAGE_INFO_PATH =
            "/repository/components/org.wso2.carbon.package-info";

    public static final String ALL_THEMES_PATH =
            "/repository/components/org.wso2.carbon.all-themes";

    public static final String THROTTLING_RULES_PATH =
            "/repository/components/org.wso2.carbon.throttling-rules";

    public static final String ORIGINATED_SERVICE_PATH =
            "/repository/components/org.wso2.carbon.originated-service";

    public static final String PATH_SEPARATOR = "/";

    public static final String CLOUD_SERVICE_ICONS_STORE_PATH =
            "/repository/components/org.wso2.carbon.cloud-manager/" +
                    "cloud-services-icons";

    public static final String VALIDATION_KEY_RESOURCE_NAME = "validation-key";
    public static final String INCOMING_PATH_DIR = "incoming";
    public static final String OUTGOING_PATH_DIR = "outgoing";
    public static final String MULTITENANCY_SCHEDULED_TASK_ID = "multitenancyScheduledTask";
    public static final String MULTITENANCY_VIEWING_TASK_ID = "multitenancyViewingTask";

    public static final String INVALID_TENANT = "invalidTenant";
    public static final String INACTIVE_TENANT = "inactiveTenant";
    public static final String ACTIVE_TENANT = "activeTenant";
    public static final String IS_EMAIL_VALIDATED = "isEmailValidated";
    public static final String IS_CREDENTIALS_ALREADY_RESET = "isCredentialsReset";
    public static final String TENANT_ADMIN = "tenantAdminUsername";

    public static final String CLOUD_MANAGER_SERVICE = "Apache Stratos Controller";
    public static final String CLOUD_IDENTITY_SERVICE = "WSO2 Stratos Identity";
    public static final String CLOUD_GOVERNANCE_SERVICE = "WSO2 Stratos Governance";
    public static final String CLOUD_ESB_SERVICE = "WSO2 Stratos Enterprise Service Bus";

    // keystore mgt related Constants
    public static final String TENANT_KS = "/repository/security/key-stores/";
    public static final String TENANT_PUB_KEY = "/repository/security/pub-key";
    public static final String PROP_TENANT_KS_TYPE = "key-store-type";
    public static final String PROP_TENANT_KS_PASSWD = "key-store-password";
    public static final String PROP_TENANT_KS_PRIV_KEY_PASSWD = "priv-key-password";
    public static final String PROP_TENANT_KS_ALIAS = "alias";

    // constants related to redirection

    public static final String UNVERIFIED_ACCOUNT_DOMAIN_SUFFIX = "-unverified";
    public static final String TENANT_SPECIFIC_URL_RESOLVED = "tenant-sepcific-url-resolved";
    public static final String SUFFIXED_UNVERIFIED_SESSION_FLAG = "temp-suffixed-unverified";

    // metering constants
    public static final String THROTTLING_ALL_ACTION = "all_actions";
    public static final String THROTTLING_IN_DATA_ACTION = "in_data_action"; //this covers registry capacity + registry bandwidth
    public static final String THROTTLING_OUT_DATA_ACTION = "out_data_action"; //this covers registry bandwidth
    public static final String THROTTLING_ADD_USER_ACTION = "add_user_action";
    public static final String THROTTLING_SERVICE_IN_BANDWIDTH_ACTION = "service_in_bandwith_action";
    public static final String THROTTLING_SERVICE_OUT_BANDWIDTH_ACTION = "service_out_bandwith_action";
    public static final String THROTTLING_WEBAPP_IN_BANDWIDTH_ACTION = "webapp_in_bandwith_action";
    public static final String THROTTLING_WEBAPP_OUT_BANDWIDTH_ACTION = "webapp_out_bandwith_action";
    public static final String THROTTLING_SERVICE_REQUEST_ACTION = "service_request_action";
    public static final String THROTTLING_SERVICE_RESPONSE_ACTION = "service_response_action";

    // the session attribute to keep track whether the registry action validated
    // and the usage persisted
    public static final String REGISTRY_ACTION_VALIDATED_SESSION_ATTR = "registryActionValidated";
    public static final String REGISTRY_USAGE_PERSISTED_SESSION_ATTR = "usagePersited";

    // Metering servlet attributes
    public static final String SERVICE_NAME_SERVLET_ATTR = "meteringServiceName";
    public static final String TENANT_ID_SERVLET_ATTR = "tenantId";
    public static final String ADMIN_SERVICE_SERVLET_ATTR = "adminService";

    // * as a Service impl related constants
    public static final String ORIGINATED_SERVICE = "originatedService";

    // Configuration file name
    public static final String STRATOS_CONF_FILE = "stratos.xml";
    //public static final String STRATOS_CONF_LOC = "repository/conf/";
    //public static final String STRATOS_CONF_FILE_WITH_PATH = STRATOS_CONF_LOC + STRATOS_CONF_FILE;

    // EULA location
    public static final String STRATOS_EULA = "eula.xml";

    // EULA default text.
    public static final String STRATOS_EULA_DEFAULT_TEXT =
            "Please refer to: " + StratosConstants.STRATOS_TERMS_OF_USAGE +
                    " for terms and usage and " + StratosConstants.STRATOS_PRIVACY_POLICY +
                    " for privacy policy of WSO2 Stratos.";

    // Web location of Terms of Usage and privacy policy
    public static final String STRATOS_TERMS_OF_USAGE =
            "http://wso2.com/cloud/services/terms-of-use/";
    public static final String STRATOS_PRIVACY_POLICY =
            "http://wso2.com/cloud/services/privacy-policy/";
    public static final String MULTITENANCY_FREE_PLAN = "Demo";
    public static final String MULTITENANCY_SMALL_PLAN = "SMB";
    public static final String MULTITENANCY_MEDIUM_PLAN = "Professional";
    public static final String MULTITENANCY_LARGE_PLAN = "Enterprise";
    public static final String EMAIL_CONFIG= "email";
    public static final String MULTITENANCY_CONFIG_FOLDER = "multitenancy";
    
    // Cloud controller - payload
    public static final String MEMBER_ID= "MEMBER_ID";
    public static final String LB_CLUSTER_ID= "LB_CLUSTER_ID";
    public static final String NETWORK_PARTITION_ID= "NETWORK_PARTITION_ID";

    // Kubernetes related constants
    public static final String KUBERNETES_CLUSTER_ID = "KUBERNETES_CLUSTER_ID";
    public static final String KUBERNETES_DEPLOYER_TYPE = "kubernetes";
    public static final String KUBERNETES_MASTER_IP = "KUBERNETES_MASTER_IP";
    public static final String KUBERNETES_MIN_REPLICAS = "KUBERNETES_REPLICAS_MIN";
    public static final String KUBERNETES_MAX_REPLICAS = "KUBERNETES_REPLICAS_MAX";
    public static final String KUBERNETES_PORT_RANGE = "KUBERNETES_PORT_RANGE";
    public static final String ALLOCATED_SERVICE_HOST_PORT = "ALLOCATED_SERVICE_HOST_PORT";
    
    //drools related constants
    public static final String DROOLS_DIR_NAME = "drools";
    public static final String CONTAINER_SCALE_CHECK_DROOL_FILE = "container-scaling.drl";
    public static final String VM_SCALE_CHECK_DROOL_FILE = "scaling.drl";
    public static final String CONTAINER_MIN_CHECK_DROOL_FILE = "container-mincheck.drl";
    public static final String VM_MIN_CHECK_DROOL_FILE = "mincheck.drl";
    public static final String CONTAINER_OBSOLETE_CHECK_DROOL_FILE = "container-obsoletecheck.drl";
    public static final String VM_OBSOLETE_CHECK_DROOL_FILE = "obsoletecheck.drl";

    // Policy and definition related constants
    public static final int PUBLIC_DEFINITION = 0;
    
    // Autoscaler member timeout constants
    public static final String PENDING_VM_MEMBER_EXPIRY_TIMEOUT = "autoscaler.member.vm.pendingMemberExpiryTimeout";
    public static final String OBSOLETED_VM_MEMBER_EXPIRY_TIMEOUT = "autoscaler.member.vm.obsoletedMemberExpiryTimeout";
    public static final String PENDING_CONTAINER_MEMBER_EXPIRY_TIMEOUT = "autoscaler.member.container.pendingMemberExpiryTimeout";
    public static final String OBSOLETED_CONTAINER_MEMBER_EXPIRY_TIMEOUT = "autoscaler.member.container.obsoletedMemberExpiryTimeout";
}

