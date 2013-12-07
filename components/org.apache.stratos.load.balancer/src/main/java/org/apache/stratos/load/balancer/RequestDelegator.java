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

package org.apache.stratos.load.balancer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.algorithm.AlgorithmContext;
import org.apache.stratos.load.balancer.algorithm.LoadBalanceAlgorithm;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.util.ArrayList;

/**
 * Implements core load balancing logic for identifying the next member
 * according to the incoming request information.
 */
public class RequestDelegator {
    private static final Log log = LogFactory.getLog(RequestDelegator.class);

    private LoadBalanceAlgorithm algorithm;

    public RequestDelegator(LoadBalanceAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Member findNextMemberFromHostName(String hostName) {
        try {
            if (hostName == null)
                return null;

            TopologyManager.acquireReadLock();
            long startTime = System.currentTimeMillis();

            Cluster cluster = LoadBalancerContext.getInstance().getSingleTenantCluster(hostName);
            if (cluster != null) {
                Member member = findNextMemberInCluster(cluster);
                if (member != null) {
                    if (log.isDebugEnabled()) {
                        long endTime = System.currentTimeMillis();
                        log.debug(String.format("Next member identified in %dms: [service] %s [cluster] %s [member] %s", (endTime - startTime), member.getServiceName(), member.getClusterId(), member.getMemberId()));
                    }
                }
                return member;
            }
            return null;
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    public Member findNextMemberFromTenantId(String hostName, int tenantId) {
        try {
            TopologyManager.acquireReadLock();
            long startTime = System.currentTimeMillis();

            // Find cluster from host name and tenant id
            Cluster cluster = LoadBalancerContext.getInstance().getMultiTenantCluster(hostName, tenantId);
            if (cluster != null) {
                Member member = findNextMemberInCluster(cluster);
                if (member != null) {
                    if (log.isDebugEnabled()) {
                        long endTime = System.currentTimeMillis();
                        log.debug(String.format("Next member identified in %dms: [service] %s [cluster] %s [tenant-id] %d [member] %s",
                                (endTime - startTime), member.getServiceName(), member.getClusterId(), tenantId, member.getMemberId()));
                    }
                }
                return member;
            }
            return null;
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    private Member findNextMemberInCluster(Cluster cluster) {
        // Find algorithm context of the cluster
        ClusterContext clusterContext = LoadBalancerContext.getInstance().getClusterContext(cluster.getClusterId());
        if (clusterContext == null) {
            clusterContext = new ClusterContext(cluster.getServiceName(), cluster.getClusterId());
            LoadBalancerContext.getInstance().addClusterContext(clusterContext);
        }

        AlgorithmContext algorithmContext = clusterContext.getAlgorithmContext();
        if (algorithmContext == null) {
            algorithmContext = new AlgorithmContext(cluster.getServiceName(), cluster.getClusterId());
            clusterContext.setAlgorithmContext(algorithmContext);
        }
        algorithm.setMembers(new ArrayList<Member>(cluster.getMembers()));
        Member member = algorithm.getNextMember(algorithmContext);
        if (member == null) {
            if (log.isWarnEnabled()) {
                log.warn(String.format("Could not find a member in cluster: [service] %s [cluster] %s", cluster.getServiceName(), cluster.getClusterId()));
            }
        }
        return member;
    }

    public boolean isTargetHostValid(String hostName) {
        try {
            if (hostName == null)
                return false;

            TopologyManager.acquireReadLock();
            return LoadBalancerContext.getInstance().clusterExists(hostName);
        } finally {
            TopologyManager.releaseReadLock();
        }
    }
}
