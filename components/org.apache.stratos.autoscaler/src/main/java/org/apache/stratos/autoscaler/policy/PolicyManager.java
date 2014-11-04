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

package org.apache.stratos.autoscaler.policy;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;

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
    public boolean deployAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Autoscaling policy id cannot be empty");
        }
        this.addASPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("Autoscaling policy is deployed successfully: [id] %s", policy.getId()));
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
    public boolean deployDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Deploying policy id cannot be empty");
        }
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Deploying deployment policy: [id] %s", policy.getId()));
            }
            fillPartitions(policy);
        } catch (InvalidPartitionException e) {
        	log.error(e);
            throw new InvalidPolicyException(String.format("Deployment policy is invalid: [id] %s", policy.getId()), e);
        }

        addDeploymentPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is deployed successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    public boolean updateDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
        if(StringUtils.isEmpty(policy.getId())){
            throw new AutoScalerException("Deploying policy id cannot be empty");
        }
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Updating deployment policy: [id] %s", policy.getId()));
            }
            fillPartitions(policy);
        } catch (InvalidPartitionException e) {
            log.error(e);
            throw new InvalidPolicyException(String.format("Deployment policy is invalid: [id] %s", policy.getId()), e);
        }

        updateDeploymentPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is updated successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    private void fillPartitions(DeploymentPolicy deploymentPolicy) throws InvalidPartitionException {
        PartitionManager partitionMgr = PartitionManager.getInstance();
        for (Partition partition : deploymentPolicy.getAllPartitions()) {
            String partitionId = partition.getId();
            if ((partitionId == null) || (!partitionMgr.partitionExist(partitionId))) {
                String msg = "Could not find partition: [id] " + partitionId + ". " +
                        "Please deploy the partitions before deploying the deployment policies.";                
                throw new InvalidPartitionException(msg);
            }
            
            fillPartition(partition, PartitionManager.getInstance().getPartitionById(partitionId));
        }
    }

    private static void fillPartition(Partition destPartition, Partition srcPartition) {
        if(srcPartition.getProvider() == null)        	
            throw new RuntimeException("Provider is not set in the deployed partition");

        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting provider for partition: [id] %s [provider] %s", destPartition.getId(), srcPartition.getProvider()));
        }
        destPartition.setProvider(srcPartition.getProvider());

        if (log.isDebugEnabled()) {
            log.debug(String.format("Setting properties for partition: [id] %s [properties] %s", destPartition.getId(), srcPartition.getProperties()));
        }
        destPartition.setProperties(srcPartition.getProperties());
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
        if (!deploymentPolicyListMap.containsKey(policy.getId())) {
            if (log.isDebugEnabled()) {
                log.debug("Adding deployment policy: " + policy.getId());
            }
            PartitionManager.getInstance().deployNewNetworkPartitions(policy);
            deploymentPolicyListMap.put(policy.getId(), policy);
        } else {
        	String errMsg = "Specified deployment policy [" + policy.getId()+ "] already exists";
        	log.error(errMsg);
            throw new InvalidPolicyException(errMsg);
        }
    }

    public void updateDeploymentPolicyToInformationModel(DeploymentPolicy policy) throws InvalidPolicyException {
        if (log.isDebugEnabled()) {
            log.debug("Updating deployment policy: " + policy.getId());
        }
        deploymentPolicyListMap.put(policy.getId(), policy);
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
            PartitionManager.getInstance().undeployNetworkPartitions(depPolicy);
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

}
