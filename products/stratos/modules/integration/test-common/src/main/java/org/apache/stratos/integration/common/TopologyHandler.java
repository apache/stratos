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
import org.apache.stratos.messaging.event.health.stat.MemberFaultEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.listener.application.*;
import org.apache.stratos.messaging.listener.health.stat.MemberFaultEventListener;
import org.apache.stratos.messaging.listener.topology.*;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.application.ApplicationsEventReceiver;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpEventReceiver;
import org.apache.stratos.messaging.message.receiver.application.signup.ApplicationSignUpManager;
import org.apache.stratos.messaging.message.receiver.health.stat.HealthStatEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantEventReceiver;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyEventReceiver;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.mock.iaas.client.MockIaasApiClient;

import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.testng.AssertJUnit.*;

/**
 * To start the Topology receivers
 */
public class TopologyHandler {
    public static final int APPLICATION_ACTIVATION_TIMEOUT = 300000;
    public static final int APPLICATION_INACTIVATION_TIMEOUT = 120000;
    public static final int APPLICATION_UNDEPLOYMENT_TIMEOUT = 30000;
    public static final int MEMBER_TERMINATION_TIMEOUT = 120000;
    public static final int APPLICATION_TOPOLOGY_INIT_TIMEOUT = 20000;
    public static final int TENANT_INIT_TIMEOUT = 20000;
    public static final int APPLICATION_SIGNUP_INIT_TIMEOUT = 20000;
    public static final int TOPOLOGY_INIT_TIMEOUT = 20000;
    public static final String APPLICATION_STATUS_CREATED = "Created";
    public static final String APPLICATION_STATUS_UNDEPLOYING = "Undeploying";
    private static final Log log = LogFactory.getLog(TopologyHandler.class);
    public static TopologyHandler topologyHandler;
    private HealthStatEventReceiver healthStatEventReceiver;
    private ApplicationsEventReceiver applicationsEventReceiver;
    private TopologyEventReceiver topologyEventReceiver;
    private TenantEventReceiver tenantEventReceiver;
    private ApplicationSignUpEventReceiver applicationSignUpEventReceiver;
    private ExecutorService executorService = StratosThreadPool.getExecutorService("stratos.integration.test.pool", 30);
    private Map<String, Long> terminatedMembers = new ConcurrentHashMap<>();
    private Map<String, Long> terminatingMembers = new ConcurrentHashMap<>();
    private Map<String, Long> createdMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> inActiveMembers = new ConcurrentHashMap<String, Long>();
    private Map<String, Long> activateddMembers = new ConcurrentHashMap<String, Long>();

    private TopologyHandler() {
        initializeHealthStatsEventReceiver();
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

    private void initializeApplicationSignUpEventReceiver() {
        applicationSignUpEventReceiver = ApplicationSignUpEventReceiver.getInstance();
//        applicationSignUpEventReceiver.setExecutorService(executorService);
//        applicationSignUpEventReceiver.execute();
    }

    private void initializeTenantEventReceiver() {
        tenantEventReceiver = TenantEventReceiver.getInstance();
    }

    /**
     * Initialize application event receiver
     */
    private void initializeHealthStatsEventReceiver() {
        healthStatEventReceiver = HealthStatEventReceiver.getInstance();
        healthStatEventReceiver.addEventListener(new MemberFaultEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberFaultEvent memberFaultEvent = (MemberFaultEvent) event;
                log.info(String.format("MemberFaultEvent received for member [member-id] %s",
                        memberFaultEvent.getMemberId()));
            }
        });
    }

    /**
     * Initialize application event receiver
     */
    private void initializeApplicationEventReceiver() {
        applicationsEventReceiver = ApplicationsEventReceiver.getInstance();
        applicationsEventReceiver.addEventListener(new ApplicationInstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ApplicationInstanceActivatedEvent appInstanceActivatedEvent = (ApplicationInstanceActivatedEvent) event;
                log.info(String.format(
                        "ApplicationInstanceActivatedEvent received for application [application-id] %s [instance-id]"
                                + " %s", appInstanceActivatedEvent.getAppId(),
                        appInstanceActivatedEvent.getInstanceId()));
            }
        });

        applicationsEventReceiver.addEventListener(new ApplicationInstanceInactivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ApplicationInstanceInactivatedEvent appInstanceInactivatedEvent
                        = (ApplicationInstanceInactivatedEvent) event;
                log.info(String.format(
                        "ApplicationInstanceInactivatedEvent received for application [application-id] %s "
                                + "[instance-id] %s", appInstanceInactivatedEvent.getAppId(),
                        appInstanceInactivatedEvent.getInstanceId()));
            }
        });
    }

    /**
     * Initialize Topology event receiver
     */
    private void initializeTopologyEventReceiver() {
        topologyEventReceiver = TopologyEventReceiver.getInstance();
//        topologyEventReceiver.setExecutorService(executorService);
        topologyEventReceiver.addEventListener(new MemberActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberActivatedEvent memberActivatedEvent = (MemberActivatedEvent) event;
                log.info(String.format("MemberActivatedEvent received for member [member-id] %s",
                        memberActivatedEvent.getMemberId()));
            }
        });

        topologyEventReceiver.addEventListener(new MemberTerminatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                MemberTerminatedEvent memberTerminatedEvent = (MemberTerminatedEvent) event;
                log.info(String.format("MemberTerminatedEvent received for member [member-id] %s",
                        memberTerminatedEvent.getMemberId()));
            }
        });
        topologyEventReceiver.addEventListener(new ClusterInstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceActivatedEvent clusterInstanceActivatedEvent = (ClusterInstanceActivatedEvent) event;
                log.info(String.format("ClusterInstanceActivatedEvent received for cluster [cluster-id] %s",
                        clusterInstanceActivatedEvent.getClusterId()));
            }
        });
        topologyEventReceiver.addEventListener(new ClusterInstanceInactivateEventListener() {
            @Override
            protected void onEvent(Event event) {
                ClusterInstanceInactivateEvent clusterInstanceInactivateEvent = (ClusterInstanceInactivateEvent) event;
                log.info(String.format("MemberTerminatedEvent received for cluster [cluster-id] %s",
                        clusterInstanceInactivateEvent.getClusterId()));
            }
        });
        //topologyEventReceiver.execute();
    }

    /**
     * Assert application Topology initialization
     */
    private void assertApplicationTopologyInitialized() {
        log.info(String.format("Asserting application topology initialization within %d ms",
                APPLICATION_TOPOLOGY_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        while (!ApplicationManager.getApplications().isInitialized()) {
            log.info("Waiting for application topology to be initialized...");
            sleep(1000);
            if ((System.currentTimeMillis() - startTime) > APPLICATION_TOPOLOGY_INIT_TIMEOUT) {
                break;
            }
        }
        if (ApplicationManager.getApplications().isInitialized()) {
            log.info(String.format("Application topology initialized under %d ms",
                    (System.currentTimeMillis() - startTime)));
        }
        assertTrue(String.format("Application topology didn't get initialized within %d ms",
                APPLICATION_TOPOLOGY_INIT_TIMEOUT), ApplicationManager.getApplications().isInitialized());
    }

    /**
     * Assert Topology initialization
     */
    private void assertTopologyInitialized() {
        log.info(String.format("Asserting topology initialization within %d ms", TOPOLOGY_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        while (!TopologyManager.getTopology().isInitialized()) {
            log.info("Waiting for topology to be initialized...");
            sleep(1000);
            if ((System.currentTimeMillis() - startTime) > TOPOLOGY_INIT_TIMEOUT) {
                break;
            }
        }
        if (TopologyManager.getTopology().isInitialized()) {
            log.info(String.format("Topology initialized under %d ms", (System.currentTimeMillis() - startTime)));
        }
        assertTrue(String.format("Topology didn't get initialized within %d ms", TOPOLOGY_INIT_TIMEOUT),
                TopologyManager.getTopology().isInitialized());
    }

    private void assertTenantInitialized() {
        log.info(String.format("Asserting tenant model initialization within %d ms", TENANT_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        while (!TenantManager.getInstance().isInitialized()) {
            log.info("Waiting for tenant model to be initialized...");
            sleep(1000);
            if ((System.currentTimeMillis() - startTime) > TENANT_INIT_TIMEOUT) {
                break;
            }
        }
        if (TenantManager.getInstance().isInitialized()) {
            log.info(String.format("Tenant model initialized under %d ms", (System.currentTimeMillis() - startTime)));
        }
        assertTrue(String.format("Tenant model didn't get initialized within %d ms", TENANT_INIT_TIMEOUT),
                TenantManager.getInstance().isInitialized());
    }

    private void assertApplicationSignUpInitialized() {
        log.info(String.format("Asserting application signup initialization within %d ms",
                APPLICATION_SIGNUP_INIT_TIMEOUT));
        long startTime = System.currentTimeMillis();
        while (!ApplicationSignUpManager.getInstance().isInitialized()) {
            log.info("Waiting for application signup model to be initialized...");
            sleep(1000);
            if ((System.currentTimeMillis() - startTime) > APPLICATION_SIGNUP_INIT_TIMEOUT) {
                break;
            }
        }
        if (ApplicationSignUpManager.getInstance().isInitialized()) {
            log.info(String.format("Application signup initialized under %d ms",
                    (System.currentTimeMillis() - startTime)));
        }
        assertTrue(String.format("Application signup didn't get initialized within %d ms",
                APPLICATION_SIGNUP_INIT_TIMEOUT), ApplicationSignUpManager.getInstance().isInitialized());
    }

    /**
     * Assert application Active status
     *
     * @param applicationId
     */
    public void assertApplicationActiveStatus(final String applicationId) throws InterruptedException {
        log.info(String.format("Asserting application status ACTIVE for [application-id] %s...", applicationId));
        final long startTime = System.currentTimeMillis();
        final Object synObject = new Object();
        ApplicationInstanceActivatedEventListener activatedEventListener
                = new ApplicationInstanceActivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                ApplicationInstanceActivatedEvent activatedEvent = (ApplicationInstanceActivatedEvent) event;
                Application application = ApplicationManager.getApplications().getApplication(applicationId);
                if (application == null) {
                    log.warn(String.format("Application is null: [application-id] %s, [instance-id] %s", applicationId,
                            activatedEvent.getInstanceId()));
                }
                if (application != null && application.getStatus() == ApplicationStatus.Active) {
                    synchronized (synObject) {
                        synObject.notify();
                    }
                }
            }
        };
        applicationsEventReceiver.addEventListener(activatedEventListener);

        Future future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                Application application = ApplicationManager.getApplications().getApplication(applicationId);
                while (!((application != null) && (application.getStatus() == ApplicationStatus.Active))) {
                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        log.error(String.format("Application [application-id] %s did not activate within [timeout] %d",
                                applicationId, APPLICATION_ACTIVATION_TIMEOUT));
                        break;
                    }
                    ApplicationStatus currentStatus = (application != null) ? application.getStatus() : null;
                    log.info(String.format(
                            "Waiting for [application-id] %s [current-status] %s to become [status] %s...",
                            applicationId, currentStatus, ApplicationStatus.Active));
                    sleep(10000);
                    application = ApplicationManager.getApplications().getApplication(applicationId);
                }
                synchronized (synObject) {
                    synObject.notify();
                }
            }
        });

        synchronized (synObject) {
            synObject.wait();
            future.cancel(true);
            applicationsEventReceiver.removeEventListener(activatedEventListener);
        }

        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        ApplicationStatus currentStatus = (application != null) ? application.getStatus() : null;
        log.info(
                String.format("Assert application active status for [application-id] %s [current-status] %s took %d ms",
                        applicationId, currentStatus, System.currentTimeMillis() - startTime));
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);
        assertEquals(
                String.format("Application status did not change to %s: [application-id] %s", ApplicationStatus.Active,
                        applicationId), ApplicationStatus.Active, application.getStatus());
    }

    /**
     * Assert application Inactive status within default timeout
     *
     * @param applicationId
     */
    public void assertApplicationInActiveStatus(final String applicationId) throws InterruptedException {
        assertApplicationInActiveStatus(applicationId, APPLICATION_INACTIVATION_TIMEOUT);
    }

    /**
     * Assert application Inactive status
     *
     * @param applicationId
     * @param timeout
     */
    public void assertApplicationInActiveStatus(final String applicationId, final int timeout)
            throws InterruptedException {
        log.info(
                String.format("Asserting application status INACTIVE for [application-id] %s within [timeout] %d ms...",
                        applicationId, timeout));
        final long startTime = System.currentTimeMillis();
        final Object synObject = new Object();
        ApplicationInstanceInactivatedEventListener inactivatedEventListener
                = new ApplicationInstanceInactivatedEventListener() {
            @Override
            protected void onEvent(Event event) {
                Application application = ApplicationManager.getApplications().getApplication(applicationId);
                if (application == null || application.getStatus() == ApplicationStatus.Inactive) {
                    synchronized (synObject) {
                        synObject.notify();
                    }
                }
            }
        };
        applicationsEventReceiver.addEventListener(inactivatedEventListener);

        Future future = executorService.submit(new Runnable() {
            @Override
            public void run() {
                Application application = ApplicationManager.getApplications().getApplication(applicationId);
                while (!((application != null) && (application.getStatus() == ApplicationStatus.Inactive))) {
                    if ((System.currentTimeMillis() - startTime) > timeout) {
                        log.error(String.format(
                                "Application [application-id] %s did not become inactive within [timeout] %d",
                                applicationId, timeout));
                        break;
                    }
                    ApplicationStatus currentStatus = (application != null) ? application.getStatus() : null;
                    log.info(String.format(
                            "Waiting for [application-id] %s [current-status] %s to become [status] %s...",
                            applicationId, currentStatus, ApplicationStatus.Inactive));
                    sleep(10000);
                    application = ApplicationManager.getApplications().getApplication(applicationId);
                }
                synchronized (synObject) {
                    synObject.notify();
                }
            }
        });

        synchronized (synObject) {
            synObject.wait();
            future.cancel(true);
            applicationsEventReceiver.removeEventListener(inactivatedEventListener);
        }
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        ApplicationStatus currentStatus = (application != null) ? application.getStatus() : null;
        log.info(String.format(
                "Assert application inactive status for [application-id] %s [current-status] %s took %d ms",
                applicationId, currentStatus, System.currentTimeMillis() - startTime));
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);
        assertEquals(String.format("Application status did not change to %s: [application-id] %s",
                ApplicationStatus.Inactive, applicationId), ApplicationStatus.Inactive, application.getStatus());
    }

    /**
     * Assert application activation
     *
     * @param applicationId
     */
    public void assertGroupActivation(String applicationId) {
        log.info(String.format("Asserting group status ACTIVE for [application-id] %s...", applicationId));
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);

        Collection<Group> groups = application.getAllGroupsRecursively();
        for (Group group : groups) {
            assertEquals(group.getInstanceContextCount() >= group.getGroupMinInstances(), true);
        }
    }

    /**
     * Assert application activation
     *
     * @param applicationId
     */
    public void assertClusterActivation(String applicationId) {
        log.info(String.format("Asserting cluster status ACTIVE for [application-id] %s...", applicationId));
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
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
        boolean memberTerminated = mockIaasApiClient.terminateInstance(memberId);
        log.info(String.format("Terminating mock instance via Mock API client: [member-id] %s", memberId));
        assertTrue(String.format("Member [member-id] %s couldn't be terminated from the mock IaaS", memberId),
                memberTerminated);
    }

    public void assertMemberTermination(String memberId) {
        log.info(String.format("Asserting member termination for [member-id] %s", memberId));
        long startTime = System.currentTimeMillis();
        assertNotNull("Member id cannot be null", memberId);
        boolean hasMemberRemoved = false;
        while (!hasMemberRemoved) {
            if (System.currentTimeMillis() - startTime > MEMBER_TERMINATION_TIMEOUT) {
                log.error("Member did not get removed from the topology within timeout period");
                break;
            }
            // Wait until the member gets removed by MemberTerminatedEvent topology receiver
            if (getTerminatingMembers().get(memberId) == null &&
                    getInActiveMembers().get(memberId) == null &&
                    getActivateddMembers().get(memberId) == null &&
                    getCreatedMembers().get(memberId) == null) {
                getTerminatedMembers().remove(memberId);
                hasMemberRemoved = true;
            }
            log.info(String.format("Waiting for [member-id] %s to be terminated...", memberId));
            sleep(2000);
        }
        log.info(String.format("Assert member termination for [member-id] %s took %d ms", memberId,
                System.currentTimeMillis() - startTime));
        assertTrue(String.format("Member [member-id] %s did not get removed from the topology", memberId),
                hasMemberRemoved);
    }

    public void assertClusterMinMemberCount(String applicationId, int minMembers) {
        log.info(String.format("Asserting cluster min member count for [application-id] %s...", applicationId));
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);

        Set<ClusterDataHolder> clusterDataHolderSet = application.getClusterDataRecursively();
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
                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        log.error("Cluster did not activate within timeout period");
                        break;
                    }
                    log.info(String.format("Waiting for [application-id] %s to be terminated...", applicationId));
                    sleep(2000);
                    service = TopologyManager.getTopology().getService(serviceName);
                    assertNotNull(String.format("Service is not found: [application-id] %s [service] %s", applicationId,
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
                                    applicationId, serviceName, clusterId), cluster);
                }
            }
            log.info(String.format("Assert cluster min member count [cluster-id] %s took %d ms", clusterId,
                    System.currentTimeMillis() - startTime));
            assertEquals(String.format("Cluster status did not change to active: [cluster-id] %s", clusterId),
                    clusterActive, true);
        }

    }

    /**
     * Assert application activation
     *
     * @param applicationId
     */
    public boolean assertApplicationUndeploy(String applicationId) {
        log.info(String.format("Asserting application undeploy for [application-id] %s...", applicationId));
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        ApplicationContext applicationContext = null;
        try {
            applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationId);
        } catch (RemoteException e) {
            log.error(
                    String.format("Error while getting the application context for [application-id] %s", applicationId),
                    e);
        }
        while (((application != null) && application.getInstanceContextCount() > 0) || (applicationContext == null
                || applicationContext.getStatus().equals(APPLICATION_STATUS_UNDEPLOYING))) {
            if ((System.currentTimeMillis() - startTime) > APPLICATION_UNDEPLOYMENT_TIMEOUT) {
                log.error(String.format("Application [application-id] %s did not undeploy within timeout period",
                        applicationId));
                break;
            }
            String currentStatus = (applicationContext != null) ? applicationContext.getStatus() : null;
            log.info(String.format("Waiting for [application-id] %s [current-status] %s to be undeployed...",
                    applicationId, currentStatus));
            sleep(2000);
            application = ApplicationManager.getApplications().getApplication(applicationId);
            try {
                applicationContext = AutoscalerServiceClient.getInstance().getApplication(applicationId);
            } catch (RemoteException e) {
                log.error(String.format("Error while getting the application context for [application-id] %s",
                        applicationId), e);
            }
        }
        log.info(String.format("Assert application undeploy for [application-id] %s took %d ms", applicationId,
                System.currentTimeMillis() - startTime));

        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);
        assertNotNull(String.format("Application Context is not found: [application-id] %s", applicationId),
                applicationContext);

        // Trigger a forced undeployment if graceful deployment fails
        if (application.getInstanceContextCount() > 0 || applicationContext.getStatus()
                .equals(APPLICATION_STATUS_UNDEPLOYING)) {
            return false;
        }
        assertEquals(String.format("Application status did not change to Created: [application-id] %s", applicationId),
                APPLICATION_STATUS_CREATED, applicationContext.getStatus());
        return true;
    }

    /**
     * Assert application activation
     *
     * @param applicationId
     */
    public void assertGroupInstanceCount(String applicationId, String groupAlias, int count) {
        log.info(String.format("Asserting group instance count for [application-id] %s, [group-alias] %s...",
                applicationId, groupAlias));
        long startTime = System.currentTimeMillis();
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        if (application != null) {
            Group group = application.getGroupRecursively(groupAlias);
            while (group.getInstanceContextCount() != count) {
                if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                    log.error("Group instance min count check failed within timeout period");
                    break;
                }
                log.info(String.format(
                        "Waiting until [application-id] %s [group-alias] %s instance count to be [count] %d...",
                        applicationId, groupAlias, count));
                sleep(2000);
            }
            log.info(String.format("Assert group instance min count for [group-alias] %s took %d ms", groupAlias,
                    System.currentTimeMillis() - startTime));

            for (GroupInstance instance : group.getInstanceIdToInstanceContextMap().values()) {
                while (!instance.getStatus().equals(GroupStatus.Active)) {
                    if ((System.currentTimeMillis() - startTime) > APPLICATION_ACTIVATION_TIMEOUT) {
                        log.error("Application did not activate within timeout period");
                        break;
                    }
                    log.info(String.format("Waiting for group [alias] %s, [group-instance-id] %s to be active...",
                            instance.getAlias(), instance.getInstanceId()));
                    sleep(2000);
                }
            }
            log.info(String.format("Assert group instance active for [group-alias] %s took %d ms", groupAlias,
                    System.currentTimeMillis() - startTime));
            assertEquals(
                    String.format("Application status did not change to active: [application-id] %s", applicationId),
                    group.getInstanceContextCount(), count);
        }
        assertNotNull(String.format("Application is not found: [application-id] %s", applicationId), application);

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

    private void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (Exception ignored) {
        }
    }
}
