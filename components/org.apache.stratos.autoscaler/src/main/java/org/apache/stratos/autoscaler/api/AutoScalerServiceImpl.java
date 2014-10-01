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
package org.apache.stratos.autoscaler.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.client.cloud.controller.CloudControllerClient;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.interfaces.AutoScalerServiceInterface;
import org.apache.stratos.autoscaler.kubernetes.KubernetesManager;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.apache.stratos.common.kubernetes.KubernetesHost;
import org.apache.stratos.common.kubernetes.KubernetesMaster;

import java.util.ArrayList;

/**
 * Auto Scaler Service API is responsible getting Partitions and Policies.
 */
public class AutoScalerServiceImpl implements AutoScalerServiceInterface {

    private static final Log log = LogFactory.getLog(AutoScalerServiceImpl.class);
    PartitionManager partitionManager = PartitionManager.getInstance();
    KubernetesManager kubernetesManager = KubernetesManager.getInstance();

    public Partition[] getAllAvailablePartitions() {
        return partitionManager.getAllPartitions();
    }

    public DeploymentPolicy[] getAllDeploymentPolicies() {
        return PolicyManager.getInstance().getDeploymentPolicyList();
    }

    public AutoscalePolicy[] getAllAutoScalingPolicy() {
        return PolicyManager.getInstance().getAutoscalePolicyList();
    }

    @Override
    public DeploymentPolicy[] getValidDeploymentPoliciesforCartridge(String cartridgeType) {
        ArrayList<DeploymentPolicy> validPolicies = new ArrayList<DeploymentPolicy>();

        for (DeploymentPolicy deploymentPolicy : this.getAllDeploymentPolicies()) {
            try {
                // call CC API
                CloudControllerClient.getInstance().validateDeploymentPolicy(cartridgeType, deploymentPolicy);
                // if this deployment policy is valid for this cartridge, add it.
                validPolicies.add(deploymentPolicy);
            } catch (PartitionValidationException ignoredException) {
                // if this policy doesn't valid for the given cartridge, add a debug log.
                if (log.isDebugEnabled()) {
                    log.debug("Deployment policy [id] " + deploymentPolicy.getId()
                            + " is not valid for Cartridge [type] " + cartridgeType, ignoredException);
                }
            }
        }
        return validPolicies.toArray(new DeploymentPolicy[0]);
    }

    @Override
    public boolean addPartition(Partition partition) throws InvalidPartitionException {
        return partitionManager.addNewPartition(partition);
    }

    @Override
    public boolean addDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException {
        return PolicyManager.getInstance().deployDeploymentPolicy(depPolicy);
    }

    @Override
    public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException {
        return PolicyManager.getInstance().deployAutoscalePolicy(aspolicy);
    }

    @Override
    public Partition getPartition(String partitionId) {
        return partitionManager.getPartitionById(partitionId);
    }

    @Override
    public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId) {
        return PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId);
    }

    @Override
    public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId) {
        return PolicyManager.getInstance().getAutoscalePolicy(autoscalingPolicyId);
    }

    @Override
    public PartitionGroup[] getPartitionGroups(String deploymentPolicyId) {
        return PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId).getPartitionGroups();
    }

    public Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId) {
        DeploymentPolicy depPol = this.getDeploymentPolicy(deploymentPolicyId);
        if (null == depPol) {
            return null;
        }

        return depPol.getAllPartitions();
    }

    @Override
    public KubernetesGroup[] getAllKubernetesGroups() {
        return kubernetesManager.getKubernetesGroups();
    }

    @Override
    public KubernetesGroup getKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        return kubernetesManager.getKubernetesGroup(kubernetesGroupId);
    }

    @Override
    public KubernetesMaster getMasterForKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        return kubernetesManager.getKubernetesMasterInGroup(kubernetesGroupId);
    }

    @Override
    public KubernetesHost[] getHostsForKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException {
        return kubernetesManager.getKubernetesHostsInGroup(kubernetesGroupId);
    }


    @Override
    public boolean addKubernetesGroup(KubernetesGroup kubernetesGroup) throws InvalidKubernetesGroupException {
        return kubernetesManager.addNewKubernetesGroup(kubernetesGroup);
    }

    @Override
    public boolean addKubernetesHost(String groupId, KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesGroupException {
        return kubernetesManager.addNewKubernetesHost(groupId, kubernetesHost);
    }

    @Override
    public boolean removeKubernetesGroup(String groupId) throws NonExistingKubernetesGroupException {
        return kubernetesManager.removeKubernetesGroup(groupId);
    }

    @Override
    public boolean removeKubernetesHost(String hostId) throws NonExistingKubernetesHostException {
        return kubernetesManager.removeKubernetesHost(hostId);
    }

    @Override
    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException, NonExistingKubernetesMasterException {
        return kubernetesManager.updateKubernetesMaster(kubernetesMaster);
    }

    @Override
    public boolean updateKubernetesHost(KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesHostException {
        return kubernetesManager.updateKubernetesHost(kubernetesHost);
    }

    @Override
    public Partition[] getPartitionsOfGroup(String deploymentPolicyId, String groupId) {
        DeploymentPolicy depPol = this.getDeploymentPolicy(deploymentPolicyId);
        if (null == depPol) {
            return null;
        }

        PartitionGroup group = depPol.getPartitionGroup(groupId);

        if (group == null) {
            return null;
        }

        return group.getPartitions();
    }

    public void checkLBExistenceAgainstPolicy(String lbClusterId, String deploymentPolicyId) throws NonExistingLBException {

        boolean exist = false;
        for (PartitionGroup partitionGroup : PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId).getPartitionGroups()) {

            NetworkPartitionLbHolder nwPartitionLbHolder = partitionManager.getNetworkPartitionLbHolder(partitionGroup.getId());

            if (nwPartitionLbHolder.isLBExist(lbClusterId)) {
                exist = true;
                break;
            }
        }

        if (!exist) {
            String msg = "LB with [cluster id] " + lbClusterId +
                    " does not exist in any network partition of [Deployment Policy] " + deploymentPolicyId;
            log.error(msg);
            throw new NonExistingLBException(msg);
        }
    }

    public boolean checkDefaultLBExistenceAgainstPolicy(String deploymentPolicyId) {

        for (PartitionGroup partitionGroup : PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId).getPartitionGroups()) {

            NetworkPartitionLbHolder nwPartitionLbHolder = partitionManager.getNetworkPartitionLbHolder(partitionGroup.getId());

            if (!nwPartitionLbHolder.isDefaultLBExist()) {
                if (log.isDebugEnabled()) {
                    log.debug("Default LB does not exist in [network partition] " +
                            nwPartitionLbHolder.getNetworkPartitionId() + " of [Deployment Policy] " +
                            deploymentPolicyId);

                }
                return false;
            }

        }

        return true;

    }

    public String getDefaultLBClusterId(String deploymentPolicyName) {
        if (log.isDebugEnabled()) {
            log.debug("Default LB Cluster Id for Deployment Policy [" + deploymentPolicyName + "] ");
        }
        for (PartitionGroup partitionGroup : PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyName).getPartitionGroups()) {

            NetworkPartitionLbHolder nwPartitionLbHolder = partitionManager.getNetworkPartitionLbHolder(partitionGroup.getId());

            if (nwPartitionLbHolder.isDefaultLBExist()) {
                if (log.isDebugEnabled()) {
                    log.debug("Default LB does not exist in [network partition] " +
                            nwPartitionLbHolder.getNetworkPartitionId() + " of [Deployment Policy] " +
                            deploymentPolicyName);

                }
                return nwPartitionLbHolder.getDefaultLbClusterId();
            }

        }

        return null;
    }

    public boolean checkServiceLBExistenceAgainstPolicy(String serviceName, String deploymentPolicyId) {

        for (PartitionGroup partitionGroup : PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId).getPartitionGroups()) {

            NetworkPartitionLbHolder nwPartitionLbHolder = partitionManager.getNetworkPartitionLbHolder(partitionGroup.getId());

            if (!nwPartitionLbHolder.isServiceLBExist(serviceName)) {
                if (log.isDebugEnabled()) {
                    log.debug("Service LB [service name] " + serviceName + " does not exist in [network partition] " +
                            nwPartitionLbHolder.getNetworkPartitionId() + " of [Deployment Policy] " +
                            deploymentPolicyId);

                }
                return false;
            }

        }

        return true;

    }

    public String getServiceLBClusterId(String serviceType, String deploymentPolicyName) {

        for (PartitionGroup partitionGroup : PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyName).getPartitionGroups()) {

            NetworkPartitionLbHolder nwPartitionLbHolder = partitionManager.getNetworkPartitionLbHolder(partitionGroup.getId());

            if (nwPartitionLbHolder.isServiceLBExist(serviceType)) {
                if (log.isDebugEnabled()) {
                    log.debug("Service LB [service name] " + serviceType + " does not exist in [network partition] " +
                            nwPartitionLbHolder.getNetworkPartitionId() + " of [Deployment Policy] " +
                            deploymentPolicyName);

                }
                return nwPartitionLbHolder.getLBClusterIdOfService(serviceType);
            }

        }

        return null;
    }

    public boolean checkClusterLBExistenceAgainstPolicy(String clusterId, String deploymentPolicyId) {

        for (PartitionGroup partitionGroup : PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyId).getPartitionGroups()) {

            NetworkPartitionLbHolder nwPartitionLbHolder = partitionManager.getNetworkPartitionLbHolder(partitionGroup.getId());

            if (!nwPartitionLbHolder.isClusterLBExist(clusterId)) {
                if (log.isDebugEnabled()) {
                    log.debug("Cluster LB [cluster id] " + clusterId + " does not exist in [network partition] " +
                            nwPartitionLbHolder.getNetworkPartitionId() + " of [Deployment Policy] " +
                            deploymentPolicyId);

                }
                return false;
            }

        }

        return true;

    }


}
