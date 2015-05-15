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
package org.apache.stratos.autoscaler.context.cluster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * It holds the runtime data of a service cluster
 */
public class AbstractClusterContext implements Serializable {

    private static final Log log = LogFactory.getLog(AbstractClusterContext.class);


    // cluster id
    protected String clusterId;
    private String serviceId;
    protected Map<String, ClusterInstance> clusterInstanceMap;


    public AbstractClusterContext(String clusterId, String serviceId) {
        this.clusterId = clusterId;
        this.serviceId = serviceId;
        clusterInstanceMap = new ConcurrentHashMap<String, ClusterInstance>();
    }

    public String getServiceId() {
        return serviceId;
    }

    public Map<String, ClusterInstance> getClusterInstanceMap() {
        return clusterInstanceMap;
    }

    public void setClusterInstanceMap(Map<String, ClusterInstance> clusterInstanceMap) {
        this.clusterInstanceMap = clusterInstanceMap;
    }

    public void addClusterInstance(ClusterInstance instance) {
        this.clusterInstanceMap.put(instance.getInstanceId(), instance);
    }

    public ClusterInstance getClusterInstance(String instanceId) {
        return this.clusterInstanceMap.get(instanceId);
    }
}
