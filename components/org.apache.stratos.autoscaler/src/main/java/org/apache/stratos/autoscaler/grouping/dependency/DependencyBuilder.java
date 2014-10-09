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
package org.apache.stratos.autoscaler.grouping.dependency;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stratos.autoscaler.*;
import org.apache.stratos.autoscaler.exception.DependencyBuilderException;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContextFactory;
import org.apache.stratos.autoscaler.grouping.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.GroupContext;
import org.apache.stratos.messaging.domain.topology.*;

import java.util.Set;

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
     * This will build the dependency tree based on the given dependency order
     * @param component it will give the necessary information to build the tree
     * @return the dependency tree out of the dependency orders
     */
    public DependencyTree buildDependency(ParentComponent component) throws DependencyBuilderException{

        String identifier = component.getUniqueIdentifier();
        DependencyTree dependencyTree = new DependencyTree(identifier);
        DependencyOrder dependencyOrder = component.getDependencyOrder();

        if (dependencyOrder != null) {
            if (log.isDebugEnabled()) {
                log.debug("Building dependency for the Application/Group " +
                        identifier);
            }

            //Parsing the kill behaviour
            String killBehavior = dependencyOrder.getKillbehavior();

            if (Constants.KILL_NONE.equals(killBehavior)) {
                dependencyTree.setKillNone(true);
            } else if (Constants.KILL_ALL.equals(killBehavior)) {
                dependencyTree.setKillAll(true);
            } else if (Constants.KILL_DEPENDENTS.equals(killBehavior)) {
                dependencyTree.setKillDependent(true);
            }
            if (log.isDebugEnabled()) {
                log.debug("Setting the [killBehavior] " + killBehavior + " to the " +
                        "[dependency-tree] " + dependencyTree.getId());
            }

            //Parsing the start up order
            String [] startupOrders = dependencyOrder.getStartupOrders();
            ApplicationContext foundContext = null; 
            if (startupOrders == null) {
            	if (log.isDebugEnabled()) {
                    log.debug("startupOrders is null, returning default dependency tree (empty)");
                }
            	
            	return dependencyTree;
            }
            for (String startupOrder : startupOrders) {
            	String start = dependencyOrder.getStartStartupOrder(startupOrder);
                foundContext = null;
                
                if (start != null) {
                    ApplicationContext applicationContext = ApplicationContextFactory.
                                    getApplicationContext(start, component,
                                                            dependencyTree.isKillDependent());
                    String id = applicationContext.getId();

                    ApplicationContext existingApplicationContext =
                            dependencyTree.findApplicationContextWithId(id);
                    if (existingApplicationContext == null) {
                        if (foundContext != null) {
                            //appending the start up order to existing group/cluster
                            foundContext.addApplicationContext(applicationContext);
                            if (log.isDebugEnabled()) {
                                log.debug("Found an existing [dependency] " + foundContext.getId() +
                                        " and adding the [dependency] " + id + " as the child");
                            }
                        } else {
                            //adding list of startup order to the dependency tree
                            dependencyTree.addApplicationContext(applicationContext);
                        }
                    } else {
                        if (foundContext == null) {
                            //assigning the found context to the later use.
                            foundContext = existingApplicationContext;
                            if (log.isDebugEnabled()) {
                                log.debug("Found an existing [dependency] " + id + " and setting it " +
                                        "for the next dependency to follow");
                            }
                        } else {
                            String msg = "Startup order is not consistent. It contains the group/cluster " +
                                    "which has been used more than one in another startup order";
                            throw new DependencyBuilderException(msg);
                        }

                    }
                }
            }
            //TODO need to parser the scalable dependencies
        }


        //adding the rest of the children who are independent to the top level
        // as they can start in parallel.
        for (Group group1 : component.getAliasToGroupMap().values()) {
            if (dependencyTree.findApplicationContextWithId(group1.getAlias()) == null) {
                dependencyTree.addApplicationContext(new GroupContext(group1.getAlias(),
                        dependencyTree.isKillDependent()));
            }
        }
        for (ClusterDataHolder dataHolder : component.getClusterDataMap().values()) {
            if (dependencyTree.findApplicationContextWithId(dataHolder.getClusterId()) == null) {
                dependencyTree.addApplicationContext(new ClusterContext(dataHolder.getClusterId(),
                        dependencyTree.isKillDependent()));

            }
        }
        return dependencyTree;
    }
}
