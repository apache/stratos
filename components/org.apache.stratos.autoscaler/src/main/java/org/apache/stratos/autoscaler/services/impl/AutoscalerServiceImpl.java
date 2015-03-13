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
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.InvalidArgumentException;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.application.InvalidApplicationPolicyException;
import org.apache.stratos.autoscaler.exception.application.InvalidServiceGroupException;
import org.apache.stratos.autoscaler.exception.policy.ApplicatioinPolicyNotExistsException;
import org.apache.stratos.autoscaler.exception.policy.InvalidPolicyException;
import org.apache.stratos.autoscaler.monitor.cluster.ClusterMonitor;
import org.apache.stratos.autoscaler.pojo.Dependencies;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.pojo.policy.PolicyManager;
import org.apache.stratos.autoscaler.pojo.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.autoscaler.pojo.policy.deployment.ApplicationPolicy;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.services.AutoscalerService;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.common.Properties;
import org.apache.stratos.common.client.StratosManagerServiceClient;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.service.stub.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.service.stub.domain.application.signup.ArtifactRepository;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.wso2.carbon.registry.api.RegistryException;

import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
		if(validateAutoScalerPolicy(autoscalePolicyId)) {
			return PolicyManager.getInstance().removeAutoscalePolicy(autoscalePolicyId);
		}
		else{
			throw new InvalidPolicyException("This auto-scalar policy cannot remove, since it is used in applications.");
		}
	}

	/**
	 * Validate the Auto Scalar policy removal
	 * @param autoscalePolicyId Auto Scalar policy id boolean
	 * @return
	 */
	private boolean validateAutoScalerPolicy(String autoscalePolicyId) {
		boolean canRemove=true;
		Collection<ApplicationContext> appContexts= AutoscalerContext.getInstance().getApplicationContexts();
		for(ApplicationContext app:appContexts){
			CartridgeContext[] cartrideContexts=app.getComponents().getCartridgeContexts();
			for(CartridgeContext cartridgeContext: cartrideContexts) {
				SubscribableInfoContext subscribableInfoContexts = cartridgeContext.getSubscribableInfoContext();
				if (subscribableInfoContexts.getAutoscalingPolicy().equals(autoscalePolicyId)) {
						canRemove=false;
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

        if(log.isInfoEnabled()) {
            log.info(String.format("Adding application: [application-id] %s",
                    applicationContext.getApplicationId()));
        }

        ApplicationParser applicationParser = new DefaultApplicationParser();
        Application application = applicationParser.parse(applicationContext);
        RegistryManager.getInstance().persistApplication(application);

        List<ApplicationClusterContext> applicationClusterContexts = applicationParser.getApplicationClusterContexts();
        ApplicationClusterContext[] applicationClusterContextsArray = applicationClusterContexts.toArray(
                new ApplicationClusterContext[applicationClusterContexts.size()]);
        applicationContext.getComponents().setApplicationClusterContexts(applicationClusterContextsArray);

        applicationContext.setStatus(ApplicationContext.STATUS_CREATED);
        AutoscalerContext.getInstance().addApplicationContext(applicationContext);
        if(log.isInfoEnabled()) {
            log.info(String.format("Application added successfully: [application-id] %s",
                    applicationContext.getApplicationId()));
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
            Application application = RegistryManager.getInstance().getApplication(applicationId);
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

            // setting application policy id in application object
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

            if(!applicationContext.isMultiTenant()) {
			    // Add application signup for single tenant applications
			    addApplicationSignUp(applicationContext, application.getKey(), findApplicationClusterIds(application));
			}
			applicationContext.setStatus(ApplicationContext.STATUS_DEPLOYED);
			AutoscalerContext.getInstance().updateApplicationContext(applicationContext);

            // Check whether all the clusters are there
            boolean allClusterInitialized = false;
            try {
                ApplicationHolder.acquireReadLock();
                application = ApplicationHolder.getApplications().getApplication(applicationId);
                if (application != null) {
                    allClusterInitialized = AutoscalerUtil.allClustersInitialized(application);
                }
            } finally {
                ApplicationHolder.releaseReadLock();
            }
            
            if (!AutoscalerContext.getInstance().containsApplicationPendingMonitor(applicationId)) {
                if (allClusterInitialized) {
                    AutoscalerUtil.getInstance().startApplicationMonitor(applicationId);
                } else {
                    log.info("The application clusters are not yet created. " +
                            "Waiting for them to be created");
                }
            } else {
                log.info("The application monitor has already been created: [application-id] " + applicationId);
            }
            return true;
        } catch (Exception e) {
            ApplicationContext applicationContext = RegistryManager.getInstance().getApplicationContext(applicationId);
            if(applicationContext != null) {
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
     * @param application
     * @return
     */
    private List<String> findApplicationClusterIds(Application application) {
        List<String> clusterIds = new ArrayList<String>();
        for(ClusterDataHolder clusterDataHolder : application.getClusterDataRecursively()) {
            clusterIds.add(clusterDataHolder.getClusterId());
        }
        return clusterIds;
    }

    /**
     * Add application signup.
     * @param applicationContext
     * @param applicationKey
     * @param clusterIds
     */
    private void addApplicationSignUp(ApplicationContext applicationContext, String applicationKey,
                                      List<String> clusterIds) {

        try {
            if(log.isInfoEnabled()) {
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

                GroupContext[] groupContexts = components.getGroupContexts();
                if (groupContexts != null) {
                    for (GroupContext groupContext : groupContexts) {
                        if (groupContext != null) {
                            updateArtifactRepositoryList(artifactRepositoryList, groupContext.getCartridgeContexts());
                        }
                    }
                }

                ArtifactRepository[] artifactRepositoryArray = artifactRepositoryList.toArray(
                        new ArtifactRepository[artifactRepositoryList.size()]);
                applicationSignUp.setArtifactRepositories(artifactRepositoryArray);

                // Encrypt artifact repository passwords
                encryptRepositoryPasswords(applicationSignUp, applicationKey);

                StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
                serviceClient.addApplicationSignUp(applicationSignUp);

                if(log.isInfoEnabled()) {
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

    private void removeApplicationSignUp(ApplicationContext applicationContext){
        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Removing application signup: [application-id] %s",
                        applicationContext.getApplicationId()));
            }

            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();

            ApplicationSignUp applicationSignUp[] = serviceClient.getApplicationSignUps(applicationContext.getApplicationId());
            if ( applicationSignUp != null){
                for(ApplicationSignUp appSignUp : applicationSignUp) {
                    if ( appSignUp != null) {
                        serviceClient.removeApplicationSignUp(appSignUp.getApplicationId(), appSignUp.getTenantId());
                    }
                }
            }

        }catch(Exception e){
            String message = "Could not remove application signup(s)";
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Encrypt artifact repository passwords.
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
        for(CartridgeContext cartridgeContext : cartridgeContexts) {
            SubscribableInfoContext subscribableInfoContext = cartridgeContext.getSubscribableInfoContext();
            ArtifactRepositoryContext artifactRepositoryContext = subscribableInfoContext.getArtifactRepositoryContext();
            if(artifactRepositoryContext != null) {

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

    @Override
    public void undeployApplication(String applicationId) {
        try {
            if (log.isInfoEnabled()) {
                log.info("Starting to undeploy application: [application-id] " + applicationId);
            }

            ApplicationContext application = AutoscalerContext.getInstance().getApplicationContext(applicationId);
            if ( application == null){
                String msg = String.format("Application not found : [application-id] %s", applicationId);
                throw new RuntimeException(msg);
            }

            if (!application.getStatus().equals(ApplicationContext.STATUS_DEPLOYED)) {
                String message = String.format("Application is not deployed: [application-id] %s", applicationId);
                log.error(message);
                throw new RuntimeException(message);
            }

            // Remove Application SignUp(s) in stratos manager
            removeApplicationSignUp(application);
            
            // Remove network partition algorithm context
            AutoscalerContext.getInstance().removeNetworkPartitionAlgorithmContext(applicationId);
            
            ApplicationBuilder.handleApplicationUndeployed(applicationId);

            ApplicationContext applicationContext = AutoscalerContext.getInstance().getApplicationContext(applicationId);
            applicationContext.setStatus(ApplicationContext.STATUS_CREATED);
            AutoscalerContext.getInstance().updateApplicationContext(applicationContext);
            
            if (log.isInfoEnabled()) {
                log.info("Application undeployed successfully: [application-id] " + applicationId);
            }
        } catch (Exception e) {
            String message = "Could not undeploy application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public void deleteApplication(String applicationId) {
     
    	ApplicationContext appContext = AutoscalerContext.getInstance().getApplicationContext(applicationId);
    	
    	if (appContext == null) {
            String msg = String.format("Application not found : [application-id] %s", applicationId);
            throw new RuntimeException(msg);
        }
        
    	if (ApplicationContext.STATUS_DEPLOYED.equals(appContext.getStatus())) {
            String msg = String.format("Application is deployed : [application-id] %s. Please undeploy before deleting it.", applicationId);
            throw new AutoScalerException(msg);
        }
    	
    	AutoscalerContext.getInstance().removeApplicationContext(applicationId);
        
        if (RegistryManager.getInstance().getApplication(applicationId)== null) {
            String msg = String.format("Application not found : [application-id] %s", applicationId);
            throw new RuntimeException(msg);
        }
        ApplicationBuilder.handleApplicationRemoval(applicationId);
        RegistryManager.getInstance().removeApplication(applicationId);
        
        log.info(String.format("Application deleted successfully: [application-id] ", applicationId));
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

        if(log.isInfoEnabled()) {
            log.info(String.format("Adding cartridge group: [group-name] %s", servicegroup.getName()));
        }
        String groupName = servicegroup.getName();
        if (RegistryManager.getInstance().serviceGroupExist(groupName)) {
            throw new InvalidServiceGroupException("Cartridge group with the name " + groupName + " already exist.");
        }

        if (log.isDebugEnabled()) {
            log.debug(MessageFormat.format("Adding cartridge group {0}", servicegroup.getName()));
        }

        String[] subGroups = servicegroup.getCartridges();
        if (log.isDebugEnabled()) {
            log.debug("SubGroups" + subGroups);
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
                log.debug("StartupOrders " + startupOrders);

                if (startupOrders != null) {
                    log.debug("StartupOrder:size  " + startupOrders.length);
                } else {
                    log.debug("StartupOrder: is null");
                }
            }
            String[] scalingDependents = dependencies.getScalingDependants();

            if (log.isDebugEnabled()) {
                log.debug("ScalingDependent " + scalingDependents);

                if (scalingDependents != null) {
                    log.debug("ScalingDependents:size " + scalingDependents.length);
                } else {
                    log.debug("ScalingDependent: is null");
                }
            }
        }

        RegistryManager.getInstance().persistServiceGroup(servicegroup);
        if(log.isInfoEnabled()) {
            log.info(String.format("Cartridge group successfully added: [group-name] %s", servicegroup.getName()));
        }
    }

    @Override
    public void removeServiceGroup(String groupName) {
        try {
            if(log.isInfoEnabled()) {
                log.info(String.format("Starting to remove cartridge group: [group-name] %s", groupName));
            }
            if(RegistryManager.getInstance().serviceGroupExist(groupName)) {
                RegistryManager.getInstance().removeServiceGroup(groupName);
                if(log.isInfoEnabled()) {
                    log.info(String.format("Cartridge group removed: [group-name] %s", groupName));
                }
            } else {
                if(log.isWarnEnabled()) {
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
            if(application != null) {
                ClusterDataHolder clusterData = application.getClusterData(alias);
                if(clusterData != null) {
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
}
