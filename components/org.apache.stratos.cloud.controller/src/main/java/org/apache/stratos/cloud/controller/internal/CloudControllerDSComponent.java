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
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.impl.CloudControllerServiceImpl;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.cloud.controller.topic.instance.status.InstanceStatusEventMessageListener;
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.broker.publish.EventPublisher;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.ConfigurationContextService;

import java.util.List;

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
//* @scr.reference name="org.apache.stratos.cloud.controller.deployers" 
//*                interface="org.apache.stratos.cloud.controller.interfaces.CloudControllerDeployerService"
//*                cardinality="1..1" policy="dynamic" bind="setCloudControllerDeployerService"
//*                unbind="unsetCloudControllerDeployerService"
public class CloudControllerDSComponent {

    private static final Log log = LogFactory.getLog(CloudControllerDSComponent.class);
    private static final FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

    protected void activate(ComponentContext context) {
        try {
            
            // register deployers of CC
//            AxisConfiguration axisConfig = ServiceReferenceHolder.getInstance().getAxisConfiguration();
//            
//            if(axisConfig ==  null) {
//                String msg = "Axis Configuration is null. Cannot register deployers.";
//                log.error(msg);
//                throw new CloudControllerException(msg);
//            }
//            
//            DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig.getConfigurator();
//            Deployer cloudControllerDeployer = new CloudControllerDeployer();
//            Deployer cartridgeDeployer = new CartridgeDeployer();
//            deploymentEngine.addDeployer(cloudControllerDeployer, "../../conf", "xml");
//            deploymentEngine.addDeployer(cartridgeDeployer, "cartridges", "xml");
        	
            // get all the topics - comma separated list
            String topicsString = dataHolder.getTopologyConfig().getProperty(CloudControllerConstants.TOPICS_PROPERTY);
            
            if(topicsString == null || topicsString.isEmpty()) {
                topicsString = Constants.TOPOLOGY_TOPIC;
            } 
            
            String[] topics = topicsString.split(",");
            for (String topic : topics) {
                
                dataHolder.addEventPublisher(new EventPublisher(topic), topic);
            }

            // Start instance status event message listener
            TopicSubscriber subscriber = new TopicSubscriber(CloudControllerConstants.INSTANCE_TOPIC);
            subscriber.setMessageListener(new InstanceStatusEventMessageListener());
            Thread tsubscriber = new Thread(subscriber);
            tsubscriber.start();
        	
        	// initialize the topic publishers
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(CloudControllerService.class.getName(),
                                          new CloudControllerServiceImpl(), null);
            

            log.debug("******* Cloud Controller Service bundle is activated ******* ");
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
	        ServiceReferenceHolder.getInstance()
	                                             .setRegistry(registryService.getGovernanceSystemRegistry());
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

		List<EventPublisher> publishers = dataHolder.getAllEventPublishers();
		for (EventPublisher topicPublisher : publishers) {
			topicPublisher.close();
		}
	}
	
}
