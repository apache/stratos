/**
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at

 *  http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.stratos.rest.endpoint.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.*;
import org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeConfig;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.domain.Persistence;
import org.apache.stratos.cloud.controller.stub.domain.Volume;
import org.apache.stratos.common.beans.ApplicationBean;
import org.apache.stratos.common.beans.GroupBean;
import org.apache.stratos.common.beans.topology.ApplicationInfoBean;
import org.apache.stratos.common.beans.topology.ApplicationInstanceBean;
import org.apache.stratos.common.beans.topology.GroupInstanceBean;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.composite.application.beans.ApplicationDefinition;
import org.apache.stratos.manager.deploy.cartridge.CartridgeDeploymentManager;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.deploy.service.ServiceDeploymentManager;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.grouping.manager.ServiceGroupingManager;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.repository.RepositoryNotification;
import org.apache.stratos.manager.subscription.ApplicationSubscription;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.DataCartridgeSubscription;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.message.receiver.applications.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
//import org.apache.stratos.common.beans.ApplicationBean;
//import org.apache.stratos.common.beans.GroupBean;
import org.apache.stratos.common.beans.autoscaler.partition.ApplicationLevelNetworkPartition;
import org.apache.stratos.common.beans.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.common.beans.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.common.beans.cartridge.definition.PersistenceBean;
import org.apache.stratos.common.beans.cartridge.definition.PropertyBean;
import org.apache.stratos.common.beans.cartridge.definition.VolumeBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesGroup;
import org.apache.stratos.common.beans.kubernetes.KubernetesHost;
import org.apache.stratos.common.beans.kubernetes.KubernetesMaster;
import org.apache.stratos.common.beans.repositoryNotificationInfoBean.Payload;
import org.apache.stratos.rest.endpoint.util.converter.ObjectConverter;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Pattern;

public class StratosApiV41Utils {
    public static final String IS_VOLUME_REQUIRED = "volume.required";
    public static final String SHOULD_DELETE_VOLUME = "volume.delete.on.unsubscription";
    public static final String VOLUME_SIZE = "volume.size.gb";
    public static final String DEVICE_NAME = "volume.device.name";
    public static final String VOLUME_ID = "volume.id";
    public static final String TENANT_RANGE_ALL = "*";
    public static final String APPLICATION_STATUS_DEPLOYED = "Deployed";

    private static Log log = LogFactory.getLog(StratosApiV41Utils.class);
    private static CartridgeSubscriptionManager cartridgeSubsciptionManager = new CartridgeSubscriptionManager();
    private static ServiceGroupingManager serviceGropingManager = new ServiceGroupingManager();
    private static ServiceDeploymentManager serviceDeploymentManager = new ServiceDeploymentManager();

    // Util methods for cartridges
    public static void addCartridge(CartridgeDefinitionBean cartridgeDefinitionBean, ConfigurationContext ctxt,
                                       String userName, String tenantDomain) throws RestAPIException {

        log.info(String.format("Starting to deploy a cartridge: [type] %s " , cartridgeDefinitionBean.getType()));

        CartridgeConfig cartridgeConfig = ObjectConverter.convertCartridgeDefinitionBeanToStubCartridgeConfig(cartridgeDefinitionBean);
        if (cartridgeConfig == null) {
            throw new RestAPIException("Could not read cartridge definition, cartridge deployment failed");
        }
	    if (StringUtils.isEmpty(cartridgeConfig.getCategory())) {
		    throw new RestAPIException(String.format("Category is not specified %s , hence cartridge deployment failed",cartridgeConfig.getDisplayName()));
	    }
        try {
            CartridgeDeploymentManager.getDeploymentManager().deploy(cartridgeConfig);
        } catch (ADCException e) {
            throw new RestAPIException(e);
        }
        log.info(String.format("Successfully deployed cartridge: [cartridge-type] %s " , cartridgeDefinitionBean.getType()));
    }

    public static void removeCartridge(String cartridgeType) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {

            CartridgeInfo cartridgeInfo = null;
            try {
                cartridgeInfo = cloudControllerServiceClient.getCartridgeInfo(cartridgeType);
            } catch (RemoteException e) {
                log.error(String.format("Could not find cartridge: [type] %s ", cartridgeType));
                throw new RestAPIException(e);

            } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
                log.error(String.format("Could not find cartridge: [type]  %s " , cartridgeType));
                throw new RestAPIException(e);
            }

            if (cartridgeInfo == null) {
                String errorMsg = String.format("Could not find cartridge: [type] %s ", cartridgeType);
                log.error(errorMsg);
                throw new RestAPIException(errorMsg);
            }

            // check if the service is multi tenant.
            if (cartridgeInfo.getMultiTenant()) {
                // check if there are any deployed MT services. If so, should not allow to undeploy
                try {
                    Service service = serviceDeploymentManager.getService(cartridgeType);
                    if (service != null) {
	                    // not allowed to undeploy!
	                    String errorMsg =
			                    String.format("Multi tenant Service already exists for %s ,hence cannot undeploy",
			                                  cartridgeType);
	                    log.error(errorMsg);
	                    throw new RestAPIException(errorMsg);
                    } else {
                        // can undeploy
                        undeployCartridgeDefinition(cloudControllerServiceClient, cartridgeType);
                    }

                } catch (ADCException e) {
                    log.error(String.format("Error in getting MT Service details for type %s " , cartridgeType));
                    throw new RestAPIException(e);
                }

            } else {
                // if not multi tenant, check if there are any existing Subscriptions
                Collection<CartridgeSubscription> cartridgeSubscriptions =
                        cartridgeSubsciptionManager.getCartridgeSubscriptionsForType(cartridgeType);
                if (cartridgeSubscriptions != null && !cartridgeSubscriptions.isEmpty()) {
                    // not allowed to undeploy!
                    String errorMsg =String.format("Subscription exists for %s, cannot undeploy",cartridgeType);
                    log.error(errorMsg);
                    throw new RestAPIException(errorMsg);
                } else {
                    // can undeploy
                    undeployCartridgeDefinition(cloudControllerServiceClient, cartridgeType);
                }
            }
        }
    }

    private static void undeployCartridgeDefinition(CloudControllerServiceClient cloudControllerServiceClient,
                                                    String cartridgeType) throws RestAPIException {

        try {
            cloudControllerServiceClient.removeCartridge(cartridgeType);

        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);

        } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
            String msg = e.getFaultMessage().getInvalidCartridgeTypeException().getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    public static List<CartridgeDefinitionBean> getCartridgesByFilter(String filter, String criteria, ConfigurationContext configurationContext)
			throws RestAPIException {
		List<CartridgeDefinitionBean> cartridges = null;

		if (filter.equals("singleTenant")) {
			cartridges = getAvailableCartridges(null, false, configurationContext);
		} else if (filter.equals("multiTenant")) {
			cartridges = getAvailableCartridges(null, true, configurationContext);
		} else if (filter.equals("loadBalancer")) {
			cartridges = getAvailableLbCartridges(false, configurationContext);
		} else if (filter.equals("provider")) {
			cartridges = getAvailableCartridgesByProvider(criteria, configurationContext);
		}

		return cartridges;
	}

	public static CartridgeDefinitionBean getCartridgeByFilter(String filter, String cartridgeType, ConfigurationContext configurationContext)
	throws RestAPIException {
		List<CartridgeDefinitionBean> cartridges = getCartridgesByFilter(filter, null, configurationContext);

		for (CartridgeDefinitionBean cartridge : cartridges) {
			if (cartridge.getType().equals(cartridgeType)) {
				return cartridge;
			}
		}
		String msg = "Unavailable cartridge type: " + cartridgeType;
		log.error(msg);
		throw new RestAPIException(msg);
	}

    private static List<CartridgeDefinitionBean> getAvailableLbCartridges(Boolean multiTenant,
                                                    ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeDefinitionBean> cartridges = getAvailableCartridges(null, multiTenant,
                configurationContext);
        List<CartridgeDefinitionBean> lbCartridges = new ArrayList<CartridgeDefinitionBean>();
        for (CartridgeDefinitionBean cartridge : cartridges) {
            if ("loadbalancer".equalsIgnoreCase(cartridge.getCategory())) {
                lbCartridges.add(cartridge);
            }
        }
        return lbCartridges;
    }

	/**
	 * Get the available cartridges by provider
	 * @param provider provide name
	 * @param configurationContext configuration context
	 * @return List of the cartridge definitions
	 * @throws RestAPIException
	 */
	private static List<CartridgeDefinitionBean> getAvailableCartridgesByProvider(String provider, ConfigurationContext configurationContext) throws RestAPIException {
		List<CartridgeDefinitionBean> cartridges = new ArrayList<CartridgeDefinitionBean>();

		if (log.isDebugEnabled()) {
			log.debug("Reading cartridges: [provider-name] " + provider );
		}

		try {
			String[] availableCartridges = CloudControllerServiceClient.getServiceClient().getRegisteredCartridges();

			if (availableCartridges != null) {
				for (String cartridgeType : availableCartridges) {
					CartridgeInfo cartridgeInfo = null;
					try {
						cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);
					} catch (Exception e) {
						if (log.isWarnEnabled()) {
							log.warn("Error when calling getCartridgeInfo for " + cartridgeType + ", Error: "
							         + e.getMessage());
						}
					}
					if (cartridgeInfo == null) {
						// This cannot happen. But continue
						if (log.isDebugEnabled()) {
							log.debug("Cartridge Info not found: " + cartridgeType);
						}
						continue;
					}


					if (!cartridgeInfo.getProvider().equals(provider)) {
						continue;
					}

                    CartridgeDefinitionBean cartridge = convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
					cartridges.add(cartridge);
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug("There are no available cartridges");
				}
			}
		} catch (AxisFault axisFault) {
			String errorMsg = String.format(
					"Error while getting CloudControllerServiceClient instance to connect to the Cloud Controller. " +
					"Cause: %s ", axisFault.getMessage());
			log.error(errorMsg, axisFault);
			throw new RestAPIException(errorMsg, axisFault);
		} catch (RemoteException e) {
			String errorMsg =
					String.format("Error while getting cartridge information for provider %s  Cause: %s ", provider,
					              e.getMessage());
			log.error(errorMsg, e);
			throw new RestAPIException(errorMsg, e);
		}

		if (log.isDebugEnabled()) {
			log.debug("Returning available cartridges " + cartridges.size());
		}

		return cartridges;
    }

    public static List<CartridgeDefinitionBean> getAvailableCartridges(String cartridgeSearchString, Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeDefinitionBean> cartridges = new ArrayList<CartridgeDefinitionBean>();

        if (log.isDebugEnabled()) {
            log.debug("Getting available cartridges. Search String: " + cartridgeSearchString + ", Multi-Tenant: " + multiTenant);
        }

        boolean allowMultipleSubscription = new Boolean(
                System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

        try {
            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

            String[] availableCartridges = CloudControllerServiceClient.getServiceClient().getRegisteredCartridges();

            if (availableCartridges != null) {
                for (String cartridgeType : availableCartridges) {
                    CartridgeInfo cartridgeInfo = null;
                    try {
                        cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Error when calling getCartridgeInfo for " + cartridgeType + ", Error: "
                                    + e.getMessage());
                        }
                    }
                    if (cartridgeInfo == null) {
                        // This cannot happen. But continue
                        if (log.isDebugEnabled()) {
                            log.debug("Cartridge Info not found: " + cartridgeType);
                        }
                        continue;
                    }

                    if (multiTenant != null && !multiTenant && cartridgeInfo.getMultiTenant()) {
                        // Need only Single-Tenant cartridges
                        continue;
                    } else if (multiTenant != null && multiTenant && !cartridgeInfo.getMultiTenant()) {
                        // Need only Multi-Tenant cartridges
                        continue;
                    }

                    if (!StratosApiV41Utils.cartridgeMatches(cartridgeInfo, searchPattern)) {
                        continue;
                    }

                    CartridgeDefinitionBean cartridge = convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
                    cartridges.add(cartridge);


                    if (cartridgeInfo.getMultiTenant() && !allowMultipleSubscription) {
                        // If the cartridge is multi-tenant. We should not let users
                        // createSubscription twice.
                        if (isAlreadySubscribed(cartridgeType,
                                ApplicationManagementUtil.getTenantId(configurationContext))) {
                            if (log.isDebugEnabled()) {
                                log.debug("Already subscribed to " + cartridgeType
                                        + ". This multi-tenant cartridge will not be available to createSubscription");
                            }
                            //cartridge.setStatus(CartridgeConstants.SUBSCRIBED);
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no available cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error while getting available cartridges. Cause: " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        //Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning available cartridges " + cartridges.size());
        }

        return cartridges;
    }

    public static CartridgeDefinitionBean getCartridge(String cartridgeType) throws RestAPIException {
        try {
            CartridgeInfo cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(cartridgeType);
            if(cartridgeInfo == null) {
                return null;
            }
            return convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    private static CartridgeDefinitionBean convertCartridgeToCartridgeDefinitionBean(CartridgeInfo cartridgeInfo) {
        CartridgeDefinitionBean cartridge = new CartridgeDefinitionBean();
        cartridge.setType(cartridgeInfo.getType());
        cartridge.setProvider(cartridgeInfo.getProvider());
        cartridge.setCategory(cartridgeInfo.getCategory());
        cartridge.setDisplayName(cartridgeInfo.getDisplayName());
        cartridge.setDescription(cartridgeInfo.getDescription());
        cartridge.setVersion(cartridgeInfo.getVersion());
        cartridge.setMultiTenant(cartridgeInfo.getMultiTenant());
        cartridge.setHost(cartridgeInfo.getHostName());
        cartridge.setDefaultAutoscalingPolicy(cartridgeInfo.getDefaultAutoscalingPolicy());
        cartridge.setDefaultDeploymentPolicy(cartridgeInfo.getDefaultDeploymentPolicy());
        cartridge.setPersistence(convertPersistenceToPersistenceBean(cartridgeInfo.getPersistence()));
        cartridge.setServiceGroup(cartridgeInfo.getServiceGroup());
        return cartridge;
    }

    private static PersistenceBean convertPersistenceToPersistenceBean(Persistence persistence) {
        if(persistence == null) {
            return null;
        }

        PersistenceBean persistenceBean = new PersistenceBean();
        persistenceBean.setRequired(persistence.isPersistanceRequiredSpecified());
        persistenceBean.setVolume(convertVolumesToVolumeBeans(persistence.getVolumes()));
        return persistenceBean;
    }

    private static List<VolumeBean> convertVolumesToVolumeBeans(Volume[] volumes) {
        List<VolumeBean> list = new ArrayList<VolumeBean>();
        for(Volume volume : volumes) {
            VolumeBean volumeBean = new VolumeBean();
            volumeBean.setId(volume.getId());
            volumeBean.setDevice(volume.getDevice());
            volumeBean.setSize(String.valueOf(volume.getSize()));
            volumeBean.setSnapshotId(volume.getSnapshotId());
            list.add(volumeBean);
        }
        return list;
    }

    private static boolean isAlreadySubscribed(String cartridgeType,
                                               int tenantId) {

        Collection<CartridgeSubscription> subscriptionList = CartridgeSubscriptionManager.isCartridgeSubscribed(tenantId, cartridgeType);
        if (subscriptionList == null || subscriptionList.isEmpty()) {
            return false;
        } else {
            return true;
        }
    }

    private static Pattern getSearchStringPattern(String searchString) {
        if (log.isDebugEnabled()) {
            log.debug("Creating search pattern for " + searchString);
        }
        if (searchString != null) {
            // Copied from org.wso2.carbon.webapp.mgt.WebappAdmin.doesWebappSatisfySearchString(WebApplication, String)
            String regex = searchString.toLowerCase().replace("..?", ".?").replace("..*", ".*").replaceAll("\\?", ".?")
                    .replaceAll("\\*", ".*?");
            if (log.isDebugEnabled()) {
                log.debug("Created regex: " + regex + " for search string " + searchString);
            }

            Pattern pattern = Pattern.compile(regex);
            return pattern;
        }
        return null;
    }

    private static boolean cartridgeMatches(CartridgeInfo cartridgeInfo, Pattern pattern) {
        if (pattern != null) {
            boolean matches = false;
            if (cartridgeInfo.getDisplayName() != null) {
                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
            }
            if (!matches && cartridgeInfo.getDescription() != null) {
                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
            }
            return matches;
        }
        return true;
    }

    private static boolean cartridgeMatches(CartridgeInfo cartridgeInfo, CartridgeSubscription cartridgeSubscription, Pattern pattern) {
        if (pattern != null) {
            boolean matches = false;
            if (cartridgeInfo.getDisplayName() != null) {
                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
            }
            if (!matches && cartridgeInfo.getDescription() != null) {
                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
            }
            if (!matches && cartridgeSubscription.getType() != null) {
                matches = pattern.matcher(cartridgeSubscription.getType().toLowerCase()).find();
            }
            if (!matches && cartridgeSubscription.getAlias() != null) {
                matches = pattern.matcher(cartridgeSubscription.getAlias().toLowerCase()).find();
            }
            return matches;
        }
        return true;
    }

    // Util methods to get the service clients

    private static CloudControllerServiceClient getCloudControllerServiceClient() throws RestAPIException {

        try {
            return CloudControllerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting CloudControllerServiceClient instance to connect to the "
                    + "Cloud Controller. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    private static AutoscalerServiceClient getAutoscalerServiceClient() throws RestAPIException {
        try {
            return AutoscalerServiceClient.getServiceClient();
        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting AutoscalerServiceClient instance to connect to the "
                    + "Autoscaler. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    // Util methods for Autoscaling policies

    public static void addAutoscalingPolicy(AutoscalePolicy autoscalePolicyBean) throws RestAPIException {

        log.info(String.format("Deploying autoscaling policy: [id] %s", autoscalePolicyBean.getId()));

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = ObjectConverter.
                    convertToCCAutoscalerPojo(autoscalePolicyBean);

            try {
                autoscalerServiceClient
                        .deployAutoscalingPolicy(autoscalePolicy);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (AutoScalerServiceInvalidPolicyExceptionException e) {
                String message = e.getFaultMessage()
                        .getInvalidPolicyException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
    }

    public static void updateAutoscalingPolicy(AutoscalePolicy autoscalePolicyBean) throws RestAPIException {

        log.info(String.format("Updating autoscaling policy: [id] %s", autoscalePolicyBean.getId()));

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = ObjectConverter.
                    convertToCCAutoscalerPojo(autoscalePolicyBean);

            try {
                autoscalerServiceClient.updateAutoscalingPolicy(autoscalePolicy);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (AutoScalerServiceInvalidPolicyExceptionException e) {
                String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
    }

    public static AutoscalePolicy[] getAutoScalePolicies() throws RestAPIException {

        org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy[] autoscalePolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalePolicies = autoscalerServiceClient.getAutoScalePolicies();

            } catch (RemoteException e) {
                String errorMsg = "Error while getting available autoscaling policies. Cause : " + e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }
        return ObjectConverter.convertStubAutoscalePoliciesToAutoscalePolicies(autoscalePolicies);
    }

    public static AutoscalePolicy getAutoScalePolicy(String autoscalePolicyId) throws RestAPIException {

        org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalePolicy = autoscalerServiceClient.getAutoScalePolicy(autoscalePolicyId);

            } catch (RemoteException e) {
                String errorMsg = "Error while getting information for autoscaling policy with id " +
                        autoscalePolicyId + ".  Cause: " + e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return ObjectConverter.convertStubAutoscalePolicyToAutoscalePolicy(autoscalePolicy);
    }

    public static org.apache.stratos.common.beans.autoscaler.policy.deployment.DeploymentPolicy
        getDeploymentPolicy(String applicationId) throws RestAPIException {

        try {
            AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
            DeploymentPolicy deploymentPolicy = autoscalerServiceClient.getDeploymentPolicy(applicationId);
            return ObjectConverter.convertStubDeploymentPolicyToDeploymentPolicy(deploymentPolicy);
        } catch (RemoteException e) {
            String errorMsg = "Could not read deployment policy: [application-id] " + applicationId;
            log.error(errorMsg, e);
            throw new RestAPIException(errorMsg, e);
        }
    }

    public static ApplicationLevelNetworkPartition[] getPartitionGroups(String deploymentPolicyId)
            throws RestAPIException {

        org.apache.stratos.autoscaler.stub.deployment.partition.ApplicationLevelNetworkPartition[] partitionGroups = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitionGroups = autoscalerServiceClient.getApplicationLevelNetworkPartition(deploymentPolicyId);

            } catch (RemoteException e) {
                String errorMsg = "Error getting available partition groups for deployment policy id "
                        + deploymentPolicyId + ". Cause: " + e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return ObjectConverter.convertStubApplicationLevelNetworkPartitionsToApplicationLevelNetworkPartitions(partitionGroups);
    }

    // Util methods for services and subscriptions

    public static List<Cartridge> getSubscriptions(String cartridgeSearchString, String serviceGroup, ConfigurationContext configurationContext) throws RestAPIException {
        List<Cartridge> cartridges = new ArrayList<Cartridge>();

        if (log.isDebugEnabled()) {
            log.debug("Getting subscribed cartridges. Search String: " + cartridgeSearchString);
        }

        try {
            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

            Collection<CartridgeSubscription> subscriptions = CartridgeSubscriptionManager.getCartridgeSubscriptions(ApplicationManagementUtil.
                    getTenantId(configurationContext), null);

            if (subscriptions != null && !subscriptions.isEmpty()) {

                for (CartridgeSubscription subscription : subscriptions) {

                    if (!cartridgeMatches(subscription.getCartridgeInfo(), subscription, searchPattern)) {
                        continue;
                    }
                    Cartridge cartridge = getCartridgeFromSubscription(subscription);
                    if (cartridge == null) {
                        continue;
                    }
                    Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
                            cartridge.getCartridgeAlias());
                    String cartridgeStatus = "Inactive";
                    int activeMemberCount = 0;
                    if (cluster != null) {
                        Collection<Member> members = cluster.getMembers();
                        for (Member member : members) {
                            if (member.isActive()) {
                                cartridgeStatus = "Active";
                                activeMemberCount++;
                            }
                        }
                    }
                    cartridge.setActiveInstances(activeMemberCount);
                    cartridge.setStatus(cartridgeStatus);

                    // Ignoring the LB cartridges since they are not shown to the user.
                    if (cartridge.isLoadBalancer())
                        continue;
                    if (StringUtils.isNotEmpty(serviceGroup)) {
                        if (cartridge.getServiceGroup() != null && serviceGroup.equals(cartridge.getServiceGroup())) {
                            cartridges.add(cartridge);
                        }
                    } else {
                        cartridges.add(cartridge);
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no subscribed cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error while getting subscribed cartridges. Cause: " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning subscribed cartridges " + cartridges.size());
        }
        
        /*if(cartridges.isEmpty()) {
            String msg = "Cannot find any subscribed Cartridge, matching the given string: "+cartridgeSearchString;
            log.error(msg);
            throw new RestAPIException(msg);
        }*/

        return cartridges;
    }

    private static Cartridge getCartridgeFromSubscription(CartridgeSubscription subscription) throws RestAPIException {

        if (subscription == null) {
            return null;
        }
        try {
            Cartridge cartridge = new Cartridge();
            cartridge.setCartridgeType(subscription.getCartridgeInfo()
                    .getType());
            cartridge.setMultiTenant(subscription.getCartridgeInfo()
                    .getMultiTenant());
            cartridge
                    .setProvider(subscription.getCartridgeInfo().getProvider());
            cartridge.setVersion(subscription.getCartridgeInfo().getVersion());
            cartridge.setDescription(subscription.getCartridgeInfo()
                    .getDescription());
            cartridge.setDisplayName(subscription.getCartridgeInfo()
                    .getDisplayName());
            cartridge.setCartridgeAlias(subscription.getAlias());
            cartridge.setHostName(subscription.getHostName());
            cartridge.setMappedDomain(subscription.getMappedDomain());
            if (subscription.getRepository() != null) {
                cartridge.setRepoURL(subscription.getRepository().getUrl());
            }

            if (subscription instanceof DataCartridgeSubscription) {
                DataCartridgeSubscription dataCartridgeSubscription = (DataCartridgeSubscription) subscription;
                cartridge.setDbHost(dataCartridgeSubscription.getDBHost());
                cartridge.setDbUserName(dataCartridgeSubscription
                        .getDBUsername());
                cartridge
                        .setPassword(dataCartridgeSubscription.getDBPassword());
            }

            if (subscription.getLbClusterId() != null
                    && !subscription.getLbClusterId().isEmpty()) {
                cartridge.setLbClusterId(subscription.getLbClusterId());
            }

            cartridge.setClusterId(subscription.getClusterDomain());
            cartridge.setStatus(subscription.getSubscriptionStatus());
            cartridge.setPortMappings(subscription.getCartridgeInfo()
                    .getPortMappings());

            if (subscription.getCartridgeInfo().getProperties() != null) {
                for (org.apache.stratos.cloud.controller.stub.Property property : subscription.getCartridgeInfo().getProperties()) {
                    if (property.getName().equals("load.balancer")) {
                        cartridge.setLoadBalancer(true);
                    }
                }
            }
            if (subscription.getCartridgeInfo().getServiceGroup() != null) {
                cartridge.setServiceGroup(subscription.getCartridgeInfo().getServiceGroup());
            }
            return cartridge;

        } catch (Exception e) {
            String msg = "Unable to extract the Cartridge from subscription. Cause: " + e.getMessage();
            log.error(msg);
            throw new RestAPIException(msg);
        }

    }

    public static CartridgeSubscription getCartridgeSubscription(String alias, ConfigurationContext configurationContext) {
        return CartridgeSubscriptionManager.getCartridgeSubscription(ApplicationManagementUtil.getTenantId(configurationContext), alias);
    }

    // Util methods for clusters

    public static org.apache.stratos.common.beans.topology.Cluster[] getClustersForTenant(ConfigurationContext configurationContext) {

        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext), null);
        ArrayList<org.apache.stratos.common.beans.topology.Cluster> clusters =
                new ArrayList<org.apache.stratos.common.beans.topology.Cluster>();
        for (Cluster cluster : clusterSet) {
            clusters.add(ObjectConverter.convertClusterToClusterBean(cluster, null));
        }
        org.apache.stratos.common.beans.topology.Cluster[] arrCluster =
                new org.apache.stratos.common.beans.topology.Cluster[clusters.size()];
        arrCluster = clusters.toArray(arrCluster);
        return arrCluster;

    }

    public static org.apache.stratos.common.beans.topology.Cluster[] getClustersForTenantAndCartridgeType(ConfigurationContext configurationContext,
                                                                                                                String cartridgeType) {

        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext), cartridgeType);
        List<org.apache.stratos.common.beans.topology.Cluster> clusters =
                new ArrayList<org.apache.stratos.common.beans.topology.Cluster>();
        for (Cluster cluster : clusterSet) {
            clusters.add(ObjectConverter.convertClusterToClusterBean(cluster, null));
        }
        org.apache.stratos.common.beans.topology.Cluster[] arrCluster =
                new org.apache.stratos.common.beans.topology.Cluster[clusters.size()];
        arrCluster = clusters.toArray(arrCluster);
        return arrCluster;

    }

    // Util methods for repo actions

    public static void getGitRepositoryNotification(Payload payload) throws RestAPIException {
        try {

            RepositoryNotification repoNotification = new RepositoryNotification();
            repoNotification.updateRepository(payload.getRepository().getUrl());

        } catch (Exception e) {
            String msg = "Failed to get git repository notifications. Cause : " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    public static void synchronizeRepository(CartridgeSubscription cartridgeSubscription) throws RestAPIException {
        try {
            RepositoryNotification repoNotification = new RepositoryNotification();
            repoNotification.updateRepository(cartridgeSubscription);
        } catch (Exception e) {
            String msg = "Failed to get git repository notifications. Cause : " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    // Util methods for service groups

    public static void addServiceGroup(ServiceGroupDefinition serviceGroupDefinition) throws RestAPIException {

        try {
            serviceGropingManager.deployServiceGroupDefinition(serviceGroupDefinition);

        } catch (InvalidServiceGroupException e) {
            throw new RestAPIException(e);
        } catch (ServiceGroupDefinitioException e) {
            throw new RestAPIException(e);
        } catch (ADCException e) {
            throw new RestAPIException(e);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            throw new RestAPIException(e);
        }

        log.info("Successfully created the Service Group Definition with name " + serviceGroupDefinition.getName());
    }

    public static ServiceGroupDefinition getServiceGroupDefinition(String serviceGroupDefinitionName) throws RestAPIException {

        try {
            return serviceGropingManager.getServiceGroupDefinition(serviceGroupDefinitionName);

        } catch (ServiceGroupDefinitioException e) {
            throw new RestAPIException(e);
        } catch (ADCException e) {
            throw new RestAPIException(e);
        }
    }

    public static ServiceGroupDefinition[] getServiceGroupDefinitions() throws RestAPIException {
        try {
            return serviceGropingManager.getServiceGroupDefinitions();
        } catch (ADCException e) {
            throw new RestAPIException(e);
        } catch (ServiceGroupDefinitioException e) {
            throw new RestAPIException(e);
        }
    }

    public static void removeServiceGroup(String serviceGroupDefinitionName) throws RestAPIException {

        try {
            serviceGropingManager.undeployServiceGroupDefinition(serviceGroupDefinitionName);

        } catch (ServiceGroupDefinitioException e) {
            throw new RestAPIException(e);
        } catch (ADCException e) {
            throw new RestAPIException(e);
        }

        log.info("Successfully deleted the Service Group Definition with name " + serviceGroupDefinitionName);
    }

    // Util methods for Applications

    /**
     * Verify the existence of the application and add it.
     * @param appDefinition
     * @param ctxt
     * @param userName
     * @param tenantDomain
     * @throws RestAPIException
     */
    public static void addApplication(ApplicationDefinition appDefinition, ConfigurationContext ctxt,
                                                   String userName, String tenantDomain)
            throws RestAPIException {

        // check if an application with same id already exists
        try {
            if (AutoscalerServiceClient.getServiceClient().getApplication(appDefinition.getApplicationId()) != null) {
                String msg = "Application already exists: [application-id]" + appDefinition.getApplicationId();
                throw new RestAPIException(msg);
            }
        } catch (RemoteException e) {
            throw new RestAPIException("Could not read application", e);
        }

        ApplicationContext applicationContext = ObjectConverter.convertApplicationDefinitionToStubApplicationContext(appDefinition);
        applicationContext.setTenantId(ApplicationManagementUtil.getTenantId(ctxt));
        applicationContext.setTenantDomain(tenantDomain);
        applicationContext.setTeantAdminUsername(userName);

        if (appDefinition.getProperty() != null) {
            org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
            for (org.apache.stratos.manager.composite.application.beans.PropertyBean propertyBean : appDefinition.getProperty()) {
                org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
                property.setName(propertyBean.getName());
                property.setValue(propertyBean.getValue());
                properties.addProperties(property);
            }
            applicationContext.setProperties(properties);
        }

        try {
            AutoscalerServiceClient.getServiceClient().addApplication(applicationContext);
        } catch (AutoScalerServiceApplicationDefinitionExceptionException e) {
            throw new RestAPIException(e);
        } catch (RemoteException e) {
            throw new RestAPIException(e);
        }
    }

    /**
     * Deploy application with a deployment policy.
     *
     * @param applicationId
     * @param deploymentPolicy
     * @throws RestAPIException
     */
    public static void deployApplication(
            String applicationId,
            org.apache.stratos.common.beans.autoscaler.policy.deployment.DeploymentPolicy deploymentPolicy)
            throws RestAPIException {

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Starting to deploy application: [application-id] %s", applicationId));
            }

            AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
            ApplicationContext application = autoscalerServiceClient.getApplication(applicationId);
            if(application == null) {
                String message = String.format("Application is not found: [application-id] %s", applicationId);
                log.error(message);
                throw new RestAPIException(message);
            }
            if ((application != null) && (application.getStatus().equals(APPLICATION_STATUS_DEPLOYED))) {
                String message = String.format("Application is already deployed: [application-id] %s", applicationId);
                log.error(message);
                throw new RestAPIException(message);
            }

            if (!applicationId.equals(deploymentPolicy.getApplicationId())) {
                String message = String.format("Application id %s does not match with the deployment policy %s",
                        applicationId, deploymentPolicy.getApplicationId());
                log.error(message);
                throw new RestAPIException(message);
            }

            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy stubDeploymentPolicy =
                    ObjectConverter.convetToASDeploymentPolicyPojo(deploymentPolicy);

            autoscalerServiceClient.deployApplication(applicationId, stubDeploymentPolicy);
            if (log.isInfoEnabled()) {
                log.info(String.format("Application deployed successfully: [application-id] %s", applicationId));
            }
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        } catch (AutoScalerServiceInvalidPolicyExceptionException e) {
            String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        } catch (AutoScalerServiceApplicationDefinitionExceptionException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static ApplicationSubscription getApplicationSubscriptions(String appId, ConfigurationContext ctxt) throws RestAPIException {
        CartridgeSubscriptionManager subscriptionMgr = new CartridgeSubscriptionManager();
        try {
            return subscriptionMgr.getApplicationSubscription(appId, ApplicationManagementUtil.getTenantId(ctxt));
        } catch (ApplicationSubscriptionException e) {
            throw new RestAPIException(e);
        }
    }

    public static void removeApplication(String applicationId) throws RestAPIException {

        try {
            AutoscalerServiceClient.getServiceClient().deleteApplication(applicationId);
        } catch (RemoteException e) {
            String message = "Could not delete application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static ApplicationDefinition getApplication(String applicationId) throws RestAPIException {
        try {
            return ObjectConverter.convertStubApplicationContextToApplicationDefinition(
                    AutoscalerServiceClient.getServiceClient().getApplication(applicationId));
        } catch (RemoteException e) {
            String message = "Could not read application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static List<ApplicationDefinition> getApplications() throws RestAPIException {
        try {
            List<ApplicationDefinition> applicationDefinitions = new ArrayList<ApplicationDefinition>();
            ApplicationContext[] applicationContexts = AutoscalerServiceClient.getServiceClient().getApplications();
            if(applicationContexts != null) {
                for (ApplicationContext applicationContext : applicationContexts) {
                    if(applicationContext != null) {
                        ApplicationDefinition applicationDefinition =
                                ObjectConverter.convertStubApplicationContextToApplicationDefinition(applicationContext);
                        applicationDefinitions.add(applicationDefinition);
                    }
                }
            }
            return applicationDefinitions;
        } catch (RemoteException e) {
            String message = "Could not read applications";
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static void undeployApplication(String applicationId) throws RestAPIException {
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalerServiceClient.undeployApplication(applicationId);
            } catch (RemoteException e) {
                String message = "Could not undeploy application: [application-id] " + applicationId;
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (AutoScalerServiceApplicationDefinitionExceptionException e) {
                String message = "Could not undeploy application: [application-id] " + applicationId;
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
    }
    
    public static ApplicationInfoBean[] getApplicationRuntimes() {
        List<ApplicationInfoBean> applicationBeanList = new ArrayList<ApplicationInfoBean>();
        try {
            ApplicationManager.acquireReadLockForApplications();
            ApplicationInfoBean applicationInfoBean;
            for (Application application : ApplicationManager.getApplications().getApplications().values()) {
                applicationInfoBean = ObjectConverter.convertApplicationToApplicationBean(application);
                for(ApplicationInstanceBean instanceBean : applicationInfoBean.getApplicationInstances()) {
                    addClustersInstancesToApplicationInstanceBean(instanceBean, application);
                    addGroupsInstancesToApplicationInstanceBean(instanceBean, application);
                }
                applicationBeanList.add(applicationInfoBean);
            }
        } finally {
            ApplicationManager.releaseReadLockForApplications();
        }

        return applicationBeanList.toArray(new ApplicationInfoBean[applicationBeanList.size()]);
    }

    /*public static ApplicationBean getApplicationRuntime(String applicationId) {
        ApplicationBean applicationBean = null;
        try {
            ApplicationManager.acquireReadLockForApplication(applicationId);
            Application application = ApplicationManager.getApplications().getApplication(applicationId);
            if (application == null) {
                return null;
            }
            applicationBean = ObjectConverter.convertApplicationToApplicationBean(application);
            addClustersToApplicationBean(applicationBean, application);
            addGroupsToApplicationBean(applicationBean, application);
        } finally {
            ApplicationManager.releaseReadLockForApplication(applicationId);
        }
        return applicationBean;
    }*/

    public static ApplicationInfoBean getApplicationInstanceRuntime(String applicationId) {
        ApplicationInfoBean applicationBean = null;
        try {
            ApplicationManager.acquireReadLockForApplication(applicationId);
            Application application = ApplicationManager.getApplications().getApplication(applicationId);
            if (application == null) {
                return null;
            }
            applicationBean = ObjectConverter.convertApplicationToApplicationInstanceBean(application);
            for(ApplicationInstanceBean instanceBean : applicationBean.getApplicationInstances()) {
                addClustersInstancesToApplicationInstanceBean(instanceBean, application);
                addGroupsInstancesToApplicationInstanceBean(instanceBean, application);
            }

        } finally {
            ApplicationManager.releaseReadLockForApplication(applicationId);
        }
        return applicationBean;
    }

    private static void addGroupsToApplicationBean(ApplicationBean applicationBean, Application application) {
        Collection<Group> groups = application.getGroups();
        for (Group group : groups) {
            GroupBean groupBean = ObjectConverter.convertGroupToGroupBean(group);
            setSubGroups(group, groupBean);
            applicationBean.addGroup(groupBean);
        }
    }

    private static void addGroupsInstancesToApplicationInstanceBean(ApplicationInstanceBean applicationInstanceBean,
                                                                    Application application) {
        Collection<Group> groups = application.getGroups();
        for (Group group : groups) {
            List<GroupInstanceBean> groupInstanceBeans = ObjectConverter.convertGroupToGroupInstancesBean(
                                                applicationInstanceBean.getInstanceId(), group);
            for(GroupInstanceBean groupInstanceBean : groupInstanceBeans) {
                setSubGroupInstances(group, groupInstanceBean);
                applicationInstanceBean.getGroupInstances().add(groupInstanceBean);
            }
        }
    }

    private static void addClustersToApplicationBean(ApplicationBean applicationBean, Application application) {
        Map<String, ClusterDataHolder> topLevelClusterDataMap = application.getClusterDataMap();
        for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
            ClusterDataHolder clusterDataHolder = entry.getValue();
            String clusterId = clusterDataHolder.getClusterId();
            String serviceType = clusterDataHolder.getServiceType();
            TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
            Cluster topLevelCluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);
            applicationBean.getClusters().add(ObjectConverter.convertClusterToClusterBean(topLevelCluster, entry.getKey()));
        }
    }

    private static void addClustersInstancesToApplicationInstanceBean(
            ApplicationInstanceBean applicationInstanceBean,
            Application application) {
        Map<String, ClusterDataHolder> topLevelClusterDataMap = application.getClusterDataMap();
        for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
            ClusterDataHolder clusterDataHolder = entry.getValue();
            String clusterId = clusterDataHolder.getClusterId();
            String serviceType = clusterDataHolder.getServiceType();
            TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
            Cluster topLevelCluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);
            applicationInstanceBean.getClusterInstances().add(ObjectConverter.
                    convertClusterToClusterInstanceBean(applicationInstanceBean.getInstanceId(),
                            topLevelCluster, entry.getKey()));
        }
    }

    private static void addClustersInstancesToGroupInstanceBean(
            GroupInstanceBean groupInstanceBean,
            Group group) {
        Map<String, ClusterDataHolder> topLevelClusterDataMap = group.getClusterDataMap();
        for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
            ClusterDataHolder clusterDataHolder = entry.getValue();
            String clusterId = clusterDataHolder.getClusterId();
            String serviceType = clusterDataHolder.getServiceType();
            TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
            Cluster topLevelCluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);
            groupInstanceBean.getClusterInstances().add(ObjectConverter.
                    convertClusterToClusterInstanceBean(groupInstanceBean.getInstanceId(),
                            topLevelCluster, entry.getKey()));
        }
    }


    private static void setSubGroups(Group group, GroupBean groupBean) {
        Collection<Group> subgroups = group.getGroups();
        addClustersToGroupBean(group, groupBean);
        for (Group subGroup : subgroups) {
            GroupBean subGroupBean = ObjectConverter.convertGroupToGroupBean(subGroup);

            setSubGroups(subGroup, subGroupBean);
            groupBean.addGroup(subGroupBean);
        }
    }
    private static void setSubGroupInstances(Group group, GroupInstanceBean groupInstanceBean) {
        Collection<Group> subgroups = group.getGroups();
        addClustersInstancesToGroupInstanceBean(groupInstanceBean, group);
        for (Group subGroup : subgroups) {
            List<GroupInstanceBean> groupInstanceBeans = ObjectConverter.
                    convertGroupToGroupInstancesBean(groupInstanceBean.getInstanceId(),
                            subGroup);
            for(GroupInstanceBean groupInstanceBean1 : groupInstanceBeans) {
                setSubGroupInstances(subGroup, groupInstanceBean1);
                groupInstanceBean.getGroupInstances().add(groupInstanceBean1);
            }

        }
    }

    private static void addClustersToGroupBean(Group group, GroupBean groupBean) {
        Map<String, ClusterDataHolder> clustersDatamap = group.getClusterDataMap();
        for (Map.Entry<String, ClusterDataHolder> x : clustersDatamap.entrySet()) {
            ClusterDataHolder clusterHolder = x.getValue();
            Cluster topLevelCluster = TopologyManager.getTopology().getService(clusterHolder.getServiceType()).getCluster(clusterHolder.getClusterId());
            groupBean.addCluster(ObjectConverter.convertClusterToClusterBean(topLevelCluster, null));
        }
    }

    // Util methods for Kubernetes clusters
    
    public static boolean addKubernetesGroup(KubernetesGroup kubernetesGroupBean) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup kubernetesGroup =
                    ObjectConverter.convertToCCKubernetesGroupPojo(kubernetesGroupBean);

            try {
                return cloudControllerServiceClient.deployKubernetesGroup(kubernetesGroup);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesGroupExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesGroupException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static boolean addKubernetesHost(String kubernetesGroupId, KubernetesHost kubernetesHostBean)
            throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost kubernetesHost =
                    ObjectConverter.convertKubernetesHostToStubKubernetesHost(kubernetesHostBean);

            try {
                return cloudControllerServiceClient.deployKubernetesHost(kubernetesGroupId, kubernetesHost);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesGroupExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesGroupException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static boolean updateKubernetesMaster(KubernetesMaster kubernetesMasterBean) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster kubernetesMaster =
                    ObjectConverter.convertStubKubernetesMasterToKubernetesMaster(kubernetesMasterBean);

            try {
                return cloudControllerServiceClient.updateKubernetesMaster(kubernetesMaster);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesMasterExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesMasterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesMasterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesMasterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static KubernetesGroup[] getAvailableKubernetesGroups() throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup[]
                        kubernetesGroups = cloudControllerServiceClient.getAvailableKubernetesGroups();
                return ObjectConverter.convertStubKubernetesGroupsToKubernetesGroups(kubernetesGroups);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return null;
    }

    public static KubernetesGroup getKubernetesGroup(String kubernetesGroupId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesGroup
                        kubernetesGroup = cloudControllerServiceClient.getKubernetesGroup(kubernetesGroupId);
                return ObjectConverter.convertStubKubernetesGroupToKubernetesGroup(kubernetesGroup);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesGroupExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesGroupException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    public static boolean removeKubernetesGroup(String kubernetesGroupId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                return cloudControllerServiceClient.undeployKubernetesGroup(kubernetesGroupId);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesGroupExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesGroupException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static boolean removeKubernetesHost(String kubernetesHostId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                return cloudControllerServiceClient.undeployKubernetesHost(kubernetesHostId);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static KubernetesHost[] getKubernetesHosts(String kubernetesGroupId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost[]
                        kubernetesHosts = cloudControllerServiceClient.getKubernetesHosts(kubernetesGroupId);

                List<KubernetesHost> arrayList = ObjectConverter.convertStubKubernetesHostsToKubernetesHosts(kubernetesHosts);
                KubernetesHost[] array = new KubernetesHost[arrayList.size()];
                array = arrayList.toArray(array);
                return array;
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesGroupExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesGroupException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    public static KubernetesMaster getKubernetesMaster(String kubernetesGroupId) throws RestAPIException {
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesMaster
                        kubernetesMaster = cloudControllerServiceClient.getKubernetesMaster(kubernetesGroupId);
                return ObjectConverter.convertStubKubernetesMasterToKubernetesMaster(kubernetesMaster);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesGroupExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesGroupException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    public static boolean updateKubernetesHost(KubernetesHost kubernetesHostBean) throws RestAPIException {
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.kubernetes.KubernetesHost kubernetesHost =
                    ObjectConverter.convertKubernetesHostToStubKubernetesHost(kubernetesHostBean);
            try {
                return cloudControllerServiceClient.updateKubernetesHost(kubernetesHost);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static void updateSubscriptionProperties(ConfigurationContext context, String alias, List<PropertyBean> property) throws RestAPIException {
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(context)
                        , alias);
                if (cluster == null) {
                    throw new RestAPIException("No matching cluster found for [alias] " + alias);
                }
                if (property != null) {
                    autoscalerServiceClient.updateClusterMonitor(cluster.getClusterId(), ObjectConverter.convertPropertyBeansToProperties(property));
                }
            } catch (AutoScalerServiceInvalidArgumentExceptionException e) {
                String message = e.getFaultMessage().getInvalidArgumentException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (RemoteException e) {
                String msg = "Error while connecting to Autoscaler Service. " + e.getMessage();
                log.error(msg, e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
    }
}
