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

package org.apache.stratos.cloud.controller.iaases;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.concurrent.ScheduledThreadExecutor;
import org.apache.stratos.cloud.controller.context.CloudControllerContext;
import org.apache.stratos.cloud.controller.domain.*;
import org.apache.stratos.cloud.controller.exception.*;
import org.apache.stratos.cloud.controller.functions.ContainerClusterContextToReplicationController;
import org.apache.stratos.cloud.controller.iaases.validators.KubernetesPartitionValidator;
import org.apache.stratos.cloud.controller.iaases.validators.PartitionValidator;
import org.apache.stratos.cloud.controller.services.impl.CloudControllerServiceUtil;
import org.apache.stratos.cloud.controller.util.CloudControllerUtil;
import org.apache.stratos.cloud.controller.util.PodActivationWatcher;
import org.apache.stratos.common.Property;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.kubernetes.KubernetesGroup;
import org.apache.stratos.common.kubernetes.PortRange;
import org.apache.stratos.kubernetes.client.KubernetesApiClient;
import org.apache.stratos.kubernetes.client.exceptions.KubernetesClientException;
import org.apache.stratos.kubernetes.client.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;

/**
 * Kubernetes iaas implementation.
 */
public class KubernetesIaas extends Iaas {

    private static final Log log = LogFactory.getLog(KubernetesIaas.class);
    private static final long POD_CREATION_TIMEOUT = 60000; // 1 min

    private PartitionValidator partitionValidator;

    public KubernetesIaas(IaasProvider iaasProvider) {
        super(iaasProvider);
        partitionValidator = new KubernetesPartitionValidator();
    }

    @Override
    public void initialize() {

    }

    @Override
    public MemberContext startInstance(MemberContext memberContext) throws CartridgeNotFoundException {
        return startContainer(memberContext);
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
    public PartitionValidator getPartitionValidator() {
        return partitionValidator;
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

    @Override
    public void setDynamicPayload(byte[] payload) {
          // Payload is passed via environment
    }

    @Override
    public void terminateInstance(MemberContext memberContext) throws InvalidCartridgeTypeException,
            InvalidMemberException, MemberTerminationFailedException {
        terminateContainer(memberContext.getMemberId());
    }

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
            handleNullObject(clusterId, "Could not start containers, cluster id is null in member context");

            // Validate cluster context
            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, "Could not start containers, cluster context not found: [cluster-id] "
                    + clusterId + " [member-id] " + memberId);

            // Validate partition
            Partition partition = memberContext.getPartition();
            handleNullObject(partition, "Could not start containers, partition not found in member context: " +
                    "[cluster-id] " + clusterId + " [member-id] " + memberId);

            // Validate cartridge
            String cartridgeType = clusterContext.getCartridgeType();
            Cartridge cartridge = CloudControllerContext.getInstance().getCartridge(cartridgeType);
            if (cartridge == null) {
                String msg = "Could not start containers, cartridge not found: [cartridge-type] " + cartridgeType + " " +
                        "[cluster-id] " + clusterId + " [member-id] " + memberId;
                log.error(msg);
                throw new CartridgeNotFoundException(msg);
            }

            try {
                String kubernetesClusterId = readProperty(StratosConstants.KUBERNETES_CLUSTER_ID,
                        partition.getProperties(),
                        partition.toString());

                KubernetesGroup kubernetesGroup = CloudControllerContext.getInstance().
                        getKubernetesGroup(kubernetesClusterId);
                handleNullObject(kubernetesGroup, "Could not start container, kubernetes group not found: " +
                        "[kubernetes-cluster-id] " + kubernetesClusterId + " [cluster-id] " + clusterId +
                        " [member-id] " + memberId);

                // Prepare kubernetes context
                String kubernetesMasterIp = kubernetesGroup.getKubernetesMaster().getHostIpAddress();
                PortRange kubernetesPortRange = kubernetesGroup.getPortRange();
                String kubernetesMasterPort = CloudControllerUtil.getProperty(
                        kubernetesGroup.getKubernetesMaster().getProperties(), StratosConstants.KUBERNETES_MASTER_PORT,
                        StratosConstants.KUBERNETES_MASTER_DEFAULT_PORT);
                KubernetesClusterContext kubClusterContext = getKubernetesClusterContext(kubernetesClusterId,
                        kubernetesMasterIp, kubernetesMasterPort, kubernetesPortRange.getLower(),
                        kubernetesPortRange.getUpper());

                // Get kubernetes API
                KubernetesApiClient kubernetesApi = kubClusterContext.getKubApi();

                // Create replication controller
                createReplicationController(memberContext, clusterId, kubernetesApi);

                // Create proxy services for port mappings
                List<Service> services = createProxyServices(clusterContext, kubClusterContext, kubernetesApi);

                // Wait for pod to be created
                Pod[] pods = waitForPodToBeCreated(memberContext, kubernetesApi);
                if (pods.length != 1) {
                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Pod did not create within %d sec, hence deleting the service: " +
                                "[cluster-id] %s [member-id] %s", ((int)POD_CREATION_TIMEOUT/1000), clusterId, memberId));
                    }
                    terminateContainers(clusterId);
                    return null;
                }
                Pod pod = pods[0];
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Pod created: [cluster-id] %s [member-id] %s [pod-id] %s",
                            clusterId, memberId, pod.getId()));
                }

                // Create member context
                MemberContext newMemberContext = new MemberContext();
                newMemberContext.setCartridgeType(cartridgeType);
                newMemberContext.setClusterId(clusterId);
                newMemberContext.setMemberId(memberContext.getMemberId());
                newMemberContext.setClusterInstanceId(memberContext.getClusterInstanceId());
                newMemberContext.setInitTime(memberContext.getInitTime());
                newMemberContext.setNetworkPartitionId(memberContext.getNetworkPartitionId());
                newMemberContext.setPartition(memberContext.getPartition());
                newMemberContext.setInitTime(System.currentTimeMillis());
                newMemberContext.setInstanceId(pod.getId());
                newMemberContext.setPrivateIpAddress(pod.getCurrentState().getHostIP());
                newMemberContext.setPublicIpAddress(pod.getCurrentState().getHostIP());
                newMemberContext.setProperties(memberContext.getProperties());

                Property servicesProperty = new Property();
                servicesProperty.setName(StratosConstants.KUBERNETES_SERVICES);
                servicesProperty.setValue(services);
                newMemberContext.getProperties().addProperty(servicesProperty);

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
                String msg = "Could not start container: " + memberContext.toString() + " Cause: " + e.getMessage();
                log.error(msg, e);
                throw new IllegalStateException(msg, e);
            }
        } finally {
            if (lock != null) {
                CloudControllerContext.getInstance().releaseWriteLock(lock);
            }
        }
    }

    private Pod[] waitForPodToBeCreated(MemberContext memberContext, KubernetesApiClient kubernetesApi) throws KubernetesClientException, InterruptedException {
        Labels labels = new Labels();
        labels.setName(memberContext.getMemberId());
        Pod[] pods = new Pod[0];
        long startTime = System.currentTimeMillis();
        while (pods.length == 1) {
            pods = kubernetesApi.queryPods(new Labels[]{labels});
            if (log.isDebugEnabled()) {
                log.debug("Member pod: [member-id] " + memberContext.getMemberId() + " [count] " + pods.length);
            }
            if ((System.currentTimeMillis() - startTime) > POD_CREATION_TIMEOUT) {
                break;
            }
            Thread.sleep(5000);
        }
        return pods;
    }

    /**
     * Create new replication controller for the cluster and generate environment variables using member context.
     * @param memberContext
     * @param clusterId
     * @param kubernetesApi
     * @throws KubernetesClientException
     */
    private ReplicationController createReplicationController(MemberContext memberContext, String clusterId, KubernetesApiClient kubernetesApi) throws KubernetesClientException {
        if (log.isDebugEnabled()) {
            log.debug("Creating kubernetes replication controller: [cluster-id] " + clusterId);
        }
        ContainerClusterContextToReplicationController controllerFunction = new ContainerClusterContextToReplicationController();
        ReplicationController replicationController = controllerFunction.apply(memberContext);
        kubernetesApi.createReplicationController(replicationController);
        if (log.isDebugEnabled()) {
            log.debug("Kubernetes replication controller successfully created: [cluster-id] " + clusterId);
        }
        return replicationController;
    }

    /**
     * Create proxy services for the cluster
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
            if (log.isInfoEnabled()) {
                log.info(String.format("Creating kubernetes service: [cluster-id] %s [protocol] %s [port] %s ",
                        clusterId, portMapping.getProtocol(), portMapping.getPort()));
            }

            Service service = new Service();
            service.setId(prepareKubernetesServiceId(clusterId, portMapping));
            service.setApiVersion("v1beta1");
            service.setKind("Service");
            int nextServicePort = kubernetesClusterContext.getNextServicePort();
            if(nextServicePort == -1) {
                throw new RuntimeException("Service port not found");
            }
            service.setPort(nextServicePort);
            Selector selector = new Selector();
            selector.setName(clusterId);
            service.setSelector(selector);

            kubernetesApi.createService(service);
            services.add(service);

            if (log.isInfoEnabled()) {
                log.info(String.format("Kubernetes service successfully created: [cluster-id] %s [protocol] %s " +
                                "[port] %s [proxy-port] %s", clusterId, portMapping.getProtocol(),
                        portMapping.getPort(), service.getPort()));
            }
        }
        // Set service port and update
        Property servicePortProperty = new Property();
        servicePortProperty.setName(StratosConstants.KUBERNETES_SERVICES);
        servicePortProperty.setValue(services);
        clusterContext.getProperties().addProperty(servicePortProperty);
        CloudControllerContext.getInstance().addClusterContext(clusterContext);

        return services;
    }

    private String prepareKubernetesServiceId(String clusterId, PortMapping portMapping) {
        return String.format("%s-%s-%s", clusterId, portMapping.getProtocol(), portMapping.getPort());
    }

    public MemberContext[] terminateContainers(String clusterId)
            throws InvalidClusterException {
        Lock lock = null;
        try {
            lock = CloudControllerContext.getInstance().acquireMemberContextWriteLock();

            ClusterContext clusterContext = CloudControllerContext.getInstance().getClusterContext(clusterId);
            handleNullObject(clusterContext, "Could not terminate containers, cluster not found: [cluster-id] " + clusterId);

            String kubernetesClusterId = CloudControllerUtil.getProperty(clusterContext.getProperties(),
                    StratosConstants.KUBERNETES_CLUSTER_ID);
            handleNullObject(kubernetesClusterId, "Could not terminate containers, kubernetes cluster id not found: " +
                    "[cluster-id] " + clusterId);

            KubernetesClusterContext kubClusterContext = CloudControllerContext.getInstance().getKubernetesClusterContext(kubernetesClusterId);
            handleNullObject(kubClusterContext, "Could not terminate containers, kubernetes cluster not found: " +
                    "[kubernetes-cluster-id] " + kubernetesClusterId);

            KubernetesApiClient kubApi = kubClusterContext.getKubApi();

            // Remove the services
            Property servicesProperty = clusterContext.getProperties().getProperty(StratosConstants.KUBERNETES_SERVICES);
            if (servicesProperty != null) {
                List<Service> services = (List<Service>) servicesProperty.getValue();
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
            for(MemberContext memberContext : memberContexts) {
                try {
                    MemberContext memberContextRemoved = terminateContainer(memberContext.getMemberId());
                    memberContextsRemoved.add(memberContextRemoved);
                } catch (MemberTerminationFailedException e) {
                    String message = "Could not terminate container: [member-id] " + memberContext.getMemberId();
                    log.error(message);
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

            String kubernetesClusterId = CloudControllerUtil.getProperty(clusterContext.getProperties(),
                    StratosConstants.KUBERNETES_CLUSTER_ID);
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
                Pod[] pods = kubApi.queryPods(new Labels[]{l});
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
}
