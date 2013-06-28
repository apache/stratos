/*
 *  Copyright (c) 2009, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.adc.topology.mgt.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.adc.topology.mgt.builder.TopologySyncher;
import org.wso2.carbon.adc.topology.mgt.service.TopologyManagementService;
import org.wso2.carbon.adc.topology.mgt.service.impl.TopologyManagementServiceImpl;
import org.wso2.carbon.adc.topology.mgt.subscriber.TopologySubscriber;
import org.wso2.carbon.adc.topology.mgt.util.TopologyConstants;
import org.wso2.carbon.adc.topology.mgt.util.ConfigHolder;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService;
import org.wso2.carbon.ntask.core.service.TaskService;

//* @scr.reference name="synapse.config.service"
//* interface="org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService"
//* cardinality="1..1" policy="dynamic" bind="setSynapseConfigurationService"
//* unbind="unsetSynapseConfigurationService"
//* @scr.reference name="registry.service"
//* interface="org.wso2.carbon.registry.core.service.RegistryService"
//* cardinality="1..1" policy="dynamic"
//* bind="setRegistryService" unbind="unsetRegistryService"
//* @scr.reference name="dependency.mgt.service"
//* interface="org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService"
//* cardinality="0..1" policy="dynamic"
//* bind="setDependencyManager" unbind="unsetDependencyManager"
//* @scr.reference name="user.realmservice.default"
//* interface="org.wso2.carbon.user.core.service.RealmService"
//* cardinality="1..1" policy="dynamic" bind="setRealmService"
//* unbind="unsetRealmService"

/**
 * @scr.component name="topology.mgt.service" immediate="true"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="org.wso2.carbon.lb.common"
 * interface="org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService"
 * cardinality="1..1" policy="dynamic" bind="setLoadBalancerConfigurationService"
 * unbind="unsetLoadBalancerConfigurationService"
 * @scr.reference name="ntask.component" interface="org.wso2.carbon.ntask.core.service.TaskService"
 * cardinality="1..1" policy="dynamic" bind="setTaskService" unbind="unsetTaskService"
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class TopologyMgtDSComponent {

    private static final Log log = LogFactory.getLog(TopologyMgtDSComponent.class);

//    private boolean activated = false;

    protected void activate(ComponentContext ctxt) {
    	try {
    		// start consumer
    		// initialize TopologyBuilder Consumer
            if (System.getProperty("mb.server.ip") != null) {
                Thread topologyConsumer = new Thread(new TopologySyncher(ConfigHolder.getInstance().getSharedTopologyDiffQueue()));
                // start consumer
                topologyConsumer.start();

                TopologySubscriber.subscribe(TopologyConstants.TOPIC_NAME);
            }

    		
            BundleContext bundleContext = ctxt.getBundleContext();
            bundleContext.registerService(TopologyManagementService.class.getName(),
                                          new TopologyManagementServiceImpl(), null);

            log.debug("******* Topology Mgt Service bundle is activated ******* ");
        } catch (Throwable e) {
            log.error("******* Topology Mgt Service Service bundle is failed to activate ****", e);
        }
//    	log.info("**************************************");
//    	for (String str : ConfigHolder.getInstance().getLbConfig().getServiceDomains()) {
//	        log.info(str);
//        }
//    	log.info("**************************************");
    	
//    	if(!activated){
//    		GroupMgtAgentBuilder.createGroupMgtAgents();
//    		activated = true;
//    	}
    	
    	
    	// topology synching task activation
//    	TaskManager tm = null;
//		try {
//			// topology sync
//			ConfigHolder.getInstance().getTaskService()
//			          .registerTaskType(TopologyConstants.TOPOLOGY_SYNC_TASK_TYPE);
//
//			tm =
//					ConfigHolder.getInstance().getTaskService()
//			               .getTaskManager(TopologyConstants.TOPOLOGY_SYNC_TASK_TYPE);
//
//			TriggerInfo triggerInfo = new TriggerInfo(TopologyConstants.TOPOLOGY_SYNC_CRON);
//			TaskInfo taskInfo =
//			                    new TaskInfo(TopologyConstants.TOPOLOGY_SYNC_TASK_NAME,
//			                                 TopologySubscriberTask.class.getName(),
//			                                 new HashMap<String, String>(), triggerInfo);
//			tm.registerTask(taskInfo);
//
//			// start consumer
//			// initialize TopologyBuilder Consumer
//	        Thread topologyConsumer = new Thread(new TopologyBuilder(ConfigHolder.getInstance().getSharedTopologyDiffQueue()));
//	        // start consumer
//	        topologyConsumer.start();
//			
//		} catch (Exception e) {
//			String msg = "Error scheduling task: " + TopologyConstants.TOPOLOGY_SYNC_TASK_NAME;
//			log.error(msg, e);
//			if (tm != null) {
//				try {
//					tm.deleteTask(TopologyConstants.TOPOLOGY_SYNC_TASK_NAME);
//				} catch (TaskException e1) {
//					log.error(e1);
//				}
//			}
//			throw new TopologyMgtException(msg, e);
//		}
    }

    protected void deactivate(ComponentContext context) {}

    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ConfigHolder.getInstance().setAxisConfiguration(
                cfgCtxService.getServerConfigContext().getAxisConfiguration());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ConfigHolder.getInstance().setAxisConfiguration(null);
    }

//    protected void setSynapseConfigurationService(
//            SynapseConfigurationService synapseConfigurationService) {
//
//        ConfigHolder.getInstance().setSynapseConfiguration(
//                synapseConfigurationService.getSynapseConfiguration());
//    }
//
//    protected void unsetSynapseConfigurationService(
//            SynapseConfigurationService synapseConfigurationService) {
//
//        ConfigHolder.getInstance().setSynapseConfiguration(null);
//    }

    /**
     * Here we receive an event about the creation of a SynapseEnvironment. If this is
     * SuperTenant we have to wait until all the other constraints are met and actual
     * initialization is done in the activate method. Otherwise we have to do the activation here.
     *
     * @param synapseEnvironmentService SynapseEnvironmentService which contains information
     *                                  about the new Synapse Instance
     */
//    protected void setSynapseEnvironmentService(
//            SynapseEnvironmentService synapseEnvironmentService) {
//        boolean alreadyCreated = ConfigHolder.getInstance().getSynapseEnvironmentServices().
//                containsKey(synapseEnvironmentService.getTenantId());
//
//        ConfigHolder.getInstance().addSynapseEnvironmentService(
//                synapseEnvironmentService.getTenantId(),
//                synapseEnvironmentService);
//        if (activated) {
//            if (!alreadyCreated) {
//                try {
//                    registerDeployer(synapseEnvironmentService.getConfigurationContext().getAxisConfiguration(),
//                            synapseEnvironmentService.getSynapseEnvironment());
//                    if (log.isDebugEnabled()) {
//                        log.debug("Endpoint Admin bundle is activated ");
//                    }
//                } catch (Throwable e) {
//                    log.error("Failed to activate Endpoint Admin bundle ", e);
//                }
//            }
//        }
//    }

//    /**
//     * Here we receive an event about Destroying a SynapseEnvironment. This can be the super tenant
//     * destruction or a tenant destruction.
//     *
//     * @param synapseEnvironmentService synapseEnvironment
//     */
//    protected void unsetSynapseEnvironmentService(
//            SynapseEnvironmentService synapseEnvironmentService) {
//        ConfigHolder.getInstance().removeSynapseEnvironmentService(
//                synapseEnvironmentService.getTenantId());
//    }
//
//    protected void setRegistryService(RegistryService regService) {
//        if (log.isDebugEnabled()) {
//            log.debug("RegistryService bound to the endpoint component");
//        }
//        try {
//            ConfigHolder.getInstance().setConfigRegistry(regService.getConfigSystemRegistry());
//            ConfigHolder.getInstance().setGovernanceRegistry(regService.getGovernanceSystemRegistry());
//        } catch (RegistryException e) {
//            log.error("Couldn't retrieve the registry from the registry service");
//        }
//    }
//
//    protected void unsetRegistryService(RegistryService regService) {
//        if (log.isDebugEnabled()) {
//            log.debug("RegistryService unbound from the endpoint component");
//        }
//        ConfigHolder.getInstance().setConfigRegistry(null);
//    }
//
//    protected void setDependencyManager(DependencyManagementService dependencyMgr) {
//        if (log.isDebugEnabled()) {
//            log.debug("Dependency management service bound to the endpoint component");
//        }
//        ConfigHolder.getInstance().setDependencyManager(dependencyMgr);
//    }
//
//    protected void unsetDependencyManager(DependencyManagementService dependencyMgr) {
//        if (log.isDebugEnabled()) {
//            log.debug("Dependency management service unbound from the endpoint component");
//        }
//        ConfigHolder.getInstance().setDependencyManager(null);
//    }
//
//    protected void setSynapseRegistrationsService(
//            SynapseRegistrationsService synapseRegistrationsService) {
//
//    }

//    protected void unsetSynapseRegistrationsService(
//            SynapseRegistrationsService synapseRegistrationsService) {
//        int tenantId = synapseRegistrationsService.getTenantId();
//        if (ConfigHolder.getInstance().getSynapseEnvironmentServices().containsKey(tenantId)) {
//            SynapseEnvironment env = ConfigHolder.getInstance().
//                    getSynapseEnvironmentService(tenantId).getSynapseEnvironment();
//
//            ConfigHolder.getInstance().removeSynapseEnvironmentService(
//                    synapseRegistrationsService.getTenantId());
//
//            AxisConfiguration axisConfig = synapseRegistrationsService.getConfigurationContext().
//                    getAxisConfiguration();
//            if (axisConfig != null) {
//                try {
//                    unregisterDeployer(axisConfig, env);
//                } catch (Exception e) {
//                    log.warn("Couldn't remove the EndpointDeployer");
//                }
//            }
//        }
//    }

//    protected void setRealmService(RealmService realmService) {
//        ConfigHolder.getInstance().setRealmService(realmService);
//    }
//
//    protected void unsetRealmService(RealmService realmService) {
//        ConfigHolder.getInstance().setRealmService(null);
//    }
//    
    protected void setLoadBalancerConfigurationService(LoadBalancerConfigurationService lbConfigSer){
        ConfigHolder.getInstance().setLbConfigService(lbConfigSer);
    }
    
    protected void unsetLoadBalancerConfigurationService(LoadBalancerConfigurationService lbConfigSer){
        ConfigHolder.getInstance().setLbConfigService(null);
    }
    
    protected void setTaskService(TaskService taskService) {
        ConfigHolder.getInstance().setTaskService(taskService);
    }

    protected void unsetTaskService(TaskService taskService) {
        ConfigHolder.getInstance().setTaskService(null);
    }
}
