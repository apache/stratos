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

public final class CloudControllerConstants {

    /**
     * cloud-controller XML file's elements
     */
    public static final String CLOUD_CONTROLLER_ELEMENT = "cloudController";
    public static final String IAAS_PROVIDERS_ELEMENT = "iaasProviders";
    public static final String IAAS_PROVIDER_ELEMENT = "iaasProvider";
    public static final String ZONE_ELEMENT = "zone";
    public static final String TYPE_ATTR = "type";
    public static final String NAME_ATTR = "name";

    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR = "name";
    public static final String PROPERTY_VALUE_ATTR = "value";
    public static final String IMAGE_ID_ELEMENT = "imageId";
    public static final String CLASS_NAME_ELEMENT = "className";
    public static final String PROVIDER_ELEMENT = "provider";
    public static final String IDENTITY_ELEMENT = "identity";
    public static final String CREDENTIAL_ELEMENT = "credential";
    public static final String DATA_PUBLISHER_ELEMENT = "dataPublisher";
    public static final String TOPOLOGY_SYNC_ELEMENT = "topologySync";
    public static final String ENABLE_ATTR = "enable";
    public static final String BAM_SERVER_ELEMENT = "bamServer";
    public static final String CRON_ELEMENT = "cron";
    public static final String BAM_SERVER_ADMIN_USERNAME_ELEMENT = "adminUserName";
    public static final String BAM_SERVER_ADMIN_PASSWORD_ELEMENT = "adminPassword";
    public static final String CASSANDRA_INFO_ELEMENT = "cassandraInfo";
    public static final String CONNECTION_URL_ELEMENT = "connectionUrl";
    public static final String USER_NAME_ELEMENT = "userName";
    public static final String PASSWORD_ELEMENT = "password";
    public static final String CLOUD_CONTROLLER_EVENT_STREAM = "org.apache.stratos.cloud.controller";

    /**
     * column names
     */
    public static final String MEMBER_ID_COL = "memberId";
    public static final String CARTRIDGE_TYPE_COL = "cartridgeType";
    public static final String CLUSTER_ID_COL = "clusterId";
    public static final String CLUSTER_INSTANCE_ID_COL = "clusterInstanceId";
    public static final String PARTITION_ID_COL = "partitionId";
    public static final String NETWORK_ID_COL = "networkId";
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
    public static final String TIME_STAMP = "timeStamp";
    public static final String SCALING_REASON = "scalingReason";
    public static final String SCALING_TIME = "scalingTime";

    /**
     * Properties
     */
    public static final String REGION_PROPERTY = "region";
    public static final String AUTO_ASSIGN_IP_PROPERTY = "autoAssignIp";
    public static final String JCLOUDS_ENDPOINT = "jclouds.endpoint";
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
    public static final String IAAS_PROVIDER_ELEMENT_XPATH = "/"
            + IAAS_PROVIDER_ELEMENT;


    public static final String DATA_PUBLISHER_XPATH = "/"
            + CLOUD_CONTROLLER_ELEMENT + "/" + DATA_PUBLISHER_ELEMENT;
    public static final String TOPOLOGY_SYNC_XPATH = "/"
            + CLOUD_CONTROLLER_ELEMENT + "/" + TOPOLOGY_SYNC_ELEMENT;

    /**
     * Secret Manager related aliases.
     */
    public static final String ALIAS_ATTRIBUTE = "secretAlias";
    public static final String ALIAS_ATTRIBUTE_PREFIX = "svns";
    public static final String ALIAS_NAMESPACE = "http://org.wso2.securevault/configuration";

    /**
     * Payload related constants
     */
    public static final String ENTRY_SEPARATOR = ",";

    /**
     * Publisher task related constants
     */
    // default is : data publisher will run in first second of every minute
    public static final String PUB_CRON_EXPRESSION = "1 * * * * ? *";
    public static final String DATA_PUB_TASK_NAME = "CartridgeInstanceDataPublisher";
    public static final String DEFAULT_BAM_SERVER_USER_NAME = "admin";
    public static final String DEFAULT_BAM_SERVER_PASSWORD = "admin";
    public static final String DEFAULT_CASSANDRA_URL = "localhost:9160";
    public static final String DEFAULT_CASSANDRA_USER = "admin";
    public static final String DEFAULT_CASSANDRA_PASSWORD = "admin";


    /**
     * Persistence
     */
    public static final String DATA_RESOURCE = "/cloud.controller/data";
    public static final String TOPOLOGY_RESOURCE = "/cloud.controller/topology";
    public static final String AVAILABILITY_ZONE = "availabilityZone";
    public static final String KEY_PAIR = "keyPair";
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

    // CloudStack specific
    public static final String USER_NAME = "username";
    public static final String DOMAIN_ID = "domainId";
    public static final String DISK_OFFERING = "diskOffering";
    public static final String NETWORK_IDS = "networkIds";

    /**
     * PortRange min max
     */
    public static final int PORT_RANGE_MAX = 65535;
    public static final int PORT_RANGE_MIN = 1;

    /**
     * Load balancing ip type enumeration values
     */
    public static final String LOADBALANCING_IP_TYPE_PRIVATE = "private";
    public static final String LOADBALANCING_IP_TYPE_PUBLIC = "public";

}
