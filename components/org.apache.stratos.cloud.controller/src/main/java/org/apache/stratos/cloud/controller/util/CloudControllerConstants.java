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
package org.apache.stratos.cloud.controller.util;

import org.wso2.carbon.utils.CarbonUtils;

import java.io.File;

public final class CloudControllerConstants {

	/**
	 * cloud-controller XML file's elements
	 */
	public static final String CLOUD_CONTROLLER_ELEMENT = "cloudController";
	public static final String SERIALIZATION_DIR_ELEMENT = "serializationDir";
	public static final String IAAS_PROVIDERS_ELEMENT = "iaasProviders";
	public static final String IAAS_PROVIDER_ELEMENT = "iaasProvider";
	public static final String PARTITION_ELEMENT = "partition";
	public static final String PARTITIONS_ELEMENT = "partitions";
	public static final String REGION_ELEMENT = "region";
	public static final String ZONE_ELEMENT = "zone";
	public static final String DEPLOYMENT_ELEMENT = "deployment";
	public static final String PORT_MAPPING_ELEMENT = "portMapping";
	public static final String APP_TYPES_ELEMENT = "appTypes";
	public static final String TYPE_ATTR = "type";
	public static final String HOST_ATTR = "host";
	public static final String BASE_DIR_ATTR = "baseDir";
	public static final String PROVIDER_ATTR = "provider";
	public static final String VERSION_ATTR = "version";
	public static final String MULTI_TENANT_ATTR = "multiTenant";
	public static final String PORT_ATTR = "port";
	public static final String PROXY_PORT_ATTR = "proxyPort";
	public static final String NAME_ATTR = "name";
	public static final String APP_SPECIFIC_MAPPING_ATTR = "appSpecificMapping";

	public static final String CARTRIDGES_ELEMENT = "cartridges";
	public static final String CARTRIDGE_ELEMENT = "cartridge";

	public static final String DISPLAY_NAME_ELEMENT = "displayName";
	public static final String DESCRIPTION_ELEMENT = "description";
	public static final String PROPERTY_ELEMENT = "property";
	public static final String PROPERTY_NAME_ATTR = "name";
	public static final String PROPERTY_VALUE_ATTR = "value";
	public static final String IMAGE_ID_ELEMENT = "imageId";
	public static final String SCALE_DOWN_ORDER_ELEMENT = "scaleDownOrder";
	public static final String SCALE_UP_ORDER_ELEMENT = "scaleUpOrder";
	public static final String CLASS_NAME_ELEMENT = "className";
	public static final String PROVIDER_ELEMENT = "provider";
	public static final String IDENTITY_ELEMENT = "identity";
	public static final String TYPE_ELEMENT = "type";
	public static final String SCOPE_ELEMENT = "scope";
	public static final String ID_ELEMENT = "id";
	public static final String CREDENTIAL_ELEMENT = "credential";
	public static final String DEFAULT_SERVICE_ELEMENT = "default";
	public static final String SERVICE_ELEMENT = "service";
	public static final String SERVICES_ELEMENT = "services";
	public static final String DIRECTORY_ELEMENT = "dir";
	public static final String HTTP_ELEMENT = "http";
	public static final String HTTPS_ELEMENT = "https";
	public static final String APP_TYPE_ELEMENT = "appType";
	public static final String SERVICE_DOMAIN_ATTR = "domain";
	public static final String SERVICE_SUB_DOMAIN_ATTR = "subDomain";
	public static final String SERVICE_TENANT_RANGE_ATTR = "tenantRange";
	public static final String POLICY_NAME = "policyName";
	public static final String PAYLOAD_ELEMENT = "payload";
	public static final String DATA_PUBLISHER_ELEMENT = "dataPublisher";
	public static final String TOPOLOGY_SYNC_ELEMENT = "topologySync";
	public static final String ENABLE_ATTR = "enable";
	public static final String BAM_SERVER_ELEMENT = "bamServer";
	public static final String CRON_ELEMENT = "cron";
	public static final String BAM_SERVER_ADMIN_USERNAME_ELEMENT = "adminUserName";
	public static final String BAM_SERVER_ADMIN_PASSWORD_ELEMENT = "adminPassword";
	public static final String CASSANDRA_INFO_ELEMENT = "cassandraInfo";
	public static final String HOST_ELEMENT = "host";
	public static final String CONNECTION_URL_ELEMENT = "connectionUrl";
	public static final String HOST_PORT_ELEMENT = "port";
	public static final String USER_NAME_ELEMENT = "userName";
	public static final String PASSWORD_ELEMENT = "password";
	public static final String CLOUD_CONTROLLER_EVENT_STREAM = "org.apache.stratos.cloud.controller";
	public static final String CLOUD_CONTROLLER_COL_FAMILY = CLOUD_CONTROLLER_EVENT_STREAM
			.replaceAll("[/.]", "_");

	/**
	 * column names
	 */
	public static final String PAYLOAD_PREFIX = "payload_";
	public static final String MEMBER_ID_COL = "memberId";
	public static final String CARTRIDGE_TYPE_COL = "cartridgeType";
	public static final String CLUSTER_ID_COL = "clusterId";
	public static final String PARTITION_ID_COL = "partitionId";
	public static final String NETWORK_ID_COL = "networkId";
	public static final String ALIAS_COL = "alias";
	public static final String TENANT_RANGE_COL = "tenantRange";
	public static final String IS_MULTI_TENANT_COL = "isMultiTenant";
	public static final String IAAS_COL = "iaas";
	public static final String STATUS_COL = "status";
	public static final String HOST_NAME_COL = "hostName";
	public static final String HYPERVISOR_COL = "hypervisor";
	public static final String RAM_COL = "ram";
	public static final String IMAGE_ID_COL = "imageId";
	public static final String LOGIN_PORT_COL = "loginPort";
	public static final String OS_NAME_COL = "osName";
	public static final String OS_VERSION_COL = "osVersion";
	public static final String OS_ARCH_COL = "osArch";
	public static final String OS_BIT_COL = "is64bitOS";
	public static final String PRIV_IP_COL = "privateIPAddresses";
	public static final String PUB_IP_COL = "publicIPAddresses";
	public static final String ALLOCATE_IP_COL = "allocateIPAddresses";

	/**
	 * Properties
	 */
	public static final String REGION_PROPERTY = "region";
	public static final String TOPICS_PROPERTY = "topics";
	public static final String PUBLIC_IP_PROPERTY = "public_ip";
	public static final String TENANT_ID_PROPERTY = "tenant_id";
	public static final String ALIAS_PROPERTY = "alias";
	public static final String AUTO_ASSIGN_IP_PROPERTY = "autoAssignIp";
	public static final String JCLOUDS_ENDPOINT = "jclouds.endpoint";
	public static final String CRON_PROPERTY = "cron";
	public static final String AMQP_CONNECTION_URL_PROPERTY = "amqpConnectionUrl";
	public static final String AMQP_INITIAL_CONTEXT_FACTORY_PROPERTY = "amqpInitialContextFactory";
	public static final String AMQP_TOPIC_CONNECTION_FACTORY_PROPERTY = "amqpTopicConnectionFactory";
	public static final String INSTANCE_TOPIC = "instance/*";
	// pre define a floating ip
	public static final String FLOATING_IP_PROPERTY = "floatingIp";
	public static final String DEFAULT_FLOATING_IP_POOL = "defaultFloatingIpPool";
	public static final String OPENSTACK_NETWORKING_PROVIDER = "openstack.networking.provider";
	public static final String OPENSTACK_NEUTRON_NETWORKING = "neutron";

	/**
	 * XPath expressions
	 */
	public static final String IAAS_PROVIDER_XPATH = "/"
			+ CLOUD_CONTROLLER_ELEMENT + "/" + IAAS_PROVIDERS_ELEMENT + "/"
			+ IAAS_PROVIDER_ELEMENT;
	public static final String PARTITION_XPATH = "/" + CLOUD_CONTROLLER_ELEMENT
			+ "/" + PARTITIONS_ELEMENT + "/" + PARTITION_ELEMENT;
	public static final String REGION_XPATH = "/" + CLOUD_CONTROLLER_ELEMENT
			+ "/" + IAAS_PROVIDERS_ELEMENT + "/" + IAAS_PROVIDER_ELEMENT + "/"
			+ REGION_ELEMENT;
	public static final String ZONE_XPATH = "/" + CLOUD_CONTROLLER_ELEMENT
			+ "/" + IAAS_PROVIDERS_ELEMENT + "/" + IAAS_PROVIDER_ELEMENT + "/"
			+ REGION_ELEMENT + "/" + ZONE_ELEMENT;
	public static final String HOST_XPATH = "/" + CLOUD_CONTROLLER_ELEMENT
			+ "/" + IAAS_PROVIDERS_ELEMENT + "/" + IAAS_PROVIDER_ELEMENT + "/"
			+ REGION_ELEMENT + "/" + ZONE_ELEMENT + "/" + HOST_ELEMENT;
	public static final String PROPERTY_ELEMENT_XPATH = "/" + PROPERTY_ELEMENT;
	public static final String IMAGE_ID_ELEMENT_XPATH = "/" + IMAGE_ID_ELEMENT;
	public static final String SCALE_UP_ORDER_ELEMENT_XPATH = "/"
			+ SCALE_UP_ORDER_ELEMENT;
	public static final String SCALE_DOWN_ORDER_ELEMENT_XPATH = "/"
			+ SCALE_DOWN_ORDER_ELEMENT;
	public static final String PROVIDER_ELEMENT_XPATH = "/" + PROPERTY_ELEMENT;
	public static final String IDENTITY_ELEMENT_XPATH = "/" + IDENTITY_ELEMENT;
	public static final String CREDENTIAL_ELEMENT_XPATH = "/"
			+ CREDENTIAL_ELEMENT;
	public static final String SERVICES_ELEMENT_XPATH = "/" + SERVICES_ELEMENT
			+ "/" + SERVICE_ELEMENT;
	public static final String SERVICE_ELEMENT_XPATH = "/" + SERVICE_ELEMENT;
	public static final String CARTRIDGE_ELEMENT_XPATH = "/"
			+ CARTRIDGE_ELEMENT;
	public static final String PAYLOAD_ELEMENT_XPATH = "/" + PAYLOAD_ELEMENT;
	public static final String HOST_ELEMENT_XPATH = "/" + HOST_ELEMENT;
	public static final String CARTRIDGES_ELEMENT_XPATH = "/"
			+ CARTRIDGES_ELEMENT + "/" + CARTRIDGE_ELEMENT;
	public static final String IAAS_PROVIDER_ELEMENT_XPATH = "/"
			+ IAAS_PROVIDER_ELEMENT;
	public static final String DEPLOYMENT_ELEMENT_XPATH = "/"
			+ DEPLOYMENT_ELEMENT;
	public static final String PORT_MAPPING_ELEMENT_XPATH = "/"
			+ PORT_MAPPING_ELEMENT;
	public static final String APP_TYPES_ELEMENT_XPATH = "/"
			+ APP_TYPES_ELEMENT;

	public static final String DATA_PUBLISHER_XPATH = "/"
			+ CLOUD_CONTROLLER_ELEMENT + "/" + DATA_PUBLISHER_ELEMENT;
	public static final String TOPOLOGY_SYNC_XPATH = "/"
			+ CLOUD_CONTROLLER_ELEMENT + "/" + TOPOLOGY_SYNC_ELEMENT;
	public static final String DATA_PUBLISHER_CRON_XPATH = "/"
			+ CLOUD_CONTROLLER_ELEMENT + "/" + CRON_ELEMENT;
	public static final String BAM_SERVER_ADMIN_USERNAME_XPATH = "/"
			+ CLOUD_CONTROLLER_ELEMENT + "/"
			+ BAM_SERVER_ADMIN_USERNAME_ELEMENT;
	public static final String BAM_SERVER_ADMIN_PASSWORD_XPATH = "/"
			+ CLOUD_CONTROLLER_ELEMENT + "/"
			+ BAM_SERVER_ADMIN_PASSWORD_ELEMENT;
	// public static final String CASSANDRA_HOST_ADDRESS_XPATH =
	// "/"+CLOUD_CONTROLLER_ELEMENT+
	// "/"+CASSANDRA_HOST_ADDRESS;
	// public static final String CASSANDRA_HOST_PORT_XPATH =
	// "/"+CLOUD_CONTROLLER_ELEMENT+
	// "/"+CASSANDRA_HOST_PORT;

	/**
	 * Secret Manager related aliases.
	 */
    public static final String ALIAS_ATTRIBUTE = "secretAlias";
    public static final String ALIAS_ATTRIBUTE_PREFIX = "svns";
    public static final String ALIAS_NAMESPACE = "http://org.wso2.securevault/configuration";

	/**
	 * Payload related constants
	 */
	public static final String PAYLOAD_NAME = "payload";
	public static final String ENTRY_SEPARATOR = ",";

	/**
	 * Publisher task related constants
	 */
	public static final String DATA_PUB_TASK_TYPE = "CLOUD_CONTROLLER_DATA_PUBLISHER_TASK";
	// default is : data publisher will run in first second of every minute
	public static final String PUB_CRON_EXPRESSION = "1 * * * * ? *";
	public static final String DATA_PUB_TASK_NAME = "CartridgeInstanceDataPublisher";
	public static final String DEFAULT_BAM_SERVER_USER_NAME = "admin";
	public static final String DEFAULT_BAM_SERVER_PASSWORD = "admin";
	public static final String DEFAULT_CASSANDRA_URL = "localhost:9160";
	public static final String DEFAULT_CASSANDRA_USER = "admin";
	public static final String DEFAULT_CASSANDRA_PASSWORD = "admin";
	public static final String DEFAULT_CASSANDRA_CLUSTER_NAME = "Test Cluster";
	public static final String DEFAULT_CASSANDRA_KEY_SPACE = "EVENT_KS";

	/**
	 * Directories
	 */
	public static final String SERVICES_DIR = CarbonUtils.getCarbonRepository()
			+ File.separator + "services" + File.separator;

	/**
	 * Topology sync related constants
	 */
	public static final String TOPOLOGY_FILE_PATH = CarbonUtils
			.getCarbonConfigDirPath()
			+ File.separator
			+ "service-topology.conf";
	public static final String TOPOLOGY_SYNC_CRON = "1 * * * * ? *";
	public static final String TOPOLOGY_SYNC_TASK_NAME = "TOPOLOGY_SYNC_TASK";
	public static final String TOPOLOGY_SYNC_TASK_TYPE = "TOPOLOGY_SYNC_TASK_TYPE";
	public static final String AMQP_CONNECTION_URL = "amqp://admin:admin@clientID/carbon?brokerlist='tcp://localhost:5672'";
	public static final String AMQP_INITIAL_CONTEXT_FACTORY = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
	public static final String AMQP_TOPIC_CONNECTION_FACTORY = "qpidConnectionfactory";

	/**
	 * Persistence
	 */
	public static final String DATA_RESOURCE = "/cloud.controller/data";
	public static final String TOPOLOGY_RESOURCE = "/cloud.controller/topology";
	public static final String AVAILABILITY_ZONE = "availabilityZone";
	public static final String KEY_PAIR = "keyPair";
	public static final String HOST = "host";
	public static final String SECURITY_GROUP_IDS = "securityGroupIds";
	public static final String SECURITY_GROUPS = "securityGroups";
	public static final String SUBNET_ID = "subnetId";
	public static final String TAGS = "tags";
	public static final String TAGS_AS_KEY_VALUE_PAIRS_PREFIX = "tag:";
	public static final String AUTO_ASSIGN_IP = "autoAssignIp";
	public static final String BLOCK_UNTIL_RUNNING = "blockUntilRunning";
	public static final String INSTANCE_TYPE = "instanceType";
	public static final String ASSOCIATE_PUBLIC_IP_ADDRESS = "associatePublicIpAddress";
	public static final String LB_CLUSTER_ID_COL = "lbclusterId";
	public static final String NETWORK_INTERFACES = "networkInterfaces";
	public static final String NETWORK_FIXED_IP = "fixedIp";
	public static final String NETWORK_PORT = "portUuid";
	public static final String NETWORK_UUID = "networkUuid";

    // CloudStack specific
    public static final String USER_NAME = "username";
    public static final String DOMAIN_ID = "domainId";
    public static final String DISK_OFFERING = "diskOffering";
    public static final String NETWORK_IDS= "networkIds";
    
    public static final String IS_LOAD_BALANCER = "load.balancer";
    
    /**
     * PortRange min max
     */
    public static final int PORT_RANGE_MAX = 65535;
    public static final int PORT_RANGE_MIN = 1;
    
    public static final String KUBERNETES_PARTITION_PROVIDER = "kubernetes";

	/**
	 * Load balancing ip type enumeration values
	 */
	public static final String LOADBALANCING_IP_TYPE_PRIVATE = "private";
	public static final String LOADBALANCING_IP_TYPE_PUBLIE = "public";

}
