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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.stub.policy.model.DeploymentPolicy;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeConfig;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceInvalidPartitionExceptionException;
import org.apache.stratos.autoscaler.stub.AutoScalerServiceInvalidPolicyExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidCartridgeDefinitionExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidCartridgeTypeExceptionException;
import org.apache.stratos.cloud.controller.stub.CloudControllerServiceInvalidIaasProviderExceptionException;
import org.apache.stratos.manager.client.AutoscalerServiceClient;
import org.apache.stratos.manager.client.CloudControllerServiceClient;
import org.apache.stratos.manager.deploy.service.Service;
import org.apache.stratos.manager.deploy.service.ServiceDeploymentManager;
import org.apache.stratos.manager.dto.Cartridge;
import org.apache.stratos.manager.exception.*;
import org.apache.stratos.manager.manager.CartridgeSubscriptionManager;
import org.apache.stratos.manager.repository.RepositoryNotification;
import org.apache.stratos.manager.subscription.CartridgeSubscription;
import org.apache.stratos.manager.subscription.DataCartridgeSubscription;
import org.apache.stratos.manager.topology.model.TopologyClusterInformationModel;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.domain.topology.Member;
import org.apache.stratos.messaging.domain.topology.MemberStatus;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.messaging.util.Constants;
import org.apache.stratos.rest.endpoint.bean.StratosApiResponse;
import org.apache.stratos.rest.endpoint.bean.SubscriptionDomainRequest;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.ServiceDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.repositoryNotificationInfoBean.Payload;
import org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean;
import org.apache.stratos.rest.endpoint.bean.util.converter.PojoConverter;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.rmi.RemoteException;
import java.util.*;
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

    private static Log log = LogFactory.getLog(StratosApiV40Utils.class);
    private static CartridgeSubscriptionManager cartridgeSubsciptionManager = new CartridgeSubscriptionManager();
    private static ServiceDeploymentManager serviceDeploymentManager = new ServiceDeploymentManager();

    static StratosApiResponse deployCartridge (CartridgeDefinitionBean cartridgeDefinitionBean, ConfigurationContext ctxt,
                                                 String userName, String tenantDomain) throws RestAPIException {

        log.info("Starting to deploy a Cartridge [type] "+cartridgeDefinitionBean.type);

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();

        if (cloudControllerServiceClient != null) {

            CartridgeConfig cartridgeConfig = PojoConverter.populateCartridgeConfigPojo(cartridgeDefinitionBean);

            if(cartridgeConfig == null) {
                throw new RestAPIException("Populated CartridgeConfig instance is null, cartridge deployment aborted");
            }


            // call CC
            try {
                cloudControllerServiceClient
                        .deployCartridgeDefinition(cartridgeConfig);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidCartridgeDefinitionExceptionException e) {
                String message = e.getFaultMessage().getInvalidCartridgeDefinitionException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceInvalidIaasProviderExceptionException e) {
                String message = e.getFaultMessage().getInvalidIaasProviderException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }

            log.info("Successfully deployed Cartridge [type] "+cartridgeDefinitionBean.type);

        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed cartridge definition with type " + cartridgeDefinitionBean.type);
        return stratosApiResponse;
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

    static StratosApiResponse undeployCartridge(String cartridgeType) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                cloudControllerServiceClient.unDeployCartridgeDefinition(cartridgeType);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidCartridgeTypeExceptionException e) {
                String msg = e.getFaultMessage().getInvalidCartridgeTypeException().getMessage();
                log.error(msg, e);
                throw new RestAPIException(msg, e);
            }

        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully undeployed cartridge definition with type " + cartridgeType);
        return stratosApiResponse;
    }


    public static StratosApiResponse deployPartition(Partition partitionBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.cloud.controller.stub.deployment.partition.Partition partition =
                    PojoConverter.convertToCCPartitionPojo(partitionBean);

            try {
                autoscalerServiceClient.deployPartition(partition);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (AutoScalerServiceInvalidPartitionExceptionException e) {
                String message = e.getFaultMessage().getInvalidPartitionException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }

        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed partition definition with id " + partitionBean.id);
        return stratosApiResponse;
    }

    public static StratosApiResponse deployAutoscalingPolicy(AutoscalePolicy autoscalePolicyBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy autoscalePolicy = PojoConverter.
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

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed autoscaling policy definition with id " + autoscalePolicyBean.getId());
        return stratosApiResponse;
    }

    public static StratosApiResponse deployDeploymentPolicy(
            org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy deploymentPolicyBean)
            throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            DeploymentPolicy deploymentPolicy =
                    PojoConverter.convetToCCDeploymentPolicyPojo(deploymentPolicyBean);

            try {
                autoscalerServiceClient
                        .deployDeploymentPolicy(deploymentPolicy);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (AutoScalerServiceInvalidPolicyExceptionException e) {
                String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }

        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed deployment policy definition with type "
                + deploymentPolicyBean.getId());
        return stratosApiResponse;
    }

    private static CloudControllerServiceClient getCloudControllerServiceClient () throws RestAPIException {

        try {
            return CloudControllerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting CloudControllerServiceClient instance to connect to the "
                    + "Cloud Controller. Cause: "+axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    public static Partition[] getAvailablePartitions () throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitions = autoscalerServiceClient.getAvailablePartitions();

            } catch (RemoteException e) {
                String errorMsg = "Error while getting available partitions. Cause : " + e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionPojos(partitions);
    }

    public static Partition[] getPartitionsOfDeploymentPolicy(String deploymentPolicyId)
            throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitions =
                        autoscalerServiceClient.getPartitionsOfDeploymentPolicy(deploymentPolicyId);

            } catch (RemoteException e) {
                String errorMsg = "Error while getting available partitions for deployment policy id " +
                        deploymentPolicyId+". Cause: "+e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionPojos(partitions);
    }

    public static Partition[]
    getPartitionsOfGroup(String deploymentPolicyId, String groupId) throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition[] partitions = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitions =
                        autoscalerServiceClient.getPartitionsOfGroup(deploymentPolicyId, groupId);

            } catch (RemoteException e) {
                String errorMsg = "Error while getting available partitions for deployment policy id " + deploymentPolicyId +
                        ", group id " + groupId+". Cause: "+e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionPojos(partitions);
    }

    public static Partition getPartition (String partitionId) throws RestAPIException {

        org.apache.stratos.cloud.controller.stub.deployment.partition.Partition partition = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partition = autoscalerServiceClient.getPartition(partitionId);

            } catch (RemoteException e) {
                String errorMsg = "Error while getting partition for id " + partitionId+". Cause: "+e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionPojo(partition);
    }

    private static AutoscalerServiceClient getAutoscalerServiceClient () throws RestAPIException {

        try {
            return AutoscalerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting AutoscalerServiceClient instance to connect to the "
                    + "Autoscaler. Cause: "+axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    public static AutoscalePolicy[] getAutoScalePolicies () throws RestAPIException {

        org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy[] autoscalePolicies = null;
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
        return PojoConverter.populateAutoscalePojos(autoscalePolicies);
    }

    public static AutoscalePolicy getAutoScalePolicy (String autoscalePolicyId) throws RestAPIException {

        org.apache.stratos.autoscaler.stub.policy.model.AutoscalePolicy autoscalePolicy = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalePolicy = autoscalerServiceClient.getAutoScalePolicy(autoscalePolicyId);

            } catch (RemoteException e) {
                String errorMsg = "Error while getting information for autoscaling policy with id " +
                        autoscalePolicyId+".  Cause: "+e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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
            } catch (RemoteException e) {
                String errorMsg = "Error getting available deployment policies. Cause : " + e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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

            } catch (RemoteException e) {
                String errorMsg = "Error while getting available deployment policies for cartridge type " +
                        cartridgeType+". Cause: "+e.getMessage();;
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        if(deploymentPolicies.length == 0) {
            String errorMsg = "Cannot find any matching deployment policy for Cartridge [type] "+cartridgeType;
            log.error(errorMsg);
            throw new RestAPIException(errorMsg);
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

            } catch (RemoteException e) {
                String errorMsg = "Error while getting deployment policy with id " +
                        deploymentPolicyId+". Cause: "+e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        if(deploymentPolicy == null) {
            String errorMsg = "Cannot find a matching deployment policy for [id] "+deploymentPolicyId;
            log.error(errorMsg);
            throw new RestAPIException(errorMsg);
        }

        return PojoConverter.populateDeploymentPolicyPojo(deploymentPolicy);
    }

    public static PartitionGroup[] getPartitionGroups (String deploymentPolicyId)
            throws RestAPIException{

        org.apache.stratos.autoscaler.stub.partition.PartitionGroup[] partitionGroups = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                partitionGroups = autoscalerServiceClient.getPartitionGroups(deploymentPolicyId);

            } catch (RemoteException e) {
                String errorMsg = "Error getting available partition groups for deployment policy id "
                        + deploymentPolicyId+". Cause: "+e.getMessage();
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionGroupPojos(partitionGroups);
    }

    static Cartridge getAvailableCartridgeInfo(String cartridgeType, Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<Cartridge> cartridges = getAvailableCartridges(null, multiTenant, configurationContext);
        for(Cartridge cartridge : cartridges) {
            if(cartridge.getCartridgeType().equals(cartridgeType)) {
                return cartridge;
            }
        }
        String msg = "Unavailable cartridge type: " + cartridgeType;
        log.error(msg);
        throw new RestAPIException(msg) ;
    }

    static List<Cartridge> getAvailableLbCartridges(Boolean multiTenant,
                                                    ConfigurationContext configurationContext) throws RestAPIException {
        List<Cartridge> cartridges = getAvailableCartridges(null, multiTenant,
                configurationContext);
        List<Cartridge> lbCartridges = new ArrayList<Cartridge>();
        for (Cartridge cartridge : cartridges) {
            if (cartridge.isLoadBalancer()) {
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

                    if (!StratosApiV40Utils.cartridgeMatches(cartridgeInfo, searchPattern)) {
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
                    cartridge.setServiceGroup(cartridgeInfo.getServiceGroup());

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
            String msg = "Error while getting available cartridges. Cause: "+e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
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
            String msg = "Unable to get deployed service information. Cause: "+e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
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
            String msg = "Unable to get deployed service information for [type]: " + type+". Cause: "+e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
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
            String msg = "Unable to get deployed service information. Cause: "+e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
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

		/*if (availableMultitenantCartridges.isEmpty()) {
			String msg = "Cannot find any active deployed service for tenant [id] "+tenantId;
			log.error(msg);
			throw new RestAPIException(msg);
		}*/

        return availableMultitenantCartridges;
    }

    static List<Cartridge> getSubscriptions (String cartridgeSearchString, String serviceGroup, ConfigurationContext configurationContext) throws RestAPIException {

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
                    if(cartridge.isLoadBalancer())
                        continue;
                    if(StringUtils.isNotEmpty(serviceGroup) && cartridge.getServiceGroup() != null &&
                            !cartridge.getServiceGroup().equals(serviceGroup)){
                        continue;
                    }
                    cartridges.add(cartridge);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no subscribed cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error while getting subscribed cartridges. Cause: "+e.getMessage();
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


    static Cartridge getSubscription(String cartridgeAlias, ConfigurationContext configurationContext) throws RestAPIException {

        Cartridge cartridge =  getCartridgeFromSubscription(cartridgeSubsciptionManager.getCartridgeSubscription(ApplicationManagementUtil.
                getTenantId(configurationContext), cartridgeAlias));

        if (cartridge == null) {
            String message = "Unregistered [alias]: "+cartridgeAlias+"! Please enter a valid alias.";
            log.error(message);
            throw new RestAPIException(Response.Status.NOT_FOUND, message);
        }
        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
                cartridge.getCartridgeAlias());
        String cartridgeStatus = "Inactive";
        int activeMemberCount = 0;

        // cluster might not be created yet, so need to check
        if (cluster != null) {
            Collection<Member> members = cluster.getMembers();
            if (members != null ) {
                for (Member member : members) {
                    if(member.isActive()) {
                        cartridgeStatus = "Active";
                        activeMemberCount++;
                    }
                }
            }
        }

        cartridge.setActiveInstances(activeMemberCount);
        cartridge.setStatus(cartridgeStatus);
        return cartridge;

    }

    static int getActiveInstances(String cartridgeType, String cartridgeAlias, ConfigurationContext configurationContext) throws RestAPIException {
        int noOfActiveInstances = 0;
        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
                cartridgeAlias);

        if(cluster == null) {
            String message = "No Cluster found for cartridge [type] "+cartridgeType+", [alias] "+cartridgeAlias;
            log.error(message);
            throw new RestAPIException(message);
        }

        for(Member member : cluster.getMembers()) {
            if(member.getStatus().toString().equals(MemberStatus.Activated)) {
                noOfActiveInstances ++;
            }
        }
        return noOfActiveInstances;
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

            cartridge.setStatus(subscription.getSubscriptionStatus());
            cartridge.setPortMappings(subscription.getCartridgeInfo()
                    .getPortMappings());

            if(subscription.getCartridgeInfo().getLbConfig() != null && subscription.getCartridgeInfo().getProperties() != null) {
                for(Property property: subscription.getCartridgeInfo().getProperties()) {
                    if(property.getName().equals("load.balancer")) {
                        cartridge.setLoadBalancer(true);
                    }
                }
            }
            if(subscription.getCartridgeInfo().getServiceGroup() != null) {
                cartridge.setServiceGroup(subscription.getCartridgeInfo().getServiceGroup());
            }
            return cartridge;

        } catch (Exception e) {
            String msg = "Unable to extract the Cartridge from subscription. Cause: "+e.getMessage();
            log.error(msg);
            throw new RestAPIException(msg);
        }

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

    public static CartridgeSubscription getCartridgeSubscription(String alias, ConfigurationContext configurationContext) {
        return cartridgeSubsciptionManager.getCartridgeSubscription(ApplicationManagementUtil.getTenantId(configurationContext), alias);
    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster getCluster (String cartridgeType, String subscriptionAlias, ConfigurationContext configurationContext) throws RestAPIException {

        Cluster cluster = TopologyClusterInformationModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext),
                subscriptionAlias);
        if(cluster == null) {
            throw new RestAPIException("No matching cluster found for [cartridge type]: "+cartridgeType+ " [alias] "+subscriptionAlias);
        } else{
            return PojoConverter.populateClusterPojos(cluster, null);
        }
    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster[] getClustersForTenant (ConfigurationContext configurationContext) {

        Set<Cluster> clusterSet = TopologyClusterInformationModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext), null);
        ArrayList<org.apache.stratos.rest.endpoint.bean.topology.Cluster> clusters =
                new ArrayList<org.apache.stratos.rest.endpoint.bean.topology.Cluster>();
        for(Cluster cluster : clusterSet) {
            clusters.add(PojoConverter.populateClusterPojos(cluster, null));
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
            clusters.add(PojoConverter.populateClusterPojos(cluster, null));
        }
        org.apache.stratos.rest.endpoint.bean.topology.Cluster[] arrCluster =
                new org.apache.stratos.rest.endpoint.bean.topology.Cluster[clusters.size()];
        arrCluster = clusters.toArray(arrCluster);
        return arrCluster;

    }

    public static org.apache.stratos.rest.endpoint.bean.topology.Cluster[] getClustersForCartridgeType(String cartridgeType) {

        Set<Cluster> clusterSet = TopologyClusterInformationModel
                .getInstance()
                .getClusters(cartridgeType);
        List<org.apache.stratos.rest.endpoint.bean.topology.Cluster> clusters = new ArrayList<org.apache.stratos.rest.endpoint.bean.topology.Cluster>();
        for (Cluster cluster : clusterSet) {
            clusters.add(PojoConverter.populateClusterPojos(cluster, null));
        }
        org.apache.stratos.rest.endpoint.bean.topology.Cluster[] arrCluster = new org.apache.stratos.rest.endpoint.bean.topology.Cluster[clusters
                .size()];
        arrCluster = clusters.toArray(arrCluster);
        return arrCluster;

    }

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

    static StratosApiResponse unsubscribe(String alias, String tenantDomain) throws RestAPIException {

        try {
            cartridgeSubsciptionManager.unsubscribeFromCartridge(tenantDomain, alias);

        } catch (ADCException e) {
            String msg = "Failed to unsubscribe from [alias] "+alias+". Cause: "+ e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);

        } catch (NotSubscribedException e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully terminated the subscription with alias " + alias);
        return stratosApiResponse;
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
    static StratosApiResponse deployService(String cartridgeType, String alias, String autoscalingPolicy, String deploymentPolicy,
                                              String tenantDomain, String tenantUsername, int tenantId, String clusterDomain, String clusterSubdomain, String tenantRange) throws RestAPIException {
        log.info("Deploying service..");
        try {
            serviceDeploymentManager.deployService(cartridgeType, autoscalingPolicy, deploymentPolicy,
                    tenantId, tenantRange, tenantDomain, tenantUsername, false);

        } catch (Exception e) {
            String msg = String.format("Failed to deploy the Service [Cartridge type] %s [alias] %s . Cause: %s", cartridgeType, alias, e.getMessage());
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully deployed service cluster definition with type " + cartridgeType);
        return stratosApiResponse;
    }

    static StratosApiResponse undeployService(String serviceType) throws RestAPIException {

        try {
            serviceDeploymentManager.undeployService(serviceType);

        } catch (Exception e) {
            String msg = "Failed to undeploy service cluster definition of type " + serviceType+" Cause: "+e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully undeployed service cluster definition for service type " + serviceType);
        return stratosApiResponse;
    }

    static void getGitRepositoryNotification(Payload payload) throws RestAPIException {
        try {

            RepositoryNotification repoNotification = new RepositoryNotification();
            repoNotification.updateRepository(payload.getRepository().getUrl());

        } catch (Exception e) {
            String msg = "Failed to get git repository notifications. Cause : " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    static StratosApiResponse synchronizeRepository(CartridgeSubscription cartridgeSubscription) throws RestAPIException {
        try {
            RepositoryNotification repoNotification = new RepositoryNotification();
            repoNotification.updateRepository(cartridgeSubscription);
        } catch (Exception e) {
            String msg = "Failed to get git repository notifications. Cause : " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully sent the repository synchronization request for " + cartridgeSubscription.getAlias());
        return stratosApiResponse;
    }

    public static StratosApiResponse addSubscriptionDomains(ConfigurationContext configurationContext, String cartridgeType,
                                                              String subscriptionAlias,
                                                              SubscriptionDomainRequest request)
            throws RestAPIException {
        try {
            int tenantId = ApplicationManagementUtil.getTenantId(configurationContext);

            for (org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean subscriptionDomain : request.domains) {
                boolean isDomainExists = isSubscriptionDomainExists(configurationContext, cartridgeType, subscriptionAlias, subscriptionDomain.domainName);
                if (isDomainExists) {
                    String message = "Subscription domain " + subscriptionDomain.domainName + " exists";
                    throw new RestAPIException(Status.INTERNAL_SERVER_ERROR, message);
                }
            }

            for (org.apache.stratos.rest.endpoint.bean.subscription.domain.SubscriptionDomainBean subscriptionDomain : request.domains) {
                cartridgeSubsciptionManager.addSubscriptionDomain(tenantId, subscriptionAlias,
                        subscriptionDomain.domainName, subscriptionDomain.applicationContext);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully added domains to cartridge subscription");
        return stratosApiResponse;
    }

    public static boolean isSubscriptionDomainExists(ConfigurationContext configurationContext, String cartridgeType,
                                                     String subscriptionAlias, String domain) throws RestAPIException {
        try {
            int tenantId = ApplicationManagementUtil.getTenantId(configurationContext);
            SubscriptionDomainBean subscriptionDomain = PojoConverter.populateSubscriptionDomainPojo(cartridgeSubsciptionManager.getSubscriptionDomain(tenantId,
                    subscriptionAlias, domain));

            if (subscriptionDomain.domainName != null) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        }

    }

    public static List<SubscriptionDomainBean> getSubscriptionDomains(ConfigurationContext configurationContext, String cartridgeType,
                                                                      String subscriptionAlias) throws RestAPIException {
        try {
            int tenantId = ApplicationManagementUtil.getTenantId(configurationContext);
            return PojoConverter.populateSubscriptionDomainPojos(cartridgeSubsciptionManager.getSubscriptionDomains(tenantId, subscriptionAlias));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        }
    }

    public static SubscriptionDomainBean getSubscriptionDomain(ConfigurationContext configurationContext, String cartridgeType,
                                                               String subscriptionAlias, String domain) throws RestAPIException {
        try {
            int tenantId = ApplicationManagementUtil
                    .getTenantId(configurationContext);
            SubscriptionDomainBean subscriptionDomain = PojoConverter.populateSubscriptionDomainPojo(cartridgeSubsciptionManager.getSubscriptionDomain(tenantId,
                    subscriptionAlias, domain));

            if (subscriptionDomain == null) {
                String message = "Could not find a subscription [domain] "+domain+ " for Cartridge [type] "
                        +cartridgeType+ " and [alias] "+subscriptionAlias;
                log.error(message);
                throw new RestAPIException(Status.NOT_FOUND, message);
            }

            return subscriptionDomain;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        }
    }

    public static StratosApiResponse removeSubscriptionDomain(ConfigurationContext configurationContext, String cartridgeType,
                                                                String subscriptionAlias, String domain) throws RestAPIException {
        try {
            int tenantId = ApplicationManagementUtil.getTenantId(configurationContext);
            cartridgeSubsciptionManager.removeSubscriptionDomain(tenantId, subscriptionAlias, domain);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        }

        StratosApiResponse stratosApiResponse = new StratosApiResponse();
        stratosApiResponse.setMessage("Successfully removed domains from cartridge subscription");
        return stratosApiResponse;
    }
}