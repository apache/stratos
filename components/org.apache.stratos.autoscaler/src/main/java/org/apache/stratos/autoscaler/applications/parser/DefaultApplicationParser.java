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

package org.apache.stratos.autoscaler.applications.parser;

import org.apache.amber.oauth2.common.exception.OAuthProblemException;
import org.apache.amber.oauth2.common.exception.OAuthSystemException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationUtils;
import org.apache.stratos.autoscaler.applications.ClusterInformation;
import org.apache.stratos.autoscaler.applications.MTClusterInformation;
import org.apache.stratos.autoscaler.applications.STClusterInformation;
import org.apache.stratos.autoscaler.applications.payload.PayloadData;
import org.apache.stratos.autoscaler.applications.pojo.*;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.client.IdentityApplicationManagementServiceClient;
import org.apache.stratos.autoscaler.client.OAuthAdminServiceClient;
import org.apache.stratos.autoscaler.exception.AutoScalerException;
import org.apache.stratos.autoscaler.exception.application.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.cartridge.CartridgeInformationException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.autoscaler.util.AutoscalerUtil;
import org.apache.stratos.cloud.controller.stub.domain.CartridgeInfo;
import org.apache.stratos.common.Properties;
import org.apache.stratos.messaging.domain.application.*;
import org.wso2.carbon.identity.oauth.stub.OAuthAdminServiceException;

import java.rmi.RemoteException;
import java.util.*;

/**
 * Default implementation of the Application Parser. One Application should be processed by one
 * instance of the DefaultApplicationParser.
 */
public class DefaultApplicationParser implements ApplicationParser {

	private static final String METADATA_APPENDER = "-";
	private static Log log = LogFactory.getLog(DefaultApplicationParser.class);

    private List<ApplicationClusterContext> applicationClusterContexts;
    private Map<String, Properties> aliasToProperties;
    private Map<String, SubscribableInfoContext> subscribableInformation = new HashMap<String, SubscribableInfoContext>();
    private String oauthToken;

    public DefaultApplicationParser() {
        this.applicationClusterContexts = new ArrayList<ApplicationClusterContext>();
        this.setAliasToProperties(new HashMap<String, Properties>());

    }

    @Override
    public Application parse(ApplicationContext applicationContext) throws ApplicationDefinitionException {

        if (applicationContext == null) {
            handleError("Application context is null");
        }

        assert applicationContext != null;
        if (applicationContext.getAlias() == null || applicationContext.getAlias().isEmpty()) {
            handleError("Application alias not found application");
        }

        if (applicationContext.getApplicationId() == null || applicationContext.getApplicationId().isEmpty()) {
            handleError("Application id not found in application");
        }

        // get the Subscribables Information
        Map<String, SubscribableInfoContext> subscribablesInfo = getSubscribableInformation(applicationContext);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, SubscribableInfoContext>> subscribableInfoCtxtEntries = subscribablesInfo.entrySet();
            log.debug("Defined Subscribable Information: [ ");
            for (Map.Entry<String, SubscribableInfoContext> subscribableInfoCtxtEntry : subscribableInfoCtxtEntries) {
                log.debug("Subscribable Information alias: " + subscribableInfoCtxtEntry.getKey());
            }
            log.debug(" ]");
        }

        if (subscribablesInfo == null) {
            handleError("Invalid application definition, subscribable information not found");
        }

        oauthToken = createToken(applicationContext.getApplicationId());
        return buildCompositeAppStructure(applicationContext, subscribablesInfo);
    }

    @Override
    public List<ApplicationClusterContext> getApplicationClusterContexts() throws ApplicationDefinitionException {
        return applicationClusterContexts;
    }


    private Map<String, SubscribableInfoContext> getSubscribableInfo(GroupContext[] groupContexts) throws
            ApplicationDefinitionException {
        if (groupContexts != null) {
            for (GroupContext groupContext : groupContexts) {
                if (groupContext.getGroupContexts() != null) {
                    getSubscribableInfo(groupContext.getGroupContexts());
                } else {
                    CartridgeContext[] cartridgeContexts = groupContext.getCartridgeContexts();
                    for (CartridgeContext cartridgeContext : cartridgeContexts) {

                        if (StringUtils.isEmpty(cartridgeContext.getSubscribableInfoContext().getAlias()) ||
                                !ApplicationUtils.isAliasValid(cartridgeContext.getSubscribableInfoContext().getAlias())) {
                            handleError("Invalid alias specified for Subscribable Information Obj: [ " +
                                    cartridgeContext.getSubscribableInfoContext().getAlias() + " ]");
                        }

                        // check if a group is already defined under the same alias
                        if (subscribableInformation.get(cartridgeContext.getSubscribableInfoContext().getAlias()) != null) {
                            // a group with same alias already exists, can't continue
                            handleError("A Subscribable Info obj with alias " + cartridgeContext.getSubscribableInfoContext().getAlias() + " already exists");
                        }
                        subscribableInformation.put(cartridgeContext.getSubscribableInfoContext().getAlias(),
                                cartridgeContext.getSubscribableInfoContext());
                        if (log.isDebugEnabled()) {
                            log.debug("Added Subcribables Info obj [ " +
                                    cartridgeContext.getSubscribableInfoContext().getAlias() + " ] to map [cartridge alias -> Subscribable Information]");
                        }
                    }
                }
            }
        }
        return subscribableInformation;
    }

    /**
     * Extract Subscription Information from the Application Definition
     *
     * @param appCtxt ApplicationContext object with Application information
     * @return Map [cartridge alias -> Group]
     * @throws ApplicationDefinitionException if the Subscription information is invalid
     */
    private Map<String, SubscribableInfoContext> getSubscribableInformation(ApplicationContext appCtxt) throws
            ApplicationDefinitionException {

        return getSubscribableInfo(appCtxt.getComponents().getGroupContexts());
    }


    /**
     * Builds the Application structure
     *
     * @param applicationContext    ApplicationContext object with Application information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @return Application Application object denoting the Application structure
     * @throws ApplicationDefinitionException If an error occurs in building the Application structure
     */
    private Application buildCompositeAppStructure(ApplicationContext applicationContext,
                                                   Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Application application = new Application(applicationContext.getApplicationId());

        // Set tenant information
        application.setTenantId(applicationContext.getTenantId());
        application.setTenantDomain(applicationContext.getTenantDomain());
        application.setTenantAdminUserName(applicationContext.getTenantAdminUsername());

        Set<StartupOrder> startupOrderSet = new HashSet<StartupOrder>();
        DependencyOrder dependencyOrder = new DependencyOrder();
        dependencyOrder.setStartupOrders(startupOrderSet);
        application.setDependencyOrder(dependencyOrder);

        ComponentContext components = applicationContext.getComponents();
        if (components != null) {
            DependencyContext dependencyContext = components.getDependencyContext();

            // Set top level dependencies
            if (dependencyContext != null) {
                // Set startup orders
                String[] startupOrders = dependencyContext.getStartupOrdersContexts();
                if (startupOrders != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsing application: startupOrders != null for app alias: " +
                                applicationContext.getAlias() + " #: " + startupOrders.length);
                    }
                    dependencyOrder.setStartupOrders(ParserUtils.convertStartupOrder(startupOrders));
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsing application: startupOrders == null for app alias: " +
                                applicationContext.getAlias());
                    }
                }

                // Set scaling dependents
                String[] scalingDependents = dependencyContext.getScalingDependents();
                if (scalingDependents != null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsing application: scalingDependents != null for app alias: " +
                                applicationContext.getAlias() + " #: " + scalingDependents.length);
                    }
                    dependencyOrder.setScalingDependents(ParserUtils.convertScalingDependentList(scalingDependents));
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Parsing application: scalingDependents == null for app alias: " +
                                applicationContext.getAlias());
                    }
                }

                // Set termination behaviour
                String terminationBehaviour = dependencyContext.getTerminationBehaviour();
                validateTerminationBehavior(terminationBehaviour);
                dependencyOrder.setTerminationBehaviour(terminationBehaviour);
            }

            // Set application cluster data
            if (components.getCartridgeContexts() != null) {
                List<CartridgeContext> cartridgeContextList = Arrays.asList(components.getCartridgeContexts());
                Set<StartupOrder> startupOrders = application.getDependencyOrder().getStartupOrders();
                Map<String, Map<String, ClusterDataHolder>> clusterDataMap;

                clusterDataMap = parseLeafLevelSubscriptions(applicationContext.getApplicationId(),
                        applicationContext.getTenantId(), application.getKey(), null, cartridgeContextList, startupOrders);
                application.setClusterData(clusterDataMap.get("alias"));
                application.setClusterDataForType(clusterDataMap.get("type"));
            }

            // Set groups
            if (components.getGroupContexts() != null) {
                application.setGroups(
		                parseGroups(applicationContext.getApplicationId(), applicationContext.getTenantId(),
		                            application.getKey(), Arrays.asList(components.getGroupContexts()),
		                            subscribableInfoCtxts));
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("Application parsed successfully: [application-id] " + applicationContext.getApplicationId());
        }
        return application;
    }


    /**
     * Parse Subscription Information
     *
     * @param appId                Application id
     * @param tenantId             Tenant id of tenant which deployed the Application
     * @param key                  Generated key for the Application
     * @param groupName            Group name (if relevant)
     * @param cartridgeContextList cartridgeContextList
     * @return Map [subscription alias -> ClusterDataHolder]
     * @throws ApplicationDefinitionException
     */
    private Map<String, Map<String, ClusterDataHolder>> parseLeafLevelSubscriptions(
            String appId, int tenantId, String key, String groupName,
            List<CartridgeContext> cartridgeContextList, Set<StartupOrder> dependencyOrder) throws ApplicationDefinitionException {

        Map<String, Map<String, ClusterDataHolder>> completeDataHolder = new HashMap<String, Map<String, ClusterDataHolder>>();
        Map<String, ClusterDataHolder> clusterDataMap = new HashMap<String, ClusterDataHolder>();
        Map<String, ClusterDataHolder> clusterDataMapByType = new HashMap<String, ClusterDataHolder>();

	    createClusterDataMap(cartridgeContextList, clusterDataMap, clusterDataMapByType);

	    for (CartridgeContext cartridgeContext : cartridgeContextList) {
            List<String> dependencyClusterIDs = new ArrayList<String>();
		    List<String> exportMetadataKeys = new ArrayList<String>();
		    List<String> importMetadataKeys = new ArrayList<String>();
            String cartridgeType = cartridgeContext.getType();
            SubscribableInfoContext subscribableInfoContext = cartridgeContext.getSubscribableInfoContext();
            String subscriptionAlias = subscribableInfoContext.getAlias();


            // check if a cartridgeInfo with relevant type is already deployed. else, can't continue
            CartridgeInfo cartridgeInfo = getCartridge(cartridgeType);

		    for (String str : cartridgeInfo.getMetadataKeys()) {
			    exportMetadataKeys.add(cartridgeContext.getSubscribableInfoContext()
			                                           .getAlias() + METADATA_APPENDER +str);
		    }


            if (cartridgeInfo == null) {
                handleError("No deployed Cartridge found with type [ " + cartridgeType +
                        " ] for Composite Application");
            }

            // get hostname and cluster id
            ClusterInformation clusterInfo;
            assert cartridgeInfo != null;
            if (cartridgeInfo.getMultiTenant()) {
                clusterInfo = new MTClusterInformation();
            } else {
                clusterInfo = new STClusterInformation();
            }

            String hostname = clusterInfo.getHostName(subscriptionAlias, cartridgeInfo.getHostName());
            String clusterId = clusterInfo.getClusterId(subscriptionAlias, cartridgeType);
            String repoUrl = null;
            if (subscribableInfoContext.getArtifactRepositoryContext() != null) {
                repoUrl = subscribableInfoContext.getArtifactRepositoryContext().getRepoUrl();
            }

           //Get dependency cluster id
            if (dependencyOrder != null) {
                for (StartupOrder startupOrder : dependencyOrder) {
                    for (String startupOrderComponent : startupOrder.getStartupOrderComponentList()) {

	                    String[] arrStartUp= startupOrderComponent.split("\\.");
	                    if(arrStartUp[0].equals("cartridge")) {
		                    String dependencType = arrStartUp[1];
		                    CartridgeInfo dependencyCartridge = getCartridge(dependencType);

		                    ClusterDataHolder dataHolder = clusterDataMapByType.get(dependencType);

		                    if (dataHolder != null) {
			                    if (!dataHolder.getClusterId().equals(clusterId)) {
				                    dependencyClusterIDs.add(dataHolder.getClusterId());
				                    for (String str : dependencyCartridge.getMetadataKeys()) {
					                    importMetadataKeys
							                    .add(dataHolder.getClusterId().split("\\.")[0] + METADATA_APPENDER + str);
				                    }
				                    if (!dataHolder.getClusterId().equals(clusterId)) {
					                    if (startupOrderComponent.equals("cartridge.".concat(cartridgeType))) {
						                    break;
					                    }
				                    }
			                    }
		                    }
	                    }
                    }
                }
            }
            String[] arrDependencyClusterIDs = new String[dependencyClusterIDs.size()];
            arrDependencyClusterIDs = dependencyClusterIDs.toArray(arrDependencyClusterIDs);

		    String[] arrExportMetadata = new String[exportMetadataKeys.size()];
		    arrExportMetadata = exportMetadataKeys.toArray(arrExportMetadata);
		    String[] arrImportMetadata = new String[importMetadataKeys.size()];
		    arrImportMetadata = importMetadataKeys.toArray(arrImportMetadata);

            // Find tenant range of cluster
            String tenantRange = AutoscalerUtil.findTenantRange(tenantId, cartridgeInfo.getTenantPartitions());
		    Boolean isLB=false;
			if(cartridgeInfo.getCategory().equals("lb")){
				isLB=true;
		    }
            // create and collect this cluster's information
            ApplicationClusterContext appClusterCtxt = createApplicationClusterContext(appId, groupName, cartridgeInfo,
                    key, tenantId, repoUrl, subscriptionAlias, clusterId, hostname,
                    subscribableInfoContext.getDeploymentPolicy(), isLB,
                    tenantRange, subscribableInfoContext.getDependencyAliases(),
                    subscribableInfoContext.getProperties(), arrDependencyClusterIDs, arrExportMetadata,
                    arrImportMetadata);

            appClusterCtxt.setAutoscalePolicyName(subscribableInfoContext.getAutoscalingPolicy());
            appClusterCtxt.setProperties(subscribableInfoContext.getProperties());
            this.applicationClusterContexts.add(appClusterCtxt);


        }
        completeDataHolder.put("type", clusterDataMapByType);
        completeDataHolder.put("alias", clusterDataMap);
        return completeDataHolder;
    }

	private void createClusterDataMap(List<CartridgeContext> cartridgeContextList,
	                                  Map<String, ClusterDataHolder> clusterDataMap,
	                                  Map<String, ClusterDataHolder> clusterDataMapByType)
			throws ApplicationDefinitionException {
		for (CartridgeContext cartridgeContext : cartridgeContextList) {

			String cartridgeType = cartridgeContext.getType();
			SubscribableInfoContext subscribableInfoContext = cartridgeContext.getSubscribableInfoContext();
			String subscriptionAlias = subscribableInfoContext.getAlias();

			// check if a cartridgeInfo with relevant type is already deployed. else, can't continue
			CartridgeInfo cartridgeInfo = getCartridge(cartridgeType);
			if (cartridgeInfo == null) {
				handleError("No deployed Cartridge found with type [ " + cartridgeType +
				            " ] for Composite Application");
			}

			// get hostname and cluster id
			ClusterInformation clusterInfo;
			assert cartridgeInfo != null;
			if (cartridgeInfo.getMultiTenant()) {
				clusterInfo = new MTClusterInformation();
			} else {
				clusterInfo = new STClusterInformation();
			}

			String clusterId = clusterInfo.getClusterId(subscriptionAlias, cartridgeType);
			// add relevant information to the map
			ClusterDataHolder clusterDataHolderPerType = new ClusterDataHolder(cartridgeType, clusterId);
			clusterDataHolderPerType.setMinInstances(cartridgeContext.getCartridgeMin());
			clusterDataHolderPerType.setMaxInstances(cartridgeContext.getCartridgeMax());
			clusterDataMapByType.put(cartridgeType, clusterDataHolderPerType);
			// add relevant information to the map
			ClusterDataHolder clusterDataHolder = new ClusterDataHolder(cartridgeType, clusterId);
			clusterDataHolder.setMinInstances(cartridgeContext.getCartridgeMin());
			clusterDataHolder.setMaxInstances(cartridgeContext.getCartridgeMax());
			clusterDataMap.put(subscriptionAlias, clusterDataHolder);

		}
	}

	/**
     * Validates terminationBehavior. The terminationBehavior should be one of the following:
     * 1. terminate-none
     * 2. terminate-dependents
     * 3. terminate-all
     *
     * @throws ApplicationDefinitionException if terminationBehavior is different to what is
     *                                        listed above
     */
    private static void validateTerminationBehavior(String terminationBehavior) throws ApplicationDefinitionException {

        if (!(terminationBehavior == null ||
                AutoscalerConstants.TERMINATE_NONE.equals(terminationBehavior) ||
                AutoscalerConstants.TERMINATE_DEPENDENTS.equals(terminationBehavior) ||
                AutoscalerConstants.TERMINATE_ALL.equals(terminationBehavior))) {
            throw new ApplicationDefinitionException("Invalid termination behaviour found: [ " +
                    terminationBehavior + " ], should be one of '" +
                    AutoscalerConstants.TERMINATE_NONE + "', '" +
                    AutoscalerConstants.TERMINATE_DEPENDENTS + "', '" +
                    AutoscalerConstants.TERMINATE_ALL + "'");
        }
    }

    /**
     * Parse Group information
     *
     * @param appId                   Application id
     * @param tenantId                tenant id of tenant which deployed the Application
     * @param key                     Generated key for the Application
     * @param groupCtxts              Group information
     * @param subscribableInformation Subscribable Information
     * @return Map [alias -> Group]
     * @throws ApplicationDefinitionException if an error occurs in parsing Group Information
     */
    private Map<String, Group> parseGroups(String appId, int tenantId, String key, List<GroupContext> groupCtxts,
                                           Map<String, SubscribableInfoContext> subscribableInformation)
            throws ApplicationDefinitionException {

        Map<String, Group> groupAliasToGroup = new HashMap<String, Group>();

        for (GroupContext groupCtxt : groupCtxts) {
            ServiceGroup serviceGroup = getServiceGroup(groupCtxt.getName());
            if (serviceGroup == null) {
                throw new RuntimeException("Cartridge group not found: [group-name] " + groupCtxt.getName());
            }
            Group group = parseGroup(appId, tenantId, key, groupCtxt, subscribableInformation, serviceGroup);
            groupAliasToGroup.put(group.getAlias(), group);
        }

        Set<Group> nestedGroups = new HashSet<Group>();
        getNestedGroupContexts(nestedGroups, groupAliasToGroup.values());
        filterDuplicatedGroupContexts(groupAliasToGroup.values(), nestedGroups);

        return groupAliasToGroup;
    }

    /**
     * Extracts nested Group information recursively
     *
     * @param nestedGroups Nested Groups set to be populated recursively
     * @param groups       Collection of Groups
     */
    private void getNestedGroupContexts(Set<Group> nestedGroups, Collection<Group> groups) {

        if (groups != null) {
            for (Group group : groups) {
                if (group.getGroups() != null) {
                    nestedGroups.addAll(group.getGroups());
                    getNestedGroupContexts(nestedGroups, group.getGroups());
                }
            }
        }
    }

    /**
     * Filters duplicated Groups from top level
     *
     * @param topLevelGroups Top level Groups
     * @param nestedGroups   nested Groups
     */
    private void filterDuplicatedGroupContexts(Collection<Group> topLevelGroups, Set<Group> nestedGroups) {

        for (Group nestedGroup : nestedGroups) {
            filterNestedGroupFromTopLevel(topLevelGroups, nestedGroup);
        }
    }

    private void filterNestedGroupFromTopLevel(Collection<Group> topLevelGroups, Group nestedGroup) {

        Iterator<Group> parentIterator = topLevelGroups.iterator();
        while (parentIterator.hasNext()) {
            Group parentGroup = parentIterator.next();
            // if there is an exactly similar nested Group Context and a top level Group Context
            // it implies that they are duplicates. Should be removed from top level.
            if (parentGroup.equals(nestedGroup)) {
                parentIterator.remove();
            }
        }
    }

    /**
     * Parses an individual Group
     *
     * @param appId                 Application id
     * @param tenantId              tenant id of tenant which deployed the Application
     * @param key                   Generated key for the Application
     * @param groupCtxt             Group definition information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @return Group object
     * @throws ApplicationDefinitionException if unable to parse
     */
    private Group parseGroup(String appId, int tenantId, String key, GroupContext groupCtxt,
                             Map<String, SubscribableInfoContext> subscribableInfoCtxts,
                             ServiceGroup serviceGroup)
            throws ApplicationDefinitionException {

        Group group = new Group(appId, groupCtxt.getName(), groupCtxt.getAlias());
        group.setGroupScalingEnabled(isGroupScalingEnabled(groupCtxt.getName(), serviceGroup));
        group.setGroupMinInstances(groupCtxt.getGroupMinInstances());
        group.setGroupMaxInstances(groupCtxt.getGroupMaxInstances());
        DependencyOrder dependencyOrder = new DependencyOrder();
        // create the Dependency Ordering
        String[] startupOrders = getStartupOrderForGroup(groupCtxt.getName(), serviceGroup);
        Set<StartupOrder> setStartUpOrder = null;
        if (startupOrders != null) {
            setStartUpOrder = ParserUtils.convertStartupOrder(startupOrders, groupCtxt);
            dependencyOrder.setStartupOrders(setStartUpOrder);
        }
        String[] scaleDependents = getScaleDependentForGroup(groupCtxt.getName(), serviceGroup);
        if (scaleDependents != null) {
            dependencyOrder.setScalingDependents(ParserUtils.convertScalingDependentList(scaleDependents, groupCtxt));
        }
        dependencyOrder.setTerminationBehaviour(getKillbehaviour(groupCtxt.getName(), serviceGroup));
        //dependencyOrder.setScalingDependents(scalingDependents);
        group.setDependencyOrder(dependencyOrder);

        Map<String, Map<String, ClusterDataHolder>> clusterDataMap;

        // get group level CartridgeContexts
        if (groupCtxt.getCartridgeContexts() != null) {
            clusterDataMap = parseLeafLevelSubscriptions(appId, tenantId, key, groupCtxt.getName(),
                    Arrays.asList(groupCtxt.getCartridgeContexts()), setStartUpOrder);
            group.setClusterData(clusterDataMap.get("alias"));
            group.setClusterDataForType(clusterDataMap.get("type"));

        }

        // get nested groups
        if (groupCtxt.getGroupContexts() != null) {
            Map<String, Group> nestedGroups = new HashMap<String, Group>();
            // check sub groups
            for (GroupContext subGroupCtxt : groupCtxt.getGroupContexts()) {
                // get the complete Group Definition
                if (subGroupCtxt != null) {
                    for (ServiceGroup nestedServiceGroup : serviceGroup.getGroups()) {
                        if (nestedServiceGroup.getName().equals(subGroupCtxt.getName())) {
                            Group nestedGroup = parseGroup(appId, tenantId, key,
                                    subGroupCtxt, subscribableInfoCtxts,
                                    nestedServiceGroup);
                            nestedGroups.put(nestedGroup.getAlias(), nestedGroup);
                        }
                    }

                }
            }

            group.setGroups(nestedGroups);
        }

        return group;
    }

    /**
     * Find the startup order
     *
     * @param serviceGroup GroupContext with Group defintion information
     * @return Set of Startup Orders which are defined in the Group
     * @throws ApplicationDefinitionException
     */
    private String[] getStartupOrderForGroup(String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedServiceGroup = getNestedServiceGroup(serviceGroupName, serviceGroup);

        if (nestedServiceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        if (log.isDebugEnabled()) {
            log.debug("parsing application ... getStartupOrderForGroup: " + serviceGroupName);
        }

        assert nestedServiceGroup != null;
        if (nestedServiceGroup.getDependencies() != null) {
            if (log.isDebugEnabled()) {
                log.debug("parsing application ... getStartupOrderForGroup: dependencies != null ");
            }
            if (nestedServiceGroup.getDependencies().getStartupOrders() != null) {

                String[] startupOrders = nestedServiceGroup.getDependencies().getStartupOrders();
                if (log.isDebugEnabled()) {
                    log.debug("parsing application ... getStartupOrderForGroup: startupOrders != null # of: " + startupOrders.length);
                }
                return startupOrders;
            }
        }

        return null;
    }


    /**
     * Find the scale dependent order
     *
     * @param serviceGroup GroupContext with Group defintion information
     * @return Set of Startup Orders which are defined in the Group
     * @throws ApplicationDefinitionException
     */
    private String[] getScaleDependentForGroup(String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedServiceGroup = getNestedServiceGroup(serviceGroupName, serviceGroup);

        if (nestedServiceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        if (log.isDebugEnabled()) {
            log.debug("parsing application ... getScaleDependentForGroup: " + serviceGroupName);
        }

        assert nestedServiceGroup != null;
        if (nestedServiceGroup.getDependencies() != null) {
            if (log.isDebugEnabled()) {
                log.debug("parsing application ... getScaleDependentForGroup: dependencies != null ");
            }
            if (nestedServiceGroup.getDependencies().getScalingDependants() != null) {

                String[] scalingDependants = nestedServiceGroup.getDependencies().getScalingDependants();
                if (log.isDebugEnabled()) {
                    log.debug("parsing application ... getScaleDependentForGroup: scalingDependants != null # of: " + scalingDependants.length);
                }
                return scalingDependants;
            }
        }

        return null;
    }


    /**
     * Get kill behaviour related to a Group
     *
     * @param serviceGroupName Group name
     * @return String indicating the kill behavior
     * @throws ApplicationDefinitionException if an error occurs
     */
    private String getKillbehaviour(String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedServiceGroup = getNestedServiceGroup(serviceGroupName, serviceGroup);

        if (nestedServiceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        assert nestedServiceGroup != null;
        if (nestedServiceGroup.getDependencies() != null) {
            return nestedServiceGroup.getDependencies().getTerminationBehaviour();
        }

        return null;

    }

    /**
     * Checks if group scaling is enabled for Service Group with name serviceGroupName
     *
     * @param serviceGroupName name of the Service Group
     * @return true if group scaling is enabled, else false
     * @throws ApplicationDefinitionException if no Service Group found for the given serviceGroupName
     */
    private boolean isGroupScalingEnabled(String serviceGroupName, ServiceGroup serviceGroup) throws ApplicationDefinitionException {

        ServiceGroup nestedGroup = getNestedServiceGroup(serviceGroupName, serviceGroup);

        if (nestedGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        return nestedGroup.isGroupscalingEnabled();
    }

    private ServiceGroup getNestedServiceGroup(String serviceGroupName, ServiceGroup serviceGroup) {
        if (serviceGroup.getName().equals(serviceGroupName)) {
            return serviceGroup;
        } else if (serviceGroup.getGroups() != null) {
            ServiceGroup[] groups = serviceGroup.getGroups();
            for (ServiceGroup sg : groups) {
                return getNestedServiceGroup(serviceGroupName, sg);
            }
        }
        return null;

    }

    /**
     * Retrieves deployed Service Group instance
     *
     * @param serviceGroupName name of the Service Group
     * @return ServiceGroup instance if exists
     * @throws ApplicationDefinitionException if no Service Group found for the given serviceGroupName
     */
    private ServiceGroup getServiceGroup(String serviceGroupName) throws ApplicationDefinitionException {

        try {
            return RegistryManager.getInstance().getServiceGroup(serviceGroupName);
        } catch (Exception e) {
            String errorMsg = "Error in getting Service Group Definition for [ " + serviceGroupName +
                    " ] from Registry";
            log.error(errorMsg, e);
            throw new ApplicationDefinitionException(errorMsg, e);
        }
    }


    /**
     * Creates a ApplicationClusterContext object to keep information related to a Cluster in this Application
     *
     * @param appId                Application id
     * @param groupName            Group name
     * @param cartridgeInfo        Cartridge information
     * @param subscriptionKey      Generated key for the Application
     * @param tenantId             Tenant Id of the tenant which deployed the Application
     * @param repoUrl              Repository URL
     * @param alias                alias specified for this Subscribable in the Application Definition
     * @param clusterId            Cluster id
     * @param hostname             Hostname
     * @param deploymentPolicy     Deployment policy used
     * @param isLB                 if this cluster is an LB
     * @param dependencyClustorIDs
     * @return ApplicationClusterContext object with relevant information
     * @throws ApplicationDefinitionException If any error occurs
     */
    private ApplicationClusterContext createApplicationClusterContext(String appId, String groupName, CartridgeInfo cartridgeInfo,
                                                                      String subscriptionKey, int tenantId, String repoUrl,
                                                                      String alias, String clusterId, String hostname,
                                                                      String deploymentPolicy, boolean isLB, String tenantRange,
                                                                      String[] dependencyAliases, Properties properties, String[] dependencyClustorIDs,
                                                                      String[] exportMetadata, String[] importMetadata)
            throws ApplicationDefinitionException {

        // Create text payload
        PayloadData payloadData = ApplicationUtils.createPayload(appId, groupName, cartridgeInfo, subscriptionKey, tenantId, clusterId,
                hostname, repoUrl, alias, null, dependencyAliases, properties, oauthToken, dependencyClustorIDs,exportMetadata,importMetadata);

        String textPayload = payloadData.toString();
        log.debug("Payload :: " + textPayload);
        return new ApplicationClusterContext(cartridgeInfo.getType(), clusterId, hostname, textPayload, deploymentPolicy, isLB, tenantRange, dependencyClustorIDs);
    }

    public String createToken(String applicationId) throws AutoScalerException {
        String token = null;
        String ouathAppName = applicationId + Math.random();
        String serviceProviderName = ouathAppName;

        try {
            OAuthAdminServiceClient.getServiceClient().registerOauthApplication(ouathAppName);
        } catch (RemoteException e) {
            throw new AutoScalerException(e);
        } catch (OAuthAdminServiceException e) {
            throw new AutoScalerException(e);
        }

        String errorMessage = String.format("Could not create oauth token: [application-id] %s", applicationId);

        try {
            token = IdentityApplicationManagementServiceClient.getServiceClient().createServiceProvider(ouathAppName,
                    serviceProviderName, applicationId);
        } catch (RemoteException e) {
            log.error(errorMessage, e);
            throw new AutoScalerException(errorMessage, e);
        } catch (OAuthAdminServiceException e) {
            log.error(errorMessage, e);
            throw new AutoScalerException(errorMessage, e);
        } catch (OAuthProblemException e) {
            log.error(errorMessage, e);
            throw new AutoScalerException(errorMessage, e);
        } catch (OAuthSystemException e) {
            log.error(errorMessage, e);
            throw new AutoScalerException(errorMessage, e);
        }

        return token;
    }

    private CartridgeInfo getCartridge(String cartridgeType) throws ApplicationDefinitionException {

        try {
            return CloudControllerClient.getInstance().getCartrdgeInformation(cartridgeType);
        } catch (CartridgeInformationException e) {
            throw new ApplicationDefinitionException(e);
        }
    }

    private void handleError(String errorMsg) throws ApplicationDefinitionException {
        log.error(errorMsg);
        throw new ApplicationDefinitionException(errorMsg);
    }

    public Map<String, Properties> getAliasToProperties() {
        return aliasToProperties;
    }

    public void setAliasToProperties(Map<String, Properties> aliasToProperties) {
        this.aliasToProperties = aliasToProperties;
    }

    public void addProperties(String alias, Properties properties) {
        this.getAliasToProperties().put(alias, properties);
    }
}
