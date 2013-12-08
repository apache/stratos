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
package org.apache.stratos.adc.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.mgt.listener.InstanceStatusListener;
import org.apache.stratos.adc.mgt.publisher.TenantEventPublisher;
import org.apache.stratos.adc.mgt.publisher.TenantSynchronizerTaskScheduler;
import org.apache.stratos.adc.mgt.utils.CartridgeConfigFileReader;
import org.apache.stratos.adc.mgt.utils.StratosDBUtils;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.message.receiver.topology.TopologyReceiver;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.service.RegistryService;
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
 * @scr.reference name="topology.mgt.service"
 *                interface=
 *                "org.apache.stratos.adc.topology.mgt.service.TopologyManagementService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setTopologyManagementService"
 *                unbind="unsetTopologyManagementService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService"
 *                unbind="unsetTaskService"
 */

public class ADCManagementServerComponent {
    private static final Log log = LogFactory.getLog(ADCManagementServerComponent.class);

    protected void activate(ComponentContext componentContext) throws Exception {
		try {
			CartridgeConfigFileReader.readProperties();
			StratosDBUtils.initialize();
			DataHolder.setEventPublisher(new EventPublisher(Constants.ARTIFACT_SYNCHRONIZATION_TOPIC));

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

            //Starting Topology Receiver
            TopologyReceiver topologyReceiver = new TopologyReceiver();
            Thread topologyReceiverThread = new Thread(topologyReceiver);
            topologyReceiverThread.start();

            if (log.isInfoEnabled()) {
                log.info("ADC management server component is activated");
            }
			
		} catch (Exception e) {
            if(log.isFatalEnabled()) {
			    log.fatal("Could not activate ADC management server component", e);
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
            DataHolder.setRegistry(registryService.getGovernanceSystemRegistry());
        } catch (Exception e) {
            log.error("Cannot retrieve governance registry", e);
        }
    }

    protected void unsetRegistryService(RegistryService registryService) {
    }

    protected void setTopologyManagementService(TopologyManagementService topologyMgtService) {
        DataHolder.setTopologyMgtService(topologyMgtService);
    }

    protected void unsetTopologyManagementService(TopologyManagementService topologyMgtService) {
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
}
