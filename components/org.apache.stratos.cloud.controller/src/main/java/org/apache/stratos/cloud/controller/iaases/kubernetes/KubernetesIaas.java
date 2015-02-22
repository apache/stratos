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

package org.apache.stratos.cloud.controller.iaases.kubernetes;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesHost;
import org.apache.stratos.cloud.controller.domain.kubernetes.PortRange;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.domain.NameValuePair;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.KubernetesConstants;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.EnvironmentVariable;
import org.apache.stratos.kubernetes.client.model.Labels;
import org.apache.stratos.kubernetes.client.model.Pod;
import org.apache.stratos.kubernetes.client.model.Service;
import org.apache.stratos.messaging.domain.topology.KubernetesService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Kubernetes IaaS implementation.
 */
public class KubernetesIaas extends Iaas {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);

    private static final long DEFAULT_POD_ACTIVATION_TIMEOUT = 300000; // 5 min
    private static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    private static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";
    private static final String PAYLOAD_PARAMETER_PREFIX = "payload_parameter.";

    private PartitionValidator partitionValidator;
    private List<NameValuePair> payload;
    private Long podActivationTimeout;

    public KubernetesIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
        partitionValidator = new KubernetesPartitionValidator();
        payload = new ArrayList<NameValuePair>();

        podActivationTimeout = Long.getLong("stratos.pod.activation.timeout");
        if(podActivationTimeout == null) {
            podActivationTimeout = DEFAULT_POD_ACTIVATION_TIMEOUT;
            if(log.isInfoEnabled()) {
                log.info("Pod activation timeout was set: " + podActivationTimeout);
            }
        }
    }

    @Override
    public void initialize() {
    }

    /**
     * Set dynamic payload which needs to be passed to the containers as environment variables.
     *
     * @param payloadByteArray
     */
    @Override
    public void setDynamicPayload(byte[] payloadByteArray) {
        // Clear existing payload parameters
        payload.clear();

        if (payloadByteArray != null) {
            String payloadString = new String(payloadByteArray);
            String[] parameterArray = payloadString.split(PAYLOAD_PARAMETER_SEPARATOR);
            if (parameterArray != null) {
                for (String parameter : parameterArray) {
                    if (parameter != null) {
                        String[] nameValueArray = parameter.split(PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR);
                        if ((nameValueArray != null) && (nameValueArray.length == 2)) {
                            NameValuePair nameValuePair = new NameValuePair(nameValueArray[0], nameValueArray[1]);
                            payload.add(nameValuePair);
                        }
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug("Dynamic payload is set: " + payload.toString());
                }
            }
        }
    }

    @Override
    public MemberContext startInstance(MemberContext memberContext) throws CartridgeNotFoundException {
        return startContainer(memberContext);
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return partitionValidator;
    }

    @Override
    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException,
            InvalidMemberException, MemberTerminationFailedException {
        terminateContainer(memberContext.getMemberId());
    }

    /**
     * Starts a container via kubernetes for the given member context.
     *
     * @param memberContext
     * @return
     * @throws CartridgeNotFoundException
     */
    public MemberContext startContainer(MemberContext memberContext)
            throws CartridgeNotFoundException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();

            handleNullObject(memberContext, "member context is null");
            if (log.isInfoEnabled()) {
                log.info(String.format("Starting container: [cartridge-type] %s", memberContext.getCartridgeType()));
            }

            // Validate cluster id
            String clusterId = memberContext.getClusterId();
            String memberId = memberContext.getMemberId();
            handleNullObject(clusterId, "cluster id is null in member context");

            // Validate cluster context
            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, "cluster context not found: [cluster-id] "
                    + clusterId + " [member-id] " + memberId);

            // Validate partition
            Partition partition = memberContext.getPartition();
            handleNullObject(partition, "partition not found in member context: " +
                    "[cluster-id] " + clusterId + " [member-id] " + memberId);

            // Validate cartridge
            String cartridgeType = clusterContext.getCartridgeType();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = "cartridge not found: [cartridge-type] " + cartridgeType + " " +
                        "[cluster-id] " + clusterId + " [member-id] " + memberId;
                log.error(msg);
                throw new CartridgeNotFoundException(msg);
            }

            String kubernetesClusterId = partition.getKubernetesClusterId();
            clusterContext.setKubernetesClusterId(kubernetesClusterId);
            KubernetesCluster kubernetesCluster = CloudControllerContext.getInstance().
                    getKubernetesCluster(kubernetesClusterId);
            handleNullObject(kubernetesCluster, "kubernetes cluster not found: " +
                    "[kubernetes-cluster-id] " + kubernetesClusterId + " [cluster-id] " + clusterId +
                    " [member-id] " + memberId);

            // Prepare kubernetes context
            String kubernetesMasterIp = kubernetesCluster.getKubernetesMaster().getPrivateIPAddress();
            PortRange kubernetesPortRange = kubernetesCluster.getPortRange();
            String kubernetesMasterPort = CloudControllerUtil.getProperty(
                    kubernetesCluster.getKubernetesMaster().getProperties(), StratosConstants.KUBERNETES_MASTER_PORT,
                    StratosConstants.KUBERNETES_MASTER_DEFAULT_PORT);

            // Add kubernetes cluster payload parameters to payload
            if ((kubernetesCluster.getProperties() != null) &&
                    (kubernetesCluster.getProperties().getProperties() != null)) {
                for (Property property : kubernetesCluster.getProperties().getProperties()) {
                    if (property != null) {
                        if (property.getName().startsWith(PAYLOAD_PARAMETER_PREFIX)) {
                            String name = property.getName().replace(PAYLOAD_PARAMETER_PREFIX, "");
                            payload.add(new NameValuePair(name, property.getValue()));
                        }
                    }
                }
            }

            KubernetesClusterContext kubClusterContext = getKubernetesClusterContext(kubernetesClusterId,
                    kubernetesMasterIp, kubernetesMasterPort, kubernetesPortRange.getUpper(),
                    kubernetesPortRange.getLower());

            // Generate proxy service ports and update port mappings in cartridge
            updateKubernetesServicePorts(kubClusterContext, clusterContext.getClusterId(), cartridge);

            // Get kubernetes API
            KubernetesApiClient kubernetesApi = kubClusterContext.getKubApi();

            // Create replication controller
            createReplicationController(clusterContext, memberContext, kubernetesApi);

            // Create proxy services for port mappings
            List<KubernetesService> kubernetesServices = createKubernetesServices(kubernetesApi, clusterContext,
                    kubClusterContext);
            clusterContext.setKubernetesServices(kubernetesServices);
            CloudControllerContext.getInstance().updateClusterContext(clusterContext);

            // Wait for pod status to be changed to running
            Pod pod = waitForPodToBeActivated(memberContext, kubernetesApi);
            if (log.isDebugEnabled()) {
                log.debug(String.format("Pod created: [cluster-id] %s [member-id] %s [pod-id] %s",
                        clusterId, memberId, pod.getId()));
            }

            // Create member context
            MemberContext newMemberContext = createNewMemberContext(memberContext, pod, kubernetesCluster);
            CloudControllerContext.getInstance().addMemberContext(newMemberContext);

            // Persist in registry
            CloudControllerContext.getInstance().persist();
            log.info(String.format("Container started successfully: [cluster-id] %s [member-id] %s",
                    newMemberContext.getClusterId(), newMemberContext.getMemberId()));

            return newMemberContext;
        } catch (Exception e) {
            String msg = String.format("Could not start container: [cartridge-type] %s [member-id] %s",
                    memberContext.getCartridgeType(), memberContext.getMemberId());
            log.error(msg, e);
            throw new IllegalStateException(msg, e);
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private MemberContext createNewMemberContext(MemberContext memberContext, Pod pod, KubernetesCluster kubernetesCluster) {
        MemberContext newMemberContext = new MemberContext(memberContext.getApplicationId(),
                memberContext.getCartridgeType(), memberContext.getClusterId(), memberContext.getMemberId());

        String memberPrivateIPAddress = pod.getCurrentState().getPodIP();
        String podHostIPAddress = pod.getCurrentState().getHost();
        String memberPublicIPAddress = podHostIPAddress;
        String kubernetesHostPublicIP = findKubernetesHostPublicIPAddress(kubernetesCluster, podHostIPAddress);
        if(StringUtils.isNotBlank(kubernetesHostPublicIP)) {
            memberPublicIPAddress = kubernetesHostPublicIP;
            if(log.isInfoEnabled()) {
                 log.info(String.format("Member public IP address set to Kubernetes host public IP address:" +
                         "[pod-host-ip] %s [kubernetes-host-public-ip] %s", podHostIPAddress, kubernetesHostPublicIP));
            }
        }

        newMemberContext.setClusterInstanceId(memberContext.getClusterInstanceId());
        newMemberContext.setNetworkPartitionId(memberContext.getNetworkPartitionId());
        newMemberContext.setPartition(memberContext.getPartition());
        newMemberContext.setInstanceId(pod.getId());
        newMemberContext.setDefaultPrivateIP(memberPrivateIPAddress);
        newMemberContext.setPrivateIPs(new String[]{memberPrivateIPAddress});
        newMemberContext.setDefaultPublicIP(memberPublicIPAddress);
        newMemberContext.setPublicIPs(new String[]{memberPublicIPAddress});
        newMemberContext.setInitTime(memberContext.getInitTime());
        newMemberContext.setProperties(memberContext.getProperties());

        return newMemberContext;
    }

    private String findKubernetesHostPublicIPAddress(KubernetesCluster kubernetesCluster, String podHostIP) {
        if((kubernetesCluster != null) && (StringUtils.isNotBlank(podHostIP))) {
            for (KubernetesHost kubernetesHost : kubernetesCluster.getKubernetesHosts()) {
                if (kubernetesHost != null) {
                    if (podHostIP.equals(kubernetesHost.getPrivateIPAddress())) {
                        return kubernetesHost.getPublicIPAddress();
                    }
                }
            }
        }
        return null;
    }

    private Pod waitForPodToBeActivated(MemberContext memberContext, KubernetesApiClient kubernetesApi)
            throws KubernetesClientException, InterruptedException {

        Labels labels = new Labels();
        String podId = CloudControllerUtil.removeSpecialCharacters(memberContext.getMemberId());
        labels.setName(podId);

        Pod pod;
        List<Pod> pods;
        boolean podCreated = false;
        boolean podRunning = false;
        long startTime = System.currentTimeMillis();

        while (!podRunning) {
            pods = kubernetesApi.queryPods(new Labels[]{labels});
            if ((pods != null) && (pods.size() > 0)) {
                if (pods.size() > 1) {
                    throw new RuntimeException("System error, more than one pod found with the same pod id: " + podId);
                }

                pod = pods.get(0);
                podCreated = true;
                if (pod.getCurrentState().getStatus().equals(KubernetesConstants.POD_STATUS_RUNNING)) {
                    log.info(String.format("Pod status changed to running: [member-id] %s [pod-id] %s",
                            memberContext.getMemberId(), pod.getId()));
                    return pod;
                } else {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Waiting pod status to be changed to running: [member-id] %s " +
                                        "[pod-id] %s [current-pod-status] %s ", memberContext.getMemberId(),
                                pod.getId(), pod.getCurrentState().getStatus().toLowerCase()));
                    }
                }
            }

            if ((System.currentTimeMillis() - startTime) > podActivationTimeout) {
                break;
            }
            Thread.sleep(5000);
        }

        String replicationControllerId = CloudControllerUtil.removeSpecialCharacters(memberContext.getMemberId());
        String message;
        if (podCreated) {
            // Pod created but status did not change to running
            message = String.format("Pod status did not change to running within %d sec: " +
                            "[cluster-id] %s [member-id] %s [replication-controller-id] %s [pod-id] %s",
                    (podActivationTimeout.intValue() / 1000), memberContext.getClusterId(), memberContext.getMemberId(),
                    replicationControllerId, podId);
            log.error(message);
        } else {
            // Pod did not create
            message = String.format("Pod did not create within %d sec: " +
                            "[cluster-id] %s [member-id] %s [replication-controller-id] %s",
                    (podActivationTimeout.intValue() / 1000), memberContext.getClusterId(), memberContext.getMemberId(),
                    replicationControllerId);
            log.error(message);
        }

        throw new RuntimeException(message);
    }

    /**
     * Create new replication controller for the cluster and generate environment variables using member context.
     *
     * @param memberContext
     * @param kubernetesApi
     * @throws KubernetesClientException
     */
    private void createReplicationController(ClusterContext clusterContext, MemberContext memberContext,
                                             KubernetesApiClient kubernetesApi)
            throws KubernetesClientException {
        if (log.isInfoEnabled()) {
            log.info(String.format("Creating replication controller: [cartridge-type] %s [member-id] %s",
                    memberContext.getCartridgeType(), memberContext.getClusterId()));
        }

        Partition partition = memberContext.getPartition();
        if (partition == null) {
            String message = "Partition not found in member context: [member-id] " + memberContext.getMemberId();
            log.error(message);
            throw new RuntimeException(message);
        }

        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(memberContext.getCartridgeType());
        if (cartridge == null) {
            String message = "Could not find cartridge: [cartridge-type] " + memberContext.getCartridgeType();
            log.error(message);
            throw new RuntimeException(message);
        }

        IaasProvider iaasProvider = cartridge.getIaasProviderOfPartition(partition.getId());
        if (iaasProvider == null) {
            String message = "Could not find iaas provider: [partition-id] " + partition.getId();
            log.error(message);
            throw new RuntimeException(message);
        }

        // Add dynamic payload to the member context
        memberContext.setDynamicPayload(payload);

        // Create replication controller
        String replicationControllerId = CloudControllerUtil.removeSpecialCharacters(memberContext.getMemberId());
        String replicationControllerName = replicationControllerId;
        String dockerImage = iaasProvider.getImage();
        EnvironmentVariable[] environmentVariables = KubernetesIaasUtil.prepareEnvironmentVariables(
                clusterContext, memberContext);
        int replicas = 1;

        kubernetesApi.createReplicationController(replicationControllerId, replicationControllerName,
                dockerImage, KubernetesIaasUtil.convertPortMappings(cartridge.getPortMappings()), environmentVariables, replicas);
        if (log.isInfoEnabled()) {
            log.info(String.format("Replication controller created successfully: [cartridge-type] %s [member-id] %s",
                    memberContext.getCartridgeType(), memberContext.getClusterId()));
        }
    }

    /**
     * Creates and returns proxy services for the cluster.
     *
     * @param kubernetesApi
     * @param clusterContext
     * @param kubernetesClusterContext
     * @return
     * @throws KubernetesClientException
     */
    private List<KubernetesService> createKubernetesServices(KubernetesApiClient kubernetesApi, ClusterContext clusterContext,
                                                             KubernetesClusterContext kubernetesClusterContext) throws KubernetesClientException {
        List<KubernetesService> kubernetesServices = new ArrayList<KubernetesService>();

        String clusterId = clusterContext.getClusterId();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(clusterContext.getCartridgeType());
        if (cartridge == null) {
            String message = "Could not create kubernetes services, cartridge not found: [cartridge-type] " +
                    clusterContext.getCartridgeType();
            log.error(message);
            throw new RuntimeException(message);
        }

        for (PortMapping portMapping : cartridge.getPortMappings()) {
            String serviceName = CloudControllerUtil.removeSpecialCharacters(clusterId);
            String serviceId = serviceName;

            if (log.isInfoEnabled()) {
                log.info(String.format("Creating kubernetes service: [cluster-id] %s [service-id] %s " +
                                "[protocol] %s [service-port] %d [container-port] %s", clusterId,
                        serviceId, portMapping.getProtocol(), portMapping.getKubernetesServicePort(),
                        portMapping.getPort()));
            }

            int servicePort = portMapping.getKubernetesServicePort();
            String containerPortName = KubernetesIaasUtil.generatePortName(portMapping);
            String publicIp = kubernetesClusterContext.getMasterIp();

            kubernetesApi.createService(serviceId, serviceName, servicePort, containerPortName, publicIp);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }

            Service service = kubernetesApi.getService(serviceId);

            KubernetesService kubernetesService = new KubernetesService();
            kubernetesService.setId(service.getId());
            kubernetesService.setPortalIP(service.getPortalIP());
            kubernetesService.setPublicIPs(service.getPublicIPs());
            kubernetesService.setProtocol(portMapping.getProtocol());
            kubernetesService.setPort(service.getPort());
            kubernetesService.setContainerPort(portMapping.getPort());
            kubernetesServices.add(kubernetesService);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes service successfully created: [cluster-id] %s [service-id] %s " +
                                "[protocol] %s [service-port] %d [container-port] %s", clusterId,
                        serviceId, portMapping.getProtocol(), servicePort, portMapping.getPort()));
            }
        }
        return kubernetesServices;
    }

    private void updateKubernetesServicePorts(KubernetesClusterContext kubernetesClusterContext, String clusterId,
                                              Cartridge cartridge) {
        if(cartridge != null) {
            boolean servicePortsUpdated = false;
            for (PortMapping portMapping : cartridge.getPortMappings()) {
                if(portMapping.getKubernetesServicePort() == 0) {
                    int nextServicePort = kubernetesClusterContext.getNextServicePort();
                    if (nextServicePort == -1) {
                        throw new RuntimeException(String.format("Could not generate service port: [cluster-id] %s [port] %d",
                                clusterId, portMapping.getPort()));
                    }
                    portMapping.setKubernetesServicePort(nextServicePort);
                    servicePortsUpdated = true;
	                portMapping.setKubernetesServicePortMapping(true);
                    if (log.isInfoEnabled()) {
                        log.info(String.format("Kubernetes service port generated: [cluster-id] %s [port] %d " +
                                "[service-port] %d", clusterId, portMapping.getPort(), nextServicePort));
                    }
                }
            }
            if(servicePortsUpdated) {
                // Persist service ports added to port mappings
                CloudControllerContext.getInstance().persist();
            }
        }
    }

    /**
     * Terminate all the containers belong to a cluster by cluster id.
     *
     * @param clusterId
     * @return
     * @throws InvalidClusterException
     */
    public MemberContext[] terminateContainers(String clusterId)
            throws InvalidClusterException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();

            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, "Could not terminate containers, cluster not found: [cluster-id] "
                    + clusterId);

            String kubernetesClusterId = clusterContext.getKubernetesClusterId();
            handleNullObject(kubernetesClusterId, "Could not terminate containers, kubernetes cluster id not found: " +
                    "[cluster-id] " + clusterId);

            KubernetesClusterContext kubClusterContext = CloudControllerContext.getInstance().
                    getKubernetesClusterContext(kubernetesClusterId);
            handleNullObject(kubClusterContext, "Could not terminate containers, kubernetes cluster not found: " +
                    "[kubernetes-cluster-id] " + kubernetesClusterId);

            KubernetesApiClient kubApi = kubClusterContext.getKubApi();

            // Remove kubernetes services
            List<KubernetesService> kubernetesServices = clusterContext.getKubernetesServices();
            if (kubernetesServices != null) {
                for (KubernetesService kubernetesService : kubernetesServices) {
                    try {
                        kubApi.deleteService(kubernetesService.getId());
                        int allocatedPort = kubernetesService.getPort();
                        kubClusterContext.deallocatePort(allocatedPort);
                    } catch (KubernetesClientException e) {
                        log.error("Could not remove kubernetes service: [cluster-id] " + clusterId, e);
                    }
                }
            }

            List<MemberContext> memberContextsRemoved = new ArrayList<MemberContext>();
            List<MemberContext> memberContexts = CloudControllerContext.getInstance().getMemberContextsOfClusterId(clusterId);
            if (memberContexts != null) {
                for (MemberContext memberContext : memberContexts) {
                    try {
                        MemberContext memberContextRemoved = terminateContainer(memberContext.getMemberId());
                        memberContextsRemoved.add(memberContextRemoved);
                    } catch (MemberTerminationFailedException e) {
                        String message = "Could not terminate container: [member-id] " + memberContext.getMemberId();
                        log.error(message);
                    }
                }
            }

            // persist
            CloudControllerContext.getInstance().persist();
            return memberContextsRemoved.toArray(new MemberContext[memberContextsRemoved.size()]);
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    /**
     * Terminate a container by member id
     *
     * @param memberId
     * @return
     * @throws MemberTerminationFailedException
     */
    public MemberContext terminateContainer(String memberId) throws MemberTerminationFailedException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();
            handleNullObject(memberId, "Could not terminate container, member id is null");

            MemberContext memberContext = CloudControllerContext.getInstance().getMemberContextOfMemberId(memberId);
            handleNullObject(memberContext, "Could not terminate container, member context not found: [member-id] " + memberId);

            String clusterId = memberContext.getClusterId();
            handleNullObject(clusterId, "Could not terminate container, cluster id is null: [member-id] " + memberId);

            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, String.format("Could not terminate container, cluster context not found: " +
                    "[cluster-id] %s [member-id] %s", clusterId, memberId));

            String kubernetesClusterId = clusterContext.getKubernetesClusterId();
            handleNullObject(kubernetesClusterId, String.format("Could not terminate container, kubernetes cluster " +
                    "context id is null: [cluster-id] %s [member-id] %s", clusterId, memberId));

            KubernetesClusterContext kubernetesClusterContext = CloudControllerContext.getInstance().getKubernetesClusterContext(kubernetesClusterId);
            handleNullObject(kubernetesClusterContext, String.format("Could not terminate container, kubernetes cluster " +
                    "context not found: [cluster-id] %s [member-id] %s", clusterId, memberId));
            KubernetesApiClient kubApi = kubernetesClusterContext.getKubApi();

            // Remove the pod forcefully
            try {
                Labels l = new Labels();
                l.setName(memberId);
                // execute the label query
                List<Pod> pods = kubApi.queryPods(new Labels[]{l});
                for (Pod pod : pods) {
                    try {
                        // delete pods forcefully
                        kubApi.deletePod(pod.getId());
                    } catch (KubernetesClientException ignore) {
                        // we can't do nothing here
                        log.warn(String.format("Could not delete pod: [pod-id] %s", pod.getId()));
                    }
                }
            } catch (KubernetesClientException e) {
                // we're not going to throw this error, but proceed with other deletions
                log.error("Could not delete pods of cluster: [cluster-id] " + clusterId, e);
            }

            // Remove the replication controller
            try {
                kubApi.deleteReplicationController(memberContext.getMemberId());
                MemberContext memberToBeRemoved = CloudControllerContext.getInstance().getMemberContextOfMemberId(memberId);
                CloudControllerServiceUtil.executeMemberTerminationPostProcess(memberToBeRemoved);
                return memberToBeRemoved;
            } catch (KubernetesClientException e) {
                String msg = String.format("Failed to terminate member: [cluster-id] %s [member-id] %s", clusterId, memberId);
                log.error(msg, e);
                throw new MemberTerminationFailedException(msg, e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    /**
     * Get kubernetes cluster context
     *
     * @param kubernetesClusterId
     * @param kubernetesMasterIp
     * @param kubernetesMasterPort
     * @param upperPort
     * @param lowerPort
     * @return
     */
    private KubernetesClusterContext getKubernetesClusterContext(String kubernetesClusterId, String kubernetesMasterIp,
                                                                 String kubernetesMasterPort, int upperPort, int lowerPort) {

        KubernetesClusterContext kubernetesClusterContext = CloudControllerContext.getInstance().
                getKubernetesClusterContext(kubernetesClusterId);
        if (kubernetesClusterContext != null) {
            return kubernetesClusterContext;
        }

        kubernetesClusterContext = new KubernetesClusterContext(kubernetesClusterId, kubernetesMasterIp,
                kubernetesMasterPort, lowerPort, upperPort);
        CloudControllerContext.getInstance().addKubernetesClusterContext(kubernetesClusterContext);
        return kubernetesClusterContext;
    }

    private String readProperty(String property, org.apache.stratos.common.Properties properties, String object) {
        String propVal = CloudControllerUtil.getProperty(properties, property);
        handleNullObject(propVal, "Property validation failed. Could not find property: '" + property + " in " + object);
        return propVal;

    }

    private void handleNullObject(Object obj, String errorMsg) {
        if (obj == null) {
            log.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }

    @Override
    public void releaseAddress(String ip) {

    }

    @Override
    public boolean isValidRegion(String region) throws InvalidRegionException {
        // No regions in kubernetes cluster
        return true;
    }

    @Override
    public boolean isValidZone(String region, String zone) throws InvalidZoneException, InvalidRegionException {
        // No zones in kubernetes cluster
        return true;
    }

    @Override
    public boolean isValidHost(String zone, String host) throws InvalidHostException {
        // No zones in kubernetes cluster
        return true;
    }

    @Override
    public String createVolume(int sizeGB, String snapshotId) {
        throw new NotImplementedException();
    }

    @Override
    public String attachVolume(String instanceId, String volumeId, String deviceName) {
        throw new NotImplementedException();
    }

    @Override
    public void detachVolume(String instanceId, String volumeId) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteVolume(String volumeId) {
        throw new NotImplementedException();
    }

    @Override
    public String getIaasDevice(String device) {
        throw new NotImplementedException();
    }

    @Override
    public void allocateIpAddresses(String clusterId, MemberContext memberContext, Partition partition) {
    }
}
