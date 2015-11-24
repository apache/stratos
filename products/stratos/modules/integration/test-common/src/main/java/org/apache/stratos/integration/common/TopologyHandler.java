/*
 * Copyright 2005-2015 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.stratos.integration.common;

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
import org.apache.stratos.messaging.event.Event;
import org.apache.stratos.messaging.event.application.*;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.application.*;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpEventReceiver;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpManager;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.mock.iaas.client.MockIaasApiClient;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import static org.testng.AssertJUnit.*;

/**
 * To start the Topology receivers
 */
public class TopologyHandler {
    private static final Log log = LogFactory.getLog(TopologyHandler.class);

    public static final int APPLICATION_ACTIVATION_TIMEOUT = 500000;
    public static final int APPLICATION_UNDEPLOYMENT_TIMEOUT = 500000;
    public static final int MEMBER_TERMINATION_TIMEOUT = 500000;
    public static final int APPLICATION_INIT_TIMEOUT = 20000;
    public static final int TENANT_INIT_TIMEOUT = 20000;
    public static final int APPLICATION_SIGNUP_INIT_TIMEOUT = 20000;
    public static final int TOPOLOGY_INIT_TIMEOUT = 20000;
    public static final String APPLICATION_STATUS_CREATED = "Created";
    public static final String APPLICATION_STATUS_UNDEPLOYING = "Undeploying";
    private ApplicationsEventReceiver applicationsEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    private TenantEventReceiver tenantEventReceiver;
    private ApplicationSignUpEventReceiver applicationSignUpEventReceiver;
    public static TopologyHandler topologyHandler;
    private ExecutorService executorService = StratosThreadPool.getExecutorService("stratos.integration.test.pool", 10);
    private Map<String, Long> terminatedMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> terminatingMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> createdMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> inActiveMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> activateddMembers = new ConcurrentHashMap<String, Long>();

    private TopologyHandler() {
        initializeApplicationEventReceiver();
        initializeTopologyEventReceiver();
        initializeTenantEventReceiver();
        initializeApplicationSignUpEventReceiver();
        assertApplicationTopologyInitialized();
        assertTopologyInitialized();
        assertTenantInitialized();
        assertApplicationSignUpInitialized();
        addTopologyEventListeners();
        addApplicationEventListeners();
    }

    private void initializeApplicationSignUpEventReceiver() {
        applicationSignUpEventReceiver = new ApplicationSignUpEventReceiver();
        applicationSignUpEventReceiver.setExecutorService(executorService);
        applicationSignUpEventReceiver.execute();
    }

    private void initializeTenantEventReceiver() {
        tenantEventReceiver = new TenantEventReceiver();
        tenantEventReceiver.setExecutorService(executorService);
        tenantEventReceiver.execute();
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
        applicationsEventReceiver = new ApplicationsEventReceiver();
        applicationsEventReceiver.setExecutorService(executorService);
        applicationsEventReceiver.execute();
    }

    /**
     * Initialize Topology event receiver
     */
    private void initializeTopologyEventReceiver() {
        topologyEventReceiver = new TopologyEventReceiver();
        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.execute();
    }

    /**
     * Assert application Topology initialization
     */
    private void assertApplicationTopologyInitialized() {
        log.info(String.format("Asserting application topology initialization within %d ms", APPLICATION_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        boolean applicationTopologyInitialized = ApplicationManager.getApplications().isInitialized();
        while (!applicationTopologyInitialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            applicationTopologyInitialized = ApplicationManager.getApplications().isInitialized();
            if ((System.currentTimeMillis() - startTime) > APPLICATION_INIT_TIMEOUT) {
                break;
            }
        }
        if (applicationTopologyInitialized) {
            log.info(String.format("Application topology initialized under %d ms",
                    (System.currentTimeMillis() - startTime)));
        }
        assertEquals(
                String.format("Application topology didn't get initialized within %d ms", APPLICATION_INIT_TIMEOUT),
                applicationTopologyInitialized, true);
    }

    /**
     * Assert Topology initialization
     */
    private void assertTopologyInitialized() {
        log.info(String.format("Asserting topology initialization within %d ms", TOPOLOGY_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        boolean topologyInitialized = TopologyManager.getTopology().isInitialized();
        while (!topologyInitialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            topologyInitialized = TopologyManager.getTopology().isInitialized();
            if ((System.currentTimeMillis() - startTime) > TOPOLOGY_INIT_TIMEOUT) {
                break;
            }
        }
        if (topologyInitialized) {
            log.info(String.format("Topology initialized under %d ms", (System.currentTimeMillis() - startTime)));
        }
        assertEquals(String.format("Topology didn't get initialized within %d ms", TOPOLOGY_INIT_TIMEOUT),
                topologyInitialized, true);
    }

    private void assertTenantInitialized() {
        log.info(String.format("Asserting tenant model initialization within %d ms", TENANT_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        boolean tenantInitialized = TenantManager.getInstance().isInitialized();
        while (!tenantInitialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            tenantInitialized = TenantManager.getInstance().isInitialized();
            if ((System.currentTimeMillis() - startTime) > TENANT_INIT_TIMEOUT) {
                break;
            }
        }
        if (tenantInitialized) {
            log.info(String.format("Tenant model initialized under %d ms", (System.currentTimeMillis() - startTime)));
        }
        assertEquals(String.format("Tenant model didn't get initialized within %d ms", TENANT_INIT_TIMEOUT),
                tenantInitialized, true);
    }

    private void assertApplicationSignUpInitialized() {
        log.info(String.format("Asserting application signup initialization within %d ms",
                APPLICATION_SIGNUP_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        boolean applicationSignUpInitialized = ApplicationSignUpManager.getInstance().isInitialized();
        while (!applicationSignUpInitialized) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            applicationSignUpInitialized = ApplicationSignUpManager.getInstance().isInitialized();
            if ((System.currentTimeMillis() - startTime) > APPLICATION_SIGNUP_INIT_TIMEOUT) {
                break;
            }
        }
        if (applicationSignUpInitialized) {
            log.info(String.format("Application signup initialized under %d ms",
                    (System.currentTimeMillis() - startTime)));
        }
        assertEquals(String.format("Application signup didn't get initialized within %d ms",
                APPLICATION_SIGNUP_INIT_TIMEOUT), applicationSignUpInitialized, true);
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
                log.info(String.format("Waiting for [application] %s to become [status] %s...", applicationName,
                        status));
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }
            application = ApplicationManager.getApplications().getApplication(applicationName);
            if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                log.error("Application did not activate within timeout period");
                break;
            }
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        assertEquals(String.format("Application status did not change to %s: [application-id] %s", status.toString(),
                applicationName), status, application.getStatus());
    }

    /**
     * Assert application activation
     *
     * @param applicationName
     */
    public void assertGroupActivation(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

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
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceName);
            assertNotNull(String.format("Service is not found: [application-id] %s [service] %s", applicationName,
                    serviceName), service);

            Cluster cluster = service.getCluster(clusterId);
            assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                    applicationName, serviceName, clusterId), cluster);
            for (Member member : cluster.getMembers()) {
                log.info(String.format("Member [member-id] %s found in cluster instance [cluster-instance] %s of "
                                + "cluster [cluster-id] %s", member.getMemberId(), member.getClusterInstanceId(),
                        member.getClusterId()));
            }
            boolean clusterActive = false;
            int activeInstances;
            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                log.info("Checking for active members in cluster instance: " + instance.getInstanceId());
                activeInstances = 0;
                for (Member member : cluster.getMembers()) {
                    if (member.getClusterInstanceId().equals(instance.getInstanceId())) {
                        if (member.getStatus().equals(MemberStatus.Active)) {
                            activeInstances++;
                        }
                    }
                }
                clusterActive = (activeInstances >= clusterDataHolder.getMinInstances());
                assertTrue(String.format("Cluster status did not change to active: [cluster-id] %s", clusterId),
                        clusterActive);
            }
        }
    }

    /**
     * Get all the members that belongs to the cluster identified by cartridge name and application name in the
     * topology
     *
     * @param cartridgeName
     * @param applicationName
     */
    public Map<String, Member> getMembersForCluster(String cartridgeName, String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        Map<String, Member> memberMap = new HashMap<String, Member>();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            if (cartridgeName.equals(serviceName)) {
                String clusterId = clusterDataHolder.getClusterId();
                Service service = TopologyManager.getTopology().getService(serviceName);
                assertNotNull(String.format("Service is not found: [application-id] %s [service] %s", applicationName,
                        serviceName), service);

                Cluster cluster = service.getCluster(clusterId);
                assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                        applicationName, serviceName, clusterId), cluster);
                for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                    for (Member member : cluster.getMembers()) {
                        memberMap.put(member.getMemberId(), member);
                    }
                }
            }
        }
        return memberMap;
    }

    /**
     * Terminate a member in mock iaas directly without involving Stratos REST API
     * This is similar to manually terminating an instance in an IaaS. This could be used to simulate member failures
     *
     * @param memberId
     * @param mockIaasApiClient
     */
    public void terminateMemberInMockIaas(String memberId, MockIaasApiClient mockIaasApiClient) {
        boolean memberTerminated = false;
        memberTerminated = mockIaasApiClient.terminateInstance(memberId);
        assertTrue(String.format("Member [member-id] %s couldn't be terminated from the mock IaaS", memberId),
                memberTerminated);
    }

    public void assertMemberTermination(String memberId) {
        long startTime = System.currentTimeMillis();
        assertNotNull(String.format("Member id is not found: [member-id] %s", memberId));
        boolean hasMemberRemoved = false;
        while (!hasMemberRemoved) {
            // Wait until the member gets removed by MemberTerminatedEvent topology receiver
            if (getTerminatingMembers().get(memberId) == null &&
                    getInActiveMembers().get(memberId) == null &&
                    getActivateddMembers().get(memberId) == null &&
                    getCreatedMembers().get(memberId) == null) {
                getTerminatedMembers().remove(memberId);
                hasMemberRemoved = true;
            } else {
                if (getTerminatedMembers().get(memberId) - startTime > MEMBER_TERMINATION_TIMEOUT) {
                    log.error("Member did not get removed from the topology within timeout period");
                    break;
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Could not sleep", e);
            }
        }
        assertTrue(String.format("Member [member-id] %s did not get removed from the topology", memberId),
                hasMemberRemoved);
    }

    public void assertClusterMinMemberCount(String applicationName, int minMembers) {
        long startTime = System.currentTimeMillis();

        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceName);
            assertNotNull(String.format("Service is not found: [application-id] %s [service] %s", applicationName,
                    serviceName), service);

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
                    assertNotNull(
                            String.format("Service is not found: [application-id] %s [service] %s", applicationName,
                                    serviceName), service);

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
                    assertNotNull(
                            String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                                    applicationName, serviceName, clusterId), cluster);

                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        log.error("Cluster did not activate within timeout period");
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
        while (((application != null) && application.getInstanceContextCount() > 0) || (applicationContext == null
                || applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING))) {
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
                log.error("Application did not undeploy within timeout period");
                break;
            }
        }

        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);
        assertNotNull(String.format("Application Context is not found: [application-id] %s", applicationName),
                applicationContext);

        //Force undeployment after the graceful deployment
        if (application.getInstanceContextCount() > 0 || applicationContext.getStatus()
                .equals(APPLICATION_STATUS_UNDEPLOYING)) {
            return false;
        }
        assertEquals(
                String.format("Application status did not change to Created: [application-id] %s", applicationName),
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
                    log.error("Group instance min count check failed within timeout period");
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
                        log.error("Application did not activate within timeout period");
                        break;
                    }
                }
            }
            assertEquals(
                    String.format("Application status did not change to active: [application-id] %s", applicationName),
                    group.getInstanceContextCount(), count);
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationName), application);

    }

    public void assertApplicationNotExists(String applicationName) {
        Application application = ApplicationManager.getApplications().getApplication(applicationName);
        assertNull(String.format("Application is found in the topology : [application-id] %s", applicationName),
                application);
    }

    private void addTopologyEventListeners() {
        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
                getTerminatedMembers().put(memberTerminatedEvent.getMemberId(), System.currentTimeMillis());
                getActivateddMembers().remove(((MemberTerminatedEvent) event).getMemberId());
                getCreatedMembers().remove(((MemberTerminatedEvent) event).getMemberId());
                getInActiveMembers().remove(((MemberTerminatedEvent) event).getMemberId());
                getTerminatingMembers().remove(((MemberTerminatedEvent) event).getMemberId());
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
        for (Map.Entry<String, Long> entry : getActivateddMembers().entrySet()) {
            if (entry.getKey().contains(applicationId)) {
                getActivateddMembers().remove(entry.getKey());
            }
        }

        for (Map.Entry<String, Long> entry : getTerminatedMembers().entrySet()) {
            if (entry.getKey().contains(applicationId)) {
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

    public List<Member> getMembersForApplication(String applicationId) {
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);
        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
        List<Member> memberList = new ArrayList<>();
        for (ClusterDataHolder clusterDataHolder : clusterDataHolderSet) {
            String serviceName = clusterDataHolder.getServiceType();
            String clusterId = clusterDataHolder.getClusterId();
            Service service = TopologyManager.getTopology().getService(serviceName);
            assertNotNull(
                    String.format("Service is not found: [application-id] %s [service] %s", applicationId, serviceName),
                    service);
            Cluster cluster = service.getCluster(clusterId);
            assertNotNull(String.format("Cluster is not found: [application-id] %s [service] %s [cluster-id] %s",
                    applicationId, serviceName, clusterId), cluster);
            for (ClusterInstance instance : cluster.getInstanceIdToInstanceContextMap().values()) {
                for (Member member : cluster.getMembers()) {
                    memberList.add(member);
                }
            }
        }
        return memberList;
    }
}
