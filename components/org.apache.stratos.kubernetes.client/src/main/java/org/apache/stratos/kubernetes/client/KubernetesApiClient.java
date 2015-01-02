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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.interfaces.KubernetesAPIClientInterface;
import org.apache.stratos.kubernetes.client.model.*;
import org.apache.stratos.kubernetes.client.rest.HttpResponse;
import org.apache.stratos.kubernetes.client.rest.RestClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class KubernetesApiClient implements KubernetesAPIClientInterface {

    private static final Log log = LogFactory.getLog(KubernetesApiClient.class);
    public static final String CONTEXT_PODS = "pods";
    private RestClient restClient;
    private String baseURL;

    public KubernetesApiClient(String endpointUrl) {
        restClient = new RestClient();
        baseURL = endpointUrl;
    }

    @Override
    public void createPod(String podId, String podName, String dockerImage, List<Integer> containerPorts)
            throws KubernetesClientException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Creating pod: [pod-id] %s [pod-name] %s [docker-image] %s [container-ports] %s",
                    podId, podName, dockerImage, containerPorts));
        }

        // Create pod definition
        Pod pod = new Pod();
        pod.setApiVersion(KubernetesConstants.KUBERNETES_API_VERSION);
        pod.setId(podId);
        pod.setKind(KubernetesConstants.KIND_POD);

        // Set pod labels
        Labels podLabels = new Labels();
        podLabels.setName(podName);
        pod.setLabels(podLabels);

        State desiredState = new State();
        Manifest manifest = new Manifest();
        manifest.setId(podId);
        manifest.setVersion(KubernetesConstants.KUBERNETES_API_VERSION);

        // Set container template
        Container containerTemplate = new Container();
        containerTemplate.setName(podName);
        containerTemplate.setImage(dockerImage);
        containerTemplate.setPorts(convertIntPortListToPortList(containerPorts));
        containerTemplate.setImagePullPolicy(KubernetesConstants.POLICY_PULL_IF_NOT_PRESENT);

        manifest.addContainer(containerTemplate);
        desiredState.setManifest(manifest);
        pod.setDesiredState(desiredState);

        // Invoke the api to create the pod
        createPod(pod);

        if (log.isInfoEnabled()) {
            log.info(String.format("Pod created successfully: [pod-id] %s", podId));
        }
    }

    @Override
    public Pod getPod(String podId) throws KubernetesClientException {
        try {
            URI uri = new URIBuilder(baseURL + "pods/" + podId).build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse(String.format("Could not retrieve pod: [pod-id] %s", podId), response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            return gson.fromJson(content, Pod.class);
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            String msg = String.format("Could not retrieve pod: [pod-id] %s", podId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public List<Pod> getPods() throws KubernetesClientException {

        try {
            URI uri = new URIBuilder(baseURL + "pods").build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse("Pod retrieval failed.", response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            PodList result = gson.fromJson(content, PodList.class);

            List<Pod> podList = new ArrayList<Pod>();
            if((result != null) && (result.getItems() != null)) {
                for(Pod pod : result.getItems()) {
                    if(pod != null) {
                        podList.add(pod);
                    }
                }
            }
            return podList;
        } catch (Exception e) {
            String msg = "Error while retrieving Pods.";
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    private void createPod(Pod pod) throws KubernetesClientException {
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String content = gson.toJson(pod);
            if (log.isDebugEnabled()) {
                log.debug("Create pod request body: " + content);
            }
            URI uri = new URIBuilder(baseURL + CONTEXT_PODS).build();
            HttpResponse response = restClient.doPost(uri, content);
            handleNullResponse(String.format("Could not create pod: [pod-id] %s", pod.getId()), response);

            if (response.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn(String.format("Pod already created: [pod-id] %s", pod.getId()));
                return;
            }

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED &&
                    response.getStatusCode() != HttpStatus.SC_OK) {
                String msg = String.format("Could not create pod: [pod-id] %s [message] %s", pod.getId());
                log.error(msg);
                throw new KubernetesClientException(msg);
            }
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            String msg = String.format("Could not create pod: [pod-id] %s", pod.getId());
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public void deletePod(String podId) throws KubernetesClientException {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Deleting pod: [pod-id] %s", podId));
            }

            URI uri = new URIBuilder(baseURL + "pods/" + podId).build();
            HttpResponse response = restClient.doDelete(uri);

            handleNullResponse("Pod [" + podId + "] deletion failed.", response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                String message = String.format("Pod not found: [pod-id] %s", podId);
                log.warn(message);
                return;
            }

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED &&
                    response.getStatusCode() != HttpStatus.SC_OK) {
                String message = String.format("Could not delete pod: [pod-id] %s [message] %s",
                        podId, response.getKubernetesResponse().getMessage());
                log.error(message);
                throw new KubernetesClientException(message);
            }

            if(log.isInfoEnabled()) {
                log.info(String.format("Pod deleted successfully: [pod-id] %s", podId));
            }
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            String message = String.format("Could not delete pod: [pod-id] %s", podId);
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    @Override
    public ReplicationController getReplicationController(String replicationControllerId)
            throws KubernetesClientException {

        try {
            URI uri = new URIBuilder(baseURL + "replicationControllers/" + replicationControllerId).build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse(String.format("Could not retrieve replication controller: " +
                    "[replication-controller-id] %s", replicationControllerId), response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            return gson.fromJson(content, ReplicationController.class);
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Error while retrieving Replication Controller info with ID: " + replicationControllerId;
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public List<ReplicationController> getReplicationControllers() throws KubernetesClientException {

        try {
            URI uri = new URIBuilder(baseURL + "replicationControllers").build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse("Could not retrieve replication controllers", response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            ReplicationControllerList controllerList = gson.fromJson(content, ReplicationControllerList.class);

            List<ReplicationController> replicationControllers = new ArrayList<ReplicationController>();
            if((controllerList != null) && (controllerList.getItems() != null)) {
                for (ReplicationController replicationController : controllerList.getItems()) {
                    if (replicationController != null) {
                        replicationControllers.add(replicationController);
                    }
                }
            }
            return replicationControllers;
        } catch (Exception e) {
            String msg = "Error while retrieving Replication Controllers.";
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public void createReplicationController(String replicationControllerId, String replicationControllerName,
                                            String dockerImage, List<Integer> containerPorts,
                                            EnvironmentVariable[] environmentVariables, int replicas) throws KubernetesClientException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Creating replication controller: [replication-controller-id] %s [name] %s " +
                                "[docker-image] %s [container-ports] %s [replicas] %d", replicationControllerId, replicationControllerName,
                        dockerImage, containerPorts, replicas));
            }

            // Create replication controller
            ReplicationController replicationController = new ReplicationController();
            replicationController.setId(replicationControllerId);
            replicationController.setKind(KubernetesConstants.KIND_REPLICATION_CONTROLLER);
            replicationController.setApiVersion(KubernetesConstants.KUBERNETES_API_VERSION);

            // Set replication controller state
            State replicationControllerDesiredState = new State();
            replicationControllerDesiredState.setReplicas(replicas);
            Selector replicationControllerSelector = new Selector();
            replicationControllerSelector.setName(replicationControllerName);
            replicationControllerDesiredState.setReplicaSelector(replicationControllerSelector);

            // Create pod template
            Pod podTemplate = new Pod();
            State desiredPodState = new State();

            Manifest manifest = new Manifest();
            manifest.setVersion(KubernetesConstants.KUBERNETES_API_VERSION);
            manifest.setId(replicationControllerId);

            // Create container template
            Container containerTemplate = new Container();
            containerTemplate.setName(replicationControllerName);
            containerTemplate.setImage(dockerImage);
            containerTemplate.setImagePullPolicy(KubernetesConstants.POLICY_PULL_IF_NOT_PRESENT);
            if(environmentVariables != null) {
                containerTemplate.setEnv(environmentVariables);
            }

            // Set container ports
            List<Port> ports = convertIntPortListToPortList(containerPorts);
            containerTemplate.setPorts(ports);
            manifest.addContainer(containerTemplate);

            desiredPodState.setManifest(manifest);
            podTemplate.setDesiredState(desiredPodState);

            // Set pod template labels
            Labels podTemplateLabels = new Labels();
            podTemplateLabels.setName(replicationControllerName);
            podTemplate.setLabels(podTemplateLabels);

            replicationControllerDesiredState.setPodTemplate(podTemplate);
            replicationController.setDesiredState(replicationControllerDesiredState);

            Labels replicationControllerLabels = new Labels();
            replicationControllerLabels.setName(replicationControllerName);
            replicationController.setLabels(replicationControllerLabels);

            // Invoke the api to create the replicate controller
            createReplicationController(replicationController);

            if (log.isInfoEnabled()) {
                log.info(String.format("Replication controller created successfully: [replication-controller-id] " +
                                "%s [name] %s [docker-image] %s [container-port] %s [replicas] %d",
                        replicationControllerId, replicationControllerName, dockerImage, containerPorts, replicas));
            }
        } catch (Exception e) {
            String message = "Could not create replication controller: [replication-controller-id] " + replicationControllerId;
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    private List<Port> convertIntPortListToPortList(List<Integer> containerPorts) {
        List<Port> ports = new ArrayList<Port>();
        for (int containerPort : containerPorts) {
            Port port = new Port();
            port.setContainerPort(containerPort);
            // A known issue related to setting host port:
            // https://github.com/openshift/origin/issues/210
            port.setHostPort(containerPort);
            ports.add(port);
        }
        return ports;
    }

    private void createReplicationController(ReplicationController replicationController)
            throws KubernetesClientException {

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String content = gson.toJson(replicationController);
            if (log.isDebugEnabled()) {
                log.debug("CreateReplicationController request body : " + content);
            }

            URI uri = new URIBuilder(baseURL + "replicationControllers").build();
            HttpResponse response = restClient.doPost(uri, content);

            handleNullResponse("Could not create replication controller: [replication-controller-id] " +
                    replicationController.getId(), response);

            if (response.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn("Replication controller already created: [replication-controller-id] " + replicationController.getId());
                return;
            }

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED &&
                    response.getStatusCode() != HttpStatus.SC_OK) {
                String message = "Could not create replication controller: [replication-controller-id] " +
                        replicationController.getId() + " [message] " + response.getKubernetesResponse().getMessage();
                log.error(message);
                throw new KubernetesClientException(message);
            }

        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            String msg = "Error while creating replication controller: "
                    + replicationController;
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public void updateReplicationController(ReplicationController replicationController)
            throws KubernetesClientException {

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String content = gson.toJson(replicationController);
            if (log.isDebugEnabled()) {
                log.debug("Update kubernetes replication controller request body: "
                        + content);
            }

            URI uri = new URIBuilder(baseURL + "replicationControllers/" + replicationController.getId()).build();
            HttpResponse response = restClient.doPut(uri, content);

            handleNullResponse("Could not update kubernetes replication controller: [replication-controller-id] " +
                    replicationController.getId(), response);

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED
                    && response.getStatusCode() != HttpStatus.SC_OK) {
                String message = "Could not update kubernetes replication controller: [replication-controller-id] " +
                        replicationController.getId() + ": " + response.getReason();
                log.error(message);
                throw new KubernetesClientException(message);
            }

        } catch (KubernetesClientException e) {
            String message = "Could not update kubernetes replication controller: [replication-controller-id] " +
                    replicationController.getId();
            log.error(message, e);
            throw e;
        } catch (Exception e) {
            String message = "Could not update kubernetes replication controller: [replication-controller-id] " +
                    replicationController.getId();
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    @Override
    public void deleteReplicationController(String replicationControllerId)
            throws KubernetesClientException {

        try {
            if(log.isInfoEnabled()) {
                log.info("Deleting replication controller: [replication-controller-id] " +
                        replicationControllerId);
            }

            URI uri = new URIBuilder(baseURL + "replicationControllers/" + replicationControllerId).build();
            HttpResponse response = restClient.doDelete(uri);

            handleNullResponse("Could not delete replication controller: [replication-controller-id] " +
                    replicationControllerId, response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                String message = "Replication controller does not exist: [replication-controller-id] "
                        + replicationControllerId;
                log.warn(message);
                return;
            }

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED &&
                    response.getStatusCode() != HttpStatus.SC_OK) {
                String message = "Could not delete replication controller: [replication-controller-id] " +
                        replicationControllerId + ": " + response.getReason();
                log.error(message);
                throw new KubernetesClientException(message);
            }

            if(log.isInfoEnabled()) {
                log.info("Replication controller deleted successfully: [replication-controller-id] " +
                        replicationControllerId);
            }
        } catch (KubernetesClientException e) {
            String message = "Could not delete replication controller: [replication-controller-id] " +
                    replicationControllerId;
            log.error(message, e);
            throw e;
        } catch (Exception e) {
            String message = "Could not delete replication controller: [replication-controller-id] " +
                    replicationControllerId;
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    @Override
    public void createService(String serviceId, String serviceName, int servicePort,
                              int containerPort, String publicIp) throws KubernetesClientException {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Creating service: [service-id] %s [service-name] %s [service-port] %d " +
                        "[container-port] %d, [public-ip] %s", serviceId, serviceName, servicePort, containerPort, publicIp));
            }

            // Create service definition
            Service service = new Service();
            service.setApiVersion(KubernetesConstants.KUBERNETES_API_VERSION);
            service.setContainerPort(String.valueOf(containerPort));
            service.setPort(servicePort);
            service.setId(serviceId);
            service.setKind(KubernetesConstants.KIND_SERVICE);
            service.setPublicIPs(new String[]{publicIp});

            // Set service labels
            Labels serviceLabels = new Labels();
            serviceLabels.setName(serviceName);
            service.setLabels(serviceLabels);
            service.setName(serviceName);

            // Set service selector
            Selector selector = new Selector();
            selector.setName(serviceLabels.getName());
            service.setSelector(selector);

            // Invoke the api to create the service
            createService(service);

            if (log.isInfoEnabled()) {
                log.info(String.format("Service created successfully: [service-id] %s [service-name] %s [service-port] %d " +
                        "[container-port] %d, [public-ip] %s", serviceId, serviceName, servicePort, containerPort, publicIp));
            }
        } catch (Exception e) {
            String message = String.format("Service created successfully: [service-id] %s [service-name] %s [service-port] %d " +
                    "[container-port] %d, [public-ip] %s", serviceId, serviceName, servicePort, containerPort, publicIp);
            log.error(message, e);
            throw new KubernetesClientException(message, e);
        }
    }

    @Override
    public Service getService(String serviceId)
            throws KubernetesClientException {
        try {
            URI uri = new URIBuilder(baseURL + "services/" + serviceId).build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse(String.format("Could not retrieve service: [service-id] %s", serviceId), response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            return gson.fromJson(content, Service.class);
        } catch (KubernetesClientException e) {
            throw e;
        } catch (Exception e) {
            String msg = String.format("Could not retrieve service: [service-id] %s", serviceId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public List<Service> getServices() throws KubernetesClientException {
        try {

            URI uri = new URIBuilder(baseURL + "services").build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse("Service retrieval failed.", response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();

            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            ServiceList result = gson.fromJson(content, ServiceList.class);

            List<Service> serviceList = new ArrayList<Service>();
            if((result != null) && (result.getItems() != null)) {
                for(Service pod : result.getItems()) {
                    if(pod != null) {
                        serviceList.add(pod);
                    }
                }
            }
            return serviceList;
        } catch (Exception e) {
            String msg = "Error while retrieving Services.";
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    private void createService(Service service) throws KubernetesClientException {

        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            String content = gson.toJson(service);
            if (log.isDebugEnabled()) {
                log.debug("CreateService Request Body : " + content);
            }

            URI uri = new URIBuilder(baseURL + "services").build();
            HttpResponse response = restClient.doPost(uri, content);

            handleNullResponse(String.format("Could not create service: [service-id] %s", service.getId()), response);

            if (response.getStatusCode() == HttpStatus.SC_CONFLICT) {
                log.warn("Service already created: [service-id] " + service.getId());
                return;
            }

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED &&
                    response.getStatusCode() != HttpStatus.SC_OK) {
                String msg = String.format("Could not create service: [service-id] %s [message] %s", service.getId(),
                        response.getKubernetesResponse().getMessage());
                log.error(msg);
                throw new KubernetesClientException(msg);
            }
        } catch (KubernetesClientException e) {
            throw e;

        } catch (Exception e) {
            String msg = "Could not create service: [service-id] " + service.getId();
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public void deleteService(String serviceId)
            throws KubernetesClientException {

        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Deleting service: [service-id] %s", serviceId));
            }

            URI uri = new URIBuilder(baseURL + "services/" + serviceId).build();
            HttpResponse response = restClient.doDelete(uri);

            handleNullResponse(String.format("Could not delete service: [service-id] %s", serviceId), response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                String msg = String.format("Service not found: [service-id] %s", serviceId);
                log.warn(msg);
                return;
            }

            if (response.getStatusCode() != HttpStatus.SC_ACCEPTED &&
                    response.getStatusCode() != HttpStatus.SC_OK) {
                String msg = String.format("Could not delete service: [service-id] %s [message] %s", serviceId,
                        response.getKubernetesResponse().getMessage());
                log.error(msg);
                throw new KubernetesClientException(msg);
            }

            if(log.isInfoEnabled()) {
                log.info(String.format("Service deleted successfully: [service-id] %s", serviceId));
            }
        } catch (KubernetesClientException e) {
            throw e;

        } catch (Exception e) {
            String msg = String.format("Could not delete service: [service-id] %s", serviceId);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    @Override
    public List<Pod> queryPods(Labels[] labels) throws KubernetesClientException {

        try {
            String labelQuery = getLabelQuery(labels);
            URI uri = new URIBuilder(baseURL + "pods").addParameter("labels", labelQuery).build();
            HttpResponse response = restClient.doGet(uri);

            handleNullResponse(String.format("Could not retrieve pods: [labels] %s", labels), response);

            if (response.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }

            String content = response.getContent();
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson gson = gsonBuilder.create();
            PodList result = gson.fromJson(content, PodList.class);

            List<Pod> podList = new ArrayList<Pod>();
            if((result != null) && (result.getItems() != null)) {
                for(Pod pod : result.getItems()) {
                    if(pod != null) {
                        podList.add(pod);
                    }
                }
            }
            return podList;
        } catch (Exception e) {
            String msg = String.format("Could not retrieve pods: [labels] %s", labels);
            log.error(msg, e);
            throw new KubernetesClientException(msg, e);
        }
    }

    private String getLabelQuery(Labels[] labels) {
        String query = "";
        for (Labels l : labels) {
            query = query.concat("name=" + l.getName() + ",");
        }
        return query.endsWith(",") ? query.substring(0, query.length() - 1) : query;
    }

    private void handleNullResponse(String message, HttpResponse response)
            throws KubernetesClientException {
        if (response == null) {
            log.error(message + ", a null response received");
            throw new KubernetesClientException(message);
        }
    }
}
