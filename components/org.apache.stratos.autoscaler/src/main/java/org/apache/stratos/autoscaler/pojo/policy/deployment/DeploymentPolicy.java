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

package org.apache.stratos.autoscaler.pojo.policy.deployment;


import org.apache.stratos.common.partition.NetworkPartitionRef;
import org.apache.stratos.common.partition.PartitionRef;

import java.io.Serializable;
import java.util.Arrays;

/**
 * The model class for Deployment-Policy definition.
 */
public class DeploymentPolicy implements Serializable {

    private static final long serialVersionUID = 5675507196284400099L;
    private String deploymentPolicyID;
    private NetworkPartitionRef[] networkPartitionRefs;

    public String getDeploymentPolicyID() {
        return deploymentPolicyID;
    }

    public void setDeploymentPolicyID(String deploymentPolicyID) {
        this.deploymentPolicyID = deploymentPolicyID;
    }

    public NetworkPartitionRef[] getNetworkPartitionRefs() {
        return networkPartitionRefs;
    }

    public void setNetworkPartitionRefs(NetworkPartitionRef[] networkPartitionRefs) {
        this.networkPartitionRefs = networkPartitionRefs;
    }

    /**
     * Get network partition reference object by network partition id.
     *
     * @param networkPartitionId
     * @return the {@link NetworkPartitionRef}
     */
    public NetworkPartitionRef getNetworkPartitionByNetworkPartitionId(String networkPartitionId) {
        if (networkPartitionRefs != null) {
            for (NetworkPartitionRef networkPartition : networkPartitionRefs) {
                if (networkPartition.getId().equals(networkPartitionId)) {
                    return networkPartition;
                }
            }
        }
        return null;
    }

    /**
     * Get partition references by network partition id.
     *
     * @param networkPartitionId
     * @return an array of {@link PartitionRef}
     */
    public PartitionRef[] getPartitionsByNetworkPartitionId(String networkPartitionId) {
        if (networkPartitionRefs != null) {
            for (NetworkPartitionRef networkPartition : networkPartitionRefs) {
                if (networkPartition.getId().equals(networkPartitionId)) {
                    return networkPartition.getPartitions();
                }
            }
        }
        return null;
    }

    public String toString() {
        return String.format("{ deployment-policy-id : %s, network-partitions : %s", deploymentPolicyID,
                Arrays.toString(networkPartitionRefs));
    }

}
