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
package org.wso2.carbon.autoscaler.service.util;

public final class AutoscalerConstant {

    /**
     * elastic-scaler-config XML file's elements
     */
    public static final String SERIALIZATION_DIR_ELEMENT = "serializationDir";
    public static final String IAAS_PROVIDER_ELEMENT = "iaasProvider";
    public static final String IAAS_PROVIDER_TYPE_ATTR = "type";
    public static final String IAAS_PROVIDER_NAME_ATTR = "name";
    public static final String PROPERTY_ELEMENT = "property";
    public static final String PROPERTY_NAME_ATTR= "name";
    public static final String PROPERTY_VALUE_ATTR = "value";
    public static final String IMAGE_ID_ELEMENT = "imageId";
    public static final String SCALE_DOWN_ORDER_ELEMENT = "scaleDownOrder";
    public static final String SCALE_UP_ORDER_ELEMENT = "scaleUpOrder";
    public static final String PROVIDER_ELEMENT = "provider";
    public static final String IDENTITY_ELEMENT = "identity";
    public static final String CREDENTIAL_ELEMENT = "credential";
    public static final String DEFAULT_SERVICE_ELEMENT = "default";
    public static final String SERVICE_ELEMENT = "service";
    public static final String SERVICE_DOMAIN_ATTR = "domain";
    public static final String SERVICE_SUB_DOMAIN_ATTR = "subDomain";
    
    /**
     * Secret Manager related aliases.
     */
    public static final String EC2_IDENTITY_ALIAS = "elastic.scaler.ec2.identity";
    public static final String EC2_CREDENTIAL_ALIAS = "elastic.scaler.ec2.credential";
    public static final String OPENSTACK_IDENTITY_ALIAS = "elastic.scaler.openstack.identity";
    public static final String OPENSTACK_CREDENTIAL_ALIAS = "elastic.scaler.openstack.credential";
    
    /**
     * Serializer related constants
     */
    public static final String IAAS_CONTEXT_LIST_SERIALIZING_FILE = "iaas-context-list.txt";
    public static final String LASTLY_USED_IAAS_MAP_SERIALIZING_FILE = "lastly-used-iaas.txt";
    
    /**
     * Payload related constants
     */
    public static final String PAYLOAD_DIR = "payload";
    public static final String PARAMS_FILE_NAME = "launch-params";
    public static final String RESOURCES_DIR = "resources";
    public static final String VALUE_SEPARATOR = "=";
    public static final String ENTRY_SEPARATOR = ",";
    public static final String APP_PATH_KEY = "APP_PATH";
    public static final String TENANT_KEY = "TENANT";
    
    /**
	 * Super tenant id
	 */
    public static final String SUPER_TENANT_ID = "-1234";
}
