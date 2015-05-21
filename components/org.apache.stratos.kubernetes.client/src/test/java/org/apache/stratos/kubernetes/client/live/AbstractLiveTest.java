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

package org.apache.stratos.kubernetes.client.live;

import junit.framework.TestCase;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.KubernetesConstants;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.Port;
import org.apache.stratos.kubernetes.client.model.Service;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Abstract live test class.
 */
public class AbstractLiveTest extends TestCase {

    protected static final Log log = LogFactory.getLog(AbstractLiveTest.class);

    protected static final String DEFAULT_KUBERNETES_MASTER_IP = "172.17.8.101";
    protected static final int KUBERNETES_API_PORT = 8080;

    protected static final String DEFAULT_DOCKER_IMAGE = "fnichol/uhttpd";
    protected static final int DEFAULT_CONTAINER_PORT = 80;
    protected static final int SERVICE_PORT = 4500;
    protected static final int POD_ACTIVATION_WAIT_TIME = 10000; // 10 seconds

    protected static final String KUBERNETES_API_ENDPOINT = "kubernetes.api.endpoint";
    protected static final String MINION_PUBLIC_IPS = "minion.public.ips";
    protected static final String DOCKER_IMAGE = "docker.image";
    protected static final String CONTAINER_PORT = "container.port";
    protected static final String TEST_SERVICE_SOCKET = "test.service.socket";
    protected static final String TEST_POD_ACTIVATION = "test.pod.activation";

    protected KubernetesApiClient client;
    protected String dockerImage;
    protected String endpoint;
    protected int containerPort;
    protected boolean testPodActivation;
    protected boolean testServiceSocket;
    protected String[] minionPublicIPs = {"172.17.8.102"};
    protected List<String> podIdList = new CopyOnWriteArrayList<String>();
    protected List<String> serviceIdList = new CopyOnWriteArrayList<String>();

    @BeforeClass
    public void setUp() {
        log.info("Setting up live test...");
        endpoint = System.getProperty(KUBERNETES_API_ENDPOINT);
        if (endpoint == null) {
            endpoint = "http://" + DEFAULT_KUBERNETES_MASTER_IP + ":" + KUBERNETES_API_PORT + "/api/"
                    + KubernetesConstants.KUBERNETES_API_VERSION + "/";
        }
        log.info(KUBERNETES_API_ENDPOINT + ": " + endpoint);
        client = new KubernetesApiClient(endpoint);

        dockerImage = System.getProperty(DOCKER_IMAGE);
        if (dockerImage == null) {
            dockerImage = DEFAULT_DOCKER_IMAGE;
        }
        log.info(DOCKER_IMAGE + ": " + dockerImage);

        String containerPortStr = System.getProperty(CONTAINER_PORT);
        if (StringUtils.isNotBlank(containerPortStr)) {
            containerPort = Integer.parseInt(containerPortStr);
        } else {
            containerPort = DEFAULT_CONTAINER_PORT;
        }
        log.info(CONTAINER_PORT + ": " + containerPort);

        testPodActivation = false;
        String testPodActivationStr = System.getProperty(TEST_POD_ACTIVATION);
        if (StringUtils.isNotBlank(testPodActivationStr)) {
            testPodActivation = Boolean.parseBoolean(testPodActivationStr);
        }
        log.info(TEST_POD_ACTIVATION + ": " + testPodActivation);

        testServiceSocket = false;
        String testServiceSocketStr = System.getProperty(TEST_SERVICE_SOCKET);
        if (StringUtils.isNotBlank(testServiceSocketStr)) {
            testServiceSocket = Boolean.parseBoolean(testServiceSocketStr);
        }
        log.info(TEST_SERVICE_SOCKET + ": " + testServiceSocket);

        String minionPublicIPsStr = System.getProperty(MINION_PUBLIC_IPS);
        if (StringUtils.isNotBlank(minionPublicIPsStr)) {
            minionPublicIPs = minionPublicIPsStr.split(",");
        }
        log.info(MINION_PUBLIC_IPS + ": " + minionPublicIPsStr);
        log.info("Kubernetes live test setup completed");
    }

    @AfterClass
    public void tearDown() {
        log.info("Cleaning kubernetes resources...");
        deleteServices();
        deletePods();
        log.info("Kubernetes resources cleaned");
    }

    protected void createPod(String podId, String podName, String containerPortName, int cpu, int memory) throws KubernetesClientException {
        log.info("Creating pod: [pod] " + podId);
        List<Port> ports = createPorts(containerPortName);
        client.createPod(podId, podName, dockerImage, cpu, memory, ports, null);
        podIdList.add(podId);

        sleep(2000);
        Pod pod = client.getPod(podId);
        assertNotNull(pod);
        log.info("Pod created successfully: [pod] " + podId);

        if (testPodActivation) {
            boolean activated = false;
            long startTime = System.currentTimeMillis();
            while (!activated) {
                if ((System.currentTimeMillis() - startTime) > POD_ACTIVATION_WAIT_TIME) {
                    log.info(String.format("Pod did not activate within %d seconds: [pod] %s",
                            POD_ACTIVATION_WAIT_TIME / 1000, podId));
                    break;
                }

                log.info("Waiting pod status to be changed to running: [pod] " + podId);
                sleep(2000);
                pod = client.getPod(podId);
                if ((pod != null) && (pod.getCurrentState().getStatus().equals(KubernetesConstants.POD_STATUS_RUNNING))) {
                    activated = true;
                    log.info("Pod state changed to running: [pod]" + pod.getId());
                }
            }

            assertNotNull(pod);
            assertEquals(KubernetesConstants.POD_STATUS_RUNNING, pod.getCurrentState().getStatus());
        }
    }

    void deletePod(String podId) throws KubernetesClientException {
        log.info("Deleting pod: " + podId);
        client.deletePod(podId);
        podIdList.remove(podId);

        sleep(1000);
        assertNull(client.getPod(podId));
        log.info("Pod deleted successfully: " + podId);
    }

    public void deletePods() {
        try {
            for (String podId : podIdList) {
                deletePod(podId);
            }
        } catch (KubernetesClientException e) {
            log.error("Could not delete pods", e);
        }
    }

    protected void createService(String serviceId, String serviceName, int servicePort, String containerPortName,
                                 String[] publicIPs) throws KubernetesClientException, InterruptedException, IOException {
        log.info("Creating service...");
        client.createService(serviceId, serviceName, servicePort, containerPortName, publicIPs);
        serviceIdList.add(serviceId);

        sleep(1000);
        Service service = client.getService(serviceId);
        assertNotNull(service);
        log.info("Service creation successful");
    }

    void deleteService(String serviceId) throws KubernetesClientException {
        log.info(String.format("Deleting service: [service] %s", serviceId));
        client.deleteService(serviceId);
        serviceIdList.remove(serviceId);

        sleep(2000);
        assertNull(client.getService(serviceId));
        log.info(String.format("Service deleted successfully: [service] %s", serviceId));
    }

    protected void deleteServices() {
        try {
            for (String serviceId : serviceIdList) {
                deleteService(serviceId);
            }
        } catch (KubernetesClientException e) {
            log.error("Could not delete services", e);
        }
    }

    protected void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignore) {
        }
    }

    protected List<Port> createPorts(String containerPortName) {
        List<Port> ports = new ArrayList<Port>();
        Port port = new Port();
        port.setName(containerPortName);
        port.setContainerPort(containerPort);
        port.setProtocol("tcp");
        ports.add(port);
        return ports;
    }
}
