/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import org.apache.stratos.autoscaler.stub.*;
import org.apache.stratos.autoscaler.stub.deployment.policy.ApplicationPolicy;
import org.apache.stratos.autoscaler.stub.pojo.ApplicationContext;
import org.apache.stratos.autoscaler.stub.pojo.ServiceGroup;
import org.apache.stratos.cloud.controller.stub.*;
import org.apache.stratos.cloud.controller.stub.domain.Cartridge;
import org.apache.stratos.cloud.controller.stub.domain.Persistence;
import org.apache.stratos.cloud.controller.stub.domain.Volume;
import org.apache.stratos.common.beans.PropertyBean;
import org.apache.stratos.common.beans.TenantInfoBean;
import org.apache.stratos.common.beans.UserInfoBean;
import org.apache.stratos.common.beans.application.*;
import org.apache.stratos.common.beans.application.domain.mapping.ApplicationDomainMappingsBean;
import org.apache.stratos.common.beans.application.domain.mapping.DomainMappingBean;
import org.apache.stratos.common.beans.application.signup.ApplicationSignUpBean;
import org.apache.stratos.common.beans.artifact.repository.GitNotificationPayloadBean;
import org.apache.stratos.common.beans.cartridge.CartridgeBean;
import org.apache.stratos.common.beans.cartridge.CartridgeReferenceBean;
import org.apache.stratos.common.beans.cartridge.PersistenceBean;
import org.apache.stratos.common.beans.cartridge.VolumeBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesClusterBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesHostBean;
import org.apache.stratos.common.beans.kubernetes.KubernetesMasterBean;
import org.apache.stratos.common.beans.partition.NetworkPartitionBean;
import org.apache.stratos.common.beans.policy.autoscale.AutoscalePolicyBean;
import org.apache.stratos.common.beans.policy.deployment.ApplicationPolicyBean;
import org.apache.stratos.common.beans.policy.deployment.DeploymentPolicyBean;
import org.apache.stratos.common.beans.topology.ApplicationInfoBean;
import org.apache.stratos.common.beans.topology.ApplicationInstanceBean;
import org.apache.stratos.common.beans.topology.ClusterBean;
import org.apache.stratos.common.beans.topology.GroupInstanceBean;
import org.apache.stratos.common.client.AutoscalerServiceClient;
import org.apache.stratos.common.client.CloudControllerServiceClient;
import org.apache.stratos.common.client.StratosManagerServiceClient;
import org.apache.stratos.common.util.ClaimsMgtUtil;
import org.apache.stratos.common.util.CommonUtil;
import org.apache.stratos.manager.service.stub.domain.application.signup.ApplicationSignUp;
import org.apache.stratos.manager.service.stub.domain.application.signup.ArtifactRepository;
import org.apache.stratos.manager.service.stub.domain.application.signup.DomainMapping;
import org.apache.stratos.manager.user.management.StratosUserManager;
import org.apache.stratos.manager.user.management.exception.UserManagerException;
import org.apache.stratos.manager.utils.ApplicationManagementUtil;
import org.apache.stratos.manager.utils.CartridgeConstants;
import org.apache.stratos.messaging.domain.application.Application;
import org.apache.stratos.messaging.domain.application.ClusterDataHolder;
import org.apache.stratos.messaging.domain.application.Group;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.messaging.message.receiver.application.ApplicationManager;
import org.apache.stratos.messaging.message.receiver.topology.TopologyManager;
import org.apache.stratos.rest.endpoint.ServiceHolder;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.exception.TenantNotFoundException;
import org.apache.stratos.rest.endpoint.util.converter.ObjectConverter;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.context.RegistryType;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.stratos.common.exception.StratosException;
import org.wso2.carbon.tenant.mgt.core.TenantPersistor;
import org.wso2.carbon.tenant.mgt.util.TenantMgtUtil;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.tenant.Tenant;
import org.wso2.carbon.user.core.tenant.TenantManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Pattern;


public class StratosApiV41Utils {
    public static final String APPLICATION_STATUS_DEPLOYED = "Deployed";
    public static final String APPLICATION_STATUS_CREATED = "Created";

    private static Log log = LogFactory.getLog(StratosApiV41Utils.class);

    // Util methods for cartridges
    public static void addCartridge(CartridgeBean cartridgeDefinition) throws RestAPIException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Adding cartridge: [cartridge-type] %s ", cartridgeDefinition.getType()));
            }

            Cartridge cartridgeConfig = createCartridgeConfig(cartridgeDefinition);
            CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getInstance();
            cloudControllerServiceClient.addCartridge(cartridgeConfig);

            if (log.isDebugEnabled()) {
                log.debug(String.format(
                        "Successfully added cartridge: [cartridge-type] %s ",
                        cartridgeDefinition.getType()));
            }
        } catch (Exception e) {
            String msg = "Could not add cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    public static void updateCartridge(CartridgeBean cartridgeDefinition) throws RestAPIException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Updating cartridge: [cartridge-type] %s ", cartridgeDefinition.getType()));
            }

            Cartridge cartridgeConfig = createCartridgeConfig(cartridgeDefinition);
            CloudControllerServiceClient cloudControllerServiceClient = CloudControllerServiceClient.getInstance();
            cloudControllerServiceClient.updateCartridge(cartridgeConfig);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully updated cartridge: [cartridge-type] %s ",
                        cartridgeDefinition.getType()));
            }
        } catch (CloudControllerServiceCartridgeDefinitionNotExistsExceptionException e) {
            String msg = "No cartridge definition exists with this definition.Please use the POST method to add the cartridge";
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (Exception e) {
            String msg = "Could not update cartridge "+e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    private static Cartridge createCartridgeConfig(CartridgeBean cartridgeDefinition)
            throws RestAPIException {
        Cartridge cartridgeConfig =
                ObjectConverter.convertCartridgeBeanToStubCartridgeConfig(cartridgeDefinition);
        if (cartridgeConfig == null) {
            throw new RestAPIException("Could not read cartridge definition, cartridge deployment failed");
        }
        if (StringUtils.isEmpty(cartridgeConfig.getCategory())) {
            throw new RestAPIException(String.format("Category is not specified in cartridge: [cartridge-type] %s",
                    cartridgeConfig.getType()));
        }
        return cartridgeConfig;
    }

    public static void removeCartridge(String cartridgeType) throws RestAPIException {

        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Removing cartridge: [cartridge-type] %s ", cartridgeType));
            }

            CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
            if (cloudControllerServiceClient.getCartridge(cartridgeType) == null) {
                throw new RuntimeException("Cartridge not found: [cartridge-type] " + cartridgeType);
            }

            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();

            // Validate whether cartridge can be removed
            if (!smServiceClient.canCartridgeBeRemoved(cartridgeType)) {
                String message = "Cannot remove cartridge : [cartridge-type] " + cartridgeType +
                        " since it is used in another cartridge group or an application";
                log.error(message);
                throw new RestAPIException(message);
            }
            cloudControllerServiceClient.removeCartridge(cartridgeType);

            if (log.isInfoEnabled()) {
                log.info(String.format("Successfully removed cartridge: [cartridge-type] %s ", cartridgeType));
            }
        } catch (Exception e) {
            String msg = "Could not remove cartridge "+e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(e.getMessage(), e);
        }
    }

    public static List<CartridgeBean> getCartridgesByFilter(
            String filter, String criteria, ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeBean> cartridges = null;

        if (filter.equals("singleTenant")) {
            cartridges = getAvailableCartridges(null, false, configurationContext);
        } else if (filter.equals("multiTenant")) {
            cartridges = getAvailableCartridges(null, true, configurationContext);
        } else if (filter.equals("loadBalancer")) {
            cartridges = getAvailableLbCartridges(false, configurationContext);
        } else if (filter.equals("provider")) {
            cartridges = getAvailableCartridgesByProvider(criteria);
        }

        return cartridges;
    }

    public static CartridgeBean getCartridgeByFilter(
            String filter, String cartridgeType, ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeBean> cartridges = getCartridgesByFilter(filter, null, configurationContext);

        for (CartridgeBean cartridge : cartridges) {
            if (cartridge.getType().equals(cartridgeType)) {
                return cartridge;
            }
        }
        String msg = "Unavailable cartridge type: " + cartridgeType;
        log.error(msg);
        throw new RestAPIException(msg);
    }

    private static List<CartridgeBean> getAvailableLbCartridges(
            Boolean multiTenant, ConfigurationContext configurationContext) throws RestAPIException {
        List<CartridgeBean> cartridges = getAvailableCartridges(null, multiTenant,
                configurationContext);
        List<CartridgeBean> lbCartridges = new ArrayList<CartridgeBean>();
        for (CartridgeBean cartridge : cartridges) {
            if ("loadbalancer".equalsIgnoreCase(cartridge.getCategory())) {
                lbCartridges.add(cartridge);
            }
        }
        return lbCartridges;
    }

    /**
     * Get the available cartridges by provider
     *
     * @param provider provide name
     * @return List of the cartridge definitions
     * @throws RestAPIException
     */
    private static List<CartridgeBean> getAvailableCartridgesByProvider(String provider) throws RestAPIException {
        List<CartridgeBean> cartridges = new ArrayList<CartridgeBean>();

        if (log.isDebugEnabled()) {
            log.debug("Reading cartridges: [provider-name] " + provider);
        }

        try {
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


                    if (!cartridgeInfo.getProvider().equals(provider)) {
                        continue;
                    }

                    CartridgeBean cartridge = convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
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

    public static List<CartridgeBean> getAvailableCartridges(
            String cartridgeSearchString, Boolean multiTenant, ConfigurationContext configurationContext)
            throws RestAPIException {

        List<CartridgeBean> cartridges = new ArrayList<CartridgeBean>();

        if (log.isDebugEnabled()) {
            log.debug("Getting available cartridges. [Search String]: " + cartridgeSearchString + ", [Multi-Tenant]: " + multiTenant);
        }


        boolean allowMultipleSubscription = Boolean.valueOf(
                System.getProperty(CartridgeConstants.FEATURE_MULTI_TENANT_MULTIPLE_SUBSCRIPTION_ENABLED));

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

                    if (!StratosApiV41Utils.cartridgeMatches(cartridgeInfo, searchPattern)) {
                        continue;
                    }

                    CartridgeBean cartridge = convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
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

    public static CartridgeBean getCartridge(String cartridgeType) throws RestAPIException {
        try {
            Cartridge cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridge(cartridgeType);
            if (cartridgeInfo == null) {
                return null;
            }
            return convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            String message = e.getFaultMessage().getCartridgeNotFoundException().getMessage();
            log.error(message);
            throw new RestAPIException(message, e);
        }
    }

    public static CartridgeBean getCartridgeForValidate(String cartridgeType) throws RestAPIException {
        try {
            Cartridge cartridgeInfo = CloudControllerServiceClient.getInstance().getCartridge(cartridgeType);
            if (cartridgeInfo == null) {
                return null;
            }
            return convertCartridgeToCartridgeDefinitionBean(cartridgeInfo);
        } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
            return null;
        } catch (RemoteException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        }

    }

    private static CartridgeBean convertCartridgeToCartridgeDefinitionBean(Cartridge cartridgeInfo) {
        CartridgeBean cartridge = new CartridgeBean();
        cartridge.setType(cartridgeInfo.getType());
        cartridge.setProvider(cartridgeInfo.getProvider());
        cartridge.setCategory(cartridgeInfo.getCategory());
        cartridge.setHost(cartridgeInfo.getHostName());
        cartridge.setDisplayName(cartridgeInfo.getDisplayName());
        cartridge.setDescription(cartridgeInfo.getDescription());
        cartridge.setVersion(cartridgeInfo.getVersion());
        cartridge.setMultiTenant(cartridgeInfo.getMultiTenant());
        cartridge.setPersistence(convertPersistenceToPersistenceBean(cartridgeInfo.getPersistence()));
        cartridge.setLoadBalancingIPType(cartridgeInfo.getLoadBalancingIPType());
        cartridge.setMetadataKeys(cartridgeInfo.getMetadataKeys());
        return cartridge;
    }

    private static PersistenceBean convertPersistenceToPersistenceBean(Persistence persistence) {
        if (persistence == null) {
            return null;
        }

        PersistenceBean persistenceBean = new PersistenceBean();
        persistenceBean.setRequired(persistence.isPersistenceRequiredSpecified());
        persistenceBean.setVolume(convertVolumesToVolumeBeans(persistence.getVolumes()));
        return persistenceBean;
    }

    private static List<VolumeBean> convertVolumesToVolumeBeans(Volume[] volumes) {
        List<VolumeBean> list = new ArrayList<VolumeBean>();
        for (Volume volume : volumes) {
            VolumeBean volumeBean = new VolumeBean();
            volumeBean.setId(volume.getId());
            volumeBean.setDevice(volume.getDevice());
            volumeBean.setSize(String.valueOf(volume.getSize()));
            volumeBean.setSnapshotId(volume.getSnapshotId());
            list.add(volumeBean);
        }
        return list;
    }

    private static boolean isAlreadySubscribed(String cartridgeType, int tenantId) {
        return false;
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

            return Pattern.compile(regex);
        }
        return null;
    }

    private static boolean cartridgeMatches(Cartridge cartridgeInfo, Pattern pattern) {
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

    // Util methods to get the service clients

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

    private static AutoscalerServiceClient getAutoscalerServiceClient() throws RestAPIException {
        try {
            return AutoscalerServiceClient.getInstance();
        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting AutoscalerServiceClient instance to connect to the "
                    + "Autoscaler. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    private static StratosManagerServiceClient getStratosManagerServiceClient() throws RestAPIException {
        try {
            return StratosManagerServiceClient.getInstance();
        } catch (AxisFault axisFault) {
            String errorMsg = "Error while getting StratosManagerServiceClient instance to connect to the "
                    + "Stratos Manager. Cause: " + axisFault.getMessage();
            log.error(errorMsg, axisFault);
            throw new RestAPIException(errorMsg, axisFault);
        }
    }

    // Util methods for Autoscaling policies

    public static void addAutoscalingPolicy(AutoscalePolicyBean autoscalePolicyBean) throws RestAPIException {

        log.info(String.format("Adding autoscaling policy: [id] %s", autoscalePolicyBean.getId()));

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
            } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
                String message = e.getFaultMessage()
                        .getInvalidPolicyException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
    }

    public static void addApplicationPolicy(ApplicationPolicyBean applicationPolicyBean) throws RestAPIException {

        if (applicationPolicyBean == null) {
            String msg = "Application policy bean is null";
            log.error(msg);
            throw new RestAPIException(msg);
        }

        AutoscalerServiceClient serviceClient = getAutoscalerServiceClient();
        try {
            ApplicationPolicy applicationPolicy = ObjectConverter.convertApplicationPolicyBeanToStubAppPolicy(
                    applicationPolicyBean);
            if (applicationPolicy == null) {
                String msg = "Application policy is null";
                log.error(msg);
                throw new RestAPIException(msg);
            }
            serviceClient.addApplicationPolicy(applicationPolicy);
        } catch (RemoteException e) {
            String msg = "Could not add application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
            String msg = "Could not add application policy. Invalid policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceRemoteExceptionException e) {
            String msg = "Could not add application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceInvalidApplicationPolicyExceptionException e) {
            String msg = "Could not add application policy. Invalid application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    public static void updateApplicationPolicy(ApplicationPolicyBean applicationPolicyBean) throws RestAPIException {

        log.info(String.format("Updating application policy: [id] %s", applicationPolicyBean.getId()));

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            ApplicationPolicy applicationPolicy = ObjectConverter.convertApplicationPolicyBeanToStubAppPolicy(
                    applicationPolicyBean);

            try {
                autoscalerServiceClient.updateApplicationPolicy(applicationPolicy);
            } catch (RemoteException e) {
                String msg = "Could not update application policy" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            } catch (AutoscalerServiceApplicatioinPolicyNotExistsExceptionException e) {
                String msg = "Could not update application policy. Application policy not exists" +
                        e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            } catch (AutoscalerServiceRemoteExceptionException e) {
                String msg = "Could not update application policy" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            } catch (AutoscalerServiceInvalidApplicationPolicyExceptionException e) {
                String msg = "Could not update application policy. Invalid application policy" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            }
        }
    }

    public static ApplicationPolicyBean[] getApplicationPolicies() throws RestAPIException {

        ApplicationPolicy[] applicationPolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                applicationPolicies = autoscalerServiceClient.getApplicationPolicies();
            } catch (RemoteException e) {
                String msg = "Could not get application policies" + e.getLocalizedMessage();
                log.error(msg, e);
                throw new RestAPIException(msg);
            }
        }
        return ObjectConverter.convertASStubApplicationPoliciesToApplicationPolicies(applicationPolicies);
    }

    public static ApplicationPolicyBean getApplicationPolicy(String applicationPolicyId) throws RestAPIException {

        if (applicationPolicyId == null) {
            String msg = "Application policy bean id null";
            log.error(msg);
            throw new RestAPIException(msg);
        }

        if (StringUtils.isBlank(applicationPolicyId)) {
            String msg = "Application policy id is empty";
            log.error(msg);
            throw new RestAPIException(msg);
        }

        try {
            AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();
            ApplicationPolicy applicationPolicy = serviceClient.getApplicationPolicy(applicationPolicyId);
            return ObjectConverter.convertASStubApplicationPolicyToApplicationPolicy(applicationPolicy);
        } catch (Exception e) {
            String message = String.format("Could not get application policy [application-policy-id] %s",
                    applicationPolicyId);
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public static void removeApplicationPolicy(String applicationPolicyId) throws RestAPIException {

        if (applicationPolicyId == null) {
            String msg = "Application policy bean id null";
            log.error(msg);
            throw new RestAPIException(msg);
        }

        if (StringUtils.isBlank(applicationPolicyId)) {
            String msg = "Application policy id is empty";
            log.error(msg);
            throw new RestAPIException(msg);
        }

        AutoscalerServiceClient serviceClient = getAutoscalerServiceClient();
        try {
            serviceClient.removeApplicationPolicy(applicationPolicyId);
        } catch (RemoteException e) {
            String msg = "Could not remove application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
            String msg = "Could not remove application policy. " + e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    public static void updateAutoscalingPolicy(AutoscalePolicyBean autoscalePolicyBean) throws RestAPIException {

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
            } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
                String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
    }

    public static void removeAutoscalingPolicy(String autoscalePolicyId) throws RestAPIException {

        log.info(String.format("Removing autoscaling policy: [id] %s", autoscalePolicyId));

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            try {
                autoscalerServiceClient.removeAutoscalingPolicy(autoscalePolicyId);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
                String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
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

    // Util methods for repo actions

    public static void notifyArtifactUpdatedEvent(GitNotificationPayloadBean payload) throws RestAPIException {
        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.notifyArtifactUpdatedEventForRepository(payload.getRepository().getUrl());
        } catch (Exception e) {
            String message = "Could not send artifact updated event";
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    // Util methods for cartridge groups

    public static void addServiceGroup(GroupBean serviceGroupDefinition) throws RestAPIException {
        try {
            if (serviceGroupDefinition == null) {
                throw new RuntimeException("Service Group definition is null");
            }

            List<String> cartridgeTypes = new ArrayList<String>();
            String[] cartridgeNames = null;
            List<String> groupNames;
            String[] cartridgeGroupNames;

            // if any cartridges are specified in the group, they should be already deployed
            if (serviceGroupDefinition.getCartridges() != null) {

                if (log.isDebugEnabled()) {
                    log.debug("checking cartridges in cartridge group " + serviceGroupDefinition.getName());
                }

                findCartridgesInGroupBean(serviceGroupDefinition, cartridgeTypes);

                Set<String> duplicates = findDuplicates(cartridgeTypes);
                if (duplicates.size() > 0) {
                    StringBuilder duplicatesOutput = new StringBuilder();
                    for (String dup : duplicates) {
                        duplicatesOutput.append(dup).append(" ");
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("duplicate cartridges defined: " + duplicatesOutput.toString());
                    }
                    throw new RestAPIException("Invalid Service Group definition, duplicate cartridges defined:" +
                            duplicatesOutput.toString());
                }

                CloudControllerServiceClient ccServiceClient = getCloudControllerServiceClient();

                cartridgeNames = new String[cartridgeTypes.size()];
                int i = 0;
                for (String cartridgeType : cartridgeTypes) {
                    try {
                        if (ccServiceClient.getCartridge(cartridgeType) == null) {
                            // cartridge is not deployed, can't continue
                            log.error("invalid cartridge found in cartridge group " + cartridgeType);
                            throw new RestAPIException("No Cartridge Definition found with type " + cartridgeType);
                        } else {
                            cartridgeNames[i] = cartridgeType;
                            i++;
                        }
                    } catch (RemoteException e) {
                        throw new RestAPIException(e);
                    } catch (CloudControllerServiceCartridgeNotFoundExceptionException e) {
                        throw new RestAPIException(e);
                    }
                }
            }

            // if any sub groups are specified in the group, they should be already deployed
            if (serviceGroupDefinition.getGroups() != null) {
                if (log.isDebugEnabled()) {
                    log.debug("checking subGroups in cartridge group " + serviceGroupDefinition.getName());
                }

                List<GroupBean> groupDefinitions = serviceGroupDefinition.getGroups();
                groupNames = new ArrayList<String>();
                cartridgeGroupNames = new String[groupDefinitions.size()];
                int i = 0;
                for (GroupBean groupList : groupDefinitions) {
                    groupNames.add(groupList.getName());
                    cartridgeGroupNames[i] = groupList.getName();
                    i++;
                }

                Set<String> duplicates = findDuplicates(groupNames);
                if (duplicates.size() > 0) {

                    StringBuilder duplicatesOutput = new StringBuilder();
                    for (String dup : duplicates) {
                        duplicatesOutput.append(dup).append(" ");
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("duplicate subGroups defined: " + duplicatesOutput.toString());
                    }
                    throw new RestAPIException("Invalid Service Group definition, duplicate subGroups defined:" +
                            duplicatesOutput.toString());
                }
            }

            ServiceGroup serviceGroup = ObjectConverter.convertServiceGroupDefinitionToASStubServiceGroup(
                    serviceGroupDefinition);

            AutoscalerServiceClient asServiceClient = getAutoscalerServiceClient();
            asServiceClient.addServiceGroup(serviceGroup);

            // Add cartridge group elements to SM cache - done after service group has been added
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();
            smServiceClient.addUsedCartridgesInCartridgeGroups(serviceGroupDefinition.getName(), cartridgeNames);
        } catch (Exception e) {
            // TODO: InvalidServiceGroupException is not received, only AxisFault. Need to fix get the custom exception
            String message = "Could not add cartridge group";
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * returns any duplicates in a List
     *
     * @param checkedList List to find duplicates from
     * @return Set of duplicates
     */
    private static Set<String> findDuplicates(List<String> checkedList) {
        final Set<String> retVals = new HashSet<String>();
        final Set<String> set1 = new HashSet<String>();

        for (String val : checkedList) {

            if (!set1.add(val)) {
                retVals.add(val);
            }
        }
        return retVals;
    }

    public static GroupBean getServiceGroupDefinition(String name) throws RestAPIException {

        if (log.isDebugEnabled()) {
            log.debug("Reading cartridge group: [group-name] " + name);
        }

        try {
            AutoscalerServiceClient asServiceClient = AutoscalerServiceClient.getInstance();
            ServiceGroup serviceGroup = asServiceClient.getServiceGroup(name);
            if (serviceGroup == null) {
                return null;
            }

            return ObjectConverter.convertStubServiceGroupToServiceGroupDefinition(serviceGroup);

        } catch (Exception e) {
            String message = "Could not get cartridge group: [group-name] " + name;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static GroupBean[] getServiceGroupDefinitions() throws RestAPIException {

        if (log.isDebugEnabled()) {
            log.debug("Reading cartridge groups...");
        }

        try {
            AutoscalerServiceClient asServiceClient = AutoscalerServiceClient.getInstance();
            ServiceGroup[] serviceGroups = asServiceClient.getServiceGroups();
            if (serviceGroups == null || serviceGroups.length == 0 || (serviceGroups.length == 1 && serviceGroups[0] == null)) {
                return null;
            }

            GroupBean[] serviceGroupDefinitions = new GroupBean[serviceGroups.length];
            for (int i = 0; i < serviceGroups.length; i++) {
                serviceGroupDefinitions[i] = ObjectConverter.convertStubServiceGroupToServiceGroupDefinition(
                        serviceGroups[i]);
            }
            return serviceGroupDefinitions;

        } catch (Exception e) {
            throw new RestAPIException(e);
        }
    }

    public static void removeServiceGroup(String name) throws RestAPIException {

        try {
            if (log.isDebugEnabled()) {
                log.debug("Removing cartridge group: [name] " + name);
            }

            AutoscalerServiceClient asServiceClient = getAutoscalerServiceClient();
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();

            // Check whether cartridge group exists
            if (asServiceClient.getServiceGroup(name) == null) {
                String message = "Cartridge group: [group-name] " + name + " cannot be removed since it does not exist";
                log.error(message);
                throw new RestAPIException(message);
            }

            // Validate whether cartridge group can be removed
            if (!smServiceClient.canCartirdgeGroupBeRemoved(name)) {
                String message = "Cannot remove cartridge group: [group-name] " + name +
                        " since it is used in another cartridge group or an application";
                log.error(message);
                throw new RestAPIException(message);
            }

            ServiceGroup serviceGroup = asServiceClient.getServiceGroup(name);

            asServiceClient.undeployServiceGroupDefinition(name);

            // Remove the dependent cartridges and cartridge groups from Stratos Manager cache
            // - done after service group has been removed
            if (serviceGroup.getCartridges() != null) {
                List<String> cartridgeList = new ArrayList<String>();
                findCartridgesInServiceGroup(serviceGroup, cartridgeList);
                String[] cartridgeNames = cartridgeList.toArray(new String[cartridgeList.size()]);
                smServiceClient.removeUsedCartridgesInCartridgeGroups(name, cartridgeNames);
            }

        } catch (Exception e) {
            throw new RestAPIException(e);
        }

        log.info("Successfully removed the cartridge group: [group-name] " + name);
    }

    private static void findCartridgesInServiceGroup(ServiceGroup serviceGroup, List<String> cartridges) {

        if (serviceGroup == null || cartridges == null) {
            return;
        }

        for (String cartridge : serviceGroup.getCartridges()) {
            if (!cartridges.contains(cartridge)) {
                cartridges.add(cartridge);
            }
        }

        if (serviceGroup.getGroups() != null) {
            for (ServiceGroup seGroup : serviceGroup.getGroups()) {
                findCartridgesInServiceGroup(seGroup, cartridges);
            }
        }
    }

    private static void findCartridgesInGroupBean(GroupBean groupBean, List<String> cartridges) {

        if (groupBean == null || cartridges == null) {
            return;
        }

        for (String cartridge : groupBean.getCartridges()) {
            if (!cartridges.contains(cartridge)) {
                cartridges.add(cartridge);
            }
        }

        if (groupBean.getGroups() != null) {
            for (GroupBean seGroup : groupBean.getGroups()) {
                findCartridgesInGroupBean(seGroup, cartridges);
            }
        }
    }

    // Util methods for Applications

    /**
     * Verify the existence of the application and add it.
     *
     * @param appDefinition Application definition
     * @param ctxt Configuration context
     * @param userName Username
     * @param tenantDomain Tenant Domain
     * @throws RestAPIException
     */
    public static void addApplication(ApplicationBean appDefinition, ConfigurationContext ctxt,
                                      String userName, String tenantDomain)
            throws RestAPIException {

        if (StringUtils.isBlank(appDefinition.getApplicationId())) {
            String message = "Please specify the application name";
            log.error(message);
            throw new RestAPIException(message);
        }
        // check if an application with same id already exists
        try {
            if (AutoscalerServiceClient.getInstance().getApplication(appDefinition.getApplicationId()) != null) {
                String msg = "Application already exists: [application-id] " + appDefinition.getApplicationId();
                throw new RestAPIException(msg);
            }
        } catch (RemoteException e) {
            throw new RestAPIException("Could not read application", e);
        }

        validateApplication(appDefinition);

        ApplicationContext applicationContext = ObjectConverter.convertApplicationDefinitionToStubApplicationContext(
                appDefinition);
        applicationContext.setTenantId(ApplicationManagementUtil.getTenantId(ctxt));
        applicationContext.setTenantDomain(tenantDomain);
        applicationContext.setTenantAdminUsername(userName);

        if (appDefinition.getProperty() != null) {
            org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
            for (PropertyBean propertyBean : appDefinition.getProperty()) {
                org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
                property.setName(propertyBean.getName());
                property.setValue(propertyBean.getValue());
                properties.addProperties(property);
            }
            applicationContext.setProperties(properties);
        }

        try {
            AutoscalerServiceClient.getInstance().addApplication(applicationContext);

            List<String> usedCartridges = new ArrayList<String>();
            List<String> usedCartridgeGroups = new ArrayList<String>();
            findCartridgesAndGroupsInApplication(appDefinition, usedCartridges, usedCartridgeGroups);
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();
            smServiceClient.addUsedCartridgesInApplications(
                    appDefinition.getApplicationId(),
                    usedCartridges.toArray(new String[usedCartridges.size()]));

            smServiceClient.addUsedCartridgeGroupsInApplications(
                    appDefinition.getApplicationId(),
                    usedCartridgeGroups.toArray(new String[usedCartridgeGroups.size()]));

        } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
            throw new RestAPIException(e);
        } catch (RemoteException e) {
            throw new RestAPIException(e);
        }
    }

    /**
     * Update the existence of the application and update it.
     *
     * @param appDefinition Application definition
     * @param ctxt Configuration context
     * @param userName Username
     * @param tenantDomain Tenant Domain
     * @throws RestAPIException
     */
    public static void updateApplication(ApplicationBean appDefinition, ConfigurationContext ctxt,
                                      String userName, String tenantDomain)
            throws RestAPIException {

        if (StringUtils.isBlank(appDefinition.getApplicationId())) {
            String message = "Please specify the application name";
            log.error(message);
            throw new RestAPIException(message);
        }

        validateApplication(appDefinition);

        ApplicationContext applicationContext = ObjectConverter.convertApplicationDefinitionToStubApplicationContext(
                appDefinition);
        applicationContext.setTenantId(ApplicationManagementUtil.getTenantId(ctxt));
        applicationContext.setTenantDomain(tenantDomain);
        applicationContext.setTenantAdminUsername(userName);

        if (appDefinition.getProperty() != null) {
            org.apache.stratos.autoscaler.stub.Properties properties = new org.apache.stratos.autoscaler.stub.Properties();
            for (PropertyBean propertyBean : appDefinition.getProperty()) {
                org.apache.stratos.autoscaler.stub.Property property = new org.apache.stratos.autoscaler.stub.Property();
                property.setName(propertyBean.getName());
                property.setValue(propertyBean.getValue());
                properties.addProperties(property);
            }
            applicationContext.setProperties(properties);
        }

        try {
            AutoscalerServiceClient.getInstance().updateApplication(applicationContext);
        } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
            throw new RestAPIException(e);
        } catch (RemoteException e) {
            throw new RestAPIException(e);
        }
    }


    private static void findCartridgesAndGroupsInApplication(
            ApplicationBean applicationBean, List<String> cartridges, List<String> cartridgeGroups) {

        if (applicationBean == null || applicationBean.getComponents() == null) {
            return;
        }

        ComponentBean componentBean = applicationBean.getComponents();

        List<GroupReferenceBean> groupReferenceBeans = componentBean.getGroups();
        if (groupReferenceBeans != null) {
            for (GroupReferenceBean groupReferenceBean : groupReferenceBeans) {
                findCartridgesAndGroupsInCartridgeGroup(groupReferenceBean, cartridges, cartridgeGroups);
            }
        }

        List<CartridgeReferenceBean> cartridgeReferenceBeans = componentBean.getCartridges();
        findCartridgeNamesInCartridges(cartridgeReferenceBeans, cartridges);
    }

    private static void findCartridgesAndGroupsInCartridgeGroup(
            GroupReferenceBean groupReferenceBean, List<String> cartridges, List<String> cartridgeGroups) {

        if (groupReferenceBean == null || cartridgeGroups == null) {
            return;
        }

        if (!cartridgeGroups.contains(groupReferenceBean.getName())) {
            cartridgeGroups.add(groupReferenceBean.getName());
        }

        if (groupReferenceBean.getGroups() != null) {
            for (GroupReferenceBean grReferenceBean : groupReferenceBean.getGroups()) {
                findCartridgesAndGroupsInCartridgeGroup(grReferenceBean, cartridges, cartridgeGroups);
                findCartridgeNamesInCartridges(groupReferenceBean.getCartridges(), cartridges);
            }
        }

        findCartridgeNamesInCartridges(groupReferenceBean.getCartridges(), cartridges);
    }

    private static void findCartridgeNamesInCartridges(
            List<CartridgeReferenceBean> cartridgeReferenceBeans, List<String> cartridges) {

        if (cartridgeReferenceBeans == null || cartridges == null) {
            return;
        }

        for (CartridgeReferenceBean cartridgeReferenceBean : cartridgeReferenceBeans) {
            if (cartridgeReferenceBean != null && !cartridges.contains(cartridgeReferenceBean.getType())) {
                cartridges.add(cartridgeReferenceBean.getType());
            }
        }
    }

    private static void validateApplication(ApplicationBean appDefinition) throws RestAPIException {

        if (StringUtils.isBlank(appDefinition.getAlias())) {
            String message = "Please specify the application alias";
            log.error(message);
            throw new RestAPIException(message);
        }
    }

    /**
     * Deploy application with an application policy.
     *
     * @param applicationId Application ID
     * @param applicationPolicyId Application policy Id
     * @throws RestAPIException
     */
    public static void deployApplication(String applicationId, String applicationPolicyId)
            throws RestAPIException {

        if (StringUtils.isEmpty(applicationPolicyId)) {
            String message = "Application policy id is Empty";
            log.error(message);
            throw new RestAPIException(message);
        }

        if (StringUtils.isEmpty(applicationPolicyId)) {
            String message = "Application policy id is Empty";
            log.error(message);
            throw new RestAPIException(message);
        }
        if (StringUtils.isEmpty(applicationPolicyId)) {
            String message = String.format("Application policy id cannot be null : [application-policy-id] %s. "
                    + "Are you passing application policy?", applicationPolicyId);
            log.error(message);
            throw new RestAPIException(message);
        }

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Starting to deploy application: [application-id] %s", applicationId));
            }

            AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
            ApplicationContext application = autoscalerServiceClient.getApplication(applicationId);

            if (application == null) {
                String message = String.format("Application is not found: [application-id] %s", applicationId);
                log.error(message);
                throw new RestAPIException(message);
            }

            if (application.getStatus().equalsIgnoreCase(APPLICATION_STATUS_DEPLOYED)) {
                String message = String.format(
                        "Application is already in DEPLOYED state: [application-id] %s [current status] %s ",
                        applicationId,
                        application.getStatus());
                log.error(message);
                throw new RestAPIException(message);
            }

            // This is a redundant state since there is only CREATED,DEPLOYED state.
            // But this will be usefull when more status are added.
            if (!application.getStatus().equalsIgnoreCase(APPLICATION_STATUS_CREATED)) {
                String message = String.format(
                        "Application is not in CREATED state: [application-id] %s [current status] %s ",
                        applicationId,
                        application.getStatus());
                log.error(message);
                throw new RestAPIException(message);
            }

            ApplicationBean applicationBean = getApplication(applicationId);
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            if (applicationBean.isMultiTenant() && (tenantId != -1234)) {
                String message = String.format(
                        "Multi-tenant applications can only be deployed by super tenant: [application-id] %s",
                        applicationId);
                log.error(message);
                throw new RestAPIException(message);
            }

            autoscalerServiceClient.deployApplication(applicationId, applicationPolicyId);
            if (log.isInfoEnabled()) {
                log.info(String.format("Application deployed successfully: [application-id] %s", applicationId));
            }
        } catch (RemoteException e) {
            log.error(e.getMessage(), e);
            throw new RestAPIException(e.getMessage(), e);
        } catch (AutoscalerServiceInvalidPolicyExceptionException e) {
            String message = e.getFaultMessage().getInvalidPolicyException().getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
            String message = e.getMessage();
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static ApplicationNetworkPartitionIdListBean getApplicationNetworkPartitions(String applicationId) {
        try {
            AutoscalerServiceClient serviceClient = AutoscalerServiceClient.getInstance();
            String[] networkPartitions = serviceClient.getApplicationNetworkPartitions(applicationId);
            ApplicationNetworkPartitionIdListBean appNetworkPartitionsBean = new ApplicationNetworkPartitionIdListBean();
            appNetworkPartitionsBean.setNetworkPartitionIds(Arrays.asList(networkPartitions));
            return appNetworkPartitionsBean;
        } catch (Exception e) {
            String message = String.format("Could not get application network partitions [application-id] %s", applicationId);
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Validate deployment policy
     *
     * @param deploymentPolicy Deployment Policy
     */
    private static void validateDeploymentPolicy(DeploymentPolicyBean deploymentPolicy) throws RestAPIException {
        if (StringUtils.isBlank(deploymentPolicy.getId())) {
            String message = "No deployment policy id specify with the policy";
            log.error(message);
            throw new RestAPIException(message);
        }
        if (deploymentPolicy.getNetworkPartitions().size() == 0) {
            String message = "No network partitions specify with the policy";
            log.error(message);
            throw new RestAPIException(message);
        }

    }

    public static void removeApplication(String applicationId) throws RestAPIException {

        try {

            log.info(String.format("Starting to remove application [application-id %s", applicationId));

            AutoscalerServiceClient asServiceClient = getAutoscalerServiceClient();

            ApplicationContext asApplication = asServiceClient.getApplication(applicationId);
            ApplicationBean application = ObjectConverter.convertStubApplicationContextToApplicationDefinition(
                    asApplication);
            asServiceClient.deleteApplication(applicationId);

            List<String> usedCartridges = new ArrayList<String>();
            List<String> usedCartridgeGroups = new ArrayList<String>();
            findCartridgesAndGroupsInApplication(application, usedCartridges, usedCartridgeGroups);
            StratosManagerServiceClient smServiceClient = getStratosManagerServiceClient();
            smServiceClient.removeUsedCartridgesInApplications(
                    application.getApplicationId(),
                    usedCartridges.toArray(new String[usedCartridges.size()]));

            smServiceClient.removeUsedCartridgeGroupsInApplications(
                    application.getApplicationId(),
                    usedCartridgeGroups.toArray(new String[usedCartridgeGroups.size()]));

        } catch (RemoteException e) {
            String message = "Could not delete application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static ApplicationBean getApplication(String applicationId) throws RestAPIException {
        try {
            return ObjectConverter.convertStubApplicationContextToApplicationDefinition(
                    AutoscalerServiceClient.getInstance().getApplication(applicationId));
        } catch (RemoteException e) {
            String message = "Could not read application: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static List<ApplicationBean> getApplications() throws RestAPIException {
        try {
            List<ApplicationBean> applicationDefinitions = new ArrayList<ApplicationBean>();
            ApplicationContext[] applicationContexts = AutoscalerServiceClient.getInstance().getApplications();
            if (applicationContexts != null) {
                for (ApplicationContext applicationContext : applicationContexts) {
                    if (applicationContext != null) {
                        ApplicationBean applicationDefinition =
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

    public static void undeployApplication(String applicationId, boolean force) throws RestAPIException {
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (force) {
            if (log.isDebugEnabled()) {
                log.debug("Forcefully undeploying application [application-id] " + applicationId);
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Gracefully undeploying application [application-id] " + applicationId);
            }
        }
        if (autoscalerServiceClient != null) {
            try {
                autoscalerServiceClient.undeployApplication(applicationId, force);
            } catch (RemoteException e) {
                String message = "Could not undeploy application: [application-id] " + applicationId;
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (AutoscalerServiceApplicationDefinitionExceptionException e) {
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
                for (ApplicationInstanceBean instanceBean : applicationInfoBean.getApplicationInstances()) {
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

    public static ApplicationInfoBean getApplicationRuntime(String applicationId) {
        ApplicationInfoBean applicationBean = null;
        try {
            ApplicationManager.acquireReadLockForApplication(applicationId);
            Application application = ApplicationManager.getApplications().getApplication(applicationId);
            if (application == null) {
                return null;
            }
            applicationBean = ObjectConverter.convertApplicationToApplicationInstanceBean(application);
            for (ApplicationInstanceBean instanceBean : applicationBean.getApplicationInstances()) {
                addClustersInstancesToApplicationInstanceBean(instanceBean, application);
                addGroupsInstancesToApplicationInstanceBean(instanceBean, application);
            }
        } finally {
            ApplicationManager.releaseReadLockForApplication(applicationId);
        }
        return applicationBean;
    }

    private static void addGroupsInstancesToApplicationInstanceBean(ApplicationInstanceBean applicationInstanceBean,
                                                                    Application application) {
        Collection<Group> groups = application.getGroups();
        if (groups != null && !groups.isEmpty()) {
            for (Group group : groups) {
                List<GroupInstanceBean> groupInstanceBeans = ObjectConverter.convertGroupToGroupInstancesBean(
                        applicationInstanceBean.getInstanceId(), group);
                for (GroupInstanceBean groupInstanceBean : groupInstanceBeans) {
                    setSubGroupInstances(group, groupInstanceBean);
                    applicationInstanceBean.getGroupInstances().add(groupInstanceBean);
                }
            }
        }

    }

    private static void addClustersInstancesToApplicationInstanceBean(
            ApplicationInstanceBean applicationInstanceBean, Application application) {

        Map<String, ClusterDataHolder> topLevelClusterDataMap = application.getClusterDataMap();
        if (topLevelClusterDataMap != null) {
            for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
                ClusterDataHolder clusterDataHolder = entry.getValue();
                String clusterId = clusterDataHolder.getClusterId();
                String serviceType = clusterDataHolder.getServiceType();
                try {
                    TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
                    Cluster cluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);

                    applicationInstanceBean.getClusterInstances().add(ObjectConverter.
                            convertClusterToClusterInstanceBean(applicationInstanceBean.getInstanceId(),
                                    cluster, entry.getKey()));
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceType, clusterId);
                }
            }
        }
    }

    private static void addClustersInstancesToGroupInstanceBean(
            GroupInstanceBean groupInstanceBean,
            Group group) {
        Map<String, ClusterDataHolder> topLevelClusterDataMap = group.getClusterDataMap();
        if (topLevelClusterDataMap != null && !topLevelClusterDataMap.isEmpty()) {
            for (Map.Entry<String, ClusterDataHolder> entry : topLevelClusterDataMap.entrySet()) {
                ClusterDataHolder clusterDataHolder = entry.getValue();
                String clusterId = clusterDataHolder.getClusterId();
                String serviceType = clusterDataHolder.getServiceType();
                try {
                    TopologyManager.acquireReadLockForCluster(serviceType, clusterId);
                    Cluster topLevelCluster = TopologyManager.getTopology().getService(serviceType).getCluster(clusterId);
                    groupInstanceBean.getClusterInstances().add(ObjectConverter.
                            convertClusterToClusterInstanceBean(groupInstanceBean.getInstanceId(),
                                    topLevelCluster, entry.getKey()));
                } finally {
                    TopologyManager.releaseReadLockForCluster(serviceType, clusterId);
                }
            }
        }

    }

    private static void setSubGroupInstances(Group group, GroupInstanceBean groupInstanceBean) {
        Collection<Group> subgroups = group.getGroups();
        addClustersInstancesToGroupInstanceBean(groupInstanceBean, group);
        if (subgroups != null && !subgroups.isEmpty()) {
            for (Group subGroup : subgroups) {
                List<GroupInstanceBean> groupInstanceBeans = ObjectConverter.
                        convertGroupToGroupInstancesBean(groupInstanceBean.getInstanceId(),
                                subGroup);
                for (GroupInstanceBean groupInstanceBean1 : groupInstanceBeans) {
                    setSubGroupInstances(subGroup, groupInstanceBean1);
                    groupInstanceBean.getGroupInstances().add(groupInstanceBean1);
                }

            }
        }

    }

    // Util methods for Kubernetes clusters

    public static boolean addKubernetesCluster(KubernetesClusterBean kubernetesClusterBean) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster kubernetesCluster =
                    ObjectConverter.convertToCCKubernetesClusterPojo(kubernetesClusterBean);

            try {
                return cloudControllerServiceClient.deployKubernetesCluster(kubernetesCluster);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesClusterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static boolean addKubernetesHost(String kubernetesClusterId, KubernetesHostBean kubernetesHostBean)
            throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost kubernetesHost =
                    ObjectConverter.convertKubernetesHostToStubKubernetesHost(kubernetesHostBean);

            try {
                return cloudControllerServiceClient.addKubernetesHost(kubernetesClusterId, kubernetesHost);
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceInvalidKubernetesHostExceptionException e) {
                String message = e.getFaultMessage().getInvalidKubernetesHostException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message, e);
                throw new RestAPIException(message, e);
            }
        }
        return false;
    }

    public static boolean updateKubernetesMaster(KubernetesMasterBean kubernetesMasterBean) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster kubernetesMaster =
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

    public static KubernetesClusterBean[] getAvailableKubernetesClusters() throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster[]
                        kubernetesClusters = cloudControllerServiceClient.getAvailableKubernetesClusters();
                if (kubernetesClusters == null) {
                    log.error("Could not find any Kubernetes Clusters.");
                    return null;
                }

                return ObjectConverter.convertStubKubernetesClustersToKubernetesClusters(kubernetesClusters);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            }
        }
        return null;
    }

    public static KubernetesClusterBean getKubernetesCluster(String kubernetesClusterId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesCluster
                        kubernetesCluster = cloudControllerServiceClient.getKubernetesCluster(kubernetesClusterId);
                return ObjectConverter.convertStubKubernetesClusterToKubernetesCluster(kubernetesCluster);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    public static boolean removeKubernetesCluster(String kubernetesClusterId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                return cloudControllerServiceClient.undeployKubernetesCluster(kubernetesClusterId);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
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

    public static KubernetesHostBean[] getKubernetesHosts(String kubernetesClusterId) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost[]
                        kubernetesHosts = cloudControllerServiceClient.getKubernetesHosts(kubernetesClusterId);

                List<KubernetesHostBean> arrayList = ObjectConverter.convertStubKubernetesHostsToKubernetesHosts(
                        kubernetesHosts);
                KubernetesHostBean[] array = new KubernetesHostBean[arrayList.size()];
                array = arrayList.toArray(array);
                return array;
            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    public static KubernetesMasterBean getKubernetesMaster(String kubernetesClusterId) throws RestAPIException {
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesMaster
                        kubernetesMaster = cloudControllerServiceClient.getKubernetesMaster(kubernetesClusterId);
                return ObjectConverter.convertStubKubernetesMasterToKubernetesMaster(kubernetesMaster);

            } catch (RemoteException e) {
                log.error(e.getMessage(), e);
                throw new RestAPIException(e.getMessage(), e);
            } catch (CloudControllerServiceNonExistingKubernetesClusterExceptionException e) {
                String message = e.getFaultMessage().getNonExistingKubernetesClusterException().getMessage();
                log.error(message);
                throw new RestAPIException(message, e);
            }
        }
        return null;
    }

    public static boolean updateKubernetesHost(KubernetesHostBean kubernetesHostBean) throws RestAPIException {
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            org.apache.stratos.cloud.controller.stub.domain.kubernetes.KubernetesHost kubernetesHost =
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

    public static void addApplicationSignUp(String applicationId, ApplicationSignUpBean applicationSignUpBean)
            throws RestAPIException {

        if (StringUtils.isBlank(applicationId)) {
            throw new RestAPIException("Application id is null");
        }

        ApplicationBean applicationBean = getApplication(applicationId);
        Application application = ApplicationManager.getApplications().getApplication(applicationId);

        if ((applicationBean == null) || (application == null)) {
            throw new RestAPIException("Application not found: [application-id] " + applicationId);
        }

        if (!APPLICATION_STATUS_DEPLOYED.equals(applicationBean.getStatus())) {
            throw new RestAPIException("Application has not been deployed: [application-id] " + applicationId);
        }

        if (!applicationBean.isMultiTenant()) {
            throw new RestAPIException("Application signups cannot be added to single-tenant applications");
        }

        if (applicationSignUpBean == null) {
            throw new RestAPIException("Application signup is null");
        }

        try {
            if (log.isInfoEnabled()) {
                log.info(String.format("Adding applicationBean signup: [application-id] %s", applicationId));
            }

            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

            ApplicationSignUp applicationSignUp = ObjectConverter.convertApplicationSignUpBeanToStubApplicationSignUp(
                    applicationSignUpBean);
            applicationSignUp.setApplicationId(applicationId);
            applicationSignUp.setTenantId(tenantId);
            List<String> clusterIds = findApplicationClusterIds(application);
            String[] clusterIdsArray = clusterIds.toArray(new String[clusterIds.size()]);
            applicationSignUp.setClusterIds(clusterIdsArray);

            // Encrypt artifact repository passwords
            encryptRepositoryPasswords(applicationSignUp, application.getKey());

            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.addApplicationSignUp(applicationSignUp);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup added successfully: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }

            serviceClient.notifyArtifactUpdatedEventForSignUp(applicationId, tenantId);
            if (log.isInfoEnabled()) {
                log.info(String.format("Artifact updated event sent: [application-id] %s [tenant-id] %d",
                        applicationId, tenantId));
            }
        } catch (Exception e) {
            String message = "Error in applicationBean signup: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    /**
     * Find application cluster ids.
     *
     * @param application Application
     * @return list of cluster Ids
     */
    private static List<String> findApplicationClusterIds(Application application) {
        List<String> clusterIds = new ArrayList<String>();
        for (ClusterDataHolder clusterDataHolder : application.getClusterDataRecursively()) {
            clusterIds.add(clusterDataHolder.getClusterId());
        }
        return clusterIds;
    }

    /**
     * Encrypt artifact repository passwords.
     *
     * @param applicationSignUp Application Signup
     * @param applicationKey Application Key
     */
    private static void encryptRepositoryPasswords(ApplicationSignUp applicationSignUp, String applicationKey) {
        if (applicationSignUp.getArtifactRepositories() != null) {
            for (ArtifactRepository artifactRepository : applicationSignUp.getArtifactRepositories()) {
                if (artifactRepository != null) {
                    String repoPassword = artifactRepository.getRepoPassword();
                    if ((StringUtils.isNotBlank(repoPassword))) {
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
    }

    public static ApplicationSignUpBean getApplicationSignUp(String applicationId) throws RestAPIException {
        if (StringUtils.isBlank(applicationId)) {
            throw new RestAPIException("Application id is null");
        }

        ApplicationBean application = getApplication(applicationId);
        if (application == null) {
            throw new RestAPIException("Application does not exist: [application-id] " + applicationId);
        }

        if (!application.isMultiTenant()) {
            throw new RestAPIException("Application singups not available for single-tenant applications");
        }

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            ApplicationSignUp applicationSignUp = serviceClient.getApplicationSignUp(applicationId, tenantId);
            if (applicationSignUp != null) {
                return ObjectConverter.convertStubApplicationSignUpToApplicationSignUpBean(applicationSignUp);
            }
            return null;
        } catch (Exception e) {
            String message = String.format("Could not get application signup: [application-id] %s [tenant-id] %d",
                    applicationId, tenantId);
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static void removeApplicationSignUp(String applicationId) throws RestAPIException {
        if (StringUtils.isBlank(applicationId)) {
            throw new RestAPIException("Application id is null");
        }

        ApplicationBean application = getApplication(applicationId);
        if (application == null) {
            throw new RestAPIException("Application does not exist: [application-id] " + applicationId);
        }

        if (!application.isMultiTenant()) {
            throw new RestAPIException("Application singups not available for single-tenant applications");
        }

        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            serviceClient.removeApplicationSignUp(applicationId, tenantId);

            if (log.isInfoEnabled()) {
                log.info(String.format("Application signup removed successfully: [application-id] %s" +
                        "[tenant-id] %d", applicationId, tenantId));
            }
        } catch (Exception e) {
            String message = String.format("Could not remove application signup: [application-id] %s [tenant-id] %d ",
                    applicationId, tenantId);
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static List<ApplicationSignUpBean> getApplicationSignUps(String applicationId) throws RestAPIException {
        if (StringUtils.isBlank(applicationId)) {
            throw new RestAPIException("Application id is null");
        }

        ApplicationBean application = getApplication(applicationId);
        if (application == null) {
            throw new RestAPIException("Application does not exist: [application-id] " + applicationId);
        }

        if (!application.isMultiTenant()) {
            throw new RestAPIException("Application singups not available for single-tenant applications");
        }

        try {
            List<ApplicationSignUpBean> applicationSignUpBeans = new ArrayList<ApplicationSignUpBean>();
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            ApplicationSignUp[] applicationSignUps = serviceClient.getApplicationSignUps(applicationId);
            if (applicationSignUps != null) {
                for (ApplicationSignUp applicationSignUp : applicationSignUps) {
                    if (applicationSignUp != null) {
                        ApplicationSignUpBean applicationSignUpBean =
                                ObjectConverter.convertStubApplicationSignUpToApplicationSignUpBean(applicationSignUp);
                        applicationSignUpBeans.add(applicationSignUpBean);
                    }
                }
            }
            return applicationSignUpBeans;
        } catch (Exception e) {
            String message = "Could not get application signups: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static void addApplicationDomainMappings(
            String applicationId, ApplicationDomainMappingsBean domainMapppingsBean) throws RestAPIException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            if (domainMapppingsBean.getDomainMappings() != null) {
                StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();

                for (DomainMappingBean domainMappingBean : domainMapppingsBean.getDomainMappings()) {
                    ClusterDataHolder clusterDataHolder = findClusterDataHolder(
                            applicationId,
                            domainMappingBean.getCartridgeAlias());

                    DomainMapping domainMapping = ObjectConverter.convertDomainMappingBeanToStubDomainMapping(
                            domainMappingBean);
                    domainMapping.setApplicationId(applicationId);
                    domainMapping.setTenantId(tenantId);
                    domainMapping.setServiceName(clusterDataHolder.getServiceType());
                    domainMapping.setClusterId(clusterDataHolder.getClusterId());
                    serviceClient.addDomainMapping(domainMapping);

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Domain mapping added: [application-id] %s [tenant-id] %d " +
                                        "[domain-name] %s [context-path] %s", applicationId, tenantId,
                                domainMapping.getDomainName(), domainMapping.getContextPath()));
                    }
                }
            }
        } catch (Exception e) {
            String message = "Could not add domain mappings: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    private static ClusterDataHolder findClusterDataHolder(String applicationId, String cartridgeAlias) {
        Application application = ApplicationManager.getApplications().getApplication(applicationId);
        if (application == null) {
            throw new RuntimeException(String.format("Application not found: [application-id] %s", applicationId));
        }

        ClusterDataHolder clusterDataHolder = application.getClusterData(cartridgeAlias);
        if (clusterDataHolder == null) {
            throw new RuntimeException(String.format("Cluster data not found for cartridge alias: [application-id] %s " +
                    "[cartridge-alias] %s", applicationId, cartridgeAlias));
        }
        return clusterDataHolder;
    }

    public static void removeApplicationDomainMappings(String applicationId,
                                                       ApplicationDomainMappingsBean domainMapppingsBean)
            throws RestAPIException {

        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            if (domainMapppingsBean.getDomainMappings() != null) {

                for (DomainMappingBean domainMappingBean : domainMapppingsBean.getDomainMappings()) {
                    serviceClient.removeDomainMapping(applicationId, tenantId, domainMappingBean.getDomainName());

                    if (log.isInfoEnabled()) {
                        log.info(String.format("Domain mapping removed: [application-id] %s [tenant-id] %d " +
                                        "[domain-name] %s", applicationId, tenantId,
                                domainMappingBean.getDomainName()));
                    }
                }
            }
        } catch (Exception e) {
            String message = "Could not remove domain mappings: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static List<DomainMappingBean> getApplicationDomainMappings(String applicationId) throws RestAPIException {
        try {
            int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
            List<DomainMappingBean> domainMappingsBeans = new ArrayList<DomainMappingBean>();
            StratosManagerServiceClient serviceClient = StratosManagerServiceClient.getInstance();
            DomainMapping[] domainMappings = serviceClient.getDomainMappings(applicationId, tenantId);
            if (domainMappings != null) {
                for (DomainMapping domainMapping : domainMappings) {
                    if (domainMapping != null) {
                        DomainMappingBean domainMappingBean =
                                ObjectConverter.convertStubDomainMappingToDomainMappingBean(domainMapping);
                        domainMappingsBeans.add(domainMappingBean);
                    }
                }
            }
            return domainMappingsBeans;
        } catch (Exception e) {
            String message = "Could not get domain mappings: [application-id] " + applicationId;
            log.error(message, e);
            throw new RestAPIException(message, e);
        }
    }

    public static void addNetworkPartition(NetworkPartitionBean networkPartitionBean) {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            serviceClient.addNetworkPartition(
                    ObjectConverter.convertNetworkPartitionToCCStubNetworkPartition(networkPartitionBean));
        } catch (Exception e) {
            String message = "Could not add network partition";
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public static NetworkPartitionBean[] getNetworkPartitions() {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition[] networkPartitions =
                    serviceClient.getNetworkPartitions();
            return ObjectConverter.convertCCStubNetworkPartitionsToNetworkPartitions(networkPartitions);
        } catch (Exception e) {
            String message = "Could not get network partitions";
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public static void removeNetworkPartition(String networkPartitionId) {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            serviceClient.removeNetworkPartition(networkPartitionId);
        } catch (Exception e) {
            String message = String.format(
                    "Could not remove network partition: [network-partition-id] %s",
                    networkPartitionId);
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public static NetworkPartitionBean getNetworkPartition(String networkPartitionId) {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            org.apache.stratos.cloud.controller.stub.domain.NetworkPartition networkPartition =
                    serviceClient.getNetworkPartition(networkPartitionId);
            return ObjectConverter.convertCCStubNetworkPartitionToNetworkPartition(networkPartition);
        } catch (Exception e) {
            String message = String.format(
                    "Could not get network partition: [network-partition-id] %s",
                    networkPartitionId);
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    public static void updateNetworkPartition(NetworkPartitionBean networkPartition) {
        try {
            CloudControllerServiceClient serviceClient = CloudControllerServiceClient.getInstance();
            serviceClient.updateNetworkPartition(ObjectConverter.
                    convertNetworkPartitionToCCStubNetworkPartition(networkPartition));
        } catch (Exception e) {
            String message = String.format("Could not update network partition: [network-partition-id] %s,",
                    networkPartition.getId());
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Add deployment policy
     *
     * @param deployementPolicyDefinitionBean DeploymentPolicyBean
     */
    public static void addDeploymentPolicy(DeploymentPolicyBean deployementPolicyDefinitionBean)
            throws RestAPIException {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Adding deployment policy: [deployment-policy-id] %s ",
                        deployementPolicyDefinitionBean.getId()));
            }

            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy =

                    ObjectConverter.convertToASDeploymentPolicy(deployementPolicyDefinitionBean);
            AutoscalerServiceClient.getInstance().addDeploymentPolicy(deploymentPolicy);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Successfully added deploymentPolicy: [deployment-policy-id] %s ",
                        deployementPolicyDefinitionBean.getId()));
            }
        } catch (AutoscalerServiceDeploymentPolicyNotExistsExceptionException e) {
            String msg =
                    "Deployment policy already exists [Deployment-policy-id]" + deployementPolicyDefinitionBean.getId();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (Exception e) {
            String msg = "Could not add deployment policy.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    /**
     * Get deployment policy by deployment policy id
     *
     * @param deploymentPolicyID deployment policy id
     * @return {@link DeploymentPolicyBean}
     */
    public static DeploymentPolicyBean getDeployementPolicy(String deploymentPolicyID) throws RestAPIException {

        DeploymentPolicyBean deploymentPolicyBean;
        try {

            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy
                    = AutoscalerServiceClient.getInstance().getDeploymentPolicy(deploymentPolicyID);
            if (deploymentPolicy == null) {
                return null;
            }
            deploymentPolicyBean = ObjectConverter.convetCCStubDeploymentPolicytoDeploymentPolicy(deploymentPolicy);
        } catch (Exception e) {
            String msg = "Could not find deployment policy: [deployment-policy-id] " + deploymentPolicyID;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        
        return deploymentPolicyBean;
    }

    /**
     * Get deployment policies
     *
     * @return array of {@link DeploymentPolicyBean}
     */
    public static DeploymentPolicyBean[] getDeployementPolicies() {
        try {
            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy[] deploymentPolicies
                    = AutoscalerServiceClient.getInstance().getDeploymentPolicies();
            return ObjectConverter.convertASStubDeploymentPoliciesToDeploymentPolicies(deploymentPolicies);
        } catch (Exception e) {
            String message = "Could not get deployment policies";
            log.error(message);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Update deployement policy
     *
     * @param deploymentPolicyDefinitionBean DeploymentPolicyBean
     * @throws RestAPIException
     */
    public static void updateDeploymentPolicy(DeploymentPolicyBean deploymentPolicyDefinitionBean)
            throws RestAPIException {
        try {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Updating deployment policy: [deployment-policy-id] %s ",
                        deploymentPolicyDefinitionBean.getId()));
            }

            org.apache.stratos.autoscaler.stub.deployment.policy.DeploymentPolicy deploymentPolicy =
                    ObjectConverter.convertToASDeploymentPolicy(deploymentPolicyDefinitionBean);

            AutoscalerServiceClient.getInstance().updateDeploymentPolicy(deploymentPolicy);

            if (log.isDebugEnabled()) {
                log.debug(String.format("DeploymentPolicy updated successfully : [deployment-policy-id] %s ",
                        deploymentPolicyDefinitionBean.getId()));
            }
        } catch (AutoscalerServiceDeploymentPolicyNotExistsExceptionException e) {
            String msg =
                    "Deployment policy does not exist [Deployment-policy-id]" + deploymentPolicyDefinitionBean.getId();
            log.error(msg, e);
            throw new RestAPIException(msg);
        } catch (Exception e) {
            String msg = "Could not update deployment policy "+e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    /**
     * Remove deployment policy
     *
     * @param deploymentPolicyID Deployment policy ID
     * @throws RestAPIException
     */
    public static void removeDeploymentPolicy(String deploymentPolicyID)
            throws RestAPIException {
        try {
            AutoscalerServiceClient.getInstance().removeDeploymentPolicy(deploymentPolicyID);
        } catch (Exception e) {
            String msg = "Could not remove deployment policy "+e.getLocalizedMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }


    public static ClusterBean getClusterInfo(String clusterId) throws RestAPIException {
        if (StringUtils.isEmpty(clusterId)) {
            throw new RestAPIException("Cluster Id can not be empty");
        }

        Cluster cluster = TopologyManager.getTopology().getCluster(clusterId);
        if (cluster == null){
            return null;
        }

        return ObjectConverter.convertClusterToClusterBean(cluster, clusterId);
    }

    //util methods for Tenants

    /**
     * Add Tenant
     *
     * @param tenantInfoBean
     * @throws RestAPIException
     */
    public static void addTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws RestAPIException {

        try {
            CommonUtil.validateEmail(tenantInfoBean.getEmail());
        } catch (Exception e) {
            String msg = "Invalid email is provided";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        String tenantDomain = tenantInfoBean.getTenantDomain();
        try {
            TenantMgtUtil.validateDomain(tenantDomain);
        } catch (Exception e) {
            String msg = "Tenant domain validation error for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        UserRegistry userRegistry = (UserRegistry) PrivilegedCarbonContext.getThreadLocalCarbonContext().
                getRegistry(RegistryType.USER_GOVERNANCE);
        if (userRegistry == null) {
            log.error("Security alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session.");
            throw new RestAPIException("Security alert! User registry is null. A user is trying create a tenant "
                    + " without an authenticated session.");
        }

        if (userRegistry.getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            log.error("Security alert! None super tenant trying to create a tenant.");
            throw new RestAPIException("Security alert! None super tenant trying to create a tenant.");
        }

        Tenant tenant = TenantMgtUtil
                .initializeTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        TenantPersistor persistor = ServiceHolder.getTenantPersistor();
        // not validating the domain ownership, since created by super tenant
        int tenantId; //TODO verify whether this is the correct approach (isSkeleton)
        try {
            tenantId = persistor
                    .persistTenant(tenant, false, tenantInfoBean.getSuccessKey(), tenantInfoBean.getOriginatedService(),
                            false);
        } catch (Exception e) {
            String msg = "Could not add tenant: " + e.getMessage();
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        tenantInfoBean.setTenantId(tenantId);

        try {
            TenantMgtUtil.addClaimsToUserStoreManager(tenant);
        } catch (Exception e) {
            String msg = "Error in granting permissions for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        //Notify tenant addition
        try {
            TenantMgtUtil.triggerAddTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        } catch (StratosException e) {
            String msg = "Error in notifying tenant addition.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
        // For the super tenant tenant creation, tenants are always activated as they are created.
        try {
            TenantMgtUtil.activateTenantInitially(
                    ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean), tenantId);
        } catch (Exception e) {
            String msg = "Error in initial activation of tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        try {
            TenantMgtUtil.prepareStringToShowThemeMgtPage(tenant.getId());
        } catch (RegistryException e) {
            String msg = "Error in preparing theme mgt page for tenant " + tenantDomain;
            log.error(msg, e);
            throw new RestAPIException(msg);
        }
    }

    /**
     * Update Existing Tenant
     *
     * @param tenantInfoBean
     * @throws Exception
     */
    public static void updateExistingTenant(org.apache.stratos.common.beans.TenantInfoBean tenantInfoBean) throws Exception {

        TenantManager tenantManager = ServiceHolder.getTenantManager();
        UserStoreManager userStoreManager;

        // filling the non-set admin and admin password first
        UserRegistry configSystemRegistry = ServiceHolder.getRegistryService()
                .getConfigSystemRegistry(tenantInfoBean.getTenantId());

        String tenantDomain = tenantInfoBean.getTenantDomain();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new TenantNotFoundException(msg, e);
        }

        // filling the first and last name values
        if (tenantInfoBean.getFirstname() != null && !tenantInfoBean.getFirstname().trim().equals("")) {
            try {
                CommonUtil.validateName(tenantInfoBean.getFirstname(), "First Name");
            } catch (Exception e) {
                String msg = "Invalid first name is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }
        if (tenantInfoBean.getLastname() != null && !tenantInfoBean.getLastname().trim().equals("")) {
            try {
                CommonUtil.validateName(tenantInfoBean.getLastname(), "Last Name");
            } catch (Exception e) {
                String msg = "Invalid last name is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        }

        tenant.setAdminFirstName(tenantInfoBean.getFirstname());
        tenant.setAdminLastName(tenantInfoBean.getLastname());
        TenantMgtUtil.addClaimsToUserStoreManager(tenant);

        // filling the email value
        if (tenantInfoBean.getEmail() != null && !tenantInfoBean.getEmail().equals("")) {
            // validate the email
            try {
                CommonUtil.validateEmail(tenantInfoBean.getEmail());
            } catch (Exception e) {
                String msg = "Invalid email is provided.";
                log.error(msg, e);
                throw new Exception(msg, e);
            }
            tenant.setEmail(tenantInfoBean.getEmail());
        }

        UserRealm userRealm = configSystemRegistry.getUserRealm();
        try {
            userStoreManager = userRealm.getUserStoreManager();
        } catch (UserStoreException e) {
            String msg = "Error in getting the user store manager for tenant, tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        boolean updatePassword = false;
        if (tenantInfoBean.getAdminPassword() != null && !tenantInfoBean.getAdminPassword().equals("")) {
            updatePassword = true;
        }
        if (!userStoreManager.isReadOnly() && updatePassword) {
            // now we will update the tenant admin with the admin given
            // password.
            try {
                userStoreManager.updateCredentialByAdmin(tenantInfoBean.getAdmin(), tenantInfoBean.getAdminPassword());
            } catch (UserStoreException e) {
                String msg = "Error in changing the tenant admin password, tenant domain: " +
                        tenantInfoBean.getTenantDomain() + ". " + e.getMessage() + " for: " +
                        tenantInfoBean.getAdmin();
                log.error(msg, e);
                throw new Exception(msg, e);
            }
        } else {
            //Password should be empty since no password update done
            tenantInfoBean.setAdminPassword("");
        }

        try {
            tenantManager.updateTenant(tenant);
        } catch (UserStoreException e) {
            String msg = "Error in updating the tenant for tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new Exception(msg, e);
        }

        //Notify tenant update to all listeners
        try {
            TenantMgtUtil
                    .triggerUpdateTenant(ObjectConverter.convertTenantInfoBeanToCarbonTenantInfoBean(tenantInfoBean));
        } catch (StratosException e) {
            String msg = "Error in notifying tenant update.";
            log.error(msg, e);
            throw new Exception(msg, e);
        }
    }

    /**
     * Get a Tenant by Domain
     *
     * @param tenantDomain
     * @return
     * @throws Exception
     */
    public static org.apache.stratos.common.beans.TenantInfoBean getTenantByDomain(String tenantDomain) throws Exception {

        TenantManager tenantManager = ServiceHolder.getTenantManager();

        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg);
            throw new Exception(msg, e);
        }
        Tenant tenant;
        try {
            tenant = (Tenant) tenantManager.getTenant(tenantId);
        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant from the tenant manager.";
            log.error(msg);
            throw new Exception(msg, e);
        }

        TenantInfoBean bean;
        try {
            bean = ObjectConverter
                    .convertCarbonTenantInfoBeanToTenantInfoBean(TenantMgtUtil.initializeTenantInfoBean(tenantId, tenant));
        } catch (NullPointerException e) {
            log.error(String.format("Couldn't find tenant for provided tenant domain. [Tenant Domain] %s", tenantDomain));
            return null;
        }

        // retrieve first and last names from the UserStoreManager
        bean.setFirstname(ClaimsMgtUtil.getFirstNamefromUserStoreManager(ServiceHolder.getRealmService(), tenantId));
        bean.setLastname(ClaimsMgtUtil.getLastNamefromUserStoreManager(ServiceHolder.getRealmService(), tenantId));

        //getting the subscription plan
        String activePlan = "";
        //TODO: usage plan using billing service

        if (activePlan != null && activePlan.trim().length() > 0) {
            bean.setUsagePlan(activePlan);
        } else {
            bean.setUsagePlan("");
        }

        return bean;
    }

    /**
     * Get a list of available Tenants
     *
     * @return
     * @throws RestAPIException
     */
    public static List<org.apache.stratos.common.beans.TenantInfoBean> getAllTenants() throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            tenants = (Tenant[]) tenantManager.getAllTenants();
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = new ArrayList<org.apache.stratos.common.beans.TenantInfoBean>();
        for (Tenant tenant : tenants) {
            org.apache.stratos.common.beans.TenantInfoBean bean = ObjectConverter
                    .convertCarbonTenantInfoBeanToTenantInfoBean(
                            TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant));
            tenantList.add(bean);
        }
        return tenantList;
    }

    /**
     * Get List of Partial Tenant Domains
     *
     * @param domain
     * @return
     * @throws RestAPIException
     */
    public static List<org.apache.stratos.common.beans.TenantInfoBean> searchPartialTenantsDomains(String domain)
            throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        Tenant[] tenants;
        try {
            domain = domain.trim();
            tenants = (Tenant[]) tenantManager.getAllTenantsForTenantDomainStr(domain);
        } catch (Exception e) {
            String msg = "Error in retrieving the tenant information.";
            log.error(msg, e);
            throw new RestAPIException(msg);
        }

        List<org.apache.stratos.common.beans.TenantInfoBean> tenantList = new ArrayList<org.apache.stratos.common.beans.TenantInfoBean>();
        for (Tenant tenant : tenants) {
            org.apache.stratos.common.beans.TenantInfoBean bean = ObjectConverter
                    .convertCarbonTenantInfoBeanToTenantInfoBean(
                            TenantMgtUtil.getTenantInfoBeanfromTenant(tenant.getId(), tenant));
            tenantList.add(bean);
        }
        return tenantList;
    }


    /**
     * Activate a Tenant
     *
     * @param tenantDomain
     * @throws RestAPIException
     */
    public static void activateTenant(String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);

        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " + tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

        try {
            TenantMgtUtil.activateTenant(tenantDomain, tenantManager, tenantId);

        } catch (Exception e) {
            throw new RestAPIException(e);
        }

        //Notify tenant activation all listeners
        try {
            TenantMgtUtil.triggerTenantActivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant activate.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }
    }

    /**
     * Deactivate Tenant
     *
     * @param tenantDomain
     * @throws RestAPIException
     */
    public static void deactivateTenant(String tenantDomain) throws RestAPIException {
        TenantManager tenantManager = ServiceHolder.getTenantManager();
        int tenantId;
        try {
            tenantId = tenantManager.getTenantId(tenantDomain);

        } catch (UserStoreException e) {
            String msg = "Error in retrieving the tenant id for the tenant domain: " +
                    tenantDomain + ".";
            log.error(msg, e);
            throw new RestAPIException(msg, e);

        }

        try {
            TenantMgtUtil.deactivateTenant(tenantDomain, tenantManager, tenantId);
        } catch (Exception e) {
            throw new RestAPIException(e);
        }

        //Notify tenant deactivation all listeners
        try {
            TenantMgtUtil.triggerTenantDeactivation(tenantId);
        } catch (StratosException e) {
            String msg = "Error in notifying tenant deactivate.";
            log.error(msg, e);
            throw new RestAPIException(msg, e);
        }

    }

    //Util methods for Users

    /**
     * Adds an User
     *
     * @param userInfoBean
     * @throws RestAPIException
     */
    public static void addUser(UserInfoBean userInfoBean) throws RestAPIException {
        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.addUser(getTenantUserStoreManager(), userInfoBean);

        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }

    }


    /**
     * Get Tenant UserStoreManager
     *
     * @return UserStoreManager
     * @throws UserManagerException
     */
    private static UserStoreManager getTenantUserStoreManager() throws UserManagerException {

        CarbonContext carbonContext = CarbonContext.getThreadLocalCarbonContext();
        UserRealm userRealm;
        UserStoreManager userStoreManager;

        try {
            userRealm = carbonContext.getUserRealm();
            userStoreManager = userRealm.getUserStoreManager();

        } catch (UserStoreException e) {
            String msg = "Error in retrieving UserStore Manager";
            log.error(msg, e);
            throw new UserManagerException(msg, e);
        }

        return userStoreManager;
    }

    /**
     * Delete an user
     *
     * @param userName
     * @throws RestAPIException
     */
    public static void removeUser(String userName) throws RestAPIException{
        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.removeUser(getTenantUserStoreManager(), userName);
        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }
    }

    /**
     * Update User
     *
     * @param userInfoBean
     * @throws RestAPIException
     */
    public static void updateUser(UserInfoBean userInfoBean) throws RestAPIException {
        StratosUserManager stratosUserManager = new StratosUserManager();

        try {
            stratosUserManager.updateUser(getTenantUserStoreManager(), userInfoBean);

        } catch (UserManagerException e) {
            throw new RestAPIException(e.getMessage());
        }

    }

    /**
     * Get List of Users
     *
     * @return
     * @throws RestAPIException
     */
    public static List<UserInfoBean> getUsers() throws RestAPIException {
        StratosUserManager stratosUserManager = new StratosUserManager();
        List<UserInfoBean> userList;
        try {
           userList  = stratosUserManager.getAllUsers(getTenantUserStoreManager());
        }catch (UserManagerException e){
            throw new RestAPIException(e.getMessage());
        }
        return userList;
    }

}
