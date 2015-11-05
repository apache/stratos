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

package org.apache.stratos.aws.extension;

/**
 * AWS proxy extension constants.
 */
public class Constants {
	public static final String CEP_STATS_PUBLISHER_ENABLED = "cep.stats.publisher.enabled";
	public static final String THRIFT_RECEIVER_IP = "thrift.receiver.ip";
	public static final String THRIFT_RECEIVER_PORT = "thrift.receiver.port";
	public static final String NETWORK_PARTITION_ID = "network.partition.id";
	public static final String CLUSTER_ID = "cluster.id";
	public static final String SERVICE_NAME = "service.name";
	public static final String AWS_PROPERTIES_FILE = "aws.properties.file";
	public static final String AWS_ACCESS_KEY = "access-key";
	public static final String AWS_SECRET_KEY = "secret-key";
	public static final String LB_PREFIX = "load-balancer-prefix";
	public static final String LOAD_BALANCER_SECURITY_GROUP_NAME = "load-balancer-security-group-name";
	public static final String LOAD_BALANCER_SECURITY_GROUP_DESCRIPTION = "Security group for load balancers created for Apache Stratos.";
	public static final String ELB_ENDPOINT_URL_FORMAT = "elasticloadbalancing.%s.amazonaws.com";
	public static final String EC2_ENDPOINT_URL_FORMAT = "ec2.%s.amazonaws.com";
	public static final String CLOUD_WATCH_ENDPOINT_URL_FORMAT = "monitoring.%s.amazonaws.com";
	public static final String ALLOWED_CIDR_IP_KEY = "allowed-cidr-ip";
	public static final String ALLOWED_PROTOCOLS = "allowed-protocols";
	public static final int LOAD_BALANCER_NAME_MAX_LENGTH = 32;
	public static final int LOAD_BALANCER_PREFIX_MAX_LENGTH = 25;
	public static final int SECURITY_GROUP_NAME_MAX_LENGTH = 255;
	public static final String REQUEST_COUNT_METRIC_NAME = "RequestCount";
	public static final String CLOUD_WATCH_NAMESPACE_NAME = "AWS/ELB";
	public static final String SUM_STATISTICS_NAME = "Sum";
	public static final String LOAD_BALANCER_DIMENTION_NAME = "LoadBalancerName";
	public static final String HTTP_RESPONSE_2XX = "HTTPCode_Backend_2XX";
	public static final String HTTP_RESPONSE_3XX = "HTTPCode_Backend_3XX";
	public static final String HTTP_RESPONSE_4XX = "HTTPCode_Backend_4XX";
	public static final String HTTP_RESPONSE_5XX = "HTTPCode_Backend_5XX";
	public static final String STATISTICS_INTERVAL = "statistics-interval";
	public static final int STATISTICS_INTERVAL_MULTIPLE_OF = 60;
	public static final String LOAD_BALANCER_SSL_CERTIFICATE_ID = "load-balancer-ssl-certificate-id";
	public static final String APP_STICKY_SESSION_COOKIE_NAME = "app-sticky-session-cookie-name";
	public static final String TERMINATE_LBS_ON_EXTENSION_STOP = "terminate.lbs.on.extension.stop";
	public static final String TERMINATE_LB_ON_CLUSTER_REMOVAL = "terminate.lb.on.cluster.removal";
	public static final String STICKINESS_POLICY = "stickiness-policy";
	public static final String OPERATIMG_IN_VPC = "operating.in.vpc";
	public static final String ENABLE_CROSS_ZONE_LOADBALANCING = "enable.cross.zone.load.balancing";
	public static final String INITIAL_AVAILABILITY_ZONES = "initial-availability-zones";
	public static final String EC2_AVAILABILITY_ZONE_PROPERTY = "EC2_AVAILABILITY_ZONE";
}
