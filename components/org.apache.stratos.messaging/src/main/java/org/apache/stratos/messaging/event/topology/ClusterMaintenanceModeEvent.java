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

package org.apache.stratos.messaging.event.topology;

import org.apache.stratos.messaging.domain.topology.ClusterStatus;

import java.io.Serializable;

public class ClusterMaintenanceModeEvent extends TopologyEvent implements Serializable {

	private final String serviceName;
	private final String clusterId;
    private ClusterStatus status;

    public ClusterMaintenanceModeEvent(String serviceName, String clusterId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return "ClusterMaintenanceModeEvent [serviceName=" + serviceName + ", clusterStatus=" +
                status.toString() + "]";
    }

    public String getClusterId() {
        return clusterId;
    }

    public ClusterStatus getStatus() {
        return status;
    }

    public void setStatus(ClusterStatus status) {
        this.status = status;
    }
}
