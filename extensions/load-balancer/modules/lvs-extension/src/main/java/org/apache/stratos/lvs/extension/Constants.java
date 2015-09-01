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

package org.apache.stratos.lvs.extension;

/**
 * LVS extension constants.
 */
public class Constants {
    public static final String LVS_PRIVATE_IP = "lvs.private.ip";
    public static final String EXECUTABLE_FILE_PATH = "executable.file.path";
    public static final String TEMPLATES_PATH = "templates.path";
    public static final String TEMPLATES_NAME = "templates.name";
    public static final String SCRIPTS_PATH = "scripts.path";
    public static final String CONF_FILE_PATH = "conf.file.path";
    public static final String STATS_SOCKET_FILE_PATH = "stats.socket.file.path";
    public static final String CEP_STATS_PUBLISHER_ENABLED = "cep.stats.publisher.enabled";
    public static final String THRIFT_RECEIVER_IP = "thrift.receiver.ip";
    public static final String THRIFT_RECEIVER_PORT = "thrift.receiver.port";
    public static final String NETWORK_PARTITION_ID = "network.partition.id";
    public static final String CLUSTER_ID = "cluster.id";
    public static final String SERVICE_NAME = "service.name";
	public static final String VIRTUALIPS_FOR_SERVICES = "lvs.service.virtualip.set" ;
	public static final String KEEPALIVED_START_COMMAND = "service keepalived restart";
	public static final String SERVER_STATE ="server.state" ;
	public static final String LVS_SCHEDULE_ALGO = "schedule.algorithm";
}
