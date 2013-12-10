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

package org.apache.stratos.load.balancer.context;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.load.balancer.TenantAwareLoadBalanceEndpointException;
import org.apache.stratos.load.balancer.context.map.*;
import org.apache.synapse.config.SynapseConfiguration;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * Defines load balancer context information.
 */
public class LoadBalancerContext {

    private static final Log log = LogFactory.getLog(LoadBalancerContext.class);
    private static volatile LoadBalancerContext instance;

    private SynapseConfiguration synapseConfiguration;
    private ConfigurationContext configCtxt;
    private AxisConfiguration axisConfiguration;
    private UserRegistry configRegistry;
    private UserRegistry governanceRegistry;
    private DependencyManagementService dependencyManager;

    // Following map is updated by the service component.
    // Map<TenantId, SynapseEnvironmentService>
    private TenantIdSynapseEnvironmentServiceMap tenantIdSynapseEnvironmentServiceMap;

    // Following maps are updated on demand by the request delegator.
    // Map<ServiceName, ServiceContext>
    private ServiceNameServiceContextMap serviceNameServiceContextMap;
    // Map<ClusterId, ClusterContext>
    private ClusterIdClusterContextMap clusterIdClusterContextMap;

    // Following maps are updated by load balancer topology & tenant receivers.
    // Map<ClusterId, Cluster>
    // Keep track of all clusters
    private ClusterIdClusterMap clusterIdClusterMap;
    // Map<HostName, Cluster>
    // Keep tack of all clusters
    private HostNameClusterMap hostNameClusterMap;
    // Map<HostName, Map<TenantId, Cluster>>
    // Keep track of multi-tenant service clusters
    private MultiTenantClusterMap multiTenantClusterMap;

    private LoadBalancerContext() {
        tenantIdSynapseEnvironmentServiceMap = new TenantIdSynapseEnvironmentServiceMap();
        serviceNameServiceContextMap = new ServiceNameServiceContextMap();
        clusterIdClusterContextMap = new ClusterIdClusterContextMap();
        clusterIdClusterMap = new ClusterIdClusterMap();
        hostNameClusterMap = new HostNameClusterMap();
        multiTenantClusterMap = new MultiTenantClusterMap();
    }

    public static LoadBalancerContext getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerContext.class) {
                if (instance == null) {
                    instance = new LoadBalancerContext();
                }
            }
        }
        return instance;
    }

    public void clear() {
        tenantIdSynapseEnvironmentServiceMap.clear();
        serviceNameServiceContextMap.clear();
        clusterIdClusterContextMap.clear();
        multiTenantClusterMap.clear();
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    private RealmService realmService;

    public SynapseConfiguration getSynapseConfiguration() throws TenantAwareLoadBalanceEndpointException {
        assertNull("SynapseConfiguration", synapseConfiguration);
        return synapseConfiguration;
    }

    public void setSynapseConfiguration(SynapseConfiguration synapseConfiguration) {
        this.synapseConfiguration = synapseConfiguration;
    }

    public AxisConfiguration getAxisConfiguration() throws TenantAwareLoadBalanceEndpointException {
        assertNull("AxisConfiguration", axisConfiguration);
        return axisConfiguration;
    }

    public void setAxisConfiguration(AxisConfiguration axisConfiguration) {
        this.axisConfiguration = axisConfiguration;
    }

    public UserRegistry getConfigRegistry() throws TenantAwareLoadBalanceEndpointException {
        assertNull("Registry", configRegistry);
        return configRegistry;
    }

    public void setConfigRegistry(UserRegistry configRegistry) {
        this.configRegistry = configRegistry;
    }

    public DependencyManagementService getDependencyManager() {
        return dependencyManager;
    }

    public void setDependencyManager(DependencyManagementService dependencyManager) {
        this.dependencyManager = dependencyManager;
    }

    private void assertNull(String name, Object object) throws TenantAwareLoadBalanceEndpointException {
        if (object == null) {
            String message = name + " reference in the proxy admin config holder is null";
            log.error(message);
            throw new TenantAwareLoadBalanceEndpointException(message);
        }
    }

    public UserRegistry getGovernanceRegistry() {
        return governanceRegistry;
    }

    public void setGovernanceRegistry(UserRegistry governanceRegistry) {
        this.governanceRegistry = governanceRegistry;
    }

    public TenantIdSynapseEnvironmentServiceMap getTenantIdSynapseEnvironmentServiceMap() {
        return tenantIdSynapseEnvironmentServiceMap;
    }

    public ConfigurationContext getConfigCtxt() {
        return configCtxt;
    }

    public void setConfigCtxt(ConfigurationContext configCtxt) {
        this.configCtxt = configCtxt;
    }

    public ServiceNameServiceContextMap getServiceNameServiceContextMap() {
        return serviceNameServiceContextMap;
    }

    public ClusterIdClusterContextMap getClusterIdClusterContextMap() {
        return clusterIdClusterContextMap;
    }

    public ClusterIdClusterMap getClusterIdClusterMap() {
        return clusterIdClusterMap;
    }

    public HostNameClusterMap getHostNameClusterMap() {
        return hostNameClusterMap;
    }

    public MultiTenantClusterMap getMultiTenantClusterMap() {
        return multiTenantClusterMap;
    }
}
