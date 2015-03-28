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
package org.apache.stratos.manager.internal;

import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.Component;
import org.apache.stratos.common.services.ComponentActivationEventListener;
import org.apache.stratos.common.services.ComponentStartUpSynchronizer;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.manager.context.StratosManagerContext;
import org.apache.stratos.manager.messaging.publisher.TenantEventPublisher;
import org.apache.stratos.manager.messaging.publisher.synchronizer.ApplicationSignUpEventSynchronizer;
import org.apache.stratos.manager.messaging.publisher.synchronizer.TenantEventSynchronizer;
import org.apache.stratos.manager.messaging.receiver.StratosManagerApplicationEventReceiver;
import org.apache.stratos.manager.messaging.receiver.StratosManagerInstanceStatusEventReceiver;
import org.apache.stratos.manager.messaging.receiver.StratosManagerTopologyEventReceiver;
import org.apache.stratos.manager.user.management.TenantUserRoleManager;
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.apache.stratos.manager.utils.CartridgeConfigFileReader;
import org.apache.stratos.manager.utils.UserRoleCreator;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.util.MessagingUtil;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @scr.component name="org.wso2.carbon.hosting.mgt.internal.StratosManagerServiceComponent"
 *                immediate="true"
 * @scr.reference name="hazelcast.instance.service" interface="com.hazelcast.core.HazelcastInstance"
 *                cardinality="0..1"policy="dynamic" bind="setHazelcastInstance" unbind="unsetHazelcastInstance"
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"
 * @scr.reference name="user.realmservice.default"
 *                interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService"
 *                unbind="unsetRealmService"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService"
 *                unbind="unsetTaskService"
 * @scr.reference name="distributedObjectProvider" interface="org.apache.stratos.common.services.DistributedObjectProvider"
 *                cardinality="1..1" policy="dynamic" bind="setDistributedObjectProvider" unbind="unsetDistributedObjectProvider"
 * @scr.reference name="componentStartUpSynchronizer" interface="org.apache.stratos.common.services.ComponentStartUpSynchronizer"
 *                cardinality="1..1" policy="dynamic" bind="setComponentStartUpSynchronizer" unbind="unsetComponentStartUpSynchronizer"
 */
public class StratosManagerServiceComponent {

	private static final Log log = LogFactory.getLog(StratosManagerServiceComponent.class);

    private static final String THREAD_POOL_ID = "stratos.manager.thread.pool";
    private static final String SCHEDULER_THREAD_POOL_ID = "stratos.manager.scheduler.thread.pool";
    private static final String STRATOS_MANAGER_COORDINATOR_LOCK = "stratos.manager.coordinator.lock";
    private static final int THREAD_POOL_SIZE = 20;
    private static final int SCHEDULER_THREAD_POOL_SIZE = 5;

    private StratosManagerTopologyEventReceiver topologyEventReceiver;
    private StratosManagerInstanceStatusEventReceiver instanceStatusEventReceiver;
    private StratosManagerApplicationEventReceiver applicationEventReceiver;
	private ExecutorService executorService;
    private ScheduledExecutorService scheduler;

    protected void activate(final ComponentContext componentContext) throws Exception {
		try {
            executorService = StratosThreadPool.getExecutorService(THREAD_POOL_ID, THREAD_POOL_SIZE);
            scheduler = StratosThreadPool.getScheduledExecutorService(SCHEDULER_THREAD_POOL_ID,
                    SCHEDULER_THREAD_POOL_SIZE);

            Runnable stratosManagerActivator = new Runnable() {
                @Override
                public void run() {
                    try {
                        ComponentStartUpSynchronizer componentStartUpSynchronizer =
                                ServiceReferenceHolder.getInstance().getComponentStartUpSynchronizer();

                        // Wait for cloud controller and autoscaler components to be activated
                        componentStartUpSynchronizer.waitForComponentActivation(Component.StratosManager,
                                Component.CloudController);
                        componentStartUpSynchronizer.waitForComponentActivation(Component.StratosManager,
                                Component.Autoscaler);

                        CartridgeConfigFileReader.readProperties();
                        if (StratosManagerContext.getInstance().isClustered()) {
                            Thread coordinatorElectorThread = new Thread() {
                                @Override
                                public void run() {
                                    try {
                                        ServiceReferenceHolder.getInstance().getHazelcastInstance()
                                                .getLock(STRATOS_MANAGER_COORDINATOR_LOCK).lock();

                                        String localMemberId = ServiceReferenceHolder.getInstance().getHazelcastInstance()
                                                .getCluster().getLocalMember().getUuid();
                                        log.info("Elected this member [" + localMemberId + "] " +
                                                "as the stratos manager coordinator for the cluster");

                                        StratosManagerContext.getInstance().setCoordinator(true);
                                        executeCoordinatorTasks(componentContext);
                                    } catch (Exception e) {
                                        if (log.isErrorEnabled()) {
                                            log.error("Could not execute coordinator tasks", e);
                                        }
                                    }
                                }
                            };
                            coordinatorElectorThread.setName("Stratos manager coordinator elector thread");
                            executorService.submit(coordinatorElectorThread);
                        } else {
                            executeCoordinatorTasks(componentContext);
                        }

                        // Initialize topology event receiver
                        initializeTopologyEventReceiver();

                        // Initialize application event receiver
                        initializeApplicationEventReceiver();

                        componentStartUpSynchronizer.waitForWebServiceActivation("StratosManagerService");
                        componentStartUpSynchronizer.setComponentStatus(Component.StratosManager, true);
                        if (log.isInfoEnabled()) {
                            log.info("Stratos manager component is activated");
                        }
                    } catch (Exception e) {
                        log.error("Could not activate stratos manager service component", e);
                    }
                }
            };
            Thread stratosManagerActivatorThread = new Thread(stratosManagerActivator);
            stratosManagerActivatorThread.start();
		} catch (Exception e) {
            log.error("Could not activate stratos manager service component", e);
		}
	}

    /**
     * Execute coordinator tasks
     * @param componentContext
     * @throws UserStoreException
     * @throws UserManagerException
     */
    private void executeCoordinatorTasks(ComponentContext componentContext) throws UserStoreException,
            UserManagerException {

        initializeTenantEventPublisher(componentContext);
        initializeInstanceStatusEventReceiver();
        registerComponentStartUpEventListeners();

        // Create internal/user Role at server start-up
        createInternalUserRole(componentContext);
    }

    /**
     * Initialize instance status event receiver
     */
    private void initializeInstanceStatusEventReceiver() {
        instanceStatusEventReceiver = new StratosManagerInstanceStatusEventReceiver();
        instanceStatusEventReceiver.setExecutorService(executorService);
        instanceStatusEventReceiver.execute();
    }

    /**
     * Initialize topology event receiver
     */
    private void initializeTopologyEventReceiver() {
        topologyEventReceiver = new StratosManagerTopologyEventReceiver();
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();
    }

    /**
     * Initialize application event receiver
     */
    private void initializeApplicationEventReceiver() {
        applicationEventReceiver = new StratosManagerApplicationEventReceiver();
        applicationEventReceiver.setExecutorService(executorService);
        applicationEventReceiver.execute();
    }

    /**
     * Create internal user role if not exists.
     * @param componentContext
     * @throws UserStoreException
     * @throws UserManagerException
     */
    private void createInternalUserRole(ComponentContext componentContext) throws UserStoreException, UserManagerException {
        RealmService realmService = ServiceReferenceHolder.getRealmService();
        UserRealm realm = realmService.getBootstrapRealm();
        UserStoreManager userStoreManager = realm.getUserStoreManager();
        UserRoleCreator.createInternalUserRole(userStoreManager);

        TenantUserRoleManager tenantUserRoleManager = new TenantUserRoleManager();
        componentContext.getBundleContext().registerService(
                org.apache.stratos.common.listeners.TenantMgtListener.class.getName(),
                tenantUserRoleManager, null);
    }

    /**
     * Schedule complete tenant event synchronizer and initialize tenant event publisher
     * @param componentContext
     */
    private void initializeTenantEventPublisher(ComponentContext componentContext) {
        // Register tenant event publisher
        if(log.isDebugEnabled()) {
            log.debug("Initializing tenant event publisher...");
        }
        final TenantEventPublisher tenantEventPublisher = new TenantEventPublisher();
        componentContext.getBundleContext().registerService(
                org.apache.stratos.common.listeners.TenantMgtListener.class.getName(),
                tenantEventPublisher, null);
        if(log.isInfoEnabled()) {
            log.info("Tenant event publisher initialized");
        }
    }

    private void registerComponentStartUpEventListeners() {
        ComponentStartUpSynchronizer componentStartUpSynchronizer =
                ServiceReferenceHolder.getInstance().getComponentStartUpSynchronizer();
        componentStartUpSynchronizer.addEventListener(new ComponentActivationEventListener() {
            @Override
            public void activated(Component component) {
                if(component == Component.StratosManager) {
                    Runnable tenantSynchronizer = new TenantEventSynchronizer();
                    scheduler.scheduleAtFixedRate(tenantSynchronizer, 0, 1, TimeUnit.MINUTES);

                    Runnable applicationSignUpSynchronizer = new ApplicationSignUpEventSynchronizer();
                    scheduler.scheduleAtFixedRate(applicationSignUpSynchronizer, 0, 1, TimeUnit.MINUTES);
                }
            }
        });
    }

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        ServiceReferenceHolder.setClientConfigContext(contextService.getClientConfigContext());
        ServiceReferenceHolder.setServerConfigContext(contextService.getServerConfigContext());
        ServiceReferenceHolder.getInstance().setAxisConfiguration(
                contextService.getServerConfigContext().getAxisConfiguration());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        ServiceReferenceHolder.setClientConfigContext(null);
        ServiceReferenceHolder.setServerConfigContext(null);
        ServiceReferenceHolder.getInstance().setAxisConfiguration(null);
    }

    public void setHazelcastInstance(HazelcastInstance hazelcastInstance) {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(hazelcastInstance);
    }

    public void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {
        ServiceReferenceHolder.getInstance().setHazelcastInstance(null);
    }

    protected void setRealmService(RealmService realmService) {
        ServiceReferenceHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
    }

    protected void setRegistryService(RegistryService registryService) {
        try {
            ServiceReferenceHolder.setRegistryService(registryService);
        } catch (Exception e) {
            log.error("Cannot retrieve governance registry", e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
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
    
    protected void setDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(distributedObjectProvider);
    }

    protected void unsetDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        ServiceReferenceHolder.getInstance().setDistributedObjectProvider(null);
    }

    protected void setComponentStartUpSynchronizer(ComponentStartUpSynchronizer componentStartUpSynchronizer) {
        ServiceReferenceHolder.getInstance().setComponentStartUpSynchronizer(componentStartUpSynchronizer);
    }

    protected void unsetComponentStartUpSynchronizer(ComponentStartUpSynchronizer componentStartUpSynchronizer) {
        ServiceReferenceHolder.getInstance().setComponentStartUpSynchronizer(null);
    }

    protected void deactivate(ComponentContext context) {
        // Close event publisher connections to message broker
        EventPublisherPool.close(MessagingUtil.Topics.INSTANCE_NOTIFIER_TOPIC.getTopicName());
        EventPublisherPool.close(MessagingUtil.Topics.TENANT_TOPIC.getTopicName());

        shutdownExecutorService(THREAD_POOL_ID);
        shutdownScheduledExecutorService(SCHEDULER_THREAD_POOL_ID);
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
}
