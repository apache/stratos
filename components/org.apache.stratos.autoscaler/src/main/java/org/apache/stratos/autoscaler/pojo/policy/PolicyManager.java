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

package org.apache.stratos.autoscaler.pojo.policy;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.AutoScalingPolicyAlreadyExistException;
import org.apache.stratos.autoscaler.exception.application.InvalidApplicationPolicyException;
import org.apache.stratos.autoscaler.exception.policy.InvalidDeploymentPolicyException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.exception.policy.PolicyDoesNotExistException;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.common.services.DistributedObjectProvider;

import java.util.Collection;
import java.util.Map;

/**
 * Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {

    private static final Log log = LogFactory.getLog(PolicyManager.class);

    private static final String AS_POLICY_ID_TO_AUTOSCALE_POLICY_MAP = "AS_POLICY_ID_TO_AUTOSCALE_POLICY_MAP";
    private static final String APPLICATION_ID_TO_APPLICATION_POLICY_MAP = "APPLICATION_ID_TO_APPLICATION_POLICY_MAP";
    private static final String DEPLOYMENT_POLICY_ID_TO_DEPLOYMENT_POLICY_MAP = "DEPLOYMENT_POLICY_ID_TO_DEPLOYMENT_POLICY_MAP";

    private static Map<String, AutoscalePolicy> autoscalePolicyListMap;

    private static Map<String, DeploymentPolicy> deploymentPolicyListMap;

    private static Map<String, ApplicationPolicy> applicationPolicyListMap;

    public Collection<DeploymentPolicy> getDeploymentPolicies() {
        return deploymentPolicyListMap.values();
    }

    /* An instance of a PolicyManager is created when the class is loaded.
     * Since the class is loaded only once, it is guaranteed that an object of
     * PolicyManager is created only once. Hence it is singleton.
     */

    private static class InstanceHolder {
        private static final PolicyManager INSTANCE = new PolicyManager();
    }

    public static PolicyManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    private PolicyManager() {
        // Initialize distributed object provider
        DistributedObjectProvider distributedObjectProvider = ServiceReferenceHolder.getInstance().getDistributedObjectProvider();
        autoscalePolicyListMap = distributedObjectProvider.getMap(AS_POLICY_ID_TO_AUTOSCALE_POLICY_MAP);
        deploymentPolicyListMap = distributedObjectProvider.getMap(DEPLOYMENT_POLICY_ID_TO_DEPLOYMENT_POLICY_MAP);
        applicationPolicyListMap = distributedObjectProvider.getMap(APPLICATION_ID_TO_APPLICATION_POLICY_MAP);
    }

    // Add the policy to information model and persist.
    public boolean addAutoscalePolicy(AutoscalePolicy policy) throws AutoScalingPolicyAlreadyExistException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Adding autoscaling policy: [id] %s", policy.getUuid()));
        }
        if (StringUtils.isEmpty(policy.getUuid())) {
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.addASPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is added successfully: [id] %s", policy.getUuid()));
        }
        return true;
    }

    public boolean updateAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if (StringUtils.isEmpty(policy.getUuid())) {
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.updateASPolicyInInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is updated successfully: [id] %s", policy.getUuid()));
        }
        return true;
    }

    public boolean removeAutoscalePolicy(String policyID) {
        if (StringUtils.isEmpty(policyID)) {
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.removeASPolicyInInformationModel(policyID);
        RegistryManager.getInstance().removeAutoscalerPolicy(policyID);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is removed successfully: [id] %s", policyID));
        }
        return true;
    }


    /**
     * Add deployment policy to in memory map and persist.
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void addDeploymentPolicy(DeploymentPolicy policy) {
        addDeploymentPolicyToPolicyListMap(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is added successfully: [deployment-policy-id] %s",
                    policy.getUuid()));
        }
    }

    /**
     * Retrieve deployment policies from registry and add it to in memory model
     *
     * @param deploymentPolicy
     * @throws InvalidDeploymentPolicyException
     */
    public void addDeploymentPolicyToInformationModel(DeploymentPolicy deploymentPolicy)
            throws InvalidDeploymentPolicyException {
        if (!deploymentPolicyListMap.containsKey(deploymentPolicy.getUuid())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding deployment policy: " + deploymentPolicy.getUuid());
            }
            deploymentPolicyListMap.put(deploymentPolicy.getUuid(), deploymentPolicy);
        } else {
            String errMsg = "Specified deployment policy [" +
                    deploymentPolicy.getUuid() + "] already exists";
            log.error(errMsg);
            throw new InvalidDeploymentPolicyException(errMsg);
        }
    }

    public boolean updateDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
        if (StringUtils.isEmpty(policy.getUuid())) {
            throw new AutoScalerException("Deployment policy id cannot be empty");
        }
        this.updateDeploymentPolicyInInformationModel(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is updated successfully: [id] %s", policy.getUuid()));
        }
        return true;
    }

    /**
     * Remove deployment policy from in memory map and registry.
     *
     * @param deploymentPolicyId Deployment policy Id
     * @throws InvalidPolicyException
     */
    public void removeDeploymentPolicy(String deploymentPolicyId) {
        removeDeploymentPolicyFromMap(deploymentPolicyId);
        RegistryManager.getInstance().removeDeploymentPolicy(deploymentPolicyId);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is removed successfully: [deployment-policy-id] %s",
                    deploymentPolicyId));
        }
    }

    public void addASPolicyToInformationModel(AutoscalePolicy asPolicy) throws AutoScalingPolicyAlreadyExistException {
        if (!autoscalePolicyListMap.containsKey(asPolicy.getUuid())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding autoscaling policy: " + asPolicy.getUuid());
            }
            autoscalePolicyListMap.put(asPolicy.getUuid(), asPolicy);
        } else {
            String errMsg = "Specified autoscaling policy [" + asPolicy.getUuid() + "] already exists";
            log.error(errMsg);
            throw new AutoScalingPolicyAlreadyExistException(errMsg);
        }
    }

    public void updateASPolicyInInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(asPolicy.getUuid())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating autoscaling policy: " + asPolicy.getUuid());
            }
            autoscalePolicyListMap.put(asPolicy.getUuid(), asPolicy);
        }
    }

    public void updateDeploymentPolicyInInformationModel(DeploymentPolicy deploymentPolicy) throws InvalidPolicyException {
        if (deploymentPolicyListMap.containsKey(deploymentPolicy.getUuid())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating deployment policy: " + deploymentPolicy.getUuid());
            }
            deploymentPolicyListMap.put(deploymentPolicy.getUuid(), deploymentPolicy);
        }
    }

    public void removeASPolicyInInformationModel(String policyID) throws PolicyDoesNotExistException {
        if (autoscalePolicyListMap.containsKey(policyID)) {
            if (log.isDebugEnabled()) {
                log.debug("Updating autoscaling policy: " + policyID);
            }
            autoscalePolicyListMap.remove(policyID);
        } else {
            throw new PolicyDoesNotExistException("No such policy ID [" + policyID + "] exists");
        }
    }

    public void removeApplicationPolicyInInformationModel(String applicationPolicyId) throws InvalidPolicyException {
        if (applicationPolicyListMap.containsKey(applicationPolicyId)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing application policy [application-policy-id] " + applicationPolicyId);
            }
            applicationPolicyListMap.remove(applicationPolicyId);
        } else {
            throw new InvalidPolicyException(String.format("Application policy not found: [application-policy-id] %s", applicationPolicyId));
        }
    }

    /**
     * Removes the specified policy
     *
     * @param policyId
     * @throws InvalidPolicyException
     */
    public void undeployAutoscalePolicy(String policyId) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(policyId)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing policy :" + policyId);
            }
            autoscalePolicyListMap.remove(policyId);
            RegistryManager.getInstance().removeAutoscalerPolicy(policyId);
        } else {
            throw new InvalidPolicyException("No such policy ID [" + policyId + "] exists");
        }
    }

    /**
     * Returns an array of the Autoscale policies contained in this manager.
     *
     * @return
     */
    public AutoscalePolicy[] getAutoscalePolicyList() {
        return autoscalePolicyListMap.values().toArray(new AutoscalePolicy[0]);
    }

    /**
     * Returns the autoscale policy to which the specified id is mapped or null
     *
     * @param uuid Autoscale policy UUID
     * @return
     */
    public AutoscalePolicy getAutoscalePolicy(String uuid) {
        return autoscalePolicyListMap.get(uuid);
    }

    /**
     * Returns the autoscale policy to which the specified id is mapped or null
     *
     * @param id Autoscle policy Id
     * @return
     */
    public AutoscalePolicy getAutoscalePolicyById(String id) {
        AutoscalePolicy autoscalePolicy = null;
        for (AutoscalePolicy autoscalePolicy1 : getAutoscalePolicyList()) {
            if (autoscalePolicy1.getId().equals(id)) {
                autoscalePolicy = autoscalePolicy1;
            }
        }
        return autoscalePolicy;
    }


    private void addDeploymentPolicyToPolicyListMap(DeploymentPolicy policy) {
        if (StringUtils.isEmpty(policy.getUuid())) {
            throw new RuntimeException("Application id is not found in the deployment policy");
        }
        if (!deploymentPolicyListMap.containsKey(policy.getUuid())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding deployment policy: [deployment-policy-id] " + policy.getUuid());
            }
            deploymentPolicyListMap.put(policy.getUuid(), policy);
        } else {
            String errMsg = "Deployment policy already exists: [deployment-policy-id] " + policy.getUuid();
            log.error(errMsg);
        }
    }

    private void removeDeploymentPolicyFromMap(String applicationId) {
        if (deploymentPolicyListMap.containsKey(applicationId)) {
            deploymentPolicyListMap.remove(applicationId);
        }
    }

    public void updateDeploymentPolicyToInformationModel(DeploymentPolicy policy) throws InvalidPolicyException {
        if (log.isDebugEnabled()) {
            log.debug("Updating deployment policy: " + policy.getUuid());
        }
        deploymentPolicyListMap.put(policy.getUuid(), policy);
    }

    /**
     * Removes the specified policy
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void undeployDeploymentPolicy(String policy) throws InvalidPolicyException {
        if (deploymentPolicyListMap.containsKey(policy)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing deployment policy :" + policy);
            }
            DeploymentPolicy depPolicy = this.getDeploymentPolicy(policy);
            // undeploy network partitions this deployment policy.
//            PartitionManager.getInstance().undeployNetworkPartitions(depPolicy);
            // undeploy the deployment policy.
            RegistryManager.getInstance().removeDeploymentPolicy(policy);
            // remove from the infromation model.
            deploymentPolicyListMap.remove(policy);
        } else {
            throw new InvalidPolicyException("No such policy [" + policy + "] exists");
        }
    }

    /**
     * Returns an array of the Deployment policies contained in this manager.
     *
     * @return
     */
    public DeploymentPolicy[] getDeploymentPolicyList() {
        return deploymentPolicyListMap.values().toArray(new DeploymentPolicy[0]);
    }

    /**
     * Returns the deployment policy to which the specified id is mapped or null
     *
     * @param uuid UUID of the deployment policy
     * @return
     */
    public DeploymentPolicy getDeploymentPolicy(String uuid) {
        return deploymentPolicyListMap.get(uuid);
    }

    /**
     * Returns the deployment policy to which the specified id is mapped or null
     *
     * @param id Id of the deployment policy
     * @return
     */
    public DeploymentPolicy getDeploymentPolicyById(String id) {
        DeploymentPolicy deploymentPolicy = null;
        for (DeploymentPolicy deploymentPolicy1 : getDeploymentPolicies()) {
            if (deploymentPolicy1.getId().equals(id)) {
                deploymentPolicy = deploymentPolicy1;
            }
        }
        return deploymentPolicy;
    }


    public void addApplicationPolicy(ApplicationPolicy applicationPolicy) throws InvalidApplicationPolicyException {
        String applicationPolicyId = applicationPolicy.getUuid();
        if (log.isInfoEnabled()) {
            log.info(String.format("Adding application policy : [application-policy-id] %s", applicationPolicyId));
        }
        this.addApplicationPolicyToInformationModel(applicationPolicy);
        RegistryManager.getInstance().persistApplicationPolicy(applicationPolicy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Application policy is added successfully: [application-policy-id] %s",
                            applicationPolicyId));
        }

    }

    public boolean removeApplicationPolicy(String applicationPolicyId) throws InvalidPolicyException {
        if (StringUtils.isEmpty(applicationPolicyId)) {
            throw new AutoScalerException("Application policy id cannot be empty");
        }
        this.removeApplicationPolicyInInformationModel(applicationPolicyId);
        RegistryManager.getInstance().removeApplicationPolicy(applicationPolicyId);
        if (log.isInfoEnabled()) {
            log.info(String.format("Application policy is removed successfully: [id] %s", applicationPolicyId));
        }
        return true;
    }

    public void addApplicationPolicyToInformationModel(ApplicationPolicy applicationPolicy)
            throws InvalidApplicationPolicyException {
        String applicationPolicyId = applicationPolicy.getUuid();
        if (!applicationPolicyListMap.containsKey(applicationPolicyId)) {
            if (log.isDebugEnabled()) {
                log.debug("Adding application policy : " + applicationPolicyId);
            }
            applicationPolicyListMap.put(applicationPolicyId, applicationPolicy);
        } else {
            String errMsg = "Application policy already exists : " + applicationPolicyId;
            log.error(errMsg);
            throw new InvalidApplicationPolicyException(errMsg);
        }

    }

    /**
     * Retruns an ApplicationPolicy of a given application
     *
     * @param applicationPolicyId Application policy Id
     * @return
     */
    public ApplicationPolicy getApplicationPolicy(String applicationPolicyId) {
        return applicationPolicyListMap.get(applicationPolicyId);
    }

    /**
     * Returns the application policy to which the specified id is mapped or null
     *
     * @param uuid Id of the deployment policy
     * @return
     */
    public ApplicationPolicy getApplicationPolicyByUuid(String uuid) {
        ApplicationPolicy applicationPolicy = null;
        for (ApplicationPolicy applicationPolicy1 : getApplicationPolicies()) {
            if (applicationPolicy1.getUuid().equals(uuid)) {
                applicationPolicy = applicationPolicy1;
            }
        }
        return applicationPolicy;
    }

    public void updateApplicationPolicyInInformationModel(ApplicationPolicy applicationPolicy) {
        if (applicationPolicyListMap.containsKey(applicationPolicy.getUuid())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating application policy: " + applicationPolicy.getUuid());
            }
            applicationPolicyListMap.put(applicationPolicy.getUuid(), applicationPolicy);
        }
    }

    public boolean updateApplicationPolicy(ApplicationPolicy applicationPolicy) {
        if (StringUtils.isEmpty(applicationPolicy.getUuid())) {
            throw new AutoScalerException("Application policy id cannot be empty");
        }
        this.updateApplicationPolicyInInformationModel(applicationPolicy);
        RegistryManager.getInstance().persistApplicationPolicy(applicationPolicy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Application policy is updated successfully: [id] %s", applicationPolicy.getUuid()));
        }
        return true;
    }

    public ApplicationPolicy[] getApplicationPolicies() {
        return applicationPolicyListMap.values().toArray(new ApplicationPolicy[0]);
    }
}
