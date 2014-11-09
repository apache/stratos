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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.ApplicationUtils;
import org.apache.stratos.autoscaler.applications.ClusterInformation;
import org.apache.stratos.autoscaler.applications.MTClusterInformation;
import org.apache.stratos.autoscaler.applications.STClusterInformation;
import org.apache.stratos.autoscaler.applications.pojo.*;
import org.apache.stratos.autoscaler.client.CloudControllerClient;
import org.apache.stratos.autoscaler.exception.ApplicationDefinitionException;
import org.apache.stratos.autoscaler.exception.CartridgeInformationException;
import org.apache.stratos.autoscaler.pojo.ServiceGroup;
import org.apache.stratos.autoscaler.registry.RegistryManager;
import org.apache.stratos.cloud.controller.stub.pojo.CartridgeInfo;
import org.apache.stratos.cloud.controller.stub.pojo.Property;
import org.apache.stratos.messaging.domain.applications.Application;
import org.apache.stratos.messaging.domain.applications.ClusterDataHolder;
import org.apache.stratos.messaging.domain.applications.DependencyOrder;
import org.apache.stratos.messaging.domain.applications.Group;
import org.apache.stratos.cloud.controller.stub.pojo.Properties;

import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

/**
 * Default implementation of the Application Parser. One Application should be processed by one
 * instance of the DefaultApplicationParser.
 */
public class DefaultApplicationParser implements ApplicationParser {

    private static Log log = LogFactory.getLog(DefaultApplicationParser.class);

    private Set<ApplicationClusterContext> applicationClusterContexts;
    private Map<String, Properties> aliasToProperties;

    public DefaultApplicationParser () {
        this.applicationClusterContexts = new HashSet<ApplicationClusterContext>();
        this.setAliasToProperties(new HashMap<String, Properties>());

    }

    @Override
    public Application parse(Object obj) throws ApplicationDefinitionException {

        ApplicationContext applicationCtxt = (ApplicationContext) obj;

        if (applicationCtxt == null) {
            handleError("Invalid Composite Application Definition");
        }

        assert applicationCtxt != null;
        if (applicationCtxt.getAlias() == null || applicationCtxt.getAlias().isEmpty()) {
            handleError("Invalid alias specified");
        }

        if (applicationCtxt.getApplicationId() == null || applicationCtxt.getApplicationId().isEmpty()) {
            handleError("Invalid Composite App id specified");
        }

        // get the defined groups
        Map<String, GroupContext> definedGroups = getDefinedGroups(applicationCtxt);
        if (log.isDebugEnabled()) {
            if (definedGroups != null) {
                Set<Map.Entry<String, GroupContext>> groupEntries = definedGroups.entrySet();
                log.debug("Defined Groups: [ ");
                for (Map.Entry<String, GroupContext> groupEntry : groupEntries) {
                    log.debug("Group alias: " + groupEntry.getKey());
                }
                log.debug(" ]");
            } else {
                log.debug("No Group definitions found in app id " + applicationCtxt.getApplicationId());
            }
        }

        // get the Subscribables Information
        Map<String, SubscribableInfoContext> subscribablesInfo = getSubscribableInformation(applicationCtxt);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, SubscribableInfoContext>> subscribableInfoCtxtEntries = subscribablesInfo.entrySet();
            log.debug("Defined Subscribable Information: [ ");
            for (Map.Entry<String, SubscribableInfoContext> subscribableInfoCtxtEntry : subscribableInfoCtxtEntries) {
                log.debug("Subscribable Information alias: " + subscribableInfoCtxtEntry.getKey());
            }
            log.debug(" ]");
        }

        if (subscribablesInfo == null) {
            handleError("Invalid Composite Application Definition, no Subscribable Information specified");
        }

        return buildCompositeAppStructure (applicationCtxt, definedGroups, subscribablesInfo);
    }

    @Override
    public Set<ApplicationClusterContext> getApplicationClusterContexts() throws ApplicationDefinitionException {
        return applicationClusterContexts;
    }

    /**
     * Extract Group information from Application Definition
     *
     * @param appCtxt ApplicationContext object with Application information
     * @return Map [group alias -> Group]
     *
     * @throws ApplicationDefinitionException if the Group information is invalid
     */
    private Map<String, GroupContext> getDefinedGroups (ApplicationContext appCtxt) throws
            ApplicationDefinitionException {

        // map [group alias -> Group Definition]
        Map<String, GroupContext> definedGroups = null;

        if (appCtxt.getComponents() != null) {
            if (appCtxt.getComponents().getGroupContexts() != null) {
                definedGroups = new HashMap<String, GroupContext>();

                for (GroupContext groupContext : appCtxt.getComponents().getGroupContexts()) {

                    // check validity of group name
                    if (StringUtils.isEmpty(groupContext.getName())) {
                        handleError("Invalid Group name specified");
                    }

                    // check if group is deployed
                    if(!isGroupDeployed(groupContext.getName())) {
                        handleError("Group with name " + groupContext.getName() + " not deployed");
                    }

                    // check validity of group alias

                    if (StringUtils.isEmpty(groupContext.getAlias()) || !ApplicationUtils.isAliasValid(groupContext.getAlias())) {
                        handleError("Invalid Group alias specified: [ " + groupContext.getAlias() + " ]");
                    }

                    // check if a group is already defined under the same alias
                    if(definedGroups.get(groupContext.getAlias()) != null) {
                        // a group with same alias already exists, can't continue
                        handleError("A Group with alias " + groupContext.getAlias() + " already exists");
                    }

                    definedGroups.put(groupContext.getAlias(), groupContext);
                    if (log.isDebugEnabled()) {
                        log.debug("Added Group Definition [ " + groupContext.getName() +" , " + groupContext.getAlias() + " ] to map [group alias -> Group Definition]");
                    }
                }
            }
        }

        return definedGroups;
    }

    /**
     * Extract Subscription Information from the Application Definition
     *
     * @param appCtxt ApplicationContext object with Application information
     * @return Map [cartridge alias -> Group]
     *
     * @throws ApplicationDefinitionException if the Subscription information is invalid
     */
    private Map<String, SubscribableInfoContext> getSubscribableInformation (ApplicationContext appCtxt) throws
            ApplicationDefinitionException {

        // map [cartridge alias -> Subscribable Information]
        Map<String, SubscribableInfoContext> subscribableInformation = null;

        if (appCtxt.getSubscribableInfoContext() != null) {
            subscribableInformation = new HashMap<String, SubscribableInfoContext>();

            for (SubscribableInfoContext subscribableInfoCtxt : appCtxt.getSubscribableInfoContext()) {

                if (StringUtils.isEmpty(subscribableInfoCtxt.getAlias()) ||
                        !ApplicationUtils.isAliasValid(subscribableInfoCtxt.getAlias())) {
                    handleError("Invalid alias specified for Subscribable Information Obj: [ " + subscribableInfoCtxt.getAlias() + " ]");
                }

                // check if a group is already defined under the same alias
                if(subscribableInformation.get(subscribableInfoCtxt.getAlias()) != null) {
                    // a group with same alias already exists, can't continue
                    handleError("A Subscribable Info obj with alias " + subscribableInfoCtxt.getAlias() + " already exists");
                }

                subscribableInformation.put(subscribableInfoCtxt.getAlias(), subscribableInfoCtxt);
                if (log.isDebugEnabled()) {
                    log.debug("Added Subcribables Info obj [ " + subscribableInfoCtxt.getAlias() + " ] to map [cartridge alias -> Subscribable Information]");
                }
            }
        }

        return subscribableInformation;
    }

    /**
     * Check if a Group Definition is deployed
     *
     * @param serviceGroupName Group name
     * @return true if the Group is deployed, else false
     *
     * @throws ApplicationDefinitionException
     */
    private boolean isGroupDeployed (String serviceGroupName) throws ApplicationDefinitionException {

        try {
            return RegistryManager.getInstance().getServiceGroup(serviceGroupName) != null;
        } catch (Exception e) {
            String errorMsg = "Error in checking if Service Group Definition [ " + serviceGroupName
                    + " ] already exists";
            log.error(errorMsg, e);
            throw new ApplicationDefinitionException(errorMsg, e);
        }
    }

    /**
     * Builds the Application structure
     *
     * @param appCtxt ApplicationContext object with Application information
     * @param definedGroupCtxts Map [cartridge alias -> Group] with extracted Group Information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @return Application Application object denoting the Application structure
     *
     * @throws ApplicationDefinitionException If an error occurs in building the Application structure
     */
    private Application buildCompositeAppStructure (ApplicationContext appCtxt,
                                                            Map<String, GroupContext> definedGroupCtxts,
                                                            Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Application application = new Application(appCtxt.getApplicationId());

        // set tenant related information
        application.setTenantId(appCtxt.getTenantId());
        application.setTenantDomain(appCtxt.getTenantDomain());
        application.setTenantAdminUserName(appCtxt.getTeantAdminUsername());

        // following keeps track of all Clusters created for this application
        //Set<Cluster> clusters = new HashSet<Cluster>();
        //ClusterDataHolder clusterDataHolder = null;
        Map<String, ClusterDataHolder> clusterDataMap;

        if (appCtxt.getComponents() != null) {
            // get top level Subscribables
            if (appCtxt.getComponents().getSubscribableContexts() != null) {
                clusterDataMap = parseLeafLevelSubscriptions(appCtxt.getApplicationId(), appCtxt.getTenantId(),
                        application.getKey(), null, Arrays.asList(appCtxt.getComponents().getSubscribableContexts()),
                        subscribableInfoCtxts);
                application.setClusterData(clusterDataMap);
                //clusters.addAll(clusterDataHolder.getApplicationClusterContexts());
            }

            // get Groups
            if (appCtxt.getComponents().getGroupContexts() != null) {
                application.setGroups(parseGroups(appCtxt.getApplicationId(), appCtxt.getTenantId(),
                        application.getKey(), Arrays.asList(appCtxt.getComponents().getGroupContexts()),
                        subscribableInfoCtxts, definedGroupCtxts));
            }

            // get top level Dependency definitions
            if (appCtxt.getComponents().getDependencyContext() != null) {
                DependencyOrder appDependencyOrder = new DependencyOrder();
                String [] startupOrders = appCtxt.getComponents().getDependencyContext().getStartupOrdersContexts();
                if (startupOrders != null) {
                	if (log.isDebugEnabled()) {
                    	log.debug("parsing application ... buildCompositeAppStructure: startupOrders != null for app alias: " +
                    				appCtxt.getAlias() + " #: " + startupOrders.length);
                    }
                    appDependencyOrder.setStartupOrders(ParserUtils.convert(startupOrders));
                } else {
                	if (log.isDebugEnabled()) {
                    	log.debug("parsing application ... buildCompositeAppStructure: startupOrders == null for app alias: " + appCtxt.getAlias());
                    }
                }
                String terminationBehavior = appCtxt.getComponents().getDependencyContext().getTerminationBehaviour();
                validateTerminationBehavior(terminationBehavior);
                appDependencyOrder.setTerminationBehaviour(terminationBehavior);

                application.setDependencyOrder(appDependencyOrder);
            }
        }

        String alias;
        Properties properties = new Properties();
        for (SubscribableInfoContext value : subscribableInfoCtxts.values()) {
            alias = value.getAlias();
            String username = value.getRepoUsername();
            String password = value.getRepoPassword();
            String repoUrl = value.getRepoUrl();
            List<Property> propertyList = new ArrayList<Property>();
            if (StringUtils.isNotEmpty(username)) {
                Property property = new Property();
                property.setName("REPO_USERNAME");
                property.setValue(username);
                propertyList.add(property);
            }

            if (StringUtils.isNotEmpty(password)) {
                String encryptedPassword = encryptPassword(password, application.getKey());
                Property property = new Property();
                property.setName("REPO_PASSWORD");
                property.setValue(encryptedPassword);
                //properties.addProperties(property);
                propertyList.add(property);

            }

            if (StringUtils.isNotEmpty(repoUrl)) {
                Property property = new Property();
                property.setName("REPO_URL");
                property.setValue(repoUrl);
                //properties.addProperties(property);
                propertyList.add(property);
            }
            if(propertyList.size() > 0 ) {
                Property[] properties1 = new Property[propertyList.size()];
                properties.setProperties(propertyList.toArray(properties1));
                this.addProperties(alias, properties);
            }

        }


        log.info("Application with id " + appCtxt.getApplicationId() + " parsed successfully");

        return application;
    }

    /**
     * Validates terminationBehavior. The terminationBehavior should be one of the following:
     *      1. terminate-none
     *      2. terminate-dependents
     *      3. terminate-all
     *
     * @throws ApplicationDefinitionException if terminationBehavior is different to what is
     * listed above
     */
    private static void validateTerminationBehavior (String terminationBehavior) throws ApplicationDefinitionException {

        if (!(terminationBehavior == null || "terminate-none".equals(terminationBehavior) ||
                "terminate-dependents".equals(terminationBehavior) || "terminate-all".equals(terminationBehavior))) {
            throw new ApplicationDefinitionException("Invalid Termination Behaviour specified: [ " +
                    terminationBehavior + " ], should be one of 'terminate-none', 'terminate-dependents', " +
                    " 'terminate-all' ");
        }
    }

    /**
     * Parse Group information
     *
     * @param appId Application id
     * @param tenantId tenant id of tenant which deployed the Application
     * @param key Generated key for the Application
     * @param groupCtxts  Group information
     * @param subscribableInformation Subscribable Information
     * @param definedGroupCtxts Map [group alias -> Group] with extracted Group Information
     * @return Map [alias -> Group]
     *
     * @throws ApplicationDefinitionException if an error occurs in parsing Group Information
     */
    private Map<String, Group> parseGroups (String appId, int tenantId, String key, List<GroupContext> groupCtxts,
                                           Map<String, SubscribableInfoContext> subscribableInformation,
                                           Map<String, GroupContext> definedGroupCtxts)
            throws ApplicationDefinitionException {

        Map<String, Group> groupAliasToGroup = new HashMap<String, Group>();

        for (GroupContext groupCtxt : groupCtxts) {
            Group group = parseGroup(appId, tenantId, key, groupCtxt, subscribableInformation, definedGroupCtxts);
            groupAliasToGroup.put(group.getAlias(), group);
        }

        //Set<GroupContext> topLevelGroupContexts = getTopLevelGroupContexts(groupAliasToGroup);
        Set<Group> nestedGroups = new HashSet<Group>();
        getNestedGroupContexts(nestedGroups, groupAliasToGroup.values());
        filterDuplicatedGroupContexts(groupAliasToGroup.values(), nestedGroups);

        return groupAliasToGroup;
    }

    /**
     * Extracts nested Group information recursively
     *
     * @param nestedGroups Nested Groups set to be populated recursively
     * @param groups Collection of Groups
     */
    private void getNestedGroupContexts (Set<Group> nestedGroups, Collection<Group> groups) {

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
     * @param nestedGroups nested Groups
     */
    private void filterDuplicatedGroupContexts (Collection<Group> topLevelGroups, Set<Group> nestedGroups) {

        for (Group nestedGroup : nestedGroups) {
            filterNestedGroupFromTopLevel(topLevelGroups, nestedGroup);
        }
    }

    private void filterNestedGroupFromTopLevel (Collection<Group> topLevelGroups, Group nestedGroup) {

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
     * @param appId Application id
     * @param tenantId tenant id of tenant which deployed the Application
     * @param key Generated key for the Application
     * @param groupCtxt Group definition information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @param definedGroupCtxts Map [group alias -> Group] with extracted Group Information
     * @return  Group object
     *
     * @throws ApplicationDefinitionException if unable to parse
     */
    private Group parseGroup (String appId, int tenantId, String key, GroupContext groupCtxt,
                             Map<String, SubscribableInfoContext> subscribableInfoCtxts,
                             Map<String, GroupContext> definedGroupCtxts)
            throws ApplicationDefinitionException {

        // check if are in the defined Group set
        GroupContext definedGroupDef = definedGroupCtxts.get(groupCtxt.getAlias());
        if (definedGroupDef == null) {
            handleError("Group Definition with name: " + groupCtxt.getName() + ", alias: " +
                    groupCtxt.getAlias() + " is not found in the all Group Definitions collection");
        }

        Group group = new Group(appId, groupCtxt.getName(), groupCtxt.getAlias());

        group.setAutoscalingPolicy(groupCtxt.getAutoscalingPolicy());
        group.setDeploymentPolicy(groupCtxt.getDeploymentPolicy());
        DependencyOrder dependencyOrder = new DependencyOrder();
        // create the Dependency Ordering
        String []  startupOrders = getStartupOrderForGroup(groupCtxt);
        if (startupOrders != null) {
            dependencyOrder.setStartupOrders(ParserUtils.convert(startupOrders, groupCtxt));
        }
        dependencyOrder.setTerminationBehaviour(getKillbehaviour(groupCtxt.getName()));
        group.setDependencyOrder(dependencyOrder);

        Map<String, ClusterDataHolder> clusterDataMap;

        // get group level Subscribables
        if (groupCtxt.getSubscribableContexts() != null) {
            clusterDataMap = parseLeafLevelSubscriptions(appId, tenantId, key, groupCtxt.getName(),
                    Arrays.asList(groupCtxt.getSubscribableContexts()), subscribableInfoCtxts);
            group.setClusterData(clusterDataMap);
        }

        // get nested groups
        if (groupCtxt.getGroupContexts() != null) {
            Map<String, Group> nestedGroups = new HashMap<String, Group>();
            // check sub groups
            for (GroupContext subGroupCtxt : groupCtxt.getGroupContexts()) {
                // get the complete Group Definition
                subGroupCtxt = definedGroupCtxts.get(subGroupCtxt.getAlias());
                Group nestedGroup = parseGroup(appId, tenantId, key, subGroupCtxt,
                        subscribableInfoCtxts,
                        definedGroupCtxts);
                nestedGroups.put(nestedGroup.getAlias(), nestedGroup);
            }

            group.setGroups(nestedGroups);
        }

        return group;
    }

    /**
     * Find the startup order
     *
     * @param groupContext GroupContext with Group defintion information
     * @return Set of Startup Orders which are defined in the Group
     *
     * @throws ApplicationDefinitionException
     */
    private String [] getStartupOrderForGroup(GroupContext groupContext) throws ApplicationDefinitionException {

        ServiceGroup serviceGroup;
        try {
            serviceGroup = RegistryManager.getInstance().getServiceGroup(groupContext.getName());
        } catch (Exception e) {
            String errorMsg = "Error in getting Service Group Definition for [ " + groupContext.getName() +
                    " ] from Registry";
            log.error(errorMsg, e);
            throw new ApplicationDefinitionException(errorMsg, e);
        }

        if (serviceGroup == null) {
            handleError("Service Group Definition not found for name " + groupContext.getName());
        }
        
        if (log.isDebugEnabled()) {
        	log.debug("parsing application ... getStartupOrderForGroup: " + groupContext.getName());
        }

        assert serviceGroup != null;
        if (serviceGroup.getDependencies() != null) {
        	if (log.isDebugEnabled()) {
            	log.debug("parsing application ... getStartupOrderForGroup: dependencies != null " );
            }
            if (serviceGroup.getDependencies().getStartupOrders() != null) {
            	
            	String [] startupOrders = serviceGroup.getDependencies().getStartupOrders();
            	if (log.isDebugEnabled()) {
                	log.debug("parsing application ... getStartupOrderForGroup: startupOrders != null # of: " +  startupOrders.length);
                }
                return startupOrders;
            }
        }

        return null;
    }

    /**
     * Get kill behaviour related to a Group
     *
     * @param serviceGroupName Group name
     * @return String indicating the kill behavior
     *
     * @throws ApplicationDefinitionException if an error occurs
     */
    private String getKillbehaviour (String serviceGroupName) throws ApplicationDefinitionException {

        ServiceGroup serviceGroup;
        try {
            serviceGroup = RegistryManager.getInstance().getServiceGroup(serviceGroupName);
        } catch (Exception e) {
            String errorMsg = "Error in getting Service Group Definition for [ " + serviceGroupName +
                    " ] from Registry";
            log.error(errorMsg, e);
            throw new ApplicationDefinitionException(errorMsg, e);
        }

        if (serviceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        assert serviceGroup != null;
        if (serviceGroup.getDependencies() != null) {
            return serviceGroup.getDependencies().getTerminationBehaviour();
        }

        return null;

    }

    /**
     * Parse Subscription Information
     *
     * @param appId Application id
     * @param tenantId Tenant id of tenant which deployed the Application
     * @param key Generated key for the Application
     * @param groupName Group name (if relevant)
     * @param subscribableCtxts Subscribable Information
     * @param subscribableInfoCtxts Map [cartridge alias -> Group] with extracted Subscription Information
     * @return Map [subscription alias -> ClusterDataHolder]
     *
     * @throws ApplicationDefinitionException
     */
    private Map<String, ClusterDataHolder> parseLeafLevelSubscriptions (String appId, int tenantId, String key, String groupName,
                                                                 List<SubscribableContext> subscribableCtxts,
                                                                 Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Map<String, ClusterDataHolder> clusterDataMap = new HashMap<String, ClusterDataHolder>();

        for (SubscribableContext subscribableCtxt : subscribableCtxts) {

            // check is there is a related Subscribable Information
            SubscribableInfoContext subscribableInfoCtxt = subscribableInfoCtxts.get(subscribableCtxt.getAlias());
            if (subscribableInfoCtxt == null) {
                handleError("Related Subscribable Information Ctxt not found for Subscribable with alias: "
                        + subscribableCtxt.getAlias());
            }

            // check if Cartridge Type is valid
            if (StringUtils.isEmpty(subscribableCtxt.getType())) {
                handleError("Invalid Cartridge Type specified : [ "
                        + subscribableCtxt.getType() + " ]");
            }

            // check if a cartridgeInfo with relevant type is already deployed. else, can't continue
            CartridgeInfo cartridgeInfo =  getCartridge(subscribableCtxt.getType());
            if (cartridgeInfo == null) {
                handleError("No deployed Cartridge found with type [ " + subscribableCtxt.getType() +
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

            String hostname = clusterInfo.getHostName(subscribableCtxt.getAlias(), cartridgeInfo.getHostName());
            String clusterId = clusterInfo.getClusterId(subscribableCtxt.getAlias(), subscribableCtxt.getType());

            // create and collect this cluster's information
            assert subscribableInfoCtxt != null;
            ApplicationClusterContext appClusterCtxt = createApplicationClusterContext(appId, groupName, cartridgeInfo,
                    key, tenantId, subscribableInfoCtxt.getRepoUrl(), subscribableCtxt.getAlias(),
                    clusterId, hostname, subscribableInfoCtxt.getDeploymentPolicy(), false, subscribableInfoCtxt.getDependencyAliases(), subscribableInfoCtxt.getProperties());

            appClusterCtxt.setAutoscalePolicyName(subscribableInfoCtxt.getAutoscalingPolicy());
           	appClusterCtxt.setProperties(subscribableInfoCtxt.getProperties());
            this.applicationClusterContexts.add(appClusterCtxt);

            // add relevant information to the map
            clusterDataMap.put(subscribableCtxt.getAlias(), new ClusterDataHolder(subscribableCtxt.getType(), clusterId));
        }

        return clusterDataMap;
    }

    /**
     * Creates a ApplicationClusterContext object to keep information related to a Cluster in this Application
     *
     * @param appId Application id
     * @param groupName Group name
     * @param cartridgeInfo Cartridge information
     * @param subscriptionKey Generated key for the Application
     * @param tenantId Tenant Id of the tenant which deployed the Application
     * @param repoUrl Repository URL
     * @param alias alias specified for this Subscribable in the Application Definition
     * @param clusterId Cluster id
     * @param hostname Hostname
     * @param deploymentPolicy Deployment policy used
     * @param isLB if this cluster is an LB
     * @return ApplicationClusterContext object with relevant information
     *
     * @throws ApplicationDefinitionException If any error occurs
     */
    private ApplicationClusterContext createApplicationClusterContext (String appId, String groupName, CartridgeInfo cartridgeInfo,
                                                                       String subscriptionKey, int tenantId, String repoUrl,
                                                                       String alias, String clusterId, String hostname,
                                                                       String deploymentPolicy, boolean isLB, String[] dependencyAliases, Properties properties)
            throws ApplicationDefinitionException {

        // Create text payload
        String textPayload = ApplicationUtils.createPayload(appId, groupName, cartridgeInfo, subscriptionKey, tenantId, clusterId,
                hostname, repoUrl, alias, null, dependencyAliases, properties).toString();

        return new ApplicationClusterContext(cartridgeInfo.getType(), clusterId, hostname, textPayload, deploymentPolicy, isLB);
    }

    private CartridgeInfo getCartridge (String cartridgeType) throws ApplicationDefinitionException {

        try {
            return CloudControllerClient.getInstance().getCartrdgeInformation(cartridgeType);
        } catch (CartridgeInformationException e) {
            throw new ApplicationDefinitionException(e);
        }
    }

    private void handleError (String errorMsg) throws ApplicationDefinitionException {
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

    public static String encryptPassword(String repoUserPassword, String secKey) {
        String encryptPassword = "";
        String secret = secKey; // secret key length must be 16
        SecretKey key;
        Cipher cipher;
        Base64 coder;
        key = new SecretKeySpec(secret.getBytes(), "AES");
        try {
            cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "SunJCE");
            coder = new Base64();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] cipherText = cipher.doFinal(repoUserPassword.getBytes());
            encryptPassword = new String(coder.encode(cipherText));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptPassword;
    }
}
