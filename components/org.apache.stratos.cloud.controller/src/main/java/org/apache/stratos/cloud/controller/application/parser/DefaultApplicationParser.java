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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.application.ApplicationUtils;
import org.apache.stratos.cloud.controller.application.ClusterInformation;
import org.apache.stratos.cloud.controller.application.MTClusterInformation;
import org.apache.stratos.cloud.controller.application.STClusterInformation;
import org.apache.stratos.cloud.controller.exception.ApplicationDefinitionException;
import org.apache.stratos.cloud.controller.interfaces.ApplicationParser;
import org.apache.stratos.cloud.controller.pojo.ApplicationDataHolder;
import org.apache.stratos.cloud.controller.pojo.Cartridge;
import org.apache.stratos.cloud.controller.pojo.ClusterDataHolder;
import org.apache.stratos.cloud.controller.pojo.ServiceGroup;
import org.apache.stratos.cloud.controller.pojo.application.*;
import org.apache.stratos.cloud.controller.runtime.FasterLookUpDataHolder;
import org.apache.stratos.messaging.domain.topology.*;

import java.util.*;

public class DefaultApplicationParser implements ApplicationParser {

    private static Log log = LogFactory.getLog(DefaultApplicationParser.class);

    @Override
    public ApplicationDataHolder parse(Object obj) throws ApplicationDefinitionException {

        ApplicationContext applicationCtxt = null;

        if (obj instanceof ApplicationContext) {
            applicationCtxt = (ApplicationContext) obj;
        }

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
            Set<Map.Entry<String, GroupContext>> groupEntries = definedGroups.entrySet();
            log.debug("Defined Groups: [ ");
            for (Map.Entry<String, GroupContext> groupEntry : groupEntries) {
                log.debug("Group alias: " + groupEntry.getKey());
            }
            log.debug(" ]");
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

    private Map<String, GroupContext> getDefinedGroups (ApplicationContext appCtxt) throws
            ApplicationDefinitionException {

        // map [group alias -> Group Definition]
        Map<String, GroupContext> definedGroups = null;

        if (appCtxt.getComponents() != null) {
            if (appCtxt.getComponents().getGroupContexts() != null) {
                definedGroups = new HashMap<String, GroupContext>();

                for (GroupContext groupContext : appCtxt.getComponents().getGroupContexts()) {

                    // check validity of group name
                    if (groupContext.getName() == null || groupContext.getName().isEmpty()) {
                        handleError("Invalid Group name specified");
                    }

                    // check if group is deployed
                    if(!isGroupDeployed(groupContext.getName())) {
                        handleError("Group with name " + groupContext.getName() + " not deployed");
                    }

                    // check validity of group alias
                    if (groupContext.getAlias() == null || groupContext.getAlias().isEmpty() || !ApplicationUtils.isAliasValid(groupContext.getAlias())) {
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

    private Map<String, SubscribableInfoContext> getSubscribableInformation (ApplicationContext appCtxt) throws
            ApplicationDefinitionException {

        // map [cartridge alias -> Subscribable Information]
        Map<String, SubscribableInfoContext> subscribableInformation = null;

        if (appCtxt.getSubscribableInfoContext() != null) {
            subscribableInformation = new HashMap<String, SubscribableInfoContext>();

            for (SubscribableInfoContext subscribableInfoCtxt : appCtxt.getSubscribableInfoContext()) {

                if (subscribableInfoCtxt.getAlias() == null || subscribableInfoCtxt.getAlias().isEmpty() ||
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

    private boolean isGroupDeployed (String serviceGroupName) throws ApplicationDefinitionException {

        return FasterLookUpDataHolder.getInstance().getServiceGroup(serviceGroupName) != null;
    }

    private ApplicationDataHolder buildCompositeAppStructure (ApplicationContext appCtxt,
                                                            Map<String, GroupContext> definedGroupCtxts,
                                                            Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Application application = new Application(appCtxt.getApplicationId());

        // set tenant related information
        application.setTenantId(appCtxt.getTenantId());
        application.setTenantDomain(appCtxt.getTenantDomain());
        application.setTenantAdminUserName(appCtxt.getTeantAdminUsername());

        // following keeps track of all Clusters created for this application
        Set<Cluster> clusters = new HashSet<Cluster>();

        if (appCtxt.getComponents() != null) {
            // get top level Subscribables
            if (appCtxt.getComponents().getSubscribableContexts() != null) {
                ClusterDataHolder clusterDataHolder = getClusterInformation(Arrays.asList(appCtxt.getComponents().getSubscribableContexts()), subscribableInfoCtxts);
                application.setClusterIds(clusterDataHolder.getClusterIdMap());
                clusters.addAll(clusterDataHolder.getClusters());
            }

            // get Groups
            if (appCtxt.getComponents().getGroupContexts() != null) {
                application.setGroups(getGroupInfo(clusters, Arrays.asList(appCtxt.getComponents().getGroupContexts()), subscribableInfoCtxts, definedGroupCtxts));
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

        ApplicationDataHolder applicationDataHolder = new ApplicationDataHolder();
        applicationDataHolder.setClusters(clusters);
        applicationDataHolder.setApplication(application);

        return applicationDataHolder;
    }

    private Map<String, Group> getGroupInfo (Set<Cluster> clusters, List<GroupContext> groupCtxts,
                                         Map<String, SubscribableInfoContext> subscribableInformation,
                                         Map<String, GroupContext> definedGroupCtxts)
            throws ApplicationDefinitionException {

        Map<String, Group> groupNameToGroup = new HashMap<String, Group>();

        for (GroupContext groupCtxt : groupCtxts) {
            Group group = getGroup(clusters, groupCtxt, subscribableInformation, definedGroupCtxts);
            if(groupNameToGroup.put(group.getName(), group) != null) {
                // Application Definition has same Group multiple times at the top-level
                handleError("Group [ " + group.getName() + " ] appears twice in the Application Definition's top level");
            }
        }

        //Set<GroupContext> topLevelGroupContexts = getTopLevelGroupContexts(groupNameToGroup);
        Set<Group> nestedGroups = new HashSet<Group>();
        getNestedGroupContexts(nestedGroups, groupNameToGroup.values());
        filterDuplicatedGroupContexts(groupNameToGroup.values(), nestedGroups);

        return groupNameToGroup;
    }

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

    private Group getGroup(Set<Cluster> clusters, GroupContext groupCtxt, Map<String, SubscribableInfoContext> subscribableInfoCtxts,
                           Map<String, GroupContext> definedGroupCtxts) throws ApplicationDefinitionException {

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
        Set<StartupOrder>  startupOrders = getStartupOrderForGroup(groupCtxt.getName());
        if (startupOrders != null) {
            dependencyOrder.setStartupOrders(startupOrders);
        }
        dependencyOrder.setKillbehavior(getKillbehaviour(groupCtxt.getName()));
        group.setDependencyOrder(dependencyOrder);

        ClusterDataHolder clusterDataHolder;

        // get group level Subscribables
        if (groupCtxt.getSubscribableContexts() != null) {
            clusterDataHolder = getClusterInformation(Arrays.asList(groupCtxt.getSubscribableContexts()), subscribableInfoCtxts);
            group.setClusterIds(clusterDataHolder.getClusterIdMap());
            clusters.addAll(clusterDataHolder.getClusters());
        }

        // get nested groups
        if (groupCtxt.getGroupContexts() != null) {
            Map<String, Group> nestedGroups = new HashMap<String, Group>();
            // check sub groups
            for (GroupContext subGroupCtxt : groupCtxt.getGroupContexts()) {
                // get the complete Group Definition
                subGroupCtxt = definedGroupCtxts.get(subGroupCtxt.getAlias());
                Group nestedGroup = getGroup(clusters, subGroupCtxt, subscribableInfoCtxts, definedGroupCtxts);
                nestedGroups.put(nestedGroup.getName(), nestedGroup);
            }

            group.setGroups(nestedGroups);
        }

        return group;
    }

    private Set<StartupOrder> getStartupOrderForGroup(String serviceGroupName) throws ApplicationDefinitionException {

        ServiceGroup serviceGroup = FasterLookUpDataHolder.getInstance().getServiceGroup(serviceGroupName);

        if (serviceGroup == null) {
            handleError("Service Group Definition not found for name " + serviceGroupName);
        }

        assert serviceGroup != null;
        if (serviceGroup.getDependencies() != null) {
            if (serviceGroup.getDependencies().getStartupOrder() != null) {
                return ParserUtils.convert(serviceGroup.getDependencies().getStartupOrder());
            }
        }

        return null;
    }

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

//    private Set<SubscribableContext> getSubsribableContexts (List<SubscribableContext> subscribableCtxts,
//                                                             Map<String, SubscribableInfoContext> subscribableInfoCtxts)
//            throws ApplicationDefinitionException {
//
//        Set<SubscribableContext> subscribableContexts = new HashSet<SubscribableContext>();
//
//        for (SubscribableContext subscribableCtxt : subscribableCtxts) {
//            // check is there is a related Subscribable Information
//            SubscribableInfo subscribableInfo = subscribableInfoCtxts.get(subscribableCtxt.getAlias());
//            if (subscribableInfo == null) {
//                throw new CompositeApplicationDefinitionException("Related Subscribable Information not found for Subscribable with alias: "
//                        + subscribableCtxt.getAlias());
//            }
//
//            // check if Cartridge Type is valid
//            if (subscribableCtxt.getType() == null || subscribableCtxt.getType().isEmpty()) {
//                throw new CompositeApplicationDefinitionException ("Invalid Cartridge Type specified : [ "
//                        + subscribableCtxt.getType() + " ]");
//            }
//
//            // check if a cartridge with relevant type is already deployed. else, can't continue
//            if (!isCartrigdeDeployed(subscribableCtxt.getType())) {
//                throw new CompositeApplicationDefinitionException("No deployed Cartridge found with type [ " + subscribableCtxt.getType() +
//                        " ] for Composite Application");
//            }
//
//            subscribableContexts.add(ParserUtils.convert(subscribableCtxt, subscribableInfo));
//        }
//
//        return subscribableContexts;
//    }

    private ClusterDataHolder getClusterInformation (List<SubscribableContext> subscribableCtxts,
                                                               Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Map<String, String> clusterIdMap = new HashMap<String, String>();
        Set<Cluster> clusters = new HashSet<Cluster>();

        for (SubscribableContext subscribableCtxt : subscribableCtxts) {
            // check is there is a related Subscribable Information
            SubscribableInfoContext subscribableInfoCtxt = subscribableInfoCtxts.get(subscribableCtxt.getAlias());
            if (subscribableInfoCtxt == null) {
                handleError("Related Subscribable Information Ctxt not found for Subscribable with alias: "
                        + subscribableCtxt.getAlias());
            }

            // check if Cartridge Type is valid
            if (subscribableCtxt.getType() == null || subscribableCtxt.getType().isEmpty()) {
                handleError("Invalid Cartridge Type specified : [ "
                        + subscribableCtxt.getType() + " ]");
            }

            // check if a cartridge with relevant type is already deployed. else, can't continue
            Cartridge cartridge =  getCartridge(subscribableCtxt.getType());
            if (cartridge == null) {
                handleError("No deployed Cartridge found with type [ " + subscribableCtxt.getType() +
                        " ] for Composite Application");
            }

            Cluster cluster = getCluster(subscribableCtxt, subscribableInfoCtxt, cartridge);
            clusters.add(cluster);
            if (clusterIdMap.put(subscribableCtxt.getType(), cluster.getClusterId()) != null) {
                // Application Definition has same cartridge multiple times at the top-level
                handleError("Cartridge [ " + subscribableCtxt.getType() + " ] appears twice in the Application Definition's top level");
            }
        }

        return new ClusterDataHolder(clusterIdMap, clusters);
    }

    private Cluster getCluster (SubscribableContext subscribableCtxt, SubscribableInfoContext subscribableInfoCtxt, Cartridge cartridge)
            throws ApplicationDefinitionException {

        // get hostname and cluster id
        ClusterInformation clusterInfo;
        if (cartridge.isMultiTenant()) {
            clusterInfo = new MTClusterInformation();
        } else {
            clusterInfo = new STClusterInformation();
        }

        String hostname = clusterInfo.getHostName(subscribableCtxt.getAlias(), cartridge.getHostName());
        String clusterId = clusterInfo.getClusterId(subscribableCtxt.getAlias(), subscribableCtxt.getType());

        Cluster cluster = new Cluster(subscribableCtxt.getType(), clusterId, subscribableInfoCtxt.getDeploymentPolicy(),
                subscribableInfoCtxt.getAutoscalingPolicy());

        cluster.addHostName(hostname);
        cluster.setLbCluster(false);
        cluster.setStatus(Status.Created);

        return cluster;
    }

//    private GroupDataHolder getGroupInformation (List<GroupContext> groupCtxts,
//                                                 Map<String, SubscribableInfoContext> subscribableInformation,
//                                                 Map<String, GroupContext> definedGroupCtxts)
//            throws ApplicationDefinitionException {
//
//        Set<GroupContext> groupContexts = new HashSet<GroupContext>();
//
//        for (GroupContext groupCtxt : groupCtxts) {
//            groupContexts.add(getGroup(groupCtxt, subscribableInformation, definedGroupCtxts));
//        }
//
//        //Set<GroupContext> topLevelGroupContexts = getTopLevelGroupContexts(groupContexts);
//        Set<GroupContext> nestedGroupContexts = new HashSet<GroupContext>();
//        getNestedGroupContexts(nestedGroupContexts, groupContexts);
//        filterDuplicatedGroupContexts(groupContexts, nestedGroupContexts);
//    }

    private Cartridge getCartridge (String cartridgeType)  {

        return FasterLookUpDataHolder.getInstance().getCartridge(cartridgeType);
    }

    private void handleError (String errorMsg) throws ApplicationDefinitionException {
        log.error(errorMsg);
        throw new ApplicationDefinitionException(errorMsg);
    }

}
