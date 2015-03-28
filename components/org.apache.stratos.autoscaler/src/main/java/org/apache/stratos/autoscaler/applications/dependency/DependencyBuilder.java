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
    public DependencyTree buildDependency(ParentComponent component)
            throws DependencyBuilderException{

        String identifier = component.getUniqueIdentifier();
        DependencyTree dependencyTree = new DependencyTree(identifier);
        DependencyOrder dependencyOrder = component.getDependencyOrder();

        if (dependencyOrder != null) {
            log.info(String.format("Building dependency tree: [component] %s ", identifier));

            String terminationBehaviour = dependencyOrder.getTerminationBehaviour();
            if (AutoscalerConstants.TERMINATE_NONE.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_NONE);
            } else if (AutoscalerConstants.TERMINATE_ALL.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_ALL);
            } else if (AutoscalerConstants.TERMINATE_DEPENDENTS.equals(terminationBehaviour)) {
                dependencyTree.setTerminationBehavior(DependencyTree.TerminationBehavior.TERMINATE_DEPENDENT);
            }

            log.info(String.format("Termination behaviour set: [component] %s [termination-behaviour] %s",
                    identifier, dependencyTree.getTerminationBehavior()));

            Set<StartupOrder> startupOrders = dependencyOrder.getStartupOrders();
            ApplicationChildContext parentContext;

            if (startupOrders != null) {
                log.debug(String.format("Processing startup orders: [component] %s [startup-orders] %s",
                        identifier, startupOrders));

                for (StartupOrder startupOrder : startupOrders) {
                    parentContext = null;
                    log.debug(String.format("Processing startup order: [component] %s " +
                            "[startup-order] %s", identifier, startupOrder));

                    for (String startupOrderComponent : startupOrder.getStartupOrderComponentList()) {
                        if (startupOrderComponent != null) {
                            log.debug(String.format("Processing startup order element: " +
                                            "[component] %s [startup-order] %s [element] %s",
                                     identifier, startupOrder, startupOrderComponent));

                            ApplicationChildContext applicationChildContext = ApplicationChildContextFactory.
                                    createApplicationChildContext(identifier, startupOrderComponent,
                                            component, dependencyTree);
                            String applicationChildContextId = applicationChildContext.getId();

                            ApplicationChildContext existingApplicationChildContext =
                                    dependencyTree.getApplicationChildContextByIdInPrimaryTree(applicationChildContextId);
                            if (existingApplicationChildContext == null) {
                                // Application child context is not found in dependency tree
                                if (parentContext != null) {
                                    // Add application child context to the current parent element
                                    parentContext.addApplicationChildContext(applicationChildContext);
                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("Added element [%s] to the parent element [%s]: " +
                                                        "[dependency-tree] %s",
                                                applicationChildContext.getId(), parentContext.getId(), dependencyTree));
                                    }
                                    parentContext = applicationChildContext;
                                } else {
                                    // This is the first element, add it as the root
                                    dependencyTree.addPrimaryApplicationContext(applicationChildContext);
                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("Added root element [%s]: [dependency-tree] %s",
                                                applicationChildContext.getId(), dependencyTree));
                                    }
                                    parentContext = applicationChildContext;
                                }
                            } else {
                                // Application child context is already there in the dependency tree
                                if (parentContext == null) {
                                    // This is the first element of the startup order, make it the parent and continue
                                    parentContext = existingApplicationChildContext;
                                    if (log.isDebugEnabled()) {
                                        log.debug(String.format("Element [%s] was found in the dependency tree," +
                                                " making it the parent and continuing: [dependency-tree] %s",
                                                existingApplicationChildContext.getId(), dependencyTree));
                                    }
                                } else {
                                    // Dependency tree is already built for the startup order up to some extent
                                    ApplicationChildContext existingParentContext =
                                            dependencyTree.findParentContextWithId(applicationChildContext.getId());
                                    if((existingParentContext != null) &&
                                            (existingParentContext.getId().equals(existingApplicationChildContext.getId()))) {
                                        // Application child context is already available in the dependency tree,
                                        // find its parent element, mark it as the parent element and continue
                                        if(log.isDebugEnabled()) {
                                            log.debug(String.format("Found an existing parent element [%s] in the " +
                                                            "dependency tree, making it the parent " +
                                                            "and continuing: [dependency-tree] %s",
                                                    existingParentContext.getId(), dependencyTree));
                                        }
                                        parentContext = existingParentContext;
                                    } else {
                                        String msg = "Startup order is not valid. It contains an element " +
                                                "which has been defined more than once in another startup order: " +
                                                startupOrder;
                                        throw new DependencyBuilderException(msg);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Adding the rest of the children who are independent to the top level
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
