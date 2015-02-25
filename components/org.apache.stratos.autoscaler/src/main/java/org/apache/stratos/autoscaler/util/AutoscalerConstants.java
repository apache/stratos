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
package org.apache.stratos.autoscaler.util;

import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;

public final class AutoscalerConstants {

    /**
     * Constant values for Autoscaler
     */
    public static final String ID_ELEMENT = "id";
    public static final String PARTITION_ELEMENT = "partition";
    public static final String PARTITIONS_ELEMENT = "partitions";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR= "name";
    public static final String PROPERTY_VALUE_ATTR = "value";
    public static final String AUTOSCALER_THREAD_POOL_ID = "autoscaler.thread.pool";
    public static final String THREAD_POOL_SIZE_KEY = "autoscaler.thread.pool.size";
    public static final int AUTOSCALER_THREAD_POOL_SIZE = 50;
    public static final String COMPONENTS_CONFIG = CarbonUtils.getCarbonConfigDirPath() +
            File.separator + "stratos-config.xml";
    
    /**
	 * Persistence
	 */
	public static final String AUTOSCALER_RESOURCE = "/autoscaler";
	public static final String PARTITION_RESOURCE = "/partitions";
	public static final String AS_POLICY_RESOURCE = "/policies/autoscalingPolicies";
    public static final String APPLICATIONS_RESOURCE = "/applications";
    public static final String APPLICATION_CONTEXTS_RESOURCE = "/applicationContexts";
    public static final String APPLICATION_POLICY_RESOURCE = "/policies/applicationPolicies";

    /**
     * Cluster monitoring  interval
     */
    public static final String Cluster_MONITOR_INTERVAL = "autoscaler.cluster.monitorInterval";

    public static final String SERVICE_GROUP = "/groups";

    /**
     * PortRange min max
     */
    public static final int PORT_RANGE_MAX = 65535;
    public static final int PORT_RANGE_MIN = 1;
    
    /**
     * Payload values
     */
    public static final String PAYLOAD_DEPLOYMENT = "default";

    public static final String APPLICATION_MONITOR_THREAD_POOL_ID = "application.monitor.thread.pool";
    public static final String APPLICATION_MONITOR_THREAD_POOL_SIZE = "application.monitor.thread.pool.size";
    public static final String GROUP_MONITOR_THREAD_POOL_ID = "group.monitor.thread.pool";
    public static final String GROUP_MONITOR_THREAD_POOL_SIZE = "group.monitor.thread.pool.size";
    public static final String CLUSTER_MONITOR_SCHEDULER_ID = "cluster.monitor.scheduler";
    public static final String CLUSTER_MONITOR_THREAD_POOL_ID = "cluster.monitor.thread.pool";
    public static final String CLUSTER_MONITOR_THREAD_POOL_SIZE = "cluster.monitor.thread.pool.size";
	public static final String MEMBER_FAULT_EVENT_NAME = "member_fault";
	//scheduler
	public static final int SCHEDULE_DEFAULT_INITIAL_DELAY = 30;
	public static final int SCHEDULE_DEFAULT_PERIOD = 15;
	public static final String APPLICATION_SYNC_CRON = "1 * * * * ? *";
	public static final String APPLICATION_SYNC_TASK_NAME = "APPLICATION_SYNC_TASK";
	public static final String APPLICATION_SYNC_TASK_TYPE = "APPLICATION_SYNC_TASK_TYPE";
	public static final String AUTOSCALER_CONFIG_FILE_NAME = "autoscaler.xml";
	public static final String CLOUD_CONTROLLER_SERVICE_SFX = "services/CloudControllerService";
	public static final int CLOUD_CONTROLLER_DEFAULT_PORT = 9444;
	public static final String STRATOS_MANAGER_SERVICE_SFX = "services/InstanceCleanupNotificationService";
	public static final int STRATOS_MANAGER_DEFAULT_PORT = 9445;
	public static final String STRATOS_MANAGER_HOSTNAME_ELEMENT = "autoscaler.stratosManager.hostname";
	public static final String STRATOS_MANAGER_DEFAULT_PORT_ELEMENT = "autoscaler.stratosManager.port";
	public static final String STRATOS_MANAGER_CLIENT_TIMEOUT_ELEMENT= "autoscaler.stratosManager.clientTimeout";
	// partition properties
	public static final String REGION_PROPERTY = "region";
	public static final String MEMBER_AVERAGE_LOAD_AVERAGE = "member_average_load_average";
	public static final String MEMBER_AVERAGE_MEMORY_CONSUMPTION = "member_average_memory_consumption";
	public static final String AVERAGE_REQUESTS_IN_FLIGHT = "average_in_flight_requests";
	public static final String MEMBER_GRADIENT_LOAD_AVERAGE = "member_gradient_load_average";
	public static final String MEMBER_GRADIENT_MEMORY_CONSUMPTION = "member_gradient_memory_consumption";
	public static final String GRADIENT_OF_REQUESTS_IN_FLIGHT = "gradient_in_flight_requests";
	public static final String MEMBER_SECOND_DERIVATIVE_OF_MEMORY_CONSUMPTION = "member_second_derivative_memory_consumption";
	public static final String MEMBER_SECOND_DERIVATIVE_OF_LOAD_AVERAGE = "member_second_derivative_load_average";
	public static final String SECOND_DERIVATIVE_OF_REQUESTS_IN_FLIGHT = "second_derivative_in_flight_requests";
	public static final String AVERAGE_LOAD_AVERAGE = "average_load_average";
	public static final String AVERAGE_MEMORY_CONSUMPTION = "average_memory_consumption";
	public static final String GRADIENT_LOAD_AVERAGE = "gradient_load_average";
	public static final String GRADIENT_MEMORY_CONSUMPTION = "gradient_memory_consumption";
	public static final String SECOND_DERIVATIVE_OF_MEMORY_CONSUMPTION = "second_derivative_memory_consumption";
	public static final String SECOND_DERIVATIVE_OF_LOAD_AVERAGE = "second_derivative_load_average";
	//member expiry interval
	public static final String MEMBER_EXPIRY_INTERVAL = "member.expiry.interval";
	//Grouping
	public static final String TERMINATE_NONE = "terminate-none";
	public static final String TERMINATE_ALL = "terminate-all";
	public static final String GROUP = "group";
	public static final String CARTRIDGE = "cartridge";
	public static final int IS_DEFAULT_PORT = 9443;
	public static final String OAUTH_SERVICE_SFX = "services/OAuthAdminService";
	public static final String IDENTITY_APPLICATION_SERVICE_SFX = "services/IdentityApplicationManagementService";
	public static final String TOKEN_ENDPOINT_SFX = "oauth2/token";
	public static final String TERMINATE_DEPENDENTS = "terminate-dependents";	
}
