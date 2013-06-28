/**
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

package org.wso2.carbon.lb.endpoint.util;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.SynapseConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService;
import org.wso2.carbon.lb.endpoint.TenantAwareLoadBalanceEndpointException;
import org.wso2.carbon.lb.endpoint.TenantLoadBalanceMembershipHandler;
import org.wso2.carbon.mediation.dependency.mgt.services.DependencyManagementService;
import org.wso2.carbon.mediation.initializer.services.SynapseEnvironmentService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.core.service.RealmService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 *
 */
public class ConfigHolder {

    private static ConfigHolder instance;
    private static final Log log = LogFactory.getLog(ConfigHolder.class);

    private SynapseConfiguration synapseConfiguration;
    private ConfigurationContext configCtxt;
    private AxisConfiguration axisConfiguration;
    private UserRegistry configRegistry;
    private UserRegistry governanceRegistry;
    private DependencyManagementService dependencyManager;
    private TenantLoadBalanceMembershipHandler tenantMembershipHandler;
    private LoadBalancerConfigurationService lbConfigService;
    private BlockingQueue<String> sharedTopologyQueue = new LinkedBlockingQueue<String>();
    private String previousMsg;
    

    private Map<Integer, SynapseEnvironmentService> synapseEnvironmentServices =
            new HashMap<Integer, SynapseEnvironmentService>();

    public RealmService getRealmService() {
        return realmService;
    }

    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    private RealmService realmService;

    private ConfigHolder() {
    }

    public static ConfigHolder getInstance() {
        if (instance == null) {
            instance = new ConfigHolder();
        }
        return instance;
    }

    public SynapseConfiguration getSynapseConfiguration() throws TenantAwareLoadBalanceEndpointException{
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

    public SynapseEnvironmentService getSynapseEnvironmentService(int id) {
        return synapseEnvironmentServices.get(id);
    }

    public void addSynapseEnvironmentService(int id,
                                             SynapseEnvironmentService synapseEnvironmentService) {
        synapseEnvironmentServices.put(id, synapseEnvironmentService);
    }

    public void removeSynapseEnvironmentService(int id) {
        synapseEnvironmentServices.remove(id);
    }

    public Map<Integer, SynapseEnvironmentService> getSynapseEnvironmentServices() {
        return synapseEnvironmentServices;
    }
    
    public void setTenantLoadBalanceMembershipHandler(TenantLoadBalanceMembershipHandler handler) {
        tenantMembershipHandler = handler;
    }
    
    public TenantLoadBalanceMembershipHandler getTenantLoadBalanceMembershipHandler() {
        return tenantMembershipHandler;
    }

    public ConfigurationContext getConfigCtxt() {
        return configCtxt;
    }

    public void setConfigCtxt(ConfigurationContext configCtxt) {
        this.configCtxt = configCtxt;
    }
    
    public void setLbConfigService(LoadBalancerConfigurationService lbConfigSer) {
        this.lbConfigService = lbConfigSer;
    }

    public LoadBalancerConfiguration getLbConfig() {
        return (LoadBalancerConfiguration) lbConfigService.getLoadBalancerConfig();
    }

	public BlockingQueue<String> getSharedTopologyDiffQueue() {
	    return sharedTopologyQueue;
    }

	public void setSharedTopologyDiffQueue(BlockingQueue<String> sharedTopologyDiffQueue) {
	    this.sharedTopologyQueue = sharedTopologyDiffQueue;
    }

	public String getPreviousMsg() {
	    return previousMsg;
    }

	public void setPreviousMsg(String previousMsg) {
	    this.previousMsg = previousMsg;
    }

}