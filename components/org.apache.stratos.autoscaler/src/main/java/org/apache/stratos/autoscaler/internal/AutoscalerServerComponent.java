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
package org.apache.stratos.autoscaler.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.NetworkPartitionLbHolder;
import org.apache.stratos.autoscaler.applications.ApplicationSynchronizerTaskScheduler;
import org.apache.stratos.autoscaler.policy.model.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.kubernetes.KubernetesManager;
import org.apache.stratos.autoscaler.event.receiver.health.AutoscalerHealthStatEventReceiver;
import org.apache.stratos.autoscaler.event.receiver.topology.AutoscalerTopologyEventReceiver;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.deployment.partition.Partition;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

import java.util.Iterator;
import java.util.List;

/**
 * @scr.component name=org.apache.stratos.autoscaler.internal.AutoscalerServerComponent"
 * immediate="true"
 * @scr.reference name="registry.service"
 * interface=
 * "org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 * cardinality="1..1" policy="dynamic" bind="setTaskService"
 * unbind="unsetTaskService"
 */

public class AutoscalerServerComponent {

    private static final Log log = LogFactory.getLog(AutoscalerServerComponent.class);

    private AutoscalerTopologyEventReceiver asTopologyReceiver;
    private AutoscalerHealthStatEventReceiver autoscalerHealthStatEventReceiver;

    protected void activate(ComponentContext componentContext) throws Exception {
        try {
            // Start topology receiver
            asTopologyReceiver = new AutoscalerTopologyEventReceiver();
            Thread topologyTopicSubscriberThread = new Thread(asTopologyReceiver);
            topologyTopicSubscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology receiver thread started");
            }

            // Start health stat receiver
            autoscalerHealthStatEventReceiver = new AutoscalerHealthStatEventReceiver();
            Thread healthDelegatorThread = new Thread(autoscalerHealthStatEventReceiver);
            healthDelegatorThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Health statistics receiver thread started");
            }

            // Adding the registry stored partitions to the information model
            List<Partition> partitions = RegistryManager.getInstance().retrievePartitions();
            Iterator<Partition> partitionIterator = partitions.iterator();
            while (partitionIterator.hasNext()) {
                Partition partition = partitionIterator.next();
                PartitionManager.getInstance().addPartitionToInformationModel(partition);
            }
            
            // Adding the network partitions stored in registry to the information model
            List<NetworkPartitionLbHolder> nwPartitionHolders = RegistryManager.getInstance().retrieveNetworkPartitionLbHolders();
            Iterator<NetworkPartitionLbHolder> nwPartitionIterator = nwPartitionHolders.iterator();
            while (nwPartitionIterator.hasNext()) {
                NetworkPartitionLbHolder nwPartition = nwPartitionIterator.next();
                PartitionManager.getInstance().addNetworkPartitionLbHolder(nwPartition);
            }
            
            List<AutoscalePolicy> asPolicies = RegistryManager.getInstance().retrieveASPolicies();
            Iterator<AutoscalePolicy> asPolicyIterator = asPolicies.iterator();
            while (asPolicyIterator.hasNext()) {
                AutoscalePolicy asPolicy = asPolicyIterator.next();
                PolicyManager.getInstance().addASPolicyToInformationModel(asPolicy);
            }

            List<DeploymentPolicy> depPolicies = RegistryManager.getInstance().retrieveDeploymentPolicies();
            Iterator<DeploymentPolicy> depPolicyIterator = depPolicies.iterator();
            while (depPolicyIterator.hasNext()) {
                DeploymentPolicy depPolicy = depPolicyIterator.next();
                PolicyManager.getInstance().addDeploymentPolicyToInformationModel(depPolicy);
            }

            // Adding KubernetesGroups stored in registry to the information model
            List<KubernetesGroup> kubernetesGroupList = RegistryManager.getInstance().retrieveKubernetesGroups();
            Iterator<KubernetesGroup> kubernetesGroupIterator = kubernetesGroupList.iterator();
            while (kubernetesGroupIterator.hasNext()) {
                KubernetesGroup kubernetesGroup = kubernetesGroupIterator.next();
                KubernetesManager.getInstance().addNewKubernetesGroup(kubernetesGroup);
            }

            if (log.isInfoEnabled()) {
                log.info("Scheduling tasks to publish applications");
            }

            ApplicationSynchronizerTaskScheduler
                    .schedule(ServiceReferenceHolder.getInstance()
                            .getTaskService());

            if (log.isInfoEnabled()) {
                log.info("Autoscaler server Component activated");
            }
        } catch (Throwable e) {
            log.error("Error in activating the autoscaler component ", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        asTopologyReceiver.terminate();
        autoscalerHealthStatEventReceiver.terminate();
    }

    protected void setRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Registry Service");
        }
        try {
            ServiceReferenceHolder.getInstance().setRegistry(registryService.getGovernanceSystemRegistry());
        } catch (RegistryException e) {
            String msg = "Failed when retrieving Governance System Registry.";
            log.error(msg, e);
            throw new AutoScalerException(msg, e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
        if (log.isDebugEnabled()) {
            log.debug("Un-setting the Registry Service");
        }
        ServiceReferenceHolder.getInstance().setRegistry(null);
    }

    protected void setTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Task Service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Un-setting the Task Service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(null);
    }
}