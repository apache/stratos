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

package org.apache.stratos.manager.composite.application.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.manager.composite.application.beans.CompositeAppDefinition;
import org.apache.stratos.manager.composite.application.beans.GroupDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableDefinition;
import org.apache.stratos.manager.composite.application.beans.SubscribableInfo;
import org.apache.stratos.manager.composite.application.structure.CompositeAppContext;
import org.apache.stratos.manager.composite.application.structure.GroupContext;
import org.apache.stratos.manager.composite.application.structure.StartupOrder;
import org.apache.stratos.manager.composite.application.structure.SubscribableContext;
import org.apache.stratos.manager.exception.CompositeApplicationDefinitionException;
import org.apache.stratos.manager.exception.PersistenceManagerException;
import org.apache.stratos.manager.grouping.definitions.ServiceGroupDefinition;
import org.apache.stratos.manager.grouping.definitions.StartupOrderDefinition;
import org.apache.stratos.manager.retriever.DataInsertionAndRetrievalManager;

import java.util.*;

public class DefaultCompositeApplicationParser implements CompositeApplicationParser {

    private static Log log = LogFactory.getLog(DefaultCompositeApplicationParser.class);

    DataInsertionAndRetrievalManager dataInsertionAndRetrievalMgr;

    public DefaultCompositeApplicationParser () {
        dataInsertionAndRetrievalMgr = new DataInsertionAndRetrievalManager();
    }

    @Override
    public CompositeAppContext parse(Object obj) throws CompositeApplicationDefinitionException {

        CompositeAppDefinition compositeAppDefinition = null;

        if (obj instanceof CompositeAppDefinition) {
            compositeAppDefinition = (CompositeAppDefinition) obj;
        }

        if (compositeAppDefinition == null) {
            throw new CompositeApplicationDefinitionException("Invlaid Composite Application Defintion");
        }

        if (compositeAppDefinition.getAlias() == null || compositeAppDefinition.getAlias().isEmpty()) {
            throw new CompositeApplicationDefinitionException("Invalid alias specified");
        }

        if (compositeAppDefinition.getApplicationId() == null || compositeAppDefinition.getApplicationId().isEmpty()) {
            throw new CompositeApplicationDefinitionException("Invalid Composite App id specified");
        }

        // get the defined groups
        Map<String, GroupDefinition> definedGroups = getDefinedGroups(compositeAppDefinition);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, GroupDefinition>> groupEntries = definedGroups.entrySet();
            log.debug("Defined Groups: [ ");
            for (Map.Entry<String, GroupDefinition> groupEntry : groupEntries) {
                log.debug("Group alias: " + groupEntry.getKey());
            }
            log.debug(" ]");
        }

        // get the Subscribables Information
        Map<String, SubscribableInfo> subscribablesInfo = getSubscribableInformation(compositeAppDefinition);
        if (log.isDebugEnabled()) {
            Set<Map.Entry<String, SubscribableInfo>> subscribableInfoEntries = subscribablesInfo.entrySet();
            log.debug("Defined Subscribable Information: [ ");
            for (Map.Entry<String, SubscribableInfo> subscribableInfoEntry : subscribableInfoEntries) {
                log.debug("Subscribable Information alias: " + subscribableInfoEntry.getKey());
            }
            log.debug(" ]");
        }

        return buildCompositeAppStructure (compositeAppDefinition, definedGroups, subscribablesInfo);
    }

    private Map<String, GroupDefinition> getDefinedGroups (CompositeAppDefinition compositeAppDefinition) throws
            CompositeApplicationDefinitionException {

        // map [group alias -> Group Definition]
        Map<String, GroupDefinition> definedGroups = null;

        if (compositeAppDefinition.getComponents() != null) {
            if (compositeAppDefinition.getComponents().getGroups() != null) {
                definedGroups = new HashMap<String, GroupDefinition>();

                for (GroupDefinition group : compositeAppDefinition.getComponents().getGroups()) {

                    // check validity of group name
                    if (group.getName() == null || group.getName().isEmpty()) {
                        throw new CompositeApplicationDefinitionException("Invalid Group name specified");
                    }

                    // check if group is deployed
                    if(isGroupDeployed(group.getName())) {
                        throw new CompositeApplicationDefinitionException("Group with name " + group.getName() + " not deployed");
                    }

                    // check validity of group alias
                    if (group.getAlias() == null || group.getAlias().isEmpty()) {
                        throw new CompositeApplicationDefinitionException("Invalid Group alias specified");
                    }

                    // check if a group is already defined under the same alias
                    if(definedGroups.get(group.getAlias()) != null) {
                        // a group with same alias already exists, can't continue
                        throw new CompositeApplicationDefinitionException("A Group with alias " + group.getAlias() + " already exists");
                    }

                    definedGroups.put(group.getAlias(), group);
                    if (log.isDebugEnabled()) {
                        log.debug("Added Group Definition [ " + group.getName() +" , " + group.getAlias() + " ] to map [group alias -> Group Definition]");
                    }
                }
            }
        }

        return definedGroups;
    }

    private Map<String, SubscribableInfo> getSubscribableInformation (CompositeAppDefinition compositeAppDefinition) throws
            CompositeApplicationDefinitionException {

        // map [cartridge alias -> Subscribable Information]
        Map<String, SubscribableInfo> subscribableInformation = null;

        if (compositeAppDefinition.getSubscribableInfo() != null) {
            subscribableInformation = new HashMap<String, SubscribableInfo>();

            for (SubscribableInfo subscribableInfo : compositeAppDefinition.getSubscribableInfo()) {

                if (subscribableInfo.getAlias() == null || subscribableInfo.getAlias().isEmpty()) {
                    throw new CompositeApplicationDefinitionException("Invalid alias specified for Subscribable Information Obj");
                }

                // check if a group is already defined under the same alias
                if(subscribableInformation.get(subscribableInfo.getAlias()) != null) {
                    // a group with same alias already exists, can't continue
                    throw new CompositeApplicationDefinitionException("A Subscribable Info obj with alias " + subscribableInfo.getAlias() + " already exists");
                }

                subscribableInformation.put(subscribableInfo.getAlias(), subscribableInfo);
                if (log.isDebugEnabled()) {
                    log.debug("Added Subcribables Info obj [ " + subscribableInfo.getAlias() + " ] to map [cartridge alias -> Subscribable Information]");
                }
            }
        }

        return subscribableInformation;
    }

    private boolean isGroupDeployed (String serviceGroupName) throws CompositeApplicationDefinitionException {

        try {
           return dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName) != null;

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

    }

    private CompositeAppContext buildCompositeAppStructure (CompositeAppDefinition compositeAppDefinition,
                                                            Map<String, GroupDefinition> definedGroups,
                                                            Map<String, SubscribableInfo> subscribableInformation)
            throws CompositeApplicationDefinitionException {

        CompositeAppContext compositeAppContext = new CompositeAppContext();

        // get top level Subscribables
        if (compositeAppDefinition.getComponents() != null) {
            if (compositeAppDefinition.getComponents().getSubscribables() != null) {
                compositeAppContext.setSubscribableContexts(getSubsribableContexts(compositeAppDefinition.getComponents().getSubscribables(),
                        subscribableInformation));
            }

            // get Groups
            if (compositeAppDefinition.getComponents().getGroups() != null) {
                compositeAppContext.setGroupContexts(getGroupContexts(compositeAppDefinition.getComponents().getGroups(),
                        subscribableInformation, definedGroups));
            }

            // get top level Dependency definitions
            if (compositeAppDefinition.getComponents().getDependencies() != null) {
                compositeAppContext.setStartupOrder(getStartupOrderForApplicationComponents(compositeAppDefinition.getComponents().
                        getDependencies().getStartupOrder()));

                compositeAppContext.setKillBehaviour(compositeAppDefinition.getComponents().getDependencies().getKillBehaviour());
            }
        }

        return compositeAppContext;
    }

    private List<GroupContext> getGroupContexts (List<GroupDefinition> groupDefinitions,
                                                 Map<String, SubscribableInfo> subscribableInformation,
                                                 Map<String, GroupDefinition> definedGroups)
            throws CompositeApplicationDefinitionException {

        List<GroupContext> groupContexts = new ArrayList<GroupContext>();

        for (GroupDefinition group : groupDefinitions) {
            groupContexts.add(getGroupContext(group, subscribableInformation, definedGroups));
        }

        return groupContexts;
    }

    private GroupContext getGroupContext (GroupDefinition group, Map<String, SubscribableInfo> subscribableInformation,
                                          Map<String, GroupDefinition> definedGroups) throws CompositeApplicationDefinitionException {

        // check if are in the defined Group set
        GroupDefinition definedGroupDef = definedGroups.get(group.getAlias());
        if (definedGroupDef == null) {
            throw new CompositeApplicationDefinitionException("Group Definition with name: " + group.getName() + ", alias: " +
                    group.getAlias() + " is not found in the all Group Definitions collection");
        }

        GroupContext groupContext = new GroupContext();
        // get group level Subscribables
        if (group.getSubscribables() != null) {
            groupContext.setName(group.getName());
            groupContext.setAlias(group.getAlias());
            groupContext.setAutoscalingPolicy(group.getAutoscalingPolicy());
            groupContext.setDeploymentPolicy(group.getDeploymentPolicy());
            groupContext.setSubscribableContexts(getSubsribableContexts(group.getSubscribables(), subscribableInformation));
            groupContext.setStartupOrder(getStartupOrderForGroup(group.getName()));
            groupContext.setKillBehaviour(getKillbehaviour(group.getName()));
        }
        // get nested groups
        if (group.getSubGroups() != null) {
            List<GroupContext> nestedGroupContexts = new ArrayList<GroupContext>();
            // check sub groups
            for (GroupDefinition subGroup : group.getSubGroups()) {
                nestedGroupContexts.add(getGroupContext(subGroup, subscribableInformation, definedGroups));
            }

            groupContext.setGroupContexts(nestedGroupContexts);
        }

        return groupContext;
    }

    private List<StartupOrder> getStartupOrderForGroup(String serviceGroupName) throws CompositeApplicationDefinitionException {

        ServiceGroupDefinition groupDefinition;

        try {
            groupDefinition = dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName);

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

        if (groupDefinition == null) {
            throw new CompositeApplicationDefinitionException("Service Group Definition not found for name " + serviceGroupName);
        }

        if (groupDefinition.getDependencies() != null) {
            if (groupDefinition.getDependencies().getStartupOrder() != null) {
                return ParserUtils.convert(groupDefinition.getDependencies().getStartupOrder());
            }
        }

        return null;
    }

    private List<StartupOrder> getStartupOrderForApplicationComponents (List<StartupOrderDefinition> startupOrderDefinitions)
            throws CompositeApplicationDefinitionException {

        if (startupOrderDefinitions == null) {
            return null;
        }

        List<StartupOrder> startupOrders = new ArrayList<StartupOrder>();

        for (StartupOrderDefinition startupOrderDefinition : startupOrderDefinitions) {
            startupOrders.add(new StartupOrder(startupOrderDefinition.getStart(), startupOrderDefinition.getAfter()));
        }

        return startupOrders;
    }

    private String getKillbehaviour (String serviceGroupName) throws CompositeApplicationDefinitionException {

        ServiceGroupDefinition groupDefinition;

        try {
            groupDefinition = dataInsertionAndRetrievalMgr.getServiceGroupDefinition(serviceGroupName);

        } catch (PersistenceManagerException e) {
            throw new CompositeApplicationDefinitionException(e);
        }

        if (groupDefinition == null) {
            throw new CompositeApplicationDefinitionException("Service Group Definition not found for name " + serviceGroupName);
        }

        if (groupDefinition.getDependencies() != null) {
            return groupDefinition.getDependencies().getKillBehaviour();
        }

        return null;

    }

    private List<SubscribableContext> getSubsribableContexts (List<SubscribableDefinition> subscribableDefinitions,
                                                              Map<String, SubscribableInfo> subscribableInformation)
            throws CompositeApplicationDefinitionException {

        List<SubscribableContext> subscribableContexts = new ArrayList<SubscribableContext>();

        for (SubscribableDefinition subscribableDefinition : subscribableDefinitions) {
            // check is there is a related Subscribable Information
            SubscribableInfo subscribableInfo = subscribableInformation.get(subscribableDefinition.getAlias());
            if (subscribableInfo == null) {
                throw new CompositeApplicationDefinitionException("Related Subscribable Information not found for Subscribable with alias: "
                        + subscribableDefinition.getAlias());
            }

            subscribableContexts.add(ParserUtils.convert(subscribableDefinition, subscribableInfo));
        }

        return subscribableContexts;
    }

}
