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
import org.apache.stratos.autoscaler.exception.policy.InvalidDeploymentPolicyException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
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
    public boolean addAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Adding autoscaling policy: [id] %s", policy.getId()));
        }
        if (StringUtils.isEmpty(policy.getId())) {
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        this.addASPolicyToInformationModel(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is added successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    public boolean updateAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if (StringUtils.isEmpty(policy.getId())) {
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.updateASPolicyInInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is updated successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    public boolean removeAutoscalePolicy(String policyID) throws InvalidPolicyException {
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
                    policy.getDeploymentPolicyID()));
        }
    }

    /**
     * Retrieve deployment policies from registy and add it to in memory model
     *
     * @param deploymentPolicy
     * @throws InvalidDeploymentPolicyException
     */
    public void addDeploymentPolicyToInformationModel(DeploymentPolicy deploymentPolicy)
            throws InvalidDeploymentPolicyException {
        if (!deploymentPolicyListMap.containsKey(deploymentPolicy.getDeploymentPolicyID())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding deployment policy: " + deploymentPolicy.getDeploymentPolicyID());
            }
            deploymentPolicyListMap.put(deploymentPolicy.getDeploymentPolicyID(), deploymentPolicy);
        } else {
            String errMsg = "Specified deployment policy [" +
                    deploymentPolicy.getDeploymentPolicyID() + "] already exists";
            log.error(errMsg);
            throw new InvalidDeploymentPolicyException(errMsg);
        }
    }

    public boolean updateDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
        if (StringUtils.isEmpty(policy.getDeploymentPolicyID())) {
            throw new AutoScalerException("Deployment policy id cannot be empty");
        }
        this.updateDeploymentPolicyInInformationModel(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is updated successfully: [id] %s", policy.getDeploymentPolicyID()));
        }
        return true;
    }

    /**
     * Remove deployment policy from in memory map and registry.
     *
     * @param deploymentPolicyID
     * @throws InvalidPolicyException
     */
    public void removeDeploymentPolicy(String deploymentPolicyID) {
        removeDeploymentPolicyFromMap(deploymentPolicyID);
        RegistryManager.getInstance().removeDeploymentPolicy(deploymentPolicyID);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is removed successfully: [deployment-policy-id] %s",
                    deploymentPolicyID));
        }
    }

    public void addASPolicyToInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException {
        if (!autoscalePolicyListMap.containsKey(asPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding autoscaling policy: " + asPolicy.getId());
            }
            autoscalePolicyListMap.put(asPolicy.getId(), asPolicy);
        } else {
            String errMsg = "Specified autoscaling policy [" + asPolicy.getId() + "] already exists";
            log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }

    public void updateASPolicyInInformationModel(AutoscalePolicy asPolicy) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(asPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating autoscaling policy: " + asPolicy.getId());
            }
            autoscalePolicyListMap.put(asPolicy.getId(), asPolicy);
        }
    }

    public void updateDeploymentPolicyInInformationModel(DeploymentPolicy deploymentPolicy) throws InvalidPolicyException {
        if (deploymentPolicyListMap.containsKey(deploymentPolicy.getDeploymentPolicyID())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating deployment policy: " + deploymentPolicy.getDeploymentPolicyID());
            }
            deploymentPolicyListMap.put(deploymentPolicy.getDeploymentPolicyID(), deploymentPolicy);
        }
    }

    public void removeASPolicyInInformationModel(String policyID) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(policyID)) {
            if (log.isDebugEnabled()) {
                log.debug("Updating autoscaling policy: " + policyID);
            }
            autoscalePolicyListMap.remove(policyID);
        } else {
            throw new InvalidPolicyException("No such policy ID [" + policyID + "] exists");
        }
    }

    public void removeApplicationPolicyInInformationModel(String applicationId) throws InvalidPolicyException {
        if (applicationPolicyListMap.containsKey(applicationId)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing application policy [application-id] " + applicationId);
            }
            applicationPolicyListMap.remove(applicationId);
        } else {
            throw new InvalidPolicyException("No such application id [" + applicationId + "] exists");
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
     * @param id
     * @return
     */
    public AutoscalePolicy getAutoscalePolicy(String id) {
        return autoscalePolicyListMap.get(id);
    }


    private void addDeploymentPolicyToPolicyListMap(DeploymentPolicy policy) {
        if (StringUtils.isEmpty(policy.getDeploymentPolicyID())) {
            throw new RuntimeException("Application id is not found in the deployment policy");
        }
        if (!deploymentPolicyListMap.containsKey(policy.getDeploymentPolicyID())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding deployment policy: [deployment-policy-id] " + policy.getDeploymentPolicyID());
            }
            deploymentPolicyListMap.put(policy.getDeploymentPolicyID(), policy);
        } else {
            String errMsg = "Deployment policy already exists: [deployment-policy-id] " + policy.getDeploymentPolicyID();
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
            log.debug("Updating deployment policy: " + policy.getDeploymentPolicyID());
        }
        deploymentPolicyListMap.put(policy.getDeploymentPolicyID(), policy);
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
     * @param id
     * @return
     */
    public DeploymentPolicy getDeploymentPolicy(String id) {
        return deploymentPolicyListMap.get(id);
    }


    public void addApplicationPolicy(ApplicationPolicy applicationPolicy) throws InvalidPolicyException {
        String applicationPolicyId = applicationPolicy.getId();
        if (log.isInfoEnabled()) {
            log.info(String.format("Adding application policy : [application-policy-id] %s", applicationPolicyId));
        }
        this.addApplicationPolicyToInformationModel(applicationPolicy);
        RegistryManager.getInstance().persistApplicationPolicy(applicationPolicy);

        if (log.isInfoEnabled()) {
            log.info(String
                    .format("Application policy is added successfully: [application-policy-id] %s",
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

    public void addApplicationPolicyToInformationModel(ApplicationPolicy applicationPolicy) throws InvalidPolicyException {
        String applicationPolicyId = applicationPolicy.getId();
        if (!applicationPolicyListMap.containsKey(applicationPolicyId)) {
            if (log.isDebugEnabled()) {
                log.debug("Adding application policy : " + applicationPolicyId);
            }
            applicationPolicyListMap.put(applicationPolicyId, applicationPolicy);
        } else {
            String errMsg = "Application policy already exists : " + applicationPolicyId;
            log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }

    }

    /**
     * Retruns an ApplicationPolicy of a given application
     *
     * @param applicationPolicyId
     * @return
     */
    public ApplicationPolicy getApplicationPolicy(String applicationPolicyId) {
        return applicationPolicyListMap.get(applicationPolicyId);
    }

    public void updateApplicationPolicyInInformationModel(ApplicationPolicy applicationPolicy) {
        if (applicationPolicyListMap.containsKey(applicationPolicy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Updating application policy: " + applicationPolicy.getId());
            }
            applicationPolicyListMap.put(applicationPolicy.getId(), applicationPolicy);
        }
    }

    public boolean updateApplicationPolicy(ApplicationPolicy applicationPolicy) {
        if (StringUtils.isEmpty(applicationPolicy.getId())) {
            throw new AutoScalerException("Application policy id cannot be empty");
        }
        this.updateApplicationPolicyInInformationModel(applicationPolicy);
        RegistryManager.getInstance().persistApplicationPolicy(applicationPolicy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Application policy is updated successfully: [id] %s", applicationPolicy.getId()));
        }
        return true;
    }

    public ApplicationPolicy[] getApplicationPolicies() {
        return applicationPolicyListMap.values().toArray(new ApplicationPolicy[0]);
    }
}
