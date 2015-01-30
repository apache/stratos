/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.util;

import java.io.Serializable;

public class CartridgeAgentConstants implements Serializable{

	public static final String JNDI_PROPERTIES_DIR = "jndi.properties.dir";
	public static final String PARAM_FILE_PATH = "param.file.path";
    public static final String EXTENSIONS_DIR = "extensions.dir";

    public static final String INSTANCE_STARTED_SH = "instance-started.sh";
    public static final String START_SERVERS_SH = "start-servers.sh";
    public static final String INSTANCE_ACTIVATED_SH = "instance-activated.sh";
    public static final String ARTIFACTS_UPDATED_SH = "artifacts-updated.sh";
    public static final String CLEAN_UP_SH = "clean.sh";
    public static final String MOUNT_VOLUMES_SH = "mount_volumes.sh";
    public static final String SUBSCRIPTION_DOMAIN_ADDED_SH = "subscription-domain-added.sh";
    public static final String SUBSCRIPTION_DOMAIN_REMOVED_SH = "subscription-domain-removed.sh";

	public static final String CARTRIDGE_KEY = "CARTRIDGE_KEY";
	public static final String APP_PATH = "APP_PATH";
	public static final String APPLICATION_ID = "APPLICATION_ID";
    public static final String SERVICE_GROUP = "SERIVCE_GROUP";
    public static final String SERVICE_NAME = "SERVICE_NAME";
    public static final String CLUSTER_ID = "CLUSTER_ID";
    public static final String LB_CLUSTER_ID = "LB_CLUSTER_ID";
    public static final String NETWORK_PARTITION_ID = "NETWORK_PARTITION_ID";
    public static final String PARTITION_ID = "PARTITION_ID";
    public static final String MEMBER_ID = "MEMBER_ID";
    public static final String TENANT_ID= "TENANT_ID";
    public static final String REPO_URL = "REPO_URL";
    public static final String PORTS = "PORTS";
    public static final String DEPLOYMENT = "DEPLOYMENT";
    public static final String MANAGER_SERVICE_TYPE = "MANAGER_SERVICE_TYPE";
    public static final String WORKER_SERVICE_TYPE = "WORKER_SERVICE_TYPE";
    public static final String DEPENDENCY_CLUSTER_IDS = "DEPENDENCY_CLUSTER_IDS";
    public static final String CLUSTER_INSTANCE_ID = "CLUSTER_INSTANCE_ID";

    // stratos.sh environment variables keys
    public static final String LOG_FILE_PATHS = "LOG_FILE_PATHS";
    public static final String MEMORY_CONSUMPTION = "memory_consumption";
    public static final String LOAD_AVERAGE = "load_average";
    public static final String PORTS_NOT_OPEN = "ports_not_open";
    public static final String MULTITENANT = "MULTITENANT";
    public static final String CLUSTERING = "CLUSTERING";
    public static final String MIN_INSTANCE_COUNT = "MIN_COUNT";
    public static final String ENABLE_ARTIFACT_UPDATE = "enable.artifact.update";
    public static final String ARTIFACT_UPDATE_INTERVAL = "artifact.update.interval";
    public static final String COMMIT_ENABLED = "COMMIT_ENABLED";
    public static final String AUTO_COMMIT = "auto.commit";
    public static final String AUTO_CHECKOUT = "auto.checkout";
    public static final String LISTEN_ADDRESS = "listen.address";
    public static final String PROVIDER = "PROVIDER";
    public static final String INTERNAL = "internal";
    public static final String LB_PRIVATE_IP = "lb.private.ip";
    public static final String LB_PUBLIC_IP = "lb.public.ip";

    // stratos.sh extension points shell scripts names keys
    public static final String INSTANCE_STARTED_SCRIPT = "extension.instance.started";
    public static final String START_SERVERS_SCRIPT = "extension.start.servers";
    public static final String INSTANCE_ACTIVATED_SCRIPT = "extension.instance.activated";
    public static final String ARTIFACTS_UPDATED_SCRIPT = "extension.artifacts.updated";
    public static final String CLEAN_UP_SCRIPT = "extension.clean";
    public static final String MOUNT_VOLUMES_SCRIPT = "extension.mount.volumes";
    public static final String MEMBER_ACTIVATED_SCRIPT = "extension.member.activated";
    public static final String MEMBER_TERMINATED_SCRIPT = "extension.member.terminated";
    public static final String MEMBER_SUSPENDED_SCRIPT = "extension.member.suspended";
    public static final String MEMBER_STARTED_SCRIPT = "extension.member.started";
    public static final String COMPLETE_TOPOLOGY_SCRIPT = "extension.complete.topology";
    public static final String COMPLETE_TENANT_SCRIPT = "extension.complete.tenant";
    public static final String SUBSCRIPTION_DOMAIN_ADDED_SCRIPT = "extension.subscription.domain.added";
    public static final String SUBSCRIPTION_DOMAIN_REMOVED_SCRIPT = "extension.subscription.domain.removed";
    public static final String ARTIFACTS_COPY_SCRIPT = "extension.artifacts.copy";
    public static final String TENANT_SUBSCRIBED_SCRIPT = "extension.tenant.subscribed";
    public static final String TENANT_UNSUBSCRIBED_SCRIPT = "extension.tenant.unsubscribed";

    public static final String SERVICE_GROUP_TOPOLOGY_KEY = "payload_parameter.SERIVCE_GROUP";
    public static final String CLUSTERING_TOPOLOGY_KEY = "payload_parameter.CLUSTERING";
    public static final String CLUSTERING_PRIMARY_KEY = "PRIMARY";
    
    public static final String SUPERTENANT_TEMP_PATH = "/tmp/-1234/";

    public static final String DEPLOYMENT_MANAGER = "manager";
    public static final String DEPLOYMENT_WORKER = "worker";
    public static final String DEPLOYMENT_DEFAULT = "default";
    public static final String SUPER_TENANT_REPO_PATH = "super.tenant.repository.path";
    public static final String TENANT_REPO_PATH = "tenant.repository.path";
    
    public static final String KUBERNETES_CLUSTER_ID = "KUBERNETES_CLUSTER_ID";
    public static final String KUBERNETES_MASTER_IP = "KUBERNETES_MASTER_IP";
}
