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
import org.apache.stratos.adc.mgt.client.AutoscalerServiceClient;
import org.apache.stratos.adc.mgt.client.CloudControllerServiceClient;
import org.apache.stratos.adc.mgt.dao.CartridgeSubscriptionInfo;
import org.apache.stratos.adc.mgt.dto.Cartridge;
import org.apache.stratos.adc.mgt.dto.SubscriptionInfo;
import org.apache.stratos.adc.mgt.exception.*;
import org.apache.stratos.adc.mgt.internal.DataHolder;
import org.apache.stratos.adc.mgt.manager.CartridgeSubscriptionManager;
import org.apache.stratos.adc.mgt.subscription.CartridgeSubscription;
import org.apache.stratos.adc.mgt.topology.model.TopologyClusterModel;
import org.apache.stratos.adc.mgt.utils.ApplicationManagementUtil;
import org.apache.stratos.adc.mgt.utils.CartridgeConstants;
import org.apache.stratos.adc.mgt.utils.PersistenceManager;
import org.apache.stratos.adc.topology.mgt.service.TopologyManagementService;
import org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy;
import org.apache.stratos.cloud.controller.pojo.CartridgeConfig;
import org.apache.stratos.cloud.controller.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.pojo.LoadbalancerConfig;
import org.apache.stratos.cloud.controller.pojo.Properties;
import org.apache.stratos.messaging.domain.topology.Cluster;
import org.apache.stratos.rest.endpoint.Constants;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.Partition;
import org.apache.stratos.rest.endpoint.bean.autoscaler.partition.PartitionGroup;
import org.apache.stratos.rest.endpoint.bean.autoscaler.policy.autoscale.AutoscalePolicy;
import org.apache.stratos.rest.endpoint.bean.cartridge.definition.CartridgeDefinitionBean;
import org.apache.stratos.rest.endpoint.bean.util.converter.PojoConverter;
import org.apache.stratos.rest.endpoint.exception.RestAPIException;
import org.wso2.carbon.context.PrivilegedCarbonContext;

import java.util.*;
import java.util.regex.Pattern;

public class ServiceUtils {
    private static Log log = LogFactory.getLog(StratosAdmin.class);
    private static CartridgeSubscriptionManager cartridgeSubsciptionManager = new CartridgeSubscriptionManager();

    static void deployCartridge (CartridgeDefinitionBean cartridgeDefinitionBean, ConfigurationContext ctxt,
        String userName, String tenantDomain) throws RestAPIException {

        log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        
        if (cloudControllerServiceClient != null) {

            CartridgeConfig cartridgeConfig = PojoConverter.populateCartridgeConfigPojo(cartridgeDefinitionBean);

            if(cartridgeConfig == null) {
                throw new RestAPIException("Populated CartridgeConfig instance is null, cartridge deployment aborted");
            }

            try {
                
                // call CC
                cloudControllerServiceClient.deployCartridgeDefinition(cartridgeConfig);
                
                org.apache.stratos.cloud.controller.pojo.Property lbRefType;
                // analyze properties and pick up, if not a LB.
                Properties properties = cartridgeConfig.getProperties();
                String cartridgeType = cartridgeConfig.getType();
                if (properties != null && properties.getProperties() != null) {
                    for (org.apache.stratos.cloud.controller.pojo.Property prop : 
                        properties.getProperties()) {

                        if (Constants.IS_LOAD_BALANCER.equals(prop.getName())) {
                            if ("true".equals(prop.getValue())) {
                                if (log.isDebugEnabled()) {
                                    log.debug("This is a load balancer Cartridge definition. " +
                                            "[Type] " + cartridgeType);
                                }
                                return;
                            }
                        } 

                    }
                }
                
                // if not an LB Cartridge
                LoadbalancerConfig lbConfig = cartridgeConfig.getLbConfig();
                
                if(lbConfig == null || lbConfig.getProperties() == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("This cartridge does not require a load balancer. " +
                                "[Type] " + cartridgeType);
                    }
                    return;
                }
                
                // retrieve lb Cartridge info
                CartridgeInfo cartridgeInfo = cloudControllerServiceClient.getCartridgeInfo(lbConfig.getType());
                
                properties = lbConfig.getProperties();
                
                for (org.apache.stratos.cloud.controller.pojo.Property prop : 
                        properties.getProperties()) {
                    
                    // TODO make following a chain of responsibility pattern
                    if (Constants.NO_LOAD_BALANCER.equals(prop.getName())) {
                        if ("true".equals(prop.getValue())) {
                            if (log.isDebugEnabled()) {
                                log.debug("This cartridge does not require a load balancer. " +
                                          "[Type] " + cartridgeType);
                            }
                            return;
                        }
                    } else if (Constants.EXISTING_LOAD_BALANCERS.equals(prop.getName())) {
                        String clusterIdsVal = prop.getValue();
                        if (log.isDebugEnabled()) {
                            log.debug("This cartridge refers to existing load balancers. " +
                                      "[Type] " + cartridgeType +
                                      "[Referenced Cluster Ids] " + clusterIdsVal);
                        }

                        String[] clusterIds = clusterIdsVal.split(",");

                        for (String clusterId : clusterIds) {
                            if (autoscalerServiceClient != null) {
                                try {
                                    autoscalerServiceClient.checkLBExistence(clusterId);
                                } catch (Exception ex) {
                                    // we don't need to throw the error here.
                                    log.error(ex.getMessage(), ex);
                                }
                            }
                        }
                        return;

                    } else if (Constants.DEFAULT_LOAD_BALANCER.equals(prop.getName())) {
                        if ("true".equals(prop.getValue())) {
                            if (log.isDebugEnabled()) {
                                log.debug("This cartridge uses default load balancer. " +
                                          "[Type] " + cartridgeType);
                            }
                            if (autoscalerServiceClient != null) {
                                try {
                                    // get the valid policies of this cartridge
                                    DeploymentPolicy[] cartridgeDepPolicies = 
                                            autoscalerServiceClient.getDeploymentPolicies(cartridgeType);
                                    DeploymentPolicy[] lbCartridgeDepPolicies = 
                                            autoscalerServiceClient.getDeploymentPolicies(lbConfig.getType());
                                    // valid deployment policies for this cartridge
                                    DeploymentPolicy[] validDepPolicies = 
                                            intersection(cartridgeDepPolicies, lbCartridgeDepPolicies);
                                    
                                    for (DeploymentPolicy deploymentPolicy : validDepPolicies) {
                                        String alias = "lb"+new Random().nextInt();
                                        subscribe(cartridgeType, alias, cartridgeInfo.getDefaultAutoscalingPolicy(), 
                                                  deploymentPolicy.getId(), null, false, null, null, null, null, 
                                                  ctxt, userName, tenantDomain);
                                    }
                                    
                                } catch (Exception ex) {
                                    // we don't need to throw the error here.
                                    log.error(ex.getMessage(), ex);
                                }
                            }
                            return;
                        } else if (Constants.SERVICE_AWARE_LOAD_BALANCER.equals(prop.getName())) {
                            if ("true".equals(prop.getValue())) {
                                if (log.isDebugEnabled()) {
                                    log.debug("This cartridge uses a service aware load balancer. " +
                                              "[Type] " + cartridgeType);
                                }
                                if (autoscalerServiceClient != null) {
                                    try {
                                        // get the valid policies of this cartridge
                                        DeploymentPolicy[] cartridgeDepPolicies = 
                                                autoscalerServiceClient.getDeploymentPolicies(cartridgeType);
                                        DeploymentPolicy[] lbCartridgeDepPolicies = 
                                                autoscalerServiceClient.getDeploymentPolicies(lbConfig.getType());
                                        // valid deployment policies for this cartridge
                                        DeploymentPolicy[] validDepPolicies = 
                                                intersection(cartridgeDepPolicies, lbCartridgeDepPolicies);
                                        
                                        for (DeploymentPolicy deploymentPolicy : validDepPolicies) {
                                            String alias = "lb"+new Random().nextInt();
                                            subscribe(cartridgeType, alias, cartridgeInfo.getDefaultAutoscalingPolicy(), 
                                                      deploymentPolicy.getId(), null, false, null, null, null, null, 
                                                      ctxt, userName, tenantDomain);
                                        }
                                        
                                    } catch (Exception ex) {
                                        // we don't need to throw the error here.
                                        log.error(ex.getMessage(), ex);
                                    }
                                }
                                return;
                            }
                        }
                    }
                }

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }
    }

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
    
    static void undeployCartridge (String cartridgeType) throws RestAPIException {

        CloudControllerServiceClient cloudControllerServiceClient = getCloudControllerServiceClient();
        if (cloudControllerServiceClient != null) {
            try {
                cloudControllerServiceClient.unDeployCartridgeDefinition(cartridgeType);

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }
    }


    public static boolean deployPartition (Partition partitionBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.cloud.controller.deployment.partition.Partition partition =
                    PojoConverter.convertToCCPartitionPojo(partitionBean);

            try {
                return autoscalerServiceClient.deployPartition(partition);

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }

        return false;
    }

    public static boolean deployAutoscalingPolicy (AutoscalePolicy autoscalePolicyBean) throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.policy.model.AutoscalePolicy autoscalePolicy = PojoConverter.
                    convertToCCAutoscalerPojo(autoscalePolicyBean);

            try {
                return autoscalerServiceClient.deployAutoscalingPolicy(autoscalePolicy);

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }

        return false;
    }

    public static boolean deployDeploymentPolicy (
        org.apache.stratos.rest.endpoint.bean.autoscaler.policy.deployment.DeploymentPolicy deploymentPolicyBean) 
                throws RestAPIException {

        //log.info("***** " + cartridgeDefinitionBean.toString() + " *****");

        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {

            org.apache.stratos.autoscaler.deployment.policy.DeploymentPolicy deploymentPolicy =
                    PojoConverter.convetToCCDeploymentPolicyPojo(deploymentPolicyBean);

            try {
                return autoscalerServiceClient.deployDeploymentPolicy(null);

            } catch (Exception e) {
                throw new RestAPIException(e);
            }
        }

        return false;
    }

    private static CloudControllerServiceClient getCloudControllerServiceClient () {

        try {
            return CloudControllerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error in getting CloudControllerServiceClient instance";
            log.error(errorMsg, axisFault);
        }
        return null;
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
                throw new RestAPIException(errorMsg, e);
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
                String errorMsg = "Error getting available partitions";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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
                String errorMsg = "Error getting available partitions";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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
                String errorMsg = "Error getting available partition";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionPojo(partition);
    }

    private static AutoscalerServiceClient getAutoscalerServiceClient () {

        try {
            return AutoscalerServiceClient.getServiceClient();

        } catch (AxisFault axisFault) {
            String errorMsg = "Error in getting AutoscalerServiceClient instance";
            log.error(errorMsg, axisFault);
        }
        return null;
    }

    public static AutoscalePolicy[] getAutoScalePolicies () throws RestAPIException {

        org.apache.stratos.autoscaler.policy.model.AutoscalePolicy[] autoscalePolicies = null;
        AutoscalerServiceClient autoscalerServiceClient = getAutoscalerServiceClient();
        if (autoscalerServiceClient != null) {
            try {
                autoscalePolicies = autoscalerServiceClient.getAutoScalePolicies();

            } catch (Exception e) {
                String errorMsg = "Error getting available autoscaling policies";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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
                String errorMsg = "Error getting available autoscaling policies";
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

            } catch (Exception e) {
                String errorMsg = "Error getting available deployment policies";
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

            } catch (Exception e) {
                String errorMsg = "Error getting available deployment policies";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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
                String errorMsg = "Error getting available deployment policies";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
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
                String errorMsg = "Error getting available partition groups";
                log.error(errorMsg, e);
                throw new RestAPIException(errorMsg, e);
            }
        }

        return PojoConverter.populatePartitionGroupPojos(partitionGroups);
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


    static SubscriptionInfo subscribe(String cartridgeType, String alias, String autoscalingPolicy, String deploymentPolicy, String repoURL,
                               boolean privateRepo, String repoUsername, String repoPassword, String dataCartridgeType,
                               String dataCartridgeAlias, ConfigurationContext configurationContext, String userName, String tenantDomain) throws ADCException, PolicyException, UnregisteredCartridgeException,
            InvalidCartridgeAliasException, DuplicateCartridgeAliasException, RepositoryRequiredException,
            AlreadySubscribedException, RepositoryCredentialsRequiredException, InvalidRepositoryException,
            RepositoryTransportException {


        CartridgeSubscription cartridgeSubscription = cartridgeSubsciptionManager.subscribeToCartridge(cartridgeType,
                alias.trim(), autoscalingPolicy, deploymentPolicy ,tenantDomain, ApplicationManagementUtil.getTenantId(configurationContext),
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


    public static Cluster getCluster (String cartridgeType, String subscriptionAlias, ConfigurationContext configurationContext) {

        return TopologyClusterModel.getInstance().getCluster(ApplicationManagementUtil.getTenantId(configurationContext)
                ,cartridgeType , subscriptionAlias);
    }

    public static Cluster[] getClustersForTenant (ConfigurationContext configurationContext) {

        Set<Cluster> clusterSet = TopologyClusterModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext));

        return (clusterSet != null && clusterSet.size() > 0 ) ?
                clusterSet.toArray(new Cluster[clusterSet.size()]) : new Cluster[0];

    }

    public static Cluster[] getClustersForTenantAndCartridgeType (ConfigurationContext configurationContext,
                                                                  String cartridgeType) {

        Set<Cluster> clusterSet = TopologyClusterModel.getInstance().getClusters(ApplicationManagementUtil.
                getTenantId(configurationContext), cartridgeType);

        return (clusterSet != null && clusterSet.size() > 0 ) ?
                clusterSet.toArray(new Cluster[clusterSet.size()]) : new Cluster[0];
    }

    static void unsubscribe(String alias, String tenantDomain) throws ADCException, NotSubscribedException {

        cartridgeSubsciptionManager.unsubscribeFromCartridge(tenantDomain, alias);
    }

}
