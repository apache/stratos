/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.load.balancer.test;

import org.apache.stratos.load.balancer.conf.LoadBalancerConfiguration;
import org.apache.stratos.load.balancer.conf.domain.TenantIdentifier;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.net.URL;

/**
 * Test sample load balancer configurations.
 */
@RunWith(JUnit4.class)
public class LoadBalancerConfigurationTest {
    private static String configPath = "/sample/configuration";

    /**
     * Test load balancer configuration parser using different configuration files.
     */
    @Test
    public final void testConfigurationParser() {
        URL resourceUrl = this.getClass().getResource(configPath);
        File folder = new File(resourceUrl.getFile());
        for (File configFile : folder.listFiles()) {
            try {
                System.setProperty("loadbalancer.conf.file", configFile.getAbsolutePath());
                LoadBalancerConfiguration.getInstance();
            } finally {
                LoadBalancerConfiguration.clear();
            }
        }
    }

    /**
     * Test load balancer configuration read from a configuration file.
     */
    @Test
    public void testConfiguration() {
        try {
            String validationError = "Load balancer configuration validation failed";

            URL resourceUrl = this.getClass().getResource(configPath + "/loadbalancer1.conf");
            File configFile = new File(resourceUrl.getFile());
            System.setProperty("loadbalancer.conf.file", configFile.getAbsolutePath());
            LoadBalancerConfiguration configuration = LoadBalancerConfiguration.getInstance();

            Assert.assertEquals(String.format("%s, algorithm not valid", validationError), "round-robin", configuration.getDefaultAlgorithmName());
            Assert.assertTrue(String.format("%s, failover is not true", validationError), configuration.isFailOverEnabled());
            Assert.assertTrue(String.format("%s, session affinity is not true", validationError), configuration.isSessionAffinityEnabled());
            Assert.assertEquals(String.format("%s, session timeout is not valid", validationError), 90000, configuration.getSessionTimeout());
            Assert.assertTrue(String.format("%s, topology event listener is not true", validationError), configuration.isTopologyEventListenerEnabled());
            Assert.assertEquals(String.format("%s, topology service filter is not valid", validationError), "service-name=service-name1,service-name2", configuration.getTopologyServiceFilter());
            Assert.assertEquals(String.format("%s, topology cluster filter is not valid", validationError), "cluster-id=cluster-id1,cluster-id2", configuration.getTopologyClusterFilter());
            Assert.assertEquals(String.format("%s, topology member filter is not valid", validationError), "lb-cluster-id=lb-cluster-id1", configuration.getTopologyMemberFilter());
            Assert.assertTrue(String.format("%s, cep stats publisher is not true", validationError), configuration.isCepStatsPublisherEnabled());
            Assert.assertEquals(String.format("%s, cep ip is not valid", validationError), "localhost", configuration.getCepIp());
            Assert.assertEquals(String.format("%s, cep port is not valid", validationError), 7615, configuration.getCepPort());
            Assert.assertEquals(String.format("%s, network partition id is not valid", validationError), "network-partition-1", configuration.getNetworkPartitionId());
            Assert.assertTrue(String.format("%s, multi-tenancy is not true", validationError), configuration.isMultiTenancyEnabled());
            Assert.assertEquals(String.format("%s, tenant-identifier is not valid", validationError), TenantIdentifier.TenantDomain, configuration.getTenantIdentifier());
            Assert.assertEquals(String.format("%s, tenant-identifier-regex is not valid", validationError), "t/([^/]*)/", configuration.getTenantIdentifierRegexList().get(0));
        } finally {
            LoadBalancerConfiguration.clear();
        }
    }

    /**
     * Test static topology configuration read from a file
     */
    @Test
    public final void testStaticTopology() {
        URL resourceUrl = this.getClass().getResource(configPath + "/loadbalancer2.conf");
        File configFile = new File(resourceUrl.getFile());

        System.setProperty("loadbalancer.conf.file", configFile.getAbsolutePath());
        LoadBalancerConfiguration.getInstance();

        try {
            String validationError = "Static topology validation failed";

            TopologyManager.acquireReadLock();
            Topology topology = TopologyManager.getTopology();
            Assert.assertTrue(String.format("%s, services not found", validationError), topology.getServices().size() > 0);

            String serviceName = "app-server";
            Service appServer = topology.getService(serviceName);
            Assert.assertNotNull(String.format("%s, service not found: [service] %s", validationError, serviceName), appServer);

            Assert.assertTrue(String.format("%s, multi-tenant is not true: [service] %s", validationError, serviceName), appServer.getServiceType() == ServiceType.MultiTenant);

            String clusterId = "app-server-cluster1";
            Cluster cluster1 = appServer.getCluster(clusterId);
            Assert.assertNotNull(String.format("%s, cluster not found: [cluster] %s", validationError, clusterId), cluster1);
            Assert.assertEquals(String.format("%s, tenant range is not valid: [cluster] %s", validationError, clusterId), cluster1.getTenantRange(), "1-100");

            String hostName = "cluster1.appserver.foo.org";
            Assert.assertTrue(String.format("%s, hostname not found: [hostname] %s", validationError, hostName), hostNameExist(cluster1, hostName));

            hostName = "cluster1.org";
            Assert.assertTrue(String.format("%s, hostname not found: [hostname] %s", validationError, hostName), hostNameExist(cluster1, hostName));
            Assert.assertEquals(String.format("%s, algorithm not valid", validationError), "round-robin", cluster1.getLoadBalanceAlgorithmName());

            String memberId = "m1";
            Member m1 = cluster1.getMember(memberId);
            Assert.assertNotNull(String.format("%s, member not found: [member] %s", validationError, memberId), m1);
            Assert.assertEquals(String.format("%s, member ip not valid", validationError), "10.0.0.10", m1.getDefaultPrivateIP());

            int proxyPort = 80;
            Port m1Http = m1.getPort(proxyPort);
            Assert.assertNotNull(String.format("%s, port not found: [member] %s [proxy-port] %d", validationError, memberId, proxyPort), m1Http);
            Assert.assertEquals(String.format("%s, port value not valid: [member] %s [proxy-port] %d", validationError, memberId, proxyPort), 8080, m1Http.getValue());
            Assert.assertEquals(String.format("%s, port proxy not valid: [member] %s [proxy-port] %d", validationError, memberId, proxyPort), 80, m1Http.getProxy());

            Assert.assertFalse(String.format("%s, rewrite-location-header is not false", validationError), LoadBalancerConfiguration.getInstance().isReWriteLocationHeader());
            Assert.assertTrue(String.format("%s, map-domain-names is not true", validationError), LoadBalancerConfiguration.getInstance().isDomainMappingEnabled());

        } finally {
            TopologyManager.releaseReadLock();
            LoadBalancerConfiguration.clear();
        }
    }

    private boolean hostNameExist(Cluster cluster, String hostName) {
        for (String hostName_ : cluster.getHostNames()) {
            if (hostName_.equals(hostName))
                return true;
        }
        return false;
    }
}
