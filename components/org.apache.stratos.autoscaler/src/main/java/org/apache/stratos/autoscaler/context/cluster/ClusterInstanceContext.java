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
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.messaging.domain.topology.Member;

import java.util.Map;

/*
 * It holds the runtime data of a VM cluster
 */
public class ClusterInstanceContext {

    private static final Log log = LogFactory.getLog(ClusterInstanceContext.class);
    private final String clusterInstanceId;

    // Map<PartitionId, Partition Context>
    protected Map<String, ClusterLevelPartitionContext> partitionCtxts;
    public ClusterInstanceContext(String clusterInstanceId, String serviceId,
                                  Map<String, ClusterLevelPartitionContext> partitionCtxts) {

        this.clusterInstanceId = clusterInstanceId;

    }

    public Map<String, ClusterLevelPartitionContext> getPartitionCtxts(){
        return partitionCtxts;
    }

    public ClusterLevelPartitionContext getNetworkPartitionCtxt(String PartitionId) {
        return partitionCtxts.get(PartitionId);
    }

    public void setPartitionCtxt(Map<String, ClusterLevelPartitionContext> partitionCtxt) {
        this.partitionCtxts = partitionCtxt;
    }

    public boolean partitionCtxtAvailable(String partitionId) {
        return partitionCtxts.containsKey(partitionId);
    }

    public void addPartitionCtxt(ClusterLevelPartitionContext ctxt) {
        this.partitionCtxts.put(ctxt.getPartitionId(), ctxt);
    }

    public ClusterLevelPartitionContext getPartitionCtxt(String id) {
        return this.partitionCtxts.get(id);
    }

    public ClusterLevelPartitionContext getPartitionCtxt(Member member) {
        log.info("Getting [Partition] " + member.getPartitionId());
        String partitionId = member.getPartitionId();
        if (partitionCtxts.containsKey(partitionId)) {
            log.info("Returning partition context, of [partition] " + partitionCtxts.get(partitionId));
            return partitionCtxts.get(partitionId);
        }

        return null;
    }

}
