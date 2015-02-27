/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.stratos.autoscaler.applications.dependency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContext;
import org.apache.stratos.autoscaler.applications.dependency.context.ApplicationChildContextFactory;
import org.apache.stratos.autoscaler.exception.application.DependencyBuilderException;
import org.apache.stratos.autoscaler.util.AutoscalerConstants;
import org.apache.stratos.messaging.domain.application.*;

import java.util.*;

/**
 * This is to build the startup/termination dependencies
 * across immediate children according to the start order defined.
 */
public class DependencyBuilder {
    private static final Log log = LogFactory.getLog(DependencyBuilder.class);

    private DependencyBuilder() {

    }

    private static class Holder {
        private static final DependencyBuilder INSTANCE = new DependencyBuilder();
    }

    public static DependencyBuilder getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * This will build the dependency tree based on the given dependencies
     * @param component it will give the necessary information to build the tree
     * @return the dependency tree out of the dependency orders
     */
    public DependencyTree buildDependency(String applicationId, ParentComponent component)
            throws DependencyBuilderException{

        String identifier = component.getUniqueIdentifier();
        DependencyTree dependencyTree = new DependencyTree(identifier);
        DependencyOrder dependencyOrder = component.getDependencyOrder();

        if (dependencyOrder != null) {
            log.info(String.format("Building dependency tree: [application-id] %s [component] %s ",
                    applicationId, identifier));

            log.info(String.format("Setting termination behaviour: [application-id] %s [component] %s ",
                    applicationId, identifier));

            String terminationBehaviour = dependencyOrder.getTerminationBehaviour();
            if (AutoscalerConstants.TERMINATE_NONE.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_NONE);
            } else if (AutoscalerConstants.TERMINATE_ALL.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_ALL);
            } else if (AutoscalerConstants.TERMINATE_DEPENDENTS.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_DEPENDENT);
            }

            Set<StartupOrder> startupOrders = dependencyOrder.getStartupOrders();
            ApplicationChildContext parentContext;

            if (startupOrders != null) {
                log.info(String.format("Processing startup orders: [application-id] %s [component] %s",
                        applicationId, identifier));

                for (StartupOrder startupOrder : startupOrders) {
                    parentContext = null;
                    for (String startupOrderComponent : startupOrder.getStartupOrderComponentList()) {

                        if (startupOrderComponent != null) {
                            ApplicationChildContext applicationChildContext = ApplicationChildContextFactory.
                                    createApplicationChildContext(applicationId, startupOrderComponent,
                                            component, dependencyTree);
                            String applicationChildContextId = applicationChildContext.getId();

                            ApplicationChildContext existingApplicationChildContext =
                                    dependencyTree.getApplicationChildContextByIdInPrimaryTree(applicationChildContextId);
                            if (existingApplicationChildContext == null) {
                                if (parentContext != null) {
                                    //appending the start up order to already added parent group/cluster
                                    parentContext.addApplicationChildContext(applicationChildContext);
                                    parentContext = applicationChildContext;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Found an existing [dependency] " + parentContext.getId() +
                                                " and adding the [dependency] " + applicationChildContextId + " as the child");
                                    }
                                } else {
                                    //adding list of startup order to the dependency tree
                                    dependencyTree.addPrimaryApplicationContext(applicationChildContext);
                                    parentContext = applicationChildContext;
                                }
                            } else {
                                if (parentContext == null) {
                                    //if existing context found, add it to child of existing context and
                                    //set the existing context as the next parent
                                    //existingApplicationChildContext.addApplicationChildContext(applicationChildContext);
                                    parentContext = existingApplicationChildContext;
                                    if (log.isDebugEnabled()) {
                                        log.debug("Found an existing [dependency] " + applicationChildContextId + " and setting it " +
                                                "for the next dependency to follow");
                                    }
                                } else {
                                    ApplicationChildContext existingParentContext =
                                            dependencyTree.findParentContextWithId(applicationChildContext.getId());
                                    if((existingParentContext != null) &&
                                            (existingParentContext.getId().equals(parentContext.getId()))) {
                                        if(log.isDebugEnabled()) {
                                            log.debug("Found an existing parent context. " +
                                                    "Hence skipping it and parsing the next value.");
                                        }
                                        parentContext = existingApplicationChildContext;
                                    } else {
                                        String msg = "Startup order is not consistent. It contains the group/cluster " +
                                                "which has been used more than one in another startup order";
                                        throw new DependencyBuilderException(msg);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        //adding the rest of the children who are independent to the top level
        // as they can start in parallel.
        Collection<Group> groups = component.getAliasToGroupMap().values();
        for (Group group : groups) {
            if (dependencyTree.getApplicationChildContextByIdInPrimaryTree(group.getAlias()) == null) {
                ApplicationChildContext context = ApplicationChildContextFactory.createGroupChildContext(group.getAlias(),
                        dependencyTree.isTerminateDependent());
                dependencyTree.addPrimaryApplicationContext(context);
            }
        }
        Collection<ClusterDataHolder> clusterData = component.getClusterDataMap().values();
        for (ClusterDataHolder dataHolder : clusterData) {
            if (dependencyTree.getApplicationChildContextByIdInPrimaryTree(dataHolder.getClusterId()) == null) {
                ApplicationChildContext context = ApplicationChildContextFactory.createClusterChildContext(dataHolder,
                        dependencyTree.isTerminateDependent());
                dependencyTree.addPrimaryApplicationContext(context);

            }
        }
        if(log.isDebugEnabled()) {
            log.debug(String.format("Dependency tree generated: [component] %s [tree] %s",
                    component.getUniqueIdentifier(), dependencyTree.toString()));
        }
        return dependencyTree;
    }
    
    /**
     * 
     * Utility method to build scaling dependencies
     * 
     */
	public Set<ScalingDependentList> buildScalingDependencies(ParentComponent component) {
		Set<ScalingDependentList> scalingDependentLists = new HashSet<ScalingDependentList>();

        if (component.getDependencyOrder() != null && component.getDependencyOrder().getScalingDependents() != null) {

            for (ScalingDependentList dependentList : component.getDependencyOrder().getScalingDependents()) {
                List<String> scalingDependencies = new ArrayList<String>();
                for (String string : dependentList.getScalingDependentListComponents()) {
                    if (string.trim().startsWith(AutoscalerConstants.GROUP + ".")) {
                        //getting the group alias
                        scalingDependencies.add(getGroupFromStartupOrder(string));
                    } else if (string.trim().startsWith(AutoscalerConstants.CARTRIDGE + ".")) {
                        //getting the cluster alias
                        String id = getClusterFromStartupOrder(string);
                        //getting the cluster-id from cluster alias

                        if(component.getClusterDataForAlias().containsKey(id)) {
                            ClusterDataHolder clusterDataHolder = (ClusterDataHolder) component.getClusterDataForAlias().get(id);
                            scalingDependencies.add(clusterDataHolder.getClusterId());
                        } else{

                            log.warn("[Scaling Dependency Id]: " + id + " is not a defined cartridge or group. " +
                                    "Therefore scaling dependent will not be effective");
                        }
                    } else {
                        log.warn("[Scaling Dependency]: " + string + " contains unknown reference. Therefore scaling " +
                                "dependent will not be effective");
                    }
                }
                ScalingDependentList scalingDependentList = new ScalingDependentList(scalingDependencies);
                scalingDependentLists.add(scalingDependentList);


            }
        }
	    return scalingDependentLists;
    }

    /**
     * Utility method to get the group alias from the startup order Eg: group.mygroup
     *
     * @param startupOrder startup order
     * @return group alias
     */
    public static String getGroupFromStartupOrder(String startupOrder) {
        return startupOrder.substring(AutoscalerConstants.GROUP.length() + 1);
    }

    /**
     * Utility method to get the cluster alias from startup order Eg: cartridge.myphp
     *
     * @param startupOrder startup order
     * @return cluster alias
     */
    public static String getClusterFromStartupOrder(String startupOrder) {
        return startupOrder.substring(AutoscalerConstants.CARTRIDGE.length() + 1);
    }
}
