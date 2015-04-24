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
package org.apache.stratos.autoscaler.services.impl;

import org.apache.axis2.AxisFault;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.algorithms.networkpartition.NetworkPartitionAlgorithmContext;
import org.apache.stratos.autoscaler.applications.ApplicationHolder;
import org.apache.stratos.autoscaler.applications.parser.ApplicationParser;
import org.apache.stratos.autoscaler.applications.parser.DefaultApplicationParser;
import org.apache.stratos.autoscaler.applications.pojo.*;
import org.apache.stratos.autoscaler.applications.topic.ApplicationBuilder;
import org.apache.stratos.autoscaler.context.AutoscalerContext;
import org.apache.stratos.autoscaler.context.InstanceContext;
import org.apache.stratos.autoscaler.context.cluster.ClusterInstanceContext;
import org.apache.stratos.autoscaler.context.partition.ClusterLevelPartitionContext;
import org.apache.stratos.autoscaler.context.partition.network.ClusterLevelNetworkPartitionContext;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.application.InvalidApplicationPolicyException;
import org.apache.stratos.autoscaler.exception.application.InvalidServiceGroupException;
import org.apache.stratos.autoscaler.exception.policy.*;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.monitor.component.ApplicationMonitor;
import org.apache.stratos.autoscaler.pojo.Dependencies;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.DeploymentPolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.services.AutoscalerService;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidCartridgeTypeExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidPartitionExceptionException;
import org.apache.stratos.cloud.controller.stub.domain.MemberContext;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.client.CloudControllerServiceClient;
import org.apache.stratos.common.client.StratosManagerServiceClient;
import org.apache.stratos.common.constants.StratosConstants;
import org.apache.stratos.common.partition.NetworkPartition;
import org.apache.stratos.common.partition.Partition;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.service.stub.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.service.stub.domain.application.signup.ArtifactRepository;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.wso2.carbon.registry.api.RegistryException;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.*;

/**
 * Auto Scaler Service API is responsible getting Partitions and Policies.
 */
public class AutoscalerServiceImpl implements AutoscalerService {

    private static final Log log = LogFactory.getLog(AutoscalerServiceImpl.class);

    public AutoscalePolicy[] getAutoScalingPolicies() {
        return PolicyManager.getInstance().getAutoscalePolicyList();
    }

    @Override
    public boolean addAutoScalingPolicy(AutoscalePolicy autoscalePolicy) throws InvalidPolicyException {
        return PolicyManager.getInstance().addAutoscalePolicy(autoscalePolicy);
    }

    @Override
    public boolean updateAutoScalingPolicy(AutoscalePolicy autoscalePolicy) throws InvalidPolicyException {
        return PolicyManager.getInstance().updateAutoscalePolicy(autoscalePolicy);
    }

    @Override
    public boolean removeAutoScalingPolicy(String autoscalePolicyId) throws InvalidPolicyException {
        if (validateAutoScalerPolicy(autoscalePolicyId)) {
            return PolicyManager.getInstance().removeAutoscalePolicy(autoscalePolicyId);
        } else {
            throw new InvalidPolicyException("This autoscaler policy cannot be removed, since it is used in applications.");
        }
    }

    /**
     * Validate the Auto Scalar policy removal
     *
     * @param autoscalePolicyId Auto Scalar policy id boolean
     * @return
     */
    private boolean validateAutoScalerPolicy(String autoscalePolicyId) {
        boolean canRemove = true;
        Collection<ApplicationContext> appContexts = AutoscalerContext.getInstance().getApplicationContexts();
        for (ApplicationContext app : appContexts) {
            CartridgeContext[] cartrideContexts = app.getComponents().getCartridgeContexts();
            for (CartridgeContext cartridgeContext : cartrideContexts) {
                SubscribableInfoContext subscribableInfoContexts = cartridgeContext.getSubscribableInfoContext();
                if (subscribableInfoContexts.getAutoscalingPolicy().equals(autoscalePolicyId)) {
                    canRemove = false;
                }
            }
        }
        return canRemove;
    }

    @Override
    public AutoscalePolicy getAutoscalingPolicy(String autoscalingPolicyId) {
        return PolicyManager.getInstance().getAutoscalePolicy(autoscalingPolicyId);
    }

    @Override
    public void addApplication(ApplicationContext applicationContext)
            throws ApplicationDefinitionException {

        if (log.isInfoEnabled()) {
            log.info(String.format("Adding application: [application-id] %s",
                    applicationContext.getApplicationId()));
        }

        ApplicationParser applicationParser = new DefaultApplicationParser();
        Application application = applicationParser.parse(applicationContext);
        ApplicationHolder.persistApplication(application);

        List<ApplicationClusterContext> applicationClusterContexts = applicationParser.getApplicationClusterContexts();
        ApplicationClusterContext[] applicationClusterContextsArray = applicationClusterContexts.toArray(
                new ApplicationClusterContext[applicationClusterContexts.size()]);
        applicationContext.getComponents().setApplicationClusterContexts(applicationClusterContextsArray);

        applicationContext.setStatus(ApplicationContext.STATUS_CREATED);
        AutoscalerContext.getInstance().addApplicationContext(applicationContext);

        if (log.isInfoEnabled()) {
            log.info(String.format("Application added successfully: [application-id] %s",
                    applicationContext.getApplicationId()));
        }
    }

    @Override
    public void updateApplication(ApplicationContext applicationContext)
            throws ApplicationDefinitionException {

        String applicationId = applicationContext.getApplicationId();
        if (log.isInfoEnabled()) {
            log.info(String.format("Updating application: [application-id] %s",
                    applicationContext.getApplicationId()));
        }

        if (AutoscalerContext.getInstance().getApplicationContext(applicationId) == null) {
            String msg = "Application is not found as ApplicationContext. Please add application before updating it";
            log.error(msg);
            throw new ApplicationDefinitionException(msg);
        }

        if (ApplicationHolder.getApplications().getApplication(applicationId) == null) {
            String msg = "Application is not found as Application. Please add application before updating it";
            log.error(msg);
            throw new ApplicationDefinitionException(msg);
        }


        ApplicationParser applicationParser = new DefaultApplicationParser();
        Application application = applicationParser.parse(applicationContext);

        //Need to update the application
        AutoscalerUtil.getInstance().updateApplicationsTopology(application);

        //Update the clusterMonitors
        AutoscalerUtil.getInstance().updateClusterMonitor(application);

        List<ApplicationClusterContext> applicationClusterContexts = applicationParser.getApplicationClusterContexts();
        ApplicationClusterContext[] applicationClusterContextsArray = applicationClusterContexts.toArray(
                new ApplicationClusterContext[applicationClusterContexts.size()]);
        applicationContext.getComponents().setApplicationClusterContexts(applicationClusterContextsArray);

        ApplicationContext existingApplicationContext = AutoscalerContext.getInstance().
                getApplicationContext(applicationId);
        applicationContext.setStatus(existingApplicationContext.getStatus());
        //updating the applicationContext
        AutoscalerContext.getInstance().updateApplicationContext(applicationContext);

        if (log.isInfoEnabled()) {
            log.info(String.format("Application added successfully: [application-id] %s",
                    applicationId));
        }
    }

    @Override
    public ApplicationContext getApplication(String applicationId) {
        return AutoscalerContext.getInstance().getApplicationContext(applicationId);
    }

    @Override
    public ApplicationContext[] getApplications() {
        return AutoscalerContext.getInstance().getApplicationContexts().
                toArray(new ApplicationContext[AutoscalerContext.getInstance().getApplicationContexts().size()]);
    }

    @Override
    public boolean deployApplication(String applicationId, String applicationPolicyId) throws ApplicationDefinitionException {
        try {
            Application application = ApplicationHolder.getApplications().getApplication(applicationId);
            if (application == null) {
                throw new RuntimeException("Application not found: " + applicationId);
            }

            ApplicationContext applicationContext = RegistryManager.getInstance().getApplicationContext(applicationId);
            if (applicationContext == null) {
                throw new RuntimeException("Application context not found: " + applicationId);
            }

            // Create application clusters in cloud controller and send application created event
            ApplicationBuilder.handleApplicationCreatedEvent(application, applicationContext.getComponents().getApplicationClusterContexts());

            // validating application policy against the application
            AutoscalerUtil.validateApplicationPolicyAgainstApplication(applicationId, applicationPolicyId);

            // Setting application policy id in application object
            try {
                ApplicationHolder.acquireWriteLock();
                application = ApplicationHolder.getApplications().getApplication(applicationId);
                application.setApplicationPolicyId(applicationPolicyId);
                ApplicationHolder.persistApplication(application);
            } finally {
                ApplicationHolder.releaseWriteLock();
            }

            // adding network partition algorithm context to registry
            ApplicationPolicy applicationPolicy = PolicyManager.getInstance().getApplicationPolicy(applicationPolicyId);
            NetworkPartitionAlgorithmContext algorithmContext = new NetworkPartitionAlgorithmContext(applicationId, applicationPolicy, 0);
            AutoscalerContext.getInstance().addNetworkPartitionAlgorithmContext(algorithmContext);

            if (!applicationContext.isMultiTenant()) {
                // Add application signup for single tenant applications
                addApplicationSignUp(applicationContext, application.getKey(), findApplicationClusterIds(application));
            }
            applicationContext.setStatus(ApplicationContext.STATUS_DEPLOYED);
            AutoscalerContext.getInstance().updateApplicationContext(applicationContext);

            log.info("Waiting for application clusters to be created: [application] " + applicationId);
            return true;
        } catch (Exception e) {
            ApplicationContext applicationContext = RegistryManager.getInstance().getApplicationContext(applicationId);
            if (applicationContext != null) {
                // Revert application status
                applicationContext.setStatus(ApplicationContext.STATUS_CREATED);
                AutoscalerContext.getInstance().updateApplicationContext(applicationContext);
            }
            String message = "Application deployment failed";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Find application cluster ids.
     *
     * @param application
     * @return
     */
    private List<String> findApplicationClusterIds(Application application) {
        List<String> clusterIds = new ArrayList<String>();
        for (ClusterDataHolder clusterDataHolder : application.getClusterDataRecursively()) {
            clusterIds.add(clusterDataHolder.getClusterId());
        }
        return clusterIds;
    }

    /**
     * Add application signup.
     *
     * @param applicationContext
     * @param applicationKey
     * @param clusterIds
     */
    private void addApplicationSignUp(ApplicationContext applicationContext, String applicationKey,
                                      List<String> clusterIds) {

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Adding application signup: [application-id] %s",
                        applicationContext.getApplicationId()));
            }

            ComponentContext components = applicationContext.getComponents();
            if (components != null) {
                ApplicationSignUp applicationSignUp = new ApplicationSignUp();
                applicationSignUp.setApplicationId(applicationContext.getApplicationId());
                applicationSignUp.setTenantId(applicationContext.getTenantId());
                String[] clusterIdsArray = clusterIds.toArray(new String[clusterIds.size()]);
                applicationSignUp.setClusterIds(clusterIdsArray);

                List<ArtifactRepository> artifactRepositoryList = new ArrayList<ArtifactRepository>();
                CartridgeContext[] cartridgeContexts = components.getCartridgeContexts();
                if (cartridgeContexts != null) {
                    updateArtifactRepositoryList(artifactRepositoryList, cartridgeContexts);
                }

                if (components.getGroupContexts() != null) {
                    CartridgeContext[] cartridgeContextsOfGroups = getCartridgeContextsOfGroupsRecursively(
                            components.getGroupContexts());
                    if (cartridgeContextsOfGroups != null) {
                        updateArtifactRepositoryList(artifactRepositoryList, cartridgeContextsOfGroups);
                    }
                }

                ArtifactRepository[] artifactRepositoryArray = artifactRepositoryList.toArray(
                        new ArtifactRepository[artifactRepositoryList.size()]);
                applicationSignUp.setArtifactRepositories(artifactRepositoryArray);

                // Encrypt artifact repository passwords
                encryptRepositoryPasswords(applicationSignUp, applicationKey);

                StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
                serviceClient.addApplicationSignUp(applicationSignUp);

                if (log.isInfoEnabled()) {
                    log.info(String.format("Application signup added successfully: [application-id] %s",
                            applicationContext.getApplicationId()));
                }
            }
        } catch (Exception e) {
            String message = "Could not add application signup";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }


    private CartridgeContext[] getCartridgeContextsOfGroupsRecursively(GroupContext[] passedGroupContexts) {

        List<CartridgeContext> cartridgeContextsList = new ArrayList<CartridgeContext>();

        for (GroupContext groupContext : passedGroupContexts) {
            if (groupContext.getCartridgeContexts() != null) {
                for (CartridgeContext cartridgeContext : groupContext.getCartridgeContexts()) {

                    cartridgeContextsList.add(cartridgeContext);
                }
            }
            if (groupContext.getGroupContexts() != null) {
                for (CartridgeContext cartridgeContext : getCartridgeContextsOfGroupsRecursively(groupContext.getGroupContexts())) {

                    cartridgeContextsList.add(cartridgeContext);
                }
            }
        }
        return cartridgeContextsList.toArray(new CartridgeContext[0]);
    }

    private void removeApplicationSignUp(ApplicationContext applicationContext) {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Removing application signup: [application-id] %s",
                        applicationContext.getApplicationId()));
            }

            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();

            ApplicationSignUp applicationSignUp[] = serviceClient.getApplicationSignUps(applicationContext.getApplicationId());
            if (applicationSignUp != null) {
                for (ApplicationSignUp appSignUp : applicationSignUp) {
                    if (appSignUp != null) {
                        serviceClient.removeApplicationSignUp(appSignUp.getApplicationId(), appSignUp.getTenantId());
                    }
                }
            }

        } catch (Exception e) {
            String message = "Could not remove application signup(s)";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Encrypt artifact repository passwords.
     *
     * @param applicationSignUp
     * @param applicationKey
     */
    private void encryptRepositoryPasswords(ApplicationSignUp applicationSignUp, String applicationKey) {
        if (applicationSignUp.getArtifactRepositories() != null) {
            for (ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                String repoPassword = artifactRepository.getRepoPassword();
                if ((artifactRepository != null) && (StringUtils.isNotBlank(repoPassword))) {
                    String encryptedRepoPassword = CommonUtil.encryptPassword(repoPassword,
                            applicationKey);
                    artifactRepository.setRepoPassword(encryptedRepoPassword);

                    if (log.isDebugEnabled()) {
                        log.debug(String.format("Artifact repository password encrypted: [application-id] %s " +
                                        "[tenant-id] %d [repo-url] %s", applicationSignUp.getApplicationId(),
                                applicationSignUp.getTenantId(), artifactRepository.getRepoUrl()));
                    }
                }
            }
        }
    }

    private void updateArtifactRepositoryList(List<ArtifactRepository> artifactRepositoryList, CartridgeContext[] cartridgeContexts) {

        if (cartridgeContexts == null) {
            return;
        }

        for (CartridgeContext cartridgeContext : cartridgeContexts) {
            SubscribableInfoContext subscribableInfoContext = cartridgeContext.getSubscribableInfoContext();
            ArtifactRepositoryContext artifactRepositoryContext = subscribableInfoContext.getArtifactRepositoryContext();
            if (artifactRepositoryContext != null) {

                ArtifactRepository artifactRepository = new ArtifactRepository();
                artifactRepository.setCartridgeType(cartridgeContext.getType());
                artifactRepository.setAlias(subscribableInfoContext.getAlias());
                artifactRepository.setRepoUrl(artifactRepositoryContext.getRepoUrl());
                artifactRepository.setPrivateRepo(artifactRepositoryContext.isPrivateRepo());
                artifactRepository.setRepoUsername(artifactRepositoryContext.getRepoUsername());
                artifactRepository.setRepoPassword(artifactRepositoryContext.getRepoPassword());

                artifactRepositoryList.add(artifactRepository);
            }
        }
    }

    public void undeployApplication(String applicationId, boolean force) {

        AutoscalerContext asCtx = AutoscalerContext.getInstance();
        ApplicationMonitor appMonitor = asCtx.getAppMonitor(applicationId);

        if (appMonitor == null) {
            log.info(String.format("Could not find application monitor for the application %s, hence returning", applicationId));
            return;
        }
        if (!force) {
            // Gracefull undeployment flow
            if (appMonitor.isTerminating()) {
                log.info("Application monitor is already in terminating, graceful undeployment is has already been attempted thus not invoking again");
                return;
            } else {
                log.info(String.format("Gracefully undeploying the application " + applicationId));
                undeployApplicationGracefully(applicationId);
            }
        } else {
            // force undeployment flow
            if (appMonitor.isTerminating()) {

                if (appMonitor.isForce()) {
                    log.warn("Force undeployment is already in progress, hence not invoking again");
                    return;
                } else {
                    log.info(String.format("Previous gracefull undeployment is in progress for [application-id] %s , thus  terminating instances directly", applicationId));
                    appMonitor.setForce(true);
                    terminateAllApplicationMembersForcefully(applicationId);
                }
            } else {
                log.info(String.format("Forcefully undeploying the application " + applicationId));
                appMonitor.setForce(true);
                undeployApplicationGracefully(applicationId);
            }
        }

    }

    private void undeployApplicationGracefully(String applicationId) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Starting to undeploy application: [application-id] " + applicationId);
            }

            ApplicationContext applicationContext = AutoscalerContext.getInstance().getApplicationContext(applicationId);
            Application application = ApplicationHolder.getApplications().getApplication(applicationId);
            if ((applicationContext == null) || (application == null)) {
                String msg = String.format("Application not found: [application-id] %s", applicationId);
                throw new RuntimeException(msg);
            }

            if (!applicationContext.getStatus().equals(ApplicationContext.STATUS_DEPLOYED)) {
                String message = String.format("Application is not deployed: [application-id] %s", applicationId);
                log.error(message);
                throw new RuntimeException(message);
            }

            // Remove application signup(s) in stratos manager
            removeApplicationSignUp(applicationContext);

            // Remove network partition algorithm context
            AutoscalerContext.getInstance().removeNetworkPartitionAlgorithmContext(applicationId);

            ApplicationBuilder.handleApplicationUnDeployedEvent(applicationId);

            if (log.isInfoEnabled()) {
                log.info("Application undeployment process started: [application-id] " + applicationId);
            }
        } catch (Exception e) {
            String message = "Could not start application undeployment process: [application-id] " + applicationId;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public void deleteApplication(String applicationId) {
        try {
            ApplicationContext applicationContext = AutoscalerContext.getInstance().getApplicationContext(applicationId);
            Application application = ApplicationHolder.getApplications().getApplication(applicationId);
            if ((applicationContext == null) || (application == null)) {
                String msg = String.format("Application cannot be deleted, application not found: [application-id] %s",
                        applicationId);
                throw new RuntimeException(msg);
            }


            if (ApplicationContext.STATUS_DEPLOYED.equals(applicationContext.getStatus())) {
                String msg = String.format("Application is in deployed state, please undeploy it before deleting: " +
                        "[application-id] %s", applicationId);
                throw new AutoScalerException(msg);
            }

            if (application.getInstanceContextCount() > 0) {
                String message = String.format("Application cannot be deleted, undeployment process is still in " +
                        "progress: [application-id] %s", applicationId);
                log.error(message);
                throw new RuntimeException(message);
            }

            ApplicationBuilder.handleApplicationRemoval(applicationId);
            log.info(String.format("Application deleted successfully: [application-id] %s", applicationId));
        } catch (Exception e) {
            String message = String.format("Could not delete application: [application-id] %s", applicationId);
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public void updateClusterMonitor(String clusterId, Properties properties) throws InvalidArgumentException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("Updating Cluster monitor [Cluster id] %s ", clusterId));
        }
        AutoscalerContext asCtx = AutoscalerContext.getInstance();
        ClusterMonitor monitor = asCtx.getClusterMonitor(clusterId);

        if (monitor != null) {
            monitor.handleDynamicUpdates(properties);
        } else {
            log.debug(String.format("Updating Cluster monitor failed: Cluster monitor [Cluster id] %s not found.",
                    clusterId));
        }
    }

    public void addServiceGroup(ServiceGroup servicegroup) throws InvalidServiceGroupException {

        if (servicegroup == null || StringUtils.isEmpty(servicegroup.getName())) {
            String msg = "Cartridge group can not be null service name can not be empty.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Adding cartridge group: [group-name] %s", servicegroup.getName()));
        }

        String groupName = servicegroup.getName();
        if (RegistryManager.getInstance().serviceGroupExist(groupName)) {
            throw new InvalidServiceGroupException("Cartridge group with the name " + groupName + " already exists.");
        }

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("Adding cartridge group {0}", servicegroup.getName()));
        }

        String[] subGroups = servicegroup.getCartridges();
        if (log.isDebugEnabled()) {
            log.debug("SubGroups" + Arrays.toString(subGroups));
            if (subGroups != null) {
                log.debug("subGroups:size" + subGroups.length);
            } else {
                log.debug("subGroups: are null");
            }
        }

        Dependencies dependencies = servicegroup.getDependencies();
        if (log.isDebugEnabled()) {
            log.debug("Dependencies" + dependencies);
        }

        if (dependencies != null) {
            String[] startupOrders = dependencies.getStartupOrders();

            if (log.isDebugEnabled()) {
                log.debug("StartupOrders " + Arrays.toString(startupOrders));

                if (startupOrders != null) {
                    log.debug("StartupOrder:size  " + startupOrders.length);
                } else {
                    log.debug("StartupOrder: is null");
                }
            }

            String[] scalingDependents = dependencies.getScalingDependants();

            if (log.isDebugEnabled()) {
                log.debug("ScalingDependent " + Arrays.toString(scalingDependents));

                if (scalingDependents != null) {
                    log.debug("ScalingDependents:size " + scalingDependents.length);
                } else {
                    log.debug("ScalingDependent: is null");
                }
            }
        }

        RegistryManager.getInstance().persistServiceGroup(servicegroup);
        if (log.isInfoEnabled()) {
            log.info(String.format("Cartridge group successfully added: [group-name] %s", servicegroup.getName()));
        }
    }

    @Override
    public void removeServiceGroup(String groupName) {
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Starting to remove cartridge group: [group-name] %s", groupName));
            }
            if (RegistryManager.getInstance().serviceGroupExist(groupName)) {
                RegistryManager.getInstance().removeServiceGroup(groupName);
                if (log.isInfoEnabled()) {
                    log.info(String.format("Cartridge group removed: [group-name] %s", groupName));
                }
            } else {
                if (log.isWarnEnabled()) {
                    log.warn(String.format("Cartridge group not found: [group-name] %s", groupName));
                }
            }
        } catch (org.wso2.carbon.registry.core.exceptions.RegistryException e) {
            String message = "Could not remove cartridge group: " + groupName;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    public ServiceGroup getServiceGroup(String name) {
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        try {
            return RegistryManager.getInstance().getServiceGroup(name);
        } catch (Exception e) {
            throw new AutoScalerException("Error occurred while retrieving cartridge group", e);
        }
    }

    @Override
    public String findClusterId(String applicationId, String alias) {
        try {
            Application application = ApplicationManager.getApplications().getApplication(applicationId);
            if (application != null) {

                ClusterDataHolder clusterData = application.getClusterDataHolderRecursivelyByAlias(alias);
                if (clusterData != null) {
                    return clusterData.getClusterId();
                }
            }
            return null;
        } catch (Exception e) {
            String message = String.format("Could not find cluster id: [application-id] %s [alias] %s",
                    applicationId, alias);
            throw new AutoScalerException(message, e);
        }
    }

    public ServiceGroup[] getServiceGroups() throws AutoScalerException {
        return RegistryManager.getInstance().getServiceGroups();
    }

    public boolean serviceGroupExist(String serviceName) {
        return false;
    }

    public void undeployServiceGroup(String name) throws AutoScalerException {
        try {
            RegistryManager.getInstance().removeServiceGroup(name);
        } catch (RegistryException e) {
            throw new AutoScalerException("Error occurred while removing the cartridge groups", e);
        }

    }

    @Override
    public String[] getApplicationNetworkPartitions(String applicationId)
            throws AutoScalerException {
        List<String> networkPartitionIds = AutoscalerUtil.getNetworkPartitionIdsReferedInApplication(applicationId);
        if (networkPartitionIds == null) {
            return null;
        }
        return networkPartitionIds.toArray(new String[networkPartitionIds.size()]);
    }

    @Override
    public void addApplicationPolicy(ApplicationPolicy applicationPolicy)
            throws RemoteException, InvalidApplicationPolicyException, InvalidPolicyException {

        // validating application policy
        AutoscalerUtil.validateApplicationPolicy(applicationPolicy);
        // Add application policy to the registry
        PolicyManager.getInstance().addApplicationPolicy(applicationPolicy);
    }

    @Override
    public ApplicationPolicy getApplicationPolicy(String applicationPolicyId) {
        return PolicyManager.getInstance().getApplicationPolicy(applicationPolicyId);
    }

    @Override
    public void removeApplicationPolicy(String applicationPolicyId) throws InvalidPolicyException {
        PolicyManager.getInstance().removeApplicationPolicy(applicationPolicyId);
    }

    @Override
    public void updateApplicationPolicy(ApplicationPolicy applicationPolicy)
            throws InvalidApplicationPolicyException, RemoteException, ApplicatioinPolicyNotExistsException {

        if (applicationPolicy == null) {
            String msg = "Application policy is null";
            log.error(msg);
            throw new InvalidApplicationPolicyException(msg);
        }

        String applicationPolicyId = applicationPolicy.getId();
        ApplicationPolicy existingApplicationPolicy = PolicyManager.getInstance().getApplicationPolicy(applicationPolicyId);
        if (existingApplicationPolicy == null) {
            String msg = String.format("No such application poliicy found [application-policy-id] %s", applicationPolicyId);
            log.error(msg);
            throw new ApplicatioinPolicyNotExistsException(msg);
        }

        // validating application policy
        AutoscalerUtil.validateApplicationPolicy(applicationPolicy);

        // updating application policy
        PolicyManager.getInstance().updateApplicationPolicy(applicationPolicy);
    }

    @Override
    public ApplicationPolicy[] getApplicationPolicies() {
        return PolicyManager.getInstance().getApplicationPolicies();
    }

    private void terminateAllApplicationMembersForcefully(String applicationId) {
        if (StringUtils.isEmpty(applicationId)) {
            throw new IllegalArgumentException("Application Id can not be empty");
        }

        Application application;
        try {
            ApplicationManager.acquireReadLockForApplication(applicationId);
            application = ApplicationManager.getApplications().getApplication(applicationId);
            if (application == null) {
                log.warn(String.format("Could not find application, thus no members to be terminated [application-id] %s", applicationId));
                return;
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(applicationId);
        }


        Set<ClusterDataHolder> allClusters = application.getClusterDataRecursively();
        //CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getInstance().ter
        for (ClusterDataHolder clusterDataHolder : allClusters) {
            String serviceType = clusterDataHolder.getServiceType();
            String clusterId = clusterDataHolder.getClusterId();

            Cluster cluster;
            try {
                TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
                cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);
            } finally {
                TopologyManager.releaseReadLockForCluster(serviceType, clusterId);
            }

            List<String> memberListToTerminate = new LinkedList<String>();
            for (Member member : cluster.getMembers()) {
                memberListToTerminate.add(member.getMemberId());
            }

            for (String memberIdToTerminate : memberListToTerminate) {
                try {
                    log.info(String.format("Terminating member forcefully [member-id] %s of the cluster [cluster-id] %s [application-id] %s", memberIdToTerminate, clusterId, application));
                    CloudControllerServiceClient.getInstance().terminateInstanceForcefully(memberIdToTerminate);
                } catch (Exception e) {
                    log.error(String.format("Forcefull termination of member %s is failed, but continuing forcefull deletion of other members", memberIdToTerminate));
                }
            }

        }
    }


    @Override
    public void addDeployementPolicy(DeploymentPolicy deploymentPolicy) throws RemoteException,
            InvalidDeploymentPolicyException, DeploymentPolicyNotExistsException, DeploymentPolicyAlreadyExistsException {

        validateDeploymentPolicy(deploymentPolicy);

        if (log.isInfoEnabled()) {
            log.info("Adding deployment policy: [deployment-policy-id] " + deploymentPolicy.getDeploymentPolicyID());
        }
        if (log.isDebugEnabled()) {
            log.debug("Deployment policy definition: " + deploymentPolicy.toString());
        }

        String deploymentPolicyID = deploymentPolicy.getDeploymentPolicyID();
        if (PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyID) != null) {
            String message = "Deployment policy already exists: [deployment-policy-id] " + deploymentPolicyID;
            log.error(message);
            throw new DeploymentPolicyAlreadyExistsException(message);
        }
        // Add cartridge to the cloud controller context and persist
        PolicyManager.getInstance().addDeploymentPolicy(deploymentPolicy);

        //        PolicyManager.getInstance().persist();

        if (log.isInfoEnabled()) {
            log.info("Successfully added deployment policy: [deployment-policy-id] " + deploymentPolicyID);
        }
    }

    private boolean validateDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws DeploymentPolicyNotExistsException,
            InvalidDeploymentPolicyException, RemoteException {

        // deployment policy can't be null
        if (null == deploymentPolicy) {
            String msg = "Invalid deployment policy. Cause -> Deployment policy is null";
            log.error(msg);
            throw new InvalidDeploymentPolicyException(msg);
        }

        if (log.isInfoEnabled()) {
            log.info(String.format("Validating deployment policy %s", deploymentPolicy.toString()));
        }

        // deployment policy id can't be null or empty
        if (null == deploymentPolicy.getDeploymentPolicyID() || deploymentPolicy.getDeploymentPolicyID().isEmpty()) {
            String msg = String.format("Invalid deployment policy. Cause -> Invalid deployment policy id [deployment-policy-id] %s",
                    deploymentPolicy.getDeploymentPolicyID());
            log.error(msg);
            throw new InvalidDeploymentPolicyException(msg);
        }

        // deployment policy should contain at least one network partition reference
        if (null == deploymentPolicy.getNetworkPartitions() || deploymentPolicy.getNetworkPartitions().length == 0) {
            String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
                            + "Cause -> Deployment policy doesn't have at least one network partition reference",
                    deploymentPolicy.getDeploymentPolicyID());
            log.error(msg);
            throw new InvalidDeploymentPolicyException(msg);
        }

        // validate each network partition references
        for (NetworkPartition networkPartition : deploymentPolicy.getNetworkPartitions()) {

            // network partition id can't be null or empty
            if (null == networkPartition.getId() || networkPartition.getId().isEmpty()) {
                String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
                                + "Cause -> Invalid network partition id in network partition references section",
                        deploymentPolicy.getDeploymentPolicyID());
                log.error(msg);
                throw new InvalidDeploymentPolicyException(msg);
            }

            // network partitions should be already added
            if (null == CloudControllerServiceClient.getInstance().getNetworkPartition(networkPartition.getId())) {
                String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
                                + "Cause -> Network partition is not added - [network-partition-id] %s",
                        deploymentPolicy.getDeploymentPolicyID(), networkPartition.getId());
                log.error(msg);
                throw new InvalidDeploymentPolicyException(msg);
            }

            // partition algorithm can't be null or empty
            if (null == networkPartition.getPartitionAlgo() || networkPartition.getPartitionAlgo().isEmpty()) {
                String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
                                + "Cause -> Invalid partition algorithm - [network-partition-id] %s [partition-algo] %s",
                        deploymentPolicy.getDeploymentPolicyID(), networkPartition.getId(), networkPartition.getPartitionAlgo());
                log.error(msg);
                throw new InvalidDeploymentPolicyException(msg);
            }

            // partition algorithm should be either one-after-another or round-robin
            if (!StratosConstants.PARTITION_ROUND_ROBIN_ALGORITHM_ID.equals(networkPartition.getPartitionAlgo())
                    && !StratosConstants.PARTITION_ONE_AFTER_ANOTHER_ALGORITHM_ID.equals(networkPartition.getPartitionAlgo())) {
                String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
                                + "Cause -> Invalid partition algorithm - [network-partition-id] %s [partition-algo] %s",
                        deploymentPolicy.getDeploymentPolicyID(), networkPartition.getId(), networkPartition.getPartitionAlgo());
                log.error(msg);
                throw new InvalidDeploymentPolicyException(msg);
            }

            // a network partition reference should contain at least one partition reference
            if (null == networkPartition.getPartitions() || networkPartition.getPartitions().length == 0) {
                String msg = String.format("Invalid deployment policy - [deployment-policy-id] %s. "
                        + "Cause -> Network partition reference doesn't have at lease one partition reference - "
                        + "[network-partition-id] %s", deploymentPolicy.getDeploymentPolicyID(), networkPartition.getId());
                log.error(msg);
                throw new InvalidDeploymentPolicyException(msg);
            }

        }
        return true;

    }

    @Override
    public void updateDeploymentPolicy(DeploymentPolicy deploymentPolicy) throws RemoteException,
            InvalidDeploymentPolicyException, DeploymentPolicyNotExistsException, InvalidPolicyException {

        validateDeploymentPolicy(deploymentPolicy);

        if (log.isInfoEnabled()) {
            log.info("Updating deployment policy: [deployment-policy-id] " + deploymentPolicy.getDeploymentPolicyID());
        }
        if (log.isDebugEnabled()) {
            log.debug("Updating Deployment policy definition: " + deploymentPolicy.toString());
        }

        String deploymentPolicyID = deploymentPolicy.getDeploymentPolicyID();
        if (PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyID) == null) {
            String message = "Deployment policy not exists: [deployment-policy-id] " + deploymentPolicyID;
            log.error(message);
            throw new DeploymentPolicyNotExistsException(message);
        }

        // Add cartridge to the cloud controller context and persist
        PolicyManager.getInstance().updateDeploymentPolicy(deploymentPolicy);
        //        PolicyManager.getInstance().persist();


        updateClusterMonitors(deploymentPolicy);

        if (log.isInfoEnabled()) {
            log.info("Successfully updated deployment policy: [deployment-policy-id] " + deploymentPolicyID);
        }
    }

    private void updateClusterMonitors(DeploymentPolicy deploymentPolicy) throws InvalidDeploymentPolicyException {

        for (ClusterMonitor clusterMonitor : AutoscalerContext.getInstance().getClusterMonitors().values()) {

            //Following if statement checks the relevant clusters for the updated deployment policy
            if (deploymentPolicy.getDeploymentPolicyID().equals(clusterMonitor.getDeploymentPolicyId())) {

                for (NetworkPartition networkPartition : deploymentPolicy.getNetworkPartitions()) {

                    ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext
                            = clusterMonitor.getClusterContext().getNetworkPartitionCtxt(networkPartition.getId());

                    try {
                        addNewPartitionsToClusterMonitor(clusterLevelNetworkPartitionContext, networkPartition,
                                deploymentPolicy.getDeploymentPolicyID(), clusterMonitor.getClusterContext().getServiceId());
                    } catch (RemoteException e) {
                        String message = "Cluster monitor update failed for [deployment-policy] "
                                + deploymentPolicy.getDeploymentPolicyID();
                        log.error(message);
                        throw new InvalidDeploymentPolicyException(message);
                    } catch (CloudControllerServiceInvalidPartitionExceptionException e) {

                        String message = "Cluster monitor update failed for [deployment-policy] "
                                + deploymentPolicy.getDeploymentPolicyID();
                        log.error(message);
                        throw new InvalidDeploymentPolicyException(message);
                    } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {

                        String message = "Cluster monitor update failed for [deployment-policy] "
                                + deploymentPolicy.getDeploymentPolicyID() + " [cluster] " + clusterMonitor.getClusterId();
                        log.error(message);
                        throw new InvalidDeploymentPolicyException(message);
                    }
                    removeOldPartitionsFromClusterMonitor(clusterLevelNetworkPartitionContext, networkPartition);
                }
            }
        }
    }

    private void removeOldPartitionsFromClusterMonitor(ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext,
                                                       NetworkPartition networkPartition) {

        for (InstanceContext instanceContext : clusterLevelNetworkPartitionContext.getInstanceIdToInstanceContextMap().values()) {

            ClusterInstanceContext clusterInstanceContext = (ClusterInstanceContext) instanceContext;

            for (ClusterLevelPartitionContext clusterLevelPartitionContext : clusterInstanceContext.getPartitionCtxts()) {

                if (null == networkPartition.getPartition(clusterLevelPartitionContext.getPartitionId())) {

                    //It has found that this partition context which is in cluster monitor is removed in updated policy
                    clusterLevelPartitionContext.setIsObsoletePartition(true);

                    while (clusterLevelPartitionContext.getActiveMembers().size() != 0) {

                        MemberContext member = clusterLevelPartitionContext.getActiveMembers().get(0);
                        clusterLevelPartitionContext.moveActiveMemberToTerminationPendingMembers(member.getMemberId());
                    }

                    while (clusterLevelPartitionContext.getPendingMembers().size() != 0) {

                        MemberContext member = clusterLevelPartitionContext.getPendingMembers().get(0);
                        clusterLevelPartitionContext.movePendingMemberToObsoleteMembers(member.getMemberId());
                    }
                }
            }
        }
    }

    private void addNewPartitionsToClusterMonitor(ClusterLevelNetworkPartitionContext clusterLevelNetworkPartitionContext,
                                                  NetworkPartition networkPartition, String deploymentPolicyID,
                                                  String cartridgeType) throws RemoteException,
            CloudControllerServiceInvalidPartitionExceptionException,
            CloudControllerServiceInvalidCartridgeTypeExceptionException {

        boolean validationOfNetworkPartitionRequired = false;
        for (Partition partition : networkPartition.getPartitions()) {

            //Iterating through instances
            for (InstanceContext instanceContext : clusterLevelNetworkPartitionContext.getInstanceIdToInstanceContextMap().values()) {

                ClusterInstanceContext clusterInstanceContext = (ClusterInstanceContext) instanceContext;
                if (null == clusterInstanceContext.getPartitionCtxt(partition.getId())) {

                    //It has found that this partition which is in deployment policy/network partition is new
                    ClusterLevelPartitionContext clusterLevelPartitionContext = new ClusterLevelPartitionContext(
                            partition, networkPartition.getId(), deploymentPolicyID);
                    validationOfNetworkPartitionRequired = true;
                    clusterInstanceContext.addPartitionCtxt(clusterLevelPartitionContext);
                }
            }
        }

        if (validationOfNetworkPartitionRequired) {

            CloudControllerServiceClient.getInstance().validateNetworkPartitionOfDeploymentPolicy(cartridgeType,
                    clusterLevelNetworkPartitionContext.getId());
        }
    }

    @Override
    public void removeDeployementPolicy(String deploymentPolicyID) throws DeploymentPolicyNotExistsException {
        if (log.isInfoEnabled()) {
            log.info("Removing deployment policy: [deployment-policy_id] " + deploymentPolicyID);
        }
        if (PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyID) == null) {
            String message = "Deployment policy not exists: [deployment-policy-id] " + deploymentPolicyID;
            log.error(message);
            throw new DeploymentPolicyNotExistsException(message);
        }
        PolicyManager.getInstance().removeDeploymentPolicy(deploymentPolicyID);
        if (log.isInfoEnabled()) {
            log.info("Successfully removed deployment policy: [deployment_policy_id] " + deploymentPolicyID);
        }

    }

    @Override
    public DeploymentPolicy getDeploymentPolicy(String deploymentPolicyID) {
        if (log.isDebugEnabled()) {
            log.debug("Getting deployment policy: [deployment-policy_id] " + deploymentPolicyID);
        }
        return PolicyManager.getInstance().getDeploymentPolicy(deploymentPolicyID);
    }

    @Override
    public DeploymentPolicy[] getDeploymentPolicies() {
        try {
            Collection<DeploymentPolicy> deploymentPolicies = PolicyManager.getInstance().getDeploymentPolicies();
            return deploymentPolicies.toArray(new DeploymentPolicy[deploymentPolicies.size()]);
        } catch (Exception e) {
            String message = "Could not get deployment policies";
            log.error(message);
            throw new AutoScalerException(message, e);
        }
    }

}
