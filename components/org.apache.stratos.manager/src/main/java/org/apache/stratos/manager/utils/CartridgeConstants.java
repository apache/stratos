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

    public static final String FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED = "feature.multitenant.multiplesubscription.enabled";

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
}
