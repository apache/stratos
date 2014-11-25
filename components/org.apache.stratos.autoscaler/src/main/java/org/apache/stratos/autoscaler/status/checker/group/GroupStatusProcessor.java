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
package org.apache.stratos.autoscaler.status.checker.group;

import org.apache.stratos.autoscaler.status.checker.StatusProcessor;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.applications.GroupStatus;
import org.apache.stratos.messaging.domain.instance.context.ClusterInstanceContext;
import org.apache.stratos.messaging.domain.instance.context.GroupInstanceContext;
import org.apache.stratos.messaging.domain.instance.context.InstanceContext;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.ClusterStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.List;
import java.util.Map;

/**
 * Created by reka on 11/25/14.
 */
public abstract class GroupStatusProcessor extends StatusProcessor {

    /**
     * Message processing and delegating logic.
     * @param idOfChild real message body.
     * @param instanceId Object that will get updated.
     * @return whether the processing was successful or not.
     */
    public abstract boolean process(String idOfChild, String idOfComponent, String appId,
                                                                                String instanceId);

    /**
     * Find out whether all the groups of a group in the same state or not
     *
     * @param groups groups of a group/application
     * @param status the state to check in all groups
     * @return whether groups in the given state or not
     */
    protected boolean getAllGroupInSameState(Map<String, Group> groups, GroupStatus status, String instanceId) {
        boolean groupStat = false;
        for (Group group : groups.values()) {
            GroupInstanceContext context = group.getInstanceContexts(instanceId);
            if(context != null) {
                if(context.getStatus() == status) {
                    groupStat = true;
                } else {
                    groupStat = false;
                    return groupStat;
                }
            } else {
                //Checking the minimum of the group instances to be satisfied
                List<InstanceContext> contexts = group.getInstanceContextsWithParentId(instanceId);
                int minGroupInstances = group.getComponentDeploymentPolicy().getMin();
                int sameStateInstances = 0;
                for(InstanceContext context1 : contexts) {
                   if(((GroupInstanceContext)context1).getStatus().equals(status)) {
                       sameStateInstances++;
                   }
                }
                if(sameStateInstances >= minGroupInstances) {
                    groupStat = true;
                } else {
                    groupStat = false;
                    return groupStat;
                }

            }
        }
        return groupStat;
    }

    /**
     * Find out whether all the clusters of a group are in the same state
     *
     * @param clusterData clusters of the group
     * @param status      the status to check of the group
     * @return whether all groups in the same state or not
     */
    protected boolean getAllClusterInSameState(Map<String, ClusterDataHolder> clusterData,
                                             ClusterStatus status, String instanceId) {
        boolean clusterStat = false;
        for (Map.Entry<String, ClusterDataHolder> clusterDataHolderEntry : clusterData.entrySet()) {
            String serviceName = clusterDataHolderEntry.getValue().getServiceType();
            String clusterId = clusterDataHolderEntry.getValue().getClusterId();
            TopologyManager.acquireReadLockForCluster(serviceName, clusterId);
            try {
                Service service = TopologyManager.getTopology().getService(serviceName);
                Cluster cluster = service.getCluster(clusterId);
                ClusterInstanceContext context = cluster.getInstanceContexts(instanceId);
                if (context.getStatus() == status) {
                    clusterStat = true;
                } else {
                    clusterStat = false;
                    return clusterStat;
                }
            } finally {
                TopologyManager.releaseReadLockForCluster(serviceName, clusterId);
            }

        }
        return clusterStat;
    }

}
