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

package org.wso2.carbon.lb.endpoint.internal;

import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.endpoints.Endpoint;
import org.apache.synapse.mediators.base.SequenceMediator;
import org.apache.synapse.mediators.builtin.SendMediator;
import org.apache.synapse.mediators.filters.InMediator;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.cartridge.messages.CreateClusterDomainMessage;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.mediation.initializer.ServiceBusUtils;
import org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService;
import org.wso2.carbon.lb.endpoint.EndpointDeployer;
import org.wso2.carbon.lb.endpoint.TenantAwareLoadBalanceEndpointException;
import org.wso2.carbon.lb.endpoint.builder.TopologySyncher;
import org.wso2.carbon.lb.endpoint.cluster.manager.ClusterDomainManagerImpl;
import org.wso2.carbon.lb.endpoint.endpoint.TenantAwareLoadBalanceEndpoint;
import org.wso2.carbon.lb.endpoint.subscriber.TopologySubscriber;
import org.wso2.carbon.lb.endpoint.util.ConfigHolder;
import org.wso2.carbon.lb.endpoint.util.TopologyConstants;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * @scr.component name="org.wso2.carbon.load.balancer.endpoint" immediate="true"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="synapse.config.service"
 * interface="org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService"
 * cardinality="1..1" policy="dynamic" bind="setSynapseConfigurationService"
 * unbind="unsetSynapseConfigurationService"
 * @scr.reference name="synapse.env.service"
 * interface="org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService"
 * cardinality="1..n" policy="dynamic" bind="setSynapseEnvironmentService"
 * unbind="unsetSynapseEnvironmentService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic"
 * bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="dependency.mgt.service"
 * interface="org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService"
 * cardinality="0..1" policy="dynamic"
 * bind="setDependencyManager" unbind="unsetDependencyManager"
 * @scr.reference name="synapse.registrations.service"
 * interface="org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService"
 * cardinality="1..n" policy="dynamic" bind="setSynapseRegistrationsService"
 * unbind="unsetSynapseRegistrationsService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 * @scr.reference name="org.wso2.carbon.lb.common"
 * interface="org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService"
 * cardinality="1..1" policy="dynamic" bind="setLoadBalancerConfigurationService"
 * unbind="unsetLoadBalancerConfigurationService"
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class LoadBalanceEndpointServiceComponent {

    private static final Log log = LogFactory.getLog(LoadBalanceEndpointServiceComponent.class);

    private boolean activated = false;

    protected void activate(ComponentContext ctxt) {
        try {
            SynapseEnvironmentService synEnvService =
                                                      ConfigHolder.getInstance()
                                                                  .getSynapseEnvironmentService(MultitenantConstants.SUPER_TENANT_ID);

            registerDeployer(ConfigHolder.getInstance().getAxisConfiguration(),
                             synEnvService.getSynapseEnvironment());

			      if (ConfigHolder.getInstance().getConfigCtxt() != null) {
				      ConfigHolder
						    .getInstance()
						    .getConfigCtxt()
						    .setNonReplicableProperty(
								CreateClusterDomainMessage.CLUSTER_DOMAIN_MANAGER,
								new ClusterDomainManagerImpl());
				        log.debug("Setting property Cluster Domain MANAGER ... ");

		  	    }
			
            SynapseEnvironment synapseEnv = synEnvService.getSynapseEnvironment();

            /* Registering Tenant Aware Load Balance Endpoint */

            // get the main sequence mediator
            SequenceMediator mainSequence =
                                            (SequenceMediator) synapseEnv.getSynapseConfiguration()
                                                                         .getSequence("main");

            boolean successfullyRegistered = false;
            
            // iterate through its child mediators
            for (Mediator child : mainSequence.getList()) {

                // find the InMediator
                if (child instanceof InMediator) {
                    
                    for(Mediator inChild : ((InMediator)child).getList()){
                        
                        // find the SendMediator
                        if (inChild instanceof SendMediator) {
                            
                            SendMediator sendMediator = (SendMediator) inChild;
                            
                            /* add Tenant Aware LB endpoint */
                            
                            TenantAwareLoadBalanceEndpoint tenantAwareEp = new TenantAwareLoadBalanceEndpoint();

                            tenantAwareEp.init(synapseEnv);
                            
                            sendMediator.setEndpoint(tenantAwareEp);
                            
                            successfullyRegistered = true;

                            if (log.isDebugEnabled()) {
                                log.debug("Added Tenant Aware Endpoint: " +
                                          sendMediator.getEndpoint().getName() + "" +
                                          " to Send Mediator.");
                            }
                        }
                    }
                }
            }
            
            if(!successfullyRegistered){
                String msg = "Failed to register Tenant Aware Load Balance Endpoint in Send Mediator.";
                log.fatal(msg);
                throw new TenantAwareLoadBalanceEndpointException(msg);
            }

            if (log.isDebugEnabled()) {
                log.debug("Endpoint Admin bundle is activated ");
            }
            
            if (ConfigHolder.getInstance().getLbConfig().getLoadBalancerConfig().getMbServerUrl() != null) {

                // start consumer
                // initialize TopologyBuilder Consumer
                Thread topologyConsumer =
                    new Thread(new TopologySyncher(ConfigHolder.getInstance().getSharedTopologyDiffQueue()));
                // start consumer
                topologyConsumer.start();

                TopologySubscriber.subscribe(TopologyConstants.TOPIC_NAME);

            }
            activated = true;
        } catch (Throwable e) {
            log.error("Failed to activate Endpoint Admin bundle ", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        try {
            Set<Map.Entry<Integer, SynapseEnvironmentService>> entrySet =
                    ConfigHolder.getInstance().getSynapseEnvironmentServices().entrySet();
            for (Map.Entry<Integer, SynapseEnvironmentService> entry : entrySet) {
                unregisterDeployer(
                        entry.getValue().getConfigurationContext().getAxisConfiguration(),
                        entry.getValue().getSynapseEnvironment());
            }
        } catch (Exception e) {
            log.warn("Couldn't remove the EndpointDeployer");
        }
    }

    /**
     * Un-registers the Endpoint deployer.
     *
     * @param axisConfig         AxisConfiguration to which this deployer belongs
     * @param synapseEnvironment SynapseEnvironment to which this deployer belongs
     */
    private void unregisterDeployer(AxisConfiguration axisConfig, SynapseEnvironment synapseEnvironment)
            throws TenantAwareLoadBalanceEndpointException {
        if (axisConfig != null) {
            DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig.getConfigurator();
            String synapseConfigPath = ServiceBusUtils.getSynapseConfigAbsPath(
                    synapseEnvironment.getServerContextInformation());
            String endpointDirPath = synapseConfigPath
                    + File.separator + MultiXMLConfigurationBuilder.ENDPOINTS_DIR;
            deploymentEngine.removeDeployer(
                    endpointDirPath, ServiceBusConstants.ARTIFACT_EXTENSION);
        }
    }

    /**
     * Registers the Endpoint deployer.
     *
     * @param axisConfig         AxisConfiguration to which this deployer belongs
     * @param synapseEnvironment SynapseEnvironment to which this deployer belongs
     */
    private void registerDeployer(AxisConfiguration axisConfig, SynapseEnvironment synapseEnvironment)
            throws TenantAwareLoadBalanceEndpointException {
        SynapseConfiguration synCfg = synapseEnvironment.getSynapseConfiguration();
        DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig.getConfigurator();
        SynapseArtifactDeploymentStore deploymentStore = synCfg.getArtifactDeploymentStore();

        String synapseConfigPath = ServiceBusUtils.getSynapseConfigAbsPath(
                synapseEnvironment.getServerContextInformation());
        String endpointDirPath = synapseConfigPath
                + File.separator + MultiXMLConfigurationBuilder.ENDPOINTS_DIR;

        for (Endpoint ep : synCfg.getDefinedEndpoints().values()) {
            if (ep.getFileName() != null) {
                deploymentStore.addRestoredArtifact(
                        endpointDirPath + File.separator + ep.getFileName());
            }
        }
        deploymentEngine.addDeployer(
                new EndpointDeployer(), endpointDirPath, ServiceBusConstants.ARTIFACT_EXTENSION);
    }

    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ConfigHolder.getInstance().setAxisConfiguration(
                cfgCtxService.getServerConfigContext().getAxisConfiguration());
        ConfigHolder.getInstance().setConfigCtxt(cfgCtxService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        ConfigHolder.getInstance().setAxisConfiguration(null);
        ConfigHolder.getInstance().setConfigCtxt(null);
    }

    protected void setSynapseConfigurationService(
            SynapseConfigurationService synapseConfigurationService) {

        ConfigHolder.getInstance().setSynapseConfiguration(
                synapseConfigurationService.getSynapseConfiguration());
    }

    protected void unsetSynapseConfigurationService(
            SynapseConfigurationService synapseConfigurationService) {

        ConfigHolder.getInstance().setSynapseConfiguration(null);
    }

    /**
     * Here we receive an event about the creation of a SynapseEnvironment. If this is
     * SuperTenant we have to wait until all the other constraints are met and actual
     * initialization is done in the activate method. Otherwise we have to do the activation here.
     *
     * @param synapseEnvironmentService SynapseEnvironmentService which contains information
     *                                  about the new Synapse Instance
     */
    protected void setSynapseEnvironmentService(
            SynapseEnvironmentService synapseEnvironmentService) {
        boolean alreadyCreated = ConfigHolder.getInstance().getSynapseEnvironmentServices().
                containsKey(synapseEnvironmentService.getTenantId());

        ConfigHolder.getInstance().addSynapseEnvironmentService(
                synapseEnvironmentService.getTenantId(),
                synapseEnvironmentService);
        if (activated) {
            if (!alreadyCreated) {
                try {
                    registerDeployer(synapseEnvironmentService.getConfigurationContext().getAxisConfiguration(),
                            synapseEnvironmentService.getSynapseEnvironment());
                    if (log.isDebugEnabled()) {
                        log.debug("Endpoint Admin bundle is activated ");
                    }
                } catch (Throwable e) {
                    log.error("Failed to activate Endpoint Admin bundle ", e);
                }
            }
        }
    }

    /**
     * Here we receive an event about Destroying a SynapseEnvironment. This can be the super tenant
     * destruction or a tenant destruction.
     *
     * @param synapseEnvironmentService synapseEnvironment
     */
    protected void unsetSynapseEnvironmentService(
            SynapseEnvironmentService synapseEnvironmentService) {
        ConfigHolder.getInstance().removeSynapseEnvironmentService(
                synapseEnvironmentService.getTenantId());
    }

    protected void setRegistryService(RegistryService regService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService bound to the endpoint component");
        }
        try {
            ConfigHolder.getInstance().setConfigRegistry(regService.getConfigSystemRegistry());
            ConfigHolder.getInstance().setGovernanceRegistry(regService.getGovernanceSystemRegistry());
        } catch (RegistryException e) {
            log.error("Couldn't retrieve the registry from the registry service");
        }
    }

    protected void unsetRegistryService(RegistryService regService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unbound from the endpoint component");
        }
        ConfigHolder.getInstance().setConfigRegistry(null);
    }

    protected void setDependencyManager(DependencyManagementService dependencyMgr) {
        if (log.isDebugEnabled()) {
            log.debug("Dependency management service bound to the endpoint component");
        }
        ConfigHolder.getInstance().setDependencyManager(dependencyMgr);
    }

    protected void unsetDependencyManager(DependencyManagementService dependencyMgr) {
        if (log.isDebugEnabled()) {
            log.debug("Dependency management service unbound from the endpoint component");
        }
        ConfigHolder.getInstance().setDependencyManager(null);
    }

    protected void setSynapseRegistrationsService(
            SynapseRegistrationsService synapseRegistrationsService) {

    }

    protected void unsetSynapseRegistrationsService(
            SynapseRegistrationsService synapseRegistrationsService) {
        int tenantId = synapseRegistrationsService.getTenantId();
        if (ConfigHolder.getInstance().getSynapseEnvironmentServices().containsKey(tenantId)) {
            SynapseEnvironment env = ConfigHolder.getInstance().
                    getSynapseEnvironmentService(tenantId).getSynapseEnvironment();

            ConfigHolder.getInstance().removeSynapseEnvironmentService(
                    synapseRegistrationsService.getTenantId());

            AxisConfiguration axisConfig = synapseRegistrationsService.getConfigurationContext().
                    getAxisConfiguration();
            if (axisConfig != null) {
                try {
                    unregisterDeployer(axisConfig, env);
                } catch (Exception e) {
                    log.warn("Couldn't remove the EndpointDeployer");
                }
            }
        }
    }

    protected void setRealmService(RealmService realmService) {
        ConfigHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        ConfigHolder.getInstance().setRealmService(null);
    }
    
    protected void setLoadBalancerConfigurationService(LoadBalancerConfigurationService lbConfigSer){
        ConfigHolder.getInstance().setLbConfigService(lbConfigSer);
    }
    
    protected void unsetLoadBalancerConfigurationService(LoadBalancerConfigurationService lbConfigSer){
        ConfigHolder.getInstance().setLbConfigService(null);
    }
}
