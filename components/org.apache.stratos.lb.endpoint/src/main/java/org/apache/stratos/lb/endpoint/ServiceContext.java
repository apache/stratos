/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.lb.endpoint;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines service context properties.
 */
public class ServiceContext {
    private String serviceName;
    private Map<String, ClusterContext> clusterContextMap;

    public ServiceContext() {
        clusterContextMap = new HashMap<String, ClusterContext>();
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public Collection<ClusterContext> getClusterContexts() {
        return clusterContextMap.values();
    }

    public ClusterContext getClusterContext(String clusterId) {
        return clusterContextMap.get(clusterId);
    }

    public void addClusterContext(ClusterContext clusterContext) {
        clusterContextMap.put(clusterContext.getClusterId(), clusterContext);
    }
}
