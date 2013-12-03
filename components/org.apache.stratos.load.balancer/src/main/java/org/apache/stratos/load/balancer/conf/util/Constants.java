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
    public static final String CONF_PROPERTY_SESSION_TIMEOUT = "session-timeout";
    public static final String CONF_PROPERTY_TOPOLOGY_EVENT_LISTENER_ENABLED = "topology-event-listener-enabled";
    public static final String CONF_PROPERTY_MB_IP = "mb-ip";
    public static final String CONF_PROPERTY_MB_PORT = "mb-port";
    public static final String CONF_PROPERTY_CEP_STATS_PUBLISHER_ENABLED = "cep-stats-publisher-enabled";
    public static final String CONF_PROPERTY_CEP_IP = "cep-ip";
    public static final String CONF_PROPERTY_CEP_PORT = "cep-port";
    public static final String CONF_PROPERTY_CLASS_NAME = "class-name";
    public static final String CONF_PROPERTY_IP = "ip";
    public static final String CONF_PROPERTY_VALUE = "value";
    public static final String CONF_PROPERTY_PROXY = "proxy";
    public static final String CONF_PROPERTY_TOPOLOGY_SERVICE_FILTER = "topology-service-filter";
    public static final String CONF_PROPERTY_TOPOLOGY_CLUSTER_FILTER = "topology-cluster-filter";

    public static final String CONF_DELIMITER_HOSTS = ",";
    public static final long DEFAULT_SESSION_TIMEOUT = 90000;

    /* Nginx format related constants */
    public static final String NGINX_COMMENT = "#";
    public static final String NGINX_NODE_START_BRACE = "{";
    public static final String NGINX_NODE_END_BRACE = "}";
    public static final String NGINX_VARIABLE = "${";
    public static final String NGINX_LINE_DELIMITER = ";";
    public static final String NGINX_SPACE_REGEX = "[\\s]+";
}
