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
import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dto.Cartridge;
import org.apache.stratos.adc.mgt.dto.SubscriptionInfo;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.manager.CartridgeSubscriptionManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.IaasProviderBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PortMappingBean;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.PropertyBean;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.apache.stratos.rest.endpoint.service.client.CartridgeMgtServiceClient;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ServiceUtils {
    private static Log log = LogFactory.getLog(StratosAdmin.class);
    private static CartridgeSubscriptionManager cartridgeSubsciptionManager = new CartridgeSubscriptionManager();

    static void deployCartridge (CartridgeDefinitionBean cartridgeDefinitionBean) throws RestAPIException {

        log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        CartridgeMgtServiceClient cartridgeMgtServiceClient = getCartridgeMgtServiceClient();
        if (cartridgeMgtServiceClient != null) {

            CartridgeConfig cartridgeConfig = populateCartridgeConfigPojo(cartridgeDefinitionBean);

            if(cartridgeConfig == null) {
                throw new RestAPIException("Populated CartridgeConfig instance is null, cartridge deployment aborted");
            }

            try {
                cartridgeMgtServiceClient.deployCartridgedefinition(cartridgeConfig);

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }
    }

    static CartridgeConfig populateCartridgeConfigPojo (CartridgeDefinitionBean cartridgeDefinitionBean) {

        CartridgeConfig cartridgeConfig = new CartridgeConfig();

        cartridgeConfig.setType(cartridgeDefinitionBean.type);
        cartridgeConfig.setHostName(cartridgeDefinitionBean.host);
        cartridgeConfig.setProvider(cartridgeDefinitionBean.provider);
        cartridgeConfig.setVersion(cartridgeDefinitionBean.version);
        cartridgeConfig.setMultiTenant(cartridgeDefinitionBean.multiTenant);
        cartridgeConfig.setDisplayName(cartridgeDefinitionBean.displayName);
        cartridgeConfig.setDescription(cartridgeDefinitionBean.description);
        //deployment information
        if(cartridgeDefinitionBean.deployment != null) {
            cartridgeConfig.setBaseDir(cartridgeDefinitionBean.deployment.baseDir);
            if(cartridgeDefinitionBean.deployment.dir != null && !cartridgeDefinitionBean.deployment.dir.isEmpty()) {
                cartridgeConfig.setDeploymentDirs(cartridgeDefinitionBean.deployment.dir.
                        toArray(new String[cartridgeDefinitionBean.deployment.dir.size()]));
            }
        }
        //port mapping
        if(cartridgeDefinitionBean.portMapping != null && !cartridgeDefinitionBean.portMapping.isEmpty()) {
            cartridgeConfig.setPortMappings(getPortMappingsAsArray(cartridgeDefinitionBean.portMapping));
        }
        //IaaS
        if(cartridgeDefinitionBean.iaasProvider != null & !cartridgeDefinitionBean.iaasProvider.isEmpty()) {
            cartridgeConfig.setIaasConfigs(getIaasConfigsAsArray(cartridgeDefinitionBean.iaasProvider));
        }
        //Properties
        if(cartridgeDefinitionBean.property != null && !cartridgeDefinitionBean.property.isEmpty()) {
            cartridgeConfig.setProperties(getProperties(cartridgeDefinitionBean.property));
        }

        return cartridgeConfig;
    }

    private static PortMapping[] getPortMappingsAsArray(List<PortMappingBean> portMappingBeans) {

        //convert to an array
        PortMappingBean [] portMappingBeanArray = new PortMappingBean[portMappingBeans.size()];
        portMappingBeans.toArray(portMappingBeanArray);
        PortMapping [] portMappingArray = new PortMapping[portMappingBeanArray.length];

        for (int i = 0 ; i < portMappingBeanArray.length ; i++) {
            PortMapping portMapping = new PortMapping();
            portMapping.setProtocol(portMappingBeanArray[i].protocol);
            portMapping.setPort(Integer.toString(portMappingBeanArray[i].port));
            portMapping.setProxyPort(Integer.toString(portMappingBeanArray[i].proxyPort));
            portMappingArray[i] = portMapping;
        }

        return portMappingArray;
    }

    private static IaasConfig[] getIaasConfigsAsArray (List<IaasProviderBean> iaasProviderBeans) {

        //convert to an array
        IaasProviderBean [] iaasProviderBeansArray = new IaasProviderBean[iaasProviderBeans.size()];
        iaasProviderBeans.toArray(iaasProviderBeansArray);
        IaasConfig [] iaasConfigsArray =  new IaasConfig[iaasProviderBeansArray.length];

        for (int i = 0 ; i < iaasProviderBeansArray.length ; i++) {
            IaasConfig iaasConfig = new IaasConfig();
            iaasConfig.setType(iaasProviderBeansArray[i].type);
            iaasConfig.setImageId(iaasProviderBeansArray[i].imageId);
            iaasConfig.setMaxInstanceLimit(iaasProviderBeansArray[i].maxInstanceLimit);

            if(iaasProviderBeansArray[i].property != null && !iaasProviderBeansArray[i].property.isEmpty()) {
                //set the Properties instance to IaasConfig instance
                iaasConfig.setProperties(getProperties(iaasProviderBeansArray[i].property));
            }
            iaasConfigsArray[i] = iaasConfig;
        }
        return iaasConfigsArray;
    }

    private static Properties getProperties (List<PropertyBean> propertyBeans) {

        //convert to an array
        PropertyBean [] propertyBeansArray = new PropertyBean[propertyBeans.size()];
        propertyBeans.toArray(propertyBeansArray);
        Property [] propertyArray = new Property[propertyBeansArray.length];

        for (int j = 0 ; j < propertyBeansArray.length ; j++) {
            Property property = new Property();
            property.setName(propertyBeansArray[j].name);
            property.setValue(propertyBeansArray[j].value);
            propertyArray[j] = property;
        }

        Properties properties = new Properties();
        properties.setProperties(propertyArray);
        return properties;
    }

    static void undeployCartridge (String cartridgeType) throws RestAPIException {

        CartridgeMgtServiceClient cartridgeMgtServiceClient = getCartridgeMgtServiceClient();
        if (cartridgeMgtServiceClient != null) {
            try {
                cartridgeMgtServiceClient.undeployCartridgeDefinition(cartridgeType);

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }
    }

    private static CartridgeMgtServiceClient getCartridgeMgtServiceClient () {

        try {
            return CartridgeMgtServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error in getting CartridgeMgtServiceClient instance";
            log.error(errorMsg, axisFault);
        }
        return null;
    }

    static List<Cartridge> getAvailableCartridges(String cartridgeSearchString, Boolean multiTenant, ConfigurationContext configurationContext) throws ADCException {
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
                    cartridge.setStatus(CartridgeConstants.NOT_SUBSCRIBED);
                    cartridge.setCartridgeAlias("-");
                    cartridge.setActiveInstances(0);
                    cartridges.add(cartridge);

                    if (cartridgeInfo.getMultiTenant() && !allowMultipleSubscription) {
                        // If the cartridge is multi-tenant. We should not let users
                        // createSubscription twice.
                        if (PersistenceManager.isAlreadySubscribed(cartridgeType,
                                ApplicationManagementUtil.getTenantId(configurationContext))) {
                            if (log.isDebugEnabled()) {
                                log.debug("Already subscribed to " + cartridgeType
                                        + ". This multi-tenant cartridge will not be available to createSubscription");
                            }
                            cartridge.setStatus(CartridgeConstants.SUBSCRIBED);
                        }
                    }
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no available cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error when getting available cartridges. " + e.getMessage();
            log.error(msg, e);
            throw new ADCException("An error occurred getting available cartridges ", e);
        }

        Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning available cartridges " + cartridges.size());
        }

        return cartridges;
    }

    static List<Cartridge> getSubscribedCartridges(String cartridgeSearchString, ConfigurationContext configurationContext) throws ADCException {
        List<Cartridge> cartridges = new ArrayList<Cartridge>();

        if (log.isDebugEnabled()) {
            log.debug("Getting subscribed cartridges. Search String: " + cartridgeSearchString);
        }

        try {
            Pattern searchPattern = getSearchStringPattern(cartridgeSearchString);

            List<CartridgeSubscriptionInfo> subscriptionList = PersistenceManager
                    .retrieveSubscribedCartridges(ApplicationManagementUtil.getTenantId(configurationContext));

            if (subscriptionList != null && !subscriptionList.isEmpty()) {
                for (CartridgeSubscriptionInfo subscription : subscriptionList) {
                    CartridgeInfo cartridgeInfo = null;
                    try {
                        cartridgeInfo = CloudControllerServiceClient.getServiceClient().getCartridgeInfo(
                                subscription.getCartridge());
                    } catch (Exception e) {
                        if (log.isWarnEnabled()) {
                            log.warn("Error when calling getCartridgeInfo for " + subscription.getCartridge()
                                    + ", Error: " + e.getMessage());
                        }
                    }
                    if (cartridgeInfo == null) {
                        // This cannot happen. But continue
                        if (log.isDebugEnabled()) {
                            log.debug("Cartridge Info not found: " + subscription.getCartridge());
                        }
                        continue;
                    }
                    if (!cartridgeMatches(cartridgeInfo, subscription, searchPattern)) {
                        continue;
                    }
                    TopologyManagementService topologyMgtService = DataHolder.getTopologyMgtService();
                    String[] ips = topologyMgtService.getActiveIPs(subscription.getCartridge(),
                            subscription.getClusterDomain(), subscription.getClusterSubdomain());
                    String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                    Cartridge cartridge = ApplicationManagementUtil.populateCartridgeInfo(cartridgeInfo, subscription, ips, tenantDomain);
                    cartridges.add(cartridge);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("There are no subscribed cartridges");
                }
            }
        } catch (Exception e) {
            String msg = "Error when getting subscribed cartridges. " + e.getMessage();
            log.error(msg, e);
            throw new ADCException("An Error occurred when getting subscribed cartridges.", e);
        }

        Collections.sort(cartridges);

        if (log.isDebugEnabled()) {
            log.debug("Returning subscribed cartridges " + cartridges.size());
        }

        return cartridges;
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

    static boolean cartridgeMatches(CartridgeInfo cartridgeInfo, CartridgeSubscriptionInfo cartridgeSubscriptionInfo, Pattern pattern) {
        if (pattern != null) {
            boolean matches = false;
            if (cartridgeInfo.getDisplayName() != null) {
                matches = pattern.matcher(cartridgeInfo.getDisplayName().toLowerCase()).find();
            }
            if (!matches && cartridgeInfo.getDescription() != null) {
                matches = pattern.matcher(cartridgeInfo.getDescription().toLowerCase()).find();
            }
            if (!matches && cartridgeSubscriptionInfo.getCartridge() != null) {
                matches = pattern.matcher(cartridgeSubscriptionInfo.getCartridge().toLowerCase()).find();
            }
            if (!matches && cartridgeSubscriptionInfo.getAlias() != null) {
                matches = pattern.matcher(cartridgeSubscriptionInfo.getAlias().toLowerCase()).find();
            }
            return matches;
        }
        return true;
    }


    static SubscriptionInfo subscribe(String cartridgeType, String alias, String policy, String repoURL,
                               boolean privateRepo, String repoUsername, String repoPassword, String dataCartridgeType,
                               String dataCartridgeAlias, ConfigurationContext configurationContext, String userName, String tenantDomain) throws ADCException, PolicyException, UnregisteredCartridgeException,
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, RepositoryRequiredException,
            AlreadySubscribedException, RepositoryCredentialsRequiredException, InvalidRepositoryException,
            RepositoryTransportException {


        CartridgeSubscription cartridgeSubscription = cartridgeSubsciptionManager.subscribeToCartridge(cartridgeType,
                alias.trim(), policy, tenantDomain, ApplicationManagementUtil.getTenantId(configurationContext),
                userName, "git", repoURL, privateRepo, repoUsername, repoPassword);

        if(dataCartridgeAlias != null && !dataCartridgeAlias.trim().isEmpty()) {

            dataCartridgeAlias = dataCartridgeAlias.trim();

            CartridgeSubscription connectingCartridgeSubscription = null;
            try {
                connectingCartridgeSubscription = cartridgeSubsciptionManager.getCartridgeSubscription(tenantDomain,
                        dataCartridgeAlias);

            } catch (NotSubscribedException e) {
                log.error(e.getMessage(), e);
            }
            if (connectingCartridgeSubscription != null) {
                try {
                    cartridgeSubsciptionManager.connectCartridges(tenantDomain, cartridgeSubscription,
                            connectingCartridgeSubscription.getAlias());

                } catch (NotSubscribedException e) {
                    log.error(e.getMessage(), e);

                } catch (AxisFault axisFault) {
                    log.error(axisFault.getMessage(), axisFault);
                }
            } else {
                log.error("Failed to connect. No cartridge subscription found for tenant " +
                        ApplicationManagementUtil.getTenantId(configurationContext) + " with alias " + alias);
            }
        }

        return cartridgeSubsciptionManager.registerCartridgeSubscription(cartridgeSubscription);

    }

    static void unsubscribe(String alias, String tenantDomain) throws ADCException, NotSubscribedException {

        cartridgeSubsciptionManager.unsubscribeFromCartridge(tenantDomain, alias);
    }

}
