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

package org.apache.stratos.cloud.controller.application.parser;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.application.ApplicationUtils;
import org.apache.stratos.cloud.controller.application.ClusterInformation;
import org.apache.stratos.cloud.controller.application.MTClusterInformation;
import org.apache.stratos.cloud.controller.application.STClusterInformation;
import org.apache.stratos.cloud.controller.exception.ApplicationDefinitionException;
import org.apache.stratos.cloud.controller.interfaces.ApplicationParser;
import org.apache.stratos.cloud.controller.pojo.*;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.application.*;
import org.apache.stratos.cloud.controller.pojo.payload.MetaDataHolder;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.messaging.domain.topology.*;
import org.apache.stratos.messaging.domain.topology.StartupOrder;

import java.util.*;

public class DefaultApplicationParser implements ApplicationParser {

    private static Log log = LogFactory.getLog(DefaultApplicationParser.class);

//    private static FasterLookUpDataHolder dataHolder = FasterLookUpDataHolder.getInstance();

    private Set<ApplicationClusterContext> applicationClusterContexts;

    private Set<MetaDataHolder> metaDataHolders;

    public DefaultApplicationParser () {

        this.applicationClusterContexts = new HashSet<ApplicationClusterContext>();
        this.metaDataHolders = new HashSet<MetaDataHolder>();
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

    @Override
    public Set<MetaDataHolder> getPayloadData() throws ApplicationDefinitionException {
        return metaDataHolders;
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

        return FasterLookUpDataHolder.getInstance().getServiceGroup(serviceGroupName) != null;
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
                Set<StartupOrder>  startupOrders = getStartupOrderForApplicationComponents(new HashSet<StartupOrderContext>(Arrays.asList(appCtxt.getComponents().
                        getDependencyContext().getStartupOrderContext())));
                if (startupOrders != null) {
                    appDependencyOrder.setStartupOrders(startupOrders);
                }
                appDependencyOrder.setKillbehavior(appCtxt.getComponents().getDependencyContext().getKillBehaviour());

                application.setDependencyOrder(appDependencyOrder);
            }
        }

        log.info("Application with id " + appCtxt.getApplicationId() + " parsed successfully");

        return application;
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

        Group group = new Group(groupCtxt.getName(), groupCtxt.getAlias());

        group.setAutoscalingPolicy(groupCtxt.getAutoscalingPolicy());
        group.setDeploymentPolicy(groupCtxt.getDeploymentPolicy());
        DependencyOrder dependencyOrder = new DependencyOrder();
        // create the Dependency Ordering
        Set<StartupOrder>  startupOrders = getStartupOrderForGroup(groupCtxt);
        if (startupOrders != null) {
            dependencyOrder.setStartupOrders(startupOrders);
        }
        dependencyOrder.setKillbehavior(getKillbehaviour(groupCtxt.getName()));
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
    private Set<StartupOrder> getStartupOrderForGroup(GroupContext groupContext) throws ApplicationDefinitionException {

        ServiceGroup serviceGroup = FasterLookUpDataHolder.getInstance().getServiceGroup(groupContext.getName());

        if (serviceGroup == null) {
            handleError("Service Group Definition not found for name " + groupContext.getName());
        }

        assert serviceGroup != null;
        if (serviceGroup.getDependencies() != null) {
            if (serviceGroup.getDependencies().getStartupOrder() != null) {

                // convert to Startup Order with aliases
                return ParserUtils.convert(serviceGroup.getDependencies().getStartupOrder(), groupContext);
            }
        }

        return null;
    }

    /**
     * Find the startup order for an Application
     *
     * @param startupOrderCtxts Startup Order information related to the Application
     * @return Set of Startup Orders
     *
     * @throws ApplicationDefinitionException if an error occurs
     */
    private Set<StartupOrder> getStartupOrderForApplicationComponents (Set<StartupOrderContext> startupOrderCtxts)
            throws ApplicationDefinitionException {

        if (startupOrderCtxts == null) {
            return null;
        }

        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();

        for (StartupOrderContext startupOrderContext : startupOrderCtxts) {
            startupOrders.add(new StartupOrder(startupOrderContext.getStart(), startupOrderContext.getAfter()));
        }

        return startupOrders;
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

        ServiceGroup serviceGroup = FasterLookUpDataHolder.getInstance().getServiceGroup(serviceGroupName);

        if (serviceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        assert serviceGroup != null;
        if (serviceGroup.getDependencies() != null) {
            return serviceGroup.getDependencies().getKillBehaviour();
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

            // check if a cartridge with relevant type is already deployed. else, can't continue
            Cartridge cartridge =  getCartridge(subscribableCtxt.getType());
            if (cartridge == null) {
                handleError("No deployed Cartridge found with type [ " + subscribableCtxt.getType() +
                        " ] for Composite Application");
            }

            // get hostname and cluster id
            ClusterInformation clusterInfo;
            assert cartridge != null;
            if (cartridge.isMultiTenant()) {
                clusterInfo = new MTClusterInformation();
            } else {
                clusterInfo = new STClusterInformation();
            }

            String hostname = clusterInfo.getHostName(subscribableCtxt.getAlias(), cartridge.getHostName());
            String clusterId = clusterInfo.getClusterId(subscribableCtxt.getAlias(), subscribableCtxt.getType());

            // create and collect this cluster's information
            assert subscribableInfoCtxt != null;
            ApplicationClusterContext appClusterCtxt = createApplicationClusterContext(appId, groupName, cartridge,
                    key, tenantId, subscribableInfoCtxt.getRepoUrl(), subscribableCtxt.getAlias(),
                    clusterId, hostname, subscribableInfoCtxt.getDeploymentPolicy(), false);
            appClusterCtxt.setAutoscalePolicyName(subscribableInfoCtxt.getAutoscalingPolicy());
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
     * @param cartridge Cartridge information
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
    private ApplicationClusterContext createApplicationClusterContext (String appId, String groupName, Cartridge cartridge,
                                                                       String subscriptionKey, int tenantId, String repoUrl,
                                                                       String alias, String clusterId, String hostname,
                                                                       String deploymentPolicy, boolean isLB)
            throws ApplicationDefinitionException {

        // Create text payload
        String textPayload = ApplicationUtils.createPayload(appId, groupName, cartridge, subscriptionKey, tenantId, clusterId,
                hostname, repoUrl, alias, null).toString();

        return new ApplicationClusterContext(cartridge.getType(), clusterId, hostname, textPayload, deploymentPolicy, isLB);
    }

    private Cartridge getCartridge (String cartridgeType)  {

        return FasterLookUpDataHolder.getInstance().getCartridge(cartridgeType);
    }

    private void handleError (String errorMsg) throws ApplicationDefinitionException {
        log.error(errorMsg);
        throw new ApplicationDefinitionException(errorMsg);
    }

}
