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
	
	public static final String CARTRIDGE_KEY = "CARTRIDGE_KEY";
	public static final String APP_PATH = "APP_PATH";
	public static final String SERVICE_NAME = "SERVICE_NAME";
    public static final String CLUSTER_ID = "CLUSTER_ID";
    public static final String NETWORK_PARTITION_ID = "NETWORK_PARTITION_ID";
    public static final String PARTITION_ID = "PARTITION_ID";
    public static final String MEMBER_ID = "MEMBER_ID";
    public static final String REPO_URL = "REPO_URL";
    public static final String PORTS = "PORTS";
    public static final String LOG_FILE_PATHS = "LOG_FILE_PATHS";
    public static final String MEMORY_CONSUMPTION = "memory_consumption";
    public static final String LOAD_AVERAGE = "load_average";
    public static final String PORTS_NOT_OPEN = "ports_not_open";
    public static final String MULTITENANT = "MULTITENANT";
    public static final String ENABLE_ARTIFACT_UPDATE = "enable.artifact.update";
    public static final String ARTIFACT_UPDATE_INTERVAL = "artifact.update.interval";
}
