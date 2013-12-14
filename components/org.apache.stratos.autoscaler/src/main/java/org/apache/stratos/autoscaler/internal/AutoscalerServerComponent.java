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
import org.apache.stratos.autoscaler.NetworkPartitionContext;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageDelegator;
import org.apache.stratos.autoscaler.message.receiver.health.HealthEventMessageReceiver;
import org.apache.stratos.autoscaler.partition.PartitionManager;
import org.apache.stratos.autoscaler.policy.PolicyManager;
import org.apache.stratos.autoscaler.policy.model.AutoscalePolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.topology.AutoscalerTopologyReceiver;
import org.apache.stratos.autoscaler.util.AutoScalerConstants;
import org.apache.stratos.autoscaler.util.Deserializer;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.deployment.partition.Partition;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;

import java.util.ArrayList;
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
 */

public class AutoscalerServerComponent {

    private static final Log log = LogFactory.getLog(AutoscalerServerComponent.class);

    protected void activate(ComponentContext componentContext) throws Exception {
        try {
            // Start topology receiver
            Thread topologyTopicSubscriberThread = new Thread(new AutoscalerTopologyReceiver());
            topologyTopicSubscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology receiver thread started");
            }

            // Start health stat receiver
            TopicSubscriber healthStatTopicSubscriber = new TopicSubscriber(Constants.HEALTH_STAT_TOPIC);
            healthStatTopicSubscriber.setMessageListener(new HealthEventMessageReceiver());
            Thread healthStatTopicSubscriberThread = new Thread(healthStatTopicSubscriber);
            healthStatTopicSubscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Health event message receiver thread started");
            }

            HealthEventMessageDelegator healthEventMessageDelegator = new HealthEventMessageDelegator();
            Thread healthDelegatorThread = new Thread(healthEventMessageDelegator);
            healthDelegatorThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Health message processor thread started");
            }

            // Adding the registry stored partitions to the information model
            List<Partition> partitions = RegistryManager.getInstance().retrievePartitions();
            Iterator<Partition> partitionIterator = partitions.iterator();
            while (partitionIterator.hasNext()) {
                Partition partition = partitionIterator.next();
                PartitionManager.getInstance().addPartitionToInformationModel(partition);
            }
            
            // Adding the network partitions stored in registry to the information model
            List<NetworkPartitionContext> nwPartitionCtxts = RegistryManager.getInstance().retrieveNetworkPartitions();
            Iterator<NetworkPartitionContext> nwPartitionIterator = nwPartitionCtxts.iterator();
            while (nwPartitionIterator.hasNext()) {
                NetworkPartitionContext nwPartition = nwPartitionIterator.next();
                PartitionManager.getInstance().addNetworkPartitionContext(nwPartition);
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

            if (log.isInfoEnabled()) {
                log.info("Autoscaler Server Component activated");
            }
        } catch (Throwable e) {
            log.error("Error in activating the autoscaler component ", e);
        }
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
            log.debug("Unsetting the Registry Service");
        }
        ServiceReferenceHolder.getInstance().setRegistry(null);
    }
}