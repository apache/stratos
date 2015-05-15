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
package org.apache.stratos.rest.endpoint.api;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.AutoscalerServiceAutoScalingPolicyAlreadyExistExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceCartridgeAlreadyExistsExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidCartridgeDefinitionExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidCartridgeTypeExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidIaasProviderExceptionException;
import org.apache.stratos.cloud.controller.stub.domain.Cartridge;
import org.apache.stratos.common.beans.ResponseMessageBean;
import org.apache.stratos.common.beans.artifact.repository.GitNotificationPayloadBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.partition.PartitionBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.common.client.CloudControllerServiceClient;
import org.apache.stratos.common.client.StratosManagerServiceClient;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.util.converter.ObjectConverter;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility class for API v4.0.
 * Please do not update this API utility class, if modifications are needed use the latest API version utility class.
 */
@Deprecated
public class StratosApiV40Utils {
    public static final String IS_VOLUME_REQUIRED = "volume.required";
    public static final String SHOULD_DELETE_VOLUME = "volume.delete.on.unsubscription";
    public static final String VOLUME_SIZE = "volume.size.gb";
    public static final String DEVICE_NAME = "volume.device.name";
    public static final String TENANT_RANGE_ALL = "*";

    private static Log log = LogFactory.getLog(StratosApiV40Utils.class);

    static ResponseMessageBean deployCartridge(CartridgeBean cartridgeDefinitionBean, ConfigurationContext ctxt,
                                               String userName, String tenantDomain) throws RestAPIException {

        log.info("Starting to deploy a Cartridge [type] " + cartridgeDefinitionBean.getType());

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();

        if (cloudControllerServiceClient != null) {

            Cartridge cartridgeConfig = ObjectConverter.convertCartridgeBeanToStubCartridgeConfig(cartridgeDefinitionBean);

            if (cartridgeConfig == null) {
                throw new RestAPIException("Populated CartridgeConfig instance is null, cartridge deployment aborted");
            }


            // call CC
            try {
                cloudControllerServiceClient.addCartridge(cartridgeConfig);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidCartridgeDefinitionExceptionException e) {
                String message = e.getFaultMessage().getInvalidCartridgeDefinitionException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceCartridgeAlreadyExistsExceptionException e) {
                String message = e.getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
                String message = e.getFaultMessage().getInvalidIaasProviderException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }

        ResponseMessageBean stratosApiResponse = new ResponseMessageBean();
        String message = "Successfully added cartridge definition: [cartridge-type] " + cartridgeDefinitionBean.getType();
        stratosApiResponse.setMessage(message);
        if (log.isInfoEnabled()) {
            log.info(message);
        }
        return stratosApiResponse;
    }

//    @SuppressWarnings("unused")
//    private static DeploymentPolicy[] intersection(
//            DeploymentPolicy[] cartridgeDepPolicies,
//            DeploymentPolicy[] lbCartridgeDepPolicies) {
//
//        List<DeploymentPolicy> commonPolicies =
//                new ArrayList<DeploymentPolicy>();
//        for (DeploymentPolicy policy1
//                : cartridgeDepPolicies) {
//            for (DeploymentPolicy policy2
//                    : lbCartridgeDepPolicies) {
//                if(policy1.equals(policy2)) {
//                    commonPolicies.add(policy1);
//                }
//            }
//
//        }
//        return commonPolicies.toArray(new DeploymentPolicy[0]);
//    }

    static ResponseMessageBean undeployCartridge(String cartridgeType) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
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

        ResponseMessageBean stratosApiResponse = new ResponseMessageBean();
        stratosApiResponse.setMessage("Successfully undeployed cartridge definition with type " + cartridgeType);
        return stratosApiResponse;
    }

    public static ResponseMessageBean deployAutoscalingPolicy(AutoscalePolicyBean autoscalePolicyBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.stub.autoscale.policy.AutoscalePolicy autoscalePolicy = ObjectConverter.
                    convertToCCAutoscalerPojo(autoscalePolicyBean);

            try {
                autoscalerServiceClient
                        .addAutoscalingPolicy(autoscalePolicy);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (AutoscalerServiceAutoScalingPolicyAlreadyExistExceptionException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }

        }

        ResponseMessageBean stratosApiResponse = new ResponseMessageBean();
        stratosApiResponse.setMessage("Successfully deployed autoscaling policy definition with id " + autoscalePolicyBean.getId());
        return stratosApiResponse;
    }

    private static CloudControllerServiceClient getCloudControllerServiceClient() throws RestAPIException {

        try {
            return CloudControllerServiceClient.getInstance();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting CloudControllerServiceClient instance to connect to the "
                    + "Cloud Controller. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    public static PartitionBean[] getAvailablePartitions() throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.domain.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
//            try {
//                partitions = autoscalerServiceClient.getAvailablePartitions();
//
//            } catch (RemoteException e) {
//                String errorMsg = "Error while getting available partitions. Cause : " + e.getMessage();
//                log.error(errorMsg, e);
//                throw new RestAPIException(errorMsg, e);
//            }
        }

        return ObjectConverter.populatePartitionPojos(partitions);
    }

    public static PartitionBean[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId)
            throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.domain.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
//        if (autoscalerServiceClient != null) {
//            try {
//                partitions =
//                        autoscalerServiceClient.getPartitionsOfDeploymentPolicy(deploymentPolicyId);
//
//            } catch (RemoteException e) {
//                String errorMsg = "Error while getting available partitions for deployment policy id " +
//                        deploymentPolicyId+". Cause: "+e.getMessage();
//                log.error(errorMsg, e);
//                throw new RestAPIException(errorMsg, e);
//            }
//        }

        return ObjectConverter.populatePartitionPojos(partitions);
    }

    public static PartitionBean[] getPartitionsOfGroup(String deploymentPolicyId, String groupId) throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.domain.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
//        if (autoscalerServiceClient != null) {
//            try {
//                partitions =
//                        autoscalerServiceClient.getPartitionsOfGroup(deploymentPolicyId, groupId);
//
//            } catch (RemoteException e) {
//                String errorMsg = "Error while getting available partitions for deployment policy id " + deploymentPolicyId +
//                        ", group id " + groupId+". Cause: "+e.getMessage();
//                log.error(errorMsg, e);
//                throw new RestAPIException(errorMsg, e);
//            }
//        }

        return ObjectConverter.populatePartitionPojos(partitions);
    }

    public static PartitionBean getPartition(String partitionId) throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.domain.Partition partition = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
//        if (autoscalerServiceClient != null) {
//            try {
//                partition = autoscalerServiceClient.getPartition(partitionId);
//
//            } catch (RemoteException e) {
//                String errorMsg = "Error while getting partition for id " + partitionId+". Cause: "+e.getMessage();
//                log.error(errorMsg, e);
//                throw new RestAPIException(errorMsg, e);
//            }
//        }

        return ObjectConverter.populatePartitionPojo(partition);
    }

    private static AutoscalerServiceClient getAutoscalerServiceClient() throws RestAPIException {

        try {
            return AutoscalerServiceClient.getInstance();
        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting AutoscalerServiceClient instance to connect to the "
                    + "Autoscaler";
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    public static AutoscalePolicyBean[] getAutoScalePolicies() throws RestAPIException {

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

    public static AutoscalePolicyBean getAutoScalePolicy(String autoscalePolicyId) throws RestAPIException {

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

//    public static DeploymentPolicyBean getDeploymentPolicy(String deploymentPolicyId) throws RestAPIException {
//
//        DeploymentPolicy deploymentPolicy = null;
//
//
//        return null;
//    }

    static CartridgeBean getAvailableCartridgeInfo(String cartridgeType, Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeBean> cartridges = getAvailableCartridges(null, multiTenant, configurationContext);
        for (CartridgeBean cartridge : cartridges) {
            if (cartridge.getType().equals(cartridgeType)) {
                return cartridge;
            }
        }
        String msg = "Unavailable cartridge type: " + cartridgeType;
        log.error(msg);
        throw new RestAPIException(msg);
    }

    static List<CartridgeBean> getAvailableLbCartridges(Boolean multiTenant,
                                                        ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeBean> cartridges = getAvailableCartridges(null, multiTenant,
                configurationContext);
        List<CartridgeBean> lbCartridges = new ArrayList<CartridgeBean>();
        for (CartridgeBean cartridge : cartridges) {
            if (cartridge.getCategory().equals("LoadBalancer")) {
                lbCartridges.add(cartridge);
            }
        }

		/*if(lbCartridges == null || lbCartridges.isEmpty()) {
            String msg = "Load balancer Cartridges are not available.";
	        log.error(msg);
	        throw new RestAPIException(msg) ;
		}*/
        return lbCartridges;
    }

    static List<CartridgeBean> getAvailableCartridges(String cartridgeSearchString, Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeBean> cartridges = new ArrayList<CartridgeBean>();

        if (log.isDebugEnabled()) {
            log.debug("Getting available cartridges. Search String: " + cartridgeSearchString + ", Multi-Tenant: " + multiTenant);
        }

        boolean allowMultipleSubscription = Boolean.valueOf(System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

        try {
            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

            String[] availableCartridges = CloudControllerServiceClient.getInstance().getRegisteredCartridges();

            if (availableCartridges != null) {
                for (String cartridgeType : availableCartridges) {
                    Cartridge cartridgeInfo = null;
                    try {
                        cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridge(cartridgeType);
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

                    if (!StratosApiV40Utils.cartridgeMatches(cartridgeInfo, searchPattern)) {
                        continue;
                    }

                    CartridgeBean cartridge = new CartridgeBean();
                    cartridge.setType(cartridgeType);
                    cartridge.setProvider(cartridgeInfo.getProvider());
                    cartridge.setDisplayName(cartridgeInfo.getDisplayName());
                    cartridge.setDescription(cartridgeInfo.getDescription());
                    cartridge.setVersion(cartridgeInfo.getVersion());
                    cartridge.setMultiTenant(cartridgeInfo.getMultiTenant());
                    cartridge.setHost(cartridgeInfo.getHostName());
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

    private static boolean isAlreadySubscribed(String cartridgeType, int tenantId) {

//        Collection<CartridgeSubscription> subscriptionList = cartridgeSubsciptionManager.isCartridgeSubscribed(tenantId, cartridgeType);
//        if(subscriptionList == null || subscriptionList.isEmpty()){
//            return false;
//        }else {
//            return true;
//        }
        return false;
    }

//    public static List<ServiceDefinitionBean> getdeployedServiceInformation () throws RestAPIException {
//
//        Collection<Service> services = null;
//
//        try {
//            services = serviceDeploymentManager.getServices();
//
//        } catch (ADCException e) {
//            String msg = "Unable to get deployed service information. Cause: "+e.getMessage();
//            log.error(msg, e);
//            throw new RestAPIException(msg, e);
//        }
//
//        if (services != null && !services.isEmpty()) {
//            return ObjectConverter.convertToServiceDefinitionBeans(services);
//        }
//
//        return null;
//    }

//    public static ServiceDefinitionBean getDeployedServiceInformation (String type) throws RestAPIException {
//
//        Service service = null;
//
//        try {
//            service = serviceDeploymentManager.getService(type);
//
//        } catch (ADCException e) {
//            String msg = "Unable to get deployed service information for [type]: " + type+". Cause: "+e.getMessage();
//            log.error(msg, e);
//            throw new RestAPIException(msg, e);
//        }
//
//        if (service != null) {
//            return ObjectConverter.convertToServiceDefinitionBean(service);
//        }
//
//        return new ServiceDefinitionBean();
//    }

//    public static List<Cartridge> getActiveDeployedServiceInformation (ConfigurationContext configurationContext) throws RestAPIException {
//
//        Collection<Service> services = null;
//
//        try {
//            services = serviceDeploymentManager.getServices();
//
//        } catch (ADCException e) {
//            String msg = "Unable to get deployed service information. Cause: "+e.getMessage();
//            log.error(msg, e);
//            throw new RestAPIException(msg, e);
//        }
//
//        List<Cartridge> availableMultitenantCartridges = new ArrayList<Cartridge>();
//        int tenantId = ApplicationManagementUtil.getTenantId(configurationContext);
//        //getting the services for the tenantId
//        for(Service service : services) {
//            String tenantRange = service.getTenantRange();
//            if(tenantRange.equals(TENANT_RANGE_ALL)) {
//                //check whether any active instances found for this service in the Topology
//
//                Cluster cluster = TopologyManager.getTopology().getService(service.getType()).
//                        getCluster(service.getClusterId());
//                boolean activeMemberFound = false;
//                for(Member member : cluster.getMembers()) {
//                    if(member.isActive()) {
//                        activeMemberFound = true;
//                        break;
//                    }
//                }
//                if(activeMemberFound) {
//                    availableMultitenantCartridges.add(getAvailableCartridgeInfo(null, true, configurationContext));
//                }
//            } else {
//                //TODO have to check for the serivces which has correct tenant range
//            }
//        }
//
//		/*if (availableMultitenantCartridges.isEmpty()) {
//			String msg = "Cannot find any active deployed service for tenant [id] "+tenantId;
//			log.error(msg);
//			throw new RestAPIException(msg);
//		}*/
//
//        return availableMultitenantCartridges;
//    }

//    static List<Cartridge> getSubscriptions (String cartridgeSearchString, String serviceGroup, ConfigurationContext configurationContext) throws RestAPIException {
//
//        List<Cartridge> cartridges = new ArrayList<Cartridge>();
//
//        if (log.isDebugEnabled()) {
//            log.debug("Getting subscribed cartridges. Search String: " + cartridgeSearchString);
//        }
//
//        try {
//            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);
//
//            Collection<CartridgeSubscription> subscriptions = cartridgeSubsciptionManager.getCartridgeSubscriptions(ApplicationManagementUtil.
//                    getTenantId(configurationContext), null);
//
//            if (subscriptions != null && !subscriptions.isEmpty()) {
//
//                for (CartridgeSubscription subscription : subscriptions) {
//
//                    if (!cartridgeMatches(subscription.getCartridgeInfo(), subscription, searchPattern)) {
//                        continue;
//                    }
//                    Cartridge cartridge = getCartridgeFromSubscription(subscription);
//                    if (cartridge == null) {
//                        continue;
//                    }
//                    Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
//                            cartridge.getCartridgeAlias());
//                    String cartridgeStatus = "Inactive";
//                    int activeMemberCount = 0;
//                    if (cluster != null) {
//                        Collection<Member> members = cluster.getMembers();
//                        for (Member member : members) {
//                            if (member.isActive()) {
//                                cartridgeStatus = "Active";
//                                activeMemberCount++;
//                            }
//                        }
//                    }
//                    cartridge.setActiveInstances(activeMemberCount);
//                    cartridge.setStatus(cartridgeStatus);
//
//                    // Ignoring the LB cartridges since they are not shown to the user.
//                    if(cartridge.isLoadBalancer())
//                        continue;
//                    if(StringUtils.isNotEmpty(serviceGroup) && cartridge.getServiceGroup() != null &&
//                            !cartridge.getServiceGroup().equals(serviceGroup)){
//                        continue;
//                    }
//                    cartridges.add(cartridge);
//                }
//            } else {
//                if (log.isDebugEnabled()) {
//                    log.debug("There are no subscribed cartridges");
//                }
//            }
//        } catch (Exception e) {
//            String msg = "Error while getting subscribed cartridges. Cause: "+e.getMessage();
//            log.error(msg, e);
//            throw new RestAPIException(msg, e);
//        }
//
//        Collections.sort(cartridges);
//
//        if (log.isDebugEnabled()) {
//            log.debug("Returning subscribed cartridges " + cartridges.size());
//        }
//
//        /*if(cartridges.isEmpty()) {
//        	String msg = "Cannot find any subscribed Cartridge, matching the given string: "+cartridgeSearchString;
//            log.error(msg);
//            throw new RestAPIException(msg);
//        }*/
//
//        return cartridges;
//    }

//    static Cartridge getSubscription(String cartridgeAlias, ConfigurationContext configurationContext) throws RestAPIException {
//
//        Cartridge cartridge =  getCartridgeFromSubscription(cartridgeSubsciptionManager.getCartridgeSubscription(ApplicationManagementUtil.
//                getTenantId(configurationContext), cartridgeAlias));
//
//        if (cartridge == null) {
//            String message = "Unregistered [alias]: "+cartridgeAlias+"! Please enter a valid alias.";
//            log.error(message);
//            throw new RestAPIException(Response.Status.NOT_FOUND, message);
//        }
//        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
//                cartridge.getCartridgeAlias());
//        String cartridgeStatus = "Inactive";
//        int activeMemberCount = 0;
//
//        // cluster might not be created yet, so need to check
//        if (cluster != null) {
//            Collection<Member> members = cluster.getMembers();
//            if (members != null ) {
//                for (Member member : members) {
//                    if(member.isActive()) {
//                        cartridgeStatus = "Active";
//                        activeMemberCount++;
//                    }
//                }
//            }
//        }
//
//        cartridge.setActiveInstances(activeMemberCount);
//        cartridge.setStatus(cartridgeStatus);
//        return cartridge;
//
//    }

//    static int getActiveInstances(String cartridgeType, String cartridgeAlias, ConfigurationContext configurationContext) throws RestAPIException {
//        int noOfActiveInstances = 0;
//        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
//                cartridgeAlias);
//
//        if(cluster == null) {
//            String message = "No Cluster found for cartridge [type] "+cartridgeType+", [alias] "+cartridgeAlias;
//            log.error(message);
//            throw new RestAPIException(message);
//        }
//
//        for(Member member : cluster.getMembers()) {
//            if(member.getStatus().toString().equals(MemberStatus.Active)) {
//                noOfActiveInstances ++;
//            }
//        }
//        return noOfActiveInstances;
//    }

//    private static Cartridge getCartridgeFromSubscription(CartridgeSubscription subscription) throws RestAPIException {
//
//        if (subscription == null) {
//            return null;
//        }
//        try {
//            Cartridge cartridge = new Cartridge();
//            cartridge.setCartridgeType(subscription.getCartridgeInfo()
//                    .getType());
//            cartridge.setMultiTenant(subscription.getCartridgeInfo()
//                    .getMultiTenant());
//            cartridge
//                    .setProvider(subscription.getCartridgeInfo().getProvider());
//            cartridge.setVersion(subscription.getCartridgeInfo().getVersion());
//            cartridge.setDescription(subscription.getCartridgeInfo()
//                    .getDescription());
//            cartridge.setDisplayName(subscription.getCartridgeInfo()
//                    .getDisplayName());
//            cartridge.setCartridgeAlias(subscription.getAlias());
//            cartridge.setHostName(subscription.getHostName());
//            cartridge.setMappedDomain(subscription.getMappedDomain());
//            if (subscription.getRepository() != null) {
//                cartridge.setRepoURL(subscription.getRepository().getUrl());
//            }
//
//            if (subscription instanceof DataCartridgeSubscription) {
//                DataCartridgeSubscription dataCartridgeSubscription = (DataCartridgeSubscription) subscription;
//                cartridge.setDbHost(dataCartridgeSubscription.getDBHost());
//                cartridge.setDbUserName(dataCartridgeSubscription
//                        .getDBUsername());
//                cartridge
//                        .setPassword(dataCartridgeSubscription.getDBPassword());
//            }
//
//            if (subscription.getLbClusterId() != null
//                    && !subscription.getLbClusterId().isEmpty()) {
//                cartridge.setLbClusterId(subscription.getLbClusterId());
//            }
//
//            cartridge.setStatus(subscription.getSubscriptionStatus());
//            cartridge.setPortMappings(subscription.getCartridgeInfo()
//                    .getPortMappings());
//
//            if(subscription.getCartridgeInfo().getLbConfig() != null && subscription.getCartridgeInfo().getProperties() != null) {
//                for(org.apache.stratos.cloud.controller.stub.Property property: subscription.getCartridgeInfo().getProperties()) {
//                    if(property.getName().equals("load.balancer")) {
//                        cartridge.setLoadBalancer(true);
//                    }
//                }
//            }
//            if(subscription.getCartridgeInfo().getServiceGroup() != null) {
//                cartridge.setServiceGroup(subscription.getCartridgeInfo().getServiceGroup());
//            }
//            return cartridge;
//
//        } catch (Exception e) {
//            String msg = "Unable to extract the Cartridge from subscription. Cause: "+e.getMessage();
//            log.error(msg);
//            throw new RestAPIException(msg);
//        }
//
//    }

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

    static boolean cartridgeMatches(Cartridge cartridgeInfo, Pattern pattern) {
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

//    static boolean cartridgeMatches(CartridgeInfo cartridgeInfo, CartridgeSubscription cartridgeSubscription, Pattern pattern) {
//        if (pattern != null) {
//            boolean matches = false;
//            if (cartridgeInfo.getDisplayName() != null) {
//                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
//            }
//            if (!matches && cartridgeInfo.getDescription() != null) {
//                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
//            }
//            if (!matches && cartridgeSubscription.getType() != null) {
//                matches = pattern.matcher(cartridgeSubscription.getType().toLowerCase()).find();
//            }
//            if (!matches && cartridgeSubscription.getAlias() != null) {
//                matches = pattern.matcher(cartridgeSubscription.getAlias().toLowerCase()).find();
//            }
//            return matches;
//        }
//        return true;
//    }

//    public static CartridgeSubscription getCartridgeSubscription(String alias, ConfigurationContext configurationContext) {
//        return cartridgeSubsciptionManager.getCartridgeSubscription(ApplicationManagementUtil.getTenantId(configurationContext), alias);
//    }
//
//    public static org.apache.stratos.common.beans.topology.Cluster getCluster (String cartridgeType, String subscriptionAlias, ConfigurationContext configurationContext) throws RestAPIException {
//
//        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
//                subscriptionAlias);
//        if(cluster == null) {
//            throw new RestAPIException("No matching cluster found for [cartridge type]: "+cartridgeType+ " [alias] "+subscriptionAlias);
//        } else{
//            return ObjectConverter.convertClusterToClusterBean(cluster, null);
//        }
//    }

//    public static org.apache.stratos.common.beans.topology.Cluster[] getClustersForTenant (ConfigurationContext configurationContext) {
//
//        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
//                getTenantId(configurationContext), null);
//        ArrayList<org.apache.stratos.common.beans.topology.Cluster> clusters =
//                new ArrayList<org.apache.stratos.common.beans.topology.Cluster>();
//        for(Cluster cluster : clusterSet) {
//            clusters.add(ObjectConverter.convertClusterToClusterBean(cluster, null));
//        }
//        org.apache.stratos.common.beans.topology.Cluster[] arrCluster =
//                new org.apache.stratos.common.beans.topology.Cluster[clusters.size()];
//        arrCluster = clusters.toArray(arrCluster);
//        return arrCluster;
//
//    }

//    public static org.apache.stratos.common.beans.topology.Cluster[] getClustersForTenantAndCartridgeType (ConfigurationContext configurationContext,
//                                                                                                                 String cartridgeType) {
//
//        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
//                getTenantId(configurationContext), cartridgeType);
//        List<org.apache.stratos.common.beans.topology.Cluster> clusters =
//                new ArrayList<org.apache.stratos.common.beans.topology.Cluster>();
//        for(Cluster cluster : clusterSet) {
//            clusters.add(ObjectConverter.convertClusterToClusterBean(cluster, null));
//        }
//        org.apache.stratos.common.beans.topology.Cluster[] arrCluster =
//                new org.apache.stratos.common.beans.topology.Cluster[clusters.size()];
//        arrCluster = clusters.toArray(arrCluster);
//        return arrCluster;
//
//    }

//    public static org.apache.stratos.common.beans.topology.Cluster[] getClustersForCartridgeType(String cartridgeType) {
//
//        Set<Cluster> clusterSet = TopologyClusterInformationModel
//                .getInstance()
//                .getClusters(cartridgeType);
//        List<org.apache.stratos.common.beans.topology.Cluster> clusters = new ArrayList<org.apache.stratos.common.beans.topology.Cluster>();
//        for (Cluster cluster : clusterSet) {
//            clusters.add(ObjectConverter.convertClusterToClusterBean(cluster, null));
//        }
//        org.apache.stratos.common.beans.topology.Cluster[] arrCluster = new org.apache.stratos.common.beans.topology.Cluster[clusters
//                .size()];
//        arrCluster = clusters.toArray(arrCluster);
//        return arrCluster;
//
//    }

    // return the cluster id for the lb. This is a temp fix.
    /*private static String subscribeToLb(String cartridgeType, String loadBalancedCartridgeType, String lbAlias,
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
            String msg = "Error while subscribing to load balancer cartridge [type] "+cartridgeType+". Cause: "+e.getMessage();
            log.error(msg, e);
            throw new ADCException(msg, e);
        }
        return cartridgeSubscription.getClusterDomain();
    } */

    static ResponseMessageBean unsubscribe(String alias, String tenantDomain) throws RestAPIException {
        throw new RestAPIException("Not implemented");
    }

    static void notifyArtifactUpdatedEvent(GitNotificationPayloadBean payload) throws RestAPIException {
        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.notifyArtifactUpdatedEventForRepository(payload.getRepository().getUrl());
        } catch (Exception e) {
            String message = "Could not send artifact updated event";
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static ResponseMessageBean removeSubscriptionDomain(ConfigurationContext configurationContext, String cartridgeType,
                                                               String subscriptionAlias, String domain) throws RestAPIException {
        throw new RestAPIException("Not implemented");
    }
}
