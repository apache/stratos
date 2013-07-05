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
package org.apache.stratos.lb.endpoint.util;

public class TopologyConstants {
    
    public static final String TOPIC_NAME = "cloud-controller-topology";
    public static final String MB_SERVER_URL = "mb.server.ip";
    public static final String DEFAULT_MB_SERVER_URL = "localhost:5672";
    
    public static final String TOPOLOGY_SYNC_CRON = "1 * * * * ? *";
	public static final String TOPOLOGY_SYNC_TASK_NAME = "TopologySubscriberTaskOfADC";
	public static final String TOPOLOGY_SYNC_TASK_TYPE = "TOPOLOGY_SUBSCRIBER_TASK";

}
