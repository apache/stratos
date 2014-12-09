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
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.common.Properties;

public interface AutoScalerServiceInterface {

    public DeploymentPolicy[] getAllDeploymentPolicies();

    public String addDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException;
    
    public boolean deployDeploymentPolicy(DeploymentPolicy deploymentPolicy);

    public boolean undeployDeploymentPolicy(String deploymentPolicyName);

    public boolean updateDeploymentPolicy(DeploymentPolicy depPolicy) throws InvalidPolicyException;

    public AutoscalePolicy[] getAllAutoScalingPolicy();

    public boolean addAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException;

    public boolean updateAutoScalingPolicy(AutoscalePolicy aspolicy) throws InvalidPolicyException;

    public DeploymentPolicy[] getValidDeploymentPoliciesforCartridge(String cartridgeType);

    public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyId);

    public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId);

    public org.apache.stratos.autoscaler.pojo.policy.deployment.partition.network.ApplicationLevelNetworkPartition[] getNetworkPartitions(String deploymentPolicyId);

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
