/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.stratos.cartridge.agent.extensions;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.RepositoryInformation;
import org.apache.stratos.cartridge.agent.artifact.deployment.synchronizer.git.impl.GitBasedArtifactRepository;
import org.apache.stratos.cartridge.agent.config.CartridgeAgentConfiguration;
import org.apache.stratos.cartridge.agent.event.publisher.CartridgeAgentEventPublisher;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentConstants;
import org.apache.stratos.cartridge.agent.util.CartridgeAgentUtils;
import org.apache.stratos.cartridge.agent.util.ExtensionUtils;
import org.apache.stratos.messaging.domain.tenant.Tenant;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.event.instance.notifier.ArtifactUpdatedEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupClusterEvent;
import org.apache.stratos.messaging.event.instance.notifier.InstanceCleanupMemberEvent;
import org.apache.stratos.messaging.event.tenant.CompleteTenantEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingRemovedEvent;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;

import java.lang.reflect.Type;
import java.util.*;

public class DefaultExtensionHandler implements ExtensionHandler {

    private static final Log log = LogFactory.getLog(DefaultExtensionHandler.class);
    private static final Gson gson = new Gson();
    private static final Type memberType = new TypeToken<Collection<Member>>() {
    }.getType();
    private static final Type tenantType = new TypeToken<Collection<Tenant>>() {
    }.getType();
    private static final Type serviceType = new TypeToken<Collection<Service>>() {
    }.getType();
    private final ArrayList<Member> wkMembers = new ArrayList<Member>();

    @Override
    public void onInstanceStartedEvent() {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Processing instance started event...");
            }

            if (CartridgeAgentConfiguration.getInstance().isMultitenant()) {
                ExtensionUtils.executeCopyArtifactsExtension(
                        CartridgeAgentConfiguration.getInstance().getAppPath() + "/repository/deployment/server/",
                        CartridgeAgentConstants.SUPERTENANT_TEMP_PATH);
            }

            Map<String, String> env = new HashMap<String, String>();
            ExtensionUtils.executeInstanceStartedExtension(env);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error processing instance started event", e);
            }
        }
    }

    @Override
    public void onInstanceActivatedEvent() {
        ExtensionUtils.executeInstanceActivatedExtension();
    }

    @Override
    public void onArtifactUpdatedEvent(ArtifactUpdatedEvent artifactUpdatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Artifact update event received: [tenant] %s [cluster] %s [status] %s",
                    artifactUpdatedEvent.getTenantId(), artifactUpdatedEvent.getClusterId(), artifactUpdatedEvent.getStatus()));
        }

        String clusterIdInMessage = artifactUpdatedEvent.getClusterId();
        String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
        String repoURL = artifactUpdatedEvent.getRepoURL();

        // we need to execute the logic if only the update is relevant to this cluster domain
        if (StringUtils.isNotEmpty(repoURL) && (clusterIdInPayload != null) && clusterIdInPayload.equals(clusterIdInMessage)) {

            String localRepoPath = CartridgeAgentConfiguration.getInstance().getAppPath();
            String repoPassword = CartridgeAgentUtils.decryptPassword(artifactUpdatedEvent.getRepoPassword());
            String repoUsername = artifactUpdatedEvent.getRepoUserName();
            String tenantId = artifactUpdatedEvent.getTenantId();
            boolean isMultitenant = CartridgeAgentConfiguration.getInstance().isMultitenant();

            if (log.isInfoEnabled()) {
                log.info("Executing git checkout");
            }
            RepositoryInformation repoInformation = new RepositoryInformation();
            repoInformation.setRepoUsername(repoUsername);
            if (repoPassword == null) {
                repoInformation.setRepoPassword("");
            } else {
                repoInformation.setRepoPassword(repoPassword);
            }
            repoInformation.setRepoUrl(repoURL);
            repoInformation.setRepoPath(localRepoPath);
            repoInformation.setTenantId(tenantId);
            repoInformation.setMultitenant(isMultitenant);
            boolean cloneExists = GitBasedArtifactRepository.getInstance().cloneExists(repoInformation);
            try {
                GitBasedArtifactRepository.getInstance().checkout(repoInformation);
            } catch (Exception e) {
                log.error(e);
            }
            Map<String, String> env = new HashMap<String, String>();
            env.put("STRATOS_ARTIFACT_UPDATED_CLUSTER_ID", artifactUpdatedEvent.getClusterId());
            env.put("STRATOS_ARTIFACT_UPDATED_TENANT_ID", artifactUpdatedEvent.getTenantId());
            env.put("STRATOS_ARTIFACT_UPDATED_REPO_URL", artifactUpdatedEvent.getRepoURL());
            env.put("STRATOS_ARTIFACT_UPDATED_REPO_PASSWORD", artifactUpdatedEvent.getRepoPassword());
            env.put("STRATOS_ARTIFACT_UPDATED_REPO_USERNAME", artifactUpdatedEvent.getRepoUserName());
            env.put("STRATOS_ARTIFACT_UPDATED_STATUS", artifactUpdatedEvent.getStatus());
            ExtensionUtils.executeArtifactsUpdatedExtension(env);

            if (!cloneExists && !isMultitenant) {
                // Executed git clone, publish instance activated event
                CartridgeAgentEventPublisher.publishInstanceActivatedEvent();
            }

            // Start the artifact update task
            boolean artifactUpdateEnabled = Boolean.parseBoolean(System.getProperty(CartridgeAgentConstants.ENABLE_ARTIFACT_UPDATE));
            if (artifactUpdateEnabled) {

                boolean autoCommit = CartridgeAgentConfiguration.getInstance().isCommitsEnabled();
                boolean autoCheckout = CartridgeAgentConfiguration.getInstance().isCheckoutEnabled();

                long artifactUpdateInterval = 10;
                // get update interval
                String artifactUpdateIntervalStr = System.getProperty(CartridgeAgentConstants.ARTIFACT_UPDATE_INTERVAL);

                if (artifactUpdateIntervalStr != null && !artifactUpdateIntervalStr.isEmpty()) {
                    try {
                        artifactUpdateInterval = Long.parseLong(artifactUpdateIntervalStr);

                    } catch (NumberFormatException e) {
                        log.error("Invalid artifact sync interval specified ", e);
                        artifactUpdateInterval = 10;
                    }
                }

                log.info("Artifact updating task enabled, update interval: " + artifactUpdateInterval + "s");
                if (autoCommit) {
                    log.info("Auto Commit is turned on ");
                } else {
                    log.info("Auto Commit is turned off ");
                }

                if (autoCheckout) {
                    log.info("Auto Checkout is turned on ");
                } else {
                    log.info("Auto Checkout is turned off ");
                }

                GitBasedArtifactRepository.getInstance().scheduleSyncTask(repoInformation, autoCheckout, autoCommit, artifactUpdateInterval);

            } else {
                log.info("Artifact updating task disabled");
            }
        }
    }

    @Override
    public void onArtifactUpdateSchedulerEvent(String tenantId) {
        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_ARTIFACT_UPDATED_TENANT_ID", tenantId);
        env.put("STRATOS_ARTIFACT_UPDATED_SCHEDULER", "true");
        ExtensionUtils.executeArtifactsUpdatedExtension(env);
    }

    @Override
    public void onInstanceCleanupClusterEvent(InstanceCleanupClusterEvent instanceCleanupClusterEvent) {
        cleanup();
    }

    @Override
    public void onInstanceCleanupMemberEvent(InstanceCleanupMemberEvent instanceCleanupMemberEvent) {
        cleanup();
    }

    private void cleanup() {
        if (log.isInfoEnabled()) {
            log.info("Executing cleaning up the data in the cartridge instance...");
        }
        //sending event on the maintenance mode
        CartridgeAgentEventPublisher.publishMaintenanceModeEvent();

        //cleaning up the cartridge instance's data
        ExtensionUtils.executeCleanupExtension();
        if (log.isInfoEnabled()) {
            log.info("cleaning up finished in the cartridge instance...");
        }
        if (log.isInfoEnabled()) {
            log.info("publishing ready to shutdown event...");
        }
        //publishing the Ready to shutdown event after performing the cleanup
        CartridgeAgentEventPublisher.publishInstanceReadyToShutdownEvent();
    }


    @Override
    public void onMemberActivatedEvent(MemberActivatedEvent memberActivatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Member activated event received: [service] %s [cluster] %s [member] %s",
                    memberActivatedEvent.getServiceName(), memberActivatedEvent.getClusterId(), memberActivatedEvent.getMemberId()));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(memberActivatedEvent);
            log.debug("Member activated event msg:" + msg);
        }

        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(memberActivatedEvent.getServiceName(),
                memberActivatedEvent.getClusterId(), memberActivatedEvent.getMemberId());
        if (!isConsistent) {
            if (log.isErrorEnabled()) {
                log.error("Topology is inconsistent...failed to execute member activated event");
            }
            return;
        }

        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberActivatedEvent.getServiceName());
        Cluster cluster = service.getCluster(memberActivatedEvent.getClusterId());
        String lbClusterId = cluster.getMember(memberActivatedEvent.getMemberId()).getLbClusterId();
        Member member = cluster.getMember(memberActivatedEvent.getMemberId());

        // check whether member activated event is received from the same cluster, lbcluster or cartridge group
        if (ExtensionUtils.isRelevantMemberEvent(memberActivatedEvent.getServiceName(), memberActivatedEvent.getClusterId(), lbClusterId)) {
            Map<String, String> env = new HashMap<String, String>();
            env.put("STRATOS_MEMBER_ACTIVATED_MEMBER_IP", memberActivatedEvent.getDefaultPrivateIP());
            env.put("STRATOS_MEMBER_ACTIVATED_MEMBER_ID", memberActivatedEvent.getMemberId());
            env.put("STRATOS_MEMBER_ACTIVATED_CLUSTER_ID", memberActivatedEvent.getClusterId());
            env.put("STRATOS_MEMBER_ACTIVATED_LB_CLUSTER_ID", lbClusterId);
            env.put("STRATOS_MEMBER_ACTIVATED_NETWORK_PARTITION_ID", memberActivatedEvent.getNetworkPartitionId());
            env.put("STRATOS_MEMBER_ACTIVATED_SERVICE_NAME", memberActivatedEvent.getServiceName());

            Collection<Port> ports = memberActivatedEvent.getPorts();
            String ports_str = "";
            for (Port port : ports) {
                ports_str += port.getProtocol() + "," + port.getValue() + "," + port.getProxy() + "|";
            }
            env.put("STRATOS_MEMBER_ACTIVATED_PORTS", ports_str);

            Collection<Member> members = cluster.getMembers();
            env.put("STRATOS_MEMBER_ACTIVATED_MEMBER_LIST_JSON", gson.toJson(members, memberType));
            String[] memberIps = ExtensionUtils.getLbMemberIp(lbClusterId);
            if (memberIps != null && memberIps.length > 1) {
                env.put("STRATOS_MEMBER_ACTIVATED_LB_IP", memberIps[0]);
                env.put("STRATOS_MEMBER_ACTIVATED_LB_PUBLIC_IP", memberIps[1]);
            }
            env.put("STRATOS_TOPOLOGY_JSON", gson.toJson(topology.getServices(), serviceType));
            ExtensionUtils.addProperties(service.getProperties(), env, "MEMBER_ACTIVATED_SERVICE_PROPERTY");
            ExtensionUtils.addProperties(cluster.getProperties(), env, "MEMBER_ACTIVATED_CLUSTER_PROPERTY");
            ExtensionUtils.addProperties(member.getProperties(), env, "MEMBER_ACTIVATED_MEMBER_PROPERTY");

            // if clustering is enabled check activated member is WK member
            String flagClustering = CartridgeAgentConfiguration.getInstance().getIsClustered();

            // if WK member is re-spawned, update axis2.xml
            if (member.getProperties() != null && "true".equals(member.getProperties().getProperty(CartridgeAgentConstants.CLUSTERING_PRIMARY_KEY)) &&
                    flagClustering != null && "true".equals(flagClustering.toLowerCase())) {
                if (log.isDebugEnabled()) {
                    log.debug(" If WK member is re-spawned, update axis2.xml ");
                }
                boolean hasWKIpChanged = true;
                for (Member m : this.wkMembers) {
                    if (m.getDefaultPrivateIP().equals(memberActivatedEvent.getDefaultPrivateIP())) {
                        hasWKIpChanged = false;
                    }
                }
                if (log.isDebugEnabled()) {
                    log.debug(" hasWKIpChanged " + hasWKIpChanged);
                }
                int minCount = Integer.parseInt(CartridgeAgentConfiguration.getInstance().getMinCount());
                boolean isWKMemberGroupReady = isWKMemberGroupReady(env, minCount);
                if (log.isDebugEnabled()) {
                    log.debug("minCount " + minCount);
                    log.debug("isWKMemberGroupReady " + isWKMemberGroupReady);
                }
                if (hasWKIpChanged && isWKMemberGroupReady) {
                    if (log.isDebugEnabled()) {
                        log.debug("Setting env var STRATOS_UPDATE_WK_IP to true");
                    }
                    env.put("STRATOS_UPDATE_WK_IP", "true");
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Setting env var STRATOS_CLUSTERING to " + flagClustering);
            }
            env.put("STRATOS_CLUSTERING", flagClustering);
            env.put("STRATOS_WK_MEMBER_COUNT", CartridgeAgentConfiguration.getInstance().getMinCount());
            ExtensionUtils.executeMemberActivatedExtension(env);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Member activated event is not relevant...skipping agent extension");
            }
        }
    }


    @Override
    public void onCompleteTopologyEvent(CompleteTopologyEvent completeTopologyEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Complete topology event received");

        }
        String serviceNameInPayload = CartridgeAgentConfiguration.getInstance().getServiceName();
        String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
        String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();


        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(serviceNameInPayload, clusterIdInPayload, memberIdInPayload);
        if (!isConsistent) {
            // if this member isn't there in the complete topology
            return;
        } else {
            CartridgeAgentConfiguration.getInstance().setInitialized(true);
        }

        Topology topology = completeTopologyEvent.getTopology();
        Service service = topology.getService(serviceNameInPayload);
        Cluster cluster = service.getCluster(clusterIdInPayload);

        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_TOPOLOGY_JSON", gson.toJson(topology.getServices(), serviceType));
        env.put("STRATOS_MEMBER_LIST_JSON", gson.toJson(cluster.getMembers(), memberType));
        ExtensionUtils.executeCompleteTopologyExtension(env);
    }


    @Override
    public void onMemberCreatedEvent(MemberCreatedEvent memberCreatedEvent) {

        // listen to this just to get updated faster about the member initialization
        if (log.isDebugEnabled()) {
            log.debug("Instance Spawned event received");
        }
        String serviceNameInPayload = CartridgeAgentConfiguration.getInstance().getServiceName();
        String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
        String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();
        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(serviceNameInPayload, clusterIdInPayload, memberIdInPayload);
        if (!isConsistent) {
            // if this event is not relevant to this member
            return;
        } else {
            CartridgeAgentConfiguration.getInstance().setInitialized(true);
        }
    }

    @Override
    public void onCompleteTenantEvent(CompleteTenantEvent completeTenantEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Complete tenant event received");
        }
        String tenantListJson = gson.toJson(completeTenantEvent.getTenants(), tenantType);
        if (log.isDebugEnabled()) {
            log.debug("Complete tenants:" + tenantListJson);
        }
        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_TENANT_LIST_JSON", tenantListJson);
        ExtensionUtils.executeCompleteTenantExtension(env);
    }

    @Override
    public void onMemberTerminatedEvent(MemberTerminatedEvent memberTerminatedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Member terminated event received: [service] %s [cluster] %s [member] %s",
                    memberTerminatedEvent.getServiceName(), memberTerminatedEvent.getClusterId(), memberTerminatedEvent.getMemberId()));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(memberTerminatedEvent);
            log.debug("Member terminated event msg:" + msg);
        }

        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(memberTerminatedEvent.getServiceName(),
                memberTerminatedEvent.getClusterId(), memberTerminatedEvent.getMemberId());
        if (!isConsistent) {
            if (log.isErrorEnabled()) {
                log.error("Topology is inconsistent...failed to execute member terminated event");
            }
            return;
        }

        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberTerminatedEvent.getServiceName());
        Cluster cluster = service.getCluster(memberTerminatedEvent.getClusterId());
        Member terminatedMember = cluster.getMember(memberTerminatedEvent.getMemberId());
        String lbClusterId = cluster.getMember(memberTerminatedEvent.getClusterId()).getLbClusterId();

        // check whether terminated member is within the same cluster, LB cluster or cartridge group
        if (ExtensionUtils.isRelevantMemberEvent(memberTerminatedEvent.getServiceName(),
                memberTerminatedEvent.getClusterId(), lbClusterId)) {

            Collection<Member> members = cluster.getMembers();
            Map<String, String> env = new HashMap<String, String>();
            env.put("STRATOS_MEMBER_TERMINATED_MEMBER_IP", terminatedMember.getDefaultPrivateIP());
            env.put("STRATOS_MEMBER_TERMINATED_MEMBER_ID", memberTerminatedEvent.getMemberId());
            env.put("STRATOS_MEMBER_TERMINATED_CLUSTER_ID", memberTerminatedEvent.getClusterId());
            env.put("STRATOS_MEMBER_TERMINATED_LB_CLUSTER_ID", lbClusterId);
            env.put("STRATOS_MEMBER_TERMINATED_NETWORK_PARTITION_ID", memberTerminatedEvent.getNetworkPartitionId());
            env.put("STRATOS_MEMBER_TERMINATED_SERVICE_NAME", memberTerminatedEvent.getServiceName());
            env.put("STRATOS_MEMBER_TERMINATED_MEMBER_LIST_JSON", gson.toJson(members, memberType));
            env.put("STRATOS_TOPOLOGY_JSON", gson.toJson(topology.getServices(), serviceType));
            String[] memberIps = ExtensionUtils.getLbMemberIp(lbClusterId);
            if (memberIps != null && memberIps.length > 1) {
                env.put("STRATOS_MEMBER_TERMINATED_LB_IP", memberIps[0]);
                env.put("STRATOS_MEMBER_TERMINATED_LB_PUBLIC_IP", memberIps[1]);
            }

            ExtensionUtils.addProperties(service.getProperties(), env, "MEMBER_TERMINATED_SERVICE_PROPERTY");
            ExtensionUtils.addProperties(cluster.getProperties(), env, "MEMBER_TERMINATED_CLUSTER_PROPERTY");
            ExtensionUtils.addProperties(terminatedMember.getProperties(), env, "MEMBER_TERMINATED_MEMBER_PROPERTY");
            ExtensionUtils.executeMemberTerminatedExtension(env);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Member terminated event is not relevant...skipping agent extension");
            }
        }
    }

    @Override
    public void onMemberSuspendedEvent(MemberSuspendedEvent memberSuspendedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Member suspended event received: [service] %s [cluster] %s [member] %s",
                    memberSuspendedEvent.getServiceName(), memberSuspendedEvent.getClusterId(), memberSuspendedEvent.getMemberId()));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(memberSuspendedEvent);
            log.debug("Member suspended event msg:" + msg);
        }

        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(memberSuspendedEvent.getServiceName(),
                memberSuspendedEvent.getClusterId(), memberSuspendedEvent.getMemberId());
        if (!isConsistent) {
            if (log.isErrorEnabled()) {
                log.error("Topology is inconsistent...failed to execute member suspended event");
            }
            return;
        }

        String clusterId = memberSuspendedEvent.getClusterId();
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberSuspendedEvent.getServiceName());
        Cluster cluster = service.getCluster(memberSuspendedEvent.getClusterId());
        Member suspendedMember = cluster.getMember(memberSuspendedEvent.getMemberId());
        String lbClusterId = cluster.getMember(memberSuspendedEvent.getClusterId()).getLbClusterId();

        // check whether new member is in the same member cluster or LB cluster of this instance
        if (ExtensionUtils.isRelevantMemberEvent(memberSuspendedEvent.getServiceName(),
                memberSuspendedEvent.getClusterId(), lbClusterId)) {
            Collection<Member> members = cluster.getMembers();
            Map<String, String> env = new HashMap<String, String>();
            env.put("STRATOS_MEMBER_SUSPENDED_MEMBER_IP", suspendedMember.getDefaultPrivateIP());
            env.put("STRATOS_MEMBER_SUSPENDED_MEMBER_ID", memberSuspendedEvent.getMemberId());
            env.put("STRATOS_MEMBER_SUSPENDED_CLUSTER_ID", memberSuspendedEvent.getClusterId());
            env.put("STRATOS_MEMBER_SUSPENDED_LB_CLUSTER_ID", lbClusterId);
            env.put("STRATOS_MEMBER_SUSPENDED_NETWORK_PARTITION_ID", memberSuspendedEvent.getNetworkPartitionId());
            env.put("STRATOS_MEMBER_SUSPENDED_SERVICE_NAME", memberSuspendedEvent.getServiceName());
            env.put("STRATOS_MEMBER_SUSPENDED_MEMBER_LIST_JSON", gson.toJson(members, memberType));
            env.put("STRATOS_TOPOLOGY_JSON", gson.toJson(topology.getServices(), serviceType));
            String[] memberIps = ExtensionUtils.getLbMemberIp(lbClusterId);
            if (memberIps != null && memberIps.length > 1) {
                env.put("STRATOS_MEMBER_SUSPENDED_LB_IP", memberIps[0]);
                env.put("STRATOS_MEMBER_SUSPENDED_LB_PUBLIC_IP", memberIps[1]);
            }
            ExtensionUtils.addProperties(service.getProperties(), env, "MEMBER_SUSPENDED_SERVICE_PROPERTY");
            ExtensionUtils.addProperties(cluster.getProperties(), env, "MEMBER_SUSPENDED_CLUSTER_PROPERTY");
            ExtensionUtils.addProperties(suspendedMember.getProperties(), env, "MEMBER_SUSPENDED_MEMBER_PROPERTY");
            ExtensionUtils.executeMemberSuspendedExtension(env);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Member suspended event is not relevant...skipping agent extension");
            }
        }
    }

    @Override
    public void onMemberStartedEvent(MemberStartedEvent memberStartedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Member started event received: [service] %s [cluster] %s [member] %s",
                    memberStartedEvent.getServiceName(), memberStartedEvent.getClusterId(), memberStartedEvent.getMemberId()));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(memberStartedEvent);
            log.debug("Member started event msg:" + msg);
        }

        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(memberStartedEvent.getServiceName(),
                memberStartedEvent.getClusterId(), memberStartedEvent.getMemberId());
        if (!isConsistent) {
            if (log.isErrorEnabled()) {
                log.error("Topology is inconsistent...failed to execute member started event");
            }
            return;
        }
        String clusterId = memberStartedEvent.getClusterId();
        Topology topology = TopologyManager.getTopology();
        Service service = topology.getService(memberStartedEvent.getServiceName());
        Cluster cluster = service.getCluster(memberStartedEvent.getClusterId());
        Member startedMember = cluster.getMember(memberStartedEvent.getMemberId());
        String lbClusterId = cluster.getMember(memberStartedEvent.getMemberId()).getLbClusterId();

        // check whether new member is in the same member cluster or LB cluster of this instance
        if (ExtensionUtils.isRelevantMemberEvent(memberStartedEvent.getServiceName(),
                memberStartedEvent.getClusterId(), lbClusterId)) {
            Collection<Member> members = cluster.getMembers();
            Map<String, String> env = new HashMap<String, String>();
            env.put("STRATOS_MEMBER_STARTED_MEMBER_IP", startedMember.getDefaultPrivateIP());
            env.put("STRATOS_MEMBER_STARTED_MEMBER_ID", memberStartedEvent.getMemberId());
            env.put("STRATOS_MEMBER_STARTED_CLUSTER_ID", memberStartedEvent.getClusterId());
            env.put("STRATOS_MEMBER_STARTED_LB_CLUSTER_ID", lbClusterId);
            env.put("STRATOS_MEMBER_STARTED_NETWORK_PARTITION_ID", memberStartedEvent.getNetworkPartitionId());
            env.put("STRATOS_MEMBER_STARTED_SERVICE_NAME", memberStartedEvent.getServiceName());
            env.put("STRATOS_MEMBER_STARTED_MEMBER_LIST_JSON", gson.toJson(members, memberType));
            env.put("STRATOS_MEMBER_STARTED_TOPOLOGY_JSON", gson.toJson(topology.getServices(), serviceType));
            String[] memberIps = ExtensionUtils.getLbMemberIp(lbClusterId);
            if (memberIps != null && memberIps.length > 1) {
                env.put("STRATOS_MEMBER_STARTED_LB_IP", memberIps[0]);
                env.put("STRATOS_MEMBER_STARTED_LB_PUBLIC_IP", memberIps[1]);
            }
            ExtensionUtils.addProperties(service.getProperties(), env, "MEMBER_STARTED_SERVICE_PROPERTY");
            ExtensionUtils.addProperties(cluster.getProperties(), env, "MEMBER_STARTED_CLUSTER_PROPERTY");
            ExtensionUtils.addProperties(startedMember.getProperties(), env, "MEMBER_STARTED_MEMBER_PROPERTY");
            ExtensionUtils.executeMemberStartedExtension(env);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Member started event is not relevant...skipping agent extension");
            }
        }
    }

    private boolean isWKMemberGroupReady(Map<String, String> envParameters, int minCount) {
        Topology topology = TopologyManager.getTopology();
        if (topology == null || !topology.isInitialized()) {
            return false;
        }
        String serviceGroupInPayload = CartridgeAgentConfiguration.getInstance().getServiceGroup();
        if (serviceGroupInPayload != null) {
            envParameters.put("STRATOS_SERVICE_GROUP", serviceGroupInPayload);
        }

        // clustering logic for apimanager
        if (serviceGroupInPayload != null && serviceGroupInPayload.equals("apim")) {

            // handle apistore and publisher case
            if (CartridgeAgentConfiguration.getInstance().getServiceName().equals("apistore") ||
                    CartridgeAgentConfiguration.getInstance().getServiceName().equals("publisher")) {

                Collection<Cluster> apistoreClusterCollection = topology.getService("apistore").getClusters();
                Collection<Cluster> publisherClusterCollection = topology.getService("publisher").getClusters();

                List<Member> apistoreMemberList = new ArrayList<Member>();
                for (Member member : apistoreClusterCollection.iterator().next().getMembers()) {
                    if (member.getStatus().equals(MemberStatus.Starting) || member.getStatus().equals(MemberStatus.Active)) {
                        apistoreMemberList.add(member);
                        this.wkMembers.add(member);
                    }
                }
                if (apistoreMemberList.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("API Store members not yet created");
                    }
                    return false;
                }
                Member apistoreMember = apistoreMemberList.get(0);
                envParameters.put("STRATOS_WK_APISTORE_MEMBER_IP", apistoreMember.getDefaultPrivateIP());
                if (log.isDebugEnabled()) {
                    log.debug("STRATOS_WK_APISTORE_MEMBER_IP: " + apistoreMember.getDefaultPrivateIP());
                }

                List<Member> publisherMemberList = new ArrayList<Member>();
                for (Member member : publisherClusterCollection.iterator().next().getMembers()) {
                    if (member.getStatus().equals(MemberStatus.Starting) || member.getStatus().equals(MemberStatus.Active)) {
                        publisherMemberList.add(member);
                        this.wkMembers.add(member);
                    }
                }
                if (publisherMemberList.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.debug("API Publisher members not yet created");
                    }
                    return false;
                }
                Member publisherMember = publisherMemberList.get(0);
                envParameters.put("STRATOS_WK_PUBLISHER_MEMBER_IP", publisherMember.getDefaultPrivateIP());
                if (log.isDebugEnabled()) {
                    log.debug("STRATOS_WK_PUBLISHER_MEMBER_IP: " + publisherMember.getDefaultPrivateIP());
                }

                return true;

            } else if (CartridgeAgentConfiguration.getInstance().getServiceName().equals("gatewaymgt") ||
                    CartridgeAgentConfiguration.getInstance().getServiceName().equals("gateway")) {

                if (CartridgeAgentConfiguration.getInstance().getDeployment() != null) {
                    // check if deployment is Manager Worker separated
                    if (CartridgeAgentConfiguration.getInstance().getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_MANAGER) ||
                            CartridgeAgentConfiguration.getInstance().getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_WORKER)) {

                        log.info("Deployment pattern for the node: " + CartridgeAgentConfiguration.getInstance().getDeployment());
                        envParameters.put("DEPLOYMENT", CartridgeAgentConfiguration.getInstance().getDeployment());
                        // check if WKA members of Manager Worker separated deployment is ready
                        return isManagerWorkerWKAGroupReady(envParameters);
                    }
                }


            } else if (CartridgeAgentConfiguration.getInstance().getServiceName().equals("keymanager")) {
                return true;
            }
        } else {

            if (CartridgeAgentConfiguration.getInstance().getDeployment() != null) {
                // check if deployment is Manager Worker separated
                if (CartridgeAgentConfiguration.getInstance().getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_MANAGER) ||
                        CartridgeAgentConfiguration.getInstance().getDeployment().equalsIgnoreCase(CartridgeAgentConstants.DEPLOYMENT_WORKER)) {

                    log.info("Deployment pattern for the node: " + CartridgeAgentConfiguration.getInstance().getDeployment());
                    envParameters.put("DEPLOYMENT", CartridgeAgentConfiguration.getInstance().getDeployment());
                    // check if WKA members of Manager Worker separated deployment is ready
                    return isManagerWorkerWKAGroupReady(envParameters);
                }
            }

            String serviceNameInPayload = CartridgeAgentConfiguration.getInstance().getServiceName();
            String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
            Service service = topology.getService(serviceNameInPayload);
            Cluster cluster = service.getCluster(clusterIdInPayload);

            List<Member> wkMembers = new ArrayList<Member>();
            for (Member member : cluster.getMembers()) {
                if (member.getProperties() != null &&
                        member.getProperties().containsKey("PRIMARY") &&
                        member.getProperties().getProperty("PRIMARY").toLowerCase().equals("true") &&
                        (member.getStatus().equals(MemberStatus.Starting) || member.getStatus().equals(MemberStatus.Active))
                        ) {
                    wkMembers.add(member);
                    this.wkMembers.add(member);
                    if (log.isDebugEnabled()) {
                        log.debug("Found WKA: STRATOS_WK_MEMBER_IP: " + member.getDefaultPrivateIP());
                    }
                }
            }
            if (wkMembers.size() >= minCount) {
                int idx = 0;
                for (Member member : wkMembers) {
                    envParameters.put("STRATOS_WK_MEMBER_" + idx + "_IP", member.getDefaultPrivateIP());
                    if (log.isDebugEnabled()) {
                        log.debug("STRATOS_WK_MEMBER_" + idx + "_IP: " + member.getDefaultPrivateIP());
                    }
                    idx++;
                }
                return true;
            }
        }
        return false;
    }

    // generic worker manager separated clustering logic
    private boolean isManagerWorkerWKAGroupReady(Map<String, String> envParameters) {

        // for this, we need both manager cluster service name and worker cluster service name
        String managerServiceName = CartridgeAgentConfiguration.getInstance().getManagerServiceName();
        String workerServiceName = CartridgeAgentConfiguration.getInstance().getWorkerServiceName();

        // managerServiceName and workerServiceName both should not be null /empty
        if (managerServiceName == null || managerServiceName.isEmpty()) {
            log.error("Manager service name [ " + managerServiceName + " ] is invalid");
            return false;
        }
        if (workerServiceName == null || workerServiceName.isEmpty()) {
            log.error("Worker service name [ " + workerServiceName + " ] is invalid");
            return false;
        }

        boolean minManagerInstancesAvailable = false;
        boolean minWorkerInstancesAvailable = false;

        TopologyManager.acquireReadLock();

        try {
            Service managerService = TopologyManager.getTopology().getService(managerServiceName);
            Service workerService = TopologyManager.getTopology().getService(workerServiceName);

            if (managerService == null) {
                log.warn("Service [ " + managerServiceName + " ] is not found");
                return false;
            }

            if (workerService == null) {
                log.warn("Service [ " + workerServiceName + " ] is not found");
                return false;
            }

            // manager clusters
            Collection<Cluster> managerClusters = managerService.getClusters();
            if (managerClusters == null || managerClusters.isEmpty()) {
                log.warn("No clusters found for service [ " + managerServiceName + " ]");
                return false;
            }

            int managerMinInstanceCount = 1;
            boolean managerMinInstanceCountFound = false;

            List<Member> managerWkaMembers = new ArrayList<Member>();
            for (Member member : managerClusters.iterator().next().getMembers()) {

                if (member.getProperties() != null && member.getProperties().containsKey("PRIMARY") &&
                        member.getProperties().getProperty("PRIMARY").toLowerCase().equals("true") &&
                        (member.getStatus().equals(MemberStatus.Starting) || member.getStatus().equals(MemberStatus.Active))) {

                    managerWkaMembers.add(member);
                    this.wkMembers.add(member);

                    // get the min instance count
                    if (!managerMinInstanceCountFound) {
                        managerMinInstanceCount = getMinInstanceCountFromMemberProperties(member);
                        managerMinInstanceCountFound = true;
                        log.info("Manager min instance count: " + managerMinInstanceCount);

                    }
                }
            }

            if (managerWkaMembers.size() >= managerMinInstanceCount) {
                minManagerInstancesAvailable = true;
                int idx = 0;
                for (Member member : managerWkaMembers) {
                    envParameters.put("STRATOS_WK_MANAGER_MEMBER_" + idx + "_IP", member.getDefaultPrivateIP());
                    if (log.isDebugEnabled()) {
                        log.debug("STRATOS_WK_MANAGER_MEMBER_" + idx + "_IP: " + member.getDefaultPrivateIP());
                    }
                    idx++;
                }

                envParameters.put("STRATOS_WK_MANAGER_MEMBER_COUNT", Integer.toString(managerMinInstanceCount));
            }

            // If all the manager members are non primary and is greate than or equal to mincount, 
            // minManagerInstancesAvailable should be true
            boolean allManagersNonPrimary = true;
            for (Member member : managerClusters.iterator().next().getMembers()) {

                // get the min instance count
                if (!managerMinInstanceCountFound) {
                    managerMinInstanceCount = getMinInstanceCountFromMemberProperties(member);
                    managerMinInstanceCountFound = true;
                    log.info("Manager min instance count when allManagersNonPrimary true : " + managerMinInstanceCount);
                }

                if (member.getProperties() != null && member.getProperties().containsKey("PRIMARY") &&
                        member.getProperties().getProperty("PRIMARY").toLowerCase().equals("true")) {
                    allManagersNonPrimary = false;
                    break;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(" allManagerNonPrimary & managerMinInstanceCount ["
                        + allManagersNonPrimary + "], [" + managerMinInstanceCount + "] ");
            }
            if (allManagersNonPrimary && managerClusters.size() >= managerMinInstanceCount) {
                minManagerInstancesAvailable = true;
            }

            // worker cluster
            Collection<Cluster> workerClusters = workerService.getClusters();
            if (workerClusters == null || workerClusters.isEmpty()) {
                log.warn("No clusters found for service [ " + workerServiceName + " ]");
                return false;
            }

            int workerMinInstanceCount = 1;
            boolean workerMinInstanceCountFound = false;

            List<Member> workerWkaMembers = new ArrayList<Member>();
            for (Member member : workerClusters.iterator().next().getMembers()) {
                if (log.isDebugEnabled()) {
                    log.debug("Checking member : " + member.getMemberId());
                }
                if (member.getProperties() != null &&
                        member.getProperties().containsKey("PRIMARY") &&
                        member.getProperties().getProperty("PRIMARY").toLowerCase().equals("true") &&
                        (member.getStatus().equals(MemberStatus.Starting) || member.getStatus().equals(MemberStatus.Active))) {
                    if (log.isDebugEnabled()) {
                        log.debug("Added worker member " + member.getMemberId());
                    }
                    workerWkaMembers.add(member);
                    this.wkMembers.add(member);

                    // get the min instance count
                    if (!workerMinInstanceCountFound) {
                        workerMinInstanceCount = getMinInstanceCountFromMemberProperties(member);
                        workerMinInstanceCountFound = true;
                        if (log.isDebugEnabled()) {
                            log.debug("Worker min instance count: " + workerMinInstanceCount);
                        }
                    }
                }
            }

            if (workerWkaMembers.size() >= workerMinInstanceCount) {
                minWorkerInstancesAvailable = true;
                int idx = 0;
                for (Member member : workerWkaMembers) {
                    envParameters.put("STRATOS_WK_WORKER_MEMBER_" + idx + "_IP", member.getDefaultPrivateIP());
                    if (log.isDebugEnabled()) {
                        log.debug("STRATOS_WK_WORKER_MEMBER_" + idx + "_IP: " + member.getDefaultPrivateIP());
                    }
                    idx++;
                }

                envParameters.put("STRATOS_WK_WORKER_MEMBER_COUNT", Integer.toString(workerMinInstanceCount));
            }

        } finally {
            TopologyManager.releaseReadLock();
        }

        if (log.isDebugEnabled()) {
            log.debug(" Returnning values minManagerInstancesAvailable && minWorkerInstancesAvailable [" +
                    minManagerInstancesAvailable + "],  [" + minWorkerInstancesAvailable + "] ");
        }
        return (minManagerInstancesAvailable && minWorkerInstancesAvailable);
    }


    private int getMinInstanceCountFromMemberProperties(Member member) {

        // default value is 1
        int minInstanceCount = 1;

        if (member.getProperties().containsKey("MIN_COUNT")) {
            minInstanceCount = Integer.parseInt(member.getProperties().getProperty("MIN_COUNT"));
        }

        return minInstanceCount;
    }

    private void waitForWKMembers(Map<String, String> envParameters) {
        int minCount = Integer.parseInt(CartridgeAgentConfiguration.getInstance().getMinCount());
        boolean isWKMemberGroupReady = false;
        while (!isWKMemberGroupReady) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Waiting for %d well known members...", minCount));
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }

            TopologyManager.acquireReadLock();
            isWKMemberGroupReady = isWKMemberGroupReady(envParameters, minCount);
            TopologyManager.releaseReadLock();
        }
    }

    @Override
    public void startServerExtension() {

        // wait until complete topology message is received to get LB IP
        ExtensionUtils.waitForCompleteTopology();
        if (log.isInfoEnabled()) {
            log.info("[start server extension] complete topology event received");
        }
        String serviceNameInPayload = CartridgeAgentConfiguration.getInstance().getServiceName();
        String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
        String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();

        try {
            TopologyManager.acquireReadLock();
            boolean isConsistent = ExtensionUtils.checkTopologyConsistency(serviceNameInPayload,
                    clusterIdInPayload, memberIdInPayload);
            if (!isConsistent) {
                if (log.isErrorEnabled()) {
                    log.error("Topology is inconsistent...failed to execute start server event");
                }
                return;
            }
            Topology topology = TopologyManager.getTopology();
            Service service = topology.getService(serviceNameInPayload);
            Cluster cluster = service.getCluster(clusterIdInPayload);

            // store environment variable parameters to be passed to extension shell script
            Map<String, String> env = new HashMap<String, String>();

            // if clustering is enabled wait until all well known members have started
            String flagClustering = CartridgeAgentConfiguration.getInstance().getIsClustered();
            if (flagClustering != null && flagClustering.toLowerCase().equals("true")) {
                env.put("STRATOS_CLUSTERING", "true");
                env.put("STRATOS_WK_MEMBER_COUNT", CartridgeAgentConfiguration.getInstance().getMinCount());
                if (CartridgeAgentConfiguration.getInstance().getIsPrimary().toLowerCase().equals("true")) {
                    env.put("STRATOS_PRIMARY", "true");
                } else {
                    env.put("STRATOS_PRIMARY", "false");
                }
                TopologyManager.releaseReadLock();
                waitForWKMembers(env);
                if (log.isInfoEnabled()) {
                    log.info(String.format("All well known members have started! Resuming start server extension..."));
                }
                TopologyManager.acquireReadLock();
            }

            env.put("STRATOS_TOPOLOGY_JSON", gson.toJson(topology.getServices(), serviceType));
            env.put("STRATOS_MEMBER_LIST_JSON", gson.toJson(cluster.getMembers(), memberType));
            ExtensionUtils.executeStartServersExtension(env);
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Error processing start servers event", e);
            }
        } finally {
            TopologyManager.releaseReadLock();
        }
    }

    @Override
    public void volumeMountExtension(String persistenceMappingsPayload) {
        ExtensionUtils.executeVolumeMountExtension(persistenceMappingsPayload);
    }

    @Override
    public void onSubscriptionDomainAddedEvent(DomainMappingAddedEvent subscriptionDomainAddedEvent) {
        String tenantDomain = findTenantDomain(subscriptionDomainAddedEvent.getTenantId());
        if (log.isInfoEnabled()) {
            log.info(String.format("Subscription domain added event received: [tenant-id] %d [tenant-domain] %s " +
                            "[domain-name] %s [application-context] %s",
                    subscriptionDomainAddedEvent.getTenantId(),
                    tenantDomain,
                    subscriptionDomainAddedEvent.getDomainName(),
                    subscriptionDomainAddedEvent.getContextPath()
            ));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(subscriptionDomainAddedEvent);
            log.debug("Subscription domain added event msg:" + msg);
        }

        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_SUBSCRIPTION_SERVICE_NAME", subscriptionDomainAddedEvent.getServiceName());
        env.put("STRATOS_SUBSCRIPTION_DOMAIN_NAME", subscriptionDomainAddedEvent.getDomainName());
        env.put("STRATOS_SUBSCRIPTION_TENANT_ID", Integer.toString(subscriptionDomainAddedEvent.getTenantId()));
        env.put("STRATOS_SUBSCRIPTION_TENANT_DOMAIN", tenantDomain);
        env.put("STRATOS_SUBSCRIPTION_APPLICATION_CONTEXT", subscriptionDomainAddedEvent.getContextPath());
        ExtensionUtils.executeSubscriptionDomainAddedExtension(env);
    }

    private String findTenantDomain(int tenantId) {
        try {
            TenantManager.acquireReadLock();
            Tenant tenant = TenantManager.getInstance().getTenant(tenantId);
            if (tenant == null) {
                throw new RuntimeException(String.format("Tenant could not be found: [tenant-id] %d", tenantId));
            }
            return tenant.getTenantDomain();
        } finally {
            TenantManager.releaseReadLock();
        }
    }

    @Override
    public void onSubscriptionDomainRemovedEvent(DomainMappingRemovedEvent subscriptionDomainRemovedEvent) {
        String tenantDomain = findTenantDomain(subscriptionDomainRemovedEvent.getTenantId());
        if (log.isInfoEnabled()) {
            log.info(String.format("Subscription domain removed event received: [tenant-id] %d [tenant-domain] %s " +
                            "[domain-name] %s",
                    subscriptionDomainRemovedEvent.getTenantId(),
                    tenantDomain,
                    subscriptionDomainRemovedEvent.getDomainName()
            ));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(subscriptionDomainRemovedEvent);
            log.debug("Subscription domain removed event msg:" + msg);
        }

        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_SUBSCRIPTION_SERVICE_NAME", subscriptionDomainRemovedEvent.getServiceName());
        env.put("STRATOS_SUBSCRIPTION_DOMAIN_NAME", subscriptionDomainRemovedEvent.getDomainName());
        env.put("STRATOS_SUBSCRIPTION_TENANT_ID", Integer.toString(subscriptionDomainRemovedEvent.getTenantId()));
        env.put("STRATOS_SUBSCRIPTION_TENANT_DOMAIN", tenantDomain);
        ExtensionUtils.executeSubscriptionDomainRemovedExtension(env);
    }

    @Override
    public void onCopyArtifactsExtension(String src, String des) {
        ExtensionUtils.executeCopyArtifactsExtension(src, des);
    }

    @Override
    public void onTenantSubscribedEvent(TenantSubscribedEvent tenantSubscribedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Tenant subscribed event received: [tenant] %s [service] %s [cluster] %s",
                            tenantSubscribedEvent.getTenantId(), tenantSubscribedEvent.getServiceName(),
                            tenantSubscribedEvent.getClusterIds())
            );
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(tenantSubscribedEvent);
            log.debug("Tenant subscribed event msg:" + msg);
        }
        Map<String, String> env = new HashMap<String, String>();
        ExtensionUtils.executeTenantSubscribedExtension(env);
    }

    @Override
    public void onTenantUnSubscribedEvent(TenantUnSubscribedEvent tenantUnSubscribedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("Tenant unsubscribed event received: [tenant] %s [service] %s [cluster] %s",
                    tenantUnSubscribedEvent.getTenantId(), tenantUnSubscribedEvent.getServiceName(),
                    tenantUnSubscribedEvent.getClusterIds()));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(tenantUnSubscribedEvent);
            log.debug("Tenant unsubscribed event msg:" + msg);
        }

        try {
            if (CartridgeAgentConfiguration.getInstance().getServiceName().equals(tenantUnSubscribedEvent.getServiceName())) {
                GitBasedArtifactRepository.getInstance().removeRepo(tenantUnSubscribedEvent.getTenantId());
            }
        } catch (Exception e) {
            log.error(e);
        }
        Map<String, String> env = new HashMap<String, String>();
        ExtensionUtils.executeTenantUnSubscribedExtension(env);
    }

}