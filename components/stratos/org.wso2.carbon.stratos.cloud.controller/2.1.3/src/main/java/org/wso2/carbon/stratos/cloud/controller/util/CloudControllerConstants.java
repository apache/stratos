/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stratos.cloud.controller.util;

import java.io.File;

import org.wso2.carbon.utils.CarbonUtils;

public final class CloudControllerConstants {

    /**
     * cloud-controller XML file's elements
     */
    public static final String CLOUD_CONTROLLER_ELEMENT = "cloudController";
    public static final String SERIALIZATION_DIR_ELEMENT = "serializationDir";
    public static final String IAAS_PROVIDERS_ELEMENT = "iaasProviders";
    public static final String IAAS_PROVIDER_ELEMENT = "iaasProvider";
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
    public static final String PROPERTY_NAME_ATTR= "name";
    public static final String PROPERTY_VALUE_ATTR = "value";
    public static final String IMAGE_ID_ELEMENT = "imageId";
    public static final String SCALE_DOWN_ORDER_ELEMENT = "scaleDownOrder";
    public static final String SCALE_UP_ORDER_ELEMENT = "scaleUpOrder";
    public static final String CLASS_NAME_ELEMENT = "className";
    public static final String MAX_INSTANCE_LIMIT_ELEMENT = "maxInstanceLimit";
    public static final String PROVIDER_ELEMENT = "provider";
    public static final String IDENTITY_ELEMENT = "identity";
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
    public static final String PAYLOAD_ELEMENT = "payload";
    public static final String DATA_PUBLISHER_ELEMENT = "dataPublisher";
    public static final String TOPOLOGY_SYNC_ELEMENT = "topologySync";
    public static final String ENABLE_ATTR = "enable";
    public static final String BAM_SERVER_ELEMENT = "bamServer";
    public static final String MB_SERVER_ELEMENT = "mbServerUrl";
    public static final String CRON_ELEMENT = "cron";
    public static final String BAM_SERVER_ADMIN_USERNAME_ELEMENT = "adminUserName";
    public static final String BAM_SERVER_ADMIN_PASSWORD_ELEMENT = "adminPassword";
    public static final String CASSANDRA_INFO_ELEMENT = "cassandraInfo";
    public static final String HOST_ELEMENT = "host";
    public static final String CONNECTION_URL_ELEMENT = "connectionUrl";
    public static final String HOST_PORT_ELEMENT = "port";
    public static final String USER_NAME_ELEMENT = "userName";
    public static final String PASSWORD_ELEMENT = "password";
    public static final String CLOUD_CONTROLLER_EVENT_STREAM = "org.wso2.stratos.cloud.controller";
    public static final String CLOUD_CONTROLLER_COL_FAMILY = CLOUD_CONTROLLER_EVENT_STREAM.replaceAll("[/.]", "_");
    
    /**
     * column names
     */
    public static final String PAYLOAD_PREFIX = "payload_";
    public static final String NODE_ID_COL = "nodeId";
    public static final String CARTRIDGE_TYPE_COL = "cartridgeType";
    public static final String DOMAIN_COL = "domain";
    public static final String SUB_DOMAIN_COL = "subDomain";
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
    
    
    /**
     * Properties
     */
    public static final String REGION_PROPERTY = "region";
    public static final String PUBLIC_IP_PROPERTY = "public_ip";
    public static final String TENANT_ID_PROPERTY = "tenant_id";
    public static final String ALIAS_PROPERTY = "alias";
    public static final String AUTO_ASSIGN_IP_PROPERTY = "autoAssignIp";
    
    /**
     * XPath expressions
     */
    public static final String IAAS_PROVIDER_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+"/"+
     IAAS_PROVIDERS_ELEMENT+"/"+IAAS_PROVIDER_ELEMENT;
    public static final String PROPERTY_ELEMENT_XPATH = "/"+PROPERTY_ELEMENT;
    public static final String IMAGE_ID_ELEMENT_XPATH = "/"+IMAGE_ID_ELEMENT;
    public static final String SCALE_UP_ORDER_ELEMENT_XPATH = "/"+SCALE_UP_ORDER_ELEMENT;
    public static final String SCALE_DOWN_ORDER_ELEMENT_XPATH = "/"+SCALE_DOWN_ORDER_ELEMENT;
    public static final String PROVIDER_ELEMENT_XPATH = "/"+PROPERTY_ELEMENT;
    public static final String IDENTITY_ELEMENT_XPATH = "/"+IDENTITY_ELEMENT;
    public static final String CREDENTIAL_ELEMENT_XPATH = "/"+CREDENTIAL_ELEMENT;
    public static final String SERVICES_ELEMENT_XPATH = "/"+SERVICES_ELEMENT+"/"+SERVICE_ELEMENT;
    public static final String SERVICE_ELEMENT_XPATH = "/"+SERVICE_ELEMENT;
    public static final String CARTRIDGE_ELEMENT_XPATH = "/"+CARTRIDGE_ELEMENT;
    public static final String PAYLOAD_ELEMENT_XPATH = "/"+PAYLOAD_ELEMENT;
    public static final String HOST_ELEMENT_XPATH = "/"+HOST_ELEMENT;
    public static final String CARTRIDGES_ELEMENT_XPATH = "/"+CARTRIDGES_ELEMENT+"/"+CARTRIDGE_ELEMENT;
    public static final String IAAS_PROVIDER_ELEMENT_XPATH = "/"+IAAS_PROVIDER_ELEMENT;
    public static final String DEPLOYMENT_ELEMENT_XPATH = "/"+DEPLOYMENT_ELEMENT;
    public static final String PORT_MAPPING_ELEMENT_XPATH = "/"+PORT_MAPPING_ELEMENT;
    public static final String APP_TYPES_ELEMENT_XPATH = "/"+APP_TYPES_ELEMENT;
    
    public static final String DATA_PUBLISHER_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
            "/"+DATA_PUBLISHER_ELEMENT;
    public static final String TOPOLOGY_SYNC_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
            "/"+TOPOLOGY_SYNC_ELEMENT;
    public static final String DATA_PUBLISHER_CRON_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
            "/"+CRON_ELEMENT;
    public static final String BAM_SERVER_ADMIN_USERNAME_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
            "/"+BAM_SERVER_ADMIN_USERNAME_ELEMENT;
    public static final String BAM_SERVER_ADMIN_PASSWORD_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
            "/"+BAM_SERVER_ADMIN_PASSWORD_ELEMENT;
//    public static final String CASSANDRA_HOST_ADDRESS_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
//            "/"+CASSANDRA_HOST_ADDRESS;
//    public static final String CASSANDRA_HOST_PORT_XPATH = "/"+CLOUD_CONTROLLER_ELEMENT+
//            "/"+CASSANDRA_HOST_PORT;
    
    
    /**
     * Secret Manager related aliases.
     */
    public static final String ALIAS_ATTRIBUTE = "svns:secretAlias";
    
    /**
     * Payload related constants
     */
    public static final String PAYLOAD_FOLDER = "payload";
    public static final String ENTRY_SEPARATOR = ",";
    
    /**
     * Publisher task related constants
     */
    public static final String DATA_PUB_TASK_TYPE = "CLOUD_CONTROLLER_DATA_PUBLISHER_TASK";
    // default is : data publisher will run in first second of every minute
    public static final String PUB_CRON_EXPRESSION = "1 * * * * ? *";
    public static final String DATA_PUB_TASK_NAME = "CartridgeInstanceDataPublisherTask";
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
	public static final String PAYLOAD_DIR = CarbonUtils.getCarbonHome() + File.separator +
	                                         "resources" + File.separator + PAYLOAD_FOLDER +
	                                         File.separator;
	public static final String SERVICES_DIR = CarbonUtils.getCarbonRepository() 
    		+ File.separator + "services"+File.separator;
    
    /**
     * Topology sync related constants
     */
    public static final String TOPOLOGY_FILE_PATH = CarbonUtils.getCarbonConfigDirPath()+File.separator+"service-topology.conf";
    public static final String TOPIC_NAME = "cloud-controller-topology";
	public static final String TOPOLOGY_SYNC_CRON = "1 * * * * ? *";
	public static final String TOPOLOGY_SYNC_TASK_NAME = "TopologySynchronizerTask";
	public static final String TOPOLOGY_SYNC_TASK_TYPE = "TOPOLOGY_SYNC_TASK";
	public static final String MB_SERVER_URL = "localhost:5672";
    
	/**
	 * Persistence
	 */
	public static final String CLOUD_CONTROLLER_RESOURCE = "/cloud.controller";
	public static final String DATA_RESOURCE = "/data";
    
}
