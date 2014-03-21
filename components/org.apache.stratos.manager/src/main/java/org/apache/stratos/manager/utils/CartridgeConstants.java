/*
 * Licensed to the Apache Software Foundation (ASF) under one 
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY 
 * KIND, either express or implied.  See the License for the 
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.manager.utils;

public class CartridgeConstants {
	public static final String AUTOSCALER_SERVICE_URL = "autoscaler.service.url";
    public static final String CLOUD_CONTROLLER_SERVICE_URL = "cloud.controller.service.url";
    public static final String ALIAS_NAMESPACE ="http://org.wso2.securevault/configuration";
    public static final String ALIAS_LOCALPART ="secretAlias";
    public static final String ALIAS_PREFIX ="svns";
	public static final String SUBSCRIPTION_ACTIVE = "SUBSCRIPTION_ACTIVE";
	public static final String SUBSCRIPTION_INACTIVE = "SUBSCRIPTION_INACTIVE";
	public static final String ACTIVE = "ACTIVE";
	public static final String NOT_READY = "NOT-READY";
	public static final String SUBSCRIBED = "SUBSCRIBED";
    public static final String UNSUBSCRIBED = "UNSUBSCRIBED";
    public static final String PUPPET_IP = "puppet.ip";
    public static final String PUPPET_HOSTNAME = "puppet.hostname";
    public static final String PUPPET_ENVIRONMENT = "puppet.environment";

	public static final String SUDO_SH = "sudo sh";
	public static final String APPEND_SCRIPT = "append.script";
	public static final String REMOVE_SCRIPT = "remove.script";
	public static final String BIND_FILE_PATH = "bind.file.path";
	
	public static final String FEATURE_EXTERNAL_REPO_VAIDATION_ENABLED = "feature.externalrepo.validation.enabled";
	public static final String FEATURE_INTERNAL_REPO_ENABLED = "feature.internalrepo.enabled";
	public static final String FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED = "feature.multitenant.multiplesubscription.enabled";
	
	public static final String MYSQL_CARTRIDGE_NAME = "mysql";
    public static final String PHP_CARTRIDGE_NAME = "php";
    public static final String TOMCAT_CARTRIDGE_NAME = "tomcat";

	public static final String DEFAULT_SUBDOMAIN = "__$default";
	public static final String DEFAULT_MGT_SUBDOMAIN = "mgt";
	public static final String MYSQL_DEFAULT_USER = "root";
	public static final String PROVIDER_NAME_WSO2 = "wso2";
	public static final String NOT_SUBSCRIBED = "NOT-SUBSCRIBED";
    public static final String SECURITY_KEY_FILE = "gitRepoKey.xml";
    public static final String SECURITY_KEY = "securityKey";
    public static final String DEFAULT_SECURITY_KEY = "tvnw63ufg9gh5111";
    public static final String DATA_CARTRIDGE_PROVIDER = "data";
	public static final String INTERNAL_GIT_USERNAME = "internal.repo.username";
	public static final String INTERNAL_GIT_PASSWORD = "internal.repo.password";

    public static final String CUSTOM_PAYLOAD_PARAM_NAME_PREFIX = "payload_parameter.";
    
    public static final String CC_SOCKET_TIMEOUT = "cc.socket.timeout";
    public static final String CC_CONNECTION_TIMEOUT = "cc.connection.timeout";
    public static final String AUTOSCALER_SOCKET_TIMEOUT = "autoscaler.socket.timeout";
    public static final String AUTOSCALER_CONNECTION_TIMEOUT = "autoscaler.connection.timeout";
    
	public static final String COMMIT_ENABLED = "COMMIT_ENABLED";

	// BAM publisher related values
	public static final String BAM_PUBLISHER_ENABLED = "bam.publisher.enabled";
	public static final String BAM_ADMIN_USERNAME = "bam.admin.username";
	public static final String BAM_ADMIN_PASSWORD = "bam.admin.password";
	public static final String DATA_PUB_TASK_NAME = "CartridgeSubscriptionDataPublisher";
	public static final String STRATOS_MANAGER_EVENT_STREAM = "org_apache_stratos_manager";

	// BAM stream definition relate values
	public static final String TENANT_ID_COL = "tenantID";
	public static final String ADMIN_USER_COL = "adminUser";
	public static final String CARTRIDGE_ALIAS_COL = "cartridgeAlias";
	public static final String CARTRIDGE_TYPE_COL = "cartridgeType";
	public static final String REPOSITORY_URL_COL = "repositoryUrl";
	public static final String MULTI_TENANT_BEHAVIOR_COL = "isMultiTenant";
	public static final String AUTO_SCALE_POLICY_COL = "autoScalePolicy";
	public static final String DEPLOYMENT_POLICY_COL = "deploymentPolicy";
	public static final String CLUSTER_ID_COL = "clusterId";
	public static final String HOST_NAME_COL = "hostname";
	public static final String MAPPED_DOMAIN_COL = "mappedDomain";
	public static final String ACTION_COL = "action";

    public static final class DomainMappingInfo {
		public static final String ACTUAL_HOST = "actual.host";
		public static final String HOSTINFO = "hostinfo/";
	}
}
