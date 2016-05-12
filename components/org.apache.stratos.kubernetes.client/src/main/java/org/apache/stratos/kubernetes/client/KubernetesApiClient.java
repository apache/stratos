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

    public KubernetesApiClient(String endpointUrl, String namespace) throws KubernetesClientException {
        kubernetesClient = new KubernetesClient(endpointUrl);
        if (!namespace.equals(KubernetesConstants.DEFAULT_NAMESPACE)) {
            try {
                setNamespace(namespace);
            } catch (Exception e) {
                throw new KubernetesClientException(e);
            }
        }
    }

    private void setNamespace(String namespace) throws Exception {
        kubernetesClient.setNamespace(namespace);
    }

    /**
     * Create new pod
     *
     * @param podId                Identifier of the pod
     * @param podName              Pod name to be used by the pod label
     * @param podLabels            Map of labels to be applied to the pod
     * @param annotations          Map of annotations to be applied to the pod
     * @param dockerImage          Docker image to be used by the pod
     * @param cpu                  Number of cpu cores
     * @param memory               Memory allocation in megabytes
     * @param ports                Ports exposed by the pod
     * @param environmentVariables Environment variables to be passed to the pod
     * @param imagePullSecrets     Image Pull Secret to be passed to the pod
     * @param imagePullPolicy      Image Pull policy to be passed to the pod
     * @throws KubernetesClientException
     */
    @Override
    public void createPod(String podId, String podName, Map<String, String> podLabels, Map<String, String> annotations,
                          String dockerImage, String cpu, String memory, List<ContainerPort> ports,
                          List<EnvVar> environmentVariables, List<String> imagePullSecrets, String imagePullPolicy)
            throws KubernetesClientException {

        try {

            if (log.isDebugEnabled()) {
                log.debug(String.format("Creating kubernetes pod: [pod-id] %s [pod-name] %s [docker-image] %s " +
                        "[cpu] %s [memory] %s [ports] %s", podId, podLabels, dockerImage, cpu, memory, ports));
            }

            // Create pod definition
            Pod pod = new Pod();
            pod.setApiVersion(Pod.ApiVersion.V_1);
            pod.setKind(KubernetesConstants.KIND_POD);
            pod.setSpec(new PodSpec());
            pod.setMetadata(new ObjectMeta());
            pod.getMetadata().setName(podId);
            pod.getMetadata().setLabels(podLabels);
            pod.getMetadata().setAnnotations(annotations);

            // Set container template
            Container containerTemplate = new Container();
            containerTemplate.setName(podName);
            containerTemplate.setImage(dockerImage);
            containerTemplate.setEnv(environmentVariables);
            List<Container> containerTemplates = new ArrayList<>();
            containerTemplates.add(containerTemplate);
            pod.getSpec().setContainers(containerTemplates);

            // set imagePullSecrets
            if ((imagePullSecrets != null) && (imagePullSecrets.size() > 0)) {
                List<LocalObjectReference> imagePullSecretsRefs = new ArrayList<>();
                for (String pullSecret : imagePullSecrets) {
                    if (pullSecret != null) {
                        imagePullSecretsRefs.add(new LocalObjectReference(pullSecret));
                    }
                }
                if (imagePullSecretsRefs.size() > 0) {
                    pod.getSpec().setImagePullSecrets(imagePullSecretsRefs);
                }
            }

            // Set resource limits
            ResourceRequirements resources = new ResourceRequirements();
            Map<String, Quantity> limits = new HashMap<>();
            limits.put(KubernetesConstants.RESOURCE_CPU, new Quantity(cpu));
            limits.put(KubernetesConstants.RESOURCE_MEMORY, new Quantity(memory));
            resources.setLimits(limits);
            containerTemplate.setResources(resources);

            containerTemplate.setPorts(ports);

            if (imagePullPolicy == null) {
                // default pull policy
                imagePullPolicy = KubernetesConstants.POLICY_PULL_IF_NOT_PRESENT;
            } else if (
                    !imagePullPolicy.equals(KubernetesConstants.POLICY_PULL_ALWAYS) &&
                            !imagePullPolicy.equals(KubernetesConstants.POLICY_PULL_NEVER) &&
                            !imagePullPolicy.equals(KubernetesConstants.POLICY_PULL_IF_NOT_PRESENT)) {

                // pull policy validation failed
                throw new KubernetesClientException("Invalid Image Pull Policy defined : " + imagePullPolicy);
            }

            containerTemplate.setImagePullPolicy(imagePullPolicy);

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
     * @param serviceName       Service name to be used by the label name
     * @param serviceLabels     Service labels map
     * @param annotations       Map of annotations to be applied to the service
     * @param servicePort       Port to be exposed by the kubernetes node
     * @param containerPortName Container port name defined in the port label
     * @param containerPort     Container port
     * @param sessionAffinity   Session affinity configuration
     * @param serviceType       Service type
     * @throws KubernetesClientException
     */
    @Override
    public void createService(String serviceId, String serviceName, Map<String, String> serviceLabels, Map<String,
            String> annotations, int servicePort, String serviceType, String containerPortName, int containerPort,
                              String sessionAffinity)
            throws KubernetesClientException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(
                        String.format("Creating kubernetes service: [service-id] %s [service-name] %s [service-port] " +
                                        "%d [container-port-name] %s [service-type] %s", serviceId, serviceName,
                                servicePort, containerPortName, serviceType));
            }

            // Create service definition
            Service service = new Service();
            service.setSpec(new ServiceSpec());
            service.setMetadata(new ObjectMeta());
            service.setApiVersion(Service.ApiVersion.V_1);
            service.setKind(KubernetesConstants.KIND_SERVICE);
            service.getMetadata().setName(serviceId);
            service.getSpec().setSessionAffinity(sessionAffinity);
            service.getMetadata().setAnnotations(annotations);

            if (serviceType.equals(KubernetesConstants.NODE_PORT)) {
                service.getSpec().setType(KubernetesConstants.NODE_PORT);
            } else {
                service.getSpec().setType(KubernetesConstants.CLUSTER_IP);
            }

            // Set port
            List<ServicePort> ports = new ArrayList<>();
            ServicePort port = new ServicePort();
            port.setName(containerPortName);
            port.setPort(containerPort);
            port.setTargetPort(new IntOrString(containerPort));
            if (serviceType.equals(KubernetesConstants.NODE_PORT)) {
                port.setNodePort(servicePort);
            }
            ports.add(port);
            service.getSpec().setPorts(ports);

            // Set labels
            service.getMetadata().setLabels(serviceLabels);

            // Set service selector
            Map<String, String> selector = new HashMap<>();
            selector.put(KubernetesConstants.SERVICE_SELECTOR_LABEL, serviceName);
            service.getSpec().setSelector(selector);

            // Invoke the api to create the service
            kubernetesClient.createService(service);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Kubernetes service created successfully: [service-id] %s [service-name] %s " +
                                "[node-port] %d [container-port-name] %s [container-port] %d", serviceId, serviceName,
                        servicePort, containerPortName, containerPort));
            }
        } catch (Exception e) {
            String message = String.format("Could not create kubernetes service: [service-id] %s [service-name] %s " +
                            "[node-port] %d [container-port-name] %s [container-port] %d", serviceId, serviceName,
                    servicePort, containerPortName, containerPort);
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