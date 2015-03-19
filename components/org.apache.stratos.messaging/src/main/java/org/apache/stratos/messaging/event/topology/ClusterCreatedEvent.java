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

import org.apache.stratos.messaging.domain.topology.Cluster;

import java.io.Serializable;

/**
 * This event is fired by Cloud Controller when a cluster is created for a service.
 */
public class ClusterCreatedEvent extends TopologyEvent implements Serializable {
    private static final long serialVersionUID = 2080623816272047762L;

    private final Cluster cluster;

    public ClusterCreatedEvent(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "ClusterCreatedEvent [serviceName=" + cluster.getServiceName() + ", " +
                "application=" + cluster.getAppId() + " , cluster= " + cluster.getClusterId() + " ]";
    }

    public Cluster getCluster() {
        return cluster;
    }
}
