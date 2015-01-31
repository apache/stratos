package org.apache.stratos.cloud.controller.internal;
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

import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.messaging.publisher.TopologySynchronizerTaskScheduler;
import org.apache.stratos.cloud.controller.messaging.receiver.application.ApplicationEventReceiver;
import org.apache.stratos.cloud.controller.messaging.receiver.cluster.status.ClusterStatusTopicReceiver;
import org.apache.stratos.cloud.controller.messaging.receiver.instance.status.InstanceStatusTopicReceiver;
import org.apache.stratos.cloud.controller.services.CloudControllerService;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceImpl;
import org.apache.stratos.common.clustering.DistributedObjectProvider;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.concurrent.ExecutorService;

/**
 * Registering Cloud Controller Service.
 *
 * @scr.component name="org.apache.stratos.cloud.controller" immediate="true"
 * @scr.reference name="hazelcast.instance.service" interface="com.hazelcast.core.HazelcastInstance"
 *                cardinality="0..1"policy="dynamic" bind="setHazelcastInstance" unbind="unsetHazelcastInstance"
 * @scr.reference name="distributedObjectProvider" interface="org.apache.stratos.common.clustering.DistributedObjectProvider"
 *                cardinality="1..1" policy="dynamic" bind="setDistributedObjectProvider" unbind="unsetDistributedObjectProvider"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService" unbind="unsetTaskService"
 * @scr.reference name="registry.service" interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="config.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 */
public class CloudControllerServiceComponent {

	private static final Log log = LogFactory.getLog(CloudControllerServiceComponent.class);

	private ClusterStatusTopicReceiver clusterStatusTopicReceiver;
	private InstanceStatusTopicReceiver instanceStatusTopicReceiver;
	private ApplicationEventReceiver applicationEventReceiver;
    private ExecutorService executorService;

	private static final String DEFAULT_IDENTIFIER = "Cloud-Controller";
	private static final int THREAD_POOL_SIZE = 10;
    private static final String CLOUD_CONTROLLER_COORDINATOR_LOCK = "CLOUD_CONTROLLER_COORDINATOR_LOCK";

    protected void activate(ComponentContext context) {
		try {
			executorService = StratosThreadPool.getExecutorService(DEFAULT_IDENTIFIER, THREAD_POOL_SIZE);

			// Register cloud controller service
			BundleContext bundleContext = context.getBundleContext();
			bundleContext.registerService(CloudControllerService.class.getName(),
			                              new CloudControllerServiceImpl(), null);

            if(CloudControllerContext.getInstance().isClustered()) {
                Thread coordinatorElectorThread = new Thread() {
                    @Override
                    public void run() {
                        ServiceReferenceHolder.getInstance().getHazelcastInstance()
                                .getLock(CLOUD_CONTROLLER_COORDINATOR_LOCK).lock();

                        String localMemberId = ServiceReferenceHolder.getInstance().getHazelcastInstance()
                                .getCluster().getLocalMember().getUuid();
                        log.info("Elected this member [" + localMemberId + "] " +
                                "as the cloud controller coordinator for the cluster");

                        CloudControllerContext.getInstance().setCoordinator(true);
                        executeCoordinatorTasks();
                    }
                };
                coordinatorElectorThread.setName("Cloud controller coordinator elector thread");
                executorService.submit(coordinatorElectorThread);
            } else {
                executeCoordinatorTasks();
            }
		} catch (Exception e) {
			log.error("Could not activate cloud controller service component", e);
        }
    }

    private void executeCoordinatorTasks() {
        applicationEventReceiver = new ApplicationEventReceiver();
        applicationEventReceiver.setExecutorService(executorService);
        applicationEventReceiver.execute();

        if (log.isInfoEnabled()) {
            log.info("Application event receiver thread started");
        }

        clusterStatusTopicReceiver = new ClusterStatusTopicReceiver();
        clusterStatusTopicReceiver.setExecutorService(executorService);
        clusterStatusTopicReceiver.execute();

        if (log.isInfoEnabled()) {
            log.info("Cluster status event receiver thread started");
        }

        instanceStatusTopicReceiver = new InstanceStatusTopicReceiver();
        instanceStatusTopicReceiver.setExecutorService(executorService);
        instanceStatusTopicReceiver.execute();

        if (log.isInfoEnabled()) {
            log.info("Instance status event receiver thread started");
        }

        if (log.isInfoEnabled()) {
            log.info("Scheduling topology synchronizer task");
        }
        TopologySynchronizerTaskScheduler.schedule(ServiceReferenceHolder.getInstance().getTaskService());
    }

    protected void setTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the task service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Un-setting the task service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(null);
    }

	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Registry Service");
		}

		try {
			UserRegistry registry = registryService.getGovernanceSystemRegistry();
	        ServiceReferenceHolder.getInstance().setRegistry(registry);
        } catch (RegistryException e) {
        	String msg = "Failed when retrieving Governance System Registry.";
        	log.error(msg, e);
        	throw new CloudControllerException(msg, e);
        }
	}

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
            log.debug("Un-setting the Registry Service");
        }
        ServiceReferenceHolder.getInstance().setRegistry(null);
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

	protected void deactivate(ComponentContext ctx) {
        // Close event publisher connections to message broker
        try {
            EventPublisherPool.close(MessagingUtil.Topics.TOPOLOGY_TOPIC.getTopicName());
        } catch (Exception e) {
            log.warn("An error occurred while closing cloud controller topology event publisher", e);
        }

        // Shutdown executor service
        if(executorService != null) {
            try {
                executorService.shutdownNow();
            } catch (Exception e) {
                log.warn("An error occurred while shutting down cloud controller executor service", e);
            }
        }
	}
}