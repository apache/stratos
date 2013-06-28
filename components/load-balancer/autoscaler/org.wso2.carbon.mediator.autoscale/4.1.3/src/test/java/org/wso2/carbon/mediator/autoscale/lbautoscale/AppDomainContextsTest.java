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
package org.wso2.carbon.mediator.autoscale.lbautoscale;

import java.io.File;
import java.util.Iterator;
import java.util.Map;

import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.axis2.clustering.tribes.TribesClusteringAgent;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.lb.common.group.mgt.SubDomainAwareGroupManagementAgent;
import org.wso2.carbon.mediator.autoscale.lbautoscale.context.AppDomainContext;
import org.wso2.carbon.mediator.autoscale.lbautoscale.util.AutoscaleUtil;

import junit.framework.Assert;
import junit.framework.TestCase;

public class AppDomainContextsTest extends TestCase {

    private static Map<String, Map<String, ?>> map;
    private LoadBalancerConfiguration lbConfig;
    ConfigurationContext configCtx;
    ClusteringAgent clusteringAgent;
    
    protected void setUp() throws Exception {
        super.setUp();
        configCtx = ConfigurationContextFactory.createEmptyConfigurationContext();
        clusteringAgent = new TribesClusteringAgent();
        clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(
                    "worker"),
                    "wso2.as1.domain", "worker", -1);
        clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(
                "mgt"),
                "wso2.as1.domain", "mgt", -1);
        clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(
                "mgt"),
                "wso2.as2.domain", "mgt", -1);
        configCtx.getAxisConfiguration().setClusteringAgent(clusteringAgent);
        
        File f = new File("src/test/resources/loadbalancer.conf");
        System.setProperty("loadbalancer.conf", f.getAbsolutePath());
        lbConfig = LoadBalancerConfiguration.getInstance();
        
        map = AutoscaleUtil.getAppDomainContexts(configCtx, lbConfig);
        
    }
    
    
    public void testRemoval(){
        // removing a cluster domain with only 1 sub domain 
        lbConfig.removeServiceConfiguration("wso2.as2.domain", "mgt");
        map = AutoscaleUtil.getAppDomainContexts(configCtx, lbConfig);
        
        Assert.assertEquals(true, !map.containsKey("wso2.as2.domain"));
        
        // removing a cluster domain with more than 1 sub domain
        lbConfig.removeServiceConfiguration("wso2.as1.domain", "mgt");
        map = AutoscaleUtil.getAppDomainContexts(configCtx, lbConfig);
        
        Assert.assertEquals(true, map.containsKey("wso2.as1.domain"));
        Assert.assertEquals(true, map.get("wso2.as1.domain").get("mgt") == null);
        Assert.assertEquals(true, map.get("wso2.as1.domain").get("worker") != null);
    }
    
    public void testAddition(){
        ServiceConfiguration config1 = lbConfig.new ServiceConfiguration();
        config1.setDomain("wso2.as3.domain");
        config1.setSub_domain("mgt");
        lbConfig.addServiceConfiguration(config1);
        clusteringAgent.addGroupManagementAgent(new SubDomainAwareGroupManagementAgent(
                "mgt"),
                "wso2.as3.domain", "mgt", -1);
        map = AutoscaleUtil.getAppDomainContexts(configCtx, lbConfig);
        
        Assert.assertEquals(true, map.containsKey("wso2.as3.domain"));
        Assert.assertEquals(true, map.get("wso2.as3.domain").get("mgt") != null);
    }

    @Deprecated // use only for writing test cases
    void printKeys(Map<?,?> map){
        for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
            Object type = iterator.next();
            System.out.println(type);
        }
    }
}
