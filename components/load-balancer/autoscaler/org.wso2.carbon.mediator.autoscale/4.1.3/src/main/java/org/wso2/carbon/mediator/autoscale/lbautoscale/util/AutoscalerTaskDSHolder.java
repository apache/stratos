/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.mediator.autoscale.lbautoscale.util;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.LBConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.lb.common.service.LoadBalancerConfigurationService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.cloud.controller.interfaces.CloudControllerService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * Singleton class to hold Agent Management Service
 */
public class AutoscalerTaskDSHolder {
    
    private ConfigurationContextService configurationContextService;
    private LoadBalancerConfiguration wholeLbConfig;
    private CloudControllerService cloudControllerService;  

    private RealmService realmService;
    private ClusteringAgent agent;
    private ConfigurationContext configCtxt;
    private UserRegistry configRegistry;
    private UserRegistry governanceRegistry;

    private static AutoscalerTaskDSHolder instance = new AutoscalerTaskDSHolder();

    private AutoscalerTaskDSHolder(){

    }

    public static AutoscalerTaskDSHolder getInstance(){
        return instance;
    }

    public ConfigurationContextService getConfigurationContextServiceService(){
        return this.configurationContextService;
    }

    public void setConfigurationContextService(ConfigurationContextService cCtxService){
        this.configurationContextService = cCtxService;
    }
    
    public LoadBalancerConfiguration getWholeLoadBalancerConfig() {
        return wholeLbConfig;
    }
    
    public LBConfiguration getLoadBalancerConfig() {
        return wholeLbConfig.getLoadBalancerConfig();
    }

    public ClusteringAgent getAgent() {
        return agent;
    }

    public void setAgent(ClusteringAgent agent) {
        this.agent = agent;
    }


    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }

    public RealmService getRealmService() {
        return realmService;
    }
    
    public void setLbConfigService(LoadBalancerConfigurationService lbConfigSer) {
        if (lbConfigSer != null) {
            this.wholeLbConfig = (LoadBalancerConfiguration) lbConfigSer.getLoadBalancerConfig();
        } else {
            this.wholeLbConfig = null;
        }
    }

	public void setConfigCtxt(ConfigurationContext configCtxt) {
		this.configCtxt = configCtxt;
	}

	public ConfigurationContext getConfigCtxt() {
		return configCtxt;
	}

	public void setCloudControllerService(CloudControllerService cc) {
        this.cloudControllerService = cc;
    }
	
	public CloudControllerService getCloudControllerService() {
        return cloudControllerService;
    }

	public UserRegistry getConfigRegistry() {
        return configRegistry;
    }

    public void setConfigRegistry(UserRegistry configRegistry) {
        this.configRegistry = configRegistry;
    }
    
    public UserRegistry getGovernanceRegistry() {
        return governanceRegistry;
    }

    public void setGovernanceRegistry(UserRegistry governanceRegistry) {
        this.governanceRegistry = governanceRegistry;
    }

}
