/*
 * Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.lb.common.test;

import java.io.File;
import java.util.List;

import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration;
import org.wso2.carbon.lb.common.conf.LoadBalancerConfiguration.ServiceConfiguration;
import org.wso2.carbon.lb.common.conf.util.HostContext;

import junit.framework.TestCase;

public class LoadBalancerConfigurationTest extends TestCase {
    
    private LoadBalancerConfiguration lbConfig ;
    private LoadBalancerConfiguration lbConfig1;
    
    
    @Override
    protected void setUp() throws Exception {

        LoadBalancerConfiguration.setInstance(null);
        File f = new File("src/test/resources/loadbalancer.conf");
        System.setProperty("loadbalancer.conf", f.getAbsolutePath());
        lbConfig = LoadBalancerConfiguration.getInstance();
    }
    
    public final void testCreateLoadBalancerConfig() {

        LoadBalancerConfiguration.LBConfiguration loadBalancerConfig =
            lbConfig.getLoadBalancerConfig();
        
        assertEquals(1, loadBalancerConfig.getInstances());
        assertEquals(5000, loadBalancerConfig.getAutoscalerTaskInterval());
        assertEquals(15000, loadBalancerConfig.getServerStartupDelay());
    }

    public final void testCreateServicesConfig() {

        /* Tests relavant to loadbalancer.conf file */
        
        ServiceConfiguration asServiceConfig =
                                               lbConfig.getServiceConfig("wso2.as1.domain",
                                                                         "worker");

        assertEquals(1, asServiceConfig.getInstancesPerScaleUp());
        assertEquals(5, asServiceConfig.getMaxAppInstances());
        assertEquals(0, asServiceConfig.getMinAppInstances());
        assertEquals(60000, asServiceConfig.getMessageExpiryTime());
        assertEquals(400, asServiceConfig.getMaxRequestsPerSecond());
        assertEquals(0.65, asServiceConfig.getAlarmingUpperRate());
        assertEquals(10, asServiceConfig.getRoundsToAverage());
        assertEquals("worker", asServiceConfig.getSubDomain());

        asServiceConfig = lbConfig.getServiceConfig("wso2.as2.domain", "worker1");
        assertEquals("worker1", asServiceConfig.getSubDomain());

        asServiceConfig = lbConfig.getServiceConfig("wso2.esb.domain", "mgt");
        assertEquals("mgt", asServiceConfig.getSubDomain());

        assertEquals(2, lbConfig.getHostNamesTracker().keySet().size());
        assertEquals(3, lbConfig.getHostNamesTracker().get("appserver").size());
        assertEquals(2, lbConfig.getHostNamesTracker().get("esb").size());

        for (HostContext ctx : lbConfig.getHostContextMap().values()) {

            if (ctx.getHostName().equals("appserver.cloud-test.wso2.com")) {

                assertEquals("nirmal", ctx.getSubDomainFromTenantId(30));
                assertEquals(18, ctx.getTenantDomainContexts().size());
            } else if (ctx.getHostName().equals("as2.cloud-test.wso2.com")) {
                assertEquals("worker", ctx.getSubDomainFromTenantId(2));
            } else if (ctx.getHostName().equals("esb.cloud-test.wso2.com")) {
                assertEquals("mgt", ctx.getSubDomainFromTenantId(5));
            }
        }
        
        /* tests relevant to loadbalancer1.conf file */
        
        File f = new File("src/test/resources/loadbalancer2.conf");
        System.setProperty("loadbalancer.conf", f.getAbsolutePath());
        
        LoadBalancerConfiguration.setInstance(null);
        lbConfig1 = LoadBalancerConfiguration.getInstance();
        
        for (HostContext ctx : lbConfig1.getHostContextMap().values()) {

            if (ctx.getHostName().equals("appserver.cloud-test.wso2.com")) {

                assertEquals("nirmal", ctx.getSubDomainFromTenantId(30));
                assertEquals("wso2.as1.domain", ctx.getDomainFromTenantId(5));
                assertEquals("wso2.as.domain", ctx.getDomainFromTenantId(8));
                assertEquals("wso2.as.domain", ctx.getDomainFromTenantId(2));
                assertEquals(4, ctx.getTenantDomainContexts().size());
                
            } else if (ctx.getHostName().equals("esb.cloud-test.wso2.com")) {
                
                assertEquals("mgt", ctx.getSubDomainFromTenantId(5));
            }
        }

    }

    public final void testGetServiceDomains() throws Exception {

        setUp();
        String[] serviceDomains = lbConfig.getServiceDomains();
        assertEquals(4, serviceDomains.length);
        
        assertTrue("wso2.as1.domain".equals(serviceDomains[0]) ||
            "wso2.as1.domain".equals(serviceDomains[1]) ||
            "wso2.as1.domain".equals(serviceDomains[2]) ||
            "wso2.as1.domain".equals(serviceDomains[3]));
        
        assertTrue("wso2.as2.domain".equals(serviceDomains[0]) ||
            "wso2.as2.domain".equals(serviceDomains[1]) ||
            "wso2.as2.domain".equals(serviceDomains[2]) ||
            "wso2.as2.domain".equals(serviceDomains[3]));
        
        assertTrue("wso2.as3.domain".equals(serviceDomains[0]) ||
            "wso2.as3.domain".equals(serviceDomains[1]) ||
            "wso2.as3.domain".equals(serviceDomains[2]) ||
            "wso2.as3.domain".equals(serviceDomains[3]));
        
        assertTrue("wso2.esb.domain".equals(serviceDomains[0]) ||
                   "wso2.esb.domain".equals(serviceDomains[1]) ||
                   "wso2.esb.domain".equals(serviceDomains[2]) ||
                   "wso2.esb.domain".equals(serviceDomains[3]));
        
    }
    
    public final void testGetServiceSubDomains() throws Exception {

        setUp();
        String[] serviceSubDomains = lbConfig.getServiceSubDomains("wso2.as3.domain");
        assertEquals(2, serviceSubDomains.length);
        
        assertTrue("nirmal".equals(serviceSubDomains[0]) ||
            "nirmal".equals(serviceSubDomains[1]));
        
        assertTrue("nirmal2".equals(serviceSubDomains[0]) ||
            "nirmal2".equals(serviceSubDomains[1]));
        
        serviceSubDomains = lbConfig.getServiceSubDomains("wso2.esb.domain");
        assertEquals(2, serviceSubDomains.length);
        
        serviceSubDomains = lbConfig.getServiceSubDomains("wso2.as1.domain");
        assertEquals(1, serviceSubDomains.length);
        
        
    }

}
