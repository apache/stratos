/*
 *
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
 *
*/

package org.apache.stratos.autoscaler.interfaces;

import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.exception.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.NonExistingLBException;
import org.apache.stratos.autoscaler.partition.PartitionGroup;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.apache.stratos.common.kubernetes.KubernetesHost;
import org.apache.stratos.common.kubernetes.KubernetesMaster;

public interface AutoScalerServiceInterface {

    public Partition[] getAllAvailablePartitions();

    public boolean addPartition(Partition partition) throws InvalidPartitionException;

    public DeploymentPolicy[] getAllDeploymentPolicies();

    public boolean addDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException;

    public boolean updateDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException;

    public AutoscalePolicy[] getAllAutoScalingPolicy();

    public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException;

    public boolean updateAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException;

    public DeploymentPolicy[] getValidDeploymentPoliciesforCartridge(String cartridgeType);

    public Partition getPartition(String partitionId);

    public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId);

    public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId);

    public PartitionGroup[] getPartitionGroups(String deploymentPolicyId);

    public Partition[] getPartitionsOfGroup(String deploymentPolicyId, String partitionGroup);

    public Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId);

    /**
     * Retrieves registered Kubernetes Groups.
     */
    public KubernetesGroup[] getAllKubernetesGroups();

    /**
     * Retrieves Kubernetes Group for given Kubernetes Group ID.
     *
     * @param kubernetesGroupId
     */
    public KubernetesGroup getKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException;

    /**
     * Retrieves Kubernetes Master for given Kubernetes Group ID.
     *
     * @param kubernetesGroupId
     */
    public KubernetesMaster getMasterForKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException;

    /**
     * Retrieves Kubernetes Hosts for given Kubernetes Group ID.
     *
     * @param kubernetesGroupId
     */
    public KubernetesHost[] getHostsForKubernetesGroup(String kubernetesGroupId) throws NonExistingKubernetesGroupException;

    /**
     * Register a Kubernetes cluster.
     *
     * @param kubernetesGroup
     * @throws InvalidKubernetesGroupException
     */
    public boolean addKubernetesGroup(KubernetesGroup kubernetesGroup) throws InvalidKubernetesGroupException;

    /**
     * Add a Kubernetes host to a Kubernetes Group.
     *
     * @param groupId
     * @param kubernetesHost
     * @throws InvalidKubernetesHostException
     */
    public boolean addKubernetesHost(String groupId, KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesGroupException;

    /**
     * Update a Kubernetes host.
     *
     * @param kubernetesHost
     * @throws InvalidKubernetesHostException
     */
    public boolean updateKubernetesHost(KubernetesHost kubernetesHost) throws
            InvalidKubernetesHostException, NonExistingKubernetesHostException;

    /**
     * Remove a Kubernetes host.
     *
     * @param groupId
     * @throws NonExistingKubernetesGroupException
     */
    public boolean removeKubernetesGroup(String groupId) throws NonExistingKubernetesGroupException;

    /**
     * Update a Kubernetes host.
     *
     * @param hostId
     * @throws InvalidKubernetesHostException
     */
    public boolean removeKubernetesHost(String hostId) throws NonExistingKubernetesHostException;

    /**
     * Update a Kubernetes Master in a Kubernetes Group.
     *
     * @param kubernetesMaster
     * @throws NonExistingKubernetesMasterException
     */
    public boolean updateKubernetesMaster(KubernetesMaster kubernetesMaster)
            throws InvalidKubernetesMasterException, NonExistingKubernetesMasterException;

    /**
     * Check existence of a lb cluster in network partitions of a given policy.
     *
     * @param lbClusterId
     * @param deploymentPolicyId
     * @throws NonExistingLBException if the lb cluster id cannot be found in any of the network partitions.
     */
    public void checkLBExistenceAgainstPolicy(String lbClusterId, String deploymentPolicyId) throws NonExistingLBException;

    /**
     * Check the existence of default lb in network partitions of a given policy.
     *
     * @param deploymentPolicyId
     * @return true if the LB exists in all the network partitions of this policy,
     * false if a LB couldn't find even in one network partition.
     */
    public boolean checkDefaultLBExistenceAgainstPolicy(String deploymentPolicyId);

    /**
     * Check the existence of per cluster lb in network partitions of a given policy.
     *
     * @param clusterId          cluster id of the service cluster which requires a dedicated LB.
     * @param deploymentPolicyId
     * @return true if the cluster based LB exists in all the network partitions of this policy,
     * false if a LB couldn't find even in one network partition.
     */
    public boolean checkClusterLBExistenceAgainstPolicy(String clusterId, String deploymentPolicyId);

    /**
     * Check the existence of per service lb in network partitions of a given policy.
     *
     * @param serviceName        service name of the service cluster which requires a dedicated LB.
     * @param deploymentPolicyId
     * @return true if the service based LB exists in all the network partitions of this policy,
     * false if a LB couldn't find even in one network partition.
     */
    public boolean checkServiceLBExistenceAgainstPolicy(String serviceName, String deploymentPolicyId);

    public String getDefaultLBClusterId(String deploymentPolicyName);

    public String getServiceLBClusterId (String serviceType, String deploymentPolicyName);

    /**
     * Dynamically update the properties of an Autoscaling Cluster Monitor
     * @param clusterId id of the cluster.
     * @param properties updated properties.
     */
    void updateClusterMonitor(String clusterId, Properties properties) throws InvalidArgumentException;
    
    /**
     * deploys an Application Definition
     *
     * @param applicationContext {@link org.apache.stratos.autoscaler.applications.pojo.ApplicationContext} object
     * @throws ApplicationDefinitionException if an error is encountered
     */
    public void deployApplicationDefinition (ApplicationContext applicationContext) throws ApplicationDefinitionException;

    /**
     * undeploys an Application Definition
     *
     * @param applicationId Id of the Application to be undeployed
     * @throws ApplicationDefinitionException if an error is encountered
     */
    public void unDeployApplicationDefinition (String applicationId, int tenantId, String tenantDomain)
            throws ApplicationDefinitionException;


    /**
     * Get service group by name
     * @param name
     * @return
     */
    public ServiceGroup getServiceGroup(String name);
}
