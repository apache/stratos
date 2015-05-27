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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.Service;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Live test to clean kubernetes cluster.
 */
public class KubernetesClusterCleanTest extends AbstractLiveTest {

    private static final Log log = LogFactory.getLog(KubernetesClusterCleanTest.class);

    @Test
    public void testClean() {
        try {
            log.info("Cleaning kubernetes cluster...");
            List<Pod> podList = getPods();
            while ((podList != null) && (podList.size() > 0)) {
                for (Pod pod : podList) {
                    deletePod(pod.getId());
                }
                podList = client.getPods();
            }

            List<Service> serviceList = getServices();
            while ((serviceList != null) && (serviceList.size() > 0)) {
                for (Service service : serviceList) {
                    deleteService(service.getId());
                }
                serviceList = getServices();
            }
            log.info("Kubernetes cluster cleaned successfully");
        } catch (Exception e) {
            log.error(e);
        }
    }

    private List<Pod> getPods() throws KubernetesClientException {
        List<Pod> podList = new ArrayList<Pod>();
        for(Pod pod : client.getPods()) {
            if(!pod.getId().startsWith("kube")) {
                podList.add(pod);
            }
        }
        return podList;
    }

    private List<Service> getServices() throws KubernetesClientException {
        List<Service> serviceList = new ArrayList<Service>();
        for (Service service : client.getServices()) {
            if (!service.getId().startsWith("kube")) {
                serviceList.add(service);
            }
        }
        return serviceList;
    }
}
