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

import org.apache.commons.lang.NotImplementedException;
import org.apache.stratos.lb.endpoint.algorithm.AlgorithmContext;
import org.apache.stratos.lb.endpoint.algorithm.LoadBalanceAlgorithm;
import org.apache.stratos.lb.endpoint.topology.TopologyManager;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Port;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.synapse.MessageContext;
import org.apache.synapse.SynapseException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implements core load balancing logic.
 */
public class RequestProcessor {
    private LoadBalanceAlgorithm algorithm;

    public RequestProcessor(LoadBalanceAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Member findNextMember(String targetHost) {

        try {
            if(targetHost == null)
                return null;

            TopologyManager.acquireReadLock();

            Cluster cluster = findCluster(targetHost);
            if(cluster != null) {
                // Find algorithm context of the cluster
                ClusterContext clusterContext = LoadBalancerContext.getInstance().getClusterContext(cluster.getClusterId());
                if(clusterContext == null) {
                    clusterContext = new ClusterContext(cluster.getServiceName(), cluster.getClusterId());
                    LoadBalancerContext.getInstance().addClusterContext(clusterContext);
                }

                AlgorithmContext algorithmContext = clusterContext.getAlgorithmContext();
                if(algorithmContext == null) {
                    algorithmContext = new AlgorithmContext(cluster.getServiceName(), cluster.getClusterId());
                    clusterContext.setAlgorithmContext(algorithmContext);
                }
                algorithm.setMembers(new ArrayList<Member>(cluster.getMembers()));
                return algorithm.getNextMember(algorithmContext);
            }
            return null;
        }
        finally {
            TopologyManager.releaseReadLock();
        }
    }

    public Member findNextMember(String serviceName, int tenantId, String targetHost) {
        throw new NotImplementedException();
    }

    private Service findService(String serviceName) {
        Collection<Service> services = TopologyManager.getTopology().getServices();
        for (Service service : services) {
            if(service.getServiceName().equals(serviceName))
                return service;
        }
        return null;
    }

    private Cluster findCluster(String targetHost) {
        Collection<Service> services = TopologyManager.getTopology().getServices();
        for (Service service : services) {
            for (Cluster cluster : service.getClusters()) {
                if (targetHost.equals(cluster.getHostName())) {
                    return cluster;
                }
            }
        }
        return null;
    }

}
