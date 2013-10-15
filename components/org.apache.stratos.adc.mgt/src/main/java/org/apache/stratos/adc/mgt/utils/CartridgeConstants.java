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

package org.apache.stratos.adc.mgt.utils;

public class CartridgeConstants {
    public static final String SC_IP = "sc.ip";
	public static final String AUTOSCALER_SERVICE_URL = "autoscalerService.url";
    public static final String ALIAS_NAMESPACE ="http://org.wso2.securevault/configuration";
    public static final String ALIAS_LOCALPART ="secretAlias";
    public static final String ALIAS_PREFIX ="svns";
	public static final String CARTRIDGE_AGENT_EPR = "cartridge.agent.epr";
	public static final String GIT_HOST_NAME = "git.host.name";
	public static final String GIT_HOST_IP = "git.host.ip";
	public static final String SUBSCRIPTION_ACTIVE = "SUBSCRIPTION_ACTIVE";
	public static final String SUBSCRIPTION_INACTIVE = "SUBSCRIPTION_INACTIVE";
	public static final String REPO_NOTIFICATION_URL = "git.repo.notification.url";
	public static final String ACTIVE = "ACTIVE";
	public static final String NOT_READY = "NOT-READY";
	public static final String SUBSCRIBED = "SUBSCRIBED";

	public static final String DB_DATASOURCE = "adc.datasource";
	public static final String DB_URL = "adc.jdbc.url";
	public static final String DB_DRIVER = "adc.jdbc.driver";
	public static final String DB_USERNAME = "adc.jdbc.username";
	public static final String DB_PASSWORD = "adc.jdbc.password";
	public static final String BAM_IP = "bam.ip";
	public static final String BAM_PORT = "bam.port";
	public static final String SUDO_SH = "sudo sh";
	public static final String APPEND_SCRIPT = "append.script";
	public static final String REMOVE_SCRIPT = "remove.script";
	public static final String BIND_FILE_PATH = "bind.file.path";
	public static final String ELB_IP = "elb.ip";
	public static final String REPO_KEY_PATH = "repo.key.path";
	public static final String REPO_KEY = "repo.key";
	public static final String MAX_ATTEMPTS = "max.attempts";
	public static final String CARTRIDGE_KEY = "cartridge.key";
	
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
	public static final String REPO_INFO_EPR = "repository.info.epr";
	public static final String INTERNAL_GIT_USERNAME = "internal.repo.username";
	public static final String INTERNAL_GIT_PASSWORD = "internal.repo.password";

    public static final class DomainMappingInfo {
		public static final String ACTUAL_HOST = "actual.host";
		public static final String HOSTINFO = "hostinfo/";
	}
}
