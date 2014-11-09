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
package org.apache.stratos.autoscaler;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.TerminationException;
import org.apache.stratos.autoscaler.policy.model.*;
import org.apache.stratos.autoscaler.status.checker.StatusChecker;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.cloud.controller.stub.pojo.MemberContext;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.health.stat.*;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/*
 * It holds the runtime data of a VM cluster
 */
public class VMClusterContext extends AbstractClusterContext {

    private static final Log log = LogFactory.getLog(VMClusterContext.class);

    // Map<NetworkpartitionId, Network Partition Context>
    protected Map<String, NetworkPartitionContext> networkPartitionCtxts;
    protected DeploymentPolicy deploymentPolicy;
    protected AutoscalePolicy autoscalePolicy;

    public VMClusterContext(String clusterId, String serviceId, AutoscalePolicy autoscalePolicy, DeploymentPolicy deploymentPolicy,
                            Map<String, NetworkPartitionContext> networkPartitionCtxts) {

        super(clusterId, serviceId);
        this.deploymentPolicy = deploymentPolicy;
        this.networkPartitionCtxts = networkPartitionCtxts;
        this.autoscalePolicy = autoscalePolicy;

    }

    public Map<String, NetworkPartitionContext> getNetworkPartitionCtxts(){
        return networkPartitionCtxts;
    }

    public DeploymentPolicy getDeploymentPolicy() {
        return deploymentPolicy;
    }

    public void setDeploymentPolicy(DeploymentPolicy deploymentPolicy) {
        this.deploymentPolicy = deploymentPolicy;
    }

    public AutoscalePolicy getAutoscalePolicy() {
        return autoscalePolicy;
    }

    public void setAutoscalePolicy(AutoscalePolicy autoscalePolicy) {
        this.autoscalePolicy = autoscalePolicy;
    }

    public NetworkPartitionContext getNetworkPartitionCtxt(String networkPartitionId) {
        return networkPartitionCtxts.get(networkPartitionId);
    }

    public void setPartitionCtxt(Map<String, NetworkPartitionContext> partitionCtxt) {
        this.networkPartitionCtxts = partitionCtxt;
    }

    public boolean partitionCtxtAvailable(String partitionId) {
        return networkPartitionCtxts.containsKey(partitionId);
    }

    public void addNetworkPartitionCtxt(NetworkPartitionContext ctxt) {
        this.networkPartitionCtxts.put(ctxt.getId(), ctxt);
    }

    public NetworkPartitionContext getPartitionCtxt(String id) {
        return this.networkPartitionCtxts.get(id);
    }

    public NetworkPartitionContext getNetworkPartitionCtxt(Member member) {
        log.info("***** getNetworkPartitionCtxt " + member.getNetworkPartitionId());
        String networkPartitionId = member.getNetworkPartitionId();
        if (networkPartitionCtxts.containsKey(networkPartitionId)) {
            log.info("returnnig network partition context " + networkPartitionCtxts.get(networkPartitionId));
            return networkPartitionCtxts.get(networkPartitionId);
        }

        log.info("returning null getNetworkPartitionCtxt");
        return null;
    }

}
