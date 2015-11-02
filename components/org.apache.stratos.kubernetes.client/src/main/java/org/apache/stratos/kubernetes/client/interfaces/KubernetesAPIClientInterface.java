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
package org.apache.stratos.kubernetes.client.interfaces;

import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;

import java.util.List;
import java.util.Map;


public interface KubernetesAPIClientInterface {

    /**
     * Create pod.
     *
     * @param podId                id of the pod
     * @param podName              pod label
     * @param podLabels            Map of labels to be applied to the pod
     * @param annotations          Map of annotations to be applied to the pod
     * @param dockerImage          docker image name
     * @param cpu                  number of cpu cores
     * @param memory               memory allocation in mega bytes
     * @param ports                ports to be opened
     * @param environmentVariables environment variables
     * @throws KubernetesClientException
     */
    public void createPod(String podId, String podName, Map<String, String> podLabels, Map<String, String> annotations,
                          String dockerImage, String cpu, String memory, List<ContainerPort> ports,
                          List<EnvVar> environmentVariables)
            throws KubernetesClientException;

    /**
     * Get information of a Pod given the PodID
     *
     * @param podId id of the pod
     * @return {@link Pod}
     * @throws KubernetesClientException
     */
    public Pod getPod(String podId) throws KubernetesClientException;

    /**
     * Get all Pods
     *
     * @return Pods
     * @throws KubernetesClientException
     */
    public List<Pod> getPods() throws KubernetesClientException;

    /**
     * Delete a Pod
     *
     * @param podId Id of the Pod to be deleted
     * @throws KubernetesClientException
     */
    public void deletePod(String podId) throws KubernetesClientException;

    /**
     * Create service.
     *
     * @param serviceId         id of the service
     * @param serviceName       service name
     * @param serviceLabels     labels to be applied to the service
     * @param annotations       Map of annotations to be applied to the service
     * @param servicePort       port to be used in the node
     * @param serviceType       port type to be used when creating the service
     * @param containerPortName port name
     * @param containerPort     port to be used in the container
     * @param sessionAffinity   session affinity policy to be used when routing requests to this service
     * @throws KubernetesClientException
     */
    public void createService(String serviceId, String serviceName, Map<String, String> serviceLabels, Map<String,
            String> annotations, int servicePort, String serviceType, String containerPortName, int containerPort,
                              String sessionAffinity)
            throws KubernetesClientException;

    /**
     * Get the Service with the given id.
     *
     * @param serviceId id of the service.
     * @return {@link Service}
     * @throws KubernetesClientException
     */
    public Service getService(String serviceId) throws KubernetesClientException;

    /**
     * Get services.
     *
     * @return array of {@link Service}s
     * @throws KubernetesClientException
     */
    public List<Service> getServices() throws KubernetesClientException;

    /**
     * Delete a service.
     *
     * @param serviceId service id to be deleted.
     * @throws KubernetesClientException
     */
    public void deleteService(String serviceId) throws KubernetesClientException;
}
