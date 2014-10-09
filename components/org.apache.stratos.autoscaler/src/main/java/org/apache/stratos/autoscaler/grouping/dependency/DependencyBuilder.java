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
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.ApplicationContextFactory;
import org.apache.stratos.autoscaler.grouping.dependency.context.ClusterContext;
import org.apache.stratos.autoscaler.grouping.dependency.context.GroupContext;
import org.apache.stratos.messaging.domain.topology.*;

import java.util.Set;

/**
 * This is to build the startup/termination dependencies
 * across all the groups and clusters
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
    public DependencyTree buildDependency(ParentComponent component) {

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
                                    getApplicationContext(start, component, dependencyTree);
                    String id = applicationContext.getId(); //TODO change the id
                    
     

                    ApplicationContext existingApplicationContext =
                            dependencyTree.findApplicationContextWithId(id);
                    if (existingApplicationContext == null) {                  
                            //adding list of startup order to the dependency tree
                            dependencyTree.addApplicationContext(applicationContext);
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

    /*public static Queue<String> getStartupOrder(ParentBehavior component) {


        Queue<String> startup = new LinkedList<String>();
        DependencyOrder dependencyOrder = component.getDependencyOrder();
        if (dependencyOrder != null) {
            Set<StartupOrder> startupOrderSet = dependencyOrder.getStartupOrders();
            for (StartupOrder startupOrder : startupOrderSet) {

                String start = startupOrder.getStart();
                String after = startupOrder.getAfter();

                if (!startup.contains(start)) {
                    startup.add(start);
                    if (!startup.contains(after)) {
                        startup.add(after);

                    } else {
                        //TODO throw exception since after is there before start
                    }
                } else {
                    if (!startup.contains(after)) {
                        startup.add(after);
                    } else {
                        //TODO throw exception since start and after already there
                    }
                }
            }
        }
        //TODO adding all the missed groups or clusters as the top child to the list
        //TODO handle by application and group itself groupName and serviceName

        if (component instanceof Application) {

        } else if (component instanceof Group) {

        }
        return startup;

    }*/

}
