/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.lb.endpoint.internal;

import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.stratos.lb.endpoint.LoadBalancerContext;
import org.apache.stratos.lb.endpoint.TenantAwareLoadBalanceEndpointException;
import org.apache.stratos.lb.endpoint.topology.TopologyEventMessageDeligator;
import org.apache.stratos.lb.endpoint.endpoint.TenantAwareLoadBalanceEndpoint;
import org.apache.stratos.lb.endpoint.topology.TopologyEventMessageReceiver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.messaging.broker.subscribe.TopicSubscriber;
import org.apache.stratos.messaging.util.Constants;
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
import org.apache.stratos.lb.endpoint.EndpointDeployer;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * @scr.component name="org.apache.stratos.lbr.endpoint" immediate="true"
 * @scr.reference name="configuration.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"
 * bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 * @scr.reference name="synapse.config.service" interface=
 * "org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService"
 * cardinality="1..1" policy="dynamic"
 * bind="setSynapseConfigurationService"
 * unbind="unsetSynapseConfigurationService"
 * @scr.reference name="synapse.env.service" interface=
 * "org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService"
 * cardinality="1..n" policy="dynamic"
 * bind="setSynapseEnvironmentService"
 * unbind="unsetSynapseEnvironmentService"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService"
 * cardinality="1..1" policy="dynamic" bind="setRegistryService"
 * unbind="unsetRegistryService"
 * @scr.reference name="dependency.mgt.service" interface=
 * "org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService"
 * cardinality="0..1" policy="dynamic"
 * bind="setDependencyManager" unbind="unsetDependencyManager"
 * @scr.reference name="synapse.registrations.service" interface=
 * "org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService"
 * cardinality="1..n" policy="dynamic"
 * bind="setSynapseRegistrationsService"
 * unbind="unsetSynapseRegistrationsService"
 * @scr.reference name="user.realmservice.default"
 * interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"
 * unbind="unsetRealmService"
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class LoadBalanceEndpointServiceComponent {

    private static final Log log = LogFactory
            .getLog(LoadBalanceEndpointServiceComponent.class);

    private boolean activated = false;

    protected void activate(ComponentContext ctxt) {
        try {
            SynapseEnvironmentService synEnvService = LoadBalancerContext.getInstance().getSynapseEnvironmentService(
                    MultitenantConstants.SUPER_TENANT_ID);

            registerDeployer(LoadBalancerContext.getInstance().getAxisConfiguration(),
                    synEnvService.getSynapseEnvironment());

            SynapseEnvironment synapseEnv = synEnvService
                    .getSynapseEnvironment();

			// Registering Tenant Aware Load Balance Endpoint
            // get the main sequence mediator
            SequenceMediator mainSequence = (SequenceMediator) synapseEnv
                    .getSynapseConfiguration().getSequence("main");

            boolean successfullyRegistered = false;

            // iterate through its child mediators
            for (Mediator child : mainSequence.getList()) {
                // find the InMediator
                if (child instanceof InMediator) {
                    for (Mediator inChild : ((InMediator) child).getList()) {
                        // find the SendMediator
                        if (inChild instanceof SendMediator) {
                            SendMediator sendMediator = (SendMediator) inChild;

							// TODO: Load the endpoint dynamically from the configuration
                            TenantAwareLoadBalanceEndpoint endpoint = new TenantAwareLoadBalanceEndpoint();
                            endpoint.init(synapseEnv);
                            sendMediator.setEndpoint(endpoint);
                            successfullyRegistered = true;

                            if (log.isDebugEnabled()) {
                                log.debug("Added Tenant Aware Endpoint: " + sendMediator.getEndpoint().getName()
                                        + "" + " to Send Mediator.");
                            }
                        }
                    }
                }
            }

            if (!successfullyRegistered) {
                String msg = "Failed to register Tenant Aware Load Balance Endpoint in Send Mediator.";
                log.fatal(msg);
                throw new TenantAwareLoadBalanceEndpointException(msg);
            }

            // Start topic subscriber thread
            TopicSubscriber topicSubscriber = new TopicSubscriber(Constants.TOPOLOGY_TOPIC);
            topicSubscriber.setMessageListener(new TopologyEventMessageReceiver());
            Thread subscriberThread = new Thread(topicSubscriber);
            subscriberThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology event message receiver thread started");
            }

            // Start topology message receiver thread
            Thread receiverThread = new Thread(new TopologyEventMessageDeligator());
            receiverThread.start();
            if (log.isDebugEnabled()) {
                log.debug("Topology message processor thread started");
            }

            activated = true;
            if (log.isDebugEnabled()) {
                log.debug("LoadBalanceEndpointServiceComponent is activated ");
            }
        } catch (Throwable e) {
            log.error("Failed to activate LoadBalanceEndpointServiceComponent", e);
        }
    }

    protected void deactivate(ComponentContext context) {
        try {
            Set<Map.Entry<Integer, SynapseEnvironmentService>> entrySet = LoadBalancerContext
                    .getInstance().getSynapseEnvironmentServices().entrySet();
            for (Map.Entry<Integer, SynapseEnvironmentService> entry : entrySet) {
                unregisterDeployer(entry.getValue().getConfigurationContext()
                        .getAxisConfiguration(), entry.getValue()
                        .getSynapseEnvironment());
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
    private void unregisterDeployer(AxisConfiguration axisConfig,
                                    SynapseEnvironment synapseEnvironment)
            throws TenantAwareLoadBalanceEndpointException {
        if (axisConfig != null) {
            DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig
                    .getConfigurator();
            String synapseConfigPath = ServiceBusUtils
                    .getSynapseConfigAbsPath(synapseEnvironment
                            .getServerContextInformation());
            String endpointDirPath = synapseConfigPath + File.separator
                    + MultiXMLConfigurationBuilder.ENDPOINTS_DIR;
            deploymentEngine.removeDeployer(endpointDirPath,
                    ServiceBusConstants.ARTIFACT_EXTENSION);
        }
    }

    /**
     * Registers the Endpoint deployer.
     *
     * @param axisConfig         AxisConfiguration to which this deployer belongs
     * @param synapseEnvironment SynapseEnvironment to which this deployer belongs
     */
    private void registerDeployer(AxisConfiguration axisConfig,
                                  SynapseEnvironment synapseEnvironment)
            throws TenantAwareLoadBalanceEndpointException {
        SynapseConfiguration synCfg = synapseEnvironment
                .getSynapseConfiguration();
        DeploymentEngine deploymentEngine = (DeploymentEngine) axisConfig
                .getConfigurator();
        SynapseArtifactDeploymentStore deploymentStore = synCfg
                .getArtifactDeploymentStore();

        String synapseConfigPath = ServiceBusUtils
                .getSynapseConfigAbsPath(synapseEnvironment
                        .getServerContextInformation());
        String endpointDirPath = synapseConfigPath + File.separator
                + MultiXMLConfigurationBuilder.ENDPOINTS_DIR;

        for (Endpoint ep : synCfg.getDefinedEndpoints().values()) {
            if (ep.getFileName() != null) {
                deploymentStore.addRestoredArtifact(endpointDirPath
                        + File.separator + ep.getFileName());
            }
        }
        deploymentEngine.addDeployer(new EndpointDeployer(), endpointDirPath,
                ServiceBusConstants.ARTIFACT_EXTENSION);
    }

    protected void setConfigurationContextService(ConfigurationContextService cfgCtxService) {
        LoadBalancerContext.getInstance().setAxisConfiguration(cfgCtxService.getServerConfigContext().getAxisConfiguration());
        LoadBalancerContext.getInstance().setConfigCtxt(cfgCtxService.getServerConfigContext());
    }

    protected void unsetConfigurationContextService(ConfigurationContextService cfgCtxService) {
        LoadBalancerContext.getInstance().setAxisConfiguration(null);
        LoadBalancerContext.getInstance().setConfigCtxt(null);
    }

    protected void setSynapseConfigurationService(SynapseConfigurationService synapseConfigurationService) {
        LoadBalancerContext.getInstance().setSynapseConfiguration(synapseConfigurationService.getSynapseConfiguration());
    }

    protected void unsetSynapseConfigurationService(SynapseConfigurationService synapseConfigurationService) {
        LoadBalancerContext.getInstance().setSynapseConfiguration(null);
    }

    /**
     * Here we receive an event about the creation of a SynapseEnvironment. If
     * this is SuperTenant we have to wait until all the other constraints are
     * met and actual initialization is done in the activate method. Otherwise
     * we have to do the activation here.
     *
     * @param synapseEnvironmentService SynapseEnvironmentService which contains information about the
     *                                  new Synapse Instance
     */
    protected void setSynapseEnvironmentService(SynapseEnvironmentService synapseEnvironmentService) {
        boolean alreadyCreated = LoadBalancerContext.getInstance()
                .getSynapseEnvironmentServices()
                .containsKey(synapseEnvironmentService.getTenantId());

        LoadBalancerContext.getInstance().addSynapseEnvironmentService(
                synapseEnvironmentService.getTenantId(),
                synapseEnvironmentService);
        if (activated) {
            if (!alreadyCreated) {
                try {
                    registerDeployer(synapseEnvironmentService
                            .getConfigurationContext().getAxisConfiguration(),
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
     * Here we receive an event about Destroying a SynapseEnvironment. This can
     * be the super tenant destruction or a tenant destruction.
     *
     * @param synapseEnvironmentService synapseEnvironment
     */
    protected void unsetSynapseEnvironmentService(
            SynapseEnvironmentService synapseEnvironmentService) {
        LoadBalancerContext.getInstance().removeSynapseEnvironmentService(
                synapseEnvironmentService.getTenantId());
    }

    protected void setRegistryService(RegistryService regService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService bound to the endpoint component");
        }
        try {
            LoadBalancerContext.getInstance().setConfigRegistry(
                    regService.getConfigSystemRegistry());
            LoadBalancerContext.getInstance().setGovernanceRegistry(
                    regService.getGovernanceSystemRegistry());
        } catch (RegistryException e) {
            log.error("Couldn't retrieve the registry from the registry service");
        }
    }

    protected void unsetRegistryService(RegistryService regService) {
        if (log.isDebugEnabled()) {
            log.debug("RegistryService unbound from the endpoint component");
        }
        LoadBalancerContext.getInstance().setConfigRegistry(null);
    }

    protected void setDependencyManager(
            DependencyManagementService dependencyMgr) {
        if (log.isDebugEnabled()) {
            log.debug("Dependency management service bound to the endpoint component");
        }
        LoadBalancerContext.getInstance().setDependencyManager(dependencyMgr);
    }

    protected void unsetDependencyManager(
            DependencyManagementService dependencyMgr) {
        if (log.isDebugEnabled()) {
            log.debug("Dependency management service unbound from the endpoint component");
        }
        LoadBalancerContext.getInstance().setDependencyManager(null);
    }

    protected void setSynapseRegistrationsService(
            SynapseRegistrationsService synapseRegistrationsService) {

    }

    protected void unsetSynapseRegistrationsService(
            SynapseRegistrationsService synapseRegistrationsService) {
        int tenantId = synapseRegistrationsService.getTenantId();
        if (LoadBalancerContext.getInstance().getSynapseEnvironmentServices()
                .containsKey(tenantId)) {
            SynapseEnvironment env = LoadBalancerContext.getInstance()
                    .getSynapseEnvironmentService(tenantId)
                    .getSynapseEnvironment();

            LoadBalancerContext.getInstance().removeSynapseEnvironmentService(
                    synapseRegistrationsService.getTenantId());

            AxisConfiguration axisConfig = synapseRegistrationsService
                    .getConfigurationContext().getAxisConfiguration();
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
        LoadBalancerContext.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        LoadBalancerContext.getInstance().setRealmService(null);
    }
}
