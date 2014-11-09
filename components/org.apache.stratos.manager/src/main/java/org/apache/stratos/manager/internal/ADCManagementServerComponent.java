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
import org.apache.stratos.manager.listener.InstanceStatusListener;
import org.apache.stratos.manager.listener.TenantUserRoleCreator;
import org.apache.stratos.manager.publisher.TenantEventPublisher;
import org.apache.stratos.manager.publisher.TenantSynchronizerTaskScheduler;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;
import org.apache.stratos.manager.topology.receiver.StratosManagerTopologyEventReceiver;
import org.apache.stratos.manager.utils.CartridgeConfigFileReader;
import org.apache.stratos.manager.utils.UserRoleCreator;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.carbon.hosting.mgt.internal.ADCManagementServerComponent"
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

public class ADCManagementServerComponent {

    private static final Log log = LogFactory.getLog(ADCManagementServerComponent.class);
    private StratosManagerTopologyEventReceiver stratosManagerTopologyEventReceiver;

    protected void activate(ComponentContext componentContext) throws Exception {
		try {
			CartridgeConfigFileReader.readProperties();

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

            // Start instance status topic subscriber
            if(log.isDebugEnabled()) {
                log.debug("Starting instance status topic subscriber...");
            }
            TopicSubscriber subscriber = new TopicSubscriber(Constants.INSTANCE_STATUS_TOPIC);
            subscriber.setMessageListener(new InstanceStatusListener());
            Thread tsubscriber = new Thread(subscriber);
			tsubscriber.start();

            RealmService realmService = DataHolder.getRealmService();
            UserRealm realm = realmService.getBootstrapRealm();
            UserStoreManager userStoreManager = realm.getUserStoreManager();
            //Create a Internal/user Role at server start-up
            UserRoleCreator.createTenantUserRole(userStoreManager);

            TenantUserRoleCreator tenantUserRoleCreator = new TenantUserRoleCreator();
            componentContext.getBundleContext().registerService(
                    org.apache.stratos.common.listeners.TenantMgtListener.class.getName(),
                    tenantUserRoleCreator, null);

            //initializing the topology event subscriber
            /*TopicSubscriber topologyTopicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
            topologyTopicSubscriber.setMessageListener(new TopologyEventListner());
            Thread topologyTopicSubscriberThread = new Thread(topologyTopicSubscriber);
            topologyTopicSubscriberThread.start();

            //Starting Topology Receiver
            TopologyReceiver topologyReceiver = new TopologyReceiver();
            Thread topologyReceiverThread = new Thread(topologyReceiver);
            topologyReceiverThread.start();*/

            stratosManagerTopologyEventReceiver = new StratosManagerTopologyEventReceiver();
            Thread topologyReceiverThread = new Thread(stratosManagerTopologyEventReceiver);
            topologyReceiverThread.start();
            log.info("Topology receiver thread started");

            // retrieve persisted CartridgeSubscriptions
            new DataInsertionAndRetrievalManager().cachePersistedSubscriptions();
            
            //Grouping
            /*
            if (log.isDebugEnabled()) {
            	log.debug("restoring composite applications ...");
            }
            new CompositeApplicationManager().restoreCompositeApplications ();
            
            if (log.isDebugEnabled()) {
            	log.debug("done restoring composite applications ...");
            }
            */

            //Component activated successfully
            log.info("ADC management server component is activated");
			
		} catch (Exception e) {
            if(log.isErrorEnabled()) {
			    log.error("Could not activate ADC management server component", e);
            }
		}
	}

    protected void setConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.setClientConfigContext(contextService.getClientConfigContext());
        DataHolder.setServerConfigContext(contextService.getServerConfigContext());

    }

    protected void unsetConfigurationContextService(ConfigurationContextService contextService) {
        DataHolder.setClientConfigContext(null);
        DataHolder.setServerConfigContext(null);
    }

    protected void setRealmService(RealmService realmService) {
        // keeping the realm service in the DataHolder class
        DataHolder.setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
    }

    protected void setRegistryService(RegistryService registryService) {
        try {
            DataHolder.setRegistryService(registryService);
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
        EventPublisherPool.close(Constants.INSTANCE_NOTIFIER_TOPIC);
        EventPublisherPool.close(Constants.TENANT_TOPIC);

        //terminate Stratos Manager Topology Receiver
        stratosManagerTopologyEventReceiver.terminate();
    }
}
