/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.integration.tests;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.common.threading.StratosThreadPool;
import org.apache.stratos.messaging.domain.application.*;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.listener.topology.MemberInitializedEventListener;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * To start the Topology receivers
 */
public class TopologyHandler {
    private static final Log log = LogFactory.getLog(TopologyHandler.class);

    public static final int APPLICATION_ACTIVATION_TIMEOUT = 120000;
    public static final int APPLICATION_TOPOLOGY_TIMEOUT = 60000;
    public static final String APPLICATION_STATUS_CREATED = "Created";
    public static final String APPLICATION_STATUS_UNDEPLOYING = "Undeploying";
    private ApplicationsEventReceiver applicationsEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    public static TopologyHandler topologyHandler;

    private TopologyHandler() {
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
        System.setProperty("autoscaler.service.url", "https://localhost:9443/services/AutoscalerService");
        initializeApplicationEventReceiver();
        initializeTopologyEventReceiver();
        assertApplicationTopologyInitialized();
        assertTopologyInitialized();
    }

    public static TopologyHandler getInstance() {
        if (topologyHandler == null) {
            synchronized (TopologyHandler.class) {
                if (topologyHandler == null) {
                    topologyHandler = new TopologyHandler();
                }
            }
        }
        return topologyHandler;
    }


    /**
     * Initialize application event receiver
     */
    private void initializeApplicationEventReceiver() {
        if (applicationsEventReceiver == null) {
            applicationsEventReceiver = new ApplicationsEventReceiver();
            ExecutorService executorService = StratosThreadPool.getExecutorService("STRATOS_TEST_SERVER", 1);
            applicationsEventReceiver.setExecutorService(executorService);
            applicationsEventReceiver.execute();
        }
    }

    /**
     * Initialize Topology event receiver
     */
    private void initializeTopologyEventReceiver() {
        if (topologyEventReceiver == null) {
            topologyEventReceiver = new TopologyEventReceiver();
            ExecutorService executorService = StratosThreadPool.getExecutorService("STRATOS_TEST_SERVER1", 1);
            topologyEventReceiver.setExecutorService(executorService);
            topologyEventReceiver.execute();
        }
    }

    /**
     * Assert application Topology initialization
     */
    private void assertApplicationTopologyInitialized() {
        long startTime = System.currentTimeMillis();
        boolean applicationTopologyInitialized = ApplicationManager.getApplications().isInitialized();
        while (!applicationTopologyInitialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            applicationTopologyInitialized = ApplicationManager.getApplications().isInitialized();
            if ((System.currentTimeMillis() - startTime) > APPLICATION_TOPOLOGY_TIMEOUT) {
                break;
            }
        }
        assertEquals(String.format("Application Topology didn't get initialized "), applicationTopologyInitialized, true);
    }

    /**
     * Assert Topology initialization
     */
    private void assertTopologyInitialized() {
        long startTime = System.currentTimeMillis();
        boolean topologyInitialized = TopologyManager.getTopology().isInitialized();
        while (!topologyInitialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            topologyInitialized = TopologyManager.getTopology().isInitialized();
            if ((System.currentTimeMillis() - startTime) > APPLICATION_TOPOLOGY_TIMEOUT) {
                break;
            }
        }
        assertEquals(String.format("Topology didn't get initialized "), topologyInitialized, true);
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public void assertApplicationActivation(String applicationName) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
        while (!((application != null) && (application.getStatus() == ApplicationStatus.Active))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
            if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                break;
            }
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        assertEquals(String.format("Application status did not change to active: [application-id] %s", applicationName),
                ApplicationStatus.Active, application.getStatus());
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public void assertGroupActivation(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Collection<Group> groups = application.getAllGroupsRecursively();
        for (Group group : groups) {
            assertEquals(group.getInstanceContextCount() >= group.getGroupMinInstances(), true);
        }
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public void assertClusterActivation(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceUuid = clusterDataHolder.getServiceUuid();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceUuid);
            assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                    applicationName, serviceUuid), service);

            Cluster cluster = service.getCluster(clusterId);
            assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                    applicationName, serviceUuid, clusterId), cluster);
            boolean clusterActive = false;

            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                int activeInstances = 0;
                for (Member member : cluster.getMembers()) {
                    if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                        if (member.getStatus().equals(MemberStatus.Active)) {
                            activeInstances++;
                        }
                    }
                }
                clusterActive = activeInstances >= clusterDataHolder.getMinInstances();

                if (!clusterActive) {
                    break;
                }
            }
            assertEquals(String.format("Cluster status did not change to active: [cluster-id] %s", clusterId),
                    clusterActive, true);
        }

    }

    public void assertClusterMinMemberCount(String applicationName, int minMembers) {
        long startTime = System.currentTimeMillis();

        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceUuid();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceName);
            assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                    applicationName, serviceName), service);

            Cluster cluster = service.getCluster(clusterId);
            assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                    applicationName, serviceName, clusterId), cluster);
            boolean clusterActive = false;

            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                int activeInstances = 0;
                for (Member member : cluster.getMembers()) {
                    if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                        if (member.getStatus().equals(MemberStatus.Active)) {
                            activeInstances++;
                        }
                    }
                }
                clusterActive = activeInstances >= minMembers;

                while (!clusterActive) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                    service = TopologyManager.getTopology().getService(serviceName);
                    assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                            applicationName, serviceName), service);

                    cluster = service.getCluster(clusterId);
                    activeInstances = 0;
                    for (Member member : cluster.getMembers()) {
                        if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                            if (member.getStatus().equals(MemberStatus.Active)) {
                                activeInstances++;
                            }
                        }
                    }
                    clusterActive = activeInstances >= minMembers;
                    assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                            applicationName, serviceName, clusterId), cluster);

                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        break;
                    }
                }
            }
            assertEquals(String.format("Cluster status did not change to active: [cluster-id] %s", clusterId),
                    clusterActive, true);
        }

    }


    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public boolean assertApplicationUndeploy(String applicationName) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationName, -1234);
        ApplicationContext applicationContext = null;
        try {
            applicationContext = AutoscalerServiceClient.getInstance().getApplicationByTenant(applicationName,-1234);
        } catch (RemoteException e) {
            log.error("Error while getting the application context for [application] " + applicationName);
        }
        while (((application != null) && application.getInstanceContextCount() > 0) ||
                (applicationContext == null || applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
            try {
                applicationContext = AutoscalerServiceClient.getInstance().getApplicationByTenant(applicationName,-1234);
            } catch (RemoteException e) {
                log.error("Error while getting the application context for [application] " + applicationName);
            }
            if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                break;
            }
        }

        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);
        assertNotNull(String.format("Application Context is not found: [application-id] %s",
                applicationName), applicationContext);

        //Force undeployment after the graceful deployment
        if (application.getInstanceContextCount() > 0 ||
                applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING)) {
            return false;
        }
        assertEquals(String.format("Application status did not change to Created: [application-id] %s", applicationName),
                APPLICATION_STATUS_CREATED, applicationContext.getStatus());
        return true;
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public void assertGroupInstanceCount(String applicationName, String groupAlias, int count) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplicationByTenant(applicationName,-1234);
        if (application != null) {
            Group group = application.getGroupRecursively(groupAlias);
            while (group.getInstanceContextCount() != count) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                    break;
                }
            }
            for (GroupInstance instance : group.getInstanceIdToInstanceContextMap().values()) {
                while (!instance.getStatus().equals(GroupStatus.Active)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignore) {
                    }
                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        break;
                    }
                }
            }
            assertEquals(String.format("Application status did not change to active: [application-id] %s", applicationName),
                    group.getInstanceContextCount(), count);
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

    }

    public void assertApplicationNotExists(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNull(String.format("Application is found in the topology : [application-id] %s", applicationName), application);
    }

    /**
     * Get resources folder path
     *
     * @return
     */
    private String getResourcesFolderPath() {
        String path = getClass().getResource("/").getPath();
        return StringUtils.removeEnd(path, File.separator);
    }
}
