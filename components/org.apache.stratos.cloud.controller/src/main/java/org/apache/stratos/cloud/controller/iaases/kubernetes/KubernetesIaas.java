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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.concurrent.ScheduledThreadExecutor;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.domain.Cartridge;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesClusterContext;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.iaases.Iaas;
import org.apache.stratos.cloud.controller.iaases.PartitionValidator;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.PodActivationWatcher;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.beans.NameValuePair;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.cloud.controller.domain.kubernetes.KubernetesCluster;
import org.apache.stratos.cloud.controller.domain.kubernetes.PortRange;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.*;
import org.apache.stratos.kubernetes.client.model.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Kubernetes IaaS implementation.
 */
public class KubernetesIaas extends Iaas {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);

    private static final long POD_CREATION_TIMEOUT = 120000; // 2 min
    private static final String PAYLOAD_PARAMETER_SEPARATOR = ",";
    private static final String PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR = "=";
    private static final String PAYLOAD_PARAMETER_PREFIX = "payload_parameter.";

    private PartitionValidator partitionValidator;
    private List<NameValuePair> payload;

    public KubernetesIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
        partitionValidator = new KubernetesPartitionValidator();
        payload = new ArrayList<NameValuePair>();
    }

    @Override
    public void initialize() {
    }

    /**
     * Set dynamic payload which needs to be passed to the containers as environment variables.
     * @param payloadByteArray
     */
    @Override
    public void setDynamicPayload(byte[] payloadByteArray) {
        // Clear existing payload parameters
        payload.clear();

        if(payloadByteArray != null) {
            String payloadString = new String(payloadByteArray);
            String[] parameterArray = payloadString.split(PAYLOAD_PARAMETER_SEPARATOR);
            if(parameterArray != null) {
                for(String parameter : parameterArray) {
                    if(parameter != null) {
                        String[] nameValueArray = parameter.split(PAYLOAD_PARAMETER_NAME_VALUE_SEPARATOR);
                        if ((nameValueArray != null) && (nameValueArray.length == 2)) {
                            NameValuePair nameValuePair = new NameValuePair(nameValueArray[0], nameValueArray[1]);
                            payload.add(nameValuePair);
                        }
                    }
                }
                if(log.isDebugEnabled()) {
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
     * @param memberContext
     * @return
     * @throws CartridgeNotFoundException
     */
    public MemberContext startContainer(MemberContext memberContext)
            throws CartridgeNotFoundException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();

            handleNullObject(memberContext, "Could not start container, member context is null");
            if (log.isInfoEnabled()) {
                log.info(String.format("Starting container: [cartridge-type] %s", memberContext.getCartridgeType()));
            }

            // Validate cluster id
            String clusterId = memberContext.getClusterId();
            String memberId = memberContext.getMemberId();
            handleNullObject(clusterId, "Could not start container, cluster id is null in member context");

            // Validate cluster context
            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, "Could not start container, cluster context not found: [cluster-id] "
                    + clusterId + " [member-id] " + memberId);

            // Validate partition
            Partition partition = memberContext.getPartition();
            handleNullObject(partition, "Could not start container, partition not found in member context: " +
                    "[cluster-id] " + clusterId + " [member-id] " + memberId);

            // Validate cartridge
            String cartridgeType = clusterContext.getCartridgeType();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = "Could not start container, cartridge not found: [cartridge-type] " + cartridgeType + " " +
                        "[cluster-id] " + clusterId + " [member-id] " + memberId;
                log.error(msg);
                throw new CartridgeNotFoundException(msg);
            }

            try {
                String kubernetesClusterId = partition.getKubernetesClusterId();
                clusterContext.setKubernetesClusterId(kubernetesClusterId);
                KubernetesCluster kubernetesCluster = CloudControllerContext.getInstance().
                        getKubernetesCluster(kubernetesClusterId);
                handleNullObject(kubernetesCluster, "Could not start container, kubernetes cluster not found: " +
                        "[kubernetes-cluster-id] " + kubernetesClusterId + " [cluster-id] " + clusterId +
                        " [member-id] " + memberId);

                // Prepare kubernetes context
                String kubernetesMasterIp = kubernetesCluster.getKubernetesMaster().getHostIpAddress();
                PortRange kubernetesPortRange = kubernetesCluster.getPortRange();
                String kubernetesMasterPort = CloudControllerUtil.getProperty(
                        kubernetesCluster.getKubernetesMaster().getProperties(), StratosConstants.KUBERNETES_MASTER_PORT,
                        StratosConstants.KUBERNETES_MASTER_DEFAULT_PORT);

                // Add kubernetes cluster payload parameters to payload
                if((kubernetesCluster.getProperties() != null) &&
                        (kubernetesCluster.getProperties().getProperties() != null)) {
                    for(Property property : kubernetesCluster.getProperties().getProperties()) {
                        if(property != null) {
                            if(property.getName().startsWith(PAYLOAD_PARAMETER_PREFIX)) {
                                String name = property.getName().replace(PAYLOAD_PARAMETER_PREFIX, "");
                                payload.add(new NameValuePair(name, property.getValue()));
                            }
                        }
                    }
                }

                KubernetesClusterContext kubClusterContext = getKubernetesClusterContext(kubernetesClusterId,
                        kubernetesMasterIp, kubernetesMasterPort, kubernetesPortRange.getUpper(),
                        kubernetesPortRange.getLower());

                // Get kubernetes API
                KubernetesApiClient kubernetesApi = kubClusterContext.getKubApi();

                // Create replication controller
                createReplicationController(clusterContext, memberContext, kubernetesApi);

                // Create proxy services for port mappings
                List<Service> services = createProxyServices(clusterContext, kubClusterContext, kubernetesApi);
                clusterContext.setKubernetesServices(services);
                CloudControllerContext.getInstance().updateClusterContext(clusterContext);

                // Wait for pod to be created
                List<Pod> pods = waitForPodToBeCreated(memberContext, kubernetesApi);
                if (pods.size() != 1) {
                    String message = String.format("Pod did not create within %d sec, hence removing " +
                            "replication controller and service: [cluster-id] %s [member-id] %s",
                            ((int)POD_CREATION_TIMEOUT/1000), clusterId, memberId);
                    if (log.isDebugEnabled()) {
                        log.debug(message);
                    }
                    try {
                        terminateContainers(clusterId);
                        throw new RuntimeException(message);
                    } catch (Exception e) {
                        String errorMessage = "Could not terminate containers which were partially created";
                        log.error(errorMessage, e);
                        throw new RuntimeException(errorMessage, e);
                    }
                }
                Pod pod = pods.get(0);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Pod created: [cluster-id] %s [member-id] %s [pod-id] %s",
                            clusterId, memberId, pod.getId()));
                }

                // Create member context
                String memberIp = kubernetesMasterIp;
                MemberContext newMemberContext = createNewMemberContext(memberContext, pod, memberIp);
                CloudControllerContext.getInstance().addMemberContext(newMemberContext);

                // wait till pod status turns to running and send member spawned.
                ScheduledThreadExecutor exec = ScheduledThreadExecutor.getInstance();
                if (log.isDebugEnabled()) {
                    log.debug("Cloud Controller is starting the instance start up thread.");
                }
                CloudControllerContext.getInstance().addScheduledFutureJob(newMemberContext.getMemberId(),
                        exec.schedule(new PodActivationWatcher(pod.getId(), newMemberContext, kubernetesApi), 5000));

                // persist in registry
                CloudControllerContext.getInstance().persist();
                log.info("Container started successfully: [cluster-id] " + clusterId + " [member-id] " +
                        memberContext.getMemberId());

                return newMemberContext;
            } catch (Exception e) {
                String msg = String.format("Could not start container: [cartridge-type] %s [member-id] %s",
                        memberContext.getCartridgeType(), memberContext.getMemberId());
                log.error(msg, e);
                throw new IllegalStateException(msg, e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private MemberContext createNewMemberContext(MemberContext memberContext, Pod pod, String memberIp) {
        MemberContext newMemberContext = new MemberContext();
        newMemberContext.setCartridgeType(memberContext.getCartridgeType());
        newMemberContext.setClusterId(memberContext.getClusterId());
        newMemberContext.setClusterInstanceId(memberContext.getClusterInstanceId());
        newMemberContext.setMemberId(memberContext.getMemberId());
        newMemberContext.setNetworkPartitionId(memberContext.getNetworkPartitionId());
        newMemberContext.setPartition(memberContext.getPartition());
        newMemberContext.setInstanceId(pod.getId());
        newMemberContext.setDefaultPrivateIP(memberIp);
        newMemberContext.setPrivateIPs(new String[]{memberIp});
        newMemberContext.setDefaultPublicIP(memberIp);
        newMemberContext.setPublicIPs(new String[]{memberIp});
        newMemberContext.setInitTime(memberContext.getInitTime());
        newMemberContext.setProperties(memberContext.getProperties());
        return newMemberContext;
    }

    private List<Pod> waitForPodToBeCreated(MemberContext memberContext, KubernetesApiClient kubernetesApi)
            throws KubernetesClientException, InterruptedException {
        Labels labels = new Labels();
        labels.setName(CloudControllerUtil.replaceDotsWithDash(memberContext.getMemberId()));
        List<Pod> podList = new ArrayList<Pod>();
        long startTime = System.currentTimeMillis();
        while (podList.size() == 0) {
            podList.clear();
            List<Pod> pods = kubernetesApi.queryPods(new Labels[]{labels});
            if((pods != null) && (pods.size() > 0)){
                for(Pod pod : pods) {
                    if(pod != null) {
                        podList.add(pod);
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Member pod: [member-id] " + memberContext.getMemberId() + " [count] " + podList.size());
            }
            if ((System.currentTimeMillis() - startTime) > POD_CREATION_TIMEOUT) {
                break;
            }
            Thread.sleep(5000);
        }
        return podList;
    }

    /**
     * Create new replication controller for the cluster and generate environment variables using member context.
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
        String replicationControllerId = CloudControllerUtil.replaceDotsWithDash(memberContext.getMemberId());
        String replicationControllerName = replicationControllerId;
        String dockerImage = iaasProvider.getImage();
        List<Integer> containerPorts = KubernetesIaasUtil.prepareCartridgePorts(cartridge);
        EnvironmentVariable[] environmentVariables = KubernetesIaasUtil.prepareEnvironmentVariables(
                clusterContext, memberContext);
        int replicas = 1;

        kubernetesApi.createReplicationController(replicationControllerId, replicationControllerName,
                dockerImage, containerPorts, environmentVariables, replicas);
        if (log.isInfoEnabled()) {
            log.info(String.format("Replication controller created successfully: [cartridge-type] %s [member-id] %s",
                    memberContext.getCartridgeType(), memberContext.getClusterId()));
        }
    }

    /**
     * Create proxy services for the cluster and add them to the cluster context.
     * @param clusterContext
     * @param kubernetesClusterContext
     * @param kubernetesApi
     * @return
     * @throws KubernetesClientException
     */
    private List<Service> createProxyServices(ClusterContext clusterContext,
                                              KubernetesClusterContext kubernetesClusterContext,
                                              KubernetesApiClient kubernetesApi) throws KubernetesClientException {
        List<Service> services = new ArrayList<Service>();

        String clusterId = clusterContext.getClusterId();
        Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(clusterContext.getCartridgeType());
        if(cartridge == null) {
            String message = "Could not create kubernetes services, cartridge not found: [cartridge-type] " +
                    clusterContext.getCartridgeType();
            log.error(message);
            throw new RuntimeException(message);
        }

        List<PortMapping> portMappings = cartridge.getPortMappings();
        for(PortMapping portMapping : portMappings) {
            String serviceId = KubernetesIaasUtil.prepareKubernetesServiceId(
                    CloudControllerUtil.replaceDotsWithDash(clusterId), portMapping);
            int nextServicePort = kubernetesClusterContext.getNextServicePort();
            if(nextServicePort == -1) {
                throw new RuntimeException(String.format("Could not generate service port: [cluster-id] %s ",
                        clusterContext.getClusterId()));
            }

            if (log.isInfoEnabled()) {
                log.info(String.format("Creating kubernetes service: [cluster-id] %s [service-id] %s " +
                                "[protocol] %s [service-port] %d [container-port] %s [proxy-port] %s", clusterId,
                        serviceId, portMapping.getProtocol(), nextServicePort, portMapping.getPort(),
                        portMapping.getProxyPort()));
            }

            String serviceName = serviceId;
            int servicePort = nextServicePort;
            int containerPort = Integer.parseInt(portMapping.getPort());
            String publicIp = kubernetesClusterContext.getMasterIp();

            kubernetesApi.createService(serviceId, serviceName, servicePort, containerPort, publicIp);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignore) {
            }

            Service service = kubernetesApi.getService(serviceId);
            services.add(service);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes service successfully created: [cluster-id] %s [service-id] %s " +
                                "[protocol] %s [service-port] %d [container-port] %s [proxy-port] %s", clusterId,
                        service.getId(), portMapping.getProtocol(), service.getPort(), portMapping.getPort(),
                        portMapping.getProxyPort()));
            }
        }
        return services;
    }

    /**
     * Terminate all the containers belong to a cluster by cluster id.
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

            // Remove the services
            List<Service> services = clusterContext.getKubernetesServices();
            if (services != null) {
                for (Service service : services) {
                    try {
                        kubApi.deleteService(service.getId());
                        int allocatedPort = service.getPort();
                        kubClusterContext.deallocatePort(allocatedPort);
                    } catch (KubernetesClientException e) {
                        log.error("Could not remove kubernetes service: [cluster-id] " + clusterId, e);
                    }
                }
            }

            List<MemberContext> memberContextsRemoved = new ArrayList<MemberContext>();
            List<MemberContext> memberContexts = CloudControllerContext.getInstance().getMemberContextsOfClusterId(clusterId);
            if(memberContexts != null) {
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
    public void allocateIpAddress(String clusterId, MemberContext memberContext, Partition partition) {

    }
}
