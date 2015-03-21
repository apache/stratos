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

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.common.services.DistributedObjectProvider;
import org.apache.stratos.load.balancer.exception.TenantAwareLoadBalanceEndpointException;
import org.apache.synapse.config.SynapseConfiguration;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service reference holder keeps references of bind OSGi services.
 */
public class ServiceReferenceHolder {

    private static final Log log = LogFactory.getLog(ServiceReferenceHolder.class);
    private static volatile ServiceReferenceHolder instance;

    private SynapseConfiguration synapseConfiguration;
    private ConfigurationContext configCtxt;
    private AxisConfiguration axisConfiguration;
    private UserRegistry configRegistry;
    private UserRegistry governanceRegistry;
    private DependencyManagementService dependencyManager;
    private RealmService realmService;
    private DistributedObjectProvider distributedObjectProvider;
    private ConcurrentHashMap<Integer, SynapseEnvironmentService> synapseEnvironmentServiceMap;

    private ServiceReferenceHolder() {
        synapseEnvironmentServiceMap = new ConcurrentHashMap<Integer, SynapseEnvironmentService>();
    }

    public static ServiceReferenceHolder getInstance() {
        if (instance == null) {
            synchronized (ServiceReferenceHolder.class) {
                if (instance == null) {
                    instance = new ServiceReferenceHolder();
                }
            }
        }
        return instance;
    }

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

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
            String message = name + " reference in service reference holder is null";
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

    public ConfigurationContext getConfigCtxt() {
        return configCtxt;
    }

    public void setConfigCtxt(ConfigurationContext configCtxt) {
        this.configCtxt = configCtxt;
    }

    public void setDistributedObjectProvider(DistributedObjectProvider distributedObjectProvider) {
        this.distributedObjectProvider = distributedObjectProvider;
    }

    public DistributedObjectProvider getDistributedObjectProvider() {
        return distributedObjectProvider;
    }

    public SynapseEnvironmentService getSynapseEnvironmentService(int tenantId) {
        return synapseEnvironmentServiceMap.get(tenantId);
    }

    public boolean containsSynapseEnvironmentService(int tenantId) {
        return synapseEnvironmentServiceMap.containsKey(tenantId);
    }

    public void addSynapseEnvironmentService(int tenantId, SynapseEnvironmentService synapseEnvironmentService) {
        synapseEnvironmentServiceMap.put(tenantId, synapseEnvironmentService);
    }

    public void removeSynapseEnvironmentService(int tenantId) {
        synapseEnvironmentServiceMap.remove(tenantId);
    }

    public Collection<SynapseEnvironmentService> getSynapseEnvironmentServices() {
        return synapseEnvironmentServiceMap.values();
    }
}
