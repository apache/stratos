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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.application.status.receiver.ApplicationStatusTopicReceiver;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.impl.CloudControllerServiceImpl;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.stratos.cloud.controller.publisher.TopologySynchronizerTaskScheduler;
import org.apache.stratos.cloud.controller.topic.instance.status.InstanceStatusEventMessageDelegator;
import org.apache.stratos.cloud.controller.topic.instance.status.InstanceStatusEventMessageListener;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * Registering Cloud Controller Service.
 * 
 * @scr.component name="org.apache.stratos.cloud.controller" immediate="true"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService"
 *                unbind="unsetTaskService"
 * @scr.reference name="registry.service"
 *                interface=
 *                "org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService"
 *                unbind="unsetRegistryService"
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic"
 *                bind="setConfigurationContextService"
 *                unbind="unsetConfigurationContextService"           
 */
public class CloudControllerDSComponent {

    private static final Log log = LogFactory.getLog(CloudControllerDSComponent.class);
    private ApplicationStatusTopicReceiver applicationStatusTopicReceiver;
    protected void activate(ComponentContext context) {
        try {
               	
            // Start instance status event message listener
            TopicSubscriber subscriber = new TopicSubscriber(CloudControllerConstants.INSTANCE_TOPIC);
            subscriber.setMessageListener(new InstanceStatusEventMessageListener());
            Thread tsubscriber = new Thread(subscriber);
            tsubscriber.start();

            // Start instance status message delegator
            InstanceStatusEventMessageDelegator delegator = new InstanceStatusEventMessageDelegator();
            Thread tdelegator = new Thread(delegator);
            tdelegator.start();

            applicationStatusTopicReceiver = new ApplicationStatusTopicReceiver();
            Thread appThread = new Thread(applicationStatusTopicReceiver);
            appThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Application status Receiver thread started");
            }

        	
        	// Register cloud controller service
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(CloudControllerService.class.getName(), new CloudControllerServiceImpl(), null);

            if(log.isInfoEnabled()) {
                log.info("Scheduling tasks");
            }
            
			TopologySynchronizerTaskScheduler
						.schedule(ServiceReferenceHolder.getInstance()
								.getTaskService());


			
        } catch (Throwable e) {
            log.error("******* Cloud Controller Service bundle is failed to activate ****", e);
        }
    }
    
    protected void setTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Setting the Task Service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        if (log.isDebugEnabled()) {
            log.debug("Unsetting the Task Service");
        }
        ServiceReferenceHolder.getInstance().setTaskService(null);
    }
    
	protected void setRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
			log.debug("Setting the Registry Service");
		}
		
		try {			
			UserRegistry registry = registryService.getGovernanceSystemRegistry();
	        ServiceReferenceHolder.getInstance()
	                                             .setRegistry(registry);
        } catch (RegistryException e) {
        	String msg = "Failed when retrieving Governance System Registry.";
        	log.error(msg, e);
        	throw new CloudControllerException(msg, e);
        } 
	}

	protected void unsetRegistryService(RegistryService registryService) {
		if (log.isDebugEnabled()) {
            log.debug("Unsetting the Registry Service");
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
	
	protected void deactivate(ComponentContext ctx) {
        // Close event publisher connections to message broker
        EventPublisherPool.close(Constants.TOPOLOGY_TOPIC);
	}
	
}
