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
import org.apache.stratos.integration.tests.rest.IntegrationMockClient;
import org.apache.stratos.messaging.domain.application.*;
import org.apache.stratos.messaging.domain.instance.ClusterInstance;
import org.apache.stratos.messaging.domain.instance.GroupInstance;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.domain.topology.Service;
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.*;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.application.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.io.File;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static junit.framework.Assert.*;

/**
 * To start the Topology receivers
 */
public class TopologyHandler {
    private static final Log log = LogFactory.getLog(TopologyHandler.class);

    public static final int APPLICATION_ACTIVATION_TIMEOUT = 360000;
    public static final int APPLICATION_UNDEPLOYMENT_TIMEOUT = 120000;
    public static final int APPLICATION_TOPOLOGY_TIMEOUT = 90000;
    public static final String APPLICATION_STATUS_CREATED = "Created";
    public static final String APPLICATION_STATUS_UNDEPLOYING = "Undeploying";
    private ApplicationsEventReceiver applicationsEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    public static TopologyHandler topologyHandler;
    private Map<String, Long> terminatedMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> terminatingMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> createdMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> inActiveMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> activateddMembers = new ConcurrentHashMap<String, Long>();

    private TopologyHandler() {
        // Set jndi.properties.dir system property for initializing event receivers
        System.setProperty("jndi.properties.dir", getResourcesFolderPath());
        System.setProperty("autoscaler.service.url", "https://localhost:9443/services/AutoscalerService");
        initializeApplicationEventReceiver();
        initializeTopologyEventReceiver();
        assertApplicationTopologyInitialized();
        assertTopologyInitialized();
        addTopologyEventListeners();
        addApplicationEventListeners();
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
    public void assertApplicationStatus(String applicationName, ApplicationStatus status) {
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        while (!((application != null) && (application.getStatus() == status))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplication(applicationName);
            if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                break;
            }
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        assertEquals(String.format("Application status did not change to %s: [application-id] %s",
                        status.toString(), applicationName),
                status, application.getStatus());
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public void assertGroupActivation(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
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
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
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
                clusterActive = activeInstances >= clusterDataHolder.getMinInstances();

                if (!clusterActive) {
                    break;
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
    public void terminateMemberFromCluster(String cartridgeName, String applicationName,
                                           IntegrationMockClient mockIaasApiClient) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            if(cartridgeName.equals(serviceName)) {
                String clusterId = clusterDataHolder.getClusterId();
                Service service = TopologyManager.getTopology().getService(serviceName);
                assertNotNull(String.format("Service is not found: [application-id] %s [service] %s",
                        applicationName, serviceName), service);

                Cluster cluster = service.getCluster(clusterId);
                assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                        applicationName, serviceName, clusterId), cluster);
                boolean memberTerminated = false;

                for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                    for (Member member : cluster.getMembers()) {
                        if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                            if (member.getStatus().equals(MemberStatus.Active)) {
                                mockIaasApiClient.terminateInstance(member.getMemberId());
                                memberTerminated = true;
                                break;
                            }
                        }
                    }

                    if(memberTerminated) {
                        break;
                    }

                }
                assertTrue("Any member couldn't be terminated from the mock IaaS client", memberTerminated);
            }

        }

    }

    public void assertClusterMinMemberCount(String applicationName, int minMembers) {
        long startTime = System.currentTimeMillis();

        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s",
                applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
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
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        ApplicationContext applicationContext = null;
        try {
            applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationName);
        } catch (RemoteException e) {
            log.error("Error while getting the application context for [application] " + applicationName);
        }
        while (((application != null) && application.getInstanceContextCount() > 0) ||
                (applicationContext == null || applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING))) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplication(applicationName);
            try {
                applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationName);
            } catch (RemoteException e) {
                log.error("Error while getting the application context for [application] " + applicationName);
            }
            if ((System.currentTimeMillis() - startTime) > APPLICATION_UNDEPLOYMENT_TIMEOUT) {
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
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
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

    private void addTopologyEventListeners() {
        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
                getTerminatedMembers().put(memberTerminatedEvent.getMemberId(), System.currentTimeMillis());

            }
        });


        topologyEventReceiver.addEventListener(new ClusterInstanceCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceCreatedEvent event1 = (ClusterInstanceCreatedEvent) event;
                String clusterId = event1.getClusterId();
                getCreatedMembers().put(clusterId, System.currentTimeMillis());
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceActivatedEvent event1 = (ClusterInstanceActivatedEvent) event;
                String clusterId = event1.getClusterId();
                getActivateddMembers().put(clusterId, System.currentTimeMillis());

            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceInactivateEvent event1 = (ClusterInstanceInactivateEvent) event;
                String clusterId = event1.getClusterId();
                getInActiveMembers().put(clusterId, System.currentTimeMillis());
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceTerminatedEvent event1 = (ClusterInstanceTerminatedEvent) event;
                String clusterId = event1.getClusterId();
                getTerminatedMembers().put(clusterId, System.currentTimeMillis());
            }
        });

        topologyEventReceiver.addEventListener(new ClusterInstanceTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceTerminatingEvent event1 = (ClusterInstanceTerminatingEvent) event;
                String clusterId = event1.getClusterId();
                getTerminatingMembers().put(clusterId, System.currentTimeMillis());
            }
        });


    }

    private void addApplicationEventListeners() {

        applicationsEventReceiver.addEventListener(new GroupInstanceCreatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                GroupInstanceCreatedEvent event1 = (GroupInstanceCreatedEvent) event;
                String appId = event1.getAppId();
                String groupId = event1.getGroupId();
                String instanceId = event1.getGroupInstance().getInstanceId();
                String id = generateId(appId, groupId, instanceId);
                getCreatedMembers().put(id, System.currentTimeMillis());

            }
        });

        applicationsEventReceiver.addEventListener(new GroupInstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                GroupInstanceActivatedEvent event1 = (GroupInstanceActivatedEvent) event;
                String appId = event1.getAppId();
                String groupId = event1.getGroupId();
                String instanceId = event1.getInstanceId();
                String id = generateId(appId, groupId, instanceId);
                getActivateddMembers().put(id, System.currentTimeMillis());
            }
        });

        applicationsEventReceiver.addEventListener(new GroupInstanceInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                GroupInstanceInactivatedEvent event1 = (GroupInstanceInactivatedEvent) event;
                String appId = event1.getAppId();
                String groupId = event1.getGroupId();
                String instanceId = event1.getInstanceId();
                String id = generateId(appId, groupId, instanceId);
                getInActiveMembers().put(id, System.currentTimeMillis());
            }
        });

        applicationsEventReceiver.addEventListener(new GroupInstanceTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                GroupInstanceTerminatedEvent event1 = (GroupInstanceTerminatedEvent) event;
                String appId = event1.getAppId();
                String groupId = event1.getGroupId();
                String instanceId = event1.getInstanceId();
                String id = generateId(appId, groupId, instanceId);
                getTerminatedMembers().put(id, System.currentTimeMillis());
            }
        });

        applicationsEventReceiver.addEventListener(new GroupInstanceTerminatingEventListener() {
            @Override
            protected void onEvent(Event event) {
                GroupInstanceTerminatingEvent event1 = (GroupInstanceTerminatingEvent) event;
                String appId = event1.getAppId();
                String groupId = event1.getGroupId();
                String instanceId = event1.getInstanceId();
                String id = generateId(appId, groupId, instanceId);
                getTerminatingMembers().put(id, System.currentTimeMillis());
            }
        });
    }

    public String generateId(String appId, String groupId, String instanceId) {
        return appId + "-" + groupId + "-" + instanceId;
    }

    public String getClusterIdFromAlias(String applicationId, String alias) {
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        assertNotNull(application);

        ClusterDataHolder dataHolder = application.getClusterDataHolderRecursivelyByAlias(alias);
        assertNotNull(dataHolder);

        return dataHolder.getClusterId();
    }


    public void removeMembersFromMaps(String applicationId) {
        for(Map.Entry<String, Long> entry: getActivateddMembers().entrySet()) {
            if(entry.getKey().contains(applicationId)) {
                getActivateddMembers().remove(entry.getKey());
            }
        }

        for(Map.Entry<String, Long> entry: getTerminatedMembers().entrySet()) {
            if(entry.getKey().contains(applicationId)) {
                getTerminatedMembers().remove(entry.getKey());
            }
        }
    }

    public Map<String, Long> getTerminatedMembers() {
        return terminatedMembers;
    }

    public void setTerminatedMembers(Map<String, Long> terminatedMembers) {
        this.terminatedMembers = terminatedMembers;
    }

    public Map<String, Long> getTerminatingMembers() {
        return terminatingMembers;
    }

    public void setTerminatingMembers(Map<String, Long> terminatingMembers) {
        this.terminatingMembers = terminatingMembers;
    }

    public Map<String, Long> getCreatedMembers() {
        return createdMembers;
    }

    public void setCreatedMembers(Map<String, Long> createdMembers) {
        this.createdMembers = createdMembers;
    }

    public Map<String, Long> getInActiveMembers() {
        return inActiveMembers;
    }

    public void setInActiveMembers(Map<String, Long> inActiveMembers) {
        this.inActiveMembers = inActiveMembers;
    }

    public Map<String, Long> getActivateddMembers() {
        return activateddMembers;
    }

    public void setActivateddMembers(Map<String, Long> activateddMembers) {
        this.activateddMembers = activateddMembers;
    }
}
