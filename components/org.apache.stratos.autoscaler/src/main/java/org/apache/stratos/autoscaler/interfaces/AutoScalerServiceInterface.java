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
import org.apache.stratos.autoscaler.exception.kubernetes.InvalidServiceGroupException;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.common.Properties;

public interface AutoScalerServiceInterface {
    /**
     * Add autoscaling policy
     * @param autoscalePolicy
     * @return
     * @throws InvalidPolicyException
     */
    public boolean addAutoScalingPolicy(AutoscalePolicy autoscalePolicy) throws InvalidPolicyException;

    /**
     * Get an autoscaling policy
     * @param autoscalingPolicyId
     * @return
     */
    public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId);

    /**
     * Get autoscaling policies
     * @return
     */
    public AutoscalePolicy[] getAutoScalingPolicies();

    /**
     * Update an autoscaling policy
     * @param autoscalePolicy
     * @return
     * @throws InvalidPolicyException
     */
    public boolean updateAutoScalingPolicy(AutoscalePolicy autoscalePolicy) throws InvalidPolicyException;

    /**
     * Add an application
     *
     * @param applicationContext {@link org.apache.stratos.autoscaler.applications.pojo.ApplicationContext}
     * @throws ApplicationDefinitionException if an error occurs
     */
    public void addApplication(ApplicationContext applicationContext) throws ApplicationDefinitionException;

    /**
     * Get an application
     * @param applicationId
     */
    public ApplicationContext getApplication(String applicationId);

    /**
     * Get all applications
     */
    public ApplicationContext[] getApplications();

    /**
     * Deploy an application in created state
     * @param applicationId
     * @param deploymentPolicy
     * @return
     */
    public boolean deployApplication(String applicationId, DeploymentPolicy deploymentPolicy) throws ApplicationDefinitionException;

    /**
     * Undeploy an application in deployed state
     * @param applicationId
     * @return
     */
    public void undeployApplication(String applicationId);

    /**
     * Delete an application
     * @param applicationId
     */
    public void deleteApplication(String applicationId);

    /**
     * Returns a deployment policy of an application
     * @param applicationId
     * @return
     */
    public DeploymentPolicy getDeploymentPolicy(String applicationId);

    /**
     * Returns network partitions of a deployment policy
     * @param deploymentPolicyId
     * @return
     */
    public org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ApplicationLevelNetworkPartition[] getNetworkPartitions(String deploymentPolicyId);

    /**
     * Dynamically update the properties of an Autoscaling Cluster Monitor
     * @param clusterId id of the cluster.
     * @param properties updated properties.
     */
    void updateClusterMonitor(String clusterId, Properties properties) throws InvalidArgumentException;

    /**
     * Add a service group
     * @param servicegroup
     * @throws InvalidServiceGroupException
     */
    public void addServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException;

    /**
     * Remove a service group
     * @param groupName
     */
    public void removeServiceGroup(String groupName);

    /**
     * Get service group
     * @param name
     * @return
     */
    public ServiceGroup getServiceGroup(String name);
}
