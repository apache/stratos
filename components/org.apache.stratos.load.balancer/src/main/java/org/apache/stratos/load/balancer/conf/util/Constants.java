/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.conf.util;

/**
 * This keeps the element names used in loadbalancer.conf file
 */
public class Constants {

    /* Load balancer configuration elements and properties */
    public static final String CONF_ELEMENT_LOADBALANCER = "loadbalancer";
    public static final String CONF_ELEMENT_ALGORITHMS = "algorithms";
    public static final String CONF_ELEMENT_HOSTS = "hosts";
    public static final String CONF_ELEMENT_SERVICES = "services";
    public static final String CONF_ELEMENT_CLUSTERS = "clusters";
    public static final String CONF_ELEMENT_MEMBERS = "members";
    public static final String CONF_ELEMENT_PORTS = "ports";

    public static final String CONF_PROPERTY_ALGORITHM = "algorithm";
    public static final String CONF_PROPERTY_FAILOVER = "failover";
    public static final String CONF_PROPERTY_SESSION_AFFINITY = "session-affinity";
    public static final String CONF_PROPERTY_ENDPOINT_TIMEOUT = "endpoint-timeout";
    public static final String CONF_PROPERTY_SESSION_TIMEOUT = "session-timeout";
    public static final String CONF_PROPERTY_TOPOLOGY_EVENT_LISTENER = "topology-event-listener";
    public static final String CONF_PROPERTY_TOPOLOGY_MEMBER_IP_TYPE = "topology-member-ip-type";
    public static final String CONF_PROPERTY_CEP_STATS_PUBLISHER = "cep-stats-publisher";
    public static final String CONF_PROPERTY_CEP_IP = "cep-ip";
    public static final String CONF_PROPERTY_CEP_PORT = "cep-port";
    public static final String CONF_PROPERTY_CLASS_NAME = "class-name";
    public static final String CONF_PROPERTY_IP = "ip";
    public static final String CONF_PROPERTY_VALUE = "value";
    public static final String CONF_PROPERTY_PROXY = "proxy";
    public static final String CONF_PROPERTY_TOPOLOGY_SERVICE_FILTER = "topology-service-filter";
    public static final String CONF_PROPERTY_TOPOLOGY_CLUSTER_FILTER = "topology-cluster-filter";
    public static final String CONF_PROPERTY_TOPOLOGY_MEMBER_FILTER = "topology-member-filter";
    public static final String CONF_PROPERTY_MULTI_TENANCY = "multi-tenancy";
    public static final String CONF_PROPERTY_MULTI_TENANT = "multi-tenant";
    public static final String CONF_PROPERTY_TENANT_IDENTIFIER = "tenant-identifier";
    public static final String CONF_PROPERTY_TENANT_RANGE = "tenant-range";
    public static final String CONF_PROPERTY_VALUE_TENANT_ID = "tenant-id";
    public static final String CONF_PROPERTY_VALUE_TENANT_DOMAIN = "tenant-domain";
    public static final String CONF_PROPERTY_TENANT_IDENTIFIER_REGEX = "tenant-identifier-regex";
    public static final String CONF_PROPERTY_NETWORK_PARTITION_ID = "network-partition-id";
    public static final String CONF_PROPERTY_REWRITE_LOCATION_HEADER = "rewrite-location-header";
    public static final String CONF_PROPERTY_MAP_DOMAIN_NAMES = "map-domain-names";

    public static final String CONF_DELIMITER_HOSTS = ",";
    public static final long DEFAULT_ENDPOINT_TIMEOUT = 15000;
    public static final long DEFAULT_SESSION_TIMEOUT = 90000;
    public static final String STATIC_NETWORK_PARTITION = "static-network-partition";
    public static final String STATIC_PARTITION = "static-partition";

    /* Nginx format related constants */
    public static final String NGINX_COMMENT = "#";
    public static final String NGINX_NODE_START_BRACE = "{";
    public static final String NGINX_NODE_END_BRACE = "}";
    public static final String NGINX_VARIABLE = "${";
    public static final String NGINX_LINE_DELIMITER = ";";
    public static final String NGINX_SPACE_REGEX = "[\\s]+";
}
