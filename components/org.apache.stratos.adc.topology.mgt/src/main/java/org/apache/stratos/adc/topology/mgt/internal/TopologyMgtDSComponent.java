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

package org.apache.stratos.adc.topology.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.adc.topology.TopologyEventMessageDelegator;
import org.apache.stratos.adc.topology.TopologyEventMessageReceiver;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.adc.topology.mgt.service.impl.TopologyManagementServiceImpl;
import org.apache.stratos.adc.topology.mgt.util.ConfigHolder;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.ntask.core.service.TaskService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="topology.mgt.service" immediate="true"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 * cardinality="1..1" policy="dynamic" bind="setTaskService" unbind="unsetTaskService"
 */
public class TopologyMgtDSComponent {

    private static final Log log = LogFactory.getLog(TopologyMgtDSComponent.class);

    protected void activate(ComponentContext ctxt) {
		try {
            // Start topic subscriber thread
            TopicSubscriber topicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
            topicSubscriber.setMessageListener(new TopologyEventMessageReceiver());
            Thread subscriberThread = new Thread(topicSubscriber);
            subscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology event message receiver thread started");
            }

            // Start topology message receiver thread
            Thread receiverThread = new Thread(new TopologyEventMessageDelegator());
            receiverThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology message processor thread started");
            }

			BundleContext bundleContext = ctxt.getBundleContext();
			bundleContext.registerService(
					TopologyManagementService.class.getName(),
					new TopologyManagementServiceImpl(), null);

			log.debug("******* Topology Mgt Service bundle is activated ******* ");
		} catch (Throwable e) {
            log.error("******* Topology Mgt Service Service bundle is failed to activate ****", e);
        }
    }

    protected void deactivate(ComponentContext context) {}

    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ConfigHolder.getInstance().setAxisConfiguration(
                cfgCtxService.getServerConfigContext().getAxisConfiguration());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ConfigHolder.getInstance().setAxisConfiguration(null);
    }
    
    protected void setTaskService(TaskService taskService) {
        ConfigHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        ConfigHolder.getInstance().setTaskService(null);
    }
}
