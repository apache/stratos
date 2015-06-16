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

import io.fabric8.kubernetes.api.model.Pod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.net.Socket;

/**
 * Notes:
 * Please ssh into the kubernetes custer and pull the docker image before running
 * the live tests. Otherwise tests would fail when running for the first time on a fresh
 * kubernetes cluster.
 * <p/>
 * Caution!
 * At the end of the tests it will remove all the replication controllers, pods and services
 * available in the given kubernetes environment.
 */
@Category(org.apache.stratos.kubernetes.client.LiveTests.class)
public class KubernetesApiClientLiveTest extends AbstractLiveTest {

    private static final Log log = LogFactory.getLog(KubernetesApiClientLiveTest.class);

    @Test
    public void testPodCreation() throws Exception {
        log.info("Testing pod creation...");

        createPod("stratos-test-pod-1", "stratos-test-pod", "http-1", 1, 512);
        createPod("stratos-test-pod-2", "stratos-test-pod", "http-1", 2, 512);

        deletePod("stratos-test-pod-1");
        deletePod("stratos-test-pod-2");
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
    public void testServiceCreation() throws Exception {
        log.info("Testing service creation...");

        String serviceId = "tomcat-domain-1";
        String serviceName = "stratos-test-pod";
        String containerPortName = "http-1";

        createService(serviceId, serviceName, SERVICE_PORT, containerPortName, containerPort, minionPublicIPs);

        createPod("stratos-test-pod-3", serviceName, containerPortName, 1, 512);
        createPod("stratos-test-pod-4", serviceName, containerPortName, 2, 512);

        if (testServiceSocket) {
            // test service accessibility
            log.info(String.format("Connecting to service: [portal] %s:%d", minionPublicIPs.get(0), SERVICE_PORT));
            sleep(4000);
            Socket socket = new Socket(minionPublicIPs.get(0), SERVICE_PORT);
            assertTrue(socket.isConnected());
            log.info(String.format("Connecting to service successful: [portal] %s:%d", minionPublicIPs.get(0),
                    SERVICE_PORT));
        }

        deleteService(serviceId);

        deletePod("stratos-test-pod-3");
        deletePod("stratos-test-pod-4");
    }
}
