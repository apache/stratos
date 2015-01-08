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

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationSynchronizerTaskScheduler;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.event.receiver.health.AutoscalerHealthStatEventReceiver;
import org.apache.stratos.autoscaler.event.receiver.topology.AutoscalerTopologyEventReceiver;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.status.processor.cluster.ClusterStatusProcessorChain;
import org.apache.stratos.autoscaler.status.processor.group.GroupStatusProcessorChain;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.common.clustering.DistributedObjectProvider;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.ConfigurationContextService;

import com.hazelcast.core.HazelcastInstance;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * @scr.component name=org.apache.stratos.autoscaler.internal.AutoscalerServerComponent" immediate="true"
 * @scr.reference name="registry.service" interface="org.wso2.carbon.registry.core.service.RegistryService"
 * 			      cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 * 				  cardinality="1..1" policy="dynamic" bind="setTaskService" unbind="unsetTaskService"
 * @scr.reference name="hazelcast.instance.service" interface="com.hazelcast.core.HazelcastInstance"
 *                cardinality="0..1"policy="dynamic" bind="setHazelcastInstance" unbind="unsetHazelcastInstance"
 * @scr.reference name="distributedObjectProvider" interface="org.apache.stratos.common.clustering.DistributedObjectProvider"
 *                cardinality="1..1" policy="dynamic" bind="setDistributedObjectProvider" unbind="unsetDistributedObjectProvider"
 * @scr.reference name="config.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 */

public class AutoscalerServerComponent {

	private static final String THREAD_IDENTIFIER_KEY = "threadPool.autoscaler.identifier";
	private static final String DEFAULT_IDENTIFIER = "Auto-Scaler";
	private static final String THREAD_POOL_SIZE_KEY = "threadPool.autoscaler.threadPoolSize";
	private static final String COMPONENTS_CONFIG = CarbonUtils.getCarbonConfigDirPath()+ File.separator+"stratos-config.xml";
	private static final int THREAD_POOL_SIZE = 10;
	private static final Log log = LogFactory.getLog(AutoscalerServerComponent.class);
	private static final String AUTOSCALER_COORDINATOR_LOCK = "AUTOSCALER_COORDINATOR_LOCK";

	private AutoscalerTopologyEventReceiver asTopologyReceiver;
	private AutoscalerHealthStatEventReceiver autoscalerHealthStatEventReceiver;
	private ExecutorService executorService;

	protected void activate(ComponentContext componentContext) throws Exception {
		try {
			
			XMLConfiguration conf = ConfUtil.getInstance(COMPONENTS_CONFIG).getConfiguration();
			int threadPoolSize = conf.getInt(THREAD_POOL_SIZE_KEY, THREAD_POOL_SIZE);
			String threadIdentifier = conf.getString(THREAD_IDENTIFIER_KEY, DEFAULT_IDENTIFIER);
			executorService = StratosThreadPool.getExecutorService(threadIdentifier, threadPoolSize);
			
			if(AutoscalerContext.getInstance().isClustered()) {
                Thread coordinatorElectorThread = new Thread() {
                    @Override
                    public void run() {
                        ServiceReferenceHolder.getInstance().getHazelcastInstance()
                                .getLock(AUTOSCALER_COORDINATOR_LOCK).lock();

                        log.info("Elected this member [" + ServiceReferenceHolder.getInstance().getHazelcastInstance()
                                .getCluster().getLocalMember().getUuid() + "] " +
                                "as the autoscaler coordinator for the cluster");

                        AutoscalerContext.getInstance().setCoordinator(true);
                        try {
                        	executeStartupTasks();
                        } catch (Throwable e) {
                			log.error("Error in activating the autoscaler component ", e);
                        }
                    }
                };
                coordinatorElectorThread.setName("Autoscaler coordinator elector thread");
                executorService.submit(coordinatorElectorThread);
            } else {
            	executeStartupTasks();
            }

			if (log.isInfoEnabled()) {
				log.info("Autoscaler server Component activated");
			}
		} catch (Throwable e) {
			log.error("Error in activating the autoscaler component ", e);
		}
	}
	
	private void executeStartupTasks() throws Exception{
		
		// Start topology receiver
		asTopologyReceiver = new AutoscalerTopologyEventReceiver();
		asTopologyReceiver.setExecutorService(executorService);
		asTopologyReceiver.execute();

		if (log.isDebugEnabled()) {
			log.debug("Topology receiver executor service started");
		}

		// Start health stat receiver
		autoscalerHealthStatEventReceiver = new AutoscalerHealthStatEventReceiver();
		autoscalerHealthStatEventReceiver.setExecutorService(executorService);
		autoscalerHealthStatEventReceiver.execute();
		if (log.isDebugEnabled()) {
			log.debug("Health statistics receiver thread started");
		}

        // Adding the registry stored partitions to the information model
        List<Partition> partitions = RegistryManager.getInstance().retrievePartitions();
        Iterator<Partition> partitionIterator = partitions.iterator();
        while (partitionIterator.hasNext()) {
            Partition partition = partitionIterator.next();
//            PartitionManager.getInstance().addPartitionToInformationModel(partition);
        }

        // Adding the network partitions stored in registry to the information model
//        List<NetworkPartitionLbHolder> nwPartitionHolders = RegistryManager.getInstance().retrieveNetworkPartitionLbHolders();
//        Iterator<NetworkPartitionLbHolder> nwPartitionIterator = nwPartitionHolders.iterator();
//        while (nwPartitionIterator.hasNext()) {
//            NetworkPartitionLbHolder nwPartition = nwPartitionIterator.next();
//            PartitionManager.getInstance().addNetworkPartitionLbHolder(nwPartition);
//        }

        // Add AS policies to information model
        List<AutoscalePolicy> asPolicies = RegistryManager.getInstance().retrieveASPolicies();
        Iterator<AutoscalePolicy> asPolicyIterator = asPolicies.iterator();
        while (asPolicyIterator.hasNext()) {
            AutoscalePolicy asPolicy = asPolicyIterator.next();
            PolicyManager.getInstance().addASPolicyToInformationModel(asPolicy);
        }

        // Add Deployment policies to information model
		List<DeploymentPolicy> depPolicies = RegistryManager.getInstance().retrieveDeploymentPolicies();
		Iterator<DeploymentPolicy> depPolicyIterator = depPolicies.iterator();
		while (depPolicyIterator.hasNext()) {
			DeploymentPolicy depPolicy = depPolicyIterator.next();
			PolicyManager.getInstance().addDeploymentPolicy(depPolicy);
		}

		//starting the processor chain
		ClusterStatusProcessorChain clusterStatusProcessorChain = new ClusterStatusProcessorChain();
		ServiceReferenceHolder.getInstance().setClusterStatusProcessorChain(clusterStatusProcessorChain);

		GroupStatusProcessorChain groupStatusProcessorChain = new GroupStatusProcessorChain();
		ServiceReferenceHolder.getInstance().setGroupStatusProcessorChain(groupStatusProcessorChain);

		if (log.isInfoEnabled()) {
			log.info("Scheduling tasks to publish applications");
		}

		ApplicationSynchronizerTaskScheduler
				.schedule(ServiceReferenceHolder.getInstance()
				                                .getTaskService());
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
    
    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ServiceReferenceHolder.getInstance().setAxisConfiguration(
                cfgCtxService.getServerConfigContext().getAxisConfiguration());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ServiceReferenceHolder.getInstance().setAxisConfiguration(null);
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(hazelcastInstance);
    }

    public void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(null);
    }

    protected void setDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(distributedObjectProvider);
    }

    protected void unsetDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(null);
    }
}

