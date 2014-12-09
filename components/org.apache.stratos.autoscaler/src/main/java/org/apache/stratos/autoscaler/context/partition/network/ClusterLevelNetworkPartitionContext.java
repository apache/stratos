/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.context.partition.network;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds runtime data of a network partition.
 *
 */
public class ClusterLevelNetworkPartitionContext extends NetworkPartitionContext implements Serializable {

    private static final Log log = LogFactory.getLog(ClusterLevelNetworkPartitionContext.class);
    private static final long serialVersionUID = 572769304374110159L;
    private final String id;

    private String partitionAlgorithm;
    private int min;

    private Map<String, ClusterInstanceContext> instanceIdToClusterInstanceContextMap;


    public ClusterLevelNetworkPartitionContext(String id, String partitionAlgorithm, int min) {
        this.id = id;
        this.partitionAlgorithm = partitionAlgorithm;
        this.min = min;
        setInstanceIdToClusterInstanceContextMap(new ConcurrentHashMap<String, ClusterInstanceContext>());

    }

    public ClusterLevelNetworkPartitionContext(String id) {
        this.id = id;
        setInstanceIdToClusterInstanceContextMap(new HashMap<String, ClusterInstanceContext>());
    }


    public int hashCode() {

        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.getId() == null) ? 0 : this.getId().hashCode());
        return result;

    }

    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ClusterLevelNetworkPartitionContext)) {
            return false;
        }
        final ClusterLevelNetworkPartitionContext other = (ClusterLevelNetworkPartitionContext) obj;
        if (this.getId() == null) {
            if (other.getId() != null) {
                return false;
            }
        } else if (!this.getId().equals(other.getId())) {
            return false;
        }
        return true;
    }


    public String getId() {
        return id;
    }

    public ClusterInstanceContext getClusterInstanceContext(String instanceId) {
        return this.getClusterInstanceContextMap().get(instanceId);
    }

    public void addClusterInstanceContext(ClusterInstanceContext clusterInstanceContext) {
        this.getClusterInstanceContextMap().put(clusterInstanceContext.getId(),
                clusterInstanceContext);
    }

    public Map<String, ClusterInstanceContext> getClusterInstanceContextMap() {
        return instanceIdToClusterInstanceContextMap;
    }

    public void setInstanceIdToClusterInstanceContextMap(
            Map<String, ClusterInstanceContext> instanceIdToClusterInstanceContextMap) {
        this.instanceIdToClusterInstanceContextMap = instanceIdToClusterInstanceContextMap;
    }

    public boolean containsClusterInstanceContext(String instanceId) {
        return this.instanceIdToClusterInstanceContextMap.containsKey(instanceId);
    }

    public int getMin() {
        return min;
    }

    public String getPartitionAlgorithm() {
        return partitionAlgorithm;
    }
}
