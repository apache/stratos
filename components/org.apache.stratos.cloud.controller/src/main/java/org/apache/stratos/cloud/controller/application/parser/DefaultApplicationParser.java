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

import org.apache.axis2.AxisFault;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.cloud.controller.application.ApplicationUtils;
import org.apache.stratos.cloud.controller.exception.ApplicationDefinitionException;
import org.apache.stratos.cloud.controller.interfaces.ApplicationParser;
import org.apache.stratos.cloud.controller.pojo.application.ApplicationContext;
import org.apache.stratos.cloud.controller.pojo.application.GroupContext;
import org.apache.stratos.cloud.controller.pojo.application.SubscribableContext;
import org.apache.stratos.cloud.controller.pojo.application.SubscribableInfoContext;
import org.apache.stratos.messaging.domain.topology.Application;

import java.rmi.RemoteException;
import java.util.*;

public class DefaultApplicationParser implements ApplicationParser {

    private static Log log = LogFactory.getLog(DefaultApplicationParser.class);

    @Override
    public Application parse(Object obj) throws ApplicationDefinitionException {

        ApplicationContext applicationCtxt = null;

        if (obj instanceof ApplicationContext) {
            applicationCtxt = (ApplicationContext) obj;
        }

        if (applicationCtxt == null) {
            throw new ApplicationDefinitionException("Invalid Composite Application Definition");
        }

        if (applicationCtxt.getAlias() == null || applicationCtxt.getAlias().isEmpty()) {
            throw new ApplicationDefinitionException("Invalid alias specified");
        }

        if (applicationCtxt.getApplicationId() == null || applicationCtxt.getApplicationId().isEmpty()) {
            throw new ApplicationDefinitionException("Invalid Composite App id specified");
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
            throw new ApplicationDefinitionException("Invalid Composite Application Definition, no Subscribable Information specified");
        }

        //TODO
        //return buildCompositeAppStructure (applicationCtxt, definedGroups, subscribablesInfo);
        return null;
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
                        throw new ApplicationDefinitionException("Invalid Group name specified");
                    }

                    // check if group is deployed
                    if(!isGroupDeployed(groupContext.getName())) {
                        throw new ApplicationDefinitionException("Group with name " + groupContext.getName() + " not deployed");
                    }

                    // check validity of group alias
                    if (groupContext.getAlias() == null || groupContext.getAlias().isEmpty() || !ApplicationUtils.isAliasValid(groupContext.getAlias())) {
                        throw new ApplicationDefinitionException("Invalid Group alias specified: [ " + groupContext.getAlias() + " ]");
                    }

                    // check if a group is already defined under the same alias
                    if(definedGroups.get(groupContext.getAlias()) != null) {
                        // a group with same alias already exists, can't continue
                        throw new ApplicationDefinitionException("A Group with alias " + groupContext.getAlias() + " already exists");
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
                    throw new ApplicationDefinitionException("Invalid alias specified for Subscribable Information Obj: [ " + subscribableInfoCtxt.getAlias() + " ]");
                }

                // check if a group is already defined under the same alias
                if(subscribableInformation.get(subscribableInfoCtxt.getAlias()) != null) {
                    // a group with same alias already exists, can't continue
                    throw new ApplicationDefinitionException("A Subscribable Info obj with alias " + subscribableInfoCtxt.getAlias() + " already exists");
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

        //TODO
        return true;
    }

//    private Application buildCompositeAppStructure (ApplicationContext appCtxt,
//                                                            Map<String, GroupContext> definedGroupCtxts,
//                                                            Map<String, SubscribableInfoContext> subscribableInfoCtxts)
//            throws ApplicationDefinitionException {
//
//        Application application = new Application(appCtxt.getApplicationId());
//
//        if (appCtxt.getComponents() != null) {
//            // get top level Subscribables
//            if (appCtxt.getComponents().getSubscribableContexts() != null) {
//                application.setSubscribableContexts(getSubsribableContexts(appCtxt.getComponents().getSubscribables(),
//                        subscribableInfoCtxts));
//            }
//
//            // get Groups
//            if (appCtxt.getComponents().getGroups() != null) {
//                application.setGroupContexts(getGroupContexts(appCtxt.getComponents().getGroups(),
//                        subscribableInfoCtxts, definedGroupCtxts));
//            }
//
//            // get top level Dependency definitions
//            if (appCtxt.getComponents().getDependencies() != null) {
//                application.setStartupOrder(getStartupOrderForApplicationComponents(appCtxt.getComponents().
//                        getDependencies().getStartupOrder()));
//
//                application.setKillBehaviour(appCtxt.getComponents().getDependencies().getKillBehaviour());
//            }
//        }
//
//        return application;
//    }
//
//    private Set<GroupContext> getGroupContexts (List<GroupDefinition> groupDefinitions,
//                                                Map<String, SubscribableInfo> subscribableInformation,
//                                                Map<String, GroupDefinition> definedGroups)
//            throws CompositeApplicationDefinitionException {
//
//        Set<GroupContext> groupContexts = new HashSet<GroupContext>();
//
//        for (GroupDefinition group : groupDefinitions) {
//            groupContexts.add(getGroupContext(group, subscribableInformation, definedGroups));
//        }
//
//        //Set<GroupContext> topLevelGroupContexts = getTopLevelGroupContexts(groupContexts);
//        Set<GroupContext> nestedGroupContexts = new HashSet<GroupContext>();
//        getNestedGroupContexts(nestedGroupContexts, groupContexts);
//        filterDuplicatedGroupContexts(groupContexts, nestedGroupContexts);
//
//        return groupContexts;
//    }
//
//    private void getNestedGroupContexts (Set<GroupContext> nestedGroupContexts, Set<GroupContext> groupContexts) {
//
//        if (groupContexts != null) {
//            for (GroupContext groupContext : groupContexts) {
//                if (groupContext.getGroupContexts() != null) {
//                    nestedGroupContexts.addAll(groupContext.getGroupContexts());
//                    getNestedGroupContexts(nestedGroupContexts, groupContext.getGroupContexts());
//                }
//            }
//        }
//    }
//
//    private void filterDuplicatedGroupContexts (Set<GroupContext> topLevelGroupContexts, Set<GroupContext> nestedGroupContexts) {
//
//        for (GroupContext nestedGropCtxt : nestedGroupContexts) {
//            filterNestedGroupFromTopLevel(topLevelGroupContexts, nestedGropCtxt);
//        }
//    }
//
//    private void filterNestedGroupFromTopLevel (Set<GroupContext> topLevelGroupContexts, GroupContext nestedGroupCtxt) {
//
//        Iterator<GroupContext> parentIterator = topLevelGroupContexts.iterator();
//        while (parentIterator.hasNext()) {
//            GroupContext parentGroupCtxt = parentIterator.next();
//            // if there is an exactly similar nested Group Context and a top level Group Context
//            // it implies that they are duplicates. Should be removed from top level.
//            if (parentGroupCtxt.equals(nestedGroupCtxt)) {
//                parentIterator.remove();
//            }
//        }
//    }
//
//    private GroupContext getGroupContext (GroupDefinition group, Map<String, SubscribableInfo> subscribableInformation,
//                                          Map<String, GroupDefinition> definedGroups) throws CompositeApplicationDefinitionException {
//
//        // check if are in the defined Group set
//        GroupDefinition definedGroupDef = definedGroups.get(group.getAlias());
//        if (definedGroupDef == null) {
//            throw new CompositeApplicationDefinitionException("Group Definition with name: " + group.getName() + ", alias: " +
//                    group.getAlias() + " is not found in the all Group Definitions collection");
//        }
//
//        GroupContext groupContext = new GroupContext();
//
//        groupContext.setName(group.getName());
//        groupContext.setAlias(group.getAlias());
//        groupContext.setAutoscalingPolicy(group.getAutoscalingPolicy());
//        groupContext.setDeploymentPolicy(group.getDeploymentPolicy());
//        groupContext.setStartupOrder(getStartupOrderForGroup(group.getName()));
//        groupContext.setKillBehaviour(getKillbehaviour(group.getName()));
//
//        // get group level Subscribables
//        if (group.getSubscribables() != null) {
//            groupContext.setSubscribableContexts(getSubsribableContexts(group.getSubscribables(), subscribableInformation));
//        }
//        // get nested groups
//        if (group.getSubGroups() != null) {
//            Set<GroupContext> nestedGroupContexts = new HashSet<GroupContext>();
//            // check sub groups
//            for (GroupDefinition subGroup : group.getSubGroups()) {
//                // get the complete Group Definition
//                subGroup = definedGroups.get(subGroup.getAlias());
//                nestedGroupContexts.add(getGroupContext(subGroup, subscribableInformation, definedGroups));
//            }
//
//            groupContext.setGroupContexts(nestedGroupContexts);
//        }
//
//        return groupContext;
//    }
//
//    private Set<StartupOrder> getStartupOrderForGroup(String serviceGroupName) throws CompositeApplicationDefinitionException {
//
//        ServiceGroupDefinition groupDefinition;
//
//        try {
//            groupDefinition = dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName);
//
//        } catch (PersistenceManagerException e) {
//            throw new CompositeApplicationDefinitionException(e);
//        }
//
//        if (groupDefinition == null) {
//            throw new CompositeApplicationDefinitionException("Service Group Definition not found for name " + serviceGroupName);
//        }
//
//        if (groupDefinition.getDependencies() != null) {
//            if (groupDefinition.getDependencies().getStartupOrder() != null) {
//                return ParserUtils.convert(groupDefinition.getDependencies().getStartupOrder());
//            }
//        }
//
//        return null;
//    }
//
//    private Set<StartupOrder> getStartupOrderForApplicationComponents (List<StartupOrderDefinition> startupOrderDefinitions)
//            throws CompositeApplicationDefinitionException {
//
//        if (startupOrderDefinitions == null) {
//            return null;
//        }
//
//        Set<StartupOrder> startupOrders = new HashSet<StartupOrder>();
//
//        for (StartupOrderDefinition startupOrderDefinition : startupOrderDefinitions) {
//            startupOrders.add(new StartupOrder(startupOrderDefinition.getStart(), startupOrderDefinition.getAfter()));
//        }
//
//        return startupOrders;
//    }
//
//    private String getKillbehaviour (String serviceGroupName) throws CompositeApplicationDefinitionException {
//
//        ServiceGroupDefinition groupDefinition;
//
//        try {
//            groupDefinition = dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName);
//
//        } catch (PersistenceManagerException e) {
//            throw new CompositeApplicationDefinitionException(e);
//        }
//
//        if (groupDefinition == null) {
//            throw new CompositeApplicationDefinitionException("Service Group Definition not found for name " + serviceGroupName);
//        }
//
//        if (groupDefinition.getDependencies() != null) {
//            return groupDefinition.getDependencies().getKillBehaviour();
//        }
//
//        return null;
//
//    }
//
//    private Set<SubscribableContext> getSubsribableContexts (List<SubscribableDefinition> subscribableDefinitions,
//                                                             Map<String, SubscribableInfo> subscribableInformation)
//            throws CompositeApplicationDefinitionException {
//
//        Set<SubscribableContext> subscribableContexts = new HashSet<SubscribableContext>();
//
//        for (SubscribableDefinition subscribableDefinition : subscribableDefinitions) {
//            // check is there is a related Subscribable Information
//            SubscribableInfo subscribableInfo = subscribableInformation.get(subscribableDefinition.getAlias());
//            if (subscribableInfo == null) {
//                throw new CompositeApplicationDefinitionException("Related Subscribable Information not found for Subscribable with alias: "
//                        + subscribableDefinition.getAlias());
//            }
//
//            // check if Cartridge Type is valid
//            if (subscribableDefinition.getType() == null || subscribableDefinition.getType().isEmpty()) {
//                throw new CompositeApplicationDefinitionException ("Invalid Cartridge Type specified : [ "
//                        + subscribableDefinition.getType() + " ]");
//            }
//
//            // check if a cartridge with relevant type is already deployed. else, can't continue
//            if (!isCartrigdeDeployed(subscribableDefinition.getType())) {
//                throw new CompositeApplicationDefinitionException("No deployed Cartridge found with type [ " + subscribableDefinition.getType() +
//                        " ] for Composite Application");
//            }
//
//            subscribableContexts.add(ParserUtils.convert(subscribableDefinition, subscribableInfo));
//        }
//
//        return subscribableContexts;
//    }
//
//    private boolean isCartrigdeDeployed (String cartridgeType) throws CompositeApplicationDefinitionException {
//
//        CloudControllerServiceClient ccServiceClient;
//
//        try {
//            ccServiceClient = CloudControllerServiceClient.getServiceClient();
//
//        } catch (AxisFault axisFault) {
//            throw new CompositeApplicationDefinitionException(axisFault);
//        }
//
//        try {
//            return ccServiceClient.getCartridgeInfo(cartridgeType) != null;
//
//        } catch (RemoteException e) {
//            throw new CompositeApplicationDefinitionException(e);
//
//        } catch (CloudControllerServiceUnregisteredCartridgeExceptionException e) {
//            throw new CompositeApplicationDefinitionException(e);
//        }
//    }

}
