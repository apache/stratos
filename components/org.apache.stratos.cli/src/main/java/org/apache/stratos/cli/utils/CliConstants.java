/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.stratos.cli.utils;

/**
 * Constants for CLI Tool
 */
public class CliConstants {

    public static final String STRATOS_APPLICATION_NAME = "stratos";
    public static final String STRATOS_URL_ENV_PROPERTY = "STRATOS_URL";
    public static final String STRATOS_USERNAME_ENV_PROPERTY = "STRATOS_USERNAME";
    public static final String STRATOS_PASSWORD_ENV_PROPERTY = "STRATOS_PASSWORD";
    public static final String STRATOS_SHELL_PROMPT = "stratos> ";

    public static final int COMMAND_SUCCESSFULL = 0;
    public static final int COMMAND_FAILED = 1;
    public static final int ERROR_CODE = 2;


    /**
     * The Directory for storing configuration
     */
    public static final String STRATOS_DIR = ".stratos";
    public static final String STRATOS_HISTORY_DIR = ".history";

    public static final String HELP_ACTION = "help";

    /**
     * Subscribe to a cartridge.
     */
    public static final String SUBSCRIBE_ACTION = "subscribe-cartridge";

    public static final String UNSUBSCRIBE_ACTION = "unsubscribe-cartridge";

    /**
     * List the subscribed cartridges
     */
    public static final String LIST_ACTION = "list-subscribed-cartridges";

    /**
     * List the subscribed cartridges' information
     */
    public static final String LIST_INFO_ACTION = "subscribed-cartridges-info";

    /**
     * List the available cartridges
     */
    public static final String CARTRIDGES_ACTION = "list-cartridges";

    /**
     * List the available network partitions
     */
    public static final String NETWORK_PARTITION_ACTION = "list-network-partitions";

    /**
     * List the available autoscaling policies
     */
    public static final String LIST_AUTOSCALE_POLICY = "list-autoscaling-policies";

    /**
     * List the available deploy services
     */
    public static final String LIST_DEPLOY_SERVICE= "list-deploy-services";

    /**
     * Add tenant
     */
    public static final String ADD_TENANT = "create-tenant";

    /**
     * Add application
     */
    public static final String ADD_APPLICATION = "create-application";

    /**
     * Add application signup
     */
    public static final String ADD_APPLICATION_SIGNUP = "add-application";

    /**
     * Add user
     */
    public static final String ADD_USER = "create-user";

    /**
     * Cartridge deployment
     */
    public static final String CARTRIDGE_DEPLOYMENT = "deploy-cartridge";

    /**
     * Partition deployment
     */
    public static final String PARTITION_DEPLOYMENT = "deploy-partition";

    /**
     * List applications
     */
    public static final String LIST_APPLICATIONS = "list-applications";

    /**
     * List partitions
     */
    public static final String LIST_PARTITION = "list-partitions";

    /**
     * List tenants
     */
    public static final String LIST_TENANTS = "list-tenants";

    /**
     * List tenants
     */
    public static final String LIST_USERS = "list-users";

    /**
     * List deployment policies
     */
    public static final String LIST_DEPLOYMENT_POLICIES = "list-deployment-policies";

    /**
     * List members
     */
    public static final String LIST_MEMBERS = "list-members";

    /**
     * List members with LB
     */
    public static final String LIST_MEMBERS_WITH_LB = "list-lb-members";

    /**
     * Autoscaling policy deployment
     */
    public static final String AUTOSCALING_POLICY_DEPLOYMENT = "add-autoscaling-policy";

    /**
     * Deploy, deployment service
     */
    public static final String DEPLOY_SERVICE_DEPLOYMENT = "add-service";

    /**
     * Deployment policy deployment
     */
    public static final String DEPLOYMENT_POLICY_DEPLOYMENT = "deploy-deployment-policy";

    /**
     * Give information of a cartridge.
     */
    public static final String INFO_ACTION = "info";

    /**
     * Update a subscription.
     */
    public static final String UPDATE_SUBSCRIPTION_ACTION = "update-subscription";

    /**
     * Synchronize repository
     */
    public static final String SYNC_ACTION = "sync";

    /**
     * Describe the application signup
     */
    public static final String DESCRIBE_APPLICATION_SIGNUP = "describe-application-signup";
    /**
     * Describe the application
     */
    public static final String DESCRIBE_APPLICATION = "describe-application";
    /**
     * Describe the cartridge
     */
    public static final String DESCRIBE_CARTRIDGE = "describe-cartridge";
    /**
     * Describe the partition
     */
    public static final String DESCRIBE_PARTITION = "describe-partition";
    /**
     * Describe the auto scaling policy
     */
    public static final String DESCRIBE_AUTO_SCALING_POLICY = "describe-autoscaling-policy";

    /**
     * Undeploy application
     */
    public static final String UNDEPLOY_APPLICATION = "undeploy-application";
    /**
     * Undeploy deployed service
     */
    public static final String UNDEPLOY_SERVICE = "remove-service";
    /**
     * Undeploy cartridge definition
     */
    public static final String UNDEPLOY_CARTRIDGE_DEFINITION = "remove-cartridge";

    /**
     * Delete tenant
     */
    public static final String DELETE_TENANT = "remove-tenant";
    /**
     * Delete user
     */
    public static final String DELETE_USER= "remove-user";
    /**
     * Deactivate tenant
     */

    public static final String DEACTIVATE_TENANT = "deactivate-tenant";
    /**
     * Activate tenant
     */
    public static final String ACTIVATE_TENANT = "activate-tenant";

    /**
     * Describe the deployment policy
     */
    public static final String DESCRIBE_DEPLOYMENT_POLICY = "describe-deployment-policy";

    /**
     * Domain mapping
     */
    public static final String ADD_DOMAIN_MAPPING_ACTION = "add-domain-mapping";
    /**
     * Remove Domain mapping
     */
    public static final String REMOVE_DOMAIN_MAPPING_ACTION = "remove-domain-mapping";

    /**
     * List the available policies
     */
    public static final String POLICIES_ACTION = "policies";

    /**
     * remove network partition
     */
    public static final String REMOVE_NETWORK_PARTITION= "remove-network-partition";

    /**
     * Delete application
     */
    public static final String DELETE_APPLICATION= "remove-application";


    /**
     * Delete application signup
     */
    public static final String DELETE_APPLICATION_SIGNUP= "remove-application-signup";

    /**
     * Exit action
     */
    public static final String EXIT_ACTION = "exit";

    public static final String REPO_URL_OPTION = "r";
    public static final String REPO_URL_LONG_OPTION = "repo-url";

    public static final String PRIVATE_REPO_OPTION = "i";
    public static final String PRIVATE_REPO_LONG_OPTION = "private-repo";

    public static final String USERNAME_OPTION = "u";
    public static final String USERNAME_LONG_OPTION = "username";

    public static final String PASSWORD_OPTION = "p";
    public static final String PASSWORD_LONG_OPTION = "password";

    public static final String HELP_OPTION = "h";
    public static final String HELP_LONG_OPTION = "help";

    public static final String POLICY_OPTION = "o";
    public static final String POLICY_LONG_OPTION = "policy";

    public static final String REMOVE_ON_TERMINATION_OPTION = "t";
    public static final String REMOVE_ON_TERMINATION_LONG_OPTION = "remove-on-termination";

    public static final String VOLUME_SIZE_OPTION = "v";
    public static final String VOLUME_SIZE_LONG_OPTION = "volume-size";

    public static final String VOLUME_ID_OPTION = "vi";
    public static final String VOLUME_ID_LONG_OPTION = "volume-id";

    public static final String PERSISTANCE_VOLUME_OPTION = "pv";
    public static final String PERSISTANCE_VOLUME_LONG_OPTION = "persistance-volume";

    public static final String AUTOSCALING_POLICY_OPTION = "ap";
    public static final String AUTOSCALING_POLICY_LONG_OPTION = "autoscaling-policy";

    public static final String DEPLOYMENT_POLICY_OPTION = "dp";
    public static final String DEPLOYMENT_POLICY_LONG_OPTION = "deployment-policy";

    public static final String DATA_ALIAS_OPTION = "d";
    public static final String DATA_ALIAS_LONG_OPTION = "data-alias";

    public static final String ALIAS_OPTION = "a";
    public static final String ALIAS_LONG_OPTION = "alias";

    public static final String CARTRIDGE_TYPE_OPTION = "t";
    public static final String CARTRIDGE_TYPE_LONG_OPTION = "cartridge-type";

    public static final String FULL_OPTION = "f";
    public static final String FULL_LONG_OPTION = "full";

    public static final String FORCE_OPTION = "f";
    public static final String FORCE_LONG_OPTION = "force";

    public static final String TRACE_OPTION = "trace";

    public static final String DEBUG_OPTION = "debug";

    public static final String ENABLE_COMMITS_OPTION = "cm";
    public static final String ENABLE_COMMITS_LONG_OPTION = "enable-commits";

    // Add tenant options
    public static final String FIRST_NAME_OPTION = "f";
    public static final String FIRST_NAME_LONG_OPTION = "first-name";

    public static final String LAST_NAME_OPTION = "l";
    public static final String LAST_NAME_LONG_OPTION = "last-name";

    public static final String DOMAIN_NAME_OPTION = "d";
    public static final String DOMAIN_NAME_LONG_OPTION = "domain-name";

    public static final String EMAIL_OPTION = "e";
    public static final String EMAIL_LONG_OPTION = "email";

    public static final String ACTIVE_OPTION = "a";
    public static final String ACTIVE_LONG_OPTION = "active";

    // Add User options
    public static final String ROLE_NAME_OPTION = "r";
    public static final String ROLE_NAME_LONG_OPTION = "role-name";

    public static final String PROFILE_NAME_OPTION = "pr";
    public static final String PROFILE_NAME_LONG_OPTION = "profile-name";

    // Deployment options
    public static final String RESOURCE_PATH = "p";
    public static final String RESOURCE_PATH_LONG_OPTION = "resource-path";

    // Kubernetes options
    public static final String CLUSTER_ID_OPTION = "c";
    public static final String CLUSTER_ID_LONG_OPTION = "cluster-id";

    public static final String HOST_ID_OPTION = "h";
    public static final String HOST_ID_LONG_OPTION = "host-id";

    // Application options
    public static final String APPLICATION_ID_OPTION = "app";
    public static final String APPLICATION_ID_LONG_OPTION = "application-id";

    public static final String RESPONSE_INTERNAL_SERVER_ERROR = "500";
    public static final String RESPONSE_AUTHORIZATION_FAIL = "403";
    public static final String RESPONSE_NO_CONTENT = "204";
    public static final String RESPONSE_OK = "200";
    public static final String RESPONSE_CREATED = "201";
    public static final String RESPONSE_BAD_REQUEST = "400";
}
