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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.InvalidPartitionException;
import org.apache.stratos.autoscaler.exception.InvalidPolicyException;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;

/**
 * Manager class for the purpose of managing Autoscale/Deployment policy definitions.
 */
public class PolicyManager {

    private static final Log log = LogFactory.getLog(PolicyManager.class);

    private static final String asResourcePath = AutoScalerConstants.AUTOSCALER_RESOURCE + AutoScalerConstants.AS_POLICY_RESOURCE + "/";

    private static Map<String, AutoscalePolicy> autoscalePolicyListMap = new HashMap<String, AutoscalePolicy>();

    private static Map<String, DeploymentPolicy> deploymentPolicyListMap = new HashMap<String, DeploymentPolicy>();

    private static PolicyManager instance = null;

    private PolicyManager() {
    }

    public static PolicyManager getInstance() {
        if (instance == null) {
            synchronized (PolicyManager.class) {
                if (instance == null) {
                    instance = new PolicyManager();
                }
            }
        }
        return instance;
    }

    // Add the policy to information model and persist.
    public boolean deployAutoscalePolicy(AutoscalePolicy policy) throws InvalidPolicyException {
        this.addASPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistAutoscalerPolicy(policy);
        if (log.isInfoEnabled()) {
            log.info(String.format("AutoScaling policy is deployed successfully: [id] %s", policy.getId()));
        }
        return true;
    }

    // Add the deployment policy to information model and persist.
    public boolean deployDeploymentPolicy(DeploymentPolicy policy) throws InvalidPolicyException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Deploying deployment policy: [id] %s", policy.getId()));
            }
            fillPartitions(policy);
        } catch (InvalidPartitionException e) {
            throw new InvalidPolicyException(String.format("Deployment policy is invalid: [id] %s", policy.getId()), e);
        }

        this.addDeploymentPolicyToInformationModel(policy);
        RegistryManager.getInstance().persistDeploymentPolicy(policy);

        if (log.isInfoEnabled()) {
            log.info(String.format("Deployment policy is deployed successfully: [id] %s", policy.getId()));
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
            fillPartition(partition, PartitionManager.getInstance().getPartitionById(deploymentPolicy.getId()));
        }
    }

    private static void fillPartition(Partition destPartition, Partition srcPartition) {
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
                log.debug("Adding policy :" + asPolicy.getId());
            }
            autoscalePolicyListMap.put(asPolicy.getId(), asPolicy);
        } else {
            throw new InvalidPolicyException("Specified policy [" + asPolicy.getId()
                    + "] already exists");
        }
    }

    /**
     * Removes the specified policy
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void removeAutoscalePolicy(String policy) throws InvalidPolicyException {
        if (autoscalePolicyListMap.containsKey(policy)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing policy :" + policy);
            }
            autoscalePolicyListMap.remove(policy);
        } else {
            throw new InvalidPolicyException("No such policy [" + policy + "] exists");
        }
    }

    /**
     * Returns a List of the Autoscale policies contained in this manager.
     *
     * @return
     */
    public List<AutoscalePolicy> getAutoscalePolicyList() {
        return Collections.unmodifiableList(new ArrayList<AutoscalePolicy>(autoscalePolicyListMap.values()));
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
                log.debug("Adding policy :" + policy.getId());
            }
            PartitionManager.getInstance().deployNewNetworkPartitions(policy);
            deploymentPolicyListMap.put(policy.getId(), policy);
        } else {
            throw new InvalidPolicyException("Specified policy [" + policy.getId()
                    + "] already exists");
        }
    }

    /**
     * Removes the specified policy
     *
     * @param policy
     * @throws InvalidPolicyException
     */
    public void removeDeploymentPolicy(String policy) throws InvalidPolicyException {
        if (deploymentPolicyListMap.containsKey(policy)) {
            if (log.isDebugEnabled()) {
                log.debug("Removing policy :" + policy);
            }
            deploymentPolicyListMap.remove(policy);
        } else {
            throw new InvalidPolicyException("No such policy [" + policy + "] exists");
        }
    }

    /**
     * Returns a List of the Deployment policies contained in this manager.
     *
     * @return
     */
    public List<DeploymentPolicy> getDeploymentPolicyList() {
        return Collections.unmodifiableList(new ArrayList<DeploymentPolicy>(deploymentPolicyListMap.values()));
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
