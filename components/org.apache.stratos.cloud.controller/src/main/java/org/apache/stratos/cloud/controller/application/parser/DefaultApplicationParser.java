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

    @Override
    public Set<ApplicationClusterContext> getApplicationClusterContexts() throws ApplicationDefinitionException {
        return applicationClusterContexts;
    }

    @Override
    public Set<MetaDataHolder> getPayloadData() throws ApplicationDefinitionException {
        return metaDataHolders;
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
        Map<String, Set<String>> serviceNameToClusterIds;

        if (appCtxt.getComponents() != null) {
            // get top level Subscribables
            if (appCtxt.getComponents().getSubscribableContexts() != null) {
                serviceNameToClusterIds = parseLeafLevelSubscriptions(appCtxt.getApplicationId(), appCtxt.getTenantId(),
                        application.getKey(), null, Arrays.asList(appCtxt.getComponents().getSubscribableContexts()),
                        subscribableInfoCtxts);
                application.setClusterIds(serviceNameToClusterIds);
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

        //ApplicationDataHolder applicationDataHolder = new ApplicationDataHolder();
        //assert clusterDataHolder != null;
        //applicationDataHolder.setClusters(clusterDataHolder.getApplicationClusterContexts());
        //applicationDataHolder.setPayloadDataHolders(clusterDataHolder.getPayloadDataHolders());
        //applicationDataHolder.setApplication(application);

        // persist the information in FasterLookUpDataHolder
        //persist(dataHolder);

        //return applicationDataHolder;
    }

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
        Set<StartupOrder>  startupOrders = getStartupOrderForGroup(groupCtxt.getName());
        if (startupOrders != null) {
            dependencyOrder.setStartupOrders(startupOrders);
        }
        dependencyOrder.setKillbehavior(getKillbehaviour(groupCtxt.getName()));
        group.setDependencyOrder(dependencyOrder);

        //ClusterDataHolder clusterDataHolderOfGroup;
        Map<String, Set<String>> serviceNameToClusterIds;

        // get group level Subscribables
        if (groupCtxt.getSubscribableContexts() != null) {
            serviceNameToClusterIds = parseLeafLevelSubscriptions(appId, tenantId, key, groupCtxt.getName(),
                    Arrays.asList(groupCtxt.getSubscribableContexts()), subscribableInfoCtxts);
            group.setClusterIds(serviceNameToClusterIds);
            //clusters.addAll(clusterDataHolderOfGroup.getApplicationClusterContexts());
//            if (clusterDataHolder == null) {
//                clusterDataHolder = clusterDataHolderOfGroup;
//            } else {
//                clusterDataHolder.getApplicationClusterContexts().addAll(clusterDataHolderOfGroup.getApplicationClusterContexts());
//                clusterDataHolder.getClusterIdMap().putAll(clusterDataHolderOfGroup.getClusterIdMap());
//                //clusterDataHolder.getPayloadDataHolders().addAll(clusterDataHolderOfGroup.getPayloadDataHolders());
//            }
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

    private Map<String, Set<String>> parseLeafLevelSubscriptions (String appId, int tenantId, String key, String groupName,
                                                                 List<SubscribableContext> subscribableCtxts,
                                                                 Map<String, SubscribableInfoContext> subscribableInfoCtxts)
            throws ApplicationDefinitionException {

        Map<String, Set<String>> clusterIdMap = new HashMap<String, Set<String>>();
//        Set<Cluster> clusters = new HashSet<Cluster>();
        //Set<PayloadDataHolder> payloadDataHolders = new HashSet<PayloadDataHolder>();

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
            ApplicationClusterContext appClusterCtxt = createApplicationClusterContext(subscribableCtxt.getType(),
                    appId, groupName, clusterId, hostname, subscribableInfoCtxt.getDeploymentPolicy(), false);
            this.applicationClusterContexts.add(appClusterCtxt);

            // create cluster level meta data
            this.metaDataHolders.add(ApplicationUtils.getClusterLevelPayloadData(appId, groupName, tenantId, key,
                    hostname, appClusterCtxt.getTenantRange(), clusterId, subscribableCtxt, subscribableInfoCtxt, cartridge));
            ////////////
            //Cluster cluster = getCluster(subscribableCtxt, subscribableInfoCtxt, cartridge);
            addClusterId(clusterIdMap, subscribableCtxt.getType(), clusterId);
            //clusterIdMap.put(subscribableCtxt.getType(), cluster.getClusterId());
            //clusters.add(cluster);
//            if (clusterIdMap.put(subscribableCtxt.getType(), cluster.getClusterId()) != null) {
//                // Application Definition has same cartridge multiple times at the top-level
//                handleError("Cartridge [ " + subscribableCtxt.getType() + " ] appears twice in the Application Definition's top level");
//            }
            ///////////////

//            createClusterContext(appId, groupName, subscribableCtxt.getType(), cluster.getClusterId(),
//                    cluster.getHostNames().get(0));
        }

        return clusterIdMap;
        //return new ClusterDataHolder(clusterIdMap, clusters);
        //clusterDataHolder.setPayloadDataHolders(payloadDataHolders);
    }

    private ApplicationClusterContext createApplicationClusterContext (String serviceType, String appId,
                                                                       String groupName, String clusterId,
                                                                       String hostname, String deploymentPolicy,
                                                                       boolean isLB) {

        // Create text payload
        String textPayload = ApplicationUtils.getTextPayload(appId, groupName, clusterId).toString();

        return new ApplicationClusterContext(serviceType, clusterId, hostname, textPayload, deploymentPolicy, isLB);
    }

    public void addClusterId (Map<String, Set<String>> serviceNameToClusterIdsMap, String serviceName, String clusterId) {

        if (serviceNameToClusterIdsMap.get(serviceName) == null) {
            // not found, create
            Set<String> clusterIds = new HashSet<String>();
            clusterIds.add(clusterId);
            serviceNameToClusterIdsMap.put(serviceName, clusterIds);
        } else {
            // the cluster id set already exists, update
            serviceNameToClusterIdsMap.get(serviceName).add(clusterId);
        }
    }

//    private void createClusterContext (String appId, String groupName, String serviceType, String clusterId,
//                                              String hostName) throws ApplicationDefinitionException {
//
//        Cartridge cartridge;
//        if ((cartridge = dataHolder.getCartridge(serviceType)) == null) {
//
//            String msg = "Unregistered Cartridge type: " + serviceType;
//            log.error(msg);
//            throw new ApplicationDefinitionException(msg);
//        }
//
//        //Properties props = CloudControllerUtil.toJavaUtilProperties(registrant.getProperties());
//        //String property = props.getProperty(Constants.IS_LOAD_BALANCER);
//        //boolean isLb = property != null ? Boolean.parseBoolean(property) : false;
//        String payload = ApplicationUtils.getTextPayload(appId, groupName, clusterId).toString();
//
//        ClusterContext ctxt = buildClusterContext(cartridge, clusterId,
//                payload, hostName, null, false, null);
//
//        dataHolder.addClusterContext(ctxt);
//    }
//
//    private void persist(FasterLookUpDataHolder dataHolder) {
//        try {
//            RegistryManager.getInstance().persist(
//                    dataHolder);
//        } catch (RegistryException e) {
//
//            String msg = "Failed to persist the Cloud Controller data in registry. Further, transaction roll back also failed.";
//            log.fatal(msg);
//            throw new CloudControllerException(msg, e);
//        }
//    }
//
//    private ClusterContext buildClusterContext(Cartridge cartridge,
//                                               String clusterId, String payload, String hostName,
//                                               Properties props, boolean isLb, Persistence persistence) {
//
//
//        // initialize ClusterContext
//        ClusterContext ctxt = new ClusterContext(clusterId, cartridge.getType(), payload,
//                hostName, isLb);
//
//        String property = null;
//        if (props != null) {
//            property = props.getProperty(Constants.GRACEFUL_SHUTDOWN_TIMEOUT);
//        }
//
//        long timeout = property != null ? Long.parseLong(property) : 30000;
//
//        boolean persistanceRequired = false;
//        if(persistence != null){
//            persistanceRequired = persistence.isPersistanceRequired();
//        }
//
//        if(persistanceRequired){
//            ctxt.setVolumes(persistence.getVolumes());
//            ctxt.setVolumeRequired(true);
//        }else{
//            ctxt.setVolumeRequired(false);
//        }
//        ctxt.setTimeoutInMillis(timeout);
//        return ctxt;
//    }
//
//    private Cluster getCluster (SubscribableContext subscribableCtxt, SubscribableInfoContext subscribableInfoCtxt, Cartridge cartridge)
//
//            throws ApplicationDefinitionException {
//
//        // get hostname and cluster id
//        ClusterInformation clusterInfo;
//        if (cartridge.isMultiTenant()) {
//            clusterInfo = new MTClusterInformation();
//        } else {
//            clusterInfo = new STClusterInformation();
//        }
//
//        String hostname = clusterInfo.getHostName(subscribableCtxt.getAlias(), cartridge.getHostName());
//        String clusterId = clusterInfo.getClusterId(subscribableCtxt.getAlias(), subscribableCtxt.getType());
//
//        Cluster cluster = new Cluster(subscribableCtxt.getType(), clusterId, subscribableInfoCtxt.getDeploymentPolicy(),
//                subscribableInfoCtxt.getAutoscalingPolicy());
//                                                                                                          Clo
//        cluster.addHostName(hostname);
//        cluster.setLbCluster(false);
//        cluster.setStatus(Status.Created);
//
//        return cluster;
//    }

//    private GroupDataHolder getGroupInformation (List<GroupContext> groupCtxts,
//                                                 Map<String, SubscribableInfoContext> subscribableInformation,
//                                                 Map<String, GroupContext> definedGroupCtxts)
//            throws ApplicationDefinitionException {
//
//        Set<GroupContext> groupContexts = new HashSet<GroupContext>();
//
//        for (GroupContext groupCtxt : groupCtxts) {
//            groupContexts.add(parseGroup(groupCtxt, subscribableInformation, definedGroupCtxts));
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
