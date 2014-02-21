/*
*  Copyright (c) 2005-2012, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.stratos.rest.endpoint.services;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.deploy.service.ServiceDeploymentManager;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.dto.SubscriptionInfo;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.DataCartridgeSubscription;
import org.apache.stratos.manager.subscription.SubscriptionData;
import org.apache.stratos.manager.subscription.utils.CartridgeSubscriptionUtils;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.rest.endpoint.bean.CartridgeInfoBean;
import org.apache.stratos.rest.endpoint.bean.StratosAdminResponse;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.util.converter.PojoConverter;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import javax.ws.rs.core.Response;
import java.util.*;
import java.util.regex.Pattern;

public class ServiceUtils {
    public static final String IS_VOLUME_REQUIRED = "volume.required";
    public static final String SHOULD_DELETE_VOLUME = "volume.delete.on.unsubscription";
    public static final String VOLUME_SIZE = "volume.size.gb";
    public static final String DEVICE_NAME = "volume.device.name";

    private static Log log = LogFactory.getLog(ServiceUtils.class);
    private static CartridgeSubscriptionManager cartridgeSubsciptionManager = new CartridgeSubscriptionManager();
    private static ServiceDeploymentManager serviceDeploymentManager = new ServiceDeploymentManager();

    static StratosAdminResponse deployCartridge (CartridgeDefinitionBean cartridgeDefinitionBean, ConfigurationContext ctxt,
        String userName, String tenantDomain) throws RestAPIException {

        log.info("Starting to deploy a Cartridge [type] "+cartridgeDefinitionBean.type);

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        
        if (cloudControllerServiceClient != null) {

            CartridgeConfig cartridgeConfig = PojoConverter.populateCartridgeConfigPojo(cartridgeDefinitionBean);

            if(cartridgeConfig == null) {
                throw new RestAPIException("Populated CartridgeConfig instance is null, cartridge deployment aborted");
            }

            try {
                
                // call CC
                cloudControllerServiceClient.deployCartridgeDefinition(cartridgeConfig);
                
                log.info("Successfully deployed Cartridge [type] "+cartridgeDefinitionBean.type);
                
            } catch (Exception e) {
                String errorMsg = "Error while deploying cartridge definition";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed cartridge definition with type " + cartridgeDefinitionBean.type);
        return stratosAdminResponse;
    }

    @SuppressWarnings("unused")
    private static DeploymentPolicy[] intersection(
        DeploymentPolicy[] cartridgeDepPolicies,
        DeploymentPolicy[] lbCartridgeDepPolicies) {
        
        List<DeploymentPolicy> commonPolicies = 
                new ArrayList<DeploymentPolicy>();
        for (DeploymentPolicy policy1 
                : cartridgeDepPolicies) {
            for (DeploymentPolicy policy2 
                    : lbCartridgeDepPolicies) {
                if(policy1.equals(policy2)) {
                    commonPolicies.add(policy1);
                }
            }
            
        }
        return commonPolicies.toArray(new DeploymentPolicy[0]);
    }
    
    static StratosAdminResponse undeployCartridge(String cartridgeType) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                cloudControllerServiceClient.unDeployCartridgeDefinition(cartridgeType);

            } catch (Exception e) {
                String errorMsg = "Error while undeploying cartridge definition for type " + cartridgeType;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully undeployed cartridge definition with type " + cartridgeType);
        return stratosAdminResponse;
    }


    public static StratosAdminResponse deployPartition(Partition partitionBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.cloud.controller.deployment.partition.Partition partition =
                    PojoConverter.convertToCCPartitionPojo(partitionBean);

            try {
                autoscalerServiceClient.deployPartition(partition);

            } catch (Exception e) {
                String errorMsg = "Error in deploying partition definition";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed partition definition with id " + partitionBean.id);
        return stratosAdminResponse;
    }

    public static StratosAdminResponse deployAutoscalingPolicy(AutoscalePolicy autoscalePolicyBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.policy.model.AutoscalePolicy autoscalePolicy = PojoConverter.
                    convertToCCAutoscalerPojo(autoscalePolicyBean);

            try {
                autoscalerServiceClient.deployAutoscalingPolicy(autoscalePolicy);

            } catch (Exception e) {
                String errorMsg = "Error in deploying autoscaling policy definition";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed autoscaling policy definition with id " + autoscalePolicyBean.getId());
        return stratosAdminResponse;
    }

    public static StratosAdminResponse deployDeploymentPolicy(
            org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy deploymentPolicyBean)
                throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy deploymentPolicy =
                    PojoConverter.convetToCCDeploymentPolicyPojo(deploymentPolicyBean);

            try {
                autoscalerServiceClient.deployDeploymentPolicy(deploymentPolicy);

            } catch (Exception e) {
                String errorMsg = "Error in deploying deployment policy definition";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed deployment policy definition with type " + deploymentPolicyBean.id);
        return stratosAdminResponse;
    }

    private static CloudControllerServiceClient getCloudControllerServiceClient () throws RestAPIException {

        try {
            return CloudControllerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error in getting CloudControllerServiceClient instance to connect to the Cloud Controller";
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg);
        }
    }

    public static Partition[] getAvailablePartitions () throws RestAPIException {

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitions = autoscalerServiceClient.getAvailablePartitions();

            } catch (Exception e) {
                String errorMsg = "Error getting available partitions";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populatePartitionPojos(partitions);
    }

    public static Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId) 
                throws RestAPIException {

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitions =
                             autoscalerServiceClient.getPartitionsOfDeploymentPolicy(deploymentPolicyId);

            } catch (Exception e) {
                String errorMsg = "Error getting available partitions for deployment policy id " + deploymentPolicyId;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populatePartitionPojos(partitions);
    }
    
    public static Partition[]
        getPartitionsOfGroup(String deploymentPolicyId, String groupId) throws RestAPIException {

        org.apache.stratos.cloud.controller.deployment.partition.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitions =
                             autoscalerServiceClient.getPartitionsOfGroup(deploymentPolicyId, groupId);

            } catch (Exception e) {
                String errorMsg = "Error getting available partitions for deployment policy id " + deploymentPolicyId +
                        ", group id " + groupId;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populatePartitionPojos(partitions);
    }

    public static Partition getPartition (String partitionId) throws RestAPIException {

        org.apache.stratos.cloud.controller.deployment.partition.Partition partition = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partition = autoscalerServiceClient.getPartition(partitionId);

            } catch (Exception e) {
                String errorMsg = "Error getting partition for id " + partitionId;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populatePartitionPojo(partition);
    }

    private static AutoscalerServiceClient getAutoscalerServiceClient () throws RestAPIException {

        try {
            return AutoscalerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error in getting AutoscalerServiceClient instance to connect to the Autoscaler";
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg);
        }
    }

    public static AutoscalePolicy[] getAutoScalePolicies () throws RestAPIException {

        org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[] autoscalePolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalePolicies = autoscalerServiceClient.getAutoScalePolicies();

            } catch (Exception e) {
                String errorMsg = "Error getting available autoscaling policies. " + "Backend error is : " + e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populateAutoscalePojos(autoscalePolicies);
    }

    public static AutoscalePolicy getAutoScalePolicy (String autoscalePolicyId) throws RestAPIException {

        org.apache.stratos.autoscaler.policy.model.AutoscalePolicy autoscalePolicy = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalePolicy = autoscalerServiceClient.getAutoScalePolicy(autoscalePolicyId);

            } catch (Exception e) {
                String errorMsg = "Error getting information for autoscaling policy with id " + autoscalePolicyId;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populateAutoscalePojo(autoscalePolicy);
    }

    public static org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy[] 
            getDeploymentPolicies () throws RestAPIException {

        DeploymentPolicy [] deploymentPolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                deploymentPolicies = autoscalerServiceClient.getDeploymentPolicies();

            } catch (Exception e) {
                String errorMsg = "Error getting available deployment policies";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populateDeploymentPolicyPojos(deploymentPolicies);
    }

    public static org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy[] 
            getDeploymentPolicies (String cartridgeType) throws RestAPIException {

        DeploymentPolicy [] deploymentPolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                deploymentPolicies = autoscalerServiceClient.getDeploymentPolicies(cartridgeType);

            } catch (Exception e) {
                String errorMsg = "Error getting available deployment policies for cartridge type " + cartridgeType;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populateDeploymentPolicyPojos(deploymentPolicies);
    }

    public static org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy 
    getDeploymentPolicy(String deploymentPolicyId) throws RestAPIException {

        DeploymentPolicy deploymentPolicy = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                deploymentPolicy = autoscalerServiceClient.getDeploymentPolicy(deploymentPolicyId);

            } catch (Exception e) {
                String errorMsg = "Error getting deployment policy with id " + deploymentPolicyId;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populateDeploymentPolicyPojo(deploymentPolicy);
    }

    public static PartitionGroup[] getPartitionGroups (String deploymentPolicyId)
            throws RestAPIException{

        org.apache.stratos.autoscaler.partition.PartitionGroup [] partitionGroups = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitionGroups = autoscalerServiceClient.getPartitionGroups(deploymentPolicyId);

            } catch (Exception e) {
                String errorMsg = "Error getting available partition groups for deployment policy id " + deploymentPolicyId;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg);
            }
        }

        return PojoConverter.populatePartitionGroupPojos(partitionGroups);
    }

    static Cartridge getAvailableCartridgeInfo(String cartridgeType, Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<Cartridge> cartridges;

        if(multiTenant != null) {
            cartridges = getAvailableCartridges(null, multiTenant, configurationContext);
        } else {
            cartridges = getAvailableCartridges(null, null, configurationContext);
        }
        for(Cartridge cartridge : cartridges) {
            if(cartridge.getCartridgeType().equals(cartridgeType)) {
                return cartridge;
            }
        }
         throw new RestAPIException("cannot find the required cartridge type " + cartridgeType) ;
    }

    static List<Cartridge> getAvailableLbCartridges(Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
       List<Cartridge> cartridges = getAvailableCartridges(null, multiTenant, configurationContext);
        List<Cartridge> lbCartridges = new ArrayList<Cartridge>();
        for(Cartridge cartridge : cartridges) {
            if(cartridge.isLoadBalancer()) {
               lbCartridges.add(cartridge);
            }
        }
        return lbCartridges;
    }

    static List<Cartridge> getAvailableCartridges(String cartridgeSearchString, Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<Cartridge> cartridges = new ArrayList<Cartridge>();

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

                    if (!ServiceUtils.cartridgeMatches(cartridgeInfo, searchPattern)) {
                        continue;
                    }

                    Cartridge cartridge = new Cartridge();
                    cartridge.setCartridgeType(cartridgeType);
                    cartridge.setProvider(cartridgeInfo.getProvider());
                    cartridge.setDisplayName(cartridgeInfo.getDisplayName());
                    cartridge.setDescription(cartridgeInfo.getDescription());
                    cartridge.setVersion(cartridgeInfo.getVersion());
                    cartridge.setMultiTenant(cartridgeInfo.getMultiTenant());
                    cartridge.setHostName(cartridgeInfo.getHostName());
                    cartridge.setDefaultAutoscalingPolicy(cartridgeInfo.getDefaultAutoscalingPolicy());
                    cartridge.setDefaultDeploymentPolicy(cartridgeInfo.getDefaultDeploymentPolicy());
                    //cartridge.setStatus(CartridgeConstants.NOT_SUBSCRIBED);
                    cartridge.setCartridgeAlias("-");
                    cartridge.setPersistence(cartridgeInfo.getPersistence());

                    if(cartridgeInfo.getLbConfig() != null && cartridgeInfo.getProperties() != null) {
                        for(Property property: cartridgeInfo.getProperties()) {
                        if(property.getName().equals("load.balancer")) {
                            cartridge.setLoadBalancer(true);
                        }
                        }
                    }
                    //cartridge.setActiveInstances(0);
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
            String msg = "Error when getting available cartridges";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning available cartridges " + cartridges.size());
        }

        return cartridges;
    }

    private static boolean isAlreadySubscribed(String cartridgeType,
			int tenantId) {
		
    	Collection<CartridgeSubscription> subscriptionList = cartridgeSubsciptionManager.isCartridgeSubscribed(tenantId, cartridgeType);
    	if(subscriptionList == null || subscriptionList.isEmpty()){
    		return false;	
    	}else {
    		return true;
    	}		
	}

    public static List<ServiceDefinitionBean> getdeployedServiceInformation () throws RestAPIException {

        Collection<Service> services = null;

        try {
            services = serviceDeploymentManager.getServices();

        } catch (ADCException e) {
            String msg = "Error in getting deployed service information ";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        if (services != null && !services.isEmpty()) {
            return PojoConverter.convertToServiceDefinitionBeans(services);
        }

        return null;
    }

    public static ServiceDefinitionBean getDeployedServiceInformation (String type) throws RestAPIException {

        Service service = null;

        try {
            service = serviceDeploymentManager.getService(type);

        } catch (ADCException e) {
            String msg = "Error in getting deployed service cluster definition " + type;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        if (service != null) {
            return PojoConverter.convertToServiceDefinitionBean(service);
        }

        return new ServiceDefinitionBean();
    }

    public static List<Cartridge> getActiveDeployedServiceInformation (ConfigurationContext configurationContext) throws RestAPIException {

        Collection<Service> services = null;

        try {
            services = serviceDeploymentManager.getServices();

        } catch (ADCException e) {
            String msg = "Error in getting deployed service cluster definition ";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<Cartridge> availableMultitenantCartridges = new ArrayList<Cartridge>();
        int tenantId = ApplicationManagementUtil.getTenantId(configurationContext);
        //getting the services for the tenantId
        for(Service service : services) {
            String tenantRange = service.getTenantRange();
            if(tenantRange.equals(Constants.TENANT_RANGE_ALL)) {
                //check whether any active instances found for this service in the Topology

                Cluster cluster = TopologyManager.getTopology().getService(service.getType()).
                                        getCluster(service.getClusterId());
                boolean activeMemberFound = false;
                for(Member member : cluster.getMembers()) {
                    if(member.isActive()) {
                        activeMemberFound = true;
                        break;
                    }
                }
                if(activeMemberFound) {
                    availableMultitenantCartridges.add(getAvailableCartridgeInfo(null, true, configurationContext));
                }
            } else {
                //TODO have to check for the serivces which has correct tenant range
            }
        }

        return availableMultitenantCartridges;
    }

	static List<Cartridge> getSubscriptions (String cartridgeSearchString, ConfigurationContext configurationContext) throws RestAPIException {

        List<Cartridge> cartridges = new ArrayList<Cartridge>();

        if (log.isDebugEnabled()) {
            log.debug("Getting subscribed cartridges. Search String: " + cartridgeSearchString);
        }

        try {
            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

            Collection<CartridgeSubscription> subscriptions = cartridgeSubsciptionManager.getCartridgeSubscriptions(ApplicationManagementUtil.
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
                    Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext)
                            ,cartridge.getCartridgeType(), cartridge.getCartridgeAlias());
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
                    cartridges.add(cartridge);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no subscribed cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error when getting subscribed cartridges";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning subscribed cartridges " + cartridges.size());
        }

        return cartridges;
    }

    
    static Cartridge getSubscription(String cartridgeAlias, ConfigurationContext configurationContext) throws RestAPIException {
    	
    	Cartridge cartridge =  getCartridgeFromSubscription(cartridgeSubsciptionManager.getCartridgeSubscription(ApplicationManagementUtil.
                    getTenantId(configurationContext), cartridgeAlias));
    	
    	if (cartridge == null) {
    		throw new RestAPIException(Response.Status.NOT_FOUND, "Unregistered [alias]: "+cartridgeAlias+"! Please enter a valid alias.");
    	}
        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext)
                ,cartridge.getCartridgeType(), cartridge.getCartridgeAlias());
        String cartridgeStatus = "Inactive";
        int activeMemberCount = 0;
        Collection<Member> members = cluster.getMembers();
        for (Member member : members) {
			if(member.isActive()) {
				cartridgeStatus = "Active";
				activeMemberCount++;
			}
		}        
        cartridge.setActiveInstances(activeMemberCount);
		cartridge.setStatus(cartridgeStatus);
		return cartridge;
    	
    }

    static int getActiveInstances(String cartridgeType, String cartridgeAlias, ConfigurationContext configurationContext) throws RestAPIException {
    	int noOfActiveInstances = 0;
        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext)
                ,cartridgeType , cartridgeAlias);
        for(Member member : cluster.getMembers()) {
            if(member.getStatus().toString().equals(MemberStatus.Activated)) {
                noOfActiveInstances ++;
            }
        }
		return noOfActiveInstances;
    }
    
    private static Cartridge getCartridgeFromSubscription (CartridgeSubscription subscription) throws RestAPIException {

    	if (subscription == null) {
    		return null;
    	}
    	
        Cartridge cartridge = new Cartridge();
        cartridge.setCartridgeType(subscription.getCartridgeInfo().getType());
        cartridge.setMultiTenant(subscription.getCartridgeInfo().getMultiTenant());
        cartridge.setProvider(subscription.getCartridgeInfo().getProvider());
        cartridge.setVersion(subscription.getCartridgeInfo().getVersion());
        cartridge.setDescription(subscription.getCartridgeInfo().getDescription());
        cartridge.setDisplayName(subscription.getCartridgeInfo().getDisplayName());
        cartridge.setCartridgeAlias(subscription.getAlias());
        cartridge.setHostName(subscription.getHostName());
        cartridge.setMappedDomain(subscription.getMappedDomain());
        if (subscription.getRepository() != null) {
            cartridge.setRepoURL(subscription.getRepository().getUrl());
        }

        if (subscription instanceof DataCartridgeSubscription) {
            DataCartridgeSubscription dataCartridgeSubscription = (DataCartridgeSubscription) subscription;
            cartridge.setDbHost(dataCartridgeSubscription.getDBHost());
            cartridge.setDbUserName(dataCartridgeSubscription.getDBUsername());
            cartridge.setPassword(dataCartridgeSubscription.getDBPassword());
        }

        if (subscription.getLbClusterId() != null && !subscription.getLbClusterId().isEmpty()) {
            cartridge.setLbClusterId(subscription.getLbClusterId());
        }

        cartridge.setStatus(subscription.getSubscriptionStatus());
        cartridge.setPortMappings(subscription.getCartridgeInfo().getPortMappings());
        return cartridge;
    }

    static Pattern getSearchStringPattern(String searchString) {
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

    static boolean cartridgeMatches(CartridgeInfo cartridgeInfo, Pattern pattern) {
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

    static boolean cartridgeMatches(CartridgeInfo cartridgeInfo, CartridgeSubscription cartridgeSubscription, Pattern pattern) {
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

    static SubscriptionInfo subscribeToCartridge (CartridgeInfoBean cartridgeInfoBean, ConfigurationContext configurationContext, String tenantUsername,
                                                  String tenantDomain) throws RestAPIException {

        try {
            return subscribe(cartridgeInfoBean, configurationContext, tenantUsername, tenantDomain);

        } catch (Exception e) {
            throw new RestAPIException(e.getMessage());
        }
    }

    private static SubscriptionInfo subscribe (CartridgeInfoBean cartridgeInfoBean, ConfigurationContext configurationContext, String tenantUsername, String tenantDomain)
                                       throws ADCException, PolicyException, UnregisteredCartridgeException,
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, RepositoryRequiredException,
            AlreadySubscribedException, RepositoryCredentialsRequiredException, InvalidRepositoryException,
            RepositoryTransportException, RestAPIException {
        // LB cartridges won't go thru this method.

        //TODO: this is a temp fix. proper fix is to move this logic to CartridgeSubscriptionManager
        // validate cartridge alias
        CartridgeSubscriptionUtils.validateCartridgeAlias(ApplicationManagementUtil.getTenantId(configurationContext), cartridgeInfoBean.getCartridgeType(), cartridgeInfoBean.getAlias());

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        CloudControllerServiceClient cloudControllerServiceClient =
                                                                    getCloudControllerServiceClient();
        CartridgeInfo cartridgeInfo;

        try {
            cartridgeInfo = cloudControllerServiceClient.getCartridgeInfo(cartridgeInfoBean.getCartridgeType());
        } catch (Exception e) {
            String msg = "Cannot get cartridge info: " + cartridgeInfoBean.getCartridgeType();
            log.error(msg, e);
            throw new ADCException(msg, e);
        }

        String cartridgeType = cartridgeInfoBean.getCartridgeType();
        String deploymentPolicy = cartridgeInfoBean.getDeploymentPolicy();
        String autoscalingPolicy = cartridgeInfoBean.getAutoscalePolicy();
        String dataCartridgeAlias = cartridgeInfoBean.getDataCartridgeAlias();

        SubscriptionData subscriptionData = new SubscriptionData();
        subscriptionData.setCartridgeType(cartridgeType);
        subscriptionData.setCartridgeAlias(cartridgeInfoBean.getAlias().trim());
        subscriptionData.setDataCartridgeAlias(dataCartridgeAlias);
        subscriptionData.setAutoscalingPolicyName(autoscalingPolicy);
        subscriptionData.setDeploymentPolicyName(deploymentPolicy);
        subscriptionData.setTenantDomain(tenantDomain);
        subscriptionData.setTenantId(ApplicationManagementUtil.getTenantId(configurationContext));
        subscriptionData.setTenantAdminUsername(tenantUsername);
        subscriptionData.setRepositoryType("git");
        subscriptionData.setRepositoryURL(cartridgeInfoBean.getRepoURL());
        subscriptionData.setRepositoryUsername(cartridgeInfoBean.getRepoURL());
        subscriptionData.setRepositoryPassword(cartridgeInfoBean.getRepoPassword());

        Properties properties = new Properties();
        if(cartridgeInfoBean.isPersistanceRequired()){
            /*
            PersistanceMapping persistanceMapping = new PersistanceMapping();
            persistanceMapping.setPersistanceRequired(persistanceMappingBean.persistanceRequired);
            persistanceMapping.setSize(persistanceMappingBean.size);
            persistanceMapping.setDevice(persistanceMappingBean.device);
            persistanceMapping.setRemoveOntermination(persistanceMappingBean.removeOnTermination);
            subscriptionData.setPersistanceMapping(persistanceMapping);
            */

            // Add persistance mapping properties to the subscription.
            Property persistanceRequiredProperty = new Property();
            persistanceRequiredProperty.setName(IS_VOLUME_REQUIRED);
            persistanceRequiredProperty.setValue(String.valueOf(cartridgeInfoBean.isPersistanceRequired()));

            Property sizeProperty = new Property();
            sizeProperty.setName(VOLUME_SIZE);
            sizeProperty.setValue(cartridgeInfoBean.getSize());

            Property deleteOnTerminationProperty = new Property();
            deleteOnTerminationProperty.setName(SHOULD_DELETE_VOLUME);
            deleteOnTerminationProperty.setValue(String.valueOf(    cartridgeInfoBean.isRemoveOnTermination()));

            properties.setProperties(new Property[]{persistanceRequiredProperty,sizeProperty, deleteOnTerminationProperty});
        }


        // If multitenant, return for now. TODO -- fix properly
        if(cartridgeInfo != null && cartridgeInfo.getMultiTenant()) {
               log.info(" ******* MT cartridge ******* ");

            subscriptionData.setPrivateRepository(false);
            subscriptionData.setLbClusterId(null);
            subscriptionData.setProperties(null);

            CartridgeSubscription cartridgeSubscription =
                                        cartridgeSubsciptionManager.subscribeToCartridgeWithProperties(subscriptionData);
               log.info(" --- ** -- ");
              return cartridgeSubsciptionManager.registerCartridgeSubscription(cartridgeSubscription, properties);
                       
        }
        
        List<Property> lbRefProp = new ArrayList<Property>();

        // get lb config reference
        LoadbalancerConfig lbConfig = cartridgeInfo.getLbConfig();
        String lbClusterId = null;

        if (lbConfig == null || lbConfig.getProperties() == null) {
            if (log.isDebugEnabled()) {
                log.debug("This Service does not require a load balancer. " + "[Service Name] " +
                          cartridgeType);
            }
        } else {

            Properties lbReferenceProperties = lbConfig.getProperties();

            Property property = new Property();
            property.setName(org.apache.stratos.messaging.util.Constants.LOAD_BALANCER_REF);


            for (org.apache.stratos.cloud.controller.pojo.Property prop : lbReferenceProperties.getProperties()) {

                String name = prop.getName();
                String value = prop.getValue();

                // TODO make following a chain of responsibility pattern
                if (Constants.NO_LOAD_BALANCER.equals(name)) {
                    if ("true".equals(value)) {
                        if (log.isDebugEnabled()) {
                            log.debug("This cartridge does not require a load balancer. " +
                                      "[Type] " + cartridgeType);
                        }
                        property.setValue(name);
                        lbRefProp.add(property);
                        break;
                    }
                } else if (Constants.EXISTING_LOAD_BALANCERS.equals(name)) {
                    String clusterIdsVal = value;
                    if (log.isDebugEnabled()) {
                        log.debug("This cartridge refers to existing load balancers. " + "[Type] " +
                                  cartridgeType + "[Referenced Cluster Ids] " + clusterIdsVal);
                    }

                    String[] clusterIds = clusterIdsVal.split(",");

                    for (String clusterId : clusterIds) {
                        if (autoscalerServiceClient != null) {
                            try {
                                autoscalerServiceClient.checkLBExistenceAgainstPolicy(clusterId,
                                                                                      deploymentPolicy);
                            } catch (Exception ex) {
                                // we don't need to throw the error here.
                                log.error(ex.getMessage(), ex);
                            }
                        }
                    }

                    property.setValue(name);
                    lbRefProp.add(property);
                    break;

                } else if (Constants.DEFAULT_LOAD_BALANCER.equals(name)) {

                    if ("true".equals(value)) {

                        CartridgeInfo lbCartridgeInfo = null;
                        String lbCartridgeType = lbConfig.getType();
                        try {
                            // retrieve lb Cartridge info
                            if(lbCartridgeType != null) {
                                lbCartridgeInfo = cloudControllerServiceClient.getCartridgeInfo(lbCartridgeType);
                            }
                        } catch (Exception e) {
                            String msg = "Cannot get cartridge info: " + cartridgeType;
                            log.error(msg, e);
                            throw new ADCException(msg, e);
                        }

                        property.setValue(name);
                        if (log.isDebugEnabled()) {
                            log.debug("This cartridge uses default load balancer. " + "[Type] " +
                                      cartridgeType);
                        }
                        if (autoscalerServiceClient != null) {
                            try {
                                // get the valid policies for lb cartridge
                                DeploymentPolicy[] lbCartridgeDepPolicies =
                                                                            autoscalerServiceClient.getDeploymentPolicies(lbCartridgeType);
                                // traverse deployment policies of lb cartridge
                                for (DeploymentPolicy policy : lbCartridgeDepPolicies) {
                                    // check existence of the subscribed policy
                                    if (deploymentPolicy.equals(policy.getId())) {

                                        if (!autoscalerServiceClient.checkDefaultLBExistenceAgainstPolicy(deploymentPolicy)) {

                                            // if lb cluster doesn't exist
                                            String lbAlias = "lb" + new Random().nextInt();
                                            if(lbCartridgeInfo != null) {
                                               lbCartridgeInfo.addProperties(property);
                                            lbClusterId = subscribeToLb(lbCartridgeType, cartridgeType,
                                                          lbAlias,
                                                          lbCartridgeInfo.getDefaultAutoscalingPolicy(),
                                                          deploymentPolicy, configurationContext,
                                                    tenantUsername, tenantDomain,
                                                          lbCartridgeInfo.getProperties());
                                            } else {
                                                String msg = "Please specify a LB cartridge type for the cartridge: "
                                                                + cartridgeType + " as category: " +
                                                                Constants.DEFAULT_LOAD_BALANCER;
                                                log.error(msg);
                                                throw new ADCException(msg);
                                            }
                                        }
                                    }
                                }

                            } catch (Exception ex) {
                                // we don't need to throw the error here.
                                log.error(ex.getMessage(), ex);
                            }
                        }

                        lbRefProp.add(property);
                        break;
                    }

                } else if (Constants.SERVICE_AWARE_LOAD_BALANCER.equals(name)) {

                    if ("true".equals(value)) {

                        CartridgeInfo lbCartridgeInfo = null;
                        String lbCartridgeType = lbConfig.getType();
                        try {
                            // retrieve lb Cartridge info
                            if(lbCartridgeType != null) {
                                lbCartridgeInfo = cloudControllerServiceClient.getCartridgeInfo(lbCartridgeType);
                            }
                        } catch (Exception e) {
                            String msg = "Cannot get cartridge info: " + cartridgeType;
                            log.error(msg, e);
                            throw new ADCException(msg, e);
                        }

                        // add a property for the service type
                        Property loadBalancedServiceTypeProperty = new Property();
                        loadBalancedServiceTypeProperty.setName(Constants.LOAD_BALANCED_SERVICE_TYPE);

                        property.setValue(name);
                        // set the load balanced service type
                        loadBalancedServiceTypeProperty.setValue(cartridgeType);
                        if (log.isDebugEnabled()) {
                            log.debug("This cartridge uses a service aware load balancer. " +
                                    "[Type] " + cartridgeType);
                        }
                        if (autoscalerServiceClient != null) {
                            try {

                                // get the valid policies for lb cartridge
                                DeploymentPolicy[] lbCartridgeDepPolicies =
                                        autoscalerServiceClient.getDeploymentPolicies(lbCartridgeType);
                                // traverse deployment policies of lb cartridge
                                for (DeploymentPolicy policy : lbCartridgeDepPolicies) {
                                    // check existence of the subscribed policy
                                    if (deploymentPolicy.equals(policy.getId())) {

                                        if (!autoscalerServiceClient.checkServiceLBExistenceAgainstPolicy(cartridgeType,
                                                deploymentPolicy)) {

                                            // if lb cluster doesn't exist
                                            String lbAlias =
                                                    "lb" + cartridgeType +
                                                            new Random().nextInt();

                                            if(lbCartridgeInfo != null) {
                                                lbCartridgeInfo.addProperties(property);
                                                lbCartridgeInfo.addProperties(loadBalancedServiceTypeProperty);

                                                lbClusterId = subscribeToLb(lbCartridgeType, cartridgeType,
                                                    lbAlias,
                                                    lbCartridgeInfo.getDefaultAutoscalingPolicy(),
                                                    deploymentPolicy,
                                                    configurationContext, tenantUsername,
                                                    tenantDomain,
                                                    lbCartridgeInfo.getProperties());
                                            } else {
                                                String msg = "Please specify a LB cartridge type for the cartridge: "
                                                                + cartridgeType + " as category: " +
                                                                Constants.SERVICE_AWARE_LOAD_BALANCER;
                                                log.error(msg);
                                                throw new ADCException(msg);
                                            }
                                        }
                                    }
                                }

                            } catch (Exception ex) {
                                // we don't need to throw the error here.
                                log.error(ex.getMessage(), ex);
                            }
                        }

                        lbRefProp.add(property);
                        break;
                    }
                }
            }
        }

        subscriptionData.setPrivateRepository(cartridgeInfoBean.isPrivateRepo());
        subscriptionData.setLbClusterId(lbClusterId);
        subscriptionData.setProperties(lbRefProp.toArray(new Property[0]));
        CartridgeSubscription cartridgeSubscription =
                                                      cartridgeSubsciptionManager.subscribeToCartridgeWithProperties(subscriptionData);


        if (dataCartridgeAlias != null && !dataCartridgeAlias.trim().isEmpty()) {

            /*dataCartridgeAlias = dataCartridgeAlias.trim();

            CartridgeSubscription connectingCartridgeSubscription = null;
            try {
                connectingCartridgeSubscription =
                                                  cartridgeSubsciptionManager.getCartridgeSubscription(tenantDomain,
                                                                                                       dataCartridgeAlias);

            } catch (NotSubscribedException e) {
                log.error(e.getMessage(), e);
            }
            if (connectingCartridgeSubscription != null) {
                try {
                    cartridgeSubsciptionManager.connectCartridges(tenantDomain,
                                                                  cartridgeSubscription,
                                                                  connectingCartridgeSubscription.getAlias());

                } catch (NotSubscribedException e) {
                    log.error(e.getMessage(), e);

                } catch (AxisFault axisFault) {
                    log.error(axisFault.getMessage(), axisFault);
                }
            } else {
                log.error("Failed to connect. No cartridge subscription found for tenant " +
                          ApplicationManagementUtil.getTenantId(configurationContext) +
                          " with alias " + alias);
            }*/
        }

        for (Property lbRefProperty : lbRefProp) {
            properties.addProperties(lbRefProperty);
        }

        SubscriptionInfo registerCartridgeSubscription = cartridgeSubsciptionManager.registerCartridgeSubscription(cartridgeSubscription, properties);
        
        return registerCartridgeSubscription;

    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster getCluster (String cartridgeType, String subscriptionAlias, ConfigurationContext configurationContext) throws RestAPIException {

        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext)
                ,cartridgeType , subscriptionAlias);
        if(cluster == null) {
            throw new RestAPIException("No matching cluster found for [cartridge type]: "+cartridgeType+ " [alias] "+subscriptionAlias);
        } else{
            return PojoConverter.populateClusterPojos(cluster);
        }
    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster[] getClustersForTenant (ConfigurationContext configurationContext) {

        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext), null);
        ArrayList<org.apache.stratos.rest.endpoint.bean.topology.Cluster> clusters =
                new ArrayList<org.apache.stratos.rest.endpoint.bean.topology.Cluster>();
        for(Cluster cluster : clusterSet) {
            clusters.add(PojoConverter.populateClusterPojos(cluster));
        }
        org.apache.stratos.rest.endpoint.bean.topology.Cluster[] arrCluster =
                new org.apache.stratos.rest.endpoint.bean.topology.Cluster[clusters.size()];
        arrCluster = clusters.toArray(arrCluster);
        return arrCluster;

    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster[] getClustersForTenantAndCartridgeType (ConfigurationContext configurationContext,
                                                                  String cartridgeType) {

        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext), cartridgeType);
        List<org.apache.stratos.rest.endpoint.bean.topology.Cluster> clusters =
                new ArrayList<org.apache.stratos.rest.endpoint.bean.topology.Cluster>();
        for(Cluster cluster : clusterSet) {
            clusters.add(PojoConverter.populateClusterPojos(cluster));
        }
         org.apache.stratos.rest.endpoint.bean.topology.Cluster[] arrCluster =
                new org.apache.stratos.rest.endpoint.bean.topology.Cluster[clusters.size()];
        arrCluster = clusters.toArray(arrCluster);
        return arrCluster;



    }

    // return the cluster id for the lb. This is a temp fix.
    private static String subscribeToLb(String cartridgeType, String loadBalancedCartridgeType, String lbAlias,
        String defaultAutoscalingPolicy, String deploymentPolicy,
        ConfigurationContext configurationContext, String userName, String tenantDomain, Property[] props) throws ADCException {

        CartridgeSubscription cartridgeSubscription;

        try {
            if(log.isDebugEnabled()) {
                log.debug("Subscribing to a load balancer [cartridge] "+cartridgeType+" [alias] "+lbAlias);
            }

            SubscriptionData subscriptionData = new SubscriptionData();
            subscriptionData.setCartridgeType(cartridgeType);
            subscriptionData.setCartridgeAlias(lbAlias.trim());
            subscriptionData.setAutoscalingPolicyName(defaultAutoscalingPolicy);
            subscriptionData.setDeploymentPolicyName(deploymentPolicy);
            subscriptionData.setTenantDomain(tenantDomain);
            subscriptionData.setTenantId(ApplicationManagementUtil.getTenantId(configurationContext));
            subscriptionData.setTenantAdminUsername(userName);
            subscriptionData.setRepositoryType("git");
            //subscriptionData.setProperties(props);
            subscriptionData.setPrivateRepository(false);

            cartridgeSubscription =
                    cartridgeSubsciptionManager.subscribeToCartridgeWithProperties(subscriptionData);

            //set a payload parameter to indicate the load balanced cartridge type
            cartridgeSubscription.getPayloadData().add("LOAD_BALANCED_SERVICE_TYPE", loadBalancedCartridgeType);

            Properties lbProperties = new Properties();
            lbProperties.setProperties(props);
            cartridgeSubsciptionManager.registerCartridgeSubscription(cartridgeSubscription, lbProperties);
            
            if(log.isDebugEnabled()) {
                log.debug("Successfully subscribed to a load balancer [cartridge] "+cartridgeType+" [alias] "+lbAlias);
            }
        } catch (Exception e) {
            String msg = "Error while subscribing to load balancer cartridge [type] "+cartridgeType;
            log.error(msg, e);
            throw new ADCException(msg, e);
        }

        return cartridgeSubscription.getClusterDomain();
    }

    static StratosAdminResponse unsubscribe(String alias, String tenantDomain) throws RestAPIException {

        try {
            cartridgeSubsciptionManager.unsubscribeFromCartridge(tenantDomain, alias);

        } catch (ADCException e) {
            throw new RestAPIException(e.getMessage(), e);

        } catch (NotSubscribedException e) {
            String msg = "Error in terminating subscription with alias  "+ alias;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully terminated the subscription with alias " + alias);
        return stratosAdminResponse;
    }
    
    /**
     * 
     * Super tenant will deploy multitenant service. 
     * 
     * get domain , subdomain as well..
     * @param clusterDomain
     * @param clusterSubdomain
     * 
     */
    static StratosAdminResponse deployService(String cartridgeType, String alias, String autoscalingPolicy, String deploymentPolicy,
                                              String tenantDomain, String tenantUsername, int tenantId, String clusterDomain, String clusterSubdomain, String tenantRange) throws RestAPIException {
    	log.info("Deploying service..");
    	try {
    		serviceDeploymentManager.deployService(cartridgeType, autoscalingPolicy, deploymentPolicy, tenantId, tenantRange, tenantDomain, tenantUsername);

		} catch (Exception e) {
            String msg = "Error in deploying service cluster definition";
            log.error(msg, e);
            throw new RestAPIException(msg);
		}

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully deployed service cluster definition with type " + cartridgeType);
        return stratosAdminResponse;
    }

    static StratosAdminResponse undeployService(String serviceType) throws RestAPIException {

        try {
            serviceDeploymentManager.undeployService(serviceType);

        } catch (Exception e) {
            String msg = "Error in undeploying service cluster definition for type " + serviceType;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        StratosAdminResponse stratosAdminResponse = new StratosAdminResponse();
        stratosAdminResponse.setMessage("Successfully undeployed service cluster definition for service type " + serviceType);
        return stratosAdminResponse;
    }
}
