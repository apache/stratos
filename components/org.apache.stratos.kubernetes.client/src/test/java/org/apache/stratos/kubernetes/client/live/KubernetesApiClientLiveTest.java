/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.stratos.kubernetes.client.live;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.KubernetesConstants;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Notes:
 * Please ssh into the kubernetes custer and pull the docker image before running
 * the live tests. Otherwise tests would fail when running for the first time on a fresh
 * kubernetes cluster.
 *
 * Caution!
 * At the end of the tests it will remove all the replication controllers, pods and services
 * available in the given kubernetes environment.
 */
@Category(org.apache.stratos.kubernetes.client.LiveTests.class)
public class KubernetesApiClientLiveTest extends TestCase{

    private static final Log log = LogFactory.getLog(KubernetesApiClientLiveTest.class);

    private static final int DEFAULT_CONTAINER_PORT = 6379; //80;
    private static final int SERVICE_PORT = 4500;
    private static final String DEFAULT_KUBERNETES_MASTER_IP = "172.17.8.100";
    private static final String DEFAULT_DOCKER_IMAGE =  "gurpartap/redis"; //"stratos/php:4.1.0-alpha";
    private static final int POD_ACTIVATION_WAIT_TIME = 10000; // 10 seconds

    private KubernetesApiClient client;
    private String dockerImage;
    private String endpoint;
    private int containerPort;
    private boolean testPodActivation;
    private boolean testProxyServiceSocket;

    @BeforeClass
    public void setUp() {
        log.info("Setting up live test...");
        endpoint = System.getProperty("kubernetes.api.endpoint");
        if (endpoint == null) {
            endpoint = "http://" + DEFAULT_KUBERNETES_MASTER_IP + ":8080/api/" + KubernetesConstants.KUBERNETES_API_VERSION + "/";
        }
        log.info("Kubernetes endpoint: " +endpoint);
        client = new KubernetesApiClient(endpoint);

        dockerImage = System.getProperty("docker.image");
        if (dockerImage == null) {
            dockerImage = DEFAULT_DOCKER_IMAGE;
        }
        log.info("Docker image: " + dockerImage);

        String containerPortStr = System.getProperty("container.port");
        if(StringUtils.isNotBlank(containerPortStr)) {
            containerPort = Integer.parseInt(containerPortStr);
        } else {
            containerPort = DEFAULT_CONTAINER_PORT;
        }

        String testPodActivationStr = System.getProperty("test.pod.activation");
        if(StringUtils.isNotBlank(testPodActivationStr)) {
            testPodActivation = Boolean.parseBoolean(testPodActivationStr);
        }

        String testProxyServiceSocketStr = System.getProperty("test.proxy.service.socket");
        if(StringUtils.isNotBlank(testProxyServiceSocketStr)) {
            testProxyServiceSocket = Boolean.parseBoolean(testProxyServiceSocketStr);
        }
        log.info("Live test setup completed");
    }

    @AfterClass
    public void tearDown() {
        log.info("Cleaning kubernetes resources...");
        deleteReplicationControllers();
        deletePods();
        deleteServices();
        log.info("Kubernetes resources cleaned");
    }

    @Test
    public void testPodCreation() throws Exception {
        log.info("Testing pod creation...");

        String podId = "stratos-test-pod-1";
        String podName = "stratos-test-pod";
        String containerPortName = "http-1";

        log.info("Creating pod: [pod-id] " + podId);
        List<Port> ports = createPorts(containerPortName);
        client.createPod(podId, podName, dockerImage, ports);

        Thread.sleep(2000);
        Pod pod = client.getPod(podId);
        assertNotNull(pod);
        log.info("Pod created successfully: [pod-id] " + podId);

        if(testPodActivation) {
            log.info("Waiting pod status to be changed to running: [pod-id] " + podId);
            Thread.sleep(POD_ACTIVATION_WAIT_TIME);
            pod = client.getPod(podId);
            assertNotNull(pod);
            assertEquals(KubernetesConstants.POD_STATUS_RUNNING, pod.getCurrentState().getStatus());
            log.info("Pod state changed to running: [pod-id]" + pod.getId());
        }

        log.info("Deleting pod: " + pod.getId());
        client.deletePod(pod.getId());
        assertNull(client.getPod(pod.getId()));
        log.info("Pod deleted successfully: " + pod.getId());
    }

    private List<Port> createPorts(String containerPortName) {
        List<Port> ports = new ArrayList<Port>();
        Port port = new Port();
        port.setName(containerPortName);
        port.setContainerPort(containerPort);
        port.setHostPort(SERVICE_PORT);
        ports.add(port);
        return ports;
    }

    @Test
    public void testDeletingAnNonExistingPod() {
        try {
            client.deletePod("-1234");
        } catch (Exception e) {
            assertEquals(true, e instanceof KubernetesClientException);
        }
    }

    @Test
    public void testReplicationControllerCreation() throws Exception {
        log.info("Testing replication controller creation...");

        String replicationControllerId = "stratos-test-rc-1";
        String replicationControllerName = "stratos-test-rc";
        String containerPortName = "http-1";
        int replicas = 2;

        List<Port> ports = createPorts(containerPortName);
        client.createReplicationController(replicationControllerId, replicationControllerName,
                dockerImage, ports, null, replicas);

        // Wait 5s for Pods to be created
        Thread.sleep(5000);
        ReplicationController replicationController = client.getReplicationController(replicationControllerId);
        assertNotNull(replicationController);
        log.info("Replication controller created successfully");

        // Validate recreation using same id
        log.info("Testing replication controller re-creation with an existing id: " + replicationController.getId());
        client.createReplicationController(replicationControllerId, replicationControllerName,
                dockerImage, ports, null, replicas);
        log.info("Replication controller re-creation with an existing id was successful");

        // Validate pod count
        Labels label = new Labels();
        label.setName(replicationControllerName);
        List<Pod> pods = client.queryPods(new Labels[]{label});
        assertEquals(replicas, pods.size());

        // Delete replication controller
        log.info("Deleting replication controller: [replication-controller-id]" + replicationControllerId);
        client.deleteReplicationController(replicationControllerId);
        assertNull(client.getReplicationController(replicationControllerId));
        log.info("Replication controller deleted successfully: [replication-controller-id]" + replicationControllerId);

        // Delete pods
        for(Pod pod : pods) {
            log.info("Deleting pod: [pod-id] " + pod.getId());
            client.deletePod(pod.getId());
            assertNull(client.getPod(pod.getId()));
            log.info("Pod deleted successfully: [pod-id] " + pod.getId());
        }
    }

    @Test
    public void testServiceCreation() throws Exception {
        log.info("Testing service creation...");

        String podId = "stratos-test-pod-1";
        String podName = "stratos-test-pod";
        String serviceId = "stratos-test-service-1";
        String serviceName = "stratos-test-service";
        String containerPortName = "http-1";

        String publicIp = new URL(endpoint).getHost();

        log.info("Creating pod...");
        List<Port> ports = createPorts(containerPortName);
        client.createPod(podId, podName, dockerImage, ports);

        Thread.sleep(2000);
        Pod podCreated = client.getPod(podId);
        assertNotNull(podCreated);
        log.info("Pod created successfully");

        log.info("Creating service...");
        client.createService(serviceId, serviceName, SERVICE_PORT, containerPortName, publicIp);

        Thread.sleep(2000);
        Service service = client.getService(serviceId);
        assertNotNull(service);
        log.info("Service creation successful");

        // test recreation using same id
        log.info("Creating a service with an existing id...");
        client.createService(serviceId, serviceName, SERVICE_PORT, containerPortName, publicIp);

        Thread.sleep(2000);
        service = client.getService(serviceId);
        assertNotNull(service);
        log.info("Service re-creation with an existing id successful");

        if(testProxyServiceSocket) {
            // test service proxy is accessibility
            log.info("Connecting to service proxy...");
            Socket socket = new Socket(publicIp, SERVICE_PORT);
            assertTrue(socket.isConnected());
            log.info("Connecting to service proxy successful");
        }

        log.info("Deleting service...");
        client.deleteService(serviceId);
        assertNull(client.getService(serviceId));
        log.info("Service deleted successfully");
    }

    public void deleteReplicationControllers() {
        try {
            List<ReplicationController> replicationControllers = client.getReplicationControllers();
            for(ReplicationController replicationController : replicationControllers) {
                client.deleteReplicationController(replicationController.getId());
            }
        } catch (KubernetesClientException e) {
            log.error("Could not delete replication controllers", e);
        }
    }

    public void deleteServices() {
        try {
            List<Service> services = client.getServices();
            for(Service service : services) {
                if(!service.getId().contains("kubernetes")) {
                    client.deleteService(service.getId());
                }
            }
        } catch (KubernetesClientException e) {
            log.error("Could not delete services", e);
        }
    }

    public void deletePods() {
        try {
            List<Pod> pods = client.getPods();
            for(Pod replicationController : pods) {
                client.deletePod(replicationController.getId());
            }
        } catch (KubernetesClientException e) {
            log.error("Could not delete pods", e);
        }
    }
}
