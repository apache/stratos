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
package org.wso2.carbon.lb.common.conf.util;

/**
 * This keeps the element names used in loadbalancer.conf file
 */
public class Constants {
    
    public static final String LOAD_BALANCER_ELEMENT = "loadbalancer";
    public static final String SERVICES_ELEMENT = "services";
    public static final String DEFAULTS_ELEMENT = "defaults";
    public static final String HOSTS_ELEMENT = "hosts";
    public static final String HOSTS_DELIMITER = ",";
    public static final String DOMAIN_ELEMENT = "domains";
    public static final String TENANT_RANGE_ELEMENT = "tenant_range";
    public static final String SUB_DOMAIN_ELEMENT = "sub_domain";
    public static final String TENANT_RANGE_DELIMITER = "-";
    public static final String UNLIMITED_TENANT_RANGE = "*";
    public static final String AUTOSCALER_ENABLE_ELEMENT = "enable_autoscaler";
    public static final String SUB_DOMAIN_DELIMITER = "#";
    public static final String DEFAULT_SUB_DOMAIN = "__$default";
    
    /* Nginx format related constants */
    
    public static final String NGINX_COMMENT = "#";
    public static final String NGINX_NODE_START_BRACE = "{";
    public static final String NGINX_NODE_END_BRACE = "}";
    public static final String NGINX_VARIABLE = "${";
    public static final String NGINX_LINE_DELIMITER = ";";
    public static final String NGINX_SPACE_REGEX = "[\\s]+";
    
    
}
