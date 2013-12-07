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
import org.apache.stratos.load.balancer.ClusterContext;
import org.apache.stratos.load.balancer.ServiceContext;
import org.apache.stratos.load.balancer.TenantAwareLoadBalanceEndpointException;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.synapse.config.SynapseConfiguration;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
    // <TenantId, SynapseEnvironmentService> Map
    private Map<Integer, SynapseEnvironmentService> tenantIdSynapseEnvironmentServiceMap;

    // Following maps are updated on demand by the request delegator.
    // <ServiceName, ServiceContext> Map
    private Map<String, ServiceContext> serviceNameServiceContextMap;
    // <ClusterId, ClusterContext> Map
    private Map<String, ClusterContext> clusterIdClusterContextMap;

    // Following maps are updated by load balancer topology receiver.
    // <ClusterId, Cluster> Map
    private Map<String, Cluster> clusterIdClusterMap;
    // <Hostname, Cluster> Map
    private Map<String, Cluster> singleTenantClusterMap;
    // <Hostname, Map<TenantId, Cluster>> Map
    private Map<String, Map<Integer, Cluster>> multiTenantClusterMap;

    private LoadBalancerContext() {
        tenantIdSynapseEnvironmentServiceMap = new ConcurrentHashMap<Integer, SynapseEnvironmentService>();
        serviceNameServiceContextMap = new ConcurrentHashMap<String, ServiceContext>();
        clusterIdClusterContextMap = new ConcurrentHashMap<String, ClusterContext>();
        clusterIdClusterMap = new ConcurrentHashMap<String, Cluster>();
        singleTenantClusterMap = new ConcurrentHashMap<String, Cluster>();
        multiTenantClusterMap = new ConcurrentHashMap<String, Map<Integer, Cluster>>();
    }

    public static LoadBalancerContext getInstance() {
        if (instance == null) {
            synchronized (LoadBalancerContext.class){
                if (instance == null) {
                    instance = new LoadBalancerContext ();
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

    public SynapseEnvironmentService getSynapseEnvironmentService(int tenantId) {
        return tenantIdSynapseEnvironmentServiceMap.get(tenantId);
    }

    public void addSynapseEnvironmentService(int tenantId, SynapseEnvironmentService synapseEnvironmentService) {
        tenantIdSynapseEnvironmentServiceMap.put(tenantId, synapseEnvironmentService);
    }

    public void removeSynapseEnvironmentService(int tenantId) {
        tenantIdSynapseEnvironmentServiceMap.remove(tenantId);
    }

    public Map<Integer, SynapseEnvironmentService> getTenantIdSynapseEnvironmentServiceMap() {
        return tenantIdSynapseEnvironmentServiceMap;
    }

    public ConfigurationContext getConfigCtxt() {
        return configCtxt;
    }

    public void setConfigCtxt(ConfigurationContext configCtxt) {
        this.configCtxt = configCtxt;
    }

    // ServiceNameServiceContextMap methods START
    public Collection<ServiceContext> getServiceContexts() {
        return serviceNameServiceContextMap.values();
    }

    public ServiceContext getServiceContext(String serviceName) {
        return serviceNameServiceContextMap.get(serviceName);
    }

    public void addServiceContext(ServiceContext serviceContext) {
        serviceNameServiceContextMap.put(serviceContext.getServiceName(), serviceContext);
    }

    public void removeServiceContext(String serviceName) {
        serviceNameServiceContextMap.remove(serviceName);
    }
    // ServiceNameServiceContextMap methods END

    // ClusterIdClusterContextMap methods START
    public Collection<ClusterContext> getClusterContexts() {
        return clusterIdClusterContextMap.values();
    }

    public ClusterContext getClusterContext(String clusterId) {
        return clusterIdClusterContextMap.get(clusterId);
    }

    public void addClusterContext(ClusterContext clusterContext) {
        clusterIdClusterContextMap.put(clusterContext.getClusterId(), clusterContext);
    }

    public void removeClusterContext(String clusterId) {
        clusterIdClusterContextMap.remove(clusterId);
    }
    // ClusterIdClusterContextMap methods END

    // ClusterIdClusterMap methods START
    public Cluster getCluster(String clusterId) {
        return clusterIdClusterMap.get(clusterId);
    }

    public boolean clusterExists(String clusterId) {
        return clusterIdClusterMap.containsKey(clusterId);
    }

    public void addCluster(Cluster cluster) {
        clusterIdClusterMap.put(cluster.getClusterId(), cluster);
    }

    public void removeCluster(String clusterId) {
        clusterIdClusterMap.remove(clusterId);
    }
    // ClusterIdClusterMap methods END

    // SingleTenantClusterMap methods START
    public Cluster getSingleTenantCluster(String hostName) {
        return singleTenantClusterMap.get(hostName);
    }

    public boolean singleTenantClusterExists(String hostName) {
        return singleTenantClusterMap.containsKey(hostName);
    }

    public void addSingleTenantCluster(String hostName, Cluster cluster) {
        singleTenantClusterMap.put(hostName, cluster);
    }

    public void removeSingleTenantCluster(Cluster cluster) {
        for(String hostName : cluster.getHostNames()) {
            removeSingleTenantCluster(hostName);
        }
    }

    public void removeSingleTenantCluster(String hostName) {
        singleTenantClusterMap.remove(hostName);
    }
    // SingleTenantClusterMap methods END

    // MultiTenantClusterMap methods START
    public Cluster getMultiTenantCluster(String hostName, int tenantId) {
        Map<Integer, Cluster> clusterMap = getMultiTenantClusters(hostName);
        if(clusterMap != null) {
            return clusterMap.get(tenantId);
        }
        return null;
    }

    public Map<Integer, Cluster> getMultiTenantClusters(String hostName) {
        if(multiTenantClustersExists(hostName)) {
            return null;
        }
        return multiTenantClusterMap.get(hostName);
    }

    public boolean multiTenantClustersExists(String hostName) {
        return multiTenantClusterMap.containsKey(hostName);
    }

    public void addMultiTenantClusters(String hostname, Map<Integer, Cluster> clusters) {
        multiTenantClusterMap.put(hostname, clusters);
    }

    public void removeMultiTenantClusters(String hostName) {
        multiTenantClusterMap.remove(hostName);
    }
    // MultiTenantClusterMap methods END
}
