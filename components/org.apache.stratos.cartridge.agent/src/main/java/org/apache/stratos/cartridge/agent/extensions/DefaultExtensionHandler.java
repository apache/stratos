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
import org.apache.stratos.messaging.event.application.signup.ApplicationSignUpRemovedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingAddedEvent;
import org.apache.stratos.messaging.event.domain.mapping.DomainMappingRemovedEvent;
import org.apache.stratos.messaging.event.tenant.TenantSubscribedEvent;
import org.apache.stratos.messaging.event.tenant.TenantUnSubscribedEvent;
import org.apache.stratos.messaging.event.topology.*;
import org.apache.stratos.messaging.message.receiver.tenant.TenantManager;

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

                // Execute instance activated shell script
                ExtensionUtils.executeInstanceActivatedExtension();
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

        boolean memberInitialized = ExtensionUtils.checkTopologyConsistency(memberActivatedEvent.getServiceName(),
                memberActivatedEvent.getClusterId(), memberActivatedEvent.getMemberId());
        if (!memberInitialized) {
            if (log.isErrorEnabled()) {
                log.error("Member has not initialized. Failed to execute member activated event");
            }
            return;
        }
        
        Map<String, String> env = new HashMap<String, String>();

        ExtensionUtils.executeMemberActivatedExtension(env);
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
    public void onMemberInitializedEvent(MemberInitializedEvent memberInitializedEvent) {
        if (log.isDebugEnabled()) {
            log.debug("Member initialized event received");
        }
        String serviceNameInPayload = CartridgeAgentConfiguration.getInstance().getServiceName();
        String clusterIdInPayload = CartridgeAgentConfiguration.getInstance().getClusterId();
        String memberIdInPayload = CartridgeAgentConfiguration.getInstance().getMemberId();

        Member activatedMember = ExtensionUtils.getMemberFromTopology(serviceNameInPayload, clusterIdInPayload, memberIdInPayload);
        if (activatedMember == null) {
            // if this member isn't there in the complete topology
        	if (log.isDebugEnabled()) {
                log.debug("Member does not exist in topology, or not in initialized state.");
            }
        } else {
        	if (log.isDebugEnabled()) {
                log.debug("Member has initialized, topology is consistent, agent is initialized");
            }

            CartridgeAgentConfiguration.getInstance().setInitialized(true);
        }
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

        boolean memberInitialized = ExtensionUtils.checkTopologyConsistency(serviceNameInPayload, clusterIdInPayload, memberIdInPayload);
        if (memberInitialized) {
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

        boolean memberInitialized = ExtensionUtils.checkTopologyConsistency(memberTerminatedEvent.getServiceName(),
                memberTerminatedEvent.getClusterId(), memberTerminatedEvent.getMemberId());
        if (!memberInitialized) {
            if (log.isErrorEnabled()) {
                log.error("Member has not initialized. Failed to execute member terminated event");
            }
            return;
        }
        
        Map<String, String> env = new HashMap<String, String>();

        ExtensionUtils.executeMemberTerminatedExtension(env);
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

        boolean memberInitialized = ExtensionUtils.checkTopologyConsistency(memberSuspendedEvent.getServiceName(),
                memberSuspendedEvent.getClusterId(), memberSuspendedEvent.getMemberId());
        if (!memberInitialized) {
            if (log.isErrorEnabled()) {
                log.error("Member has not initialized. Failed to execute member suspended event");
            }
            return;
        }
        
        Map<String, String> env = new HashMap<String, String>();

        ExtensionUtils.executeMemberSuspendedExtension(env);
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

        boolean memberInitialized = ExtensionUtils.checkTopologyConsistency(memberStartedEvent.getServiceName(),
                memberStartedEvent.getClusterId(), memberStartedEvent.getMemberId());
        if (!memberInitialized) {
            if (log.isErrorEnabled()) {
                log.error("Member has not initialized. Failed to execute member started event");
            }
            return;
        }
        
        Map<String, String> env = new HashMap<String, String>();

        ExtensionUtils.executeMemberStartedExtension(env);
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

        boolean isConsistent = ExtensionUtils.checkTopologyConsistency(serviceNameInPayload,
                clusterIdInPayload, memberIdInPayload);
        if (!isConsistent) {
            if (log.isErrorEnabled()) {
                log.error("Topology is inconsistent...Failed to execute start server event");
            }
            return;
        }
        
        Map<String, String> env = new HashMap<String, String>();

        ExtensionUtils.executeStartServersExtension(env);
    }

    @Override
    public void volumeMountExtension(String persistenceMappingsPayload) {
        ExtensionUtils.executeVolumeMountExtension(persistenceMappingsPayload);
    }

    @Override
    public void onDomainMappingAddedEvent(DomainMappingAddedEvent domainMappingAddedEvent) {
        String tenantDomain = findTenantDomain(domainMappingAddedEvent.getTenantId());
        if (log.isInfoEnabled()) {
            log.info(String.format("Domain mapping added event received: [tenant-id] %d [tenant-domain] %s " +
                            "[domain-name] %s [application-context] %s",
                    domainMappingAddedEvent.getTenantId(),
                    tenantDomain,
                    domainMappingAddedEvent.getDomainName(),
                    domainMappingAddedEvent.getContextPath()
            ));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(domainMappingAddedEvent);
            log.debug("Domain mapping added event msg:" + msg);
        }

        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_SUBSCRIPTION_APPLICATION_ID", domainMappingAddedEvent.getApplicationId());
        env.put("STRATOS_SUBSCRIPTION_SERVICE_NAME", domainMappingAddedEvent.getServiceName());
        env.put("STRATOS_SUBSCRIPTION_DOMAIN_NAME", domainMappingAddedEvent.getDomainName());
        env.put("STRATOS_SUBSCRIPTION_CLUSTER_ID", domainMappingAddedEvent.getClusterId());
        env.put("STRATOS_SUBSCRIPTION_TENANT_ID", Integer.toString(domainMappingAddedEvent.getTenantId()));
        env.put("STRATOS_SUBSCRIPTION_TENANT_DOMAIN", tenantDomain);
        env.put("STRATOS_SUBSCRIPTION_CONTEXT_PATH", domainMappingAddedEvent.getContextPath());
        ExtensionUtils.executeDomainMappingAddedExtension(env);
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
    public void onDomainMappingRemovedEvent(DomainMappingRemovedEvent subscriptionDomainRemovedEvent) {
        String tenantDomain = findTenantDomain(subscriptionDomainRemovedEvent.getTenantId());
        if (log.isInfoEnabled()) {
            log.info(String.format("Domain mapping removed event received: [tenant-id] %d [tenant-domain] %s " +
                            "[domain-name] %s",
                    subscriptionDomainRemovedEvent.getTenantId(),
                    tenantDomain,
                    subscriptionDomainRemovedEvent.getDomainName()
            ));
        }

        if (log.isDebugEnabled()) {
            String msg = gson.toJson(subscriptionDomainRemovedEvent);
            log.debug("Domain mapping removed event msg:" + msg);
        }

        Map<String, String> env = new HashMap<String, String>();
        env.put("STRATOS_SUBSCRIPTION_APPLICATION_ID", subscriptionDomainRemovedEvent.getApplicationId());
        env.put("STRATOS_SUBSCRIPTION_SERVICE_NAME", subscriptionDomainRemovedEvent.getServiceName());
        env.put("STRATOS_SUBSCRIPTION_DOMAIN_NAME", subscriptionDomainRemovedEvent.getDomainName());
        env.put("STRATOS_SUBSCRIPTION_CLUSTER_ID", subscriptionDomainRemovedEvent.getClusterId());
        env.put("STRATOS_SUBSCRIPTION_TENANT_ID", Integer.toString(subscriptionDomainRemovedEvent.getTenantId()));
        env.put("STRATOS_SUBSCRIPTION_TENANT_DOMAIN", tenantDomain);
        ExtensionUtils.executeDomainMappingRemovedExtension(env);
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
    
    //ApplicationSignUpRemovedEvent
    @Override
    public void onApplicationSignUpRemovedEvent(ApplicationSignUpRemovedEvent applicationSignUpRemovedEvent) {
        if (log.isInfoEnabled()) {
            log.info(String.format("applicationSignUpRemovedEvent event received: [appId] %s [tenantId] %s ",
            		applicationSignUpRemovedEvent.getApplicationId(), applicationSignUpRemovedEvent.getTenantId()));
        }

        try {
            if (CartridgeAgentConfiguration.getInstance().getApplicationId().equals(applicationSignUpRemovedEvent.getApplicationId())) {
                GitBasedArtifactRepository.getInstance().removeRepo(applicationSignUpRemovedEvent.getTenantId());
            }
        } catch (Exception e) {
            log.error(e);
        }
        
    }

}