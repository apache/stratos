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

package org.apache.stratos.messaging.event.cluster.status;

import org.apache.stratos.messaging.event.Event;

/**
 * This event is fired by cartridge agent when it has started the server and
 * applications are ready to serve the incoming requests.
 */
public class ClusterStatusClusterInstanceCreatedEvent extends Event {
    private static final long serialVersionUID = 2625412714611885089L;

    private final String clusterId;
    private String instanceId;
    private String alias;
    private String serviceName;

    public ClusterStatusClusterInstanceCreatedEvent(String alias, String serviceName,
                                                    String clusterId, String instanceId) {
        this.clusterId = clusterId;
        this.instanceId = instanceId;
        this.alias = alias;
        this.serviceName = serviceName;
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getAlias() {
        return alias;
    }

    public String getServiceName() {
        return serviceName;
    }
}
