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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.KubernetesConstants;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.Labels;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.ReplicationController;
import org.apache.stratos.kubernetes.client.model.Service;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Notes:
 * Initially it would take some time to pull the docker image and create a pod.
 * As a result live test would fail when running for the first time on a fresh
 * kubernetes cluster.
 */
@Category(org.apache.stratos.kubernetes.client.LiveTests.class)
public class KubernetesApiClientLiveTest extends TestCase{

    private static final Log log = LogFactory.getLog(KubernetesApiClientLiveTest.class);

    private static final int CONTAINER_PORT = 6379;
    private static final int SERVICE_PORT = 4500;
    private static final String DEFAULT_KUBERNETES_MASTER_IP = "172.17.8.100";
    private static final String DEFAULT_DOCKER_IMAGE =  "gurpartap/redis";
    private static final int POD_ACTIVATION_WAIT_TIME = 10000; // 10 seconds

    private KubernetesApiClient client;
    private String dockerImage;
    private String endpoint;

    @BeforeClass
    public void setUp() {
        endpoint = System.getProperty("kubernetes.api.endpoint");
        if (endpoint == null) {
            endpoint = "http://" + DEFAULT_KUBERNETES_MASTER_IP + ":8080/api/" + KubernetesConstants.KUBERNETES_API_VERSION + "/";
        }
        log.info("Provided Kubernetes endpoint using system property [kubernetes.api.endpoint] : " +endpoint);
        client = new KubernetesApiClient(endpoint);

        dockerImage = System.getProperty("docker.image");
        if (dockerImage == null) {
            dockerImage = DEFAULT_DOCKER_IMAGE;
        }
    }

    @AfterClass
    public void tearDown() {
        deleteReplicationControllers();
        deletePods();
        deleteServices();
    }

    @Test
    public void testPodCreationAndDeletion() throws Exception {
        log.info("Testing pod creation...");

        String podId = "stratos-test-pod-1";
        String podName = "stratos-test-pod";

        List<Integer> ports = new ArrayList<Integer>();
        ports.add(CONTAINER_PORT);
        client.createPod(podId, podName, dockerImage, ports);

        Thread.sleep(2000);
        Pod pod = client.getPod(podId);
        assertNotNull(pod);

        Thread.sleep(POD_ACTIVATION_WAIT_TIME);
        pod = client.getPod(podId);
        assertNotNull(pod);
        assertEquals(pod.getCurrentState().getStatus(), KubernetesConstants.POD_STATUS_RUNNING);
        log.info("Pod state changed to running: " + pod.getId());

        log.info("Deleting pod: " + pod.getId());
        client.deletePod(pod.getId());
        assertNull(client.getPod(pod.getId()));
        log.info("Pod deleted successfully: " + pod.getId());
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
    public void testReplicationControllerCreationAndDeletion() throws Exception {
        String replicationControllerId = "stratos-test-rc-1";
        String replicationControllerName = "stratos-test-rc";
        int replicas = 2;

        List<Integer> ports = new ArrayList<Integer>();
        ports.add(CONTAINER_PORT);
        client.createReplicationController(replicationControllerId, replicationControllerName,
                dockerImage, ports, null, replicas);

        // Wait 5s for Pods to be created
        Thread.sleep(5000);
        ReplicationController replicationController = client.getReplicationController(replicationControllerId);

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
        client.deleteReplicationController(replicationControllerId);
        assertNull(client.getReplicationController(replicationControllerId));

        // Delete pods
        for(Pod pod : pods) {
            client.deletePod(pod.getId());
            assertNull(client.getPod(pod.getId()));
        }
    }

    @Test
    public void testServiceCreationAndDeletion() throws Exception {
        String podId = "stratos-test-pod-1";
        String podName = "stratos-test-pod";
        String serviceId = "stratos-test-service-1";
        String serviceName = "stratos-test-service";
        InetAddress address = InetAddress.getByName(new URL(endpoint).getHost());
        String publicIp = address.getHostAddress();

        List<Integer> ports = new ArrayList<Integer>();
        ports.add(CONTAINER_PORT);
        client.createPod(podId, podName, dockerImage, ports);

        Thread.sleep(2000);
        Pod podCreated = client.getPod(podId);
        assertNotNull(podCreated);

        client.createService(serviceId, serviceName, SERVICE_PORT, CONTAINER_PORT, publicIp);

        Thread.sleep(2000);
        Service service = client.getService(serviceId);

        // test recreation using same id
        client.createService(serviceId, serviceName, SERVICE_PORT, CONTAINER_PORT, publicIp);

        Thread.sleep(2000);
        service = client.getService(serviceId);
        assertNotNull(service);

        client.deleteService(serviceId);
        assertNull(client.getService(serviceId));
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
