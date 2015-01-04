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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.manager.messaging.receiver.StratosManagerInstanceStatusEventReceiver;
import org.apache.stratos.manager.user.management.TenantUserRoleManager;
import org.apache.stratos.manager.messaging.publisher.TenantEventPublisher;
import org.apache.stratos.manager.messaging.TenantSynchronizerTaskScheduler;
import org.apache.stratos.manager.messaging.receiver.StratosManagerTopologyEventReceiver;
import org.apache.stratos.manager.utils.CartridgeConfigFileReader;
import org.apache.stratos.manager.utils.UserRoleCreator;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.util.Util;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.concurrent.ExecutorService;

/**
 * @scr.component name="org.wso2.carbon.hosting.mgt.internal.StratosManagerServiceComponent"
 *                immediate="true"
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
 *                interface=
 *                "org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService"
 *                unbind="unsetTaskService"
 */

public class StratosManagerServiceComponent {

	private static final Log log = LogFactory.getLog(StratosManagerServiceComponent.class);
	private static final String IDENTIFIER = "Stratos_manager";
	private static final int THREAD_POOL_SIZE = 20;

    private StratosManagerTopologyEventReceiver topologyEventReceiver;
    private StratosManagerInstanceStatusEventReceiver instanceStatusEventReceiver;
	private ExecutorService executorService;

    protected void activate(ComponentContext componentContext) throws Exception {
		try {
			CartridgeConfigFileReader.readProperties();
			executorService=StratosThreadPool.getExecutorService(IDENTIFIER, THREAD_POOL_SIZE);
			
            // Schedule complete tenant event synchronizer
            if(log.isDebugEnabled()) {
                log.debug("Scheduling tenant synchronizer task...");
            }
            TenantSynchronizerTaskScheduler.schedule(ServiceReferenceHolder.getInstance().getTaskService());

            // Register tenant event publisher
            if(log.isDebugEnabled()) {
                log.debug("Starting tenant event publisher...");
            }
            TenantEventPublisher tenantEventPublisher = new TenantEventPublisher();
            componentContext.getBundleContext().registerService(
                    org.apache.stratos.common.listeners.TenantMgtListener.class.getName(),
                    tenantEventPublisher, null);

            if(log.isDebugEnabled()) {
                log.debug("Starting instance status event receiver...");
            }
            instanceStatusEventReceiver = new StratosManagerInstanceStatusEventReceiver();
            instanceStatusEventReceiver.setExecutorService(executorService);
            instanceStatusEventReceiver.execute();
            if(log.isInfoEnabled()) {
                log.info("Instance status event receiver thread started");
            }

            if(log.isDebugEnabled()) {
                log.debug("Starting topology event receiver...");
            }
            topologyEventReceiver = new StratosManagerTopologyEventReceiver();
            topologyEventReceiver.setExecutorService(executorService);
            executorService.execute(topologyEventReceiver);
            if(log.isInfoEnabled()) {
                log.info("Topology event receiver thread started");
            }

            RealmService realmService = ServiceReferenceHolder.getRealmService();
            UserRealm realm = realmService.getBootstrapRealm();
            UserStoreManager userStoreManager = realm.getUserStoreManager();
            //Create a Internal/user Role at server start-up
            UserRoleCreator.createTenantUserRole(userStoreManager);

            TenantUserRoleManager tenantUserRoleManager = new TenantUserRoleManager();
            componentContext.getBundleContext().registerService(
                    org.apache.stratos.common.listeners.TenantMgtListener.class.getName(),
                    tenantUserRoleManager, null);

            // retrieve persisted CartridgeSubscriptions
//            new DataInsertionAndRetrievalManager().cachePersistedSubscriptions();

            //Component activated successfully
            log.info("ADC management server component is activated");
			
		} catch (Exception e) {
            if(log.isErrorEnabled()) {
			    log.error("Could not activate ADC management server component", e);
            }
		}
	}

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        ServiceReferenceHolder.setClientConfigContext(contextService.getClientConfigContext());
        ServiceReferenceHolder.setServerConfigContext(contextService.getServerConfigContext());

    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        ServiceReferenceHolder.setClientConfigContext(null);
        ServiceReferenceHolder.setServerConfigContext(null);
    }

    protected void setRealmService(RealmService realmService) {
        // keeping the realm service in the DataHolder class
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

    /*protected void setTopologyManagementService(TopologyManagementService topologyMgtService) {
        DataHolder.setTopologyMgtService(topologyMgtService);
    }

    protected void unsetTopologyManagementService(TopologyManagementService topologyMgtService) {
    }*/

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

    protected void deactivate(ComponentContext context) {
        // Close event publisher connections to message broker
        EventPublisherPool.close(Util.Topics.INSTANCE_NOTIFIER_TOPIC.getTopicName());
        EventPublisherPool.close(Util.Topics.TENANT_TOPIC.getTopicName());

	    executorService.shutdownNow();
        //terminate Stratos Manager Topology Receiver
        topologyEventReceiver.terminate();
    }
}
