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

import com.google.common.collect.Lists;
import io.fabric8.kubernetes.api.model.*;
import org.apache.commons.codec.digest.DigestUtils;
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
import org.apache.stratos.cloud.controller.util.CloudControllerConstants;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.domain.NameValuePair;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.KubernetesConstants;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.messaging.domain.topology.KubernetesService;

import java.util.*;
import java.util.concurrent.locks.Lock;

/**
 * Kubernetes IaaS implementation.
 */
public class KubernetesIaas extends Iaas {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);

    private static final long DEFAULT_POD_ACTIVATION_TIMEOUT = 60000; // 1 min
    private static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    private static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";
    private static final String PAYLOAD_PARAMETER_PREFIX = "payload_parameter.";
    private static final String PORT_MAPPINGS = "PORT_MAPPINGS";
    private static final String KUBERNETES_CONTAINER_CPU = "KUBERNETES_CONTAINER_CPU";
    private static final String KUBERNETES_CONTAINER_MEMORY = "KUBERNETES_CONTAINER_MEMORY";
    private static final String KUBERNETES_SERVICE_SESSION_AFFINITY = "KUBERNETES_SERVICE_SESSION_AFFINITY";
    private static final String KUBERNETES_CONTAINER_CPU_DEFAULT = "kubernetes.container.cpu.default";
    private static final String KUBERNETES_CONTAINER_MEMORY_DEFAULT = "kubernetes.container.memory.default";
    public static final String POD_ID_PREFIX = "pod";
    public static final String SERVICE_NAME_PREFIX = "service";

    private PartitionValidator partitionValidator;
    private List<NameValuePair> payload;
    private Long podActivationTimeout;

    public KubernetesIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
        partitionValidator = new KubernetesPartitionValidator();
        payload = new ArrayList<NameValuePair>();

        podActivationTimeout = Long.getLong("stratos.pod.activation.timeout");
        if (podActivationTimeout == null) {
            podActivationTimeout = DEFAULT_POD_ACTIVATION_TIMEOUT;
            if (log.isInfoEnabled()) {
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
                        String[] nameValueArray = parameter.split(PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR, 2);
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
    public MemberContext startInstance(MemberContext memberContext, byte[] payload) throws CartridgeNotFoundException {
        setDynamicPayload(payload);
        return startContainer(memberContext);
    }

    @Override
    public PartitionValidator getPartitionValidator() {
        return partitionValidator;
    }

    @Override
    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException,
            InvalidMemberException, MemberTerminationFailedException {
        terminateContainer(memberContext);
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
            log.info(String.format("Starting container: [application] %s [cartridge] %s [member] %s",
                    memberContext.getApplicationId(), memberContext.getCartridgeType(),
                    memberContext.getMemberId()));

            // Validate cluster id
            String clusterId = memberContext.getClusterId();
            String memberId = memberContext.getMemberId();
            handleNullObject(clusterId, "cluster id is null in member context");

            // Validate cluster context
            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext,
                    String.format("Cluster context not found: [application] %s [cartridge] %s " +
                                    "[cluster] %s", memberContext.getApplicationId(), memberContext.getCartridgeType(),
                            clusterId));

            // Validate partition
            Partition partition = memberContext.getPartition();
            handleNullObject(partition, String.format("partition not found in member context: [application] %s " +
                            "[cartridge] %s [member] %s", memberContext.getApplicationId(),
                    memberContext.getCartridgeType(),
                    memberContext.getMemberId()));

            // Validate cartridge
            String cartridgeType = clusterContext.getCartridgeType();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = String.format("Cartridge not found: [application] %s [cartridge] %s",
                        memberContext.getApplicationId(), memberContext.getCartridgeType());
                log.error(msg);
                throw new CartridgeNotFoundException(msg);
            }

            String kubernetesClusterId = partition.getKubernetesClusterId();

            KubernetesCluster kubernetesCluster = CloudControllerContext.getInstance().
                    getKubernetesCluster(kubernetesClusterId);
            handleNullObject(kubernetesCluster, "kubernetes cluster not found: " +
                    "[kubernetes-cluster] " + kubernetesClusterId + " [cluster] " + clusterId +
                    " [member] " + memberId);

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

            KubernetesClusterContext kubernetesClusterContext = getKubernetesClusterContext(kubernetesClusterId,
                    kubernetesMasterIp, kubernetesMasterPort, kubernetesPortRange.getUpper(),
                    kubernetesPortRange.getLower());

            // Generate kubernetes service ports and update port mappings in cartridge
            generateKubernetesServicePorts(clusterContext.getApplicationId(), clusterContext.getClusterId(),
                    kubernetesClusterContext, cartridge);

            // Create kubernetes services for port mappings
            KubernetesApiClient kubernetesApi = kubernetesClusterContext.getKubApi();
            createKubernetesServices(kubernetesApi, clusterContext, kubernetesCluster, kubernetesClusterContext,
                    memberContext);

            // Create pod
            createPod(clusterContext, memberContext, kubernetesApi, kubernetesClusterContext);

            // Wait for pod status to be changed to running
            Pod pod = waitForPodToBeActivated(memberContext, kubernetesApi);

            // Update member context
            updateMemberContext(memberContext, pod, kubernetesCluster);

            log.info(String.format("Container started successfully: [application] %s [cartridge] %s [member] %s " +
                            "[pod] %s [cpu] %s [memory] %s",
                    memberContext.getApplicationId(), memberContext.getCartridgeType(),
                    memberContext.getMemberId(), memberContext.getKubernetesPodId(),
                    memberContext.getInstanceMetadata().getCpu(), memberContext.getInstanceMetadata().getRam()));
            return memberContext;
        }
        catch (Exception e) {
            String msg = String.format("Could not start container: [application] %s [cartridge] %s [member] %s",
                    memberContext.getApplicationId(), memberContext.getCartridgeType(),
                    memberContext.getMemberId());
            log.error(msg, e);
            throw new RuntimeException(msg, e);
        }
        finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private void updateMemberContext(MemberContext memberContext, Pod pod, KubernetesCluster kubernetesCluster) {

        String memberPrivateIPAddress = pod.getStatus().getPodIP();
        String podHostIPAddress = pod.getStatus().getHostIP();
        String memberPublicIPAddress = podHostIPAddress;
        String kubernetesHostPublicIP = findKubernetesHostPublicIPAddress(kubernetesCluster, podHostIPAddress);

        if (StringUtils.isNotBlank(kubernetesHostPublicIP)) {
            memberPublicIPAddress = kubernetesHostPublicIP;
            if (log.isInfoEnabled()) {
                log.info(String.format("Member public IP address set to kubernetes host public IP address:" +
                        "[pod-host-ip] %s [kubernetes-host-public-ip] %s", podHostIPAddress, kubernetesHostPublicIP));
            }
        }

        memberContext.setInstanceId(pod.getMetadata().getName());
        memberContext.setDefaultPrivateIP(memberPrivateIPAddress);
        memberContext.setPrivateIPs(new String[]{memberPrivateIPAddress});
        memberContext.setDefaultPublicIP(memberPublicIPAddress);
        memberContext.setPublicIPs(new String[]{memberPublicIPAddress});
        memberContext.setInitTime(memberContext.getInitTime());
        memberContext.setProperties(memberContext.getProperties());
    }

    private String findKubernetesHostPublicIPAddress(KubernetesCluster kubernetesCluster, String podHostIP) {
        if ((kubernetesCluster != null) && (StringUtils.isNotBlank(podHostIP))) {
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

        Pod pod;
        boolean podCreated = false;
        boolean podRunning = false;
        long startTime = System.currentTimeMillis();

        while (!podRunning) {
            pod = kubernetesApi.getPod(memberContext.getKubernetesPodId());
            if (pod != null) {
                podCreated = true;
                if (pod.getStatus().getPhase().equals(KubernetesConstants.POD_STATUS_RUNNING)) {
                    log.info(String.format(
                            "Pod status changed to running: [application] %s [cartridge] %s [member] %s " +
                                    "[pod] %s", memberContext.getApplicationId(), memberContext.getCartridgeType(),
                            memberContext.getMemberId(), pod.getMetadata().getName()));
                    return pod;
                } else {
                    log.info(String.format("Waiting pod status to be changed to running: [application] %s " +
                                    "[cartridge] %s [member] %s [pod] %s", memberContext.getApplicationId(),
                            memberContext.getCartridgeType(), memberContext.getMemberId(),
                            pod.getMetadata().getName()));
                }
            } else {
                log.info(String.format("Waiting for pod to be created: [application] %s " +
                                "[cartridge] %s [member] %s [pod] %s", memberContext.getApplicationId(),
                        memberContext.getCartridgeType(), memberContext.getMemberId(),
                        memberContext.getKubernetesPodId()));
            }

            if ((System.currentTimeMillis() - startTime) > podActivationTimeout) {
                break;
            }
            Thread.sleep(5000);
        }

        String message;
        if (podCreated) {
            // Pod created but status did not change to running
            message = String.format("Pod status did not change to running within %d sec: " +
                            "[application] %s [cartridge] %s [member] %s [pod] %s",
                    (podActivationTimeout.intValue() / 1000),
                    memberContext.getApplicationId(), memberContext.getCartridgeType(), memberContext.getMemberId(),
                    memberContext.getKubernetesPodId());
            log.error(message);
        } else {
            // Pod did not create
            message = String.format("Pod did not create within %d sec: " +
                            "[application] %s [cartridge] %s [member] %s [pod] %s",
                    (podActivationTimeout.intValue() / 1000),
                    memberContext.getApplicationId(), memberContext.getCartridgeType(), memberContext.getMemberId(),
                    memberContext.getKubernetesPodId());
            log.error(message);
        }

        throw new RuntimeException(message);
    }

    /**
     * Create new pod and pass environment variables.
     *
     * @param memberContext
     * @param kubernetesApi
     * @param kubernetesClusterContext
     * @throws KubernetesClientException
     */
    private void createPod(ClusterContext clusterContext, MemberContext memberContext,
                           KubernetesApiClient kubernetesApi, KubernetesClusterContext kubernetesClusterContext)
            throws KubernetesClientException {

        String applicationId = memberContext.getApplicationId();
        String cartridgeType = memberContext.getCartridgeType();
        String clusterId = memberContext.getClusterId();
        String memberId = memberContext.getMemberId();

        if (log.isInfoEnabled()) {
            log.info(String.format("Creating kubernetes pod: [application] %s [cartridge] %s [member] %s",
                    applicationId, cartridgeType, memberId));
        }

        Partition partition = memberContext.getPartition();
        if (partition == null) {
            String message = String.format("Partition not found in member context: [application] %s [cartridge] %s " +
                            "[member] %s ", applicationId, cartridgeType,
                    memberId);
            log.error(message);
            throw new RuntimeException(message);
        }

        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
        if (cartridge == null) {
            String message = "Could not find cartridge: [cartridge] " + cartridgeType;
            log.error(message);
            throw new RuntimeException(message);
        }

        // Set default values to zero to avoid cpu and memory restrictions
        String cpu = System.getProperty(KUBERNETES_CONTAINER_CPU_DEFAULT, "0");
        String memory = System.getProperty(KUBERNETES_CONTAINER_MEMORY_DEFAULT, "0");
        Property cpuProperty = cartridge.getProperties().getProperty(KUBERNETES_CONTAINER_CPU);
        if (cpuProperty != null) {
            cpu = cpuProperty.getValue();
        }
        Property memoryProperty = cartridge.getProperties().getProperty(KUBERNETES_CONTAINER_MEMORY);
        if (memoryProperty != null) {
            memory = memoryProperty.getValue();
        }

        IaasProvider iaasProvider =
                CloudControllerContext.getInstance().getIaasProviderOfPartition(cartridge.getType(), partition.getId());
        if (iaasProvider == null) {
            String message = "Could not find iaas provider: [partition] " + partition.getId();
            log.error(message);
            throw new RuntimeException(message);
        }

        // Add dynamic payload to the member context
        memberContext.setDynamicPayload(payload.toArray(new NameValuePair[payload.size()]));

        // Find next available sequence number
        long podSeqNo = kubernetesClusterContext.getNextPodSeqNo();
        String podId = preparePodId(podSeqNo);
        while (kubernetesApi.getPod(podId) != null) {
            podSeqNo = kubernetesClusterContext.getNextPodSeqNo();
            podId = preparePodId(podSeqNo);
        }

        // Create pod
        String podName = DigestUtils.md5Hex(clusterId);
        String dockerImage = iaasProvider.getImage();
        List<EnvVar> environmentVariables = KubernetesIaasUtil.prepareEnvironmentVariables(
                clusterContext, memberContext);

        List<ContainerPort> ports = KubernetesIaasUtil.convertPortMappings(Arrays.asList(cartridge.getPortMappings()));

        log.info(String.format("Starting pod: [application] %s [cartridge] %s [member] %s " +
                        "[cpu] %s [memory] %s",
                memberContext.getApplicationId(), memberContext.getCartridgeType(),
                memberContext.getMemberId(), cpu, memory));

        Map<String, String> podLabels = new HashMap<>();
        podLabels.put(KubernetesConstants.SERVICE_SELECTOR_LABEL, podName);

        podLabels.put(CloudControllerConstants.APPLICATION_ID_LABEL,
                trimLabel(CloudControllerConstants.APPLICATION_ID_LABEL, memberContext.getApplicationId()));

        podLabels.put(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL,
                trimLabel(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL, memberContext.getClusterInstanceId()));

        podLabels.put(CloudControllerConstants.MEMBER_ID_LABEL,
                trimLabel(CloudControllerConstants.MEMBER_ID_LABEL, memberContext.getMemberId()));

        Map<String, String> podAnnotations = new HashMap<>();
        podAnnotations.put(CloudControllerConstants.APPLICATION_ID_LABEL, memberContext.getApplicationId());
        podAnnotations.put(CloudControllerConstants.CARTRIDGE_TYPE_LABEL, memberContext.getCartridgeType());
        podAnnotations.put(CloudControllerConstants.CLUSTER_ID_LABEL, memberContext.getClusterId());
        podAnnotations.put(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL, memberContext.getClusterInstanceId());
        podAnnotations.put(CloudControllerConstants.MEMBER_ID_LABEL, memberContext.getMemberId());

        kubernetesApi.createPod(podId, podName, podLabels, podAnnotations, dockerImage, cpu, memory, ports,
                environmentVariables);

        log.info(String.format("Pod started successfully: [application] %s [cartridge] %s [member] %s " +
                        "[pod] %s [pod-label] %s [cpu] %s [memory] %s",
                memberContext.getApplicationId(), memberContext.getCartridgeType(),
                memberContext.getMemberId(), podId, podName, cpu, memory));

        // Add pod id to member context
        memberContext.setKubernetesPodId(podId);
        memberContext.setKubernetesPodName(podName);

        // Create instance metadata
        InstanceMetadata instanceMetadata = new InstanceMetadata();
        instanceMetadata.setImageId(dockerImage);
        instanceMetadata.setCpu(cpu);
        instanceMetadata.setRam(memory);
        memberContext.setInstanceMetadata(instanceMetadata);

        // Persist cloud controller context
        CloudControllerContext.getInstance().persist();
    }

    private String preparePodId(long podSeqNo) {
        return POD_ID_PREFIX + "-" + podSeqNo;
    }

    /**
     * Creates and returns proxy services for the cluster.
     *
     * @param kubernetesApi
     * @param clusterContext
     * @param kubernetesCluster
     * @param kubernetesClusterContext
     * @throws KubernetesClientException
     */
    private void createKubernetesServices(KubernetesApiClient kubernetesApi, ClusterContext clusterContext,
                                          KubernetesCluster kubernetesCluster, KubernetesClusterContext
                                                  kubernetesClusterContext, MemberContext memberContext)
            throws KubernetesClientException {
        String clusterId = clusterContext.getClusterId();
        String cartridgeType = clusterContext.getCartridgeType();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
        if (cartridge == null) {
            String message = "Could not create kubernetes services, cartridge not found: [cartridge] " +
                    cartridgeType;
            log.error(message);
            throw new RuntimeException(message);
        }

        String sessionAffinity = null;
        Property sessionAffinityProperty = cartridge.getProperties().getProperty(KUBERNETES_SERVICE_SESSION_AFFINITY);
        if (sessionAffinityProperty != null) {
            sessionAffinity = sessionAffinityProperty.getValue();
        }

        // Prepare minion public IP addresses
        List<String> minionPublicIPList = prepareMinionIPAddresses(kubernetesCluster);
        if (log.isDebugEnabled()) {
            log.debug(String.format("Minion public IPs: %s", minionPublicIPList));
        }

        Collection<ClusterPortMapping> clusterPortMappings = CloudControllerContext.getInstance()
                .getClusterPortMappings(clusterContext.getApplicationId(), clusterId);

        if (clusterPortMappings == null) {
            log.info("No cluster port mappings found. Stratos will not attempt to create Kubernetes services");
            return;
        }

        String serviceName = DigestUtils.md5Hex(clusterId);
        Collection<KubernetesService> kubernetesServices =
                clusterContext.getKubernetesServices(memberContext.getClusterInstanceId());

        for (ClusterPortMapping clusterPortMapping : clusterPortMappings) {
            // Skip if already created
            int containerPort = clusterPortMapping.getPort();
            KubernetesService existingService = findKubernetesService(kubernetesServices, containerPort);
            if ((existingService != null) && serviceExistsInCluster(
                    existingService.getId(), kubernetesClusterContext,
                    memberContext, clusterPortMapping.getName())) {

                log.info(String.format("Kubernetes service already exists: [kubernetes-cluster] %s " +
                                "[cluster] %s [service-name] %s [container-port] %d ",
                        kubernetesCluster.getClusterId(), clusterId, serviceName, containerPort));
                continue;
            }

            // Find next available service sequence number
            long serviceSeqNo = kubernetesClusterContext.getNextServiceSeqNo();
            String serviceId =
                    KubernetesIaasUtil.fixSpecialCharacters(prepareServiceName(serviceSeqNo));
            while (kubernetesApi.getService(serviceId) != null) {
                serviceSeqNo = kubernetesClusterContext.getNextServiceSeqNo();
                serviceId = KubernetesIaasUtil.fixSpecialCharacters(prepareServiceName(serviceSeqNo));
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("Creating kubernetes service: [cluster] %s [service-id] %s [service-name] " +
                                "%s " + "[protocol] %s [service-port] %d [container-port] %s", clusterId,
                        serviceId, serviceName, clusterPortMapping.getProtocol(),
                        clusterPortMapping.getKubernetesServicePort(), containerPort));
            }

            // Create kubernetes service for port mapping
            int servicePort = clusterPortMapping.getKubernetesServicePort();
            String serviceType = clusterPortMapping.getKubernetesPortType();
            String containerPortName = KubernetesIaasUtil.preparePortNameFromPortMapping(clusterPortMapping);

            Map<String, String> serviceLabels = new HashMap<>();
            serviceLabels.put(CloudControllerConstants.APPLICATION_ID_LABEL,
                    trimLabel(CloudControllerConstants.APPLICATION_ID_LABEL, clusterContext.getApplicationId()));

            serviceLabels.put(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL,
                    trimLabel(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL,
                            memberContext.getClusterInstanceId()));

            serviceLabels.put(CloudControllerConstants.PORT_NAME_LABEL,
                    trimLabel(CloudControllerConstants.PORT_NAME_LABEL, clusterPortMapping.getName()));

            Map<String, String> serviceAnnotations = new HashMap<>();
            serviceAnnotations
                    .put(CloudControllerConstants.APPLICATION_ID_LABEL, clusterContext.getApplicationId());
            serviceAnnotations.put(CloudControllerConstants.CLUSTER_ID_LABEL, clusterContext.getClusterId());
            serviceAnnotations.put(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL,
                    memberContext.getClusterInstanceId());
            serviceAnnotations.put(CloudControllerConstants.PORT_NAME_LABEL, clusterPortMapping.getName());
            serviceAnnotations.put(CloudControllerConstants.PROTOCOL_LABEL, clusterPortMapping.getProtocol());
            serviceAnnotations.put(CloudControllerConstants.PORT_TYPE_LABEL,
                    clusterPortMapping.getKubernetesPortType());
            serviceAnnotations.put(CloudControllerConstants.SERVICE_PORT_LABEL, String.valueOf(clusterPortMapping
                    .getKubernetesServicePort()));
            serviceAnnotations
                    .put(CloudControllerConstants.PORT_LABEL, String.valueOf(clusterPortMapping.getPort()));
            serviceAnnotations.put(CloudControllerConstants.PROXY_PORT_LABEL,
                    String.valueOf(clusterPortMapping.getProxyPort()));

            kubernetesApi.createService(serviceId, serviceName, serviceLabels, serviceAnnotations, servicePort,
                    serviceType, containerPortName, containerPort, sessionAffinity);
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException ignore) {
            }

            Service service = kubernetesApi.getService(serviceId);
            if (service == null) {
                throw new KubernetesClientException("Kubernetes service was not created: [service] " + serviceId);
            }

            KubernetesService kubernetesService = new KubernetesService();
            kubernetesService.setId(service.getMetadata().getName());
            kubernetesService.setPortalIP(service.getSpec().getClusterIP());
            // Expose minions public IP addresses as they need to be accessed by external networks
            String[] minionPublicIPArray = minionPublicIPList.toArray(new String[minionPublicIPList.size()]);
            kubernetesService.setPublicIPs(minionPublicIPArray);
            kubernetesService.setProtocol(clusterPortMapping.getProtocol());
            kubernetesService.setPortName(clusterPortMapping.getName());

            String kubernetesPortType = service.getSpec().getType();
            kubernetesService.setServiceType(kubernetesPortType);
            kubernetesService.setKubernetesClusterId(memberContext.getPartition().getKubernetesClusterId());

            if (kubernetesPortType.equals(KubernetesConstants.NODE_PORT)) {
                kubernetesService.setPort(service.getSpec().getPorts().get(0).getNodePort());
            } else {
                kubernetesService.setPort(service.getSpec().getPorts().get(0).getPort());
            }

            kubernetesService.setContainerPort(containerPort);

            clusterContext.addKubernetesService(memberContext.getClusterInstanceId(), kubernetesService);
            CloudControllerContext.getInstance().persist();

            if (log.isInfoEnabled()) {
                log.info(String.format(
                        "Kubernetes service successfully created: [cluster] %s [service-id] %s [protocol] %s " +
                                "[node-port] %d [container-port] %s", clusterId, serviceId,
                        clusterPortMapping.getProtocol(), servicePort, containerPort));
            }
        }
    }

    /**
     * Check a given kubernetes service exists in kubernetes cluster
     *
     * @param serviceId
     * @param kubernetesClusterContext
     * @param memberContext
     * @param portName
     * @return
     * @throws KubernetesClientException
     */
    private boolean serviceExistsInCluster(String serviceId, KubernetesClusterContext kubernetesClusterContext,
                                           MemberContext memberContext, String portName)
            throws KubernetesClientException {

        KubernetesApiClient kubernetesApi = kubernetesClusterContext.getKubApi();
        Service service = kubernetesApi.getService(serviceId);

        if (service != null) {
            Map<String, String> annotations = service.getMetadata().getAnnotations();
            String applicationIdLabel = annotations.get(CloudControllerConstants.APPLICATION_ID_LABEL);
            String clusterInstanceIdLabel = annotations.get(CloudControllerConstants.CLUSTER_INSTANCE_ID_LABEL);
            String portNameLabel = annotations.get(CloudControllerConstants.PORT_NAME_LABEL);

            return (StringUtils.isNotEmpty(applicationIdLabel) &&
                    StringUtils.isNotEmpty(clusterInstanceIdLabel) &&
                    StringUtils.isNotEmpty(portNameLabel) &&
                    applicationIdLabel.equals(memberContext.getApplicationId()) &&
                    clusterInstanceIdLabel.equals(memberContext.getClusterInstanceId()) &&
                    portNameLabel.equals(portName)
            );
        }
        return false;
    }

    private String trimLabel(String key, String value) {
        if (StringUtils.isNotEmpty(value) && (value.length() > KubernetesConstants.MAX_LABEL_LENGTH)) {
            String trimmed = value.substring(0, KubernetesConstants.MAX_LABEL_LENGTH - 2).concat("X");
            log.warn(String.format("Kubernetes label trimmed: [key] %s [original] %s [trimmed] %s",
                    key, value, trimmed));
            return trimmed;
        }
        return value;
    }

    private String prepareServiceName(long serviceSeqNo) {
        return SERVICE_NAME_PREFIX + "-" + (serviceSeqNo);
    }

    private List<String> prepareMinionIPAddresses(KubernetesCluster kubernetesCluster) {
        List<String> minionPublicIPList = new ArrayList<String>();
        KubernetesHost[] kubernetesHosts = kubernetesCluster.getKubernetesHosts();
        if ((kubernetesHosts == null) || (kubernetesHosts.length == 0) || (kubernetesHosts[0] == null)) {
            throw new RuntimeException("Hosts not found in kubernetes cluster: [cluster] "
                    + kubernetesCluster.getClusterId());
        }
        for (KubernetesHost host : kubernetesHosts) {
            if (host != null) {
                minionPublicIPList.add(host.getPublicIPAddress());
            }
        }
        return minionPublicIPList;
    }

    /**
     * Find a kubernetes service by container port
     *
     * @param kubernetesServices
     * @param containerPort
     * @return
     */
    private KubernetesService findKubernetesService(Collection<KubernetesService> kubernetesServices,
                                                    int containerPort) {

        if (kubernetesServices != null) {
            for (KubernetesService kubernetesService : kubernetesServices) {
                if (kubernetesService.getContainerPort() == containerPort) {
                    return kubernetesService;
                }
            }
        }
        return null;
    }

    /**
     * Generate kubernetes service ports for cluster.
     *
     * @param kubernetesClusterContext
     * @param clusterId
     * @param cartridge
     */
    private void generateKubernetesServicePorts(String applicationId, String clusterId,
                                                KubernetesClusterContext kubernetesClusterContext,
                                                Cartridge cartridge) throws KubernetesClientException {
        synchronized (KubernetesIaas.class) {
            if (cartridge != null) {

                StringBuilder portMappingStrBuilder = new StringBuilder();
                for (PortMapping portMapping : Arrays.asList(cartridge.getPortMappings())) {

                    Collection<ClusterPortMapping> clusterPortMappings =
                            CloudControllerContext.getInstance().getClusterPortMappings(applicationId, clusterId);
                    if (clusterPortMappings == null) {
                        throw new CloudControllerException(String.format("Cluster port mappings not found: " +
                                "[application-id] %s [cluster-id] %s", applicationId, clusterId));
                    }

                    ClusterPortMapping clusterPortMapping = findClusterPortMapping(clusterPortMappings, portMapping);
                    if (clusterPortMapping == null) {
                        throw new CloudControllerException(String.format("Cluster port mapping not found: " +
                                        "[application-id] %s [cluster-id] %s [transport] %s", applicationId, clusterId,
                                portMapping.getName()));
                    }

                    if (clusterPortMapping.getKubernetesPortType() == null) {
                        throw new CloudControllerException(String.format("Kubernetes service type not " +
                                        "found [application-id] %s [cluster-id] %s [cartridge] %s", applicationId,
                                clusterId, cartridge));
                    }

                    String serviceType = portMapping.getKubernetesPortType();
                    clusterPortMapping.setKubernetesPortType(serviceType);

                    // If kubernetes service port is already set, skip setting a new one
                    if (clusterPortMapping.getKubernetesServicePort() == 0) {
                        if (serviceType.equals(KubernetesConstants.NODE_PORT)) {
                            int nextServicePort = kubernetesClusterContext.getNextServicePort();
                            if (nextServicePort == -1) {
                                throw new RuntimeException(
                                        String.format("Could not generate service port: [cluster-id] %s " +
                                                "[port] %d", clusterId, portMapping.getPort()));
                            }

                            // Find next available service port
                            KubernetesApiClient kubernetesApi = kubernetesClusterContext.getKubApi();
                            List<Service> services = kubernetesApi.getServices();
                            while (!nodePortAvailable(services, nextServicePort)) {
                                nextServicePort = kubernetesClusterContext.getNextServicePort();
                            }

                            clusterPortMapping.setKubernetesServicePort(nextServicePort);
                        } else {
                            clusterPortMapping.setKubernetesServicePort(portMapping.getPort());
                        }
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Kubernetes service port is already set: [application-id] %s " +
                                            "[cluster-id] %s [port] %d [service-port] %d",
                                    applicationId, clusterId, clusterPortMapping.getPort(),
                                    clusterPortMapping.getKubernetesServicePort()));
                        }
                    }

                    // Add port mappings to payload
                    if (portMappingStrBuilder.toString().length() > 0) {
                        portMappingStrBuilder.append(";");
                    }
                    portMappingStrBuilder.append(String.format("NAME:%s|PROTOCOL:%s|PORT:%d|PROXY_PORT:%d|TYPE:%s",
                            clusterPortMapping.getName(), clusterPortMapping.getProtocol(),
                            clusterPortMapping.getKubernetesServicePort(), clusterPortMapping.getProxyPort(),
                            clusterPortMapping.getKubernetesPortType()));

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Kubernetes service port generated: [application-id] %s " +
                                        "[cluster-id] %s [port] %d [service-port] %d",
                                applicationId, clusterId, clusterPortMapping.getPort(),
                                clusterPortMapping.getKubernetesServicePort()));
                    }
                }

                NameValuePair nameValuePair = new NameValuePair(PORT_MAPPINGS, portMappingStrBuilder.toString());
                payload.add(nameValuePair);

                // Persist service ports added to cluster port mappings
                CloudControllerContext.getInstance().persist();
            }
        }
    }

    private boolean nodePortAvailable(List<Service> services, int nodePort)
            throws KubernetesClientException {

        for (Service service : services) {
            for (ServicePort servicePort : service.getSpec().getPorts()) {
                if (servicePort.getNodePort() == nodePort) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Find cluster port mapping that corresponds to cartridge port mapping.
     *
     * @param clusterPortMappings
     * @param portMapping
     * @return
     */
    private ClusterPortMapping findClusterPortMapping(Collection<ClusterPortMapping> clusterPortMappings,
                                                      PortMapping portMapping) {
        for (ClusterPortMapping clusterPortMapping : clusterPortMappings) {
            if (clusterPortMapping.getName().equals(portMapping.getName())) {
                return clusterPortMapping;
            }
        }
        return null;
    }

    /**
     * Terminate a container by member id
     *
     * @param memberContext
     * @return
     * @throws MemberTerminationFailedException
     */
    public MemberContext terminateContainer(MemberContext memberContext) throws MemberTerminationFailedException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();
            handleNullObject(memberContext, "Could not terminate container, member context not found");

            Partition partition = memberContext.getPartition();
            if (partition == null) {
                String message = String.format("Partition not found in member context: [member] %s ",
                        memberContext.getMemberId());

                log.error(message);
                throw new RuntimeException(message);
            }

            String kubernetesClusterId = memberContext.getPartition().getKubernetesClusterId();
            handleNullObject(kubernetesClusterId, String.format("Could not terminate container, kubernetes cluster " +
                            "context id is null: [partition-id] %s [member-id] %s", partition.getId(),
                    memberContext.getMemberId()));

            KubernetesClusterContext kubernetesClusterContext =
                    CloudControllerContext.getInstance().getKubernetesClusterContext(kubernetesClusterId);
            handleNullObject(kubernetesClusterContext,
                    String.format("Could not terminate container, kubernetes cluster " +
                                    "context not found: [partition-id] %s [member-id] %s", partition.getId(),
                            memberContext.getMemberId()));
            KubernetesApiClient kubApi = kubernetesClusterContext.getKubApi();

            try {
                log.info(String.format("Removing kubernetes pod: [application] %s [cartridge] %s [member] %s [pod] %s",
                        memberContext.getApplicationId(), memberContext.getCartridgeType(), memberContext.getMemberId(),
                        memberContext.getKubernetesPodId()));

                // Remove pod
                kubApi.deletePod(memberContext.getKubernetesPodId());
                // Persist changes
                CloudControllerContext.getInstance().persist();

                log.info(String.format("Kubernetes pod removed successfully: [application] %s [cartridge] %s " +
                                "[member] %s [pod] %s",
                        memberContext.getApplicationId(), memberContext.getCartridgeType(), memberContext.getMemberId(),
                        memberContext.getKubernetesPodId()));
            }
            catch (KubernetesClientException ignore) {
                // we can't do nothing here
                log.warn(String.format("Could not delete pod: [pod-id] %s", memberContext.getKubernetesPodId()));
            }
            return memberContext;
        }
        finally {
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
                                                                 String kubernetesMasterPort, int upperPort,
                                                                 int lowerPort) {

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
        handleNullObject(propVal,
                "Property validation failed. Could not find property: '" + property + " in " + object);
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

    /**
     * Remove kubernetes services if available for application cluster.
     *
     * @param clusterContext
     * @param clusterInstanceId
     */
    public static void removeKubernetesServices(ClusterContext clusterContext, String clusterInstanceId) {

        if (clusterContext != null) {
            ArrayList<KubernetesService> kubernetesServices =
                    Lists.newArrayList(clusterContext.getKubernetesServices(clusterInstanceId));

            for (KubernetesService kubernetesService : kubernetesServices) {
                KubernetesClusterContext kubernetesClusterContext =
                        CloudControllerContext.getInstance()
                                .getKubernetesClusterContext(kubernetesService.getKubernetesClusterId());
                KubernetesApiClient kubernetesApiClient = kubernetesClusterContext.getKubApi();
                String serviceId = kubernetesService.getId();
                log.info(String.format("Deleting kubernetes service: [application-id] %s " +
                        "[service-id] %s", clusterContext.getApplicationId(), serviceId));

                try {
                    kubernetesApiClient.deleteService(serviceId);
                    kubernetesClusterContext.deallocatePort(kubernetesService.getPort());
                    clusterContext.removeKubernetesService(clusterInstanceId, serviceId);
                }
                catch (KubernetesClientException e) {
                    log.error(String.format("Could not delete kubernetes service: [application-id] %s " +
                            "[service-id] %s", clusterContext.getApplicationId(), serviceId), e);
                }
            }
        }

    }
}