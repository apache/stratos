/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.messaging.event.topology;

import org.apache.stratos.messaging.domain.topology.KubernetesService;
import org.apache.stratos.messaging.event.Event;

import java.util.List;

/**
 * Cluster activated event will be sent by Autoscaler
 */
public class ClusterInstanceActivatedEvent extends Event {

    private final String serviceName;
    private final String clusterId;
    private String appId;
    private String instanceId;
    private List<KubernetesService> kubernetesServices;

    public ClusterInstanceActivatedEvent(String appId, String serviceName, String clusterId, String instanceId) {
        this.serviceName = serviceName;
        this.clusterId = clusterId;
        this.appId = appId;
        this.instanceId = instanceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return "ClusterActivatedEvent [serviceName=" + serviceName + ", clusterStatus=" +
                "]";
    }

    public String getClusterId() {
        return clusterId;
    }

    public String getAppId() {
        return appId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public List<KubernetesService> getKubernetesServices() {
        return kubernetesServices;
    }

    public void setKubernetesServices(List<KubernetesService> kubernetesServices) {
        this.kubernetesServices = kubernetesServices;
    }
}
