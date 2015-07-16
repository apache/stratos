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

package org.apache.stratos.autoscaler.services;

import org.apache.stratos.autoscaler.applications.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.exception.*;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.application.InvalidApplicationPolicyException;
import org.apache.stratos.autoscaler.exception.application.InvalidServiceGroupException;
import org.apache.stratos.autoscaler.exception.CartridgeNotFoundException;
import org.apache.stratos.autoscaler.exception.policy.*;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.common.Properties;

import java.rmi.RemoteException;

public interface AutoscalerService {
    /**
     * Add autoscaling policy
     *
     * @param autoscalePolicy
     * @return
     * @throws InvalidPolicyException
     */
    public boolean addAutoScalingPolicy(AutoscalePolicy autoscalePolicy) throws InvalidPolicyException,
            AutoScalingPolicyAlreadyExistException;

    /**
     * Get an autoscaling policy
     *
     * @param autoscalingPolicyUuid
     * @return
     */
    public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyUuid);

	/**
	 * Get an autoscaling policy
	 *
	 * @param autoscalingPolicyId
	 * @return
	 */
	public AutoscalePolicy getAutoscalingPolicyForTenant(String autoscalingPolicyId,int tenantId);

    /**
     * Get autoscaling policies
     *
     * @return
     */
    public AutoscalePolicy[] getAutoScalingPolicies();

    /**
     * Update an autoscaling policy
     *
     * @param autoscalePolicy
     * @return
     * @throws InvalidPolicyException
     */
    public boolean updateAutoScalingPolicy(AutoscalePolicy autoscalePolicy) throws InvalidPolicyException;

    /**
     * Remove autoscaling policy
     *
     * @param autoscalingPolicyId
     * @return
     * @throws InvalidPolicyException
     */
    public boolean removeAutoScalingPolicy(String autoscalingPolicyId) throws InvalidPolicyException, UnremovablePolicyException;

    /**
     * Add an application
     *
     * @param applicationContext {@link ApplicationContext}
     * @throws ApplicationDefinitionException if an error occurs
     */
    public boolean addApplication(ApplicationContext applicationContext)
            throws ApplicationDefinitionException, CartridgeGroupNotFoundException,
            CartridgeNotFoundException;

    /**
     * update an application
     *
     * @param applicationContext {@link org.apache.stratos.autoscaler.applications.pojo.ApplicationContext}
     * @throws ApplicationDefinitionException if an error occurs
     */
    public boolean updateApplication(ApplicationContext applicationContext)
            throws ApplicationDefinitionException, CartridgeGroupNotFoundException, CartridgeNotFoundException;


    /**
     * Get an application
     *
     * @param applicationId
     */
    public ApplicationContext getApplication(String applicationId);

    /**
     * Get an application
     *
     * @param applicationId
     * @param tenantId
     */
    public ApplicationContext getApplicationByTenant(String applicationId, int tenantId);

    /**
     * Check if the application exists
     *
     * @param applicationId
     * @return whether application exists or not
     */
    boolean existApplication(String applicationId,int tenantId);

    /**
     * Get all applications
     */
    public ApplicationContext[] getApplications();

    /**
     * Deploy an application in created state
     *
     * @param applicationId
     * @param applicationPolicyId
     * @return
     */
    public boolean deployApplication(String applicationId, String applicationPolicyId) throws ApplicationDefinitionException;

    /**
     * Undeploy an application in deployed state
     *
     * @param applicationId
     * @return
     */

    public boolean undeployApplication(String applicationId, boolean force);

    /**
     * Delete an application
     *
     * @param applicationId
     */
    public boolean deleteApplication(String applicationId);

    /**
     * Add application policy
     *
     * @param applicationPolicy the application policy to be added
     * @throws InvalidApplicationPolicyException
     * @throws RemoteException
     * @throws InvalidPolicyException
     * @throws ApplicationPolicyAlreadyExistsException
     */
    public boolean addApplicationPolicy(ApplicationPolicy applicationPolicy) throws RemoteException,
            InvalidApplicationPolicyException, InvalidPolicyException, ApplicationPolicyAlreadyExistsException;

    /**
     * Get application policy by application uuid
     *
     * @param applicationPolicyId the application policy id
     * @return {@link ApplicationPolicy} used by the given application
     */
    public ApplicationPolicy getApplicationPolicy(String applicationPolicyId);

	/**
	 *
	 * Get application policy by application id and tenant id
	 *
	 * @param applicationPolicyId
	 * @param tenantId
	 * @return
	 */

	public ApplicationPolicy getApplicationPolicyByTenant(String applicationPolicyId, int tenantId);

    /**
     * Remove application policy
     *
     * @param applicationPolicyId the application policy id
     * @throws InvalidPolicyException
     */
    public boolean removeApplicationPolicy(String applicationPolicyId) throws InvalidPolicyException;

    /**
     * Update the given application policy if exists
     *
     * @param applicationPolicy
     * @throws InvalidApplicationPolicyException
     * @throws RemoteException
     * @throws ApplicatioinPolicyNotExistsException
     */
    public boolean updateApplicationPolicy(ApplicationPolicy applicationPolicy)
            throws InvalidApplicationPolicyException, RemoteException, ApplicatioinPolicyNotExistsException;

    /**
     * Get all application policies
     *
     * @return
     */
    public ApplicationPolicy[] getApplicationPolicies();

    /**
     * Dynamically update the properties of an Autoscaling Cluster Monitor
     *
     * @param clusterId  id of the cluster.
     * @param properties updated properties.
     */
    boolean updateClusterMonitor(String clusterId, Properties properties) throws InvalidArgumentException;

    /**
     * Add a cartridge group
     *
     * @param servicegroup
     * @throws InvalidServiceGroupException
     */
    public boolean addServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException;

    /**
     * Update a cartridge group
     *
     * @param serviceGroup
     * @return
     * @throws InvalidServiceGroupException
     */
    public boolean updateServiceGroup(ServiceGroup serviceGroup) throws InvalidServiceGroupException;

    /**
     * Remove a cartridge group
     *
     * @param groupName
     */
    public boolean removeServiceGroup(String groupName) throws CartridgeGroupNotFoundException;

    /**
     * Get cartridge group
     *
     * @param name
     * @return
     */
    public ServiceGroup getServiceGroup(String name);

	/**
	 * Get cartridge group by tenant
	 * @param name
	 * @param tenantId
	 * @return
	 */
	public ServiceGroup getServiceGroupByTenant(String name, int tenantId);

    /**
     * Find cluster id of an application by subscription alias.
     *
     * @param applicationId
     * @param alias
     * @return
     */
    public String findClusterId(String applicationId, String alias);

    public String[] getApplicationNetworkPartitions(String applicationId) throws AutoScalerException;

    /**
     * Add a deployment policy
     *
     * @param deploymentPolicy DeployementPolicy
     * @throws InvalidDeploymentPolicyException if the deployment policy is not valid
     * @throws IllegalArgumentException         if the provided argument is not valid.
     */
    public boolean addDeployementPolicy(DeploymentPolicy deploymentPolicy) throws DeploymentPolicyAlreadyExistsException,
            InvalidDeploymentPolicyException, RemoteException, DeploymentPolicyNotExistsException;

    /**
     * Update existing deployment policy
     *
     * @param deploymentPolicy DeployementPolicy
     * @throws IllegalArgumentException if the provided argument is not valid.
     */
    public boolean updateDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws DeploymentPolicyNotExistsException,
            InvalidDeploymentPolicyException, RemoteException, InvalidPolicyException, CloudControllerConnectionException;

    /**
     * Remove deployment policy
     *
     * @param deploymentPolicyID deploymentPolicyID
     * @throws IllegalArgumentException if the provided argument is not valid.
     */
    public boolean removeDeployementPolicy(String deploymentPolicyID) throws DeploymentPolicyNotExistsException,
            DeploymentPolicyNotExistsException;

    /**
     * Get deployment policy definition
     *
     * @param deploymentPolicyUuid
     * @return
     */
    public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyUuid);

	/**
	 * Get deployment policy definition
	 *
	 * @param deploymentPolicyID
	 * @param tenantId
	 * @return
	 */
	public DeploymentPolicy getDeploymentPolicyForTenant(String deploymentPolicyID,int tenantId);

    /**
     * Get deployment policies
     *
     * @return array of {@link DeploymentPolicy}
     */
    public DeploymentPolicy[] getDeploymentPolicies();

    /**
     * Get applications by tenant
     *
     * @return array of {@link DeploymentPolicy}
     */
    public ApplicationContext[] getApplicationsByTenant(int tenantId);

    /**
     * Get deployment policy definition
     *
     * @param tenantId tenant id
     * @return Deployment policy
     */
    public DeploymentPolicy[] getDeploymentPoliciesByTenant(int tenantId);

}
