/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.messaging.topology.locking;

import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.junit.*;

import java.util.HashMap;
import java.util.Map;

public class TopologyLockingTest {

    private static Topology topology;

    @BeforeClass
    public static void setUpBeforeClass() {
        System.out.println("Setting up TopologyLockingTest");
        topology = TopologyManager.getTopology();

        //add Services
        topology.addService(new Service("service1", ServiceType.SingleTenant));
        topology.addService(new Service("service2", ServiceType.SingleTenant));
        topology.addService(new Service("service3", ServiceType.SingleTenant));
        topology.addService(new Service("service4", ServiceType.SingleTenant));

        // add Clusters
        topology.getService("service1").addCluster(new Cluster("service1", "service1.cluster1.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));
        topology.getService("service1").addCluster(new Cluster("service1", "service1.cluster2.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));

        topology.getService("service2").addCluster(new Cluster("service2", "service2.cluster1.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));
        topology.getService("service2").addCluster(new Cluster("service2", "service2.cluster2.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));

        topology.getService("service3").addCluster(new Cluster("service3", "service3.cluster1.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));
        topology.getService("service3").addCluster(new Cluster("service3", "service3.cluster2.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));

        topology.getService("service4").addCluster(new Cluster("service4", "service4.cluster1.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));
        topology.getService("service4").addCluster(new Cluster("service4", "service4.cluster2.domain",
                "dummyDeploymentPolicy", "dummyAutoscalePolicy", null));

        // Create Application 1
        Application app1 = new Application("app1");
        Map<String, ClusterDataHolder> clusterDataMap1 = new HashMap<String, ClusterDataHolder>();
        clusterDataMap1.put("alias1", new ClusterDataHolder("service1", "service1.cluster1.domain"));
        clusterDataMap1.put("alias2", new ClusterDataHolder("service1", "service1.cluster2.domain"));
        clusterDataMap1.put("alias3", new ClusterDataHolder("service2", "service2.cluster1.domain"));
        clusterDataMap1.put("alias4", new ClusterDataHolder("service2", "service2.cluster2.domain"));

        // add cluster data to Application
        app1.setClusterData(clusterDataMap1);

        // add Applicaiton to Topology
        topology.addApplication(app1);

        // Create Application 2
        Application app2 = new Application("app2");
        Map<String, ClusterDataHolder> clusterDataMap2 = new HashMap<String, ClusterDataHolder>();
        clusterDataMap2.put("alias5", new ClusterDataHolder("service3", "service3.cluster1.domain"));
        clusterDataMap2.put("alias6", new ClusterDataHolder("service3", "service3.cluster2.domain"));
        clusterDataMap2.put("alias7", new ClusterDataHolder("service4", "service4.cluster1.domain"));
        clusterDataMap2.put("alias8", new ClusterDataHolder("service4", "service4.cluster2.domain"));

        // add cluster data to Application
        app2.setClusterData(clusterDataMap2);

        // add Applicaiton to Topology
        topology.addApplication(app2);
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseReadLocksForServices1To2 () {

        TopologyManager.acquireReadLockForService("service1");
        TopologyManager.acquireReadLockForService("service2");

        TopologyManager.releaseReadLockForService("service1");
        TopologyManager.releaseReadLockForService("service2");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseReadLocksForServices3To4 () {

        TopologyManager.acquireReadLockForService("service3");
        TopologyManager.acquireReadLockForService("service4");

        TopologyManager.releaseReadLockForService("service3");
        TopologyManager.releaseReadLockForService("service4");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseWriteLocksForServices1To2 () {

        TopologyManager.acquireWriteLockForService("service1");
        TopologyManager.acquireWriteLockForService("service2");

        TopologyManager.releaseWriteLockForService("service1");
        TopologyManager.releaseWriteLockForService("service2");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseWriteLocksForServices3To4 () {

        TopologyManager.acquireWriteLockForService("service3");
        TopologyManager.acquireWriteLockForService("service4");

        TopologyManager.releaseWriteLockForService("service3");
        TopologyManager.releaseWriteLockForService("service4");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseReadLocksForClustersOfService1 () {

        TopologyManager.acquireReadLockForCluster("service1", "service1.cluster1.domain");
        TopologyManager.acquireReadLockForCluster("service1", "service1.cluster2.domain");

        TopologyManager.releaseReadLockForCluster("service1", "service1.cluster1.domain");
        TopologyManager.releaseReadLockForCluster("service1", "service1.cluster2.domain");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseReadLocksForClustersOfService2 () {

        TopologyManager.acquireReadLockForCluster("service2", "service2.cluster1.domain");
        TopologyManager.acquireReadLockForCluster("service2", "service2.cluster2.domain");

        TopologyManager.releaseReadLockForCluster("service2", "service2.cluster1.domain");
        TopologyManager.releaseReadLockForCluster("service2", "service2.cluster2.domain");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseWriteLocksForClustersOfService1 () {

        TopologyManager.acquireWriteLockForCluster("service1", "service1.cluster1.domain");
        TopologyManager.acquireWriteLockForCluster("service1", "service1.cluster2.domain");

        TopologyManager.releaseWriteLockForCluster("service1", "service1.cluster1.domain");
        TopologyManager.releaseWriteLockForCluster("service1", "service1.cluster2.domain");
    }

    @Test(timeout=10000)
    public void testAqcuireAndReleaseWriteLocksForClustersOfService2 () {

        TopologyManager.acquireWriteLockForCluster("service2", "service2.cluster1.domain");
        TopologyManager.acquireWriteLockForCluster("service2", "service2.cluster2.domain");

        TopologyManager.releaseWriteLockForCluster("service2", "service2.cluster1.domain");
        TopologyManager.releaseWriteLockForCluster("service2", "service2.cluster2.domain");
    }

    @Test(timeout=10000)
    public void testAcquireAndReleaseReadLockForApp1 () {

        TopologyManager.acquireReadLockForApplication("app1");
        TopologyManager.releaseReadLockForApplication("app1");
    }

    @Test(timeout=10000)
    public void testAcquireAndReleaseWriteLockForApp1 () {

        TopologyManager.acquireWriteLockForApplication("app1");
        TopologyManager.releaseWriteLockForApplication("app1");
    }

    @Test
    public void testAcquireAndReleaseReadLockForApp2 () {

        TopologyManager.acquireReadLockForApplication("app2");
        TopologyManager.releaseReadLockForApplication("app2");
    }

    @Test(timeout=10000)
    public void testAcquireAndReleaseWriteLockForApp2 () {

        TopologyManager.acquireWriteLockForApplication("app2");
        TopologyManager.releaseWriteLockForApplication("app2");
    }

    @AfterClass
    public static void tearDownAfterClass() {
        System.out.println("Cleaning up TopologyLockingTest");
        topology = null;
    }
}
