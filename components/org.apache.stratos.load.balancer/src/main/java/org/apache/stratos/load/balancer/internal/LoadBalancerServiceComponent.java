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

package org.apache.stratos.load.balancer.internal;

import org.apache.axis2.deployment.DeploymentEngine;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.EndpointDeployer;
import org.apache.stratos.load.balancer.LoadBalancerTenantEventReceiver;
import org.apache.stratos.load.balancer.LoadBalancerTopologyEventReceiver;
import org.apache.stratos.load.balancer.TenantAwareLoadBalanceEndpointException;
import org.apache.stratos.load.balancer.common.statistics.LoadBalancerStatisticsReader;
import org.apache.stratos.load.balancer.common.statistics.notifier.LoadBalancerStatisticsNotifier;
import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;
import org.apache.stratos.load.balancer.conf.configurator.CEPConfigurator;
import org.apache.stratos.load.balancer.conf.configurator.SynapseConfigurator;
import org.apache.stratos.load.balancer.conf.configurator.TopologyFilterConfigurator;
import org.apache.stratos.load.balancer.context.LoadBalancerContext;
import org.apache.stratos.load.balancer.statistics.LoadBalancerStatisticsCollector;
import org.apache.stratos.messaging.message.filter.topology.TopologyClusterFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyMemberFilter;
import org.apache.stratos.messaging.message.filter.topology.TopologyServiceFilter;
import org.apache.synapse.config.SynapseConfiguration;
import org.apache.synapse.config.xml.MultiXMLConfigurationBuilder;
import org.apache.synapse.core.SynapseEnvironment;
import org.apache.synapse.deployers.SynapseArtifactDeploymentStore;
import org.apache.synapse.endpoints.Endpoint;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.caching.impl.DistributedMapProvider;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.mediation.initializer.ServiceBusConstants;
import org.wso2.carbon.mediation.initializer.ServiceBusUtils;
import org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.File;
import java.util.Map;
import java.util.Set;

/**
 * @scr.component name="org.apache.stratos.load.balancer.internal.LoadBalancerServiceComponent" immediate="true"
 * @scr.reference name="distributedMapProvider" interface="org.wso2.carbon.caching.impl.DistributedMapProvider"
 *                cardinality="1..1" policy="dynamic" bind="setDistributedMapProvider" unbind="unsetDistributedMapProvider"
 * @scr.reference name="configuration.context.service" interface="org.wso2.carbon.utils.ConfigurationContextService"
 *                cardinality="1..1" policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="synapse.config.service" interface="org.wso2.carbon.mediation.initializer.services.SynapseConfigurationService"
 *                cardinality="1..1" policy="dynamic" bind="setSynapseConfigurationService" unbind="unsetSynapseConfigurationService"
 * @scr.reference name="synapse.env.service" interface="org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService"
 *                cardinality="1..n" policy="dynamic" bind="setSynapseEnvironmentService" unbind="unsetSynapseEnvironmentService"
 * @scr.reference name="registry.service" interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                cardinality="1..1" policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="dependency.mgt.service" interface="org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService"
 *                cardinality="0..1" policy="dynamic" bind="setDependencyManager" unbind="unsetDependencyManager"
 * @scr.reference name="synapse.registrations.service" interface="org.wso2.carbon.mediation.initializer.services.SynapseRegistrationsService"
 *                cardinality="1..n" policy="dynamic" bind="setSynapseRegistrationsService" unbind="unsetSynapseRegistrationsService"
 * @scr.reference name="user.realmservice.default" interface="org.wso2.carbon.user.core.service.RealmService"
 *                cardinality="1..1" policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class LoadBalancerServiceComponent {
    private static final Log log = LogFactory.getLog(LoadBalancerServiceComponent.class);

    private boolean activated = false;
    private LoadBalancerTopologyEventReceiver topologyReceiver;
    private LoadBalancerTenantEventReceiver tenantReceiver;
    private LoadBalancerStatisticsNotifier statisticsNotifier;

    protected void activate(ComponentContext ctxt) {
        try {
            // Register endpoint deployer
            SynapseEnvironmentService synEnvService = LoadBalancerContext.getInstance().getTenantIdSynapseEnvironmentServiceMap()
                    .getSynapseEnvironmentService(MultitenantConstants.SUPER_TENANT_ID);
            registerDeployer(LoadBalancerContext.getInstance().getAxisConfiguration(),
                    synEnvService.getSynapseEnvironment());

            // Configure synapse settings
            LoadBalancerConfiguration configuration = LoadBalancerConfiguration.getInstance();
            SynapseConfigurator.configure(configuration);

            // Configure cep settings
            CEPConfigurator.configure(configuration);

            // Configure topology filters
            TopologyFilterConfigurator.configure(configuration);

            if (configuration.isMultiTenancyEnabled()) {

                tenantReceiver = new LoadBalancerTenantEventReceiver();
                Thread tenantReceiverThread = new Thread(tenantReceiver);
                tenantReceiverThread.start();
                if (log.isInfoEnabled()) {
                    log.info("Tenant receiver thread started");
                }
            }

            if (configuration.isTopologyEventListenerEnabled()) {

                // Start topology receiver
                topologyReceiver = new LoadBalancerTopologyEventReceiver();
                Thread topologyReceiverThread = new Thread(topologyReceiver);
                topologyReceiverThread.start();
                if (log.isInfoEnabled()) {
                    log.info("Topology receiver thread started");
                }

                if (log.isInfoEnabled()) {
                    if (TopologyServiceFilter.getInstance().isActive()) {
                        StringBuilder sb = new StringBuilder();
                        for (String serviceName : TopologyServiceFilter.getInstance().getIncludedServiceNames()) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(serviceName);
                        }
                        log.info(String.format("Service filter activated: [services] %s", sb.toString()));
                    }
                    if (TopologyClusterFilter.getInstance().isActive()) {
                        StringBuilder sb = new StringBuilder();
                        for (String clusterId : TopologyClusterFilter.getInstance().getIncludedClusterIds()) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(clusterId);
                        }
                        log.info(String.format("Cluster filter activated: [clusters] %s", sb.toString()));
                    }
                    if (TopologyMemberFilter.getInstance().isActive()) {
                        StringBuilder sb = new StringBuilder();
                        for (String clusterId : TopologyMemberFilter.getInstance().getIncludedLbClusterIds()) {
                            if (sb.length() > 0) {
                                sb.append(", ");
                            }
                            sb.append(clusterId);
                        }
                        log.info(String.format("Member filter activated: [lb-cluster-ids] %s", sb.toString()));
                    }
                }
            }

            if(configuration.isCepStatsPublisherEnabled()) {
                // Get stats reader
                LoadBalancerStatisticsReader statsReader = LoadBalancerStatisticsCollector.getInstance();

                // Start stats notifier thread
                statisticsNotifier = new LoadBalancerStatisticsNotifier(statsReader);
                Thread statsNotifierThread = new Thread(statisticsNotifier);
                statsNotifierThread.start();
                if (log.isInfoEnabled()) {
                    log.info("Load balancer statistics notifier thread started");
                }
            }

            activated = true;
            if (log.isInfoEnabled()) {
                log.info("Load balancer service component is activated ");
            }
        } catch (Exception e) {
            if (log.isFatalEnabled()) {
                log.fatal("Failed to activate load balancer service component", e);
            }
        }
    }

    protected void deactivate(ComponentContext context) {
        try {
            Set<Map.Entry<Integer, SynapseEnvironmentService>> entrySet = LoadBalancerContext
                    .getInstance().getTenantIdSynapseEnvironmentServiceMap().getTenantIdSynapseEnvironmentServiceMap().entrySet();
            for (Map.Entry<Integer, SynapseEnvironmentService> entry : entrySet) {
                unregisterDeployer(entry.getValue().getConfigurationContext()
                        .getAxisConfiguration(), entry.getValue()
                        .getSynapseEnvironment());
            }
        } catch (Exception e) {
            log.warn("Couldn't remove the endpoint deployer");
        }
        // Terminate tenant receiver
        tenantReceiver.terminate();
        // Terminate topology receiver
        topologyReceiver.terminate();
        // Terminate statistics notifier
        statisticsNotifier.terminate();
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
                .getTenantIdSynapseEnvironmentServiceMap()
                .containsSynapseEnvironmentService(synapseEnvironmentService.getTenantId());

        LoadBalancerContext.getInstance().getTenantIdSynapseEnvironmentServiceMap()
                .addSynapseEnvironmentService(synapseEnvironmentService.getTenantId(),
                        synapseEnvironmentService);
        if (activated) {
            if (!alreadyCreated) {
                try {
                    registerDeployer(synapseEnvironmentService
                            .getConfigurationContext().getAxisConfiguration(),
                            synapseEnvironmentService.getSynapseEnvironment());
                    if (log.isDebugEnabled()) {
                        log.debug("Endpoint admin bundle is activated ");
                    }
                } catch (Throwable e) {
                    log.error("Failed to activate endpoint admin bundle ", e);
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
        LoadBalancerContext.getInstance().getTenantIdSynapseEnvironmentServiceMap().removeSynapseEnvironmentService(
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
        if (LoadBalancerContext.getInstance().getTenantIdSynapseEnvironmentServiceMap()
                .containsSynapseEnvironmentService(tenantId)) {
            SynapseEnvironment env = LoadBalancerContext.getInstance()
                    .getTenantIdSynapseEnvironmentServiceMap()
                    .getSynapseEnvironmentService(tenantId)
                    .getSynapseEnvironment();

            LoadBalancerContext.getInstance().getTenantIdSynapseEnvironmentServiceMap()
                    .removeSynapseEnvironmentService(
                            synapseRegistrationsService.getTenantId());

            AxisConfiguration axisConfig = synapseRegistrationsService
                    .getConfigurationContext().getAxisConfiguration();
            if (axisConfig != null) {
                try {
                    unregisterDeployer(axisConfig, env);
                } catch (Exception e) {
                    log.warn("Couldn't remove the endpoint deployer");
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

    protected void setDistributedMapProvider(DistributedMapProvider mapProvider) {
        LoadBalancerContext.getInstance().setDistributedMapProvider(mapProvider);
    }

    protected void unsetDistributedMapProvider(DistributedMapProvider mapProvider) {
        LoadBalancerContext.getInstance().setDistributedMapProvider(null);
    }
}
