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
package org.apache.stratos.kubernetes.client;

import io.fabric8.kubernetes.api.KubernetesClient;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.resource.Quantity;
import io.fabric8.kubernetes.api.model.util.IntOrString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.interfaces.KubernetesAPIClientInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KubernetesApiClient implements KubernetesAPIClientInterface {

    private static final Log log = LogFactory.getLog(KubernetesApiClient.class);

    private KubernetesClient kubernetesClient;

    public KubernetesApiClient(String endpointUrl) {
        kubernetesClient = new KubernetesClient(endpointUrl);
    }

    /**
     * Create new pod
     *
     * @param podId                Identifier of the pod
     * @param podLabel             Pod name to be used by the pod label
     * @param dockerImage          Docker image to be used by the pod
     * @param cpu                  Number of cpu cores
     * @param memory               Memory allocation in megabytes
     * @param ports                Ports exposed by the pod
     * @param environmentVariables Environment variables to be passed to the pod
     * @throws KubernetesClientException
     */
    @Override
    public void createPod(String podId, String podLabel, String dockerImage, int cpu, int memory, List<ContainerPort> ports,
                          List<EnvVar> environmentVariables)
            throws KubernetesClientException {

        try {
            int memoryInMB = 1024 * 1024 * memory;
            if (log.isDebugEnabled()) {
                log.debug(String.format("Creating kubernetes pod: [pod-id] %s [pod-name] %s [docker-image] %s " +
                                "[cpu] %d [memory] %d MB [ports] %s",
                        podId, podLabel, dockerImage, cpu, memoryInMB, ports));
            }

            // Create pod definition
            Pod pod = new Pod();
            pod.setApiVersion(Pod.ApiVersion.V_1_BETA_3);
            pod.setKind(KubernetesConstants.KIND_POD);

            pod.setSpec(new PodSpec());
            pod.setMetadata(new ObjectMeta());

            pod.getMetadata().setName(podId);

            Map<String, String> labels = new HashMap<String, String>();
            labels.put(KubernetesConstants.LABEL_NAME, podLabel);
            pod.getMetadata().setLabels(labels);

            // Set container template
            Container containerTemplate = new Container();
            containerTemplate.setName(podLabel);
            containerTemplate.setImage(dockerImage);
            containerTemplate.setEnv(environmentVariables);
            List<Container> containerTemplates = new ArrayList<Container>();
            containerTemplates.add(containerTemplate);
            pod.getSpec().setContainers(containerTemplates);

            // Set resource limits
            ResourceRequirements resources = new ResourceRequirements();
            Map<String, Quantity> limits = new HashMap<String, Quantity>();
            limits.put(KubernetesConstants.RESOURCE_CPU, new Quantity(String.valueOf(cpu)));
            limits.put(KubernetesConstants.RESOURCE_MEMORY, new Quantity(String.valueOf(memoryInMB)));
            resources.setLimits(limits);
            containerTemplate.setResources(resources);

            containerTemplate.setPorts(ports);
            containerTemplate.setImagePullPolicy(KubernetesConstants.POLICY_PULL_IF_NOT_PRESENT);
            if (environmentVariables != null) {
                containerTemplate.setEnv(environmentVariables);
            }

            // Invoke the api to create the pod
            kubernetesClient.createPod(pod);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes pod created successfully: [pod-id] %s", podId));
            }
        } catch (Exception e) {
            String msg = String.format("Could not create kubernetes pod: [pod-id] %s", podId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public Pod getPod(String podId) throws KubernetesClientException {
        try {
            return kubernetesClient.getPod(podId);
        } catch (Exception e) {
            String msg = String.format("Could not retrieve kubernetes pod: [pod-id] %s", podId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public List<Pod> getPods() throws KubernetesClientException {
        try {
            return kubernetesClient.getPods().getItems();
        } catch (Exception e) {
            String msg = "Error while retrieving kubernetes pods.";
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public void deletePod(String podId) throws KubernetesClientException {
        try {
            kubernetesClient.deletePod(podId);
        } catch (Exception e) {
            String message = String.format("Could not delete kubernetes pod: [pod-id] %s", podId);
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    /**
     * Create kubernetes service
     *
     * @param serviceId         Service id
     * @param serviceLabel      Service name to be used by the label name
     * @param servicePort       Port to be exposed by the service
     * @param containerPortName Container port name defined in the port label
     * @param containerPort     Container port
     * @param publicIPs         Public IP addresses of the minions
     * @param sessionAffinity   Session affinity configuration
     * @throws KubernetesClientException
     */
    @Override
    public void createService(String serviceId, String serviceLabel, int servicePort,
                              String containerPortName, int containerPort, List<String> publicIPs,
                              String sessionAffinity)
            throws KubernetesClientException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Creating kubernetes service: [service-id] %s [service-name] %s [service-port] %d " +
                                "[container-port-name] %s", serviceId, serviceLabel, servicePort,
                        containerPortName));
            }

            // Create service definition
            Service service = new Service();
            service.setSpec(new ServiceSpec());
            service.setMetadata(new ObjectMeta());

            service.setApiVersion(Service.ApiVersion.V_1_BETA_3);
            service.setKind(KubernetesConstants.KIND_SERVICE);

            service.getMetadata().setName(serviceId);
            service.getSpec().setPublicIPs(publicIPs);
            service.getSpec().setSessionAffinity(sessionAffinity);

            // Set port
            List<ServicePort> ports = new ArrayList<ServicePort>();
            ServicePort port = new ServicePort();
            port.setName(containerPortName);
            port.setPort(servicePort);
            port.setTargetPort(new IntOrString(containerPort));
            ports.add(port);
            service.getSpec().setPorts(ports);

            // Set label
            Map<String, String> labels = new HashMap<String, String>();
            labels.put(KubernetesConstants.LABEL_NAME, serviceLabel);
            service.getMetadata().setLabels(labels);

            // Set service selector
            Map<String, String> selector = new HashMap<String, String>();
            selector.put(KubernetesConstants.LABEL_NAME, serviceLabel);
            service.getSpec().setSelector(selector);

            // Invoke the api to create the service
            kubernetesClient.createService(service);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes service created successfully: [service-id] %s [service-name] %s [service-port] %d " +
                        "[container-port-name] %s", serviceId, serviceLabel, servicePort, containerPortName));
            }
        } catch (Exception e) {
            String message = String.format("Could not create kubernetes service: [service-id] %s [service-name] %s [service-port] %d " +
                    "[container-port-name] %s", serviceId, serviceLabel, servicePort, containerPortName);
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    @Override
    public Service getService(String serviceId)
            throws KubernetesClientException {
        try {
            return kubernetesClient.getService(serviceId);
        } catch (Exception e) {
            String msg = String.format("Could not retrieve kubernetes service: [service-id] %s", serviceId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public List<Service> getServices() throws KubernetesClientException {
        try {
            return kubernetesClient.getServices().getItems();
        } catch (Exception e) {
            String msg = "Could not retrieve kubernetes services";
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public void deleteService(String serviceId)
            throws KubernetesClientException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Deleting kubernetes service: [service-id] %s", serviceId));
            }

            kubernetesClient.deleteService(serviceId);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes service deleted successfully: [service-id] %s", serviceId));
            }
        } catch (Exception e) {
            String msg = String.format("Could not delete kubernetes service: [service-id] %s", serviceId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }
}
