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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.partition.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
//import org.apache.stratos.autoscaler.pojo.policy.deployment.partition.PartitionManager;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.domain.Partition;

/**
 * Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {

    private static final Log log = LogFactory.getLog(PolicyManager.class);

    private static Map<String, AutoscalePolicy> autoscalePolicyListMap = new HashMap<String, AutoscalePolicy>();

    private static Map<String, DeploymentPolicy> deploymentPolicyListMap = new HashMap<String, DeploymentPolicy>();
    
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
    }

    // Add the policy to information model and persist.
    public boolean addAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Starting to add autoscaling policy: [id] %s", policy.getId()));
        }
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.addASPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is added successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    public boolean updateAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.updateASPolicyInInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is updated successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    // Add the deployment policy to information model and persist.
    public void addDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
        addDeploymentPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is added successfully: [application-id] %s",
                    policy.getApplicationId()));
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

    /**
     * Removes the specified policy
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void undeployAutoscalePolicy(String policy) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(policy)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing policy :" + policy);
            }
            autoscalePolicyListMap.remove(policy);
            RegistryManager.getInstance().removeAutoscalerPolicy(this.getAutoscalePolicy(policy));
        } else {
            throw new InvalidPolicyException("No such policy [" + policy + "] exists");
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

    // Add the deployment policy to As in memmory information model. Does not persist.
    public void addDeploymentPolicyToInformationModel(DeploymentPolicy policy) throws InvalidPolicyException {
        if (StringUtils.isEmpty(policy.getApplicationId())) {
            throw new RuntimeException("Application id is not defined in deployment policy");
        }
        if (!deploymentPolicyListMap.containsKey(policy.getApplicationId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding deployment policy: " + policy.getApplicationId());
            }
            deploymentPolicyListMap.put(policy.getApplicationId(), policy);
        } else {
        	String errMsg = "Specified deployment policy [" + policy.getApplicationId()+ "] already exists";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }

    public void updateDeploymentPolicyToInformationModel(DeploymentPolicy policy) throws InvalidPolicyException {
        if (log.isDebugEnabled()) {
            log.debug("Updating deployment policy: " + policy.getApplicationId());
        }
        deploymentPolicyListMap.put(policy.getApplicationId(), policy);
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
            RegistryManager.getInstance().removeDeploymentPolicy(depPolicy);
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

    public DeploymentPolicy getDeploymentPolicyByApplication(String appId) {
        for(DeploymentPolicy deploymentPolicy : deploymentPolicyListMap.values()) {
            if(deploymentPolicy.getApplicationId().equals(appId)) {
                return deploymentPolicy;
            }
        }
        return null;
    }

    public String getDeploymentPolicyIdByApplication(String appId) {
        for(Map.Entry<String, DeploymentPolicy> entry : deploymentPolicyListMap.entrySet()) {
            if(entry.getValue().getApplicationId().equals(appId)) {
                return entry.getKey();
            }
        }
        return null;
    }

}
