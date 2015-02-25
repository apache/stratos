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
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.status.processor.cluster.ClusterStatusProcessorChain;
import org.apache.stratos.autoscaler.status.processor.group.GroupStatusProcessorChain;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.ConfUtil;
import org.apache.stratos.autoscaler.util.ServiceReferenceHolder;
import org.apache.stratos.cloud.controller.stub.domain.Partition;
import org.apache.stratos.common.clustering.DistributedObjectProvider;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.api.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.ConfigurationContextService;

import com.hazelcast.core.HazelcastInstance;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @scr.component name=org.apache.stratos.autoscaler.internal.AutoscalerServiceComponent" immediate="true"
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

public class AutoscalerServiceComponent {

    private static final Log log = LogFactory.getLog(AutoscalerServiceComponent.class);

	private static final String AUTOSCALER_COORDINATOR_LOCK = "AUTOSCALER_COORDINATOR_LOCK";

	private AutoscalerTopologyEventReceiver asTopologyReceiver;
	private AutoscalerHealthStatEventReceiver autoscalerHealthStatEventReceiver;
	private ExecutorService executorService;

	protected void activate(ComponentContext componentContext) throws Exception {
		try {
			
			XMLConfiguration conf = ConfUtil.getInstance(AutoscalerConstants.COMPONENTS_CONFIG).getConfiguration();
            int threadPoolSize = conf.getInt(AutoscalerConstants.THREAD_POOL_SIZE_KEY,
                    AutoscalerConstants.AUTOSCALER_THREAD_POOL_SIZE);
			executorService = StratosThreadPool.getExecutorService(AutoscalerConstants.AUTOSCALER_THREAD_POOL_ID,
                    threadPoolSize);

            ServiceReferenceHolder.getInstance().setExecutorService(executorService);
			
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
                        	executeCoordinatorTasks();
                        } catch (Exception e) {
                			log.error("Error in activating the autoscaler component ", e);
                        }
                    }
                };
                coordinatorElectorThread.setName("Autoscaler coordinator elector thread");
                executorService.submit(coordinatorElectorThread);
            } else {
            	executeCoordinatorTasks();
            }

			if (log.isInfoEnabled()) {
				log.info("Autoscaler service component activated");
			}
		} catch (Exception e) {
			log.error("Error in activating the autoscaler service component ", e);
		}
	}
	
	private void executeCoordinatorTasks() throws InvalidPolicyException {
		
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

        // Add AS policies to information model
        List<AutoscalePolicy> asPolicies = RegistryManager.getInstance().retrieveASPolicies();
        Iterator<AutoscalePolicy> asPolicyIterator = asPolicies.iterator();
        while (asPolicyIterator.hasNext()) {
            AutoscalePolicy asPolicy = asPolicyIterator.next();
            PolicyManager.getInstance().addASPolicyToInformationModel(asPolicy);
        }
        
        // Add application policies to information model
        List<ApplicationPolicy> applicationPolicies = RegistryManager.getInstance().retrieveApplicationPolicies();
        Iterator<ApplicationPolicy> applicationPolicyIterator = applicationPolicies.iterator();
        while (applicationPolicyIterator.hasNext()) {
            ApplicationPolicy applicationPolicy = applicationPolicyIterator.next();
            PolicyManager.getInstance().addApplicationPolicyToInformationModel(applicationPolicy);
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
        try {
            asTopologyReceiver.terminate();
        } catch (Exception e) {
            log.warn("An error occurred while terminating autoscaler topology event receiver", e);
        }

        try {
            autoscalerHealthStatEventReceiver.terminate();
        } catch (Exception e) {
            log.warn("An error occurred while terminating autoscaler health statistics event receiver", e);
        }

        // Shutdown executor service
        if(executorService != null) {
            shutdownExecutorService(executorService);
        }

        // Shutdown application monitor executor service
        shutdownExecutorService(AutoscalerConstants.APPLICATION_MONITOR_THREAD_POOL_ID);

        // Shutdown group monitor executor service
        shutdownExecutorService(AutoscalerConstants.GROUP_MONITOR_THREAD_POOL_ID);

        // Shutdown cluster monitor scheduler executor service
        shutdownScheduledExecutorService(AutoscalerConstants.CLUSTER_MONITOR_SCHEDULER_ID);

        // Shutdown cluster monitor executor service
        shutdownExecutorService(AutoscalerConstants.CLUSTER_MONITOR_THREAD_POOL_ID);
    }

    private void shutdownExecutorService(String executorServiceId) {
        ExecutorService executorService = StratosThreadPool.getExecutorService(executorServiceId, 1);
        if(executorService != null) {
            shutdownExecutorService(executorService);
        }
    }

    private void shutdownScheduledExecutorService(String executorServiceId) {
        ExecutorService executorService = StratosThreadPool.getScheduledExecutorService(executorServiceId, 1);
        if(executorService != null) {
            shutdownExecutorService(executorService);
        }
    }

    private void shutdownExecutorService(ExecutorService executorService) {
        try {
            executorService.shutdownNow();
        } catch (Exception e) {
            log.warn("An error occurred while shutting down executor service", e);
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

