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
import org.apache.stratos.cloud.controller.receiver.application.ApplicationTopicReceiver;
import org.apache.stratos.cloud.controller.receiver.cluster.status.ClusterStatusTopicReceiver;
import org.apache.stratos.cloud.controller.exception.CloudControllerException;
import org.apache.stratos.cloud.controller.impl.CloudControllerServiceImpl;
import org.apache.stratos.cloud.controller.interfaces.CloudControllerService;
import org.apache.stratos.cloud.controller.publisher.TopologySynchronizerTaskScheduler;
import org.apache.stratos.cloud.controller.receiver.instance.status.InstanceStatusTopicReceiver;
import org.apache.stratos.cloud.controller.util.ServiceReferenceHolder;
import org.apache.stratos.messaging.broker.publish.EventPublisherPool;
import org.apache.stratos.messaging.util.Util;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.caching.impl.DistributedMapProvider;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * Registering Cloud Controller Service.
 *
 * @scr.component name="org.apache.stratos.cloud.controller" immediate="true"
 * @scr.reference name="distributedMapProvider" interface="org.wso2.carbon.caching.impl.DistributedMapProvider"
 *                cardinality="1..1" policy="dynamic" bind="setDistributedMapProvider" unbind="unsetDistributedMapProvider"
 * @scr.reference name="ntask.component"
 *                interface="org.wso2.carbon.ntask.core.service.TaskService"
 *                cardinality="1..1" policy="dynamic" bind="setTaskService" unbind="unsetTaskService"
 * @scr.reference name="registry.service"
 *                interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="config.context.service"
 *                interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 */
public class CloudControllerServiceComponent {

    private static final Log log = LogFactory.getLog(CloudControllerServiceComponent.class);
    private ClusterStatusTopicReceiver clusterStatusTopicReceiver;
    private InstanceStatusTopicReceiver instanceStatusTopicReceiver;
    private ApplicationTopicReceiver applicationTopicReceiver;

    protected void activate(ComponentContext context) {
        try {
            applicationTopicReceiver = new ApplicationTopicReceiver();
            Thread tApplicationTopicReceiver = new Thread(applicationTopicReceiver);
            tApplicationTopicReceiver.start();

            if (log.isInfoEnabled()) {
                log.info("Application Receiver thread started");
            }

            clusterStatusTopicReceiver = new ClusterStatusTopicReceiver();
            Thread tClusterStatusTopicReceiver = new Thread(clusterStatusTopicReceiver);
            tClusterStatusTopicReceiver.start();

            if (log.isInfoEnabled()) {
                log.info("Cluster status Receiver thread started");
            }

            instanceStatusTopicReceiver = new InstanceStatusTopicReceiver();
            Thread tInstanceStatusTopicReceiver = new Thread(instanceStatusTopicReceiver);
            tInstanceStatusTopicReceiver.start();
            if(log.isInfoEnabled()) {
                log.info("Instance status message receiver thread started");
            }

        	// Register cloud controller service
            BundleContext bundleContext = context.getBundleContext();
            bundleContext.registerService(CloudControllerService.class.getName(),
                    new CloudControllerServiceImpl(), null);

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

    protected void setDistributedMapProvider(DistributedMapProvider mapProvider) {
        ServiceReferenceHolder.getInstance().setDistributedMapProvider(mapProvider);
    }

    protected void unsetDistributedMapProvider(DistributedMapProvider mapProvider) {
        ServiceReferenceHolder.getInstance().setDistributedMapProvider(null);
    }
	
	protected void deactivate(ComponentContext ctx) {
        // Close event publisher connections to message broker
        EventPublisherPool.close(Util.Topics.TOPOLOGY_TOPIC.getTopicName());
	}
}
